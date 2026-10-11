package com.college.event.dao;

import com.college.event.model.Student;
import com.college.event.model.StudentSummary;
import java.util.ArrayList;
import java.util.List;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;

/**
 * Data Access Object for the students table.
 * All SQL for students lives here and nowhere else.
 * Every query uses PreparedStatement (placeholders) to prevent SQL injection.
 */
public class StudentDAO {

    /** Inserts a student, stores the new id in the object, and returns it. */
    public int insert(Student student) throws SQLException {
        String sql = "INSERT INTO students "
                + "(full_name, email, department, year_of_study, phone, password_hash) "
                + "VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, student.getFullName());
            ps.setString(2, student.getEmail());
            ps.setString(3, student.getDepartment());
            ps.setInt(4, student.getYearOfStudy());
            ps.setString(5, student.getPhone());
            ps.setString(6, student.getPasswordHash());
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    int id = keys.getInt(1);
                    student.setId(id);
                    return id;
                }
            }
            throw new SQLException("Student was inserted but no id was returned.");
        }
    }

    /** Optional means "a student, or nothing if the email is not found". */
    public Optional<Student> findByEmail(String email) throws SQLException {
        String sql = "SELECT * FROM students WHERE email = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        }
    }

    public Optional<Student> findById(int studentId) throws SQLException {
        String sql = "SELECT * FROM students WHERE student_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        }
    }

    public boolean existsByEmail(String email) throws SQLException {
        String sql = "SELECT COUNT(*) FROM students WHERE email = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    /** Converts the current row of a ResultSet into a Student object. */
    private Student mapRow(ResultSet rs) throws SQLException {
        return new Student(
                rs.getInt("student_id"),
                rs.getString("full_name"),
                rs.getString("email"),
                rs.getString("password_hash"),
                rs.getString("department"),
                rs.getInt("year_of_study"),
                rs.getString("phone"));
    }
    
    // ================= admin: list and search with registration counts =================

    // LEFT JOIN keeps students who have no registrations (their count is 0)
    private static final String SELECT_WITH_COUNTS =
            "SELECT s.*, COUNT(r.registration_id) AS active_registrations "
                    + "FROM students s LEFT JOIN registrations r "
                    + "ON r.student_id = s.student_id AND r.status = 'REGISTERED' ";
    private static final String GROUP_AND_ORDER = "GROUP BY s.student_id ORDER BY s.full_name";

    public List<StudentSummary> findAllWithCounts() throws SQLException {
        return queryWithCounts(SELECT_WITH_COUNTS + GROUP_AND_ORDER);
    }

    /** Matches name, email or department (partial), or an exact student id. */
    public List<StudentSummary> searchWithCounts(String keyword) throws SQLException {
        String trimmed = keyword.trim();
        String like = "%" + escapeLike(trimmed) + "%";
        int id = -1;
        try {
            id = Integer.parseInt(trimmed);
        } catch (NumberFormatException ignored) {
            // not a number, so only the text search applies
        }
        String sql = SELECT_WITH_COUNTS
                + "WHERE s.full_name LIKE ? OR s.email LIKE ? OR s.department LIKE ? "
                + "OR s.student_id = ? " + GROUP_AND_ORDER;
        return queryWithCounts(sql, like, like, like, id);
    }

    private List<StudentSummary> queryWithCounts(String sql, Object... params) throws SQLException {
        try (Connection conn = DBConnection.getConnection();

             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                List<StudentSummary> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(new StudentSummary(mapRow(rs), rs.getInt("active_registrations")));
                }
                return list;
            }
        }
    }

    /** Stops the characters % and _ typed by a user from acting as wildcards. */
    private String escapeLike(String text) {
        return text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}