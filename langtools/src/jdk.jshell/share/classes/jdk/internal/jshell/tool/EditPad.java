/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package jdk.internal.jshell.tool;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

/**
 * A minimal Swing editor as a fallback when the user does not specify an
 * external editor.
 */
@SuppressWarnings("serial")             // serialVersionUID intentionally omitted
public class EditPad extends JFrame implements Runnable {
    private final Consumer<String> errorHandler; // For possible future error handling
    private final String initialText;
    private final CountDownLatch closeLock;
    private final Consumer<String> saveHandler;

    EditPad(Consumer<String> errorHandler, String initialText,
            CountDownLatch closeLock, Consumer<String> saveHandler) {
        super("JShell Edit Pad");
        this.errorHandler = errorHandler;
        this.initialText = initialText;
        this.closeLock = closeLock;
        this.saveHandler = saveHandler;
    }

    @Override
    public void run() {
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                EditPad.this.dispose();
                closeLock.countDown();
            }
        });
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        JTextArea textArea = new JTextArea(initialText);
        add(new JScrollPane(textArea), BorderLayout.CENTER);
        add(buttons(textArea), BorderLayout.SOUTH);

        setSize(800, 600);
        setVisible(true);
    }

    private JPanel buttons(JTextArea textArea) {
        FlowLayout flow = new FlowLayout();
        flow.setHgap(35);
        JPanel buttons = new JPanel(flow);
        JButton cancel = new JButton("Cancel");
        cancel.setMnemonic(KeyEvent.VK_C);
        JButton accept = new JButton("Accept");
        accept.setMnemonic(KeyEvent.VK_A);
        JButton exit = new JButton("Exit");
        exit.setMnemonic(KeyEvent.VK_X);
        buttons.add(cancel);
        buttons.add(accept);
        buttons.add(exit);

        cancel.addActionListener(e -> {
            close();
        });
        accept.addActionListener(e -> {
            saveHandler.accept(textArea.getText());
        });
        exit.addActionListener(e -> {
            saveHandler.accept(textArea.getText());
            close();
        });

        return buttons;
    }

    private void close() {
        setVisible(false);
        dispose();
        closeLock.countDown();
    }

    public static void edit(Consumer<String> errorHandler, String initialText,
            Consumer<String> saveHandler) {
        CountDownLatch closeLock = new CountDownLatch(1);
        SwingUtilities.invokeLater(
                new EditPad(errorHandler, initialText, closeLock, saveHandler));
        do {
            try {
                closeLock.await();
                break;
            } catch (InterruptedException ex) {
                // ignore and loop
            }
        } while (true);
    }
}
