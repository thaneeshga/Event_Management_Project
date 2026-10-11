package com.college.event.service;

import com.college.event.dao.StudentDAO;
import com.college.event.model.Person;
import com.college.event.model.StudentSummary;
import com.college.event.util.ValidationUtil;

import java.sql.SQLException;
import java.util.List;

/** Viewing the student list is administrator-only. */
public class StudentService {

    private final StudentDAO studentDAO = new StudentDAO();

    public List<StudentSummary> getAllStudents(Person admin) throws ServiceException {
        AuthenticationService.requireAdmin(admin);
        try {
            return studentDAO.findAllWithCounts();
        } catch (SQLException e) {
            throw new ServiceException("Could not load students: " + e.getMessage(), e);
        }
    }

    public List<StudentSummary> searchStudents(Person admin, String keyword) throws ServiceException {
        AuthenticationService.requireAdmin(admin);
        if (ValidationUtil.isBlank(keyword)) {
            return getAllStudents(admin);
        }
        try {
            return studentDAO.searchWithCounts(keyword);
        } catch (SQLException e) {

            throw new ServiceException("Could not search students: " + e.getMessage(), e);
        }
    }
}