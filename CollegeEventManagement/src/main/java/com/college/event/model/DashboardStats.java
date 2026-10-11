package com.college.event.model;

/**
 * The four numbers shown on the admin dashboard.
 * A record is a short way to write a small, unchangeable data-holder class (Java 16+).
 * Java creates the constructor and the getters (totalEvents(), ...) automatically.
 */
public record DashboardStats(int totalEvents, int totalStudents,
                             int totalRegistrations, int upcomingEvents) {
}
