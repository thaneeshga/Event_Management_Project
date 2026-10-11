package com.college.event.model;

/** An administrator user. Has no extra fields, only a different role. */
public class Admin extends Person {

    public Admin(int id, String fullName, String email, String passwordHash) {
        super(id, fullName, email, passwordHash);
    }

    @Override
    public Role getRole() {
        return Role.ADMIN;
    }
}
