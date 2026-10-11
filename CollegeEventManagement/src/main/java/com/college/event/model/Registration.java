package com.college.event.model;

import java.time.LocalDateTime;

/** One student's registration for one event. Matches the 'registrations' table. */
public class Registration implements Summarizable {

    private int registrationId;
    private int studentId;
    private int eventId;
    private LocalDateTime registrationDate;
    private RegistrationStatus status;
    private LocalDateTime cancelledAt;

    // Extra display fields, filled by JOIN queries so tables can show names, not just ids
    private String studentName;
    private String eventName;

    public Registration() {
        this.status = RegistrationStatus.REGISTERED;
    }

    public Registration(int registrationId, int studentId, int eventId,
                        LocalDateTime registrationDate, RegistrationStatus status) {
        this.registrationId = registrationId;
        this.studentId = studentId;
        this.eventId = eventId;
        this.registrationDate = registrationDate;
        this.status = status;
    }

    /** The id shown to students, for example REG-000007. */
    public String getFormattedId() {
        return String.format("REG-%06d", registrationId);
    }

    public int getRegistrationId() { return registrationId; }
    public void setRegistrationId(int registrationId) { this.registrationId = registrationId; }

    public int getStudentId() { return studentId; }
    public void setStudentId(int studentId) { this.studentId = studentId; }

    public int getEventId() { return eventId; }
    public void setEventId(int eventId) { this.eventId = eventId; }

    public LocalDateTime getRegistrationDate() { return registrationDate; }
    public void setRegistrationDate(LocalDateTime registrationDate) { this.registrationDate = registrationDate; }

    public RegistrationStatus getStatus() { return status; }
    public void setStatus(RegistrationStatus status) { this.status = status; }

    public LocalDateTime getCancelledAt() { return cancelledAt; }
    public void setCancelledAt(LocalDateTime cancelledAt) { this.cancelledAt = cancelledAt; }

    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }

    public String getEventName() { return eventName; }
    public void setEventName(String eventName) { this.eventName = eventName; }

    @Override
    public String getSummary() {
        return getFormattedId() + " (" + status + ")";
    }

    @Override
    public String toString() {
        return "Registration[" + getSummary() + ", student " + studentId + ", event " + eventId + "]";
    }
}