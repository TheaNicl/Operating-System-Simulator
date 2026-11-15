import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class PageReplacementPanel extends JPanel {
    private final JTextField referenceField;
    private final JTextField framesField;
    private final ButtonGroup algorithmGroup = new ButtonGroup();
    private final JToggleButton fifoBtn;
    private final JToggleButton lruBtn;
    private final JToggleButton optimalBtn;
    private DefaultTableModel traceModel;
    private JTable traceTable;
    private final UIKit.Section traceSection;
    private final UIKit.Stat faultsStat = new UIKit.Stat("Page faults", "-");
    private final UIKit.Stat hitsStat = new UIKit.Stat("Page hits", "-");
    private final UIKit.Stat ratioStat = new UIKit.Stat("Hit ratio", "-");

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

        traceModel = UIKit.model(new String[]{"Ref", "-", "-", "-", "-", "-", "-", "-"}, false);
        traceTable = UIKit.table(traceModel);
        traceTable.setDefaultRenderer(Object.class, new StatusRenderer());
        traceTable.setRowSelectionAllowed(false);

        JPanel traceBody = UIKit.transparent(new BorderLayout(0, 12));
        JScrollPane scrollPane = new JScrollPane(traceTable);

        scrollPane.setHorizontalScrollBarPolicy(
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
        );

        scrollPane.setVerticalScrollBarPolicy(
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
        );

        traceBody.add(scrollPane, BorderLayout.CENTER);
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

    private int[] storedFrames;
    private void run() {
        int[] references = UIKit.parseIntList(referenceField, "Page reference string", 0, this);
        if (references == null) return;
        Integer frames = UIKit.parseInt(framesField, "Frames", 1, 20, this);
        if (frames == null) return;

        storedFrames = new int[frames];

        setTableReferences(references);
        traceModel.setRowCount(0);

        // Add empty rows
        String[] emptyRow = new String[references.length + 1];
        Arrays.fill(emptyRow, 1, emptyRow.length, "-");
        for (int i = 0; i < frames; i++) {
            emptyRow[0] = "Frame " + (i + 1);
            traceModel.addRow(emptyRow);
        }

        emptyRow[0] = "Status";
        traceModel.addRow(emptyRow);

        onRun(selectedAlgorithm(), references, frames);
    }

    /**
     * Hook for the replacement logic. Compute the trace (one column per reference,
     * one row per frame plus a final "Status" row) and pass it to {@link #showTrace}.
     */
    protected void onRun(String algorithm, int[] references, int frames) {
        traceSection.setHint(algorithm + " \u00b7 " + frames + " frames \u00b7 " + references.length + " references");
        int[] hits = {0};
        int[] faults = {0};

        if (algorithm.equals("FIFO")) {
            final int[] frame = {0};
            final int[] step = {0};

            Timer timer = new Timer(750, e -> {
                if (step[0] >= references.length) {
                    ((Timer) e.getSource()).stop();

                    // Show stats after calculation
                    showTrace(hits[0], faults[0]);

                    return;
                }

                int currRef = references[step[0]];

                boolean hit = fifoStep(frames, frame[0], step[0], currRef);

                // Set status
                if (hit) hits[0]++;
                else     faults[0]++;
                traceModel.setValueAt((hit ? "HIT" : "MISS"), frames, step[0] + 1);

                frame[0]++;
                step[0]++;

                if (frame[0] == frames) {
                    frame[0] = 0;
                }
            });

            faultsStat.setValue("-");
            hitsStat.setValue("-");
            ratioStat.setValue("-");
            timer.start();
        }
    }

    public void showTrace(int faults, int hits) {
        int total = faults + hits;
        faultsStat.setValue(String.valueOf(faults));
        hitsStat.setValue(String.valueOf(hits));
        ratioStat.setValue(total == 0 ? "—" : String.format("%.1f%%", hits * 100.0 / total));
    }

    private void setTableReferences(int[] references) {
        String[] refRow = new String[references.length + 1];
        refRow[0] = "Ref";
        for (int i = 0; i < references.length + 1; i++) {
            if (i == 0) {
                continue;
            }

            refRow[i] = Integer.toString(references[i - 1]);
        }

        traceModel.setColumnIdentifiers(refRow);
    }

    private boolean fifoStep(int frames, int frame, int step, int currRef) {
        // Check if curr ref exists in frame
        boolean shouldHit = false;
        for (int f : storedFrames) {
            if (f == currRef) {
                shouldHit = true;
                break;
            }
        }

        if (!shouldHit) storedFrames[frame] = currRef;

        // Keep empty frames empty
        if (step < frames) {
            traceModel.setValueAt(currRef, frame, step + 1);

            if (frame - 1 < 0) return false;

            // Set previous frames also
            for (frame = frame; frame >= 0; frame--) {
                traceModel.setValueAt(storedFrames[frame], frame, step + 1);
            }

            return false;
        }

        for (frame = 0; frame < frames; frame++) {
            traceModel.setValueAt(storedFrames[frame], frame, step + 1);
        }

        return shouldHit;
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
