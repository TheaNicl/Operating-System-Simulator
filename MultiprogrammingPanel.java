import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

public class MultiprogrammingPanel extends JPanel {
    private final DefaultTableModel jobModel;
    private final JTable jobTable;
    private final DefaultTableModel logModel;
    private final UIKit.Section logSection;

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
        logModel.addRow(new Object[]{"10.0", "J1 arrives / starts", "0", "0", "0", "1", "0.3"});
        logModel.addRow(new Object[]{"10.2", "J2 arrives / starts", "1", "0.2", "0.2", "1 / 2", "0.1 / 0.5"});
        logModel.addRow(new Object[]{"10.4", "J3 arrives / starts", "2", "0.5", "0.2", "2 / 3", "0.40 / 0.1"});
        logSection = new UIKit.Section("System Status & Execution Log", "Sample output",
                UIKit.scroll(UIKit.table(logModel)));

        add(UIKit.split(JSplitPane.HORIZONTAL_SPLIT,
                new UIKit.Section("Job Queue", jobBody), logSection, 0.3), BorderLayout.CENTER);
    }

    private void run() {
        if (jobModel.getRowCount() == 0) {
            UIKit.error(this, "Add at least one job before running the simulation.");
            return;
        }
        if (!UIKit.validateNumericTable(jobTable, 1, "Job queue", this)) {
            return;
        }
        logSection.setHint(jobModel.getRowCount() + " jobs");
        onRun(jobModel);
    }

    /** Hook for the simulation. Append rows with {@link #appendLog(Object[])} or clear via {@link #clearLog()}. */
    protected void onRun(DefaultTableModel jobs) {
    }

    public void clearLog() {
        logModel.setRowCount(0);
    }

    public void appendLog(Object[] row) {
        logModel.addRow(row);
    }
}
