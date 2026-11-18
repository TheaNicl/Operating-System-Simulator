import java.awt.*;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Module 5: Segmentation.
 *
 * The user enters each segment's name, base address and limit in a table. A successful validation
 * draws the occupied and free physical-memory ranges beside the input table.
 */
public class SegmentationPanel extends JPanel {
    private static final int MAX_SEGMENTS = 32;
    private static final long DEFAULT_MAP_CHUNK_BYTES = 4L * 1024 * 1024 * 1024;
    private static final int MAX_MAP_CHUNKS = 1024;

    // RAM configuration, editable inputs and the read-only physical map.
    private final JTextField ramSizeField;
    private final DefaultTableModel tableModel;
    private final JTable segTable;
    private final UIKit.Section tableSection;
    private final DefaultTableModel mapModel;
    private final UIKit.Section mapSection;

    /** Builds the RAM configuration card, editable segment table and physical-memory map. */
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
                UIKit.muted("Enter B, KB, MB, GB or TB (e.g. 4KB = 4096B); ranges must fit in RAM without overlap."),
                runBtn), BorderLayout.SOUTH);

        tableModel = UIKit.model(new String[]{"Segment #", "Name", "Base Address", "Limit"}, 1);
        Object[] newSegment = getNewSegment(0);
        tableModel.addRow(newSegment);
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

        mapModel = UIKit.model(new String[]{"Address Range", "Allocation", "Size"}, false);
        JTable mapTable = UIKit.table(mapModel);
        mapSection = new UIKit.Section("Physical Memory Map", "Run to draw the validated layout",
                UIKit.scroll(mapTable));

        JPanel center = UIKit.transparent(new BorderLayout(0, 16));
        center.add(new UIKit.Section("Configuration", configBody), BorderLayout.NORTH);
        center.add(UIKit.split(JSplitPane.HORIZONTAL_SPLIT, tableSection, mapSection, 0.5), BorderLayout.CENTER);
        add(center, BorderLayout.CENTER);
    }

    private void addSegment() {
        UIKit.stopEditing(segTable);
        if (tableModel.getRowCount() >= MAX_SEGMENTS) {
            UIKit.error(this, "A maximum of " + MAX_SEGMENTS + " segments is supported.");
            return;
        }
        tableModel.addRow(getNewSegment(tableModel.getRowCount()));
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
        List<SegmentRange> segments = new ArrayList<>();
        for (int row = 0; row < tableModel.getRowCount(); row++) {
            Object nameValue = tableModel.getValueAt(row, 1);
            if (nameValue == null || nameValue.toString().trim().isEmpty()) {
                rejectCell(row, 1, "Segment " + row + " must have a name.");
                return;
            }

            Long base = parseTableSize(row, 2);
            if (base == null) return;
            Long limit = parseTableSize(row, 3);
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
            segments.add(new SegmentRange(row, nameValue.toString().trim(), base, end));
        }
        drawMemoryMap(ram, segments);
        tableSection.setHint(tableModel.getRowCount() + " segments validated; values converted to bytes \u00b7 RAM "
                + UIKit.formatSize(ram));
    }

    private void drawMemoryMap(long ram, List<SegmentRange> segments) {
        segments.sort(Comparator.comparingLong(segment -> segment.base));
        long chunkBytes = chooseMapChunkSize(ram);
        mapModel.setRowCount(0);

        long cursor = 0;
        for (SegmentRange segment : segments) {
            if (segment.base > cursor) {
                addMapRows(cursor, segment.base, "Free", chunkBytes);
            }
            addMapRows(segment.base, segment.end,
                    "Segment " + segment.number + " (" + segment.name + ")", chunkBytes);
            cursor = segment.end;
        }
        if (cursor < ram) {
            addMapRows(cursor, ram, "Free", chunkBytes);
        }
        mapSection.setHint(mapModel.getRowCount() + " address ranges \u00b7 " + UIKit.formatSize(ram));
    }

    private long chooseMapChunkSize(long ram) {
        long chunkBytes = DEFAULT_MAP_CHUNK_BYTES;
        // Keep large maps bounded; exact segment boundaries are added independently.
        while ((ram - 1) / chunkBytes + 1 > MAX_MAP_CHUNKS) {
            chunkBytes *= 2;
        }
        return chunkBytes;
    }

    private void addMapRows(long start, long end, String allocation, long chunkBytes) {
        long address = start;
        while (address < end) {
            long distanceToChunk = chunkBytes - address % chunkBytes;
            long remaining = end - address;
            long nextAddress = remaining <= distanceToChunk ? end : address + distanceToChunk;
            mapModel.addRow(new Object[]{
                    formatRange(address, nextAddress),
                    allocation,
                    UIKit.formatSize(nextAddress - address)
            });
            address = nextAddress;
        }
    }

    private String formatRange(long start, long end) {
        String startLabel;
        if (start == 0) {
            String endLabel = UIKit.formatSize(end);
            startLabel = "0 " + endLabel.substring(endLabel.lastIndexOf(' ') + 1);
        } else {
            startLabel = UIKit.formatSize(start);
        }
        return startLabel + " - " + UIKit.formatSize(end);
    }

    private Long parseTableSize(int row, int column) {
        Object value = tableModel.getValueAt(row, column);
        String text = value == null ? "" : value.toString().trim();
        try {
            return UIKit.parseSize(text);
        } catch (IllegalArgumentException e) {
            rejectCell(row, column, tableModel.getColumnName(column)
                    + " must be a size like 4096, 4KB, 4MB, 4GB or 1TB.");
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

    /** The read-only physical-memory map generated by a successful run. */
    public DefaultTableModel getMemoryMapModel() {
        return mapModel;
    }

    private Object[] getNewSegment(int index) {
        String segmentName = "Segment " + index;
        return new Object[]{String.valueOf(index), segmentName, 0, 100};
    }

    private static final class SegmentRange {
        private final int number;
        private final String name;
        private final long base;
        private final long end;

        private SegmentRange(int number, String name, long base, long end) {
            this.number = number;
            this.name = name;
            this.base = base;
            this.end = end;
        }
    }
}
