package com.college.event.model;

/**
 * Interface: any class that can describe itself in one line.
 * Person, Event and Registration all promise to provide getSummary().
 * This lets the UI show a short description without knowing the exact class.
 */
public interface Summarizable {
    String getSummary();
}
