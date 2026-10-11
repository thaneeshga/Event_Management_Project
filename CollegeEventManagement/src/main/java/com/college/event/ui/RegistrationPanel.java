package com.college.event.ui;

import com.college.event.model.Person;
import com.college.event.model.RegistrationDetail;
import com.college.event.model.RegistrationStatus;
import com.college.event.service.RegistrationService;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

/**
 * Admin screen: every registration. The list is loaded once from MySQL and the
 * search box and status filter work on it with the Stream API.
 */
public class RegistrationPanel extends JPanel {


    private static final String ALL_STATUSES = "All statuses";

    private final Person admin;
    private final RegistrationService registrationService = new RegistrationService();

    private final DefaultTableModel model = UITheme.readOnlyModel(
            "Reg. ID", "Student", "Email", "Event", "Event Date", "Fee",
            "Fee Paid", "Payment", "Registered On", "Status");
    private final JTable table = UITheme.createTable(model);

    private final JTextField searchField = new JTextField(22);
    private final JComboBox<String> statusCombo = new JComboBox<>(
            new String[]{ALL_STATUSES, "REGISTERED", "CANCELLED"});
    private final JLabel countLabel = UITheme.label(" ", UITheme.SMALL, UITheme.MUTED);

    private List<RegistrationDetail> allRegistrations = new ArrayList<>();
    private List<RegistrationDetail> shown = new ArrayList<>();

    public RegistrationPanel(Person admin) {
        this.admin = admin;

        setLayout(new BorderLayout(0, 14));
        setBackground(UITheme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));

        table.getColumnModel().getColumn(2).setPreferredWidth(200);
        table.getColumnModel().getColumn(3).setPreferredWidth(200);

        JPanel top = new JPanel(new BorderLayout(0, 12));
        top.setOpaque(false);
        top.add(buildHeader(), BorderLayout.NORTH);

        top.add(buildToolbar(), BorderLayout.CENTER);

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(UITheme.BORDER));
        scroll.getViewport().setBackground(Color.WHITE);

        add(top, BorderLayout.NORTH);
        add(scroll, BorderLayout.CENTER);
        add(countLabel, BorderLayout.SOUTH);
    }

    private JPanel buildHeader() {
        JPanel titles = new JPanel();
        titles.setLayout(new BoxLayout(titles, BoxLayout.Y_AXIS));
        titles.setOpaque(false);
        titles.add(UITheme.label("Manage Registrations", UITheme.TITLE, UITheme.TEXT));
        titles.add(UITheme.label("Every student registration, with search and filters",
                UITheme.BODY, UITheme.MUTED));

        JButton refreshButton = UITheme.secondaryButton("Refresh");
        refreshButton.addActionListener(e -> loadAll());
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 8));
        right.setOpaque(false);
        right.add(refreshButton);

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(titles, BorderLayout.WEST);
        header.add(right, BorderLayout.EAST);
        return header;
    }


    private JPanel buildToolbar() {
        UITheme.styleInput(searchField);
        UITheme.styleCombo(statusCombo);
        searchField.setToolTipText("Student name, email, event name or registration ID");

        JButton searchButton = UITheme.primaryButton("Search");
        JButton clearButton = UITheme.secondaryButton("Clear");
        JButton cancelButton = UITheme.dangerButton("Cancel Selected Registration");

        searchButton.addActionListener(e -> applyFilters());
        searchField.addActionListener(e -> applyFilters());
        statusCombo.addActionListener(e -> applyFilters());
        clearButton.addActionListener(e -> {
            searchField.setText("");
            statusCombo.setSelectedIndex(0);
            applyFilters();
        });
        cancelButton.addActionListener(e -> cancelSelected());

        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        bar.setOpaque(false);
        bar.add(UITheme.label("Search:", UITheme.BODY_BOLD, UITheme.TEXT));
        bar.add(searchField);
        bar.add(UITheme.label("Status:", UITheme.BODY_BOLD, UITheme.TEXT));
        bar.add(statusCombo);
        bar.add(searchButton);
        bar.add(clearButton);
        bar.add(cancelButton);
        return bar;
    }

    // ---------- loading and filtering ----------


    public void refresh() {
        loadAll();
    }

    private void loadAll() {
        BackgroundTask.run(this, () -> registrationService.getAllRegistrations(admin), list -> {
            allRegistrations = new ArrayList<>(list);
            applyFilters();
        });
    }

    /** Filters the loaded list with streams. No database call is needed. */
    private void applyFilters() {
        String keyword = searchField.getText().trim().toLowerCase();
        String status = (String) statusCombo.getSelectedItem();

        shown = allRegistrations.stream()
                .filter(d -> keyword.isEmpty() || matches(d, keyword))
                .filter(d -> ALL_STATUSES.equals(status) || d.getStatus().name().equals(status))
                .collect(Collectors.toList());

        model.setRowCount(0);
        for (RegistrationDetail d : shown) {
            model.addRow(new Object[]{
                    d.getFormattedId(),
                    d.getStudentName(),
                    d.getStudentEmail(),
                    d.getEventName(),
                    d.getEventDate().format(UITheme.DATE_FORMAT),
                    UITheme.formatFee(d.getRegistrationFee()),
                    d.getFeePaid().signum() == 0 ? "-" : UITheme.formatFee(d.getFeePaid()),

                    d.getPaymentStatus(),
                    d.getRegistrationDate().format(UITheme.DATETIME_FORMAT),
                    d.getStatus()});
        }

        // reduce(): adds up the fee paid of the active rows being shown
        BigDecimal paid = shown.stream()
                .filter(d -> d.getStatus() == RegistrationStatus.REGISTERED)
                .map(RegistrationDetail::getFeePaid)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        countLabel.setText("Showing " + shown.size() + " of " + allRegistrations.size()
                + " registrations.  Fee paid, as recorded by students (active rows shown): Rs. " + paid);
    }

    private boolean matches(RegistrationDetail d, String keyword) {
        return d.getStudentName().toLowerCase().contains(keyword)
                || d.getStudentEmail().toLowerCase().contains(keyword)
                || d.getEventName().toLowerCase().contains(keyword)
                || d.getFormattedId().toLowerCase().contains(keyword);
    }

    // ---------- admin cancel ----------

    private void cancelSelected() {
        int row = table.getSelectedRow();
        if (row < 0 || row >= shown.size()) {
            UITheme.showInfo(this, "Select a registration", "Please click a registration in the table first.");
            return;
        }
        RegistrationDetail detail = shown.get(row);
        if (detail.getStatus() != RegistrationStatus.REGISTERED) {

            UITheme.showError(this, "This registration is already cancelled.");
            return;
        }
        if (!UITheme.confirm(this, "Cancel " + detail.getFormattedId() + " for "
                + detail.getStudentName() + " (" + detail.getEventName() + ")?")) {
            return;
        }

        int registrationId = detail.getRegistrationId();
        String formattedId = detail.getFormattedId();
        BackgroundTask.run(this,
                () -> {
                    registrationService.cancelRegistration(admin, registrationId);
                    return formattedId;
                },
                id -> UITheme.showInfo(this, "Registration cancelled", id + " has been cancelled."),
                this::loadAll);
    }
}