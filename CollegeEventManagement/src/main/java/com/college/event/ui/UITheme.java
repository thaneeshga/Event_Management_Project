package com.college.event.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.plaf.basic.BasicButtonUI;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;

/**
 * One place for the look of the whole application: colors, fonts and ready-made
 * styled components. Changing a color here changes it on every screen.
 */
public final class UITheme {

    // ----- colors -----
    public static final Color PRIMARY = new Color(37, 99, 235);
    public static final Color PRIMARY_DARK = new Color(29, 78, 216);
    public static final Color SIDEBAR = new Color(17, 24, 39);
    public static final Color SIDEBAR_HOVER = new Color(55, 65, 81);
    public static final Color BACKGROUND = new Color(243, 244, 246);
    public static final Color CARD = Color.WHITE;
    public static final Color TEXT = new Color(31, 41, 55);
    public static final Color MUTED = new Color(107, 114, 128);
    public static final Color BORDER = new Color(209, 213, 219);
    public static final Color DANGER = new Color(220, 38, 38);
    public static final Color SUCCESS = new Color(22, 163, 74);
    public static final Color WARNING = new Color(217, 119, 6);
    public static final Color STRIPE = new Color(249, 250, 251);
    public static final Color SELECTION = new Color(219, 234, 254);
    public static final Color TABLE_HEADER = new Color(30, 58, 138);

    // ----- fonts -----
    public static final Font TITLE = new Font("Segoe UI", Font.BOLD, 26);
    public static final Font HEADING = new Font("Segoe UI", Font.BOLD, 18);
    public static final Font BODY = new Font("Segoe UI", Font.PLAIN, 14);
    public static final Font BODY_BOLD = new Font("Segoe UI", Font.BOLD, 14);
    public static final Font SMALL = new Font("Segoe UI", Font.PLAIN, 12);

    // ----- date formats -----
    public static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy");
    public static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("hh:mm a");
    public static final DateTimeFormatter DATETIME_FORMAT =
            DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");

    private UITheme() {
    }

    /** Call once at start-up, before any window is created. */
    public static void install() {
        try {
            // The cross-platform look honours our button and table colors on every PC
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception ignored) {
            // keep the default look if it cannot be set
        }
        String[] fontKeys = {"Label.font", "TextField.font", "PasswordField.font",
                "ComboBox.font", "CheckBox.font", "Button.font", "Table.font",
                "OptionPane.messageFont", "OptionPane.buttonFont"};
        for (String key : fontKeys) {
            UIManager.put(key, BODY);
        }
    }

    // ----- labels and panels -----

    public static JLabel label(String text, Font font, Color color) {
        JLabel label = new JLabel(text);
        label.setFont(font);
        label.setForeground(color);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    /** A white panel with a thin border and inner padding. */
    public static JPanel cardPanel() {
        JPanel panel = new JPanel();
        panel.setBackground(CARD);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                BorderFactory.createEmptyBorder(20, 24, 20, 24)));
        return panel;
    }

    // ----- inputs -----

    public static void styleInput(JComponent field) {
        field.setFont(BODY);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER),
                BorderFactory.createEmptyBorder(7, 10, 7, 10)));
    }

    public static void styleCombo(JComboBox<?> combo) {
        combo.setFont(BODY);
        combo.setBackground(Color.WHITE);
    }

    // ----- buttons -----

    public static JButton primaryButton(String text) {
        return baseButton(text, PRIMARY, PRIMARY_DARK);
    }

    public static JButton secondaryButton(String text) {
        return baseButton(text, new Color(107, 114, 128), new Color(75, 85, 99));
    }

    public static JButton dangerButton(String text) {
        return baseButton(text, DANGER, new Color(185, 28, 28));
    }

    public static JButton successButton(String text) {
        return baseButton(text, SUCCESS, new Color(21, 128, 61));
    }

    /** A text-only button that looks like a link. */
    public static JButton linkButton(String text) {
        JButton button = new JButton(text);
        button.setUI(new BasicButtonUI());
        button.setFont(BODY_BOLD);
        button.setForeground(PRIMARY);
        button.setContentAreaFilled(false);
        button.setOpaque(false);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setAlignmentX(Component.LEFT_ALIGNMENT);
        return button;
    }

    /** A full-width navigation button for the sidebar. */
    public static JButton sidebarButton(String text) {
        JButton button = baseButton(text, SIDEBAR, SIDEBAR_HOVER);
        button.setFont(BODY);
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setBorder(BorderFactory.createEmptyBorder(12, 24, 12, 20));
        button.setAlignmentX(Component.LEFT_ALIGNMENT);
        button.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        return button;
    }

    /** Highlights (or un-highlights) a sidebar button. */
    public static void setSelected(JButton button, boolean selected) {
        button.putClientProperty("selected", selected);
        button.setBackground(selected ? PRIMARY : SIDEBAR);
    }

    private static boolean isSelected(JButton button) {
        return Boolean.TRUE.equals(button.getClientProperty("selected"));
    }

    private static JButton baseButton(String text, Color background, Color hover) {
        JButton button = new JButton(text);
        // BasicButtonUI makes the background color show on every look and feel
        button.setUI(new BasicButtonUI());
        button.setFont(BODY_BOLD);
        button.setForeground(Color.WHITE);
        button.setBackground(background);
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(10, 18, 10, 18));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (button.isEnabled() && !isSelected(button)) {
                    button.setBackground(hover);
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(isSelected(button) ? PRIMARY : background);
            }
        });
        return button;
    }

    // ----- tables -----

    /** A table model whose cells cannot be edited by typing in them. */
    public static DefaultTableModel readOnlyModel(String... columns) {
        return new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    /** A styled JTable with a dark header and striped rows. */
    public static JTable createTable(DefaultTableModel model) {
        JTable table = new JTable(model) {
            @Override
            public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
                Component cell = super.prepareRenderer(renderer, row, column);
                if (!isRowSelected(row)) {
                    cell.setBackground(row % 2 == 0 ? Color.WHITE : STRIPE);
                }
                return cell;
            }
        };
        table.setFont(BODY);
        table.setForeground(TEXT);
        table.setRowHeight(32);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setSelectionBackground(SELECTION);
        table.setSelectionForeground(TEXT);
        table.setFillsViewportHeight(true);

        // cell text with a little padding
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value,
                    boolean selected, boolean focus, int row, int col) {
                super.getTableCellRendererComponent(t, value, selected, false, row, col);
                setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
                return this;
            }
        });

        // dark header
        JTableHeader header = table.getTableHeader();
        header.setReorderingAllowed(false);
        header.setPreferredSize(new Dimension(0, 38));
        header.setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value,
                    boolean selected, boolean focus, int row, int col) {
                super.getTableCellRendererComponent(t, value, selected, false, row, col);
                setBackground(TABLE_HEADER);
                setForeground(Color.WHITE);
                setFont(BODY_BOLD);
                setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
                return this;
            }
        });
        return table;
    }

    // ----- dialogs and formatting -----

    public static void showError(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    public static void showInfo(Component parent, String title, String message) {
        JOptionPane.showMessageDialog(parent, message, title, JOptionPane.INFORMATION_MESSAGE);
    }

    /** Yes/No question. Returns true only if the user clicks Yes. */
    public static boolean confirm(Component parent, String message) {
        return JOptionPane.showConfirmDialog(parent, message, "Please confirm",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION;
    }

    /** "Free" for zero, otherwise "Rs. 100.00". Change the prefix here if you like. */
    public static String formatFee(BigDecimal fee) {
        if (fee == null || fee.signum() == 0) {
            return "Free";
        }
        return "Rs. " + fee.setScale(2, RoundingMode.HALF_UP);
    }
}