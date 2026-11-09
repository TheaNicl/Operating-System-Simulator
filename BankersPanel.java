import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class BankersPanel extends JPanel {
    private static final String[] RESOURCES = {"A", "B", "C"};

    private final DefaultTableModel allocModel;
    private final DefaultTableModel maxModel;
    private final JTable allocTable;
    private final JTable maxTable;
    private final JTextField availAField;
    private final JTextField availBField;
    private final JTextField availCField;
    private final JLabel safeSequenceLabel;

    public BankersPanel() {
        UIKit.setupModule(this);
        add(UIKit.header("Banker's Algorithm",
                "Check whether the current allocation leaves the system in a safe state."), BorderLayout.NORTH);

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
        add(matrices, BorderLayout.CENTER);

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

        safeSequenceLabel = new JLabel("Run the check to compute a safe sequence.");
        UIKit.styleClass(safeSequenceLabel, "h4", Font.BOLD, 1.1f);

        JPanel resultBody = UIKit.transparent(new BorderLayout(0, 12));
        resultBody.add(UIKit.toolbar(availRow, calcBtn), BorderLayout.NORTH);
        resultBody.add(safeSequenceLabel, BorderLayout.CENTER);
        add(new UIKit.Section("Available Resources & Result", resultBody), BorderLayout.SOUTH);
    }

    private static Object[] row(String name, int[] values) {
        Object[] row = new Object[values.length + 1];
        row[0] = name;
        for (int i = 0; i < values.length; i++) {
            row[i + 1] = String.valueOf(values[i]);
        }
        return row;
    }

    private void addProcess() {
        UIKit.stopEditing(allocTable);
        UIKit.stopEditing(maxTable);
        String name = "P" + (allocModel.getRowCount() + 1);
        allocModel.addRow(row(name, new int[RESOURCES.length]));
        maxModel.addRow(row(name, new int[RESOURCES.length]));
    }

    private void removeLastProcess() {
        UIKit.stopEditing(allocTable);
        UIKit.stopEditing(maxTable);
        if (allocModel.getRowCount() > 1) {
            allocModel.removeRow(allocModel.getRowCount() - 1);
            maxModel.removeRow(maxModel.getRowCount() - 1);
        }
    }

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
        int[][] need = new int[n][m];
        int[][] alloc = new int[n][m];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < m; j++) {
                alloc[i][j] = UIKit.intAt(allocModel, i, j + 1);
                need[i][j] = UIKit.intAt(maxModel, i, j + 1) - alloc[i][j];
                if (need[i][j] < 0) {
                    maxTable.changeSelection(i, j + 1, false, false);
                    UIKit.error(this, allocModel.getValueAt(i, 0) + " is allocated more " + RESOURCES[j]
                            + " than its maximum demand.");
                    return;
                }
            }
        }

        int[] work = {a, b, c};
        boolean[] finished = new boolean[n];
        List<String> sequence = new ArrayList<>();
        boolean progressed = true;
        while (sequence.size() < n && progressed) {
            progressed = false;
            for (int i = 0; i < n; i++) {
                if (!finished[i] && fits(need[i], work)) {
                    for (int j = 0; j < m; j++) {
                        work[j] += alloc[i][j];
                    }
                    finished[i] = true;
                    sequence.add(allocModel.getValueAt(i, 0).toString());
                    progressed = true;
                }
            }
        }

        if (sequence.size() == n) {
            safeSequenceLabel.setText("Safe sequence: " + String.join("  \u2192  ", sequence));
            safeSequenceLabel.setForeground(Theme.SUCCESS);
        } else {
            safeSequenceLabel.setText("Unsafe state: no sequence lets every process finish.");
            safeSequenceLabel.setForeground(Theme.DANGER);
        }
    }

    private static boolean fits(int[] need, int[] work) {
        for (int j = 0; j < need.length; j++) {
            if (need[j] > work[j]) {
                return false;
            }
        }
        return true;
    }
}
