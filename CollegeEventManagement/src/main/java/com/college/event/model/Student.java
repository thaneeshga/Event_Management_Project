package com.college.event.model;

/** A student user. Extends Person, so it gets id, name, email and password hash. */
public class Student extends Person {

    private String department;
    private int yearOfStudy;
    private String phone;

    public Student(int id, String fullName, String email, String passwordHash,
                   String department, int yearOfStudy, String phone) {
        super(id, fullName, email, passwordHash); // call the parent constructor
        this.department = department;
        this.yearOfStudy = yearOfStudy;
        this.phone = phone;
    }

    /**
     * Overloaded constructor (same name, different parameters) used when a new
     * student signs up and does not have a database id yet.
     */
    public Student(String fullName, String email, String passwordHash,
                   String department, int yearOfStudy, String phone) {
        this(0, fullName, email, passwordHash, department, yearOfStudy, phone);
    }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public int getYearOfStudy() { return yearOfStudy; }
    public void setYearOfStudy(int yearOfStudy) { this.yearOfStudy = yearOfStudy; }


    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    // Method overriding: implements the abstract method from Person
    @Override
    public Role getRole() {
        return Role.STUDENT;
    }

    // Method overriding: extends the parent's version with student details
    @Override
    public String getSummary() {
        return super.getSummary() + " | " + department + ", Year " + yearOfStudy;
    }
}
