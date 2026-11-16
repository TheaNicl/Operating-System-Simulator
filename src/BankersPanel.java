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

    // Output: table with Allocation / Max / Need / Work and the safe sequence line
    private final DefaultTableModel outputModel;
    private final JLabel safeSequenceLabel;

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

        safeSequenceLabel = new JLabel("Run the check to compute a safe sequence.");
        UIKit.styleClass(safeSequenceLabel, "h4", Font.BOLD, 1.1f);

        JPanel resultBody = UIKit.transparent(new BorderLayout(0, 12));
        resultBody.add(UIKit.toolbar(availRow, calcBtn), BorderLayout.NORTH);
        resultBody.add(outputScroll, BorderLayout.CENTER);
        resultBody.add(safeSequenceLabel, BorderLayout.SOUTH);
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

        // Read the matrices and derive Need = Max - Allocation.
        int n = allocModel.getRowCount();
        int m = RESOURCES.length;
        int[][] alloc = new int[n][m];
        int[][] maxDemand = new int[n][m];
        int[][] need = new int[n][m];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < m; j++) {
                alloc[i][j] = UIKit.intAt(allocModel, i, j + 1);
                maxDemand[i][j] = UIKit.intAt(maxModel, i, j + 1);
                need[i][j] = maxDemand[i][j] - alloc[i][j];
                if (need[i][j] < 0) {
                    maxTable.changeSelection(i, j + 1, false, false);
                    UIKit.error(this, allocModel.getValueAt(i, 0) + " is allocated more " + RESOURCES[j]
                            + " than its maximum demand.");
                    return;
                }
            }
        }

        // Safety algorithm. workAtRun[i] stays null for processes that cannot finish.
        int[] work = {a, b, c};
        int[][] workAtRun = new int[n][];
        boolean[] finished = new boolean[n];
        List<String> sequence = new ArrayList<>();
        boolean progressed = true;
        while (sequence.size() < n && progressed) {
            progressed = false;
            for (int i = 0; i < n; i++) {
                if (!finished[i] && fits(need[i], work)) {
                    workAtRun[i] = work.clone();
                    for (int j = 0; j < m; j++) {
                        work[j] += alloc[i][j];
                    }
                    finished[i] = true;
                    sequence.add(allocModel.getValueAt(i, 0).toString());
                    progressed = true;
                }
            }
        }

        // Fill the output table: process | allocation | max | need | work
        outputModel.setRowCount(0);
        for (int i = 0; i < n; i++) {
            Object[] outputRow = new Object[1 + 4 * m];
            outputRow[0] = allocModel.getValueAt(i, 0);
            for (int j = 0; j < m; j++) {
                outputRow[1 + j] = String.valueOf(alloc[i][j]);
                outputRow[1 + m + j] = String.valueOf(maxDemand[i][j]);
                outputRow[1 + 2 * m + j] = String.valueOf(need[i][j]);
                outputRow[1 + 3 * m + j] = workAtRun[i] == null ? "—" : String.valueOf(workAtRun[i][j]);
            }
            outputModel.addRow(outputRow);
        }

        if (sequence.size() == n) {
            safeSequenceLabel.setText("SAFE SEQUENCE:  " + String.join("  \u2192  ", sequence));
            safeSequenceLabel.setForeground(Theme.SUCCESS);
        } else {
            safeSequenceLabel.setText("Unsafe state: no sequence lets every process finish.");
            safeSequenceLabel.setForeground(Theme.DANGER);
        }
    }

    /** True when every resource in {@code need} is available in {@code work}. */
    private static boolean fits(int[] need, int[] work) {
        for (int j = 0; j < need.length; j++) {
            if (need[j] > work[j]) {
                return false;
            }
        }
        return true;
    }
}
