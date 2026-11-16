import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Module 1: CPU Scheduling.
 *
 * <p>Front end only. The user edits Arrival, Burst and Priority in one table and presses Run;
 * the panel validates the input and calls {@link #onSchedule}. The back end overrides that hook,
 * computes the schedule and hands it to {@link #showResults}, which fills the read-only
 * Completion, Turnaround, Waiting and Response columns, the average-time calculations and the
 * Gantt chart. Until then the sample from the design issue is displayed.</p>
 */
public class CPUSchedulingPanel extends JPanel {
    // Column layout of the process table: inputs first, computed results after.
    private static final String[] COLUMNS = {"Process", "Arrival Time", "Burst Time", "Priority",
            "Completion Time", "Turnaround Time", "Waiting Time", "Response Time"};
    private static final int COL_ARRIVAL = 1;
    private static final int COL_BURST = 2;
    private static final int COL_PRIORITY = 3;
    private static final int COL_COMPLETION = 4;
    private static final int INPUT_COLUMNS = 4;
    private static final String NO_RESULT = "";

    private final DecimalFormat averageFormat = new DecimalFormat("0.##");

    // Process table (inputs + results)
    private final DefaultTableModel tableModel;
    private final JTable processTable;

    // Algorithm selection controls
    private final JTextField quantumField;
    private final JLabel quantumLabel;
    private final ButtonGroup algorithmGroup = new ButtonGroup();
    private final JToggleButton fcfsBtn;
    private final JToggleButton sjfBtn;
    private final JToggleButton priorityBtn;
    private final JToggleButton rrBtn;
    private final JCheckBox preemptiveBox = new JCheckBox("Preemptive");

    // Result widgets
    private final UIKit.Section resultsSection;
    private final UIKit.Stat avgWaitingStat = new UIKit.Stat("Avg. waiting time", "—");
    private final UIKit.Stat avgTurnaroundStat = new UIKit.Stat("Avg. turnaround time", "—");
    private final JLabel waitingFormula = UIKit.muted(" ");
    private final JLabel turnaroundFormula = UIKit.muted(" ");
    private final GanttChart ganttChart = new GanttChart();
    private final JPanel ganttArea = UIKit.transparent(new BorderLayout());
    private final JComponent emptyGantt = UIKit.emptyState("No schedule yet",
            "Pick an algorithm below and press Run to see the Gantt chart and timing results.");

    /** True while the panel itself writes result cells, so those writes don't clear the results. */
    private boolean writingResults;

    public CPUSchedulingPanel() {
        UIKit.setupModule(this);
        add(UIKit.header("CPU Scheduling",
                "Compare FCFS, SJF, Priority and Round Robin on the same set of processes."), BorderLayout.NORTH);

        // --- process table: only the first four columns are editable ---
        tableModel = UIKit.model(COLUMNS, 0, INPUT_COLUMNS);
        tableModel.addRow(new Object[]{"P1", "0", "5", "10", NO_RESULT, NO_RESULT, NO_RESULT, NO_RESULT});
        tableModel.addRow(new Object[]{"P2", "1", "4", "20", NO_RESULT, NO_RESULT, NO_RESULT, NO_RESULT});
        tableModel.addRow(new Object[]{"P3", "2", "2", "30", NO_RESULT, NO_RESULT, NO_RESULT, NO_RESULT});
        tableModel.addRow(new Object[]{"P4", "4", "1", "40", NO_RESULT, NO_RESULT, NO_RESULT, NO_RESULT});
        processTable = UIKit.table(tableModel);
        // Changing an input makes the old results out of date, so clear them.
        tableModel.addTableModelListener(e -> {
            if (!writingResults && e.getType() == TableModelEvent.UPDATE
                    && e.getColumn() != TableModelEvent.ALL_COLUMNS && e.getColumn() < INPUT_COLUMNS) {
                clearResults();
            }
        });

        JButton addBtn = UIKit.secondary("Add process");
        JButton removeBtn = UIKit.secondary("Remove selected");
        addBtn.addActionListener(e -> addProcess());
        removeBtn.addActionListener(e -> {
            UIKit.removeSelectedRows(processTable);
            clearResults();
        });

        JPanel processBody = UIKit.transparent(new BorderLayout(0, 10));
        processBody.add(UIKit.scroll(processTable), BorderLayout.CENTER);
        processBody.add(UIKit.row(addBtn, removeBtn), BorderLayout.SOUTH);
        UIKit.Section processSection = new UIKit.Section("Processes",
                "Double-click a cell to edit · results fill in after Run", processBody);

        // --- results: averages with their formulas, then the Gantt chart ---
        ganttArea.add(emptyGantt);
        JPanel formulas = UIKit.transparent(null);
        formulas.setLayout(new BoxLayout(formulas, BoxLayout.Y_AXIS));
        waitingFormula.setAlignmentX(Component.LEFT_ALIGNMENT);
        turnaroundFormula.setAlignmentX(Component.LEFT_ALIGNMENT);
        formulas.add(waitingFormula);
        formulas.add(Box.createVerticalStrut(2));
        formulas.add(turnaroundFormula);

        JPanel summary = UIKit.transparent(new BorderLayout(0, 8));
        summary.add(UIKit.stats(avgWaitingStat, avgTurnaroundStat), BorderLayout.NORTH);
        summary.add(formulas, BorderLayout.CENTER);

        JPanel resultsBody = UIKit.transparent(new BorderLayout(0, 12));
        resultsBody.add(summary, BorderLayout.NORTH);
        resultsBody.add(ganttArea, BorderLayout.CENTER);
        resultsSection = new UIKit.Section("Results & Gantt Chart", resultsBody);

        add(UIKit.split(JSplitPane.VERTICAL_SPLIT, processSection, resultsSection, 0.5), BorderLayout.CENTER);

        // --- bottom bar: algorithm picker, preemption, quantum, Run ---
        fcfsBtn = UIKit.option("FCFS", algorithmGroup);
        sjfBtn = UIKit.option("SJF", algorithmGroup);
        priorityBtn = UIKit.option("Priority", algorithmGroup);
        rrBtn = UIKit.option("Round Robin", algorithmGroup);
        fcfsBtn.setSelected(true);

        quantumLabel = new JLabel("Time quantum");
        quantumField = UIKit.field("", "e.g. 2", 5);
        quantumLabel.setLabelFor(quantumField);
        preemptiveBox.setToolTipText("SJF and Priority only. Priority: a higher number means higher priority.");
        for (JToggleButton button : new JToggleButton[]{fcfsBtn, sjfBtn, priorityBtn, rrBtn}) {
            button.addItemListener(e -> updateOptionState());
        }
        updateOptionState();

        JButton runBtn = UIKit.primary("Run");
        runBtn.addActionListener(e -> run());

        JPanel controls = UIKit.row(UIKit.muted("Algorithm"), fcfsBtn, sjfBtn, priorityBtn, rrBtn,
                Box.createHorizontalStrut(12), preemptiveBox,
                Box.createHorizontalStrut(12), quantumLabel, quantumField);
        add(UIKit.toolbar(controls, runBtn), BorderLayout.SOUTH);

        showSample();
    }

    /** Fills in the layout from the design issue (preemptive Priority) until the back end is connected. */
    private void showSample() {
        List<GanttChart.Segment> gantt = new ArrayList<>();
        int[][] blocks = {{0, 0, 1}, {1, 1, 2}, {2, 2, 4}, {3, 4, 5}, {1, 5, 8}, {0, 8, 12}};
        for (int[] b : blocks) {
            gantt.add(new GanttChart.Segment(b[0], b[1], b[2]));
        }
        showResults(new int[]{12, 8, 4, 5}, new int[]{12, 7, 2, 1}, new int[]{7, 3, 0, 0},
                new int[]{0, 0, 0, 0}, gantt);
        resultsSection.setHint("Sample: Priority (preemptive)");
    }

    /** Appends a blank-result process row and puts the cursor on its Arrival cell. */
    private void addProcess() {
        UIKit.stopEditing(processTable);
        tableModel.addRow(new Object[]{"P" + (tableModel.getRowCount() + 1), "0", "1", "1",
                NO_RESULT, NO_RESULT, NO_RESULT, NO_RESULT});
        int last = tableModel.getRowCount() - 1;
        processTable.changeSelection(last, COL_ARRIVAL, false, false);
        clearResults();
    }

    /** Quantum only applies to Round Robin; Preemptive only to SJF and Priority. */
    private void updateOptionState() {
        boolean roundRobin = rrBtn.isSelected();
        quantumField.setEnabled(roundRobin);
        quantumLabel.setEnabled(roundRobin);
        preemptiveBox.setEnabled(sjfBtn.isSelected() || priorityBtn.isSelected());
        if (!roundRobin) {
            UIKit.markInvalid(quantumField, false);
        }
    }

    private String selectedAlgorithm() {
        if (sjfBtn.isSelected()) return "SJF";
        if (priorityBtn.isSelected()) return "Priority";
        if (rrBtn.isSelected()) return "Round Robin";
        return "FCFS";
    }

    /** Validates the inputs, runs the scheduler and displays the results. */
    private void run() {
        if (tableModel.getRowCount() == 0) {
            UIKit.error(this, "Add at least one process before running the scheduler.");
            return;
        }
        if (!UIKit.validateNumericTable(processTable, COL_ARRIVAL, INPUT_COLUMNS, "Processes", this)) {
            return;
        }
        int n = tableModel.getRowCount();
        for (int row = 0; row < n; row++) {
            if (UIKit.intAt(tableModel, row, COL_BURST) < 1) {
                processTable.changeSelection(row, COL_BURST, false, false);
                UIKit.error(this, "Processes: row " + (row + 1) + ", \"Burst Time\" must be at least 1.");
                return;
            }
        }

        int quantum = 1;
        if (rrBtn.isSelected()) {
            if (quantumField.getText().trim().isEmpty()) {
                UIKit.markInvalid(quantumField, true);
                JOptionPane.showMessageDialog(this,
                        "Round Robin needs a time quantum. Enter a value greater than 0.",
                        "Time quantum required", JOptionPane.WARNING_MESSAGE);
                quantumField.requestFocusInWindow();
                return;
            }
            Integer parsed = UIKit.parseInt(quantumField, "Time quantum", 1, 1000, this);
            if (parsed == null) {
                return;
            }
            quantum = parsed;
        }

        String algorithm = selectedAlgorithm();
        boolean preemptive = preemptiveBox.isEnabled() && preemptiveBox.isSelected();
        onSchedule(algorithm, preemptive, rrBtn.isSelected() ? quantum : null);
    }

    /**
     * BACK-END HOOK. Read the inputs from {@link #getProcessModel()} (columns: Process, Arrival,
     * Burst, Priority), compute the schedule for {@code algorithm} ("FCFS", "SJF", "Priority" or
     * "Round Robin"), then call {@link #showResults}. {@code preemptive} only applies to SJF and
     * Priority; {@code quantum} is null unless Round Robin is selected.
     */
    protected void onSchedule(String algorithm, boolean preemptive, Integer quantum) {
        resultsSection.setHint(algorithm + " is not connected to the back end yet");
    }

    /**
     * Displays a computed schedule. All arrays have one entry per table row, in row order.
     * Averages and their formulas are derived from the arrays here.
     *
     * @param gantt segments in time order; process = row index, or -1 for idle time
     */
    public void showResults(int[] completion, int[] turnaround, int[] waiting, int[] response,
                            List<GanttChart.Segment> gantt) {
        List<String> names = new ArrayList<>();
        for (int row = 0; row < tableModel.getRowCount(); row++) {
            names.add(String.valueOf(tableModel.getValueAt(row, 0)));
        }
        int n = names.size();
        writingResults = true;
        try {
            for (int row = 0; row < n; row++) {
                tableModel.setValueAt(String.valueOf(completion[row]), row, COL_COMPLETION);
                tableModel.setValueAt(String.valueOf(turnaround[row]), row, COL_COMPLETION + 1);
                tableModel.setValueAt(String.valueOf(waiting[row]), row, COL_COMPLETION + 2);
                tableModel.setValueAt(String.valueOf(response[row]), row, COL_COMPLETION + 3);
            }
        } finally {
            writingResults = false;
        }

        double avgWaiting = average(waiting);
        double avgTurnaround = average(turnaround);
        avgWaitingStat.setValue(String.format("%.2f", avgWaiting));
        avgTurnaroundStat.setValue(String.format("%.2f", avgTurnaround));
        waitingFormula.setText("Average Waiting Time = (" + joinSum(waiting) + ")/" + n
                + " = " + averageFormat.format(avgWaiting));
        turnaroundFormula.setText("Average Turnaround Time = (" + joinSum(turnaround) + ")/" + n
                + " = " + averageFormat.format(avgTurnaround));

        ganttChart.setSchedule(gantt, names);
        JScrollPane scroll = UIKit.scroll(ganttChart);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        ganttArea.removeAll();
        ganttArea.add(scroll, BorderLayout.CENTER);
        ganttArea.revalidate();
        ganttArea.repaint();

        resultsSection.setHint(n + " processes");
    }

    /** Resets result columns, averages and chart; used when the inputs change. */
    private void clearResults() {
        writingResults = true;
        try {
            for (int row = 0; row < tableModel.getRowCount(); row++) {
                for (int column = COL_COMPLETION; column < COLUMNS.length; column++) {
                    tableModel.setValueAt(NO_RESULT, row, column);
                }
            }
        } finally {
            writingResults = false;
        }
        avgWaitingStat.setValue("—");
        avgTurnaroundStat.setValue("—");
        waitingFormula.setText(" ");
        turnaroundFormula.setText(" ");
        ganttArea.removeAll();
        ganttArea.add(emptyGantt, BorderLayout.CENTER);
        ganttArea.revalidate();
        ganttArea.repaint();
        resultsSection.setHint("");
    }

    private static double average(int[] values) {
        int sum = 0;
        for (int value : values) {
            sum += value;
        }
        return values.length == 0 ? 0 : (double) sum / values.length;
    }

    /** "7+3+0+0" for the average formulas. */
    private static String joinSum(int[] values) {
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            if (i > 0) {
                text.append('+');
            }
            text.append(values[i]);
        }
        return text.toString();
    }

    /** Process table model (inputs and results), for tests or other modules. */
    public DefaultTableModel getProcessModel() {
        return tableModel;
    }
}
