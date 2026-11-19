import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Module 8: Multiprogramming.
 * 
 * A job queue (arrival and burst time) on the left and a chronological system status / execution
 * log on the right.
 */
public class MultiprogrammingPanel extends JPanel {
    private final DecimalFormat numberFormat = new DecimalFormat("0.################");

    // Editable job queue and the read-only execution log.
    private final DefaultTableModel jobModel;
    private final JTable jobTable;
    private final DefaultTableModel logModel;
    private final UIKit.Section logSection;

    /** Builds the job queue card (with Add/Remove/Run) and the execution log table. */
    public MultiprogrammingPanel() {
        UIKit.setupModule(this);
        add(UIKit.header("Multiprogramming",
                "Simulate how CPU time is shared among concurrently active jobs."), BorderLayout.NORTH);

        jobModel = UIKit.model(new String[]{"Job", "Arrival Time", "Burst Time"}, 1);
        jobModel.addRow(new Object[]{"J1", "4", "3"});
        jobModel.addRow(new Object[]{"J2", "45", "32"});
        jobTable = UIKit.table(jobModel);

        JButton addBtn = UIKit.secondary("Add job");
        JButton removeBtn = UIKit.secondary("Remove selected");
        JButton runBtn = UIKit.primary("Run simulation");
        addBtn.addActionListener(e -> {
            UIKit.stopEditing(jobTable);
            jobModel.addRow(new Object[]{"J" + (jobModel.getRowCount() + 1), "0", "1"});
        });
        removeBtn.addActionListener(e -> UIKit.removeSelectedRows(jobTable));
        runBtn.addActionListener(e -> run());

        JPanel jobActions = UIKit.transparent(new BorderLayout(0, 8));
        jobActions.add(UIKit.row(addBtn, removeBtn), BorderLayout.NORTH);
        jobActions.add(runBtn, BorderLayout.SOUTH);

        JPanel jobBody = UIKit.transparent(new BorderLayout(0, 10));
        jobBody.add(UIKit.scroll(jobTable), BorderLayout.CENTER);
        jobBody.add(jobActions, BorderLayout.SOUTH);

        logModel = UIKit.model(new String[]{"Time", "Event", "Active Jobs (nj)", "% CPU / Job",
                "Hardware / Job", "Job #", "Time Left"}, false);
        logSection = new UIKit.Section("System Status & Execution Log", "Ready to simulate",
                UIKit.scroll(UIKit.table(logModel)));
        logSection.setHint("Equal CPU and hardware shares · press Run to simulate");
        jobModel.addTableModelListener(e -> {
            if (e.getType() == TableModelEvent.UPDATE || e.getType() == TableModelEvent.INSERT
                    || e.getType() == TableModelEvent.DELETE) {
                clearLog();
                logSection.setHint("Job queue changed · run the simulation");
            }
        });

        add(UIKit.split(JSplitPane.HORIZONTAL_SPLIT,
                new UIKit.Section("Job Queue", jobBody), logSection, 0.3), BorderLayout.CENTER);
    }

    /** Validates the job queue, then calls {@link #onRun}. */
    private void run() {
        if (jobModel.getRowCount() == 0) {
            UIKit.error(this, "Add at least one job before running the simulation.");
            return;
        }
        UIKit.stopEditing(jobTable);
        for (int row = 0; row < jobModel.getRowCount(); row++) {
            double arrival;
            try {
                arrival = Double.parseDouble(jobModel.getValueAt(row, 1).toString().trim());
            } catch (NumberFormatException e) {
                jobTable.changeSelection(row, 1, false, false);
                UIKit.error(this, "Job queue: row " + (row + 1)
                        + ", \"Arrival Time\" must be a finite number greater than or equal to 0.");
                return;
            }
            if (Double.isNaN(arrival) || Double.isInfinite(arrival) || arrival < 0) {
                jobTable.changeSelection(row, 1, false, false);
                UIKit.error(this, "Job queue: row " + (row + 1)
                        + ", \"Arrival Time\" must be a finite number greater than or equal to 0.");
                return;
            }
            double burst;
            try {
                burst = Double.parseDouble(jobModel.getValueAt(row, 2).toString().trim());
            } catch (NumberFormatException e) {
                jobTable.changeSelection(row, 2, false, false);
                UIKit.error(this, "Job queue: row " + (row + 1)
                        + ", \"Burst Time\" must be a finite number greater than 0.");
                return;
            }
            if (Double.isNaN(burst) || Double.isInfinite(burst) || burst <= 0) {
                jobTable.changeSelection(row, 2, false, false);
                UIKit.error(this, "Job queue: row " + (row + 1)
                        + ", \"Burst Time\" must be a finite number greater than 0.");
                return;
            }
        }
        logSection.setHint(jobModel.getRowCount() + " jobs");
        clearLog();
        onRun(jobModel);
    }

    /**
     * Simulates processor sharing: each active job receives an equal share of one CPU, and its
     * remaining burst decreases at that rate. The hardware share column shows the same allocation
     * as a fraction of one CPU.
     */
    protected void onRun(DefaultTableModel jobs) {
        List<Job> waiting = new ArrayList<>();
        for (int row = 0; row < jobs.getRowCount(); row++) {
            waiting.add(new Job(String.valueOf(jobs.getValueAt(row, 0)),
                    Double.parseDouble(jobs.getValueAt(row, 1).toString().trim()),
                    Double.parseDouble(jobs.getValueAt(row, 2).toString().trim()), row));
        }
        waiting.sort(Comparator.comparingDouble((Job job) -> job.arrival).thenComparingInt(job -> job.order));

        List<Job> active = new ArrayList<>();
        int nextArrival = 0;
        double time = waiting.get(0).arrival;

        while (nextArrival < waiting.size() || !active.isEmpty()) {
            double nextCompletion = Double.POSITIVE_INFINITY;
            double leastRemaining = Double.POSITIVE_INFINITY;
            if (!active.isEmpty()) {
                for (Job job : active) {
                    leastRemaining = Math.min(leastRemaining, job.remaining);
                }
                nextCompletion = time + leastRemaining * active.size();
            }
            double nextArrivalTime = nextArrival < waiting.size()
                    ? waiting.get(nextArrival).arrival : Double.POSITIVE_INFINITY;
            boolean simultaneousEvents = nearlyEqual(nextArrivalTime, nextCompletion);
            boolean completionDue = nextCompletion < nextArrivalTime || simultaneousEvents;
            double eventTime = simultaneousEvents ? nextArrivalTime : Math.min(nextArrivalTime, nextCompletion);

            if (!active.isEmpty()) {
                double servicePerJob = (eventTime - time) / active.size();
                for (Job job : active) {
                    if (completionDue && job.remaining == leastRemaining) {
                        job.remaining = 0;
                    } else {
                        job.remaining = Math.max(0, job.remaining - servicePerJob);
                    }
                }
            }
            time = eventTime;

            List<Job> completed = new ArrayList<>();
            for (int i = 0; i < active.size();) {
                Job job = active.get(i);
                if (job.remaining <= 0) {
                    active.remove(i);
                    completed.add(job);
                } else {
                    i++;
                }
            }
            for (Job job : completed) {
                appendEvent(time, job.name + " completes", active);
            }
            while (nextArrival < waiting.size()
                    && waiting.get(nextArrival).arrival == time) {
                Job job = waiting.get(nextArrival++);
                active.add(job);
                appendEvent(time, job.name + " arrives", active);
            }
        }
        logSection.setHint(waiting.size() + " jobs · equal-share CPU simulation complete");
    }

    private static boolean nearlyEqual(double first, double second) {
        if (Double.isInfinite(first) || Double.isInfinite(second) || Double.isNaN(first) || Double.isNaN(second)) {
            return false;
        }
        double tolerance = 4 * Math.max(Math.ulp(first), Math.ulp(second));
        return Math.abs(first - second) <= tolerance;
    }

    private void appendEvent(double time, String event, List<Job> active) {
        int activeCount = active.size();
        String cpuShare = activeCount == 0 ? "0%" : numberFormat.format(100.0 / activeCount) + "%";
        String hardwareShare = activeCount == 0 ? "0" : "1 / " + activeCount;
        StringBuilder names = new StringBuilder();
        StringBuilder remaining = new StringBuilder();
        for (Job job : active) {
            if (names.length() > 0) {
                names.append(", ");
                remaining.append(", ");
            }
            names.append(job.name);
            remaining.append(job.name).append(": ").append(numberFormat.format(job.remaining));
        }
        logModel.addRow(new Object[]{numberFormat.format(time), event, activeCount, cpuShare, hardwareShare,
                names.length() == 0 ? "\u2014" : names.toString(),
                remaining.length() == 0 ? "\u2014" : remaining.toString()});
    }

    private static final class Job {
        private final String name;
        private final double arrival;
        private final int order;
        private double remaining;

        private Job(String name, double arrival, double burst, int order) {
            this.name = name;
            this.arrival = arrival;
            this.order = order;
            this.remaining = burst;
        }
    }

    /** Removes every row from the execution log. */
    public void clearLog() {
        logModel.setRowCount(0);
    }

    /** Adds one row to the execution log. */
    public void appendLog(Object[] row) {
        logModel.addRow(row);
    }
}
