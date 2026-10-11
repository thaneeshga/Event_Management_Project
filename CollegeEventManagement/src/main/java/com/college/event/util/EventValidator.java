package com.college.event.util;

import com.college.event.model.Event;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Checks every field of an Event before it is saved. Returns a list of problems. */
public final class EventValidator {

    private static final BigDecimal MAX_FEE = new BigDecimal("999999.99");
    private static final int MAX_CAPACITY = 100_000;

    private EventValidator() {
    }

    /**
     * @param allowPastDate true when updating an event whose date is not being changed
     *                      (old, completed events keep their old date)
     * @return list of error messages; an empty list means the event is valid
     */
    public static List<String> validate(Event event, boolean allowPastDate) {
        List<String> errors = new ArrayList<>();
        if (event == null) {
            errors.add("Event details are missing.");
            return errors;
        }

        String name = event.getEventName();
        if (ValidationUtil.isBlank(name) || name.trim().length() < 2 || name.trim().length() > 150) {

            errors.add("Event name must be 2-150 characters.");
        }

        String description = event.getDescription();
        if (description != null && description.length() > 1000) {
            errors.add("Description cannot exceed 1000 characters.");
        }

        if (event.getEventDate() == null) {
            errors.add("Event date is required.");
        } else if (!allowPastDate && event.getEventDate().isBefore(LocalDate.now())) {
            errors.add("Event date cannot be in the past.");
        }

        if (event.getStartTime() == null) {
            errors.add("Start time is required.");
        }

        String venue = event.getVenue();
        if (ValidationUtil.isBlank(venue) || venue.trim().length() > 150) {
            errors.add("Venue is required (max 150 characters).");
        }

        String organizer = event.getOrganizer();
        if (ValidationUtil.isBlank(organizer) || organizer.trim().length() > 100) {
            errors.add("Organizer is required (max 100 characters).");
        }

        BigDecimal fee = event.getRegistrationFee();
        if (fee == null || fee.signum() < 0 || fee.compareTo(MAX_FEE) > 0
                || fee.stripTrailingZeros().scale() > 2) {
            errors.add("Registration fee must be between 0 and 999999.99 with at most 2 decimals.");

        }

        if (event.getMaxCapacity() < 1 || event.getMaxCapacity() > MAX_CAPACITY) {
            errors.add("Maximum capacity must be between 1 and 100000.");
        }

        if (event.getStatus() == null) {
            errors.add("Event status is required.");
        }
        return errors;
    }
}
