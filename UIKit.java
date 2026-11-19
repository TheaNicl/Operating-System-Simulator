import javax.swing.*;
import javax.swing.border.AbstractBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.util.Arrays;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Shared building blocks so every module panel has the same spacing,
 * typography, tables, buttons and input validation.
 * Colors are resolved from UIManager at paint time.
 */
public final class UIKit {
    private static final Pattern SIZE_PATTERN =
            Pattern.compile("^\\s*(\\d+(?:\\.\\d+)?)\\s*(B|KB|MB|GB|TB)?\\s*$", Pattern.CASE_INSENSITIVE);

    // Utility class: not meant to be instantiated.
    private UIKit() {}

    // ---------------------------------------------------------------- layout

    /** Standard module layout: BorderLayout with a 16px gap and a 20/24px outer margin. */
    public static void setupModule(JPanel panel) {
        panel.setLayout(new BorderLayout(0, 16));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));
    }

    /** Panel with the given layout that lets the parent background show through. */
    public static JPanel transparent(LayoutManager layout) {
        JPanel panel = new JPanel(layout);
        panel.setOpaque(false);
        return panel;
    }

    /** Left-aligned horizontal row with consistent 8px gaps. */
    public static JPanel row(Component... items) {
        JPanel panel = transparent(null);
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
        for (int i = 0; i < items.length; i++) {
            Component item = items[i];
            if (item instanceof JTextField || item instanceof JComboBox) {
                item.setMaximumSize(item.getPreferredSize());
            }
            if (i > 0) {
                panel.add(Box.createHorizontalStrut(8));
            }
            panel.add(item);
        }
        return panel;
    }

    /** A toolbar with content on the left and actions on the right. */
    public static JPanel toolbar(JComponent left, JComponent right) {
        JPanel panel = transparent(new BorderLayout(16, 0));
        if (left != null) {
            panel.add(left, BorderLayout.WEST);
        }
        if (right != null) {
            panel.add(right, BorderLayout.EAST);
        }
        return panel;
    }

    /** Borderless split pane; {@code weight} is the share of extra space given to the first component. */
    public static JSplitPane split(int orientation, Component first, Component second, double weight) {
        JSplitPane split = new JSplitPane(orientation, first, second);
        split.setResizeWeight(weight);
        split.setContinuousLayout(true);
        split.setBorder(BorderFactory.createEmptyBorder());
        split.setOpaque(false);
        return split;
    }

    /**
     * Label/field grid. Pass alternating label strings and components:
     * form(2, "Name", nameField, "Age", ageField)
     */
    public static JPanel form(int pairsPerRow, Object... labelsAndFields) {
        JPanel panel = transparent(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        for (int i = 0; i + 1 < labelsAndFields.length; i += 2) {
            int pair = i / 2;
            int column = (pair % pairsPerRow) * 2;
            int rowIndex = pair / pairsPerRow;
            JComponent field = (JComponent) labelsAndFields[i + 1];

            JLabel label = new JLabel((String) labelsAndFields[i]);
            label.setLabelFor(field);

            gbc.gridx = column;
            gbc.gridy = rowIndex;
            gbc.weightx = 0;
            gbc.insets = new Insets(rowIndex == 0 ? 0 : 10, column == 0 ? 0 : 20, 0, 10);
            panel.add(label, gbc);

            gbc.gridx = column + 1;
            gbc.weightx = 1;
            gbc.insets = new Insets(rowIndex == 0 ? 0 : 10, 0, 0, 0);
            panel.add(field, gbc);
        }
        return panel;
    }

    // ----------------------------------------------------------- typography

    /** Page heading with an optional muted subtitle. */
    public static JComponent header(String title, String subtitle) {
        return header(title, subtitle, null);
    }

    public static JComponent header(String title, String subtitle, JComponent trailing) {
        JLabel titleLabel = new JLabel(title);
        styleClass(titleLabel, "h2", Font.BOLD, 1.6f);
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel text = transparent(null);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        text.add(titleLabel);
        if (subtitle != null) {
            JLabel subtitleLabel = muted(subtitle);
            subtitleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            text.add(Box.createVerticalStrut(4));
            text.add(subtitleLabel);
        }

        JPanel header = transparent(new BorderLayout(16, 0));
        header.add(text, BorderLayout.CENTER);
        if (trailing != null) {
            header.add(trailing, BorderLayout.EAST);
        }
        return header;
    }

    /** Applies a FlatLaf typography class, with a manual fallback for other look and feels. */
    public static void styleClass(JComponent component, String flatStyleClass, int fallbackStyle, float fallbackScale) {
        component.putClientProperty("FlatLaf.styleClass", flatStyleClass);
        if (!Theme.isFlatLaf()) {
            Font font = component.getFont();
            component.setFont(font.deriveFont(fallbackStyle, font.getSize2D() * fallbackScale));
        }
    }

    /** Label drawn in the theme's muted (disabled) text colour, for hints and captions. */
    public static JLabel muted(String text) {
        JLabel label = new JLabel(text);
        label.putClientProperty("FlatLaf.style", "foreground: $Label.disabledForeground");
        if (!Theme.isFlatLaf()) {
            label.setForeground(Color.GRAY);
        }
        return label;
    }

    /** Centered placeholder (title + message) shown before any results exist. */
    public static JComponent emptyState(String title, String message) {
        JLabel titleLabel = new JLabel(title, SwingConstants.CENTER);
        styleClass(titleLabel, "h4", Font.BOLD, 1.1f);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel messageLabel = muted("<html><div style='text-align:center;width:300px'>" + message + "</div></html>");
        messageLabel.setHorizontalAlignment(SwingConstants.CENTER);
        messageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel box = transparent(null);
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
        box.add(titleLabel);
        box.add(Box.createVerticalStrut(6));
        box.add(messageLabel);

        JPanel wrapper = transparent(new GridBagLayout());
        wrapper.add(box);
        return wrapper;
    }

    // -------------------------------------------------------------- controls

    /** Blue call-to-action button (Run, Calculate, ...). */
    public static JButton primary(String text) {
        JButton button = new JButton(text);
        button.putClientProperty("FlatLaf.style",
                "background: #2563EB; foreground: #FFFFFF; hoverBackground: #1D4ED8; "
                        + "pressedBackground: #1E40AF; font: bold");
        if (!Theme.isFlatLaf()) {
            button.setBackground(Theme.ACCENT);
            button.setForeground(Color.WHITE);
        }
        return button;
    }

    /** Plain button for secondary actions (Add, Remove, ...). */
    public static JButton secondary(String text) {
        return new JButton(text);
    }

    /** Mutually exclusive option button (used for algorithm pickers). */
    public static JToggleButton option(String text, ButtonGroup group) {
        JToggleButton button = new JToggleButton(text);
        button.putClientProperty("FlatLaf.style", "selectedBackground: #2563EB; selectedForeground: #FFFFFF");
        group.add(button);
        return button;
    }

    /** Text field with placeholder text; its red error outline clears as soon as the user types. */
    public static JTextField field(String text, String placeholder, int columns) {
        final JTextField field = new JTextField(text, columns);
        if (placeholder != null) {
            field.putClientProperty("JTextField.placeholderText", placeholder);
        }
        field.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { markInvalid(field, false); }
            @Override public void removeUpdate(DocumentEvent e) { markInvalid(field, false); }
            @Override public void changedUpdate(DocumentEvent e) { markInvalid(field, false); }
        });
        return field;
    }

    /** Turns the red error outline on or off (FlatLaf only; harmless on other look and feels). */
    public static void markInvalid(JComponent component, boolean invalid) {
        component.putClientProperty("JComponent.outline", invalid ? "error" : null);
    }

    // ---------------------------------------------------------------- tables

    /** Table model where all columns are editable or none are. */
    public static DefaultTableModel model(String[] columns, boolean editable) {
        return model(columns, editable ? 0 : Integer.MAX_VALUE);
    }

    /** Columns before {@code firstEditableColumn} are read-only (e.g. auto-generated IDs). */
    public static DefaultTableModel model(String[] columns, final int firstEditableColumn) {
        return new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column >= firstEditableColumn;
            }
        };
    }

    /**
     * Columns from {@code firstEditableColumn} up to (but not including) {@code endColumn} are editable.
     * Used when input columns are followed by read-only result columns.
     */
    public static DefaultTableModel model(String[] columns, final int firstEditableColumn, final int endColumn) {
        return new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column >= firstEditableColumn && column < endColumn;
            }
        };
    }

    /** Styled table: row height, centred cells, single-line rows and no column reordering. */
    public static JTable table(DefaultTableModel model) {
        return table(new JTable(model));
    }

    /** Applies the standard table styling to an existing table. */
    public static JTable table(JTable table) {
        table.setRowHeight(30);
        table.setFillsViewportHeight(true);
        table.setShowVerticalLines(false);
        table.setShowHorizontalLines(true);
        table.setIntercellSpacing(new Dimension(0, 1));
        table.getTableHeader().setReorderingAllowed(false);
        table.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);

        // Re-apply the alignment on every call: a look-and-feel switch can reset it.
        DefaultTableCellRenderer centered = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value, boolean selected,
                                                           boolean focused, int row, int column) {
                super.getTableCellRendererComponent(t, value, selected, focused, row, column);
                setHorizontalAlignment(SwingConstants.CENTER);
                return this;
            }
        };
        table.setDefaultRenderer(Object.class, centered);

        TableCellRenderer headerRenderer = table.getTableHeader().getDefaultRenderer();
        if (headerRenderer instanceof JLabel) {
            ((JLabel) headerRenderer).setHorizontalAlignment(SwingConstants.CENTER);
        }
        return table;
    }

    /** Scroll pane with smooth scrolling. */
    public static JScrollPane scroll(JComponent view) {
        JScrollPane scrollPane = new JScrollPane(view);
        scrollPane.putClientProperty("JScrollPane.smoothScrolling", Boolean.TRUE);
        return scrollPane;
    }

    /** Commits a cell that is still being edited so its value is read correctly. */
    public static void stopEditing(JTable table) {
        if (table.isEditing() && table.getCellEditor() != null) {
            table.getCellEditor().stopCellEditing();
        }
    }

    /** Deletes the selected rows, last first so the indices stay valid. */
    public static void removeSelectedRows(JTable table) {
        stopEditing(table);
        int[] selected = table.getSelectedRows();
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        for (int i = selected.length - 1; i >= 0; i--) {
            model.removeRow(table.convertRowIndexToModel(selected[i]));
        }
    }

    /** Validates that every cell from {@code fromColumn} onward is a non-negative whole number. */
    public static boolean validateNumericTable(JTable table, int fromColumn, String tableName, Component parent) {
        return validateNumericTable(table, fromColumn, table.getModel().getColumnCount(), tableName, parent);
    }

    /** Same as above but only checks columns {@code fromColumn} to {@code toColumn - 1}. */
    public static boolean validateNumericTable(JTable table, int fromColumn, int toColumn,
                                               String tableName, Component parent) {
        stopEditing(table);
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        for (int row = 0; row < model.getRowCount(); row++) {
            for (int column = fromColumn; column < toColumn; column++) {
                Object value = model.getValueAt(row, column);
                String text = value == null ? "" : value.toString().trim();
                boolean valid = text.matches("\\d+");
                if (!valid) {
                    table.changeSelection(row, column, false, false);
                    table.requestFocusInWindow();
                    error(parent, tableName + ": row " + (row + 1) + ", \"" + model.getColumnName(column)
                            + "\" must be a whole number (0 or more).");
                    return false;
                }
            }
        }
        return true;
    }

    /** Reads a cell as an int. Call validateNumericTable first. */
    public static int intAt(DefaultTableModel model, int row, int column) {
        return Integer.parseInt(model.getValueAt(row, column).toString().trim());
    }

    // ------------------------------------------------------------ validation

    /** Parses a whole number within [min, max]. Shows an error and returns null when invalid. */
    public static Integer parseInt(JTextField field, String name, int min, int max, Component parent) {
        try {
            int value = Integer.parseInt(field.getText().trim());
            if (value < min || value > max) {
                throw new NumberFormatException();
            }
            markInvalid(field, false);
            return value;
        } catch (NumberFormatException e) {
            return reject(field, parent, name + " must be a whole number between " + min + " and " + max + ".");
        }
    }

    /** Parses "1, 2, 3" or "1 2 3" into ints (each at least {@code min}); null plus an error when invalid. */
    public static int[] parseIntList(JTextField field, String name, int min, Component parent) {
        String text = field.getText().trim();
        if (text.isEmpty()) {
            return reject(field, parent, name + " cannot be empty.");
        }
        String[] parts = text.split("[,\\s]+");
        int[] values = new int[parts.length];
        try {
            for (int i = 0; i < parts.length; i++) {
                values[i] = Integer.parseInt(parts[i]);
                if (values[i] < min) {
                    throw new NumberFormatException();
                }
            }
        } catch (NumberFormatException e) {
            return reject(field, parent, name + " must be a comma-separated list of whole numbers (" + min + " or more).");
        }
        markInvalid(field, false);
        return values;
    }

    /** Parses sizes such as "512", "4KB", "16 GB" into bytes. */
    public static long parseSize(String text) {
        Matcher matcher = SIZE_PATTERN.matcher(text);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid size: " + text);
        }
        double amount = Double.parseDouble(matcher.group(1));
        String unit = matcher.group(2) == null ? "B" : matcher.group(2).toUpperCase(Locale.ROOT);
        int power = Arrays.asList("B", "KB", "MB", "GB", "TB").indexOf(unit);
        return (long) (amount * Math.pow(1024, power));
    }

    /** Parses a size field such as "4KB" into bytes; null plus an error when invalid. */
    public static Long parseSizeField(JTextField field, String name, Component parent) {
        try {
            long bytes = parseSize(field.getText());
            if (bytes <= 0) {
                throw new IllegalArgumentException();
            }
            markInvalid(field, false);
            return bytes;
        } catch (IllegalArgumentException e) {
            return reject(field, parent, name + " must be a size like 4096, 4KB or 16GB.");
        }
    }

    /** Parses a comma-separated list of sizes ("100KB, 500KB") into bytes. */
    public static long[] parseSizeList(JTextField field, String name, Component parent) {
        String text = field.getText().trim();
        if (text.isEmpty()) {
            return reject(field, parent, name + " cannot be empty.");
        }
        String[] parts = text.split("\\s*,\\s*");
        long[] values = new long[parts.length];
        try {
            for (int i = 0; i < parts.length; i++) {
                values[i] = parseSize(parts[i]);
            }
        } catch (IllegalArgumentException e) {
            return reject(field, parent, name + " must be comma-separated sizes, e.g. 100KB, 500KB, 200KB.");
        }
        markInvalid(field, false);
        return values;
    }

    /** Formats bytes with the largest unit that divides evenly, e.g. 4096 -> "4 KB". */
    public static String formatSize(long bytes) {
        String[] units = {"B", "KB", "MB", "GB", "TB"};
        double value = bytes;
        int unit = 0;
        while (value >= 1024 && unit < units.length - 1 && value % 1024 == 0) {
            value /= 1024;
            unit++;
        }
        return (value == Math.floor(value) ? String.valueOf((long) value) : String.format("%.2f", value)) + " " + units[unit];
    }

    /** Marks the field invalid, shows the message, selects the text and returns null. */
    private static <T> T reject(JTextField field, Component parent, String message) {
        markInvalid(field, true);
        error(parent, message);
        field.requestFocusInWindow();
        field.selectAll();
        return null;
    }

    /** Standard "Check your input" error dialog. */
    public static void error(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Check your input", JOptionPane.ERROR_MESSAGE);
    }

    // ---------------------------------------------------------------- colors

    // Light-theme colours used by the custom-painted components below.
    static Color cardBackground() {
        Color base = UIManager.getColor("Panel.background");
        if (base == null) {
            base = Color.WHITE;
        }
        return mix(base, Color.WHITE, 0.7f);
    }

    static Color borderColor() {
        Color color = UIManager.getColor("Component.borderColor");
        if (color == null) {
            color = UIManager.getColor("Separator.foreground");
        }
        return color != null ? color : Color.LIGHT_GRAY;
    }

    static Color textColor() {
        Color color = UIManager.getColor("Label.foreground");
        return color != null ? color : Color.BLACK;
    }

    static Color mutedTextColor() {
        Color color = UIManager.getColor("Label.disabledForeground");
        return color != null ? color : Color.GRAY;
    }

    /** Blends a toward b; amount 0 gives a, amount 1 gives b. */
    static Color mix(Color a, Color b, float amount) {
        return new Color(
                Math.round(a.getRed() + (b.getRed() - a.getRed()) * amount),
                Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * amount),
                Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * amount));
    }

    /**
     * Gives a table a two-level header: group titles (e.g. "ALLOCATION") spanning several columns
     * above the individual column names. A group with a null title is drawn as a single tall cell
     * showing the column name.
     *
     * @param titles one title per group
     * @param spans  how many columns each group covers; must add up to the column count
     */
    public static void groupedHeader(JTable table, String[] titles, int[] spans) {
        GroupedHeader header = new GroupedHeader(table, titles, spans);
        table.setTableHeader(header);
        table.getTableHeader().setReorderingAllowed(false);
        table.getTableHeader().setResizingAllowed(false);
    }

    /** Table header that paints a group row above the normal column-name row. */
    private static final class GroupedHeader extends javax.swing.table.JTableHeader {
        private final String[] titles;
        private final int[] spans;

        GroupedHeader(JTable table, String[] titles, int[] spans) {
            super(table.getColumnModel());
            this.titles = titles;
            this.spans = spans;
            setOpaque(true);
        }

        @Override
        public Dimension getPreferredSize() {
            Dimension size = super.getPreferredSize();
            return new Dimension(size.width, 56);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            Color background = UIManager.getColor("TableHeader.background");
            g2.setColor(background != null ? background : getBackground());
            g2.fillRect(0, 0, getWidth(), getHeight());
            Color foreground = UIManager.getColor("TableHeader.foreground");
            g2.setFont(getFont().deriveFont(Font.BOLD));
            FontMetrics metrics = g2.getFontMetrics();
            int half = getHeight() / 2;

            int column = 0;
            for (int group = 0; group < titles.length && column < getColumnModel().getColumnCount(); group++) {
                int span = spans[group];
                Rectangle first = getHeaderRect(column);
                Rectangle last = getHeaderRect(Math.min(column + span - 1, getColumnModel().getColumnCount() - 1));
                int x = first.x;
                int width = last.x + last.width - first.x;
                g2.setColor(borderColor());
                if (titles[group] == null) {
                    drawCell(g2, metrics, foreground, getColumnModel().getColumn(column).getHeaderValue().toString(),
                            x, 0, width, getHeight());
                } else {
                    drawCell(g2, metrics, foreground, titles[group], x, 0, width, half);
                    for (int i = column; i < column + span; i++) {
                        Rectangle r = getHeaderRect(i);
                        drawCell(g2, metrics, foreground, getColumnModel().getColumn(i).getHeaderValue().toString(),
                                r.x, half, r.width, getHeight() - half);
                    }
                }
                column += span;
            }
            g2.dispose();
        }

        private void drawCell(Graphics2D g2, FontMetrics metrics, Color foreground, String text,
                              int x, int y, int width, int height) {
            g2.setColor(borderColor());
            g2.drawRect(x, y, width - 1, height - 1);
            g2.setColor(foreground != null ? foreground : textColor());
            g2.drawString(text, x + (width - metrics.stringWidth(text)) / 2,
                    y + (height + metrics.getAscent() - metrics.getDescent()) / 2);
        }
    }

    // ------------------------------------------------------------ components

    /** Rounded card with a title row and optional status hint on the right. */
    public static final class Section extends JPanel {
        private final JLabel hintLabel;

        public Section(String title, JComponent content) {
            this(title, null, content);
        }

        public Section(String title, String hint, JComponent content) {
            super(new BorderLayout(0, 12));
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(14, 16, 16, 16));

            JLabel titleLabel = new JLabel(title);
            styleClass(titleLabel, "h4", Font.BOLD, 1.05f);
            hintLabel = muted(hint == null ? "" : hint);

            JPanel head = transparent(new BorderLayout(12, 0));
            head.add(titleLabel, BorderLayout.WEST);
            head.add(hintLabel, BorderLayout.EAST);
            add(head, BorderLayout.NORTH);

            if (content != null) {
                add(content, BorderLayout.CENTER);
            }
        }

        public void setHint(String hint) {
            hintLabel.setText(hint);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(cardBackground());
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
            g2.setColor(borderColor());
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** Small metric tile: muted caption above a large value. */
    public static final class Stat extends JPanel {
        private final JLabel valueLabel;

        public Stat(String caption, String initialValue) {
            setOpaque(false);
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setBorder(new RoundedBorder(10, new Insets(10, 14, 10, 14)));

            JLabel captionLabel = muted(caption);
            captionLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            valueLabel = new JLabel(initialValue);
            styleClass(valueLabel, "h3", Font.BOLD, 1.3f);
            valueLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

            add(captionLabel);
            add(Box.createVerticalStrut(4));
            add(valueLabel);
            getAccessibleContext().setAccessibleName(caption);
        }

        public void setValue(String value) {
            setValue(value, null);
        }

        /** @param tone highlight color, or null for the default text color */
        public void setValue(String value, Color tone) {
            valueLabel.setText(value);
            valueLabel.setForeground(tone);
        }
    }

    /** Lays the given stat tiles out in one equal-width row. */
    public static JPanel stats(Stat... stats) {
        JPanel panel = transparent(new GridLayout(1, stats.length, 10, 0));
        for (Stat stat : stats) {
            panel.add(stat);
        }
        return panel;
    }

    public static final class RoundedBorder extends AbstractBorder {
        private final int arc;
        private final Insets padding;

        public RoundedBorder(int arc, Insets padding) {
            this.arc = arc;
            this.padding = padding;
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(borderColor());
            g2.drawRoundRect(x, y, width - 1, height - 1, arc, arc);
            g2.dispose();
        }

        @Override
        public Insets getBorderInsets(Component c) {
            return new Insets(padding.top, padding.left, padding.bottom, padding.right);
        }

        @Override
        public Insets getBorderInsets(Component c, Insets insets) {
            insets.set(padding.top, padding.left, padding.bottom, padding.right);
            return insets;
        }
    }

    /** One-pixel divider along the bottom edge, colored from the active theme. */
    public static final class BottomLineBorder extends AbstractBorder {
        private final Insets padding;

        public BottomLineBorder(Insets padding) {
            this.padding = padding;
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            g.setColor(borderColor());
            g.fillRect(x, y + height - 1, width, 1);
        }

        @Override
        public Insets getBorderInsets(Component c) {
            return new Insets(padding.top, padding.left, padding.bottom + 1, padding.right);
        }

        @Override
        public Insets getBorderInsets(Component c, Insets insets) {
            insets.set(padding.top, padding.left, padding.bottom + 1, padding.right);
            return insets;
        }
    }
}
