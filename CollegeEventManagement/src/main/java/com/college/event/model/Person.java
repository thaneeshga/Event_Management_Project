package com.college.event.model;

import java.util.Objects;

/**
 * Abstract parent class for everyone who can log in.
 * Student and Admin share these fields, so we write them only once (inheritance).
 * It is abstract because "a generic person" never exists in our system.
 */
public abstract class Person implements Summarizable {

    // Encapsulation: fields are private, accessed only through getters/setters
    private int id;
    private String fullName;
    private String email;
    private String passwordHash; // never the plain password

    protected Person(int id, String fullName, String email, String passwordHash) {
        this.id = id;
        this.fullName = fullName;
        this.email = email;
        this.passwordHash = passwordHash;
    }

    /** Every child class must say which role it has. */
    public abstract Role getRole();

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    @Override
    public String getSummary() {
        return fullName + " (" + email + ")";
    }

    // Methods inherited from the Object class, overridden here:

    @Override
    public String toString() {
        return getRole() + ": " + getSummary();
    }

    /** Two people are the same if they have the same role and email. */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Person other = (Person) obj;
        return email != null && email.equalsIgnoreCase(other.email);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getRole(), email == null ? null : email.toLowerCase());
    }
}
