import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class PageReplacementPanel extends JPanel {
    private final JTextField referenceField;
    private final JTextField framesField;
    private final ButtonGroup algorithmGroup = new ButtonGroup();
    private final JToggleButton fifoBtn;
    private final JToggleButton lruBtn;
    private final JToggleButton optimalBtn;
    private final DefaultTableModel traceModel;
    private final JTable traceTable;
    private final UIKit.Section traceSection;
    private final UIKit.Stat faultsStat = new UIKit.Stat("Page faults", "6");
    private final UIKit.Stat hitsStat = new UIKit.Stat("Page hits", "1");
    private final UIKit.Stat ratioStat = new UIKit.Stat("Hit ratio", "14.3%");

    public PageReplacementPanel() {
        UIKit.setupModule(this);
        add(UIKit.header("Page Replacement",
                "Trace how FIFO, LRU and Optimal fill frames for a page reference string."), BorderLayout.NORTH);

        referenceField = UIKit.field("1, 3, 0, 3, 5, 6, 3", "e.g. 7, 0, 1, 2, 0", 24);
        framesField = UIKit.field("3", null, 4);

        fifoBtn = UIKit.option("FIFO", algorithmGroup);
        lruBtn = UIKit.option("LRU", algorithmGroup);
        optimalBtn = UIKit.option("Optimal", algorithmGroup);
        fifoBtn.setSelected(true);

        JButton runBtn = UIKit.primary("Run");
        runBtn.addActionListener(e -> run());

        JPanel form = UIKit.form(2, "Page reference string", referenceField, "Frames", framesField);
        JPanel configBody = UIKit.transparent(new BorderLayout(0, 12));
        configBody.add(form, BorderLayout.CENTER);
        configBody.add(UIKit.toolbar(UIKit.row(UIKit.muted("Algorithm"), fifoBtn, lruBtn, optimalBtn), runBtn),
                BorderLayout.SOUTH);

        traceModel = UIKit.model(new String[]{"Ref", "1", "3", "0", "3", "5", "6", "3"}, false);
        traceModel.addRow(new Object[]{"Frame 1", "1", "1", "1", "1", "5", "5", "5"});
        traceModel.addRow(new Object[]{"Frame 2", "-", "3", "3", "3", "3", "6", "6"});
        traceModel.addRow(new Object[]{"Frame 3", "-", "-", "0", "0", "0", "0", "3"});
        traceModel.addRow(new Object[]{"Status", "Miss", "Miss", "Miss", "Hit", "Miss", "Miss", "Miss"});
        traceTable = UIKit.table(traceModel);
        traceTable.setDefaultRenderer(Object.class, new StatusRenderer());
        traceTable.setRowSelectionAllowed(false);

        JPanel traceBody = UIKit.transparent(new BorderLayout(0, 12));
        traceBody.add(UIKit.scroll(traceTable), BorderLayout.CENTER);
        traceBody.add(UIKit.stats(faultsStat, hitsStat, ratioStat), BorderLayout.SOUTH);
        traceSection = new UIKit.Section("Frame Trace", "Sample: FIFO \u00b7 3 frames", traceBody);

        JPanel center = UIKit.transparent(new BorderLayout(0, 16));
        center.add(new UIKit.Section("Configuration", configBody), BorderLayout.NORTH);
        center.add(traceSection, BorderLayout.CENTER);
        add(center, BorderLayout.CENTER);
    }

    private String selectedAlgorithm() {
        if (lruBtn.isSelected()) return "LRU";
        if (optimalBtn.isSelected()) return "Optimal";
        return "FIFO";
    }

    private void run() {
        int[] references = UIKit.parseIntList(referenceField, "Page reference string", 0, this);
        if (references == null) return;
        Integer frames = UIKit.parseInt(framesField, "Frames", 1, 20, this);
        if (frames == null) return;
        onRun(selectedAlgorithm(), references, frames);
    }

    /**
     * Hook for the replacement logic. Compute the trace (one column per reference,
     * one row per frame plus a final "Status" row) and pass it to {@link #showTrace}.
     */
    protected void onRun(String algorithm, int[] references, int frames) {
        traceSection.setHint(algorithm + " \u00b7 " + frames + " frames \u00b7 " + references.length + " references");
    }

    public void showTrace(String[] columns, Object[][] rows, int faults, int hits) {
        traceModel.setDataVector(rows, columns);
        int total = faults + hits;
        faultsStat.setValue(String.valueOf(faults));
        hitsStat.setValue(String.valueOf(hits));
        ratioStat.setValue(total == 0 ? "—" : String.format("%.1f%%", hits * 100.0 / total));
    }

    /** Colors the last row's Hit/Miss cells and emphasizes the row labels. */
    private static final class StatusRenderer extends DefaultTableCellRenderer {
        StatusRenderer() {
            setHorizontalAlignment(SwingConstants.CENTER);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean selected,
                                                       boolean focused, int row, int column) {
            super.getTableCellRendererComponent(table, value, selected, focused, row, column);
            String text = value == null ? "" : value.toString();
            setFont(table.getFont().deriveFont(column == 0 ? Font.BOLD : Font.PLAIN));
            if ("Hit".equalsIgnoreCase(text)) {
                setForeground(Theme.SUCCESS);
                setFont(getFont().deriveFont(Font.BOLD));
            } else if ("Miss".equalsIgnoreCase(text) || "Fault".equalsIgnoreCase(text)) {
                setForeground(Theme.DANGER);
                setFont(getFont().deriveFont(Font.BOLD));
            } else {
                setForeground(table.getForeground());
            }
            return this;
        }
    }
}
