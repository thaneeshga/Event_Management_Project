package com.college.event.dao;

import com.college.event.model.Event;
import com.college.event.model.EventStatus;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object for events.
 * Reads use the view v_event_summary, which already contains the number of
 * registered students. Writes go to the events table.
 */
public class EventDAO {

    private static final String SELECT_EVENTS =
            "SELECT event_id, event_name, description, event_date, start_time, venue, "
                    + "organizer, registration_fee, max_capacity, status, registered_count "
                    + "FROM v_event_summary ";

    // ---------- CREATE ----------

    public int insert(Event event) throws SQLException {

        String sql = "INSERT INTO events (event_name, description, event_date, start_time, "
                + "venue, organizer, registration_fee, max_capacity, status) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            bindEventFields(ps, event);
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    int id = keys.getInt(1);
                    event.setEventId(id);
                    return id;
                }
            }
            throw new SQLException("Event was inserted but no id was returned.");
        }
    }

    // ---------- READ ----------

    public List<Event> findAll() throws SQLException {
        return query(SELECT_EVENTS + "ORDER BY event_date, start_time");
    }

    public Optional<Event> findById(int eventId) throws SQLException {
        List<Event> result = query(SELECT_EVENTS + "WHERE event_id = ?", eventId);
        return result.isEmpty() ? Optional.empty() : Optional.of(result.get(0));
    }


    /** Matches events whose name contains the keyword, or whose id equals the keyword. */
    public List<Event> search(String keyword) throws SQLException {
        String trimmed = keyword.trim();
        String like = "%" + escapeLike(trimmed) + "%";
        int id = -1;
        try {
            id = Integer.parseInt(trimmed);
        } catch (NumberFormatException ignored) {
            // keyword is not a number, so only the name search applies
        }
        return query(SELECT_EVENTS + "WHERE event_name LIKE ? OR event_id = ? "
                + "ORDER BY event_date, start_time", like, id);
    }

    /** Events between two dates, inclusive. For a single day, use the same date twice. */
    public List<Event> findByDateRange(LocalDate from, LocalDate to) throws SQLException {
        return query(SELECT_EVENTS + "WHERE event_date BETWEEN ? AND ? "
                + "ORDER BY event_date, start_time", Date.valueOf(from), Date.valueOf(to));
    }

    // ---------- UPDATE ----------

    /** @return number of rows changed (0 means the event id does not exist) */
    public int update(Event event) throws SQLException {
        String sql = "UPDATE events SET event_name = ?, description = ?, event_date = ?, "
                + "start_time = ?, venue = ?, organizer = ?, registration_fee = ?, "
                + "max_capacity = ?, status = ? WHERE event_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            bindEventFields(ps, event);
            ps.setInt(10, event.getEventId());

            return ps.executeUpdate();
        }
    }

    /** Marks OPEN/CLOSED events whose date has passed as COMPLETED. */
    public int autoCompletePastEvents() throws SQLException {
        String sql = "UPDATE events SET status = 'COMPLETED' "
                + "WHERE event_date < CURDATE() AND status IN ('OPEN', 'CLOSED')";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            return ps.executeUpdate();
        }
    }

    // ---------- DELETE ----------

    /**
     * Deletes an event. If students have registered, the foreign key rule
     * ON DELETE RESTRICT makes MySQL refuse, and an SQLException is thrown.
     */
    public int delete(int eventId) throws SQLException {
        String sql = "DELETE FROM events WHERE event_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, eventId);
            return ps.executeUpdate();
        }
    }

    // ---------- helpers ----------

    /** Fills the nine event placeholders (shared by insert and update). */

    private void bindEventFields(PreparedStatement ps, Event event) throws SQLException {
        ps.setString(1, event.getEventName().trim());
        ps.setString(2, event.getDescription());
        ps.setDate(3, Date.valueOf(event.getEventDate()));
        ps.setTime(4, Time.valueOf(event.getStartTime()));
        ps.setString(5, event.getVenue().trim());
        ps.setString(6, event.getOrganizer().trim());
        ps.setBigDecimal(7, event.getRegistrationFee());
        ps.setInt(8, event.getMaxCapacity());
        ps.setString(9, event.getStatus().name());
    }

    /** Runs a SELECT with any number of ? parameters and returns the events found. */
    private List<Event> query(String sql, Object... params) throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                List<Event> events = new ArrayList<>();
                while (rs.next()) {
                    events.add(mapRow(rs));
                }
                return events;
            }
        }
    }

    /** Converts the current ResultSet row into an Event object. */
    private Event mapRow(ResultSet rs) throws SQLException {

        Event event = new Event(
                rs.getInt("event_id"),
                rs.getString("event_name"),
                rs.getString("description"),
                rs.getDate("event_date").toLocalDate(),
                rs.getTime("start_time").toLocalTime(),
                rs.getString("venue"),
                rs.getString("organizer"),
                rs.getBigDecimal("registration_fee"),
                rs.getInt("max_capacity"),
                EventStatus.valueOf(rs.getString("status")));
        event.setRegisteredCount(rs.getInt("registered_count"));
        return event;
    }

    /** Stops the characters % and _ typed by a user from acting as wildcards. */
    private String escapeLike(String text) {
        return text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}