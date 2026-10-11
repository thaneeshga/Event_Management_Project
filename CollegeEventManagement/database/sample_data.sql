USE college_event_db;

INSERT INTO events
    (event_name, description, event_date, start_time, venue, organizer,
     registration_fee, max_capacity, status)
VALUES
('TechSymposium 2026',
 'Annual technical symposium with paper presentations and tech talks.',
 DATE_ADD(CURDATE(), INTERVAL 10 DAY), '09:30:00', 'Main Auditorium',
 'Computer Science Dept', 200.00, 150, 'OPEN'),

('Java Workshop',
 'Hands-on workshop on Java OOP, collections and JDBC.',
 DATE_ADD(CURDATE(), INTERVAL 5 DAY), '10:00:00', 'Lab 3',
 'Coding Club', 100.00, 40, 'OPEN'),

('CodeSprint Hackathon',
 '24-hour team hackathon. Teams of up to four students.',
 DATE_ADD(CURDATE(), INTERVAL 21 DAY), '08:00:00', 'Innovation Center',
 'IEEE Student Branch', 300.00, 80, 'OPEN'),

('Competitive Coding Contest',
 'Individual contest with algorithmic problems.',
 DATE_ADD(CURDATE(), INTERVAL 14 DAY), '14:00:00', 'Lab 1',
 'Coding Club', 0.00, 60, 'OPEN'),

('Cultural Fest - Rhythm',
 'Dance, music and drama competitions.',
 DATE_ADD(CURDATE(), INTERVAL 30 DAY), '17:00:00', 'Open Air Theatre',
 'Cultural Committee', 50.00, 300, 'OPEN'),

('AI and Machine Learning Seminar',
 'Guest lecture by industry experts.',
 DATE_ADD(CURDATE(), INTERVAL 7 DAY), '11:00:00', 'Seminar Hall',
 'AI Club', 0.00, 5, 'OPEN'),

('Web Design Workshop',
 'Completed workshop on UI design basics.',
 DATE_SUB(CURDATE(), INTERVAL 15 DAY), '10:00:00', 'Lab 2',
 'Design Club', 75.00, 50, 'COMPLETED'),

('Freshers Quiz Night',
 'Cancelled due to scheduling conflicts.',
 DATE_ADD(CURDATE(), INTERVAL 3 DAY), '18:00:00', 'Room 204',
 'Student Council', 0.00, 100, 'CANCELLED');