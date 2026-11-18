import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

/**
 * Module 5: Segmentation.
 * 
 * The user sets the RAM size and the number of segments; the table lists each segment's name,
 * base address and limit.
 */
public class SegmentationPanel extends JPanel {
    // Names given to newly generated segments (later ones are "Segment N").
    private static final String[] DEFAULT_NAMES = {"Code", "Data", "Stack", "Heap"};

    // Inputs and the editable segment table.
    private final JTextField ramSizeField;
    private final JTextField segCountField;
    private final DefaultTableModel tableModel;
    private final JTable segTable;
    private final UIKit.Section tableSection;

    /** Builds the configuration card and an empty segment table. */
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
        configBody.add(UIKit.toolbar(UIKit.muted("RAM is divided evenly; base addresses and limits are in bytes."),
                generateBtn),
                BorderLayout.SOUTH);

        tableModel = UIKit.model(new String[]{"Segment #", "Name", "Base Address (B)", "Limit (B)"}, 1);
        segTable = UIKit.table(tableModel);
        tableSection = new UIKit.Section("Segment Table", "Run to generate segments", UIKit.scroll(segTable));

        JPanel center = UIKit.transparent(new BorderLayout(0, 16));
        center.add(new UIKit.Section("Configuration", configBody), BorderLayout.NORTH);
        center.add(tableSection, BorderLayout.CENTER);
        add(center, BorderLayout.CENTER);
    }

    /** Validates the inputs, then partitions RAM into contiguous, evenly sized segments. */
    private void generate() {
        Long ram = UIKit.parseSizeField(ramSizeField, "Size of RAM", this);
        if (ram == null) return;
        Integer count = UIKit.parseInt(segCountField, "Number of segments", 1, 32, this);
        if (count == null) return;
        if (count > ram) {
            UIKit.markInvalid(segCountField, true);
            UIKit.error(this, "Number of segments cannot exceed the RAM size in bytes.");
            segCountField.requestFocusInWindow();
            segCountField.selectAll();
            return;
        }

        UIKit.stopEditing(segTable);
        tableModel.setRowCount(0);
        long base = 0;
        long bytesPerSegment = ram / count;
        long remainder = ram % count;
        for (int i = 0; i < count; i++) {
            String name = i < DEFAULT_NAMES.length ? DEFAULT_NAMES[i] : "Segment " + i;
            long limit = bytesPerSegment + (i < remainder ? 1 : 0);
            tableModel.addRow(new Object[]{String.valueOf(i), name, String.valueOf(base), String.valueOf(limit)});
            base += limit;
        }
        tableSection.setHint(count + " segments \u00b7 RAM " + UIKit.formatSize(ram));
    }

    /** The generated segment table model. */
    public DefaultTableModel getSegmentModel() {
        return tableModel;
    }
}
