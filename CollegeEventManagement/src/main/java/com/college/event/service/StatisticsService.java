package com.college.event.service;

import com.college.event.dao.StatisticsDAO;
import com.college.event.model.DashboardStats;
import com.college.event.model.Person;

import java.sql.SQLException;

/** Statistics are administrator-only. */
public class StatisticsService {

    private final StatisticsDAO statisticsDAO = new StatisticsDAO();

    public DashboardStats getDashboardStats(Person admin) throws ServiceException {
        AuthenticationService.requireAdmin(admin);
        try {
            return statisticsDAO.getDashboardStats();
        } catch (SQLException e) {
            throw new ServiceException("Could not load statistics: " + e.getMessage(), e);
        }
    }
}