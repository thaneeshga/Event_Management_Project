package com.college.event.ui;

import com.college.event.service.AuthenticationService;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

/** Student sign-up form. Opened from the login window. */
public class RegisterFrame extends JFrame {

    private static final String[] DEPARTMENTS =
            {"CSE", "IT", "ECE", "EEE", "MECH", "CIVIL", "AI&DS"};

    private final AuthenticationService auth = new AuthenticationService();

    private final LoginFrame loginFrame;

    private final JTextField nameField = new JTextField(20);
    private final JTextField emailField = new JTextField(20);
    private final JComboBox<String> departmentCombo = new JComboBox<>(DEPARTMENTS);
    private final JComboBox<Integer> yearCombo = new JComboBox<>(new Integer[]{1, 2, 3, 4, 5, 6});
    private final JTextField phoneField = new JTextField(20);
    private final JPasswordField passwordField = new JPasswordField(20);
    private final JPasswordField confirmField = new JPasswordField(20);
    private final JCheckBox showPassword = new JCheckBox("Show passwords");
    private final JButton createButton = UITheme.primaryButton("Create Account");

    public RegisterFrame(LoginFrame loginFrame) {
        super("College Event Management System - Register");
        this.loginFrame = loginFrame;

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                backToLogin(null); // closing this window returns to the login window
            }
        });
        setSize(640, 720);
        setMinimumSize(new Dimension(560, 560));
        setLocationRelativeTo(null);

        JPanel card = UITheme.cardPanel();
        card.setLayout(new BorderLayout(0, 16));
        card.add(buildHeader(), BorderLayout.NORTH);
        card.add(buildForm(), BorderLayout.CENTER);
        card.add(buildButtons(), BorderLayout.SOUTH);


        JPanel outer = new JPanel(new GridBagLayout());
        outer.setBackground(UITheme.BACKGROUND);
        outer.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        outer.add(card);

        JScrollPane scroll = new JScrollPane(outer);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        add(scroll);

        getRootPane().setDefaultButton(createButton);
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setOpaque(false);
        header.add(UITheme.label("Create your account", UITheme.TITLE, UITheme.TEXT));
        header.add(UITheme.label("Students can register for events after signing up",
                UITheme.BODY, UITheme.MUTED));
        return header;
    }

    private JPanel buildForm() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);

        UITheme.styleInput(nameField);
        UITheme.styleInput(emailField);
        UITheme.styleInput(phoneField);
        UITheme.styleInput(passwordField);

        UITheme.styleInput(confirmField);
        UITheme.styleCombo(departmentCombo);
        UITheme.styleCombo(yearCombo);
        departmentCombo.setEditable(true); // pick from the list or type your own

        showPassword.setOpaque(false);
        showPassword.setFont(UITheme.SMALL);
        char defaultEcho = passwordField.getEchoChar();
        showPassword.addActionListener(e -> {
            char echo = showPassword.isSelected() ? (char) 0 : defaultEcho;
            passwordField.setEchoChar(echo);
            confirmField.setEchoChar(echo);
        });

        addRow(form, 0, "Full name", nameField);
        addRow(form, 1, "Email", emailField);
        addRow(form, 2, "Department", departmentCombo);
        addRow(form, 3, "Year of study", yearCombo);
        addRow(form, 4, "Phone (10 digits)", phoneField);
        addRow(form, 5, "Password", passwordField);
        addRow(form, 6, "Confirm password", confirmField);
        addRow(form, 7, "", showPassword);

        JComponent hint = UITheme.label("Password: at least 8 characters with a letter and a digit.",
                UITheme.SMALL, UITheme.MUTED);
        addRow(form, 8, "", hint);
        return form;
    }

    private void addRow(JPanel form, int row, String labelText, JComponent field) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = row;


        c.gridx = 0;
        c.weightx = 0;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(6, 0, 6, 16);
        form.add(UITheme.label(labelText, UITheme.BODY_BOLD, UITheme.TEXT), c);

        c.gridx = 1;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(6, 0, 6, 0);
        form.add(field, c);
    }

    private JPanel buildButtons() {
        JButton clearButton = UITheme.secondaryButton("Clear");
        JButton backButton = UITheme.secondaryButton("Back to Login");
        createButton.addActionListener(e -> handleCreate());
        clearButton.addActionListener(e -> clearForm());
        backButton.addActionListener(e -> backToLogin(null));

        JPanel buttons = new JPanel(new GridLayout(1, 3, 8, 0));
        buttons.setOpaque(false);
        buttons.add(createButton);
        buttons.add(clearButton);
        buttons.add(backButton);
        return buttons;
    }

    // ---------- actions ----------

    private void handleCreate() {

        String name = nameField.getText();
        String email = emailField.getText();
        Object selectedDepartment = departmentCombo.getSelectedItem();
        String department = selectedDepartment == null ? "" : selectedDepartment.toString().trim();
        int year = (Integer) yearCombo.getSelectedItem();
        String phone = phoneField.getText();
        String password = new String(passwordField.getPassword());
        String confirm = new String(confirmField.getPassword());

        createButton.setEnabled(false);
        BackgroundTask.run(this,
                () -> auth.registerStudent(name, email, department, year, phone, password, confirm),
                student -> {
                    UITheme.showInfo(this, "Account created",
                            "Your account has been created.\nYou can now log in with " + student.getEmail());
                    backToLogin(student.getEmail());
                },
                () -> createButton.setEnabled(true));
    }

    private void clearForm() {
        nameField.setText("");
        emailField.setText("");
        departmentCombo.setSelectedIndex(0);
        yearCombo.setSelectedIndex(0);
        phoneField.setText("");
        passwordField.setText("");
        confirmField.setText("");
        nameField.requestFocusInWindow();
    }

    private void backToLogin(String email) {

        dispose();
        loginFrame.showAgain(email);
    }
}
