-- ============================================================
-- College Event Management System - schema.sql
-- Database: college_event_db (MySQL 8.0.16+ because of CHECK constraints)
-- ============================================================

-- Uncomment the next line ONLY if you want to wipe everything and start over.
-- DROP DATABASE IF EXISTS college_event_db;

CREATE DATABASE IF NOT EXISTS college_event_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE college_event_db;

-- ------------------------------------------------------------
-- TABLE 1: administrators
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS administrators (
    admin_id       INT           NOT NULL AUTO_INCREMENT,
    full_name      VARCHAR(100)  NOT NULL,
    email          VARCHAR(150)  NOT NULL,
    password_hash  VARCHAR(255)  NOT NULL,
    created_at     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (admin_id),
    CONSTRAINT uq_admin_email UNIQUE (email)
) ENGINE=InnoDB;

-- ------------------------------------------------------------
-- TABLE 2: students
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS students (
    student_id     INT           NOT NULL AUTO_INCREMENT,
    full_name      VARCHAR(100)  NOT NULL,
    email          VARCHAR(150)  NOT NULL,
    department     VARCHAR(100)  NOT NULL,
    year_of_study  TINYINT       NOT NULL,
    phone          VARCHAR(15)   NOT NULL,
    password_hash  VARCHAR(255)  NOT NULL,
    created_at     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (student_id),
    CONSTRAINT uq_student_email UNIQUE (email),
    CONSTRAINT chk_student_year CHECK (year_of_study BETWEEN 1 AND 6)
) ENGINE=InnoDB;

-- ------------------------------------------------------------
-- TABLE 3: events
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS events (
    event_id          INT            NOT NULL AUTO_INCREMENT,
    event_name        VARCHAR(150)   NOT NULL,
    description       TEXT           NULL,
    event_date        DATE           NOT NULL,
    start_time        TIME           NOT NULL,
    venue             VARCHAR(150)   NOT NULL,
    organizer         VARCHAR(100)   NOT NULL,
    registration_fee  DECIMAL(8,2)   NOT NULL DEFAULT 0.00,
    max_capacity      INT            NOT NULL,
    status            ENUM('OPEN','CLOSED','CANCELLED','COMPLETED')
                                     NOT NULL DEFAULT 'OPEN',
    created_at        TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (event_id),
    CONSTRAINT chk_event_fee      CHECK (registration_fee >= 0),
    CONSTRAINT chk_event_capacity CHECK (max_capacity > 0)
) ENGINE=InnoDB;

-- ------------------------------------------------------------
-- TABLE 4: registrations (junction table: students <-> events)
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS registrations (
    registration_id    INT       NOT NULL AUTO_INCREMENT,
    student_id         INT       NOT NULL,
    event_id           INT       NOT NULL,
    registration_date  DATETIME  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status             ENUM('REGISTERED','CANCELLED')
                                 NOT NULL DEFAULT 'REGISTERED',
    cancelled_at       DATETIME  NULL,
    PRIMARY KEY (registration_id),
    -- Same student cannot register for the same event twice
    CONSTRAINT uq_student_event UNIQUE (student_id, event_id),
    -- RESTRICT = you cannot delete a student/event that has registrations
    CONSTRAINT fk_reg_student FOREIGN KEY (student_id)
        REFERENCES students (student_id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_reg_event FOREIGN KEY (event_id)
        REFERENCES events (event_id)
        ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB;

-- ------------------------------------------------------------
-- INDEXES (speed up searching, filtering and joins)
-- ------------------------------------------------------------
CREATE INDEX idx_events_date       ON events (event_date);
CREATE INDEX idx_events_status     ON events (status);
CREATE INDEX idx_events_name       ON events (event_name);
CREATE INDEX idx_students_dept     ON students (department);
CREATE INDEX idx_reg_event         ON registrations (event_id);
CREATE INDEX idx_reg_status        ON registrations (status);

-- ------------------------------------------------------------
-- VIEW 1: events with registration count and available seats
-- Only registrations with status REGISTERED use a seat.
-- ------------------------------------------------------------
USE college_event_db;

CREATE OR REPLACE VIEW v_event_summary AS
SELECT  e.event_id,
        e.event_name,
        e.description,
        e.event_date,
        e.start_time,
        e.venue,
        e.organizer,
        e.registration_fee,
        e.max_capacity,
        e.status,
        COUNT(r.registration_id)                    AS registered_count,
        e.max_capacity - COUNT(r.registration_id)   AS available_seats
FROM events e
LEFT JOIN registrations r
       ON r.event_id = e.event_id
      AND r.status = 'REGISTERED'
GROUP BY e.event_id;

-- ------------------------------------------------------------
-- VIEW 2: registration details joined with student and event
-- ------------------------------------------------------------
CREATE OR REPLACE VIEW v_registration_details AS
SELECT  r.registration_id,
        r.registration_date,
        r.status AS registration_status,
        s.student_id,
        s.full_name   AS student_name,
        s.email       AS student_email,
        s.department,
        e.event_id,
        e.event_name,
        e.event_date,
        e.venue,
        e.registration_fee
FROM registrations r
INNER JOIN students s ON s.student_id = r.student_id
INNER JOIN events   e ON e.event_id   = r.event_id;
