package com.college.event.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/** Reusable input checks. Event checks are added in Step 7. */
public final class ValidationUtil {

    private static final Pattern EMAIL =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern PHONE = Pattern.compile("^[0-9]{10}$");
    private static final Pattern NAME = Pattern.compile("^\\p{L}[\\p{L} .'-]{1,99}$");

    private ValidationUtil() {
    }

    public static boolean isBlank(String text) {
        return text == null || text.trim().isEmpty();
    }

    public static boolean isValidEmail(String email) {
        return !isBlank(email) && email.length() <= 150 && EMAIL.matcher(email.trim()).matches();
    }

    public static boolean isValidPhone(String phone) {
        return !isBlank(phone) && PHONE.matcher(phone.trim()).matches();
    }

    public static boolean isValidName(String name) {
        return !isBlank(name) && NAME.matcher(name.trim()).matches();
    }

    public static boolean isValidYear(int year) {
        return year >= 1 && year <= 6;
    }

    /** At least 8 characters, with at least one letter and one digit. */
    public static boolean isStrongPassword(String password) {
        if (password == null || password.length() < 8) {
            return false;
        }
        boolean hasLetter = false;
        boolean hasDigit = false;
        for (char c : password.toCharArray()) {
            if (Character.isLetter(c)) hasLetter = true;
            if (Character.isDigit(c)) hasDigit = true;
        }
        return hasLetter && hasDigit;
    }

    /** Checks every field and returns a list of problems (empty list = all valid). */
    public static List<String> validateStudentRegistration(String fullName, String email,
            String department, int year, String phone, String password, String confirmPassword) {

        List<String> errors = new ArrayList<>();
        if (!isValidName(fullName)) {
            errors.add("Full name must be 2-100 characters (letters, spaces, . ' - only).");
        }
        if (!isValidEmail(email)) {
            errors.add("Enter a valid email address.");
        }
        if (isBlank(department)) {
            errors.add("Department is required.");
        }
        if (!isValidYear(year)) {
            errors.add("Year of study must be between 1 and 6.");
        }
        if (!isValidPhone(phone)) {
            errors.add("Phone number must be exactly 10 digits.");
        }
        if (!isStrongPassword(password)) {
            errors.add("Password must be at least 8 characters and contain a letter and a digit.");
        }
        if (password == null || !password.equals(confirmPassword)) {
            errors.add("Passwords do not match.");
        }
        return errors;
    }
}