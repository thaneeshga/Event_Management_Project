package com.college.event.service;

import com.college.event.dao.EventDAO;
import com.college.event.model.Event;
import com.college.event.model.Person;
import com.college.event.util.EventValidator;
import com.college.event.util.ValidationUtil;

import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Business rules for events. Admin-only operations check the user's role first,
 * then validate, then call the DAO.
 */
public class EventService {

    private static final int MYSQL_FOREIGN_KEY_BLOCKS_DELETE = 1451;

    private final EventDAO eventDAO = new EventDAO();

    // ---------- admin operations ----------

    /** Adds a new event and returns its new id. */
    public int addEvent(Person actor, Event event) throws ServiceException {
        AuthenticationService.requireAdmin(actor);

        List<String> errors = EventValidator.validate(event, false);
        if (!errors.isEmpty()) {
            throw new ServiceException(String.join("\n", errors));
        }
        try {
            return eventDAO.insert(event);
        } catch (SQLException e) {
            throw new ServiceException("Could not save the event: " + e.getMessage(), e);
        }
    }

    public void updateEvent(Person actor, Event event) throws ServiceException {
        AuthenticationService.requireAdmin(actor);

        try {
            Event existing = eventDAO.findById(event.getEventId())
                    .orElseThrow(() -> new ServiceException("Event not found."));

            // A past date is only acceptable if the date was not changed
            boolean dateUnchanged = existing.getEventDate().equals(event.getEventDate());
            List<String> errors = EventValidator.validate(event, dateUnchanged);

            // Capacity cannot drop below the students who already registered
            if (event.getMaxCapacity() < existing.getRegisteredCount()) {
                errors.add("Capacity cannot be less than the current registrations ("
                        + existing.getRegisteredCount() + ").");
            }
            if (!errors.isEmpty()) {
                throw new ServiceException(String.join("\n", errors));
            }
            eventDAO.update(event);

        } catch (SQLException e) {
            throw new ServiceException("Could not update the event: " + e.getMessage(), e);
        }
    }

    public void deleteEvent(Person actor, int eventId) throws ServiceException {
        AuthenticationService.requireAdmin(actor);

        try {
            if (eventDAO.delete(eventId) == 0) {
                throw new ServiceException("Event not found.");
            }
        } catch (SQLIntegrityConstraintViolationException e) {
            throw new ServiceException("This event has registrations and cannot be deleted. "
                    + "Change its status to CANCELLED instead.", e);
        } catch (SQLException e) {
            if (e.getErrorCode() == MYSQL_FOREIGN_KEY_BLOCKS_DELETE) {
                throw new ServiceException("This event has registrations and cannot be deleted. "
                        + "Change its status to CANCELLED instead.", e);
            }
            throw new ServiceException("Could not delete the event: " + e.getMessage(), e);
        }
    }

    // ---------- read operations (admins and students) ----------

    public List<Event> getAllEvents() throws ServiceException {
        try {
            return eventDAO.findAll();
        } catch (SQLException e) {
            throw new ServiceException("Could not load events: " + e.getMessage(), e);
        }
    }

    public Event getEventById(int eventId) throws ServiceException {
        try {
            return eventDAO.findById(eventId)
                    .orElseThrow(() -> new ServiceException("Event not found."));
        } catch (SQLException e) {
            throw new ServiceException("Could not load the event: " + e.getMessage(), e);
        }
    }

    /** Search by name (partial match) or by exact event id. */
    public List<Event> searchEvents(String keyword) throws ServiceException {
        if (ValidationUtil.isBlank(keyword)) {
            return getAllEvents();
        }
        try {
            return eventDAO.search(keyword);
        } catch (SQLException e) {
            throw new ServiceException("Could not search events: " + e.getMessage(), e);
        }
    }

    public List<Event> filterByDateRange(LocalDate from, LocalDate to) throws ServiceException {
        if (from == null || to == null) {
            throw new ServiceException("Please choose both a From date and a To date.");
        }
        if (from.isAfter(to)) {
            throw new ServiceException("The From date cannot be after the To date.");
        }
        try {
            return eventDAO.findByDateRange(from, to);
        } catch (SQLException e) {
            throw new ServiceException("Could not filter events: " + e.getMessage(), e);
        }
    }

    /** Upcoming events, soonest first. Uses the Stream API to filter and sort. */
    public List<Event> getUpcomingEvents() throws ServiceException {
        return getAllEvents().stream()
                .filter(Event::isUpcoming)
                .sorted(Comparator.comparing(Event::getEventDate)
                        .thenComparing(Event::getStartTime))
                .collect(Collectors.toList());
    }

    /** Housekeeping: marks events whose date has passed as COMPLETED. */
    public int autoCompletePastEvents() throws ServiceException {
        try {
            return eventDAO.autoCompletePastEvents();
        } catch (SQLException e) {
            throw new ServiceException("Could not update event statuses: " + e.getMessage(), e);
        }
    }
}