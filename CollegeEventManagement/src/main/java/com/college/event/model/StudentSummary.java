package com.college.event.model;

/** A student together with the number of events they are currently registered for. */
public record StudentSummary(Student student, int activeRegistrations) {
}
