package com.college.event.dao;

import com.college.event.model.DashboardStats;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/** SQL for dashboard and statistics numbers. More queries are added in Step 11. */
public class StatisticsDAO {

    /**
     * One query with four nested subqueries, each using the COUNT() aggregate function.
     * The numbers come from MySQL every time, never from hardcoded values.
     */
    public DashboardStats getDashboardStats() throws SQLException {
        String sql = "SELECT "
                + "(SELECT COUNT(*) FROM events) AS total_events, "
                + "(SELECT COUNT(*) FROM students) AS total_students, "
                + "(SELECT COUNT(*) FROM registrations WHERE status = 'REGISTERED') "
                + "    AS total_registrations, "
                + "(SELECT COUNT(*) FROM events "
                + "  WHERE event_date >= CURDATE() AND status IN ('OPEN', 'CLOSED')) "
                + "    AS upcoming_events";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            rs.next();
            return new DashboardStats(

                    rs.getInt("total_events"),
                    rs.getInt("total_students"),
                    rs.getInt("total_registrations"),
                    rs.getInt("upcoming_events"));
        }
    }
}
