import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

/**
 * Module 7: Disk Scheduling.
 * 
 * The user enters a request queue, head position, disk size and algorithm. The panel lists each
 * seek (from track, to track, distance), shows total and average head movement and animates
 * the path in a {@link HeadMovementChart}.
 */
public class DiskSchedulingPanel extends JPanel {
    // Algorithm names shown as option buttons, and the columns of the service-order table.
    private static final String[] ALGORITHMS = {"FCFS", "SSTF", "SCAN", "C-SCAN", "LOOK", "C-LOOK"};
    private static final String[] COLUMNS = {"Step", "From Track", "To Track", "Head Movement"};

    // Inputs.
    private final JTextField requestField;
    private final JTextField headField;
    private final JTextField diskSizeField;
    private final JComboBox<String> directionBox;
    private final ButtonGroup algorithmGroup = new ButtonGroup();
    private final JToggleButton[] algorithmButtons = new JToggleButton[ALGORITHMS.length];
    // Outputs: service-order table, head chart and summary tiles.
    private final DefaultTableModel tableModel;
    private final HeadMovementChart chart = new HeadMovementChart();
    private final UIKit.Section chartSection;
    private final UIKit.Stat totalStat = new UIKit.Stat("Total head movement", "—");
    private final UIKit.Stat averageStat = new UIKit.Stat("Average seek length", "—");

    /** Builds the request form, the head movement chart and the service-order table. */
    public DiskSchedulingPanel() {
        UIKit.setupModule(this);
        add(UIKit.header("Disk Scheduling",
                "Visualize head movement for FCFS, SSTF, SCAN, C-SCAN, LOOK and C-LOOK."), BorderLayout.NORTH);

        requestField = UIKit.field("30, 40, 55, 30, 20", "e.g. 98, 183, 37, 122", 22);
        headField = UIKit.field("50", null, 5);
        diskSizeField = UIKit.field("200", null, 5);
        directionBox = new JComboBox<>(new String[]{"Toward higher tracks", "Toward track 0"});

        JPanel form = UIKit.form(2,
                "Request queue", requestField,
                "Initial head", headField,
                "Disk size (tracks)", diskSizeField,
                "Direction", directionBox);

        JPanel options = UIKit.transparent(null);
        options.setLayout(new BoxLayout(options, BoxLayout.X_AXIS));
        options.add(UIKit.muted("Algorithm"));
        for (int i = 0; i < ALGORITHMS.length; i++) {
            algorithmButtons[i] = UIKit.option(ALGORITHMS[i], algorithmGroup);
            algorithmButtons[i].addItemListener(e -> updateDirectionState());
            options.add(Box.createHorizontalStrut(8));
            options.add(algorithmButtons[i]);
        }
        algorithmButtons[0].setSelected(true);
        updateDirectionState();

        JButton runBtn = UIKit.primary("Run");
        runBtn.addActionListener(e -> run());

        JPanel configBody = UIKit.transparent(new BorderLayout(0, 12));
        configBody.add(form, BorderLayout.CENTER);
        configBody.add(UIKit.toolbar(options, runBtn), BorderLayout.SOUTH);

        chartSection = new UIKit.Section("Head Movement", "Sample: FCFS from track 50", chart);

        tableModel = UIKit.model(COLUMNS, false);
        JPanel tableBody = UIKit.transparent(new BorderLayout(0, 12));
        tableBody.add(UIKit.scroll(UIKit.table(tableModel)), BorderLayout.CENTER);
        tableBody.add(UIKit.stats(totalStat, averageStat), BorderLayout.SOUTH);

        JPanel center = UIKit.transparent(new BorderLayout(0, 16));
        center.add(new UIKit.Section("Disk Request Queue", configBody), BorderLayout.NORTH);
        center.add(UIKit.split(JSplitPane.HORIZONTAL_SPLIT, chartSection,
                new UIKit.Section("Service Order", tableBody), 0.6), BorderLayout.CENTER);
        add(center, BorderLayout.CENTER);

        showResult(new int[]{50, 30, 40, 55, 30, 20}, 200, false);
    }

    /** Name of the selected algorithm button (FCFS if none). */
    private String selectedAlgorithm() {
        for (int i = 0; i < algorithmButtons.length; i++) {
            if (algorithmButtons[i].isSelected()) {
                return ALGORITHMS[i];
            }
        }
        return ALGORITHMS[0];
    }

    /** Direction only matters for the sweep algorithms (SCAN, C-SCAN, LOOK, C-LOOK). */
    private void updateDirectionState() {
        String algorithm = selectedAlgorithm();
        directionBox.setEnabled(!"FCFS".equals(algorithm) && !"SSTF".equals(algorithm));
    }

    /** Validates every input (tracks must lie inside the disk), computes the order and shows it. */
    private void run() {
        Integer diskSize = UIKit.parseInt(diskSizeField, "Disk size", 2, 100000, this);
        if (diskSize == null) return;
        int[] requests = UIKit.parseIntList(requestField, "Request queue", 0, this);
        if (requests == null) return;
        for (int request : requests) {
            if (request >= diskSize) {
                UIKit.markInvalid(requestField, true);
                UIKit.error(this, "Track " + request + " is outside the disk (0 – " + (diskSize - 1) + ").");
                return;
            }
        }
        Integer head = UIKit.parseInt(headField, "Initial head", 0, diskSize - 1, this);
        if (head == null) return;

        String algorithm = selectedAlgorithm();
        boolean towardHigher = directionBox.getSelectedIndex() == 0;
        int[] order = computeServiceOrder(algorithm, head, requests, diskSize, towardHigher);
        if (order == null) {
            chartSection.setHint(algorithm + " is not implemented yet");
            return;
        }
        chartSection.setHint(algorithm + " from track " + head);
        showResult(order, diskSize, true);
    }

    /**
     * Hook for the scheduling algorithms. Return the visited tracks in order, starting
     * with the initial head position, or null when the algorithm is not implemented.
     */
    protected int[] computeServiceOrder(String algorithm, int head, int[] requests, int diskSize,
                                        boolean towardHigher) {
        if ("FCFS".equals(algorithm)) {
            int[] order = new int[requests.length + 1];
            order[0] = head;
            System.arraycopy(requests, 0, order, 1, requests.length);
            return order;
        }

        if ("SSTF".equals(algorithm)) {
            java.util.ArrayList<Integer> remaining = new java.util.ArrayList<>();
            for (int request : requests) remaining.add(request);
            java.util.ArrayList<Integer> order = new java.util.ArrayList<>();
            order.add(head);
            int current = head;
            while (!remaining.isEmpty()) {
                int closestIndex = 0;
                for (int i = 1; i < remaining.size(); i++) {
                    int candidateDistance = Math.abs(remaining.get(i) - current);
                    int closestDistance = Math.abs(remaining.get(closestIndex) - current);
                    if (candidateDistance < closestDistance) {
                        closestIndex = i;
                    }
                }
                current = remaining.remove(closestIndex);
                order.add(current);
            }
            int[] result = new int[order.size()];
            for (int i = 0; i < order.size(); i++) result[i] = order.get(i);
            return result;
        }

        java.util.ArrayList<Integer> lower = new java.util.ArrayList<>();
        java.util.ArrayList<Integer> higher = new java.util.ArrayList<>();
        for (int request : requests) {
            if (request < head) lower.add(request);
            else if (request > head) higher.add(request);
        }
        java.util.Collections.sort(lower);
        java.util.Collections.sort(higher);

        java.util.ArrayList<Integer> order = new java.util.ArrayList<>();
        order.add(head);

        if ("LOOK".equals(algorithm)) {
            if (towardHigher) {
                for (int request : higher) order.add(request);
                for (int i = lower.size() - 1; i >= 0; i--) order.add(lower.get(i));
            } else {
                for (int i = lower.size() - 1; i >= 0; i--) order.add(lower.get(i));
                for (int request : higher) order.add(request);
            }
        } else if ("C-LOOK".equals(algorithm)) {
            if (towardHigher) {
                for (int request : higher) order.add(request);
                if (!lower.isEmpty()) {
                    for (int request : lower) order.add(request);
                }
            } else {
                for (int i = lower.size() - 1; i >= 0; i--) order.add(lower.get(i));
                if (!higher.isEmpty()) {
                    for (int i = higher.size() - 1; i >= 0; i--) order.add(higher.get(i));
                }
            }
        } else if ("SCAN".equals(algorithm)) {
            if (towardHigher) {
                for (int request : higher) order.add(request);
                addTrack(order, diskSize - 1);
                for (int i = lower.size() - 1; i >= 0; i--) order.add(lower.get(i));
            } else {
                for (int i = lower.size() - 1; i >= 0; i--) order.add(lower.get(i));
                addTrack(order, 0);
                for (int request : higher) order.add(request);
            }
        } else if ("C-SCAN".equals(algorithm)) {
            if (towardHigher) {
                for (int request : higher) order.add(request);
                addTrack(order, diskSize - 1);
                addTrack(order, 0);
                for (int request : lower) order.add(request);
            } else {
                for (int i = lower.size() - 1; i >= 0; i--) order.add(lower.get(i));
                addTrack(order, 0);
                addTrack(order, diskSize - 1);
                for (int i = higher.size() - 1; i >= 0; i--) order.add(higher.get(i));
            }
        } else {
            return null;
        }

        int[] result = new int[order.size()];
        for (int i = 0; i < order.size(); i++) result[i] = order.get(i);
        return result;
    }

    private static void addTrack(java.util.ArrayList<Integer> order, int track) {
        if (order.isEmpty() || order.get(order.size() - 1) != track) {
            order.add(track);
        }
    }

    /** Fills the table with every seek, updates the totals and redraws the chart. */
    public void showResult(int[] order, int diskSize, boolean animate) {
        tableModel.setRowCount(0);
        long total = 0;
        for (int i = 1; i < order.length; i++) {
            int distance = Math.abs(order[i] - order[i - 1]);
            total += distance;
            tableModel.addRow(new Object[]{String.valueOf(i), String.valueOf(order[i - 1]),
                    String.valueOf(order[i]), String.valueOf(distance)});
        }
        int moves = Math.max(1, order.length - 1);
        totalStat.setValue(String.valueOf(total));
        averageStat.setValue(String.format("%.2f", (double) total / moves));
        chart.setSequence(order, diskSize - 1, animate);
    }
}
