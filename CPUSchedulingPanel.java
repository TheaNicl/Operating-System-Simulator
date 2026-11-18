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
            "Completion Time", "Turnaround Time", "Waiting Time", "Response Time", "Avg. Time Calculations"};
    private static final int COL_ARRIVAL = 1;
    private static final int COL_BURST = 2;
    private static final int COL_PRIORITY = 3;
    private static final int COL_COMPLETION = 4;
    private static final int COL_AVERAGES = 8;   // merged "Avg. Time Calculations" column (no model data)
    private static final int INPUT_COLUMNS = 4;
    private static final int ROW_HEIGHT = 36;
    private static final int MAX_VISIBLE_ROWS = 8;
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

    // Result widgets: the whole result lives in one card, like the design (table + averages + Gantt)
    private final UIKit.Section resultsSection;
    private String waitingText = "";      // "Average Waiting Time = (7+3+0+0)/4 = 2.5"
    private String turnaroundText = "";
    private JScrollPane tableScroll;
    private final GanttChart ganttChart = new GanttChart();
    private final JPanel ganttArea = UIKit.transparent(new BorderLayout());
    private final JComponent emptyGantt = UIKit.muted("Press Run to see the Gantt chart.");

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
            resizeTable();
        });

        // Grid look of the design: full cell borders, taller rows. Rows are separated by the cell
        // renderers (not by the table) so the averages column can merge cells across rows.
        processTable.setRowHeight(ROW_HEIGHT);
        processTable.setIntercellSpacing(new Dimension(0, 0));
        processTable.setShowHorizontalLines(false);
        processTable.setShowVerticalLines(true);
        processTable.setDefaultRenderer(Object.class, new GridCellRenderer());
        processTable.getColumnModel().getColumn(COL_AVERAGES).setCellRenderer(new AverageRenderer());
        processTable.getColumnModel().getColumn(COL_AVERAGES).setPreferredWidth(240);

        tableScroll = UIKit.scroll(processTable);
        resizeTable();

        // --- Gantt chart attached directly under the table, with its "Gantt Chart" label cell ---
        JLabel ganttLabel = new JLabel("Gantt Chart", SwingConstants.CENTER);
        ganttLabel.setBorder(new UIKit.RoundedBorder(8, new Insets(6, 12, 6, 12)));
        ganttArea.add(emptyGantt, BorderLayout.CENTER);
        JPanel labelCell = UIKit.transparent(new GridBagLayout());
        labelCell.add(ganttLabel);
        JPanel ganttRow = UIKit.transparent(new BorderLayout(12, 0));
        ganttRow.add(labelCell, BorderLayout.WEST);
        ganttRow.add(ganttArea, BorderLayout.CENTER);
        ganttRow.setPreferredSize(new Dimension(0, 90));

        // Table and Gantt chart are stacked at their natural height, so the chart touches the table.
        JPanel tableAndGantt = UIKit.transparent(new BorderLayout(0, 12));
        tableAndGantt.add(tableScroll, BorderLayout.NORTH);
        tableAndGantt.add(ganttRow, BorderLayout.CENTER);

        JPanel body = UIKit.transparent(new BorderLayout(0, 12));
        body.add(tableAndGantt, BorderLayout.NORTH);
        body.add(UIKit.row(addBtn, removeBtn), BorderLayout.SOUTH);
        resultsSection = new UIKit.Section("Processes & Results",
                "Double-click a cell to edit · results fill in after Run", body);
        add(resultsSection, BorderLayout.CENTER);

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
        resizeTable();
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
        waitingText = "Average Waiting Time<br>= (" + joinSum(waiting) + ")/" + n
                + " = " + averageFormat.format(avgWaiting);
        turnaroundText = "Average Turnaround Time<br>= (" + joinSum(turnaround) + ")/" + n
                + " = " + averageFormat.format(avgTurnaround);
        processTable.repaint();

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
                for (int column = COL_COMPLETION; column < COL_AVERAGES; column++) {
                    tableModel.setValueAt(NO_RESULT, row, column);
                }
            }
        } finally {
            writingResults = false;
        }
        waitingText = "";
        turnaroundText = "";
        processTable.repaint();
        ganttArea.removeAll();
        ganttArea.add(emptyGantt, BorderLayout.CENTER);
        ganttArea.revalidate();
        ganttArea.repaint();
        resultsSection.setHint("Double-click a cell to edit · results fill in after Run");
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

    /** Sizes the table to its rows (up to MAX_VISIBLE_ROWS) so the Gantt chart sits right below it. */
    private void resizeTable() {
        int rows = Math.min(Math.max(tableModel.getRowCount(), 1), MAX_VISIBLE_ROWS);
        int header = processTable.getTableHeader().getPreferredSize().height;
        tableScroll.setPreferredSize(new Dimension(0, header + rows * ROW_HEIGHT + 6));
        tableScroll.revalidate();
    }

    /** Normal cells: centred text with a thin line below, so the grid looks like the design. */
    private static final class GridCellRenderer extends javax.swing.table.DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean selected,
                                                       boolean focused, int row, int column) {
            super.getTableCellRendererComponent(table, value, selected, focused, row, column);
            setHorizontalAlignment(SwingConstants.CENTER);
            setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, UIKit.borderColor()));
            return this;
        }
    }

    /**
     * Renderer for the "Avg. Time Calculations" column. The column is split into two merged cells
     * (first half of the rows = waiting time, second half = turnaround time), or one merged cell
     * with both texts when there are fewer than four rows. Every row paints its own slice of the
     * tall merged cell, so the pieces line up into one block.
     */
    private final class AverageRenderer extends JComponent implements javax.swing.table.TableCellRenderer {
        private final JLabel label = new JLabel();
        private int row;
        private int rows;
        private Color background = Color.WHITE;
        private Color foreground = Color.BLACK;

        AverageRenderer() {
            label.setVerticalAlignment(SwingConstants.CENTER);
            label.setBorder(BorderFactory.createEmptyBorder(4, 12, 4, 12));
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean selected,
                                                       boolean focused, int row, int column) {
            this.row = row;
            this.rows = table.getRowCount();
            Color bg = table.getBackground();
            background = bg != null ? bg : Color.WHITE;
            foreground = UIKit.textColor();
            label.setFont(table.getFont());
            return this;
        }

        @Override
        protected void paintComponent(Graphics g) {
            int width = getWidth();
            int height = getHeight();
            boolean split = rows >= 4;
            int middle = (rows + 1) / 2;
            int blockStart = !split || row < middle ? 0 : middle;
            int blockEnd = !split ? rows : (row < middle ? middle : rows);
            String text;
            if (waitingText.isEmpty()) {
                text = "";
            } else if (!split) {
                text = waitingText + "<br><br>" + turnaroundText;
            } else {
                text = row < middle ? waitingText : turnaroundText;
            }

            Graphics2D g2 = (Graphics2D) g.create();
            g2.setColor(background);
            g2.fillRect(0, 0, width, height);
            if (row == blockEnd - 1) {
                g2.setColor(UIKit.borderColor());
                g2.fillRect(0, height - 1, width, 1);
            }
            // Paint the tall label shifted up by the rows above this one in the block.
            g2.clipRect(0, 0, width, height);
            g2.translate(0, -(row - blockStart) * height);
            label.setForeground(foreground);
            label.setText("<html>" + text + "</html>");
            label.setSize(width, (blockEnd - blockStart) * height);
            label.paint(g2);
            g2.dispose();
        }
    }

    /** Process table model (inputs and results), for tests or other modules. */
    public DefaultTableModel getProcessModel() {
        return tableModel;
    }
}
