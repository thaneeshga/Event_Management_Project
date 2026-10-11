package com.college.event;

import com.college.event.ui.LoginFrame;
import com.college.event.ui.UITheme;

import javax.swing.SwingUtilities;

/** Starts the College Event Management System. */
public class Main {

    public static void main(String[] args) {
        // Swing windows must be created on the Event Dispatch Thread
        SwingUtilities.invokeLater(() -> {
            UITheme.install();
            new LoginFrame().setVisible(true);
        });
    }
}