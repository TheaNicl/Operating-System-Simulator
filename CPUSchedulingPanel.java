import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class CPUSchedulingPanel extends JPanel {
    private final DefaultTableModel tableModel;
    private final JTable processTable;
    private final JTextField quantumField;
    private final JLabel quantumLabel;
    private final ButtonGroup algorithmGroup = new ButtonGroup();
    private final JToggleButton fcfsBtn;
    private final JToggleButton sjfBtn;
    private final JToggleButton priorityBtn;
    private final JToggleButton rrBtn;
    private final UIKit.Section resultsSection;
    private final UIKit.Stat avgWaitingStat = new UIKit.Stat("Avg. waiting time", "—");
    private final UIKit.Stat avgTurnaroundStat = new UIKit.Stat("Avg. turnaround time", "—");
    private final JPanel ganttArea = UIKit.transparent(new BorderLayout());

    public CPUSchedulingPanel() {
        UIKit.setupModule(this);
        add(UIKit.header("CPU Scheduling",
                "Compare FCFS, SJF, Priority and Round Robin on the same set of processes."), BorderLayout.NORTH);

        tableModel = UIKit.model(new String[]{"PID", "Arrival Time", "Burst Time", "Priority"}, true);
        tableModel.addRow(new Object[]{"P1", "0", "5", "1"});
        tableModel.addRow(new Object[]{"P2", "1", "3", "2"});
        tableModel.addRow(new Object[]{"P3", "2", "8", "1"});
        processTable = UIKit.table(tableModel);

        JButton addBtn = UIKit.secondary("Add process");
        JButton removeBtn = UIKit.secondary("Remove selected");
        addBtn.addActionListener(e -> addProcess());
        removeBtn.addActionListener(e -> UIKit.removeSelectedRows(processTable));

        JPanel processBody = UIKit.transparent(new BorderLayout(0, 10));
        processBody.add(UIKit.scroll(processTable), BorderLayout.CENTER);
        processBody.add(UIKit.row(addBtn, removeBtn), BorderLayout.SOUTH);
        UIKit.Section processSection = new UIKit.Section("Processes", "Double-click a cell to edit", processBody);

        ganttArea.add(UIKit.emptyState("No schedule yet",
                "Pick an algorithm below and press Run to see the Gantt chart and timing results."));
        JPanel resultsBody = UIKit.transparent(new BorderLayout(0, 12));
        resultsBody.add(UIKit.stats(avgWaitingStat, avgTurnaroundStat), BorderLayout.NORTH);
        resultsBody.add(ganttArea, BorderLayout.CENTER);
        resultsSection = new UIKit.Section("Results & Gantt Chart", resultsBody);

        add(UIKit.split(JSplitPane.VERTICAL_SPLIT, processSection, resultsSection, 0.5), BorderLayout.CENTER);

        fcfsBtn = UIKit.option("FCFS", algorithmGroup);
        sjfBtn = UIKit.option("SJF", algorithmGroup);
        priorityBtn = UIKit.option("Priority", algorithmGroup);
        rrBtn = UIKit.option("Round Robin", algorithmGroup);
        fcfsBtn.setSelected(true);

        quantumLabel = new JLabel("Time quantum");
        quantumField = UIKit.field("", "e.g. 2", 5);
        quantumLabel.setLabelFor(quantumField);
        for (JToggleButton button : new JToggleButton[]{fcfsBtn, sjfBtn, priorityBtn, rrBtn}) {
            button.addItemListener(e -> updateQuantumState());
        }
        updateQuantumState();

        JButton runBtn = UIKit.primary("Run");
        runBtn.addActionListener(e -> run());

        JPanel controls = UIKit.row(UIKit.muted("Algorithm"), fcfsBtn, sjfBtn, priorityBtn, rrBtn,
                Box.createHorizontalStrut(12), quantumLabel, quantumField);
        add(UIKit.toolbar(controls, runBtn), BorderLayout.SOUTH);
    }

    private void addProcess() {
        UIKit.stopEditing(processTable);
        tableModel.addRow(new Object[]{"P" + (tableModel.getRowCount() + 1), "0", "1", "1"});
        int last = tableModel.getRowCount() - 1;
        processTable.changeSelection(last, 1, false, false);
    }

    private void updateQuantumState() {
        boolean roundRobin = rrBtn.isSelected();
        quantumField.setEnabled(roundRobin);
        quantumLabel.setEnabled(roundRobin);
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

    private void run() {
        if (tableModel.getRowCount() == 0) {
            UIKit.error(this, "Add at least one process before running the scheduler.");
            return;
        }
        if (!UIKit.validateNumericTable(processTable, 1, "Processes", this)) {
            return;
        }

        Integer quantum = null;
        if (rrBtn.isSelected()) {
            if (quantumField.getText().trim().isEmpty()) {
                UIKit.markInvalid(quantumField, true);
                JOptionPane.showMessageDialog(this,
                        "Round Robin needs a time quantum. Enter a value greater than 0.",
                        "Time quantum required", JOptionPane.WARNING_MESSAGE);
                quantumField.requestFocusInWindow();
                return;
            }
            quantum = UIKit.parseInt(quantumField, "Time quantum", 1, 1000, this);
            if (quantum == null) {
                return;
            }
        }
        onSchedule(selectedAlgorithm(), quantum);
    }

    /**
     * Hook for the scheduling logic. Read processes from {@link #getProcessModel()},
     * compute the schedule, then call {@link #showResults(JComponent, double, double)}.
     */
    protected void onSchedule(String algorithm, Integer quantum) {
        resultsSection.setHint(algorithm + (quantum != null ? " · q = " + quantum : "")
                + " · " + tableModel.getRowCount() + " processes");
    }

    public DefaultTableModel getProcessModel() {
        return tableModel;
    }

    public void showResults(JComponent ganttChart, double avgWaiting, double avgTurnaround) {
        avgWaitingStat.setValue(String.format("%.2f", avgWaiting));
        avgTurnaroundStat.setValue(String.format("%.2f", avgTurnaround));
        ganttArea.removeAll();
        ganttArea.add(ganttChart, BorderLayout.CENTER);
        ganttArea.revalidate();
        ganttArea.repaint();
    }
}
