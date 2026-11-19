import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.Color;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JButton;

/**
 * Module 3: Paging.
 * 
 * The user enters memory sizes, page size and a logical address plus a page table; the panel
 * splits the address into page number and offset and maps it to a physical address.
 */
public class PagingPanel extends JPanel {
    // Inputs: memory sizes, page size and the logical address to translate.
    private final JTextField logicalSizeField;
    private final JTextField physicalSizeField;
    private final JTextField pageSizeField;
    private final JTextField addressField;
    // Editable page table (page number -> frame number).
    private final DefaultTableModel tableModel;
    private final JTable pageTable;
    private final JPopupMenu pagesPopup = new JPopupMenu();
    private final JLabel pagesInfo = new JLabel();
    private final JPopupMenu framesPopup = new JPopupMenu();
    private final JLabel framesInfo = new JLabel();

    // Result tiles: page count, frame count, page/offset split and the physical address.
    private final UIKit.Stat pagesStat = new UIKit.Stat("Pages (logical)", "—");
    private final UIKit.Stat framesStat = new UIKit.Stat("Frames (physical)", "—");
    private final UIKit.Stat pageOffsetStat = new UIKit.Stat("Page / Offset", "—");
    private final UIKit.Stat physicalStat = new UIKit.Stat("Physical address", "—");

    /** Builds the configuration form, page table and translation result tiles. */
    public PagingPanel() {
        UIKit.setupModule(this);
        add(UIKit.header("Paging",
                "Translate a logical address into a physical address using the page table."), BorderLayout.NORTH);

        logicalSizeField = UIKit.field("16GB", "e.g. 16GB", 10);
        physicalSizeField = UIKit.field("16GB", "e.g. 16GB", 10);
        pageSizeField = UIKit.field("4KB", "e.g. 4KB", 10);
        addressField = UIKit.field("1024", "Address in bytes", 10);

        JPanel form = UIKit.form(2,
                "Logical memory size", logicalSizeField,
                "Physical memory size", physicalSizeField,
                "Page size", pageSizeField,
                "Logical address", addressField);
        JButton calcBtn = UIKit.primary("Calculate & map");
        calcBtn.addActionListener(e -> calculate());

        JPanel configBody = UIKit.transparent(new BorderLayout(0, 12));
        configBody.add(form, BorderLayout.CENTER);
        configBody.add(UIKit.toolbar(UIKit.muted("Sizes accept B, KB, MB, GB or TB."), calcBtn), BorderLayout.SOUTH);

        tableModel = UIKit.model(new String[]{"Page Number", "Frame Number"}, 1);
        int[] frames = {1, 3, 4, 0, 2};
        for (int page = 0; page < frames.length; page++) {
            tableModel.addRow(new Object[]{String.valueOf(page), String.valueOf(frames[page])});
        }
        pageTable = UIKit.table(tableModel);

        JButton addPageBtn = UIKit.secondary("Add page");
        JButton removePageBtn = UIKit.secondary("Remove last");
        addPageBtn.addActionListener(e -> {
            UIKit.stopEditing(pageTable);
            tableModel.addRow(new Object[]{String.valueOf(tableModel.getRowCount()), "0"});
        });
        removePageBtn.addActionListener(e -> {
            UIKit.stopEditing(pageTable);
            if (tableModel.getRowCount() > 0) {
                tableModel.removeRow(tableModel.getRowCount() - 1);
            }
        });

        JPanel tableBody = UIKit.transparent(new BorderLayout(0, 10));
        tableBody.add(UIKit.scroll(pageTable), BorderLayout.CENTER);
        tableBody.add(UIKit.row(addPageBtn, removePageBtn), BorderLayout.SOUTH);

        JPanel statsGrid = UIKit.transparent(new GridLayout(2, 2, 10, 10));
        statsGrid.add(pagesStat);
        statsGrid.add(framesStat);

        pagesInfo.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        pagesPopup.add(pagesInfo);
        framesInfo.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        framesPopup.add(framesInfo);

        pagesStat.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                pagesPopup.show(pagesStat, 30, pagesStat.getHeight());
            }

            @Override
            public void mouseExited(MouseEvent e) {
                pagesPopup.setVisible(false);
            }
        });

        framesStat.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                framesPopup.show(framesStat, 30, framesStat.getHeight());
            }

            @Override
            public void mouseExited(MouseEvent e) {
                framesPopup.setVisible(false);
            }
        });

        statsGrid.add(pageOffsetStat);
        statsGrid.add(physicalStat);
        JPanel translationBody = UIKit.transparent(new BorderLayout());
        translationBody.add(statsGrid, BorderLayout.NORTH);

        JPanel center = UIKit.transparent(new BorderLayout(0, 16));
        center.add(new UIKit.Section("Memory Configuration", configBody), BorderLayout.NORTH);
        center.add(UIKit.split(JSplitPane.HORIZONTAL_SPLIT,
                new UIKit.Section("Page Table", "Page \u2192 Frame", tableBody),
                new UIKit.Section("Address Translation", translationBody), 0.5), BorderLayout.CENTER);
        add(center, BorderLayout.CENTER);
    }

    /**
     * Validates all inputs, then computes the translation:
     * page = address / pageSize, offset = address % pageSize,
     * physical = frame * pageSize + offset. A page missing from the table is reported as a page fault.
     */
    private void calculate() {
        Long logical = UIKit.parseSizeField(logicalSizeField, "Logical memory size", this);
        if (logical == null) return;
        Long physical = UIKit.parseSizeField(physicalSizeField, "Physical memory size", this);
        if (physical == null) return;
        Long pageSize = UIKit.parseSizeField(pageSizeField, "Page size", this);
        if (pageSize == null) return;
        if (pageSize > logical || pageSize > physical) {
            UIKit.markInvalid(pageSizeField, true);
            UIKit.error(this, "Page size must be smaller than both memory sizes.");
            return;
        }

        long address;
        try {
            address = Long.parseLong(addressField.getText().trim());
            if (address < 0 || address >= logical) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            UIKit.markInvalid(addressField, true);
            UIKit.error(this, "Logical address must be a whole number between 0 and " + (logical - 1) + ".");
            return;
        }
        if (!UIKit.validateNumericTable(pageTable, 1, "Page table", this)) {
            return;
        }

        long page = address / pageSize;
        long offset = address % pageSize;

        // Calculate page sizes
        long pages = logical / pageSize;
        long frames = physical / pageSize;

        // Set value of stats
        pagesStat.setValue(formatBytes(pages));
        framesStat.setValue(formatBytes(frames));

        pagesInfo.setText(getByteFormat(pages));
        framesInfo.setText(getByteFormat(frames));

        pageOffsetStat.setValue(page + " / " + offset);

        int pageRow = -1;
        for (int row = 0; row < tableModel.getRowCount(); row++) {
            if (String.valueOf(page).equals(tableModel.getValueAt(row, 0).toString().trim())) {
                pageRow = row;
                break;
            }
        }
        if (pageRow < 0) {
            physicalStat.setValue("Page fault", Theme.DANGER);
            return;
        }

        long frame;
        try {
            frame = Long.parseLong(tableModel.getValueAt(pageRow, 1).toString().trim());
        } catch (NumberFormatException e) {
            pageTable.changeSelection(pageRow, 1, false, false);
            UIKit.error(this, "Frame number in the page table is too large.");
            return;
        }
        if (frame >= frames) {
            pageTable.changeSelection(pageRow, 1, false, false);
            UIKit.error(this, "Frame number must be between 0 and " + (frames - 1) + ".");
            return;
        }

        pageTable.changeSelection(pageRow, 1, false, false);
        physicalStat.setValue(String.format("%,d", frame * pageSize + offset), Theme.SUCCESS);
    }

    /**
     * Displays a computed translation.
     *
     * @param physicalAddress the translated address, or null to show a page fault
     */
    public void showResult(long pages, long frames, long page, long offset, Long physicalAddress) {
        pagesStat.setValue(String.format("%,d", pages));
        framesStat.setValue(String.format("%,d", frames));
        pageOffsetStat.setValue(page + " / " + offset);
        if (physicalAddress == null) {
            physicalStat.setValue("Page fault", Theme.DANGER);
            return;
        }
        if (page < pageTable.getRowCount()) {
            pageTable.changeSelection((int) page, 1, false, false);
        }
        physicalStat.setValue(String.format("%,d", physicalAddress), Theme.SUCCESS);
    }

    private int getBaseTwo(double num) {
        return (int) (Math.log(num) / Math.log(2));
    }

    public static String formatBytes(double bytes) {
        String[] units = {"B", "KB", "MB",
                "GB", "TB"};

        int unitIndex = 0;
        while (bytes >= 1024 && unitIndex < units.length - 1) {
            bytes /= 1024;
            unitIndex++;
        }

        String formatted = (bytes % 1 == 0)
                ? String.format("%,.0f", bytes)
                : String.format("%,.2f", bytes);

        return formatted + " " + units[unitIndex];
    }

    public String getByteFormat(double bytes) {
        StringBuilder info = new StringBuilder("<html>");

        info.append("<b>Base 2:</b> 2<sup>")
                .append(getBaseTwo(bytes))
                .append("</sup><br>");

        info.append("<b>Bytes:</b> ")
                .append(String.format("%,d", (long) bytes))
                .append(" B<br>");

        String[] units = {"Kilobytes", "Megabytes", "Gigabytes", "Terabytes"};
        String[] abbreviations = {"KB", "MB", "GB", "TB"};

        for (int i = 1; i <= 4; i++) {
            double value = bytes / Math.pow(1024, i);

            if (value >= 0.01) {
                String formatted = (value % 1 == 0)
                        ? String.format("%,.0f", value)
                        : String.format("%,.2f", value);

                info.append("<b>")
                        .append(units[i - 1])
                        .append(":</b> ")
                        .append(formatted)
                        .append(" ")
                        .append(abbreviations[i - 1])
                        .append("<br>");
            }
        }

        info.append("</html>");

        return info.toString();
    }
}