package com.college.event.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

/** One college event. Matches the 'events' table (plus a registeredCount from the view). */
public class Event implements Summarizable {

    private int eventId;
    private String eventName;
    private String description;
    private LocalDate eventDate;
    private LocalTime startTime;
    private String venue;
    private String organizer;
    private BigDecimal registrationFee; // BigDecimal is the correct type for money
    private int maxCapacity;
    private EventStatus status;
    private int registeredCount;        // filled from v_event_summary, not stored in events

    /** Empty event, used by forms before the user fills in the fields. */
    public Event() {
        this.registrationFee = BigDecimal.ZERO;
        this.status = EventStatus.OPEN;
    }

    public Event(int eventId, String eventName, String description, LocalDate eventDate,
                 LocalTime startTime, String venue, String organizer,
                 BigDecimal registrationFee, int maxCapacity, EventStatus status) {
        this.eventId = eventId;
        this.eventName = eventName;
        this.description = description;
        this.eventDate = eventDate;
        this.startTime = startTime;
        this.venue = venue;
        this.organizer = organizer;
        setRegistrationFee(registrationFee); // setters check the values
        setMaxCapacity(maxCapacity);
        this.status = status;
    }

    public int getEventId() { return eventId; }
    public void setEventId(int eventId) { this.eventId = eventId; }

    public String getEventName() { return eventName; }
    public void setEventName(String eventName) { this.eventName = eventName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDate getEventDate() { return eventDate; }
    public void setEventDate(LocalDate eventDate) { this.eventDate = eventDate; }

    public LocalTime getStartTime() { return startTime; }
    public void setStartTime(LocalTime startTime) { this.startTime = startTime; }

    public String getVenue() { return venue; }
    public void setVenue(String venue) { this.venue = venue; }

    public String getOrganizer() { return organizer; }
    public void setOrganizer(String organizer) { this.organizer = organizer; }

    public BigDecimal getRegistrationFee() { return registrationFee; }

    /** Throws an exception for a negative or missing fee. */
    public void setRegistrationFee(BigDecimal registrationFee) {
        if (registrationFee == null || registrationFee.signum() < 0) {
            throw new IllegalArgumentException("Registration fee cannot be negative");
        }
        this.registrationFee = registrationFee;
    }

    public int getMaxCapacity() { return maxCapacity; }

    /** Throws an exception for zero or negative capacity. */
    public void setMaxCapacity(int maxCapacity) {
        if (maxCapacity <= 0) {
            throw new IllegalArgumentException("Maximum capacity must be greater than zero");
        }
        this.maxCapacity = maxCapacity;
    }

    public EventStatus getStatus() { return status; }
    public void setStatus(EventStatus status) { this.status = status; }

    public int getRegisteredCount() { return registeredCount; }

    public void setRegisteredCount(int registeredCount) {
        if (registeredCount < 0) {
            throw new IllegalArgumentException("Registered count cannot be negative");
        }
        this.registeredCount = registeredCount;
    }

    /** Seats left = capacity minus registered students. Calculated, never stored. */
    public int getAvailableSeats() {
        return maxCapacity - registeredCount;
    }

    /** True if the event is today or later and has not been cancelled or completed. */
    public boolean isUpcoming() {
        return eventDate != null
                && !eventDate.isBefore(LocalDate.now())
                && (status == EventStatus.OPEN || status == EventStatus.CLOSED);
    }

    /** Quick check used by the UI. The real seat check happens inside a transaction in Step 8. */
    public boolean isRegistrationOpen() {
        return status == EventStatus.OPEN && isUpcoming() && getAvailableSeats() > 0;
    }

    @Override
    public String getSummary() {
        return eventName + " on " + eventDate + " at " + venue;
    }

    @Override
    public String toString() {
        return "Event[" + eventId + ", " + getSummary() + ", " + status
                + ", seats left: " + getAvailableSeats() + "]";
    }
}