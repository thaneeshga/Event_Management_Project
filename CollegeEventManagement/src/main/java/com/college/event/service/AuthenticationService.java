package com.college.event.service;

import com.college.event.dao.AdminDAO;
import com.college.event.dao.StudentDAO;
import com.college.event.model.Person;
import com.college.event.model.Role;
import com.college.event.model.Student;
import com.college.event.util.PasswordUtil;
import com.college.event.util.ValidationUtil;

import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.List;

/**
 * Business logic for sign-up, login and role-based authorization.
 * The UI calls this class. This class calls the DAOs. The UI never touches SQL.
 */
public class AuthenticationService {

    private final StudentDAO studentDAO = new StudentDAO();
    private final AdminDAO adminDAO = new AdminDAO();

    /** Validates the form, hashes the password and saves a new student. */
    public Student registerStudent(String fullName, String email, String department,
            int yearOfStudy, String phone, String password, String confirmPassword)
            throws ServiceException {

        List<String> errors = ValidationUtil.validateStudentRegistration(
                fullName, email, department, yearOfStudy, phone, password, confirmPassword);
        if (!errors.isEmpty()) {
            throw new ServiceException(String.join("\n", errors));
        }

        String cleanEmail = email.trim().toLowerCase();
        try {
            if (studentDAO.existsByEmail(cleanEmail)) {
                throw new ServiceException("This email is already registered.");
            }
            Student student = new Student(
                    fullName.trim(), cleanEmail, PasswordUtil.hashPassword(password),
                    department.trim(), yearOfStudy, phone.trim());
            studentDAO.insert(student);
            return student;

        } catch (SQLIntegrityConstraintViolationException e) {
            // Safety net: if two people register the same email at the same instant,
            // the UNIQUE constraint in MySQL still stops the duplicate.
            throw new ServiceException("This email is already registered.", e);
        } catch (SQLException e) {
            throw new ServiceException("Could not register because of a database error: "
                    + e.getMessage(), e);
        }
    }

    /**
     * Logs in as the given role. The error message is the same for "unknown email"
     * and "wrong password", so an attacker cannot discover which emails exist.
     */
    public Person login(String email, String password, Role role) throws ServiceException {
        if (ValidationUtil.isBlank(email) || ValidationUtil.isBlank(password)) {
            throw new ServiceException("Please enter your email and password.");
        }
        String cleanEmail = email.trim().toLowerCase();

        try {
            Person user;
            if (role == Role.ADMIN) {
                user = adminDAO.findByEmail(cleanEmail).orElse(null);
            } else {
                user = studentDAO.findByEmail(cleanEmail).orElse(null);
            }

            if (user == null || !PasswordUtil.verifyPassword(password, user.getPasswordHash())) {
                throw new ServiceException("Invalid email or password.");
            }
            return user;

        } catch (SQLException e) {
            throw new ServiceException("Could not log in because of a database error: "
                    + e.getMessage(), e);
        }
    }

    /** Role-based authorization: call this before any administrator-only operation. */
    public static void requireAdmin(Person user) throws ServiceException {
        if (user == null || user.getRole() != Role.ADMIN) {
            throw new ServiceException("Access denied: administrator privileges required.");
        }
    }

    /** Role-based authorization for student-only operations. */
    public static void requireStudent(Person user) throws ServiceException {
        if (user == null || user.getRole() != Role.STUDENT) {
            throw new ServiceException("Access denied: student account required.");
        }
    }
}
