package com.college.event.ui;

import com.college.event.model.Person;
import com.college.event.model.Role;
import com.college.event.service.AuthenticationService;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

/** The first window: brand panel on the left, login form on the right. */
public class LoginFrame extends JFrame {

    private final AuthenticationService auth = new AuthenticationService();

    private final JComboBox<String> roleCombo =
            new JComboBox<>(new String[]{"Student", "Administrator"});
    private final JTextField emailField = new JTextField();
    private final JPasswordField passwordField = new JPasswordField();
    private final JCheckBox showPassword = new JCheckBox("Show password");
    private final JButton loginButton = UITheme.primaryButton("Login");

    public LoginFrame() {
        super("College Event Management System - Login");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(920, 580);
        setMinimumSize(new Dimension(780, 540));
        setLocationRelativeTo(null);

        setLayout(new GridLayout(1, 2));
        add(buildBrandPanel());
        add(buildFormPanel());

        // Pressing Enter anywhere in the window clicks Login
        getRootPane().setDefaultButton(loginButton);
    }

    // ---------- left side: logo area ----------

    private JPanel buildBrandPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(UITheme.TABLE_HEADER);

        LogoPanel logo = new LogoPanel();
        logo.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel title = brandLabel("College Event", new Font("Segoe UI", Font.BOLD, 30), Color.WHITE);
        JLabel subtitle = brandLabel("Management System", new Font("Segoe UI", Font.BOLD, 30), Color.WHITE);
        JLabel line1 = brandLabel("Symposiums, workshops, hackathons,", UITheme.BODY, new Color(191, 219, 254));
        JLabel line2 = brandLabel("coding contests and cultural programs", UITheme.BODY, new Color(191, 219, 254));

        panel.add(Box.createVerticalGlue());
        panel.add(logo);
        panel.add(Box.createVerticalStrut(24));
        panel.add(title);
        panel.add(subtitle);
        panel.add(Box.createVerticalStrut(16));
        panel.add(line1);
        panel.add(line2);
        panel.add(Box.createVerticalGlue());
        return panel;
    }

    private JLabel brandLabel(String text, Font font, Color color) {
        JLabel label = UITheme.label(text, font, color);
        label.setAlignmentX(Component.CENTER_ALIGNMENT);
        return label;
    }

    // ---------- right side: the form ----------

    private JPanel buildFormPanel() {
        JPanel outer = new JPanel(new GridBagLayout()); // centers the form
        outer.setBackground(Color.WHITE);

        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setOpaque(false);
        form.setPreferredSize(new Dimension(340, 440));

        UITheme.styleInput(emailField);
        UITheme.styleInput(passwordField);
        UITheme.styleCombo(roleCombo);
        fixHeight(emailField, 40);
        fixHeight(passwordField, 40);
        fixHeight(roleCombo, 38);
        roleCombo.setAlignmentX(Component.LEFT_ALIGNMENT);
        emailField.setAlignmentX(Component.LEFT_ALIGNMENT);
        passwordField.setAlignmentX(Component.LEFT_ALIGNMENT);

        showPassword.setOpaque(false);
        showPassword.setFont(UITheme.SMALL);
        showPassword.setAlignmentX(Component.LEFT_ALIGNMENT);
        char defaultEcho = passwordField.getEchoChar();
        showPassword.addActionListener(e ->
                passwordField.setEchoChar(showPassword.isSelected() ? (char) 0 : defaultEcho));

        JButton clearButton = UITheme.secondaryButton("Clear");
        JButton exitButton = UITheme.dangerButton("Exit");
        loginButton.addActionListener(e -> handleLogin());
        clearButton.addActionListener(e -> clearForm());
        exitButton.addActionListener(e -> System.exit(0));

        JPanel buttons = new JPanel(new GridLayout(1, 3, 8, 0));
        buttons.setOpaque(false);
        buttons.setAlignmentX(Component.LEFT_ALIGNMENT);
        buttons.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        buttons.add(loginButton);
        buttons.add(clearButton);
        buttons.add(exitButton);

        JButton registerLink = UITheme.linkButton("New student? Create an account");
        registerLink.addActionListener(e -> openRegisterWindow());

        form.add(UITheme.label("Welcome back", UITheme.TITLE, UITheme.TEXT));
        form.add(UITheme.label("Log in to continue", UITheme.BODY, UITheme.MUTED));
        form.add(Box.createVerticalStrut(24));
        form.add(UITheme.label("Login as", UITheme.BODY_BOLD, UITheme.TEXT));
        form.add(Box.createVerticalStrut(6));
        form.add(roleCombo);
        form.add(Box.createVerticalStrut(14));
        form.add(UITheme.label("Email", UITheme.BODY_BOLD, UITheme.TEXT));
        form.add(Box.createVerticalStrut(6));
        form.add(emailField);
        form.add(Box.createVerticalStrut(14));
        form.add(UITheme.label("Password", UITheme.BODY_BOLD, UITheme.TEXT));
        form.add(Box.createVerticalStrut(6));
        form.add(passwordField);
        form.add(Box.createVerticalStrut(6));
        form.add(showPassword);
        form.add(Box.createVerticalStrut(20));
        form.add(buttons);
        form.add(Box.createVerticalStrut(22));
        form.add(registerLink);

        outer.add(form);
        return outer;
    }

    private void fixHeight(JComponent component, int height) {
        component.setPreferredSize(new Dimension(300, height));
        component.setMaximumSize(new Dimension(Integer.MAX_VALUE, height));
    }

    // ---------- actions ----------

    private void handleLogin() {
        String email = emailField.getText();
        String password = new String(passwordField.getPassword());
        Role role = roleCombo.getSelectedIndex() == 0 ? Role.STUDENT : Role.ADMIN;

        loginButton.setEnabled(false);
        BackgroundTask.run(this,
                () -> auth.login(email, password, role),   // work (background thread)
                this::openDashboard,                       // success (Swing thread)
                () -> loginButton.setEnabled(true));       // always
    }

    private void openDashboard(Person user) {
        dispose(); // close the login window
        JFrame next = (user.getRole() == Role.ADMIN)
                ? new AdminDashboard(user)
                : new StudentDashboard(user);
        next.setVisible(true);
    }

    private void clearForm() {
        emailField.setText("");
        passwordField.setText("");
        showPassword.setSelected(false);
        passwordField.setEchoChar('\u2022');
        roleCombo.setSelectedIndex(0);
        emailField.requestFocusInWindow();
    }

    private void openRegisterWindow() {
        setVisible(false);
        new RegisterFrame(this).setVisible(true);
    }

    /** Called by RegisterFrame to bring this window back. */
    public void showAgain(String prefillEmail) {
        if (prefillEmail != null) {
            emailField.setText(prefillEmail);
        }
        passwordField.setText("");
        roleCombo.setSelectedIndex(0);
        setVisible(true);
        passwordField.requestFocusInWindow();
    }

    // ---------- the round logo (nested class) ----------

    private static class LogoPanel extends JPanel {

        LogoPanel() {
            setOpaque(false);
            Dimension size = new Dimension(110, 110);
            setPreferredSize(size);
            setMaximumSize(size);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            g2.setColor(Color.WHITE);
            g2.fillOval(0, 0, 108, 108);

            g2.setColor(UITheme.PRIMARY);
            g2.setFont(new Font("Segoe UI", Font.BOLD, 44));
            FontMetrics metrics = g2.getFontMetrics();
            String text = "CE";
            int x = (108 - metrics.stringWidth(text)) / 2;
            int y = (108 - metrics.getHeight()) / 2 + metrics.getAscent();
            g2.drawString(text, x, y);
            g2.dispose();
        }
    }
}
