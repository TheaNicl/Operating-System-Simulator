import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

/**
 * Module 5: Segmentation.
 *
 * The user enters each segment's name, base address and limit in a table, then validates the
 * physical ranges against the configured RAM size.
 */
public class SegmentationPanel extends JPanel {
    private static final int MAX_SEGMENTS = 32;

    // RAM configuration and the editable segment table.
    private final JTextField ramSizeField;
    private final DefaultTableModel tableModel;
    private final JTable segTable;
    private final UIKit.Section tableSection;

    /** Builds the RAM configuration card and editable segment table. */
    public SegmentationPanel() {
        UIKit.setupModule(this);
        add(UIKit.header("Segmentation",
                "Define logical segments with a base address and limit inside physical memory."), BorderLayout.NORTH);

        ramSizeField = UIKit.field("32GB", "e.g. 32GB", 10);
        JButton runBtn = UIKit.primary("Run");
        runBtn.addActionListener(e -> run());

        JPanel configBody = UIKit.transparent(new BorderLayout(0, 12));
        configBody.add(UIKit.form(1, "Size of RAM", ramSizeField), BorderLayout.CENTER);
        configBody.add(UIKit.toolbar(
                UIKit.muted("Addresses and limits are in bytes; ranges must fit in RAM without overlap."),
                runBtn), BorderLayout.SOUTH);

        tableModel = UIKit.model(new String[]{"Segment #", "Name", "Base Address (B)", "Limit (B)"}, 1);
        tableModel.addRow(new Object[]{"0", "", "", ""});
        segTable = UIKit.table(tableModel);

        JButton addBtn = UIKit.secondary("Add segment");
        JButton removeBtn = UIKit.secondary("Remove selected");
        addBtn.addActionListener(e -> addSegment());
        removeBtn.addActionListener(e -> {
            UIKit.removeSelectedRows(segTable);
            renumberSegments();
        });

        JPanel tableBody = UIKit.transparent(new BorderLayout(0, 10));
        tableBody.add(UIKit.scroll(segTable), BorderLayout.CENTER);
        tableBody.add(UIKit.row(addBtn, removeBtn), BorderLayout.SOUTH);
        tableSection = new UIKit.Section("Segment Table", "Enter each segment, then press Run", tableBody);

        JPanel center = UIKit.transparent(new BorderLayout(0, 16));
        center.add(new UIKit.Section("Configuration", configBody), BorderLayout.NORTH);
        center.add(tableSection, BorderLayout.CENTER);
        add(center, BorderLayout.CENTER);
    }

    private void addSegment() {
        UIKit.stopEditing(segTable);
        if (tableModel.getRowCount() >= MAX_SEGMENTS) {
            UIKit.error(this, "A maximum of " + MAX_SEGMENTS + " segments is supported.");
            return;
        }
        tableModel.addRow(new Object[]{"", "", "", ""});
        renumberSegments();
    }

    private void renumberSegments() {
        for (int row = 0; row < tableModel.getRowCount(); row++) {
            tableModel.setValueAt(String.valueOf(row), row, 0);
        }
    }

    /** Validates user-entered segment ranges against the configured RAM size. */
    private void run() {
        Long ram = UIKit.parseSizeField(ramSizeField, "Size of RAM", this);
        if (ram == null) return;
        UIKit.stopEditing(segTable);
        if (tableModel.getRowCount() == 0) {
            UIKit.error(this, "Add at least one segment before running.");
            return;
        }

        long[][] ranges = new long[tableModel.getRowCount()][2];
        for (int row = 0; row < tableModel.getRowCount(); row++) {
            Object nameValue = tableModel.getValueAt(row, 1);
            if (nameValue == null || nameValue.toString().trim().isEmpty()) {
                rejectCell(row, 1, "Segment " + row + " must have a name.");
                return;
            }

            Long base = parseTableLong(row, 2);
            if (base == null) return;
            Long limit = parseTableLong(row, 3);
            if (limit == null) return;
            if (limit == 0) {
                rejectCell(row, 3, "Segment limit must be greater than zero.");
                return;
            }
            if (base > ram || limit > ram - base) {
                rejectCell(row, 2, "Segment " + row + " must fit inside RAM.");
                return;
            }

            long end = base + limit;
            for (int previous = 0; previous < row; previous++) {
                if (base < ranges[previous][1] && ranges[previous][0] < end) {
                    rejectCell(row, 2, "Segment " + row + " overlaps segment " + previous + ".");
                    return;
                }
            }
            ranges[row][0] = base;
            ranges[row][1] = end;
        }
        tableSection.setHint(tableModel.getRowCount() + " segments validated \u00b7 RAM " + UIKit.formatSize(ram));
    }

    private Long parseTableLong(int row, int column) {
        Object value = tableModel.getValueAt(row, column);
        String text = value == null ? "" : value.toString().trim();
        if (!text.matches("\\d+")) {
            rejectCell(row, column, tableModel.getColumnName(column) + " must be a whole number (0 or more).");
            return null;
        }
        try {
            return Long.parseLong(text);
        } catch (NumberFormatException e) {
            rejectCell(row, column, tableModel.getColumnName(column) + " is too large.");
            return null;
        }
    }

    private void rejectCell(int row, int column, String message) {
        segTable.changeSelection(row, column, false, false);
        segTable.requestFocusInWindow();
        UIKit.error(this, message);
    }

    /** The editable segment table model. */
    public DefaultTableModel getSegmentModel() {
        return tableModel;
    }
}
