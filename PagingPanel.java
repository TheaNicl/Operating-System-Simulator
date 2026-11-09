import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class PagingPanel extends JPanel {
    private final JTextField logicalSizeField;
    private final JTextField physicalSizeField;
    private final JTextField pageSizeField;
    private final JTextField addressField;
    private final DefaultTableModel tableModel;
    private final JTable pageTable;

    private final UIKit.Stat pagesStat = new UIKit.Stat("Pages (logical)", "—");
    private final UIKit.Stat framesStat = new UIKit.Stat("Frames (physical)", "—");
    private final UIKit.Stat pageOffsetStat = new UIKit.Stat("Page / Offset", "—");
    private final UIKit.Stat physicalStat = new UIKit.Stat("Physical address", "—");

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
        pagesStat.setValue(String.format("%,d", logical / pageSize));
        framesStat.setValue(String.format("%,d", physical / pageSize));
        pageOffsetStat.setValue(page + " / " + offset);

        if (page >= tableModel.getRowCount()) {
            physicalStat.setValue("Page fault", Theme.DANGER);
            return;
        }
        long frame = UIKit.intAt(tableModel, (int) page, 1);
        pageTable.changeSelection((int) page, 1, false, false);
        physicalStat.setValue(String.format("%,d", frame * pageSize + offset), Theme.SUCCESS);
    }
}
