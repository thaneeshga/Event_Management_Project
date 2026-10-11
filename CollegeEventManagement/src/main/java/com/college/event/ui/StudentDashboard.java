package com.college.event.ui;

import com.college.event.model.Event;
import com.college.event.model.Person;
import com.college.event.model.RegistrationDetail;
import com.college.event.model.RegistrationStatus;
import com.college.event.model.Role;
import com.college.event.model.Student;
import com.college.event.service.EventService;
import com.college.event.service.RegistrationService;
import com.college.event.service.ServiceException;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

/**
 * The student window: browse and search upcoming events, view details, register,
 * see "My Registrations", cancel, record the fee paid, and remove cancelled rows.
 */
public class StudentDashboard extends JFrame {

    private final Student student;
    private final EventService eventService = new EventService();
    private final RegistrationService registrationService = new RegistrationService();

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel content = new JPanel(cardLayout);

    private final DefaultTableModel upcomingModel = UITheme.readOnlyModel(
            "ID", "Event", "Date", "Time", "Venue", "Fee", "Seats Left", "Status");
    private final DefaultTableModel myRegistrationsModel = UITheme.readOnlyModel(
            "Reg. ID", "Event", "Event Date", "Venue", "Fee", "Fee Paid", "Payment",
            "Registered On", "Status");
    private final JTable upcomingTable = UITheme.createTable(upcomingModel);
    private final JTable myRegistrationsTable = UITheme.createTable(myRegistrationsModel);

    private final JTextField searchField = new JTextField(22);
    private final JButton registerButton = UITheme.successButton("Register for Selected Event");
    private final JButton cancelButton = UITheme.dangerButton("Cancel Registration");
    private final JButton updateFeeButton = UITheme.primaryButton("Update Fee Paid");
    private final JButton removeButton = UITheme.secondaryButton("Remove Cancelled");


    // The objects behind the table rows, so a selected row can be turned back into an object
    private List<Event> shownEvents = new ArrayList<>();
    private List<RegistrationDetail> shownRegistrations = new ArrayList<>();

    public StudentDashboard(Person user) {
        super("College Event Management - Student");
        if (user == null || user.getRole() != Role.STUDENT) {
            throw new IllegalArgumentException("A student account is required.");
        }
        this.student = (Student) user;

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1180, 720);
        setMinimumSize(new Dimension(980, 600));
        setLocationRelativeTo(null);

        Sidebar sidebar = new Sidebar("Event Manager",
                student.getFullName() + " (" + student.getDepartment() + ", Year "
                        + student.getYearOfStudy() + ")");
        JButton upcoming = sidebar.addNavItem("Upcoming Events", () -> {
            cardLayout.show(content, "upcoming");
            loadUpcoming();
        });
        sidebar.addNavItem("My Registrations", () -> {
            cardLayout.show(content, "mine");
            loadMyRegistrations();
        });
        sidebar.addLogoutButton(this::logout);

        upcomingTable.getColumnModel().getColumn(1).setPreferredWidth(240);
        myRegistrationsTable.getColumnModel().getColumn(1).setPreferredWidth(220);


        String firstName = student.getFullName().split(" ")[0];
        content.setBackground(UITheme.BACKGROUND);
        content.add(buildPage("Welcome, " + firstName, "Here are the events coming up",
                buildUpcomingToolbar(), upcomingTable, this::loadUpcoming,
                "Tip: double-click an event to see its full details."), "upcoming");
        content.add(buildPage("My Registrations", "Events you have registered for",
                buildMineToolbar(), myRegistrationsTable, this::loadMyRegistrations,
                "Fee paid is the amount you record yourself; the college does not verify it here. "
                        + "A registration can be cancelled until the day before the event."), "mine");

        add(sidebar, BorderLayout.WEST);
        add(content, BorderLayout.CENTER);

        sidebar.select(upcoming);
        cardLayout.show(content, "upcoming");
        loadUpcoming();
        loadMyRegistrations();
    }

    // ---------- page building ----------

    private JPanel buildUpcomingToolbar() {
        UITheme.styleInput(searchField);
        searchField.setToolTipText("Search by event name or event ID");

        JButton searchButton = UITheme.primaryButton("Search");
        JButton clearButton = UITheme.secondaryButton("Clear");
        JButton detailsButton = UITheme.secondaryButton("View Details");

        searchButton.addActionListener(e -> loadUpcoming());
        searchField.addActionListener(e -> loadUpcoming()); // Enter key
        clearButton.addActionListener(e -> {

            searchField.setText("");
            loadUpcoming();
        });
        detailsButton.addActionListener(e -> showDetails());
        registerButton.addActionListener(e -> registerSelected());

        upcomingTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    showDetails();
                }
            }
        });

        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        bar.setOpaque(false);
        bar.add(UITheme.label("Search:", UITheme.BODY_BOLD, UITheme.TEXT));
        bar.add(searchField);
        bar.add(searchButton);
        bar.add(clearButton);
        bar.add(Box.createHorizontalStrut(16));
        bar.add(detailsButton);
        bar.add(registerButton);
        return bar;
    }

    private JPanel buildMineToolbar() {
        cancelButton.addActionListener(e -> cancelSelected());
        updateFeeButton.addActionListener(e -> updateFeeSelected());
        removeButton.addActionListener(e -> removeSelected());


        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        bar.setOpaque(false);
        bar.add(updateFeeButton);
        bar.add(cancelButton);
        bar.add(removeButton);
        return bar;
    }

    /** A page with a title, Refresh button, a toolbar, a table and a hint line. */
    private JPanel buildPage(String title, String subtitle, JPanel toolbar,
                             JTable table, Runnable refresh, String hint) {
        JPanel page = new JPanel(new BorderLayout(0, 16));
        page.setBackground(UITheme.BACKGROUND);
        page.setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));

        JPanel titles = new JPanel();
        titles.setLayout(new BoxLayout(titles, BoxLayout.Y_AXIS));
        titles.setOpaque(false);
        titles.add(UITheme.label(title, UITheme.TITLE, UITheme.TEXT));
        titles.add(UITheme.label(subtitle, UITheme.BODY, UITheme.MUTED));

        JButton refreshButton = UITheme.secondaryButton("Refresh");
        refreshButton.addActionListener(e -> refresh.run());
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 8));
        right.setOpaque(false);
        right.add(refreshButton);

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(titles, BorderLayout.WEST);
        header.add(right, BorderLayout.EAST);


        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(UITheme.BORDER));
        scroll.getViewport().setBackground(Color.WHITE);

        JPanel center = new JPanel(new BorderLayout(0, 12));
        center.setOpaque(false);
        center.add(toolbar, BorderLayout.NORTH);
        center.add(scroll, BorderLayout.CENTER);

        page.add(header, BorderLayout.NORTH);
        page.add(center, BorderLayout.CENTER);
        page.add(UITheme.label(hint, UITheme.SMALL, UITheme.MUTED), BorderLayout.SOUTH);
        return page;
    }

    // ---------- loading data from MySQL ----------

    /** Upcoming events, or the upcoming events that match the search text. */
    private List<Event> fetchEvents(String keyword) throws ServiceException {
        if (keyword == null || keyword.isBlank()) {
            return eventService.getUpcomingEvents();
        }
        return eventService.searchEvents(keyword).stream()
                .filter(Event::isUpcoming)
                .collect(Collectors.toList());
    }

    private void loadUpcoming() {
        String keyword = searchField.getText().trim();
        BackgroundTask.run(this, () -> fetchEvents(keyword), events -> {
            shownEvents = new ArrayList<>(events);
            upcomingModel.setRowCount(0);

            for (Event ev : events) {
                upcomingModel.addRow(new Object[]{
                        ev.getEventId(),
                        ev.getEventName(),
                        ev.getEventDate().format(UITheme.DATE_FORMAT),
                        ev.getStartTime().format(UITheme.TIME_FORMAT),
                        ev.getVenue(),
                        UITheme.formatFee(ev.getRegistrationFee()),
                        ev.getAvailableSeats(),
                        ev.getStatus()});
            }
        });
    }

    private void loadMyRegistrations() {
        BackgroundTask.run(this, () -> registrationService.getMyRegistrations(student), list -> {
            shownRegistrations = new ArrayList<>(list);
            myRegistrationsModel.setRowCount(0);
            for (RegistrationDetail d : list) {
                myRegistrationsModel.addRow(new Object[]{
                        d.getFormattedId(),
                        d.getEventName(),
                        d.getEventDate().format(UITheme.DATE_FORMAT),
                        d.getVenue(),
                        UITheme.formatFee(d.getRegistrationFee()),
                        d.getFeePaid().signum() == 0 ? "-" : UITheme.formatFee(d.getFeePaid()),
                        d.getPaymentStatus(),
                        d.getRegistrationDate().format(UITheme.DATETIME_FORMAT),
                        d.getStatus()});
            }
        });
    }


    // ---------- upcoming events: button actions ----------

    /** Returns the event selected in the table, or null (after telling the user). */
    private Event selectedEvent() {
        int row = upcomingTable.getSelectedRow();
        if (row < 0 || row >= shownEvents.size()) {
            UITheme.showInfo(this, "Select an event", "Please click an event in the table first.");
            return null;
        }
        return shownEvents.get(row);
    }

    private void showDetails() {
        Event ev = selectedEvent();
        if (ev == null) {
            return;
        }
        String description = (ev.getDescription() == null || ev.getDescription().isBlank())
                ? "No description provided." : ev.getDescription();
        String text = ev.getEventName() + "\n\n"
                + "Date: " + ev.getEventDate().format(UITheme.DATE_FORMAT)
                + " at " + ev.getStartTime().format(UITheme.TIME_FORMAT) + "\n"
                + "Venue: " + ev.getVenue() + "\n"
                + "Organizer: " + ev.getOrganizer() + "\n"
                + "Fee: " + UITheme.formatFee(ev.getRegistrationFee()) + "\n"
                + "Seats: " + ev.getAvailableSeats() + " of " + ev.getMaxCapacity() + " available\n"
                + "Status: " + ev.getStatus() + "\n\n"
                + description;

        JTextArea area = new JTextArea(text, 13, 40);
        area.setEditable(false);

        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setFont(UITheme.BODY);
        JOptionPane.showMessageDialog(this, new JScrollPane(area), "Event details",
                JOptionPane.INFORMATION_MESSAGE);
    }

    /** The Register button: the checks and the transaction happen in RegistrationService. */
    private void registerSelected() {
        Event ev = selectedEvent();
        if (ev == null) {
            return;
        }
        if (!UITheme.confirm(this, "Register for \"" + ev.getEventName() + "\"?\n"
                + "Fee: " + UITheme.formatFee(ev.getRegistrationFee()))) {
            return;
        }

        registerButton.setEnabled(false);
        BackgroundTask.run(this,
                () -> registrationService.registerForEvent(student, ev.getEventId()),
                registration -> UITheme.showInfo(this, "Registration successful",
                        "You are registered for " + ev.getEventName() + ".\n\n"
                                + "Your registration ID: " + registration.getFormattedId()),
                () -> {
                    // runs after success OR failure, so seat counts are always up to date
                    registerButton.setEnabled(true);
                    loadUpcoming();
                    loadMyRegistrations();
                });
    }


    // ---------- my registrations: button actions ----------

    private RegistrationDetail selectedRegistration() {
        int row = myRegistrationsTable.getSelectedRow();
        if (row < 0 || row >= shownRegistrations.size()) {
            UITheme.showInfo(this, "Select a registration",
                    "Please click a registration in the table first.");
            return null;
        }
        return shownRegistrations.get(row);
    }

    private void cancelSelected() {
        RegistrationDetail detail = selectedRegistration();
        if (detail == null) {
            return;
        }
        if (!detail.isCancellable()) {
            UITheme.showError(this, "This registration cannot be cancelled.\n"
                    + "Only active registrations for events after today can be cancelled.");
            return;
        }
        if (!UITheme.confirm(this, "Cancel your registration for \""
                + detail.getEventName() + "\"?")) {
            return;
        }

        cancelButton.setEnabled(false);
        BackgroundTask.run(this,
                () -> {
                    registrationService.cancelRegistration(student, detail.getRegistrationId());
                    return detail.getFormattedId();

                },
                id -> UITheme.showInfo(this, "Registration cancelled",
                        "Registration " + id + " has been cancelled."),
                () -> {
                    cancelButton.setEnabled(true);
                    loadUpcoming();
                    loadMyRegistrations();
                });
    }

    /** Lets the student record the total amount they have paid for the selected registration. */
    private void updateFeeSelected() {
        RegistrationDetail detail = selectedRegistration();
        if (detail == null) {
            return;
        }
        if (detail.getStatus() != RegistrationStatus.REGISTERED) {
            UITheme.showError(this, "The fee can only be updated for an active registration.");
            return;
        }
        if (detail.getRegistrationFee().signum() == 0) {
            UITheme.showInfo(this, "Free event", "This event is free, so there is nothing to pay.");
            return;
        }

        // Suggest the full fee when nothing is recorded yet, otherwise the current amount
        String suggestion = detail.getFeePaid().signum() == 0
                ? detail.getRegistrationFee().toPlainString()
                : detail.getFeePaid().toPlainString();
        String input = JOptionPane.showInputDialog(this,
                "Event fee: " + UITheme.formatFee(detail.getRegistrationFee())
                        + "\nEnter the total amount you have paid:", suggestion);

        if (input == null) {
            return; // the user pressed Cancel
        }
        BigDecimal amount = parseAmount(input);
        if (amount == null) {
            UITheme.showError(this, "Please enter a number, for example 150 or 150.50");
            return;
        }

        updateFeeButton.setEnabled(false);
        BackgroundTask.run(this,
                () -> {
                    registrationService.updateFeePaid(student, detail.getRegistrationId(), amount);
                    return amount;
                },
                paid -> UITheme.showInfo(this, "Payment recorded",
                        "Recorded: " + UITheme.formatFee(paid) + " paid of "
                                + UITheme.formatFee(detail.getRegistrationFee())),
                () -> {
                    updateFeeButton.setEnabled(true);
                    loadMyRegistrations();
                });
    }

    /** Turns text into a number, or returns null if it is not a valid number. */
    private BigDecimal parseAmount(String text) {
        try {
            return new BigDecimal(text.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }


    /** Permanently deletes a cancelled registration from the student's list. */
    private void removeSelected() {
        RegistrationDetail detail = selectedRegistration();
        if (detail == null) {
            return;
        }
        if (detail.getStatus() != RegistrationStatus.CANCELLED) {
            UITheme.showInfo(this, "Cancel first",
                    "Only cancelled registrations can be removed.\n"
                            + "Use \"Cancel Registration\" first if you want to remove this one.");
            return;
        }
        if (!UITheme.confirm(this, "Permanently remove cancelled registration "
                + detail.getFormattedId() + "?\nThis cannot be undone.")) {
            return;
        }

        removeButton.setEnabled(false);
        BackgroundTask.run(this,
                () -> {
                    registrationService.removeCancelledRegistration(student, detail.getRegistrationId());
                    return detail.getFormattedId();
                },
                id -> UITheme.showInfo(this, "Removed", "Registration " + id + " was removed."),
                () -> {
                    removeButton.setEnabled(true);
                    loadMyRegistrations();
                });
    }

    private void logout() {

        if (UITheme.confirm(this, "Do you want to log out?")) {
            dispose();
            new LoginFrame().setVisible(true);
        }
    }
}