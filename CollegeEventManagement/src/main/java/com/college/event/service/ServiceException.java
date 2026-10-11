package com.college.event.service;

/**
 * Custom checked exception. The service layer throws it with a message that is
 * safe to show directly to the user, so the UI only needs to display getMessage().
 */
public class ServiceException extends Exception {

    public ServiceException(String message) {
        super(message);
    }

    public ServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
