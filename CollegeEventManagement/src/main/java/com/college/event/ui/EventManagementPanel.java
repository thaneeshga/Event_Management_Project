package com.college.event.ui;

import com.college.event.model.Event;
import com.college.event.model.Person;
import com.college.event.service.EventService;
import com.college.event.service.ServiceException;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;

/** Admin screen: view, search, filter, add, update and delete events. */
public class EventManagementPanel extends JPanel {


    private final Person admin;
    private final EventService eventService = new EventService();

    private final DefaultTableModel model = UITheme.readOnlyModel(
            "ID", "Event", "Date", "Time", "Venue", "Organizer", "Fee",
            "Capacity", "Registered", "Seats Left", "Status");
    private final JTable table = UITheme.createTable(model);

    private final JTextField searchField = new JTextField(18);
    private final JTextField fromField = new JTextField(9);
    private final JTextField toField = new JTextField(9);
    private final JLabel countLabel = UITheme.label(" ", UITheme.SMALL, UITheme.MUTED);

    // The objects behind the table rows (row number = position in this list)
    private List<Event> shownEvents = new ArrayList<>();

    public EventManagementPanel(Person admin) {
        this.admin = admin;

        setLayout(new BorderLayout(0, 14));
        setBackground(UITheme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));

        table.getColumnModel().getColumn(1).setPreferredWidth(220);
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    updateSelected(); // double-click edits the event
                }
            }
        });


        JPanel top = new JPanel(new BorderLayout(0, 12));
        top.setOpaque(false);
        top.add(buildHeader(), BorderLayout.NORTH);
        top.add(buildToolbars(), BorderLayout.CENTER);

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
        titles.add(UITheme.label("Manage Events", UITheme.TITLE, UITheme.TEXT));
        titles.add(UITheme.label("Add, change, search and remove events", UITheme.BODY, UITheme.MUTED));

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

    private JPanel buildToolbars() {
        UITheme.styleInput(searchField);
        UITheme.styleInput(fromField);
        UITheme.styleInput(toField);
        searchField.setToolTipText("Event name or event ID");
        fromField.setToolTipText("yyyy-MM-dd, for example 2026-11-01");
        toField.setToolTipText("yyyy-MM-dd, for example 2026-11-30");

        JButton addButton = UITheme.successButton("Add Event");
        JButton updateButton = UITheme.primaryButton("Update Selected");
        JButton deleteButton = UITheme.dangerButton("Delete Selected");
        JButton searchButton = UITheme.primaryButton("Search");
        JButton clearButton = UITheme.secondaryButton("Clear");
        JButton filterButton = UITheme.primaryButton("Filter by Date");
        JButton showAllButton = UITheme.secondaryButton("Show All");

        addButton.addActionListener(e -> openForm(null));
        updateButton.addActionListener(e -> updateSelected());
        deleteButton.addActionListener(e -> deleteSelected());
        searchButton.addActionListener(e -> search());
        searchField.addActionListener(e -> search()); // Enter key
        clearButton.addActionListener(e -> {
            searchField.setText("");
            loadAll();
        });
        filterButton.addActionListener(e -> filterByDate());
        showAllButton.addActionListener(e -> {
            fromField.setText("");
            toField.setText("");

            loadAll();
        });

        JPanel row1 = flowRow();
        row1.add(addButton);
        row1.add(updateButton);
        row1.add(deleteButton);

        JPanel row2 = flowRow();
        row2.add(UITheme.label("Search:", UITheme.BODY_BOLD, UITheme.TEXT));
        row2.add(searchField);
        row2.add(searchButton);
        row2.add(clearButton);

        JPanel row3 = flowRow();
        row3.add(UITheme.label("From (yyyy-MM-dd):", UITheme.BODY_BOLD, UITheme.TEXT));
        row3.add(fromField);
        row3.add(UITheme.label("To:", UITheme.BODY_BOLD, UITheme.TEXT));
        row3.add(toField);
        row3.add(filterButton);
        row3.add(showAllButton);

        JPanel toolbars = new JPanel();
        toolbars.setLayout(new BoxLayout(toolbars, BoxLayout.Y_AXIS));
        toolbars.setOpaque(false);
        toolbars.add(row1);
        toolbars.add(row2);
        toolbars.add(row3);
        return toolbars;
    }

    private JPanel flowRow() {

        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        row.setOpaque(false);
        row.setAlignmentX(LEFT_ALIGNMENT);
        return row;
    }

    // ---------- loading ----------

    /** Called by the dashboard each time this page is opened. */
    public void refresh() {
        loadAll();
    }

    private void loadAll() {
        BackgroundTask.run(this, () -> {
            eventService.autoCompletePastEvents(); // past events become COMPLETED
            return eventService.getAllEvents();
        }, this::fillTable);
    }

    private void search() {
        String keyword = searchField.getText();
        BackgroundTask.run(this, () -> eventService.searchEvents(keyword), this::fillTable);
    }

    private void filterByDate() {
        // Read the text on the Swing thread, then do the work in the background
        String fromText = fromField.getText();
        String toText = toField.getText();
        BackgroundTask.run(this,
                () -> eventService.filterByDateRange(parseDate(fromText, "From"), parseDate(toText, "To")),
                this::fillTable);

    }

    private static LocalDate parseDate(String text, String label) throws ServiceException {
        String trimmed = text.trim();
        if (trimmed.isEmpty()) {
            return null; // the service reports that both dates are needed
        }
        try {
            return LocalDate.parse(trimmed);
        } catch (DateTimeParseException ex) {
            throw new ServiceException(label + " date must look like 2026-11-15 (yyyy-MM-dd).");
        }
    }

    private void fillTable(List<Event> events) {
        shownEvents = new ArrayList<>(events);
        model.setRowCount(0);
        for (Event ev : events) {
            model.addRow(new Object[]{
                    ev.getEventId(),
                    ev.getEventName(),
                    ev.getEventDate().format(UITheme.DATE_FORMAT),
                    ev.getStartTime().format(UITheme.TIME_FORMAT),
                    ev.getVenue(),
                    ev.getOrganizer(),
                    UITheme.formatFee(ev.getRegistrationFee()),
                    ev.getMaxCapacity(),
                    ev.getRegisteredCount(),
                    ev.getAvailableSeats(),
                    ev.getStatus()});
        }
        countLabel.setText("Showing " + events.size() + " event(s). Double-click a row to edit it.");

    }

    // ---------- add / update / delete ----------

    private Event selectedEvent() {
        int row = table.getSelectedRow();
        if (row < 0 || row >= shownEvents.size()) {
            UITheme.showInfo(this, "Select an event", "Please click an event in the table first.");
            return null;
        }
        return shownEvents.get(row);
    }

    private void updateSelected() {
        Event ev = selectedEvent();
        if (ev != null) {
            openForm(ev);
        }
    }

    /** Opens the form for a new event (existing == null) or for editing one. */
    private void openForm(Event existing) {
        Window owner = SwingUtilities.getWindowAncestor(this);

        EventFormDialog.EventSaver saver;
        if (existing == null) {
            saver = event -> eventService.addEvent(admin, event);
        } else {
            saver = event -> eventService.updateEvent(admin, event);
        }
        new EventFormDialog(owner, existing, saver, this::loadAll).setVisible(true);
    }


    private void deleteSelected() {
        Event ev = selectedEvent();
        if (ev == null) {
            return;
        }
        if (!UITheme.confirm(this, "Delete the event \"" + ev.getEventName() + "\" permanently?\n"
                + "This cannot be undone.")) {
            return;
        }
        int eventId = ev.getEventId();
        String name = ev.getEventName();

        BackgroundTask.run(this,
                () -> {
                    eventService.deleteEvent(admin, eventId);
                    return name;
                },
                deleted -> UITheme.showInfo(this, "Event deleted", "\"" + deleted + "\" was deleted."),
                this::loadAll); // reload after success or failure
    }
}
