package com.college.event.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * A read-only snapshot of one registration together with its student and event
 * details, filled from the view v_registration_details. All fields are final,
 * so the object cannot change after it is created (an immutable class).
 */
public class RegistrationDetail implements Summarizable {

    private final int registrationId;
    private final LocalDateTime registrationDate;
    private final RegistrationStatus status;
    private final BigDecimal feePaid;
    private final int studentId;
    private final String studentName;
    private final String studentEmail;
    private final String department;
    private final int eventId;
    private final String eventName;
    private final LocalDate eventDate;
    private final String venue;
    private final BigDecimal registrationFee;

    public RegistrationDetail(int registrationId, LocalDateTime registrationDate,
            RegistrationStatus status, BigDecimal feePaid, int studentId, String studentName,
            String studentEmail, String department, int eventId, String eventName,
            LocalDate eventDate, String venue, BigDecimal registrationFee) {
        this.registrationId = registrationId;

        this.registrationDate = registrationDate;
        this.status = status;
        this.feePaid = feePaid;
        this.studentId = studentId;
        this.studentName = studentName;
        this.studentEmail = studentEmail;
        this.department = department;
        this.eventId = eventId;
        this.eventName = eventName;
        this.eventDate = eventDate;
        this.venue = venue;
        this.registrationFee = registrationFee;
    }

    public int getRegistrationId() { return registrationId; }
    public LocalDateTime getRegistrationDate() { return registrationDate; }
    public RegistrationStatus getStatus() { return status; }
    public BigDecimal getFeePaid() { return feePaid; }
    public int getStudentId() { return studentId; }
    public String getStudentName() { return studentName; }
    public String getStudentEmail() { return studentEmail; }
    public String getDepartment() { return department; }
    public int getEventId() { return eventId; }
    public String getEventName() { return eventName; }
    public LocalDate getEventDate() { return eventDate; }
    public String getVenue() { return venue; }
    public BigDecimal getRegistrationFee() { return registrationFee; }

    /** The id shown to people, for example REG-000007. */
    public String getFormattedId() {
        return String.format("REG-%06d", registrationId);
    }


    /** True if a student may still cancel: registered, and the event is after today. */
    public boolean isCancellable() {
        return status == RegistrationStatus.REGISTERED && eventDate.isAfter(LocalDate.now());
    }

    /** Free, Unpaid, Partial or Paid ("-" for a cancelled registration). */
    public String getPaymentStatus() {
        if (status == RegistrationStatus.CANCELLED) {
            return "-";
        }
        if (registrationFee.signum() == 0) {
            return "Free";
        }
        if (feePaid.signum() == 0) {
            return "Unpaid";
        }
        return feePaid.compareTo(registrationFee) < 0 ? "Partial" : "Paid";
    }

    @Override
    public String getSummary() {
        return getFormattedId() + " - " + eventName + " (" + status + ")";
    }

    @Override
    public String toString() {
        return getSummary() + " for " + studentName;
    }
}