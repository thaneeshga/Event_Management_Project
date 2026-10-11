package com.college.event.ui;

import com.college.event.model.Person;
import com.college.event.model.Role;
import com.college.event.service.StatisticsService;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * The administrator window: a sidebar on the left and a CardLayout on the right.
 * CardLayout stacks several panels and shows one at a time, like pages of a book.
 */
public class AdminDashboard extends JFrame {

    private final Person admin;
    private final StatisticsService statisticsService = new StatisticsService();

    private final CardLayout cardLayout = new CardLayout();

    private final JPanel content = new JPanel(cardLayout);

    private final JLabel totalEventsValue = createValueLabel();
    private final JLabel totalStudentsValue = createValueLabel();
    private final JLabel totalRegistrationsValue = createValueLabel();
    private final JLabel upcomingEventsValue = createValueLabel();

    public AdminDashboard(Person admin) {
        super("College Event Management - Administrator");
        if (admin == null || admin.getRole() != Role.ADMIN) {
            throw new IllegalArgumentException("An administrator account is required.");
        }
        this.admin = admin;

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1240, 760);
        setMinimumSize(new Dimension(1000, 640));
        setLocationRelativeTo(null);

        // The three management screens
        EventManagementPanel eventPanel = new EventManagementPanel(admin);
        StudentManagementPanel studentPanel = new StudentManagementPanel(admin);
        RegistrationPanel registrationPanel = new RegistrationPanel(admin);

        Sidebar sidebar = new Sidebar("Event Manager", admin.getFullName() + " (Administrator)");
        JButton home = sidebar.addNavItem("Dashboard", () -> {
            cardLayout.show(content, "dashboard");
            loadStats();
        });
        sidebar.addNavItem("Manage Events", () -> {
            cardLayout.show(content, "events");
            eventPanel.refresh();

        });
        sidebar.addNavItem("Manage Students", () -> {
            cardLayout.show(content, "students");
            studentPanel.refresh();
        });
        sidebar.addNavItem("Manage Registrations", () -> {
            cardLayout.show(content, "registrations");
            registrationPanel.refresh();
        });
        sidebar.addNavItem("Event Statistics", () -> cardLayout.show(content, "statistics"));
        sidebar.addLogoutButton(this::logout);

        content.setBackground(UITheme.BACKGROUND);
        content.add(buildDashboardCard(), "dashboard");
        content.add(eventPanel, "events");
        content.add(studentPanel, "students");
        content.add(registrationPanel, "registrations");
        content.add(buildPlaceholder("Event Statistics", "Reports and charts are built in Step 11."), "statistics");

        add(sidebar, BorderLayout.WEST);
        add(content, BorderLayout.CENTER);

        sidebar.select(home);
        cardLayout.show(content, "dashboard");
        loadStats();
    }

    // ---------- dashboard page ----------

    private JPanel buildDashboardCard() {
        JPanel page = new JPanel(new BorderLayout(0, 24));
        page.setBackground(UITheme.BACKGROUND);

        page.setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));

        JPanel titles = new JPanel();
        titles.setLayout(new BoxLayout(titles, BoxLayout.Y_AXIS));
        titles.setOpaque(false);
        titles.add(UITheme.label("Dashboard", UITheme.TITLE, UITheme.TEXT));
        titles.add(UITheme.label("Welcome back, " + admin.getFullName(), UITheme.BODY, UITheme.MUTED));

        JButton refresh = UITheme.secondaryButton("Refresh");
        refresh.addActionListener(e -> loadStats());
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 8));
        right.setOpaque(false);
        right.add(refresh);

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.add(titles, BorderLayout.WEST);
        header.add(right, BorderLayout.EAST);

        JPanel grid = new JPanel(new GridLayout(1, 4, 16, 0));
        grid.setOpaque(false);
        grid.add(statCard("Total Events", totalEventsValue, UITheme.PRIMARY));
        grid.add(statCard("Registered Students", totalStudentsValue, UITheme.SUCCESS));
        grid.add(statCard("Total Registrations", totalRegistrationsValue, UITheme.WARNING));
        grid.add(statCard("Upcoming Events", upcomingEventsValue, new Color(124, 58, 237)));

        JPanel body = new JPanel(new BorderLayout(0, 16));
        body.setOpaque(false);
        body.add(grid, BorderLayout.NORTH);
        body.add(UITheme.label("These numbers are read live from MySQL each time this page opens.",
                UITheme.SMALL, UITheme.MUTED), BorderLayout.CENTER);


        page.add(header, BorderLayout.NORTH);
        page.add(body, BorderLayout.CENTER);
        return page;
    }

    private JPanel statCard(String title, JLabel valueLabel, Color accent) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(UITheme.CARD);
        card.setBorder(BorderFactory.createLineBorder(UITheme.BORDER));
        card.setPreferredSize(new Dimension(200, 120));

        JPanel strip = new JPanel();
        strip.setBackground(accent);
        strip.setPreferredSize(new Dimension(6, 0));

        JPanel text = new JPanel();
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        text.setOpaque(false);
        text.setBorder(BorderFactory.createEmptyBorder(18, 18, 18, 18));
        text.add(UITheme.label(title, UITheme.BODY, UITheme.MUTED));
        text.add(Box.createVerticalStrut(8));
        text.add(valueLabel);

        card.add(strip, BorderLayout.WEST);
        card.add(text, BorderLayout.CENTER);
        return card;
    }

    private static JLabel createValueLabel() {
        return UITheme.label("-", new Font("Segoe UI", Font.BOLD, 36), UITheme.TEXT);
    }


    /** Reads the four numbers from MySQL (background thread) and shows them. */
    private void loadStats() {
        BackgroundTask.run(this,
                () -> statisticsService.getDashboardStats(admin),
                stats -> {
                    totalEventsValue.setText(String.valueOf(stats.totalEvents()));
                    totalStudentsValue.setText(String.valueOf(stats.totalStudents()));
                    totalRegistrationsValue.setText(String.valueOf(stats.totalRegistrations()));
                    upcomingEventsValue.setText(String.valueOf(stats.upcomingEvents()));
                });
    }

    // ---------- statistics page (filled in Step 11) ----------

    private JPanel buildPlaceholder(String title, String message) {
        JPanel page = new JPanel(new BorderLayout());
        page.setBackground(UITheme.BACKGROUND);
        page.setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));

        JPanel box = new JPanel();
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
        box.setOpaque(false);
        box.add(UITheme.label(title, UITheme.TITLE, UITheme.TEXT));
        box.add(Box.createVerticalStrut(8));
        box.add(UITheme.label(message, UITheme.BODY, UITheme.MUTED));

        page.add(box, BorderLayout.NORTH);
        return page;
    }

    private void logout() {
        if (UITheme.confirm(this, "Do you want to log out?")) {

            dispose();
            new LoginFrame().setVisible(true);
        }
    }
}
