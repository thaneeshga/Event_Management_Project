package com.college.event.service;

import com.college.event.dao.DBConnection;
import com.college.event.dao.RegistrationDAO;
import com.college.event.dao.RegistrationDAO.CancelInfo;
import com.college.event.dao.RegistrationDAO.EventLock;
import com.college.event.dao.RegistrationDAO.PaymentInfo;
import com.college.event.model.EventStatus;
import com.college.event.model.Person;
import com.college.event.model.Registration;
import com.college.event.model.RegistrationDetail;
import com.college.event.model.RegistrationStatus;
import com.college.event.model.Role;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Registering and cancelling are TRANSACTIONS: several SQL statements that must
 * either all succeed (commit) or all be undone (rollback).
 *
 * The pattern used in both methods:
 *   setAutoCommit(false)  ->  do the work  ->  commit()
 *   if anything fails     ->  rollback()
 */
public class RegistrationService {

    private static final String ALREADY_REGISTERED = "You are already registered for this event.";


    private final RegistrationDAO registrationDAO = new RegistrationDAO();

    // ============================ REGISTER ============================

    /** Registers the logged-in student for an event and returns the new registration. */
    public Registration registerForEvent(Person actor, int eventId) throws ServiceException {
        AuthenticationService.requireStudent(actor);

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false); // start the transaction
            // READ COMMITTED: every query sees the latest committed data
            conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
            try {
                Registration registration = doRegister(conn, actor.getId(), eventId);
                conn.commit();         // make everything permanent together
                return registration;
            } catch (ServiceException | SQLException ex) {
                conn.rollback();       // undo everything done in this transaction
                throw ex;
            }
            // If an unexpected RuntimeException happens, the connection closes and
            // MySQL automatically discards the uncommitted work.

        } catch (SQLIntegrityConstraintViolationException e) {
            // Safety net: the UNIQUE (student_id, event_id) constraint caught a duplicate
            throw new ServiceException(ALREADY_REGISTERED, e);
        } catch (SQLException e) {
            throw new ServiceException("Could not register because of a database error: "
                    + e.getMessage(), e);
        }
    }


    /** The checks and writes that happen inside one transaction. */
    private Registration doRegister(Connection conn, int studentId, int eventId)
            throws ServiceException, SQLException {

        // 1. Verify the event exists. This also LOCKS its row, so other students
        //    registering for the same event now wait for us.
        EventLock event = registrationDAO.lockEvent(conn, eventId)
                .orElseThrow(() -> new ServiceException("This event does not exist."));

        // 2. Verify registration is open
        if (event.status() != EventStatus.OPEN) {
            throw new ServiceException("Registration is closed for this event.");
        }
        if (event.eventDate().isBefore(LocalDate.now())) {
            throw new ServiceException("This event has already taken place.");
        }

        // 3. Prevent duplicate registration
        Optional<Registration> existing = registrationDAO.findExisting(conn, studentId, eventId);
        if (existing.isPresent() && existing.get().getStatus() == RegistrationStatus.REGISTERED) {
            throw new ServiceException(ALREADY_REGISTERED);
        }

        // 4. Verify seats are available (safe, because the event row is locked)
        int seatsTaken = registrationDAO.countRegistered(conn, eventId);
        if (seatsTaken >= event.maxCapacity()) {
            throw new ServiceException("Sorry, this event is full. No seats are available.");
        }

        // 5. Save the registration
        int registrationId;

        if (existing.isPresent()) {
            registrationId = existing.get().getRegistrationId();
            registrationDAO.reactivate(conn, registrationId); // earlier cancelled row
        } else {
            registrationId = registrationDAO.insert(conn, studentId, eventId);
        }

        return new Registration(registrationId, studentId, eventId,
                LocalDateTime.now(), RegistrationStatus.REGISTERED);
    }

    // ============================ CANCEL ============================

    /**
     * Students may cancel only their own registrations, and only before the day of
     * the event. Administrators may cancel any registration.
     */
    public void cancelRegistration(Person actor, int registrationId) throws ServiceException {
        if (actor == null) {
            throw new ServiceException("Access denied: please log in first.");
        }

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
            try {
                doCancel(conn, actor, registrationId);
                conn.commit();
            } catch (ServiceException | SQLException ex) {
                conn.rollback();
                throw ex;
            }

        } catch (SQLException e) {
            throw new ServiceException("Could not cancel because of a database error: "
                    + e.getMessage(), e);
        }
    }

    private void doCancel(Connection conn, Person actor, int registrationId)
            throws ServiceException, SQLException {

        CancelInfo info = registrationDAO.lockForCancel(conn, registrationId)
                .orElseThrow(() -> new ServiceException("Registration not found."));

        boolean isAdmin = actor.getRole() == Role.ADMIN;

        // A student must not learn that other students' registrations exist
        if (!isAdmin && info.studentId() != actor.getId()) {
            throw new ServiceException("Registration not found.");
        }
        if (info.status() == RegistrationStatus.CANCELLED) {
            throw new ServiceException("This registration is already cancelled.");
        }
        if (!isAdmin && !info.eventDate().isAfter(LocalDate.now())) {
            throw new ServiceException(
                    "Registrations can only be cancelled before the day of the event.");
        }
        registrationDAO.markCancelled(conn, registrationId);
    }

    // ============================ VIEW ============================

    /** The logged-in student's own registrations. */
    public List<RegistrationDetail> getMyRegistrations(Person student) throws ServiceException {

        AuthenticationService.requireStudent(student);
        try {
            return registrationDAO.findDetailsByStudent(student.getId());
        } catch (SQLException e) {
            throw new ServiceException("Could not load your registrations: " + e.getMessage(), e);
        }
    }

    public List<RegistrationDetail> getAllRegistrations(Person admin) throws ServiceException {
        AuthenticationService.requireAdmin(admin);
        try {
            return registrationDAO.findAllDetails();
        } catch (SQLException e) {
            throw new ServiceException("Could not load registrations: " + e.getMessage(), e);
        }
    }

    public List<RegistrationDetail> getRegistrationsForEvent(Person admin, int eventId)
            throws ServiceException {
        AuthenticationService.requireAdmin(admin);
        try {
            return registrationDAO.findDetailsByEvent(eventId);
        } catch (SQLException e) {
            throw new ServiceException("Could not load registrations: " + e.getMessage(), e);
        }
    }

    /** Used by Manage Students: the events a particular student registered for. */
    public List<RegistrationDetail> getRegistrationsForStudent(Person admin, int studentId)
            throws ServiceException {
        AuthenticationService.requireAdmin(admin);
        try {

            return registrationDAO.findDetailsByStudent(studentId);
        } catch (SQLException e) {
            throw new ServiceException("Could not load registrations: " + e.getMessage(), e);
        }
    }
    
    // ============================ PAYMENT AND REMOVAL ============================

    /** A student records the total fee they have paid for one of their registrations. */
    public void updateFeePaid(Person actor, int registrationId, BigDecimal amount)
            throws ServiceException {
        AuthenticationService.requireStudent(actor);
        if (amount == null || amount.signum() < 0 || amount.stripTrailingZeros().scale() > 2) {
            throw new ServiceException("Enter an amount of 0 or more with at most 2 decimals.");
        }

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
            try {
                doUpdateFeePaid(conn, actor.getId(), registrationId, amount);
                conn.commit();
            } catch (ServiceException | SQLException ex) {
                conn.rollback();
                throw ex;
            }
        } catch (SQLException e) {
            throw new ServiceException("Could not save the payment: " + e.getMessage(), e);
        }
    }

    private void doUpdateFeePaid(Connection conn, int studentId, int registrationId,
                                 BigDecimal amount) throws ServiceException, SQLException {
        PaymentInfo info = registrationDAO.lockForPayment(conn, registrationId)
                .filter(i -> i.studentId() == studentId)   // must be the student's own row
                .orElseThrow(() -> new ServiceException("Registration not found."));


        if (info.status() != RegistrationStatus.REGISTERED) {
            throw new ServiceException("The fee can only be updated for an active registration.");
        }
        if (info.eventFee().signum() == 0) {
            throw new ServiceException("This event is free, so there is no fee to pay.");
        }
        if (amount.compareTo(info.eventFee()) > 0) {
            throw new ServiceException("The amount paid cannot be more than the event fee ("
                    + info.eventFee().toPlainString() + ").");
        }
        registrationDAO.updateFeePaid(conn, registrationId, amount);
    }

    /** A student permanently removes one of their CANCELLED registrations. */
    public void removeCancelledRegistration(Person actor, int registrationId)
            throws ServiceException {
        AuthenticationService.requireStudent(actor);

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
            try {
                int studentId = actor.getId();
                CancelInfo info = registrationDAO.lockForCancel(conn, registrationId)
                        .filter(i -> i.studentId() == studentId)
                        .orElseThrow(() -> new ServiceException("Registration not found."));
                if (info.status() != RegistrationStatus.CANCELLED) {
                    throw new ServiceException(
                            "Only a cancelled registration can be removed. Cancel it first.");
                }
                registrationDAO.deleteRegistration(conn, registrationId);
                conn.commit();

            } catch (ServiceException | SQLException ex) {
                conn.rollback();
                throw ex;
            }
        } catch (SQLException e) {
            throw new ServiceException("Could not remove the registration: " + e.getMessage(), e);
        }
    }
}
