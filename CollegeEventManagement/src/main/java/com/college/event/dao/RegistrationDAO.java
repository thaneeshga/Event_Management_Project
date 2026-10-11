package com.college.event.dao;

import com.college.event.model.EventStatus;
import com.college.event.model.Registration;
import com.college.event.model.RegistrationDetail;
import com.college.event.model.RegistrationStatus;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * All SQL for registrations.
 *
 * Two groups of methods:
 *  1. "Transactional" methods receive a Connection from the service, so that
 *     several statements can succeed or fail together (commit / rollback).
 *  2. "Read" methods open their own connection, because they are single queries.
 */
public class RegistrationDAO {

    // A record is a short way to write a small data-holder class (Java 16+).

    /** Facts about an event, read while its row is locked. */
    public record EventLock(EventStatus status, LocalDate eventDate, int maxCapacity) {
    }

    /** Facts about a registration, read while it is being cancelled. */
    public record CancelInfo(int studentId, RegistrationStatus status, LocalDate eventDate) {
    }

    // ================= transactional methods =================

    /**
     * Locks the event row. "FOR UPDATE" makes any other transaction that wants the
     * same event wait here until we commit or roll back. This is what prevents
     * two students from taking the last seat at the same time.
     */
    public Optional<EventLock> lockEvent(Connection conn, int eventId) throws SQLException {
        String sql = "SELECT status, event_date, max_capacity FROM events "
                + "WHERE event_id = ? FOR UPDATE";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, eventId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new EventLock(
                            EventStatus.valueOf(rs.getString("status")),
                            rs.getDate("event_date").toLocalDate(),
                            rs.getInt("max_capacity")));
                }
                return Optional.empty();
            }
        }
    }

    /** Number of seats currently taken (cancelled registrations do not count). */
    public int countRegistered(Connection conn, int eventId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM registrations WHERE event_id = ? AND status = 'REGISTERED'";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, eventId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    /** Finds this student's existing row for this event (registered OR cancelled). */
    public Optional<Registration> findExisting(Connection conn, int studentId, int eventId)
            throws SQLException {
        String sql = "SELECT registration_id, status FROM registrations "
                + "WHERE student_id = ? AND event_id = ? FOR UPDATE";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            ps.setInt(2, eventId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new Registration(
                            rs.getInt("registration_id"), studentId, eventId, null,
                            RegistrationStatus.valueOf(rs.getString("status"))));
                }
                return Optional.empty();
            }
        }
    }

    /** Inserts a new registration. Status and date use the column defaults. */
    public int insert(Connection conn, int studentId, int eventId) throws SQLException {
        String sql = "INSERT INTO registrations (student_id, event_id) VALUES (?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, studentId);
            ps.setInt(2, eventId);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
            throw new SQLException("Registration was inserted but no id was returned.");
        }
    }

    /**
     * A student who cancelled and registers again re-uses the same row, because
     * UNIQUE (student_id, event_id) allows only one row per student and event.
     */
    public void reactivate(Connection conn, int registrationId) throws SQLException {
            String sql = "UPDATE registrations SET status = 'REGISTERED', fee_paid = 0.00, "
                + "registration_date = NOW(), cancelled_at = NULL WHERE registration_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, registrationId);
            ps.executeUpdate();
        }
    }

    /** Locks one registration row (and reads its event date) before cancelling it. */
    public Optional<CancelInfo> lockForCancel(Connection conn, int registrationId)
            throws SQLException {
        String sql = "SELECT r.student_id, r.status, e.event_date "
                + "FROM registrations r INNER JOIN events e ON e.event_id = r.event_id "
                + "WHERE r.registration_id = ? FOR UPDATE OF r";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, registrationId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new CancelInfo(
                            rs.getInt("student_id"),
                            RegistrationStatus.valueOf(rs.getString("status")),
                            rs.getDate("event_date").toLocalDate()));
                }
                return Optional.empty();
            }
        }
    }

    public void markCancelled(Connection conn, int registrationId) throws SQLException {
        String sql = "UPDATE registrations SET status = 'CANCELLED', cancelled_at = NOW() "
                + "WHERE registration_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, registrationId);
            ps.executeUpdate();
        }
    }

    // ================= read methods (own connection) =================

    private static final String SELECT_DETAILS =
            "SELECT registration_id, registration_date, registration_status, fee_paid, student_id, "
                    + "student_name, student_email, department, event_id, event_name, "
                    + "event_date, venue, registration_fee FROM v_registration_details ";

    public List<RegistrationDetail> findDetailsByStudent(int studentId) throws SQLException {
        return queryDetails(SELECT_DETAILS + "WHERE student_id = ? "
                + "ORDER BY registration_date DESC", studentId);
    }

    public List<RegistrationDetail> findDetailsByEvent(int eventId) throws SQLException {
        return queryDetails(SELECT_DETAILS + "WHERE event_id = ? "
                + "ORDER BY registration_date DESC", eventId);
    }

    public List<RegistrationDetail> findAllDetails() throws SQLException {
        return queryDetails(SELECT_DETAILS + "ORDER BY registration_date DESC");
    }

    private List<RegistrationDetail> queryDetails(String sql, Object... params)
            throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                List<RegistrationDetail> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(new RegistrationDetail(
                            rs.getInt("registration_id"),
                            rs.getTimestamp("registration_date").toLocalDateTime(),
                            RegistrationStatus.valueOf(rs.getString("registration_status")),
                            rs.getBigDecimal("fee_paid"),
                            rs.getInt("student_id"),
                            rs.getString("student_name"),
                            rs.getString("student_email"),
                            rs.getString("department"),
                            rs.getInt("event_id"),
                            rs.getString("event_name"),
                            rs.getDate("event_date").toLocalDate(),
                            rs.getString("venue"),
                            rs.getBigDecimal("registration_fee")));
                }
                return list;
            }
        }
    }
    
    // ================= payment and removal =================

    /** Facts needed to record a payment while the registration row is locked. */
    public record PaymentInfo(int studentId, RegistrationStatus status, BigDecimal eventFee) {
    }

    public Optional<PaymentInfo> lockForPayment(Connection conn, int registrationId)
            throws SQLException {
        String sql = "SELECT r.student_id, r.status, e.registration_fee "
                + "FROM registrations r INNER JOIN events e ON e.event_id = r.event_id "
                + "WHERE r.registration_id = ? FOR UPDATE OF r";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, registrationId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new PaymentInfo(
                            rs.getInt("student_id"),
                            RegistrationStatus.valueOf(rs.getString("status")),
                            rs.getBigDecimal("registration_fee")));
                }
                return Optional.empty();
            }
        }
    }

    public void updateFeePaid(Connection conn, int registrationId, BigDecimal amount)
            throws SQLException {
        String sql = "UPDATE registrations SET fee_paid = ? WHERE registration_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBigDecimal(1, amount);
            ps.setInt(2, registrationId);
            ps.executeUpdate();
        }
    }

    /** Deletes the row. The AND status condition is a second safety check. */
    public void deleteRegistration(Connection conn, int registrationId) throws SQLException {
        String sql = "DELETE FROM registrations WHERE registration_id = ? AND status = 'CANCELLED'";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, registrationId);
            ps.executeUpdate();
        }
    }
}