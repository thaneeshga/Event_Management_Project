package com.college.event.util;

import com.college.event.dao.AdminDAO;
import com.college.event.model.Admin;

import java.sql.SQLException;
import java.util.Scanner;

/**
 * One-time console tool that creates an administrator account.
 * Students cannot create admins from the app, so this tool is the safe way
 * to create the first one. The password is hashed before it is saved.
 */
public class CreateAdminTool {

    public static void main(String[] args) {
        Scanner in = new Scanner(System.in);
        System.out.println("=== Create Administrator Account ===");

        System.out.print("Full name: ");
        String name = in.nextLine().trim();
        System.out.print("Email: ");
        String email = in.nextLine().trim().toLowerCase();
        System.out.print("Password (min 8 characters, letters and digits): ");
        String password = in.nextLine();
        System.out.print("Confirm password: ");
        String confirm = in.nextLine();

        if (!ValidationUtil.isValidName(name)) {
            System.out.println("Invalid name.");
            return;
        }

        if (!ValidationUtil.isValidEmail(email)) {
            System.out.println("Invalid email address.");
            return;
        }
        if (!ValidationUtil.isStrongPassword(password)) {
            System.out.println("Password must be at least 8 characters with a letter and a digit.");
            return;
        }
        if (!password.equals(confirm)) {
            System.out.println("Passwords do not match.");
            return;
        }

        AdminDAO dao = new AdminDAO();
        try {
            if (dao.existsByEmail(email)) {
                System.out.println("An administrator with this email already exists.");
                return;
            }
            Admin admin = new Admin(0, name, email, PasswordUtil.hashPassword(password));
            int id = dao.insert(admin);
            System.out.println("Administrator created successfully. Admin id = " + id);
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
    }
}