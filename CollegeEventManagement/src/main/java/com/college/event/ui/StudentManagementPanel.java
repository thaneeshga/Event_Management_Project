package com.college.event.ui;

import com.college.event.model.RegistrationDetail;
import com.college.event.model.Student;
import com.college.event.model.StudentSummary;
import com.college.event.model.Person;
import com.college.event.service.RegistrationService;
import com.college.event.service.StudentService;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

/**
 * Admin screen: all students on top. Click a student and their details and the
 * events they registered for appear underneath.
 */
public class StudentManagementPanel extends JPanel {


    private final Person admin;
    private final StudentService studentService = new StudentService();
    private final RegistrationService registrationService = new RegistrationService();

    private final DefaultTableModel studentModel = UITheme.readOnlyModel(
            "ID", "Name", "Email", "Department", "Year", "Phone", "Active Registrations");
    private final JTable studentTable = UITheme.createTable(studentModel);

    private final DefaultTableModel registrationModel = UITheme.readOnlyModel(
            "Reg. ID", "Event", "Event Date", "Venue", "Fee", "Fee Paid", "Payment", "Status");
    private final JTable registrationTable = UITheme.createTable(registrationModel);

    private final JTextField searchField = new JTextField(22);
    private final JLabel countLabel = UITheme.label(" ", UITheme.SMALL, UITheme.MUTED);
    private final JLabel detailsLabel = UITheme.label(
            "Select a student to see their details and registered events.",
            UITheme.BODY_BOLD, UITheme.TEXT);

    private List<StudentSummary> shownStudents = new ArrayList<>();
    private int currentStudentId = -1; // used to ignore results that arrive late

    public StudentManagementPanel(Person admin) {
        this.admin = admin;

        setLayout(new BorderLayout(0, 14));
        setBackground(UITheme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));

        studentTable.getColumnModel().getColumn(2).setPreferredWidth(220);
        registrationTable.getColumnModel().getColumn(1).setPreferredWidth(220);


        // When a different row is selected, load that student's registrations
        studentTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                showSelectedStudent();
            }
        });

        JPanel top = new JPanel(new BorderLayout(0, 12));
        top.setOpaque(false);
        top.add(buildHeader(), BorderLayout.NORTH);
        top.add(buildToolbar(), BorderLayout.CENTER);

        JScrollPane studentScroll = new JScrollPane(studentTable);
        studentScroll.setBorder(BorderFactory.createLineBorder(UITheme.BORDER));
        studentScroll.getViewport().setBackground(Color.WHITE);
        studentScroll.setPreferredSize(new Dimension(0, 260));

        JPanel bottom = new JPanel(new BorderLayout(0, 8));
        bottom.setOpaque(false);
        bottom.setBorder(BorderFactory.createEmptyBorder(12, 0, 0, 0));
        JPanel bottomHeader = new JPanel();
        bottomHeader.setLayout(new BoxLayout(bottomHeader, BoxLayout.Y_AXIS));
        bottomHeader.setOpaque(false);
        bottomHeader.add(UITheme.label("Events registered by the selected student",
                UITheme.HEADING, UITheme.TEXT));
        bottomHeader.add(detailsLabel);
        JScrollPane registrationScroll = new JScrollPane(registrationTable);
        registrationScroll.setBorder(BorderFactory.createLineBorder(UITheme.BORDER));
        registrationScroll.getViewport().setBackground(Color.WHITE);
        bottom.add(bottomHeader, BorderLayout.NORTH);
        bottom.add(registrationScroll, BorderLayout.CENTER);


        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, studentScroll, bottom);
        split.setBorder(null);
        split.setOpaque(false);
        split.setResizeWeight(0.55);

        add(top, BorderLayout.NORTH);
        add(split, BorderLayout.CENTER);
        add(countLabel, BorderLayout.SOUTH);
    }

    private JPanel buildHeader() {
        JPanel titles = new JPanel();
        titles.setLayout(new BoxLayout(titles, BoxLayout.Y_AXIS));
        titles.setOpaque(false);
        titles.add(UITheme.label("Manage Students", UITheme.TITLE, UITheme.TEXT));
        titles.add(UITheme.label("Registered students and the events they signed up for",
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
        searchField.setToolTipText("Name, email, department or student ID");

        JButton searchButton = UITheme.primaryButton("Search");
        JButton clearButton = UITheme.secondaryButton("Clear");
        searchButton.addActionListener(e -> search());
        searchField.addActionListener(e -> search());
        clearButton.addActionListener(e -> {
            searchField.setText("");
            loadAll();
        });

        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        bar.setOpaque(false);
        bar.add(UITheme.label("Search:", UITheme.BODY_BOLD, UITheme.TEXT));
        bar.add(searchField);
        bar.add(searchButton);
        bar.add(clearButton);
        return bar;
    }

    // ---------- loading ----------

    public void refresh() {
        loadAll();
    }

    private void loadAll() {
        BackgroundTask.run(this, () -> studentService.getAllStudents(admin), this::fillStudents);
    }

    private void search() {

        String keyword = searchField.getText();
        BackgroundTask.run(this, () -> studentService.searchStudents(admin, keyword), this::fillStudents);
    }

    private void fillStudents(List<StudentSummary> students) {
        shownStudents = new ArrayList<>(students);
        studentModel.setRowCount(0); // also clears the selection and the lower table
        for (StudentSummary summary : students) {
            Student s = summary.student();
            studentModel.addRow(new Object[]{
                    s.getId(), s.getFullName(), s.getEmail(), s.getDepartment(),
                    s.getYearOfStudy(), s.getPhone(), summary.activeRegistrations()});
        }
        countLabel.setText("Showing " + students.size() + " student(s). Click a row to see their events.");
    }

    // ---------- the selected student ----------

    private void showSelectedStudent() {
        int row = studentTable.getSelectedRow();
        if (row < 0 || row >= shownStudents.size()) {
            currentStudentId = -1;
            registrationModel.setRowCount(0);
            detailsLabel.setText("Select a student to see their details and registered events.");
            return;
        }

        Student s = shownStudents.get(row).student();
        currentStudentId = s.getId();
        detailsLabel.setText(s.getFullName() + "   |   " + s.getEmail() + "   |   "
                + s.getDepartment() + ", Year " + s.getYearOfStudy() + "   |   Phone " + s.getPhone());


        int requestedId = s.getId();
        BackgroundTask.run(this,
                () -> registrationService.getRegistrationsForStudent(admin, requestedId),
                list -> {
                    if (requestedId != currentStudentId) {
                        return; // the admin already clicked another student
                    }
                    registrationModel.setRowCount(0);
                    for (RegistrationDetail d : list) {
                        registrationModel.addRow(new Object[]{
                                d.getFormattedId(),
                                d.getEventName(),
                                d.getEventDate().format(UITheme.DATE_FORMAT),
                                d.getVenue(),
                                UITheme.formatFee(d.getRegistrationFee()),
                                d.getFeePaid().signum() == 0 ? "-" : UITheme.formatFee(d.getFeePaid()),
                                d.getPaymentStatus(),
                                d.getStatus()});
                    }
                });
    }
}