import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Module 2: Banker's Algorithm.
 *
 * <p>The user edits the Allocation and Maximum matrices and the Available resources. Pressing
 * "Check safety" fills an output table (Allocation, Max, Need and Work for every process) and
 * prints the safe sequence underneath, or reports an unsafe state.</p>
 */
public class BankersPanel extends JPanel {
    private static final String[] RESOURCES = {"A", "B", "C"};

    // Input matrices
    private final DefaultTableModel allocModel;
    private final DefaultTableModel maxModel;
    private final JTable allocTable;
    private final JTable maxTable;

    // Available resources entered by the user
    private final JTextField availAField;
    private final JTextField availBField;
    private final JTextField availCField;

    // Output: table with Allocation / Max / Need / Work and the safe sequence
    private final DefaultTableModel outputModel;
    private final JTextArea safeSequenceText;

    public BankersPanel() {
        UIKit.setupModule(this);
        add(UIKit.header("Banker's Algorithm",
                "Check whether the current allocation leaves the system in a safe state."), BorderLayout.NORTH);

        // --- input matrices (the Process column is read-only) ---
        String[] columns = {"Process", "A", "B", "C"};
        allocModel = UIKit.model(columns, 1);
        maxModel = UIKit.model(columns, 1);
        int[][] alloc = {{0, 1, 0}, {2, 0, 0}, {3, 0, 2}, {2, 1, 1}, {0, 0, 2}};
        int[][] max = {{7, 5, 3}, {3, 2, 2}, {9, 0, 2}, {2, 2, 2}, {4, 3, 3}};
        for (int i = 0; i < alloc.length; i++) {
            allocModel.addRow(row("P" + (i + 1), alloc[i]));
            maxModel.addRow(row("P" + (i + 1), max[i]));
        }
        allocTable = UIKit.table(allocModel);
        maxTable = UIKit.table(maxModel);

        UIKit.Section allocSection = new UIKit.Section("Allocation", "Currently held", UIKit.scroll(allocTable));
        UIKit.Section maxSection = new UIKit.Section("Maximum", "Max demand", UIKit.scroll(maxTable));

        JButton addBtn = UIKit.secondary("Add process");
        JButton removeBtn = UIKit.secondary("Remove last");
        addBtn.addActionListener(e -> addProcess());
        removeBtn.addActionListener(e -> removeLastProcess());

        JPanel matrices = UIKit.transparent(new BorderLayout(0, 10));
        matrices.add(UIKit.split(JSplitPane.HORIZONTAL_SPLIT, allocSection, maxSection, 0.5), BorderLayout.CENTER);
        matrices.add(UIKit.row(addBtn, removeBtn), BorderLayout.SOUTH);

        // --- available resources + check button ---
        availAField = UIKit.field("3", null, 4);
        availBField = UIKit.field("3", null, 4);
        availCField = UIKit.field("2", null, 4);
        JButton calcBtn = UIKit.primary("Check safety");
        calcBtn.addActionListener(e -> calculate());

        JLabel aLabel = new JLabel("A");
        JLabel bLabel = new JLabel("B");
        JLabel cLabel = new JLabel("C");
        aLabel.setLabelFor(availAField);
        bLabel.setLabelFor(availBField);
        cLabel.setLabelFor(availCField);
        JPanel availRow = UIKit.row(aLabel, availAField, Box.createHorizontalStrut(8),
                bLabel, availBField, Box.createHorizontalStrut(8), cLabel, availCField);

        // --- output table: grouped header "ALLOCATION | MAX | NEED | WORK" over A, B, C ---
        String[] outputColumns = {"# Process", "A", "B", "C", "A", "B", "C", "A", "B", "C", "A", "B", "C"};
        outputModel = UIKit.model(outputColumns, false);
        JTable outputTable = UIKit.table(outputModel);
        outputTable.setRowSelectionAllowed(false);
        UIKit.groupedHeader(outputTable, new String[]{null, "ALLOCATION", "MAX", "NEED", "WORK"},
                new int[]{1, 3, 3, 3, 3});
        outputTable.getColumnModel().getColumn(0).setPreferredWidth(90);
        JScrollPane outputScroll = UIKit.scroll(outputTable);

        safeSequenceText = new JTextArea("Run the check to compute a safe sequence.", 2, 20);
        safeSequenceText.setEditable(false);
        safeSequenceText.setLineWrap(true);
        safeSequenceText.setWrapStyleWord(true);
        safeSequenceText.setOpaque(false);
        UIKit.styleClass(safeSequenceText, "h4", Font.BOLD, 1.1f);
        JScrollPane sequenceScroll = UIKit.scroll(safeSequenceText);
        sequenceScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        sequenceScroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        sequenceScroll.setBorder(BorderFactory.createEmptyBorder());

        JPanel resultBody = UIKit.transparent(new BorderLayout(0, 12));
        resultBody.add(UIKit.toolbar(availRow, calcBtn), BorderLayout.NORTH);
        resultBody.add(outputScroll, BorderLayout.CENTER);
        resultBody.add(sequenceScroll, BorderLayout.SOUTH);
        UIKit.Section resultSection = new UIKit.Section("Available Resources & Result",
                "Work = resources available when the process runs", resultBody);

        add(UIKit.split(JSplitPane.VERTICAL_SPLIT, matrices, resultSection, 0.4), BorderLayout.CENTER);
    }

    /** Builds a table row: the process name followed by one text cell per resource. */
    private static Object[] row(String name, int[] values) {
        Object[] row = new Object[values.length + 1];
        row[0] = name;
        for (int i = 0; i < values.length; i++) {
            row[i + 1] = String.valueOf(values[i]);
        }
        return row;
    }

    /** Adds a zero-filled process to both matrices. */
    private void addProcess() {
        UIKit.stopEditing(allocTable);
        UIKit.stopEditing(maxTable);
        String name = "P" + (allocModel.getRowCount() + 1);
        allocModel.addRow(row(name, new int[RESOURCES.length]));
        maxModel.addRow(row(name, new int[RESOURCES.length]));
    }

    /** Removes the last process from both matrices (at least one is kept). */
    private void removeLastProcess() {
        UIKit.stopEditing(allocTable);
        UIKit.stopEditing(maxTable);
        if (allocModel.getRowCount() > 1) {
            allocModel.removeRow(allocModel.getRowCount() - 1);
            maxModel.removeRow(maxModel.getRowCount() - 1);
        }
    }

    /**
     * Validates the inputs, runs the safety algorithm and shows the output table.
     *
     * <p>Need = Max - Allocation. Starting with Work = Available, any unfinished process whose
     * Need fits inside Work can finish; it then returns its Allocation to Work. The "Work" column
     * shows the Work vector at the moment each process was picked. Processes that never get to
     * run (unsafe state) show a dash.</p>
     */
    private void calculate() {
        if (!UIKit.validateNumericTable(allocTable, 1, "Allocation", this)
                || !UIKit.validateNumericTable(maxTable, 1, "Maximum", this)) {
            return;
        }
        Integer a = UIKit.parseInt(availAField, "Available A", 0, 100000, this);
        if (a == null) return;
        Integer b = UIKit.parseInt(availBField, "Available B", 0, 100000, this);
        if (b == null) return;
        Integer c = UIKit.parseInt(availCField, "Available C", 0, 100000, this);
        if (c == null) return;

        int n = allocModel.getRowCount();
        int m = RESOURCES.length;
        int[][] alloc = new int[n][m];
        int[][] maxDemand = new int[n][m];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < m; j++) {
                alloc[i][j] = UIKit.intAt(allocModel, i, j + 1);
                maxDemand[i][j] = UIKit.intAt(maxModel, i, j + 1);
                if (maxDemand[i][j] < alloc[i][j]) {
                    maxTable.changeSelection(i, j + 1, false, false);
                    UIKit.error(this, allocModel.getValueAt(i, 0) + " is allocated more " + RESOURCES[j]
                            + " than its maximum demand.");
                    return;
                }
            }
        }
        onCalculate(new int[]{a, b, c}, alloc, maxDemand);
    }

    /**
     * Runs the safety algorithm on the validated inputs, then displays its result. Rows of
     * {@code alloc} / {@code maxDemand} follow the table row order.
     */
    protected void onCalculate(int[] available, int[][] alloc, int[][] maxDemand) {
        int processCount = alloc.length;
        int resourceCount = RESOURCES.length;
        int[][] need = new int[processCount][resourceCount];
        long[][] workAtRun = new long[processCount][];
        boolean[] finished = new boolean[processCount];
        long[] work = new long[resourceCount];
        List<String> safeSequence = new ArrayList<>();

        for (int resource = 0; resource < resourceCount; resource++) {
            work[resource] = available[resource];
        }
        for (int process = 0; process < processCount; process++) {
            for (int resource = 0; resource < resourceCount; resource++) {
                need[process][resource] = maxDemand[process][resource] - alloc[process][resource];
            }
        }

        int finishedCount = 0;
        boolean progress;
        do {
            progress = false;
            for (int process = 0; process < processCount; process++) {
                if (finished[process] || !canFinish(need[process], work)) {
                    continue;
                }

                workAtRun[process] = work.clone();
                for (int resource = 0; resource < resourceCount; resource++) {
                    work[resource] += alloc[process][resource];
                }
                finished[process] = true;
                finishedCount++;
                safeSequence.add(String.valueOf(allocModel.getValueAt(process, 0)));
                progress = true;
            }
        } while (progress && finishedCount < processCount);

        showLongWorkResult(alloc, maxDemand, need, workAtRun,
                finishedCount == processCount ? safeSequence : null);
    }

    private static boolean canFinish(int[] need, long[] work) {
        for (int resource = 0; resource < need.length; resource++) {
            if (need[resource] > work[resource]) {
                return false;
            }
        }
        return true;
    }

    /**
     * Displays a computed result.
     *
     * @param need        Need matrix (Max - Allocation), one row per process
     * @param workAtRun   Work vector at the moment each process runs; a null row means it never finished
     * @param safeSequence process names in safe order, or null when the state is unsafe
     */
    public void showResult(int[][] alloc, int[][] maxDemand, int[][] need, int[][] workAtRun,
                           List<String> safeSequence) {
        long[][] longWorkAtRun = new long[workAtRun.length][];
        for (int process = 0; process < workAtRun.length; process++) {
            if (workAtRun[process] != null) {
                longWorkAtRun[process] = new long[workAtRun[process].length];
                for (int resource = 0; resource < workAtRun[process].length; resource++) {
                    longWorkAtRun[process][resource] = workAtRun[process][resource];
                }
            }
        }
        showLongWorkResult(alloc, maxDemand, need, longWorkAtRun, safeSequence);
    }

    private void showLongWorkResult(int[][] alloc, int[][] maxDemand, int[][] need, long[][] workAtRun,
                                    List<String> safeSequence) {
        int n = allocModel.getRowCount();
        int m = RESOURCES.length;
        outputModel.setRowCount(0);
        for (int i = 0; i < n; i++) {
            Object[] outputRow = new Object[1 + 4 * m];
            outputRow[0] = allocModel.getValueAt(i, 0);
            for (int j = 0; j < m; j++) {
                outputRow[1 + j] = String.valueOf(alloc[i][j]);
                outputRow[1 + m + j] = String.valueOf(maxDemand[i][j]);
                outputRow[1 + 2 * m + j] = String.valueOf(need[i][j]);
                outputRow[1 + 3 * m + j] = workAtRun[i] == null ? "\u2014" : String.valueOf(workAtRun[i][j]);
            }
            outputModel.addRow(outputRow);
        }
        if (safeSequence != null) {
            safeSequenceText.setText("SAFE SEQUENCE:  " + String.join("  \u2192  ", safeSequence));
            safeSequenceText.setForeground(Theme.SUCCESS);
        } else {
            safeSequenceText.setText("Unsafe state: no sequence lets every process finish.");
            safeSequenceText.setForeground(Theme.DANGER);
        }
        safeSequenceText.setCaretPosition(0);
    }
}
