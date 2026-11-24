import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Module 4: Page Replacement.
 * 
 * Shows a frame-by-frame trace (one column per reference, one row per frame, plus a Hit/Miss row)
 * and fault/hit statistics for FIFO, LRU or Optimal.
 */
public class PageReplacementPanel extends JPanel {
    // Inputs: reference string, frame count and algorithm choice.
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
    private JButton runBtn;
    private Timer simulationTimer;
    private String[] simulationReferences;
    private int simulationFrames;
    private int simulationFrame;
    private int simulationStep;
    private int simulationHits;
    private int simulationFaults;
    private boolean simulationRunning = false;
    private int fifoPointer = 0;
    private int[] lastUsed;
    private String simulationAlgorithm;
    private int highlightedStep = -1;
    private int highlightedFrame = -1;

    /** Builds the configuration card, the frame trace table and the statistics tiles. */
    public PageReplacementPanel() {
        UIKit.setupModule(this);
        add(UIKit.header("Page Replacement",
                "Trace how FIFO, LRU and Optimal fill frames for a page reference string."), BorderLayout.NORTH);

        referenceField = UIKit.field("1, 3, 0, 3, 5, 6, 3", "e.g. A, B, page1, A", 24);
        framesField = UIKit.field("3", null, 4);

        fifoBtn = UIKit.option("FIFO", algorithmGroup);
        lruBtn = UIKit.option("LRU", algorithmGroup);
        optimalBtn = UIKit.option("Optimal", algorithmGroup);
        fifoBtn.setSelected(true);

        runBtn = UIKit.primary("Run");
        runBtn = UIKit.primary("Run");

        runBtn.addActionListener(e -> {
            if (simulationRunning) {
                showResults();
            } else {
                run();
            }
        });

        JPanel form = UIKit.form(2, "Page reference string", referenceField, "Frames", framesField);
        JPanel configBody = UIKit.transparent(new BorderLayout(0, 12));
        configBody.add(form, BorderLayout.CENTER);
        configBody.add(UIKit.toolbar(UIKit.row(UIKit.muted("Algorithm"), fifoBtn, lruBtn, optimalBtn), runBtn),
                BorderLayout.SOUTH);

        traceModel = UIKit.model(new String[]{"Ref", "-", "-", "-", "-", "-", "-", "-"}, false);
        traceTable = UIKit.table(new FrameTraceTable(traceModel));
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

    /** Name of the selected algorithm button. */
    private String selectedAlgorithm() {
        if (lruBtn.isSelected()) return "LRU";
        if (optimalBtn.isSelected()) return "Optimal";
        return "FIFO";
    }

    private String[] storedFrames;

    private void run() {
        String[] references = UIKit.parseStringList(referenceField, "Page reference string", this);
        if (references == null) return;
        Integer frames = UIKit.parseInt(framesField, "Frames", 1, 20, this);
        if (frames == null) return;

        storedFrames = new String[frames];
        fifoPointer = 0;

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

    protected void onRun(String algorithm, String[] references, int frames) {
        highlightedStep = -1;
        highlightedFrame = -1;
        traceTable.repaint();

        traceSection.setHint(
                algorithm + " · " + frames + " frames · "
                        + references.length + " references"
        );

        if (simulationTimer != null) {
            simulationTimer.stop();
        }

        simulationAlgorithm = algorithm;
        simulationReferences = references;
        simulationFrames = frames;

        simulationFrame = 0;
        simulationStep = 0;
        simulationHits = 0;
        simulationFaults = 0;

        fifoPointer = 0;

        // Track the last-used time of each frame (LRU)
        lastUsed = new int[frames];
        Arrays.fill(lastUsed, -1);

        simulationRunning = true;
        runBtn.setText("Show Results");

        faultsStat.setValue("-");
        hitsStat.setValue("-");
        ratioStat.setValue("-");

        simulationTimer = new Timer(750, e -> executeNextStep());
        simulationTimer.start();
    }

    private void executeNextStep() {
        if (simulationStep >= simulationReferences.length) {
            finishSimulation();
            return;
        }

        String currRef = simulationReferences[simulationStep];

        boolean hit;
        String[] previousFrames = storedFrames.clone();

        switch (simulationAlgorithm) {
            case "FIFO":
                hit = fifoStep(simulationFrames, simulationStep, currRef);
                break;

            case "LRU":
                hit = lruStep(simulationFrames, simulationStep, currRef);
                break;

            case "Optimal":
                hit = optimalStep(
                        simulationFrames,
                        simulationStep,
                        currRef
                );
                break;

            default:
                finishSimulation();
                return;
        }

        if (hit) {
            simulationHits++;
        } else {
            simulationFaults++;
        }

        traceModel.setValueAt(
                hit ? "HIT" : "MISS",
                simulationFrames,
                simulationStep + 1
        );

        // Detect the actual replaced frame
        highlightedFrame = -1;

        if (!hit) {
            for (int i = 0; i < simulationFrames; i++) {
                if (!java.util.Objects.equals(previousFrames[i], storedFrames[i])) {
                    highlightedFrame = i;
                    break;
                }
            }
        }

        highlightedStep = simulationStep;

        traceTable.repaint();

        simulationStep++;

        if (simulationStep >= simulationReferences.length) {
            finishSimulation();
        }
    }

    private void showResults() {
        if (!simulationRunning) return;

        // Stop the 750ms timer
        if (simulationTimer != null) {
            simulationTimer.stop();
        }

        // Execute all remaining steps immediately
        while (simulationStep < simulationReferences.length) {
            executeNextStep();
        }

        finishSimulation();
    }

    private void finishSimulation() {
        if (!simulationRunning) return;

        if (simulationTimer != null) {
            simulationTimer.stop();
        }

        // Display final statistics
        showTrace(simulationFaults, simulationHits);

        // Reset button state
        simulationRunning = false;
        runBtn.setText("Run");
    }

    public void showTrace(int faults, int hits) {
        int total = faults + hits;
        faultsStat.setValue(String.valueOf(faults));
        hitsStat.setValue(String.valueOf(hits));
        ratioStat.setValue(total == 0 ? "—" : String.format("%.1f%%", hits * 100.0 / total));
    }

    private void setTableReferences(String[] references) {
        String[] refRow = new String[references.length + 1];
        refRow[0] = "Ref";
        for (int i = 0; i < references.length + 1; i++) {
            if (i == 0) {
                continue;
            }

            refRow[i] = references[i - 1];
        }

        traceModel.setColumnIdentifiers(refRow);
    }

    private boolean fifoStep(int frames, int step, String currRef) {
        // Check for page hit
        for (String page : storedFrames) {
            if (currRef.equals(page)) {
                updateFrameTable(frames, step);
                return true;
            }
        }

        // Find an empty frame first
        int emptyFrame = findEmptyFrame(frames);

        if (emptyFrame != -1) {
            storedFrames[emptyFrame] = currRef;
        } else {
            // Replace the oldest page
            storedFrames[fifoPointer] = currRef;
            fifoPointer = (fifoPointer + 1) % frames;
        }

        updateFrameTable(frames, step);
        return false;
    }

    private boolean lruStep(int frames, int step, String currRef) {
        // Check for page hit
        for (int i = 0; i < frames; i++) {
            if (currRef.equals(storedFrames[i])) {
                lastUsed[i] = step;

                updateFrameTable(frames, step);
                return true;
            }
        }

        // Find an empty frame first
        int emptyFrame = findEmptyFrame(frames);

        if (emptyFrame != -1) {
            storedFrames[emptyFrame] = currRef;
            lastUsed[emptyFrame] = step;
        } else {
            // Find the least recently used frame
            int lruFrame = 0;

            for (int i = 1; i < frames; i++) {
                if (lastUsed[i] < lastUsed[lruFrame]) {
                    lruFrame = i;
                }
            }

            storedFrames[lruFrame] = currRef;
            lastUsed[lruFrame] = step;
        }

        updateFrameTable(frames, step);
        return false;
    }

    private boolean optimalStep(int frames, int step, String currRef) {
        // Check for page hit
        for (String page : storedFrames) {
            if (currRef.equals(page)) {
                updateFrameTable(frames, step);
                return true;
            }
        }

        // Find an empty frame first
        int emptyFrame = findEmptyFrame(frames);

        if (emptyFrame != -1) {
            storedFrames[emptyFrame] = currRef;
        } else {
            int optimalFrame = 0;
            int farthestUse = -1;

            // Find the page whose next use is farthest away
            for (int i = 0; i < frames; i++) {
                int nextUse = Integer.MAX_VALUE;

                for (int j = step + 1; j < simulationReferences.length; j++) {
                    if (simulationReferences[j].equals(storedFrames[i])) {
                        nextUse = j;
                        break;
                    }
                }

                // A page that will never be used again
                // should be replaced first
                if (nextUse == Integer.MAX_VALUE) {
                    optimalFrame = i;
                    break;
                }

                if (nextUse > farthestUse) {
                    farthestUse = nextUse;
                    optimalFrame = i;
                }
            }

            storedFrames[optimalFrame] = currRef;
        }

        updateFrameTable(frames, step);
        return false;
    }

    private int findEmptyFrame(int frames) {
        for (int i = 0; i < frames; i++) {
            if (storedFrames[i] == null) {
                return i;
            }
        }

        return -1;
    }

    private void updateFrameTable(int frames, int step) {
        for (int i = 0; i < frames; i++) {
            traceModel.setValueAt(
                    storedFrames[i] == null
                            ? "-"
                            : String.valueOf(storedFrames[i]),
                    i,
                    step + 1
            );
        }
    }

    /** Fills the viewport when columns fit and keeps their natural widths when they don't. */
    private static final class FrameTraceTable extends JTable {
        private boolean fillsViewport;

        FrameTraceTable(DefaultTableModel model) {
            super(model);
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            Container parent = getParent();
            if (!(parent instanceof JViewport)) {
                return true;
            }

            int preferredWidth = 0;
            for (int column = 0; column < getColumnCount(); column++) {
                preferredWidth += getColumnModel().getColumn(column).getPreferredWidth();
            }
            boolean shouldFill = preferredWidth <= parent.getWidth();
            if (shouldFill != fillsViewport) {
                fillsViewport = shouldFill;
                if (shouldFill) {
                    setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
                } else {
                    setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
                    for (int column = 0; column < getColumnCount(); column++) {
                        getColumnModel().getColumn(column).setWidth(
                                getColumnModel().getColumn(column).getPreferredWidth());
                    }
                }
            }
            return shouldFill;
        }
    }

    /** Colors the last row's Hit/Miss cells and emphasizes the row labels. */
    private final class StatusRenderer extends DefaultTableCellRenderer {

        StatusRenderer() {
            setHorizontalAlignment(SwingConstants.CENTER);
        }

        @Override
        public Component getTableCellRendererComponent(
                JTable table,
                Object value,
                boolean selected,
                boolean focused,
                int row,
                int column
        ) {
            super.getTableCellRendererComponent(
                    table, value, selected, focused, row, column
            );

            String text = value == null ? "" : value.toString();

            setFont(table.getFont().deriveFont(
                    column == 0 ? Font.BOLD : Font.PLAIN
            ));

            setForeground(table.getForeground());
            setBackground(table.getBackground());

            // Highlight only the actual replaced/inserted frame
            if (column == highlightedStep + 1
                    && row == highlightedFrame
                    && highlightedFrame != -1) {

                setBackground(new Color(34, 197, 94));
                setForeground(Color.WHITE);
                setFont(getFont().deriveFont(Font.BOLD));
            }

            // Preserve HIT/MISS text colors
            if ("HIT".equalsIgnoreCase(text)) {
                setForeground(Theme.SUCCESS);
                setFont(getFont().deriveFont(Font.BOLD));

            } else if ("MISS".equalsIgnoreCase(text)
                    || "FAULT".equalsIgnoreCase(text)) {

                setForeground(Theme.DANGER);
                setFont(getFont().deriveFont(Font.BOLD));
            }

            return this;
        }
    }
}
