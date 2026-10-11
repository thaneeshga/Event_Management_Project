package com.college.event.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

/** The dark navigation panel on the left of both dashboards. */
public class Sidebar extends JPanel {

    private final List<JButton> navButtons = new ArrayList<>();

    public Sidebar(String appTitle, String userLine) {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(UITheme.SIDEBAR);
        setPreferredSize(new Dimension(250, 0));
        setBorder(BorderFactory.createEmptyBorder(24, 0, 24, 0));

        JLabel title = UITheme.label(appTitle, UITheme.HEADING, Color.WHITE);
        title.setBorder(BorderFactory.createEmptyBorder(0, 24, 0, 20));
        JLabel user = UITheme.label(userLine, UITheme.SMALL, new Color(156, 163, 175));
        user.setBorder(BorderFactory.createEmptyBorder(4, 24, 24, 20));

        add(title);
        add(user);

    }

    /** Adds a navigation button. When clicked it highlights itself and runs the action. */
    public JButton addNavItem(String text, Runnable action) {
        JButton button = UITheme.sidebarButton(text);
        button.addActionListener(e -> {
            select(button);
            action.run();
        });
        navButtons.add(button);
        add(button);
        return button;
    }

    /** Adds a Logout button pinned to the bottom of the sidebar. */
    public void addLogoutButton(Runnable action) {
        add(Box.createVerticalGlue());
        JButton button = UITheme.sidebarButton("Logout");
        button.addActionListener(e -> action.run());
        add(button);
    }

    /** Highlights one navigation button and un-highlights the others. */
    public void select(JButton selected) {
        for (JButton button : navButtons) {
            UITheme.setSelected(button, button == selected);
        }
    }
}