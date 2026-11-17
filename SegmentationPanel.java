import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class SegmentationPanel extends JPanel {
    private static final String[] DEFAULT_NAMES = {"Code", "Data", "Stack", "Heap"};

    private final JTextField ramSizeField;
    private final JTextField segCountField;
    private final DefaultTableModel tableModel;
    private final JTable segTable;
    private final UIKit.Section tableSection;

    public SegmentationPanel() {
        UIKit.setupModule(this);
        add(UIKit.header("Segmentation",
                "Define logical segments with a base address and limit inside physical memory."), BorderLayout.NORTH);

        ramSizeField = UIKit.field("32GB", "e.g. 32GB", 10);
        segCountField = UIKit.field("3", "1 – 32", 6);
        JButton generateBtn = UIKit.primary("Generate segment table");
        generateBtn.addActionListener(e -> generate());

        JPanel configBody = UIKit.transparent(new BorderLayout(0, 12));
        configBody.add(UIKit.form(2, "Size of RAM", ramSizeField, "Number of segments", segCountField),
                BorderLayout.CENTER);
        configBody.add(UIKit.toolbar(UIKit.muted("Existing rows are kept when you change the count."), generateBtn),
                BorderLayout.SOUTH);

        tableModel = UIKit.model(new String[]{"Segment #", "Name", "Base Address", "Limit"}, 1);
        tableModel.addRow(new Object[]{"0", "Code", "1000", "400"});
        tableModel.addRow(new Object[]{"1", "Data", "2000", "500"});
        tableModel.addRow(new Object[]{"2", "Stack", "3000", "300"});
        segTable = UIKit.table(tableModel);
        tableSection = new UIKit.Section("Segment Table", "Double-click a cell to edit", UIKit.scroll(segTable));

        JPanel center = UIKit.transparent(new BorderLayout(0, 16));
        center.add(new UIKit.Section("Configuration", configBody), BorderLayout.NORTH);
        center.add(tableSection, BorderLayout.CENTER);
        add(center, BorderLayout.CENTER);
    }

    private void generate() {
        Long ram = UIKit.parseSizeField(ramSizeField, "Size of RAM", this);
        if (ram == null) return;
        Integer count = UIKit.parseInt(segCountField, "Number of segments", 1, 32, this);
        if (count == null) return;

        UIKit.stopEditing(segTable);
        while (tableModel.getRowCount() > count) {
            tableModel.removeRow(tableModel.getRowCount() - 1);
        }
        for (int i = tableModel.getRowCount(); i < count; i++) {
            String name = i < DEFAULT_NAMES.length ? DEFAULT_NAMES[i] : "Segment " + i;
            tableModel.addRow(new Object[]{String.valueOf(i), name, "", ""});
        }
        tableSection.setHint(count + " segments \u00b7 RAM " + UIKit.formatSize(ram));
        onGenerate(ram, count);
    }

    /** Hook for allocation logic, e.g. computing base addresses or checking for overlaps. */
    protected void onGenerate(long ramBytes, int segmentCount) {
    }

    public DefaultTableModel getSegmentModel() {
        return tableModel;
    }
}
