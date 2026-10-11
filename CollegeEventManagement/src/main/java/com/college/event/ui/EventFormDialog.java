package com.college.event.ui;

import com.college.event.model.Event;
import com.college.event.model.EventStatus;
import com.college.event.service.ServiceException;

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

/**
 * One form used for both "Add Event" and "Update Event".
 * If the dialog is given an existing event the fields are pre-filled.
 * The dialog stays open until the save succeeds, so a mistake never loses the typing.

 */
public class EventFormDialog extends JDialog {

    /**
     * A nested interface: "something that can save an event". The panel passes in
     * the code (a lambda), so this dialog does not need to know whether it is
     * adding or updating.
     */
    @FunctionalInterface
    public interface EventSaver {
        void save(Event event) throws ServiceException;
    }

    private final Event existing;       // null when adding a new event
    private final EventSaver saver;
    private final Runnable onSaved;

    private final JTextField nameField = new JTextField(26);
    private final JTextArea descriptionArea = new JTextArea(4, 26);
    private final JTextField dateField = new JTextField(12);
    private final JTextField timeField = new JTextField(12);
    private final JTextField venueField = new JTextField(26);
    private final JTextField organizerField = new JTextField(26);
    private final JTextField feeField = new JTextField(12);
    private final JTextField capacityField = new JTextField(12);
    private final JComboBox<EventStatus> statusCombo = new JComboBox<>(EventStatus.values());
    private final JButton saveButton = UITheme.primaryButton("Save");

    public EventFormDialog(Window owner, Event existing, EventSaver saver, Runnable onSaved) {
        super(owner, existing == null ? "Add Event" : "Update Event",
                Dialog.ModalityType.APPLICATION_MODAL);
        this.existing = existing;

        this.saver = saver;
        this.onSaved = onSaved;

        UITheme.styleInput(nameField);
        UITheme.styleInput(dateField);
        UITheme.styleInput(timeField);
        UITheme.styleInput(venueField);
        UITheme.styleInput(organizerField);
        UITheme.styleInput(feeField);
        UITheme.styleInput(capacityField);
        UITheme.styleCombo(statusCombo);
        descriptionArea.setFont(UITheme.BODY);
        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);

        fillFields();

        JPanel root = new JPanel(new BorderLayout(0, 16));
        root.setBackground(UITheme.CARD);
        root.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));
        root.add(UITheme.label(existing == null ? "Add a new event" : "Update event #" + existing.getEventId(),
                UITheme.HEADING, UITheme.TEXT), BorderLayout.NORTH);
        root.add(buildForm(), BorderLayout.CENTER);
        root.add(buildButtons(), BorderLayout.SOUTH);

        setContentPane(root);
        getRootPane().setDefaultButton(null); // Enter must not save by accident in the text area
        pack();
        setResizable(false);
        setLocationRelativeTo(owner);
    }


    private void fillFields() {
        if (existing == null) {
            dateField.setText(LocalDate.now().plusDays(7).toString());
            timeField.setText("10:00");
            feeField.setText("0");
            capacityField.setText("50");
            statusCombo.setSelectedItem(EventStatus.OPEN);
        } else {
            nameField.setText(existing.getEventName());
            descriptionArea.setText(existing.getDescription() == null ? "" : existing.getDescription());
            dateField.setText(existing.getEventDate().toString());
            timeField.setText(existing.getStartTime().toString());
            venueField.setText(existing.getVenue());
            organizerField.setText(existing.getOrganizer());
            feeField.setText(existing.getRegistrationFee().toPlainString());
            capacityField.setText(String.valueOf(existing.getMaxCapacity()));
            statusCombo.setSelectedItem(existing.getStatus());
        }
    }

    private JPanel buildForm() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);

        JScrollPane descriptionScroll = new JScrollPane(descriptionArea);
        descriptionScroll.setBorder(BorderFactory.createLineBorder(UITheme.BORDER));

        addRow(form, 0, "Event name", nameField);
        addRow(form, 1, "Description", descriptionScroll);
        addRow(form, 2, "Date (yyyy-MM-dd)", dateField);
        addRow(form, 3, "Start time (HH:mm)", timeField);
        addRow(form, 4, "Venue", venueField);

        addRow(form, 5, "Organizer", organizerField);
        addRow(form, 6, "Registration fee (Rs.)", feeField);
        addRow(form, 7, "Maximum capacity", capacityField);
        addRow(form, 8, "Status", statusCombo);
        return form;
    }

    private void addRow(JPanel form, int row, String labelText, JComponent field) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = row;

        c.gridx = 0;
        c.weightx = 0;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.insets = new Insets(8, 0, 8, 16);
        form.add(UITheme.label(labelText, UITheme.BODY_BOLD, UITheme.TEXT), c);

        c.gridx = 1;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(6, 0, 6, 0);
        form.add(field, c);
    }

    private JPanel buildButtons() {
        JButton cancelButton = UITheme.secondaryButton("Cancel");
        saveButton.addActionListener(e -> handleSave());
        cancelButton.addActionListener(e -> dispose());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.setOpaque(false);
        buttons.add(cancelButton);

        buttons.add(saveButton);
        return buttons;
    }

    // ---------- saving ----------

    private void handleSave() {
        Event event = tryReadForm();
        if (event == null) {
            return; // an error message was already shown
        }
        saveButton.setEnabled(false);
        BackgroundTask.run(this,
                () -> {
                    saver.save(event);      // the service validates and writes to MySQL
                    return event;
                },
                saved -> {
                    dispose();
                    onSaved.run();          // lets the panel reload its table
                },
                () -> saveButton.setEnabled(true));
    }

    private Event tryReadForm() {
        try {
            return readForm();
        } catch (IllegalArgumentException ex) {
            UITheme.showError(this, ex.getMessage());
            return null;
        }
    }


    /**
     * Copies the form into an Event object. The setters inside Event reject some
     * bad values (negative fee, zero capacity) by throwing IllegalArgumentException.
     */
    private Event readForm() {
        Event event = new Event();
        if (existing != null) {
            event.setEventId(existing.getEventId());
        }
        event.setEventName(nameField.getText());
        String description = descriptionArea.getText().trim();
        event.setDescription(description.isEmpty() ? null : description);
        event.setEventDate(parseDate(dateField.getText()));
        event.setStartTime(parseTime(timeField.getText()));
        event.setVenue(venueField.getText());
        event.setOrganizer(organizerField.getText());
        event.setRegistrationFee(parseFee(feeField.getText()));
        event.setMaxCapacity(parseCapacity(capacityField.getText()));
        event.setStatus((EventStatus) statusCombo.getSelectedItem());
        return event;
    }

    private LocalDate parseDate(String text) {
        try {
            return LocalDate.parse(text.trim());
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("Date must look like 2026-11-15 (yyyy-MM-dd).");
        }
    }

    private LocalTime parseTime(String text) {

        try {
            return LocalTime.parse(text.trim());
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("Start time must look like 14:30 (24-hour HH:mm).");
        }
    }

    private BigDecimal parseFee(String text) {
        try {
            return new BigDecimal(text.trim());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Registration fee must be a number, for example 150 or 99.50.");
        }
    }

    private int parseCapacity(String text) {
        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Maximum capacity must be a whole number, for example 50.");
        }
    }
}