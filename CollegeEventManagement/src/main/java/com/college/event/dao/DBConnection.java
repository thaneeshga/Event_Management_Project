package com.college.event.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Creates connections to the MySQL database using JDBC.
 *
 * Credentials come from Windows environment variables, never from the source code:
 *   DB_USER      (required)  e.g. event_app
 *   DB_PASSWORD  (required)
 *   DB_URL       (optional)  only needed if your MySQL is not on localhost:3306
 *
 * Design choice: every call opens a NEW connection and the caller closes it with
 * try-with-resources. This is simple and safe for a college project. (Large
 * applications use a "connection pool" instead.)
 */
public final class DBConnection {

    private static final String DEFAULT_URL =
            "jdbc:mysql://localhost:3306/college_event_db?useSSL=false&allowPublicKeyRetrieval=true";

    // Private constructor: this class only has static methods, so nobody creates objects of it
    private DBConnection() {
    }

    /**
     * Opens a connection to college_event_db.
     * The caller must close it, normally with try-with-resources.
     *
     * @throws SQLException if the credentials are missing or MySQL cannot be reached
     */
    public static Connection getConnection() throws SQLException {
        String url = readEnv("DB_URL", DEFAULT_URL);
        String user = System.getenv("DB_USER");
        String password = System.getenv("DB_PASSWORD");

        if (user == null || user.isBlank() || password == null) {
            throw new SQLException(
                    "Database credentials not found. Set the DB_USER and DB_PASSWORD "
                            + "environment variables, then restart VS Code.");
        }

        // The MySQL driver registers itself automatically (JDBC 4), so no Class.forName() is needed
        return DriverManager.getConnection(url, user, password);
    }

    /** Reads an environment variable, or returns the fallback if it is not set. */
    private static String readEnv(String name, String fallback) {
        String value = System.getenv(name);
        return (value == null || value.isBlank()) ? fallback : value;
    }
}
