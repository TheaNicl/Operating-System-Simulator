import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

/**
 * Module 6: Memory Fragmentation.
 *
 * Editable starting memory and process tables sit above a single final-map table. Algorithm
 * buttons are present for UI selection only; memory placement is not implemented yet.
 */
public class FragmentationPanel extends JPanel {
    /** The placement strategies available in the UI. */
    public enum Fit {
        FIRST("First Fit"), BEST("Best Fit"), NEXT("Next Fit");

        final String label;

        Fit(String label) {
            this.label = label;
        }
    }

    private final DefaultTableModel memoryModel;
    private final JTable memoryTable;
    private final DefaultTableModel processModel;
    private final JTable processTable;
    private final DefaultTableModel finalMapModel;

    /** Builds editable starting-map and process tables above the single final-map table. */
    public FragmentationPanel() {
        UIKit.setupModule(this);
        add(UIKit.header("Memory Fragmentation",
                "Enter the current memory map and processes, then choose a placement strategy."), BorderLayout.NORTH);

        memoryModel = UIKit.model(new String[]{"Block #", "Base Address", "Size", "Status"}, 1);
        memoryModel.addRow(new Object[]{"0", "0B", "32GB", "Free"});
        memoryModel.addRow(new Object[]{"1", "32GB", "48GB", "Free"});
        memoryModel.addRow(new Object[]{"2", "80GB", "64GB", "Free"});
        memoryModel.addRow(new Object[]{"3", "144GB", "16GB", "Free"});
        memoryTable = UIKit.table(memoryModel);
        memoryTable.getColumnModel().getColumn(3).setCellEditor(
                new DefaultCellEditor(new JComboBox<>(new String[]{"Free", "Occupied"})));

        JButton addMemoryBtn = UIKit.secondary("Add block");
        JButton removeMemoryBtn = UIKit.secondary("Remove selected");
        addMemoryBtn.addActionListener(e -> addMemoryBlock());
        removeMemoryBtn.addActionListener(e -> {
            UIKit.removeSelectedRows(memoryTable);
            renumber(memoryModel, false);
        });
        JPanel memoryBody = createTableBody(memoryTable, addMemoryBtn, removeMemoryBtn);
        UIKit.Section memorySection = new UIKit.Section("Current Memory Map", "Sizes accept B, KB, MB, GB or TB.",
                memoryBody);

        processModel = UIKit.model(new String[]{"Process #", "Name", "Size"}, 1);
        processModel.addRow(new Object[]{"1", "P1", "15GB"});
        processModel.addRow(new Object[]{"2", "P2", "30GB"});
        processModel.addRow(new Object[]{"3", "P3", "40GB"});
        processTable = UIKit.table(processModel);

        JButton addProcessBtn = UIKit.secondary("Add process");
        JButton removeProcessBtn = UIKit.secondary("Remove selected");
        addProcessBtn.addActionListener(e -> addProcess());
        removeProcessBtn.addActionListener(e -> {
            UIKit.removeSelectedRows(processTable);
            renumber(processModel, true);
        });
        JPanel processBody = createTableBody(processTable, addProcessBtn, removeProcessBtn);
        UIKit.Section processSection = new UIKit.Section("Processes", "Process sizes accept B, KB, MB, GB or TB.",
                processBody);

        JComponent inputs = UIKit.split(JSplitPane.HORIZONTAL_SPLIT, memorySection, processSection, 0.5);

        finalMapModel = UIKit.model(new String[]{"Block #", "Base Address", "Size", "Status / Process"}, false);
        JTable finalMapTable = UIKit.table(finalMapModel);
        JPanel algorithmButtons = UIKit.transparent(new FlowLayout(FlowLayout.LEFT, 8, 0));
        ButtonGroup fitGroup = new ButtonGroup();
        for (Fit fit : Fit.values()) {
            JToggleButton button = UIKit.option("Run " + fit.label, fitGroup);
            if (fit == Fit.FIRST) {
                button.setSelected(true);
            }
            algorithmButtons.add(button);
        }

        JPanel finalMapBody = UIKit.transparent(new BorderLayout(0, 10));
        finalMapBody.add(algorithmButtons, BorderLayout.NORTH);
        finalMapBody.add(UIKit.scroll(finalMapTable), BorderLayout.CENTER);
        UIKit.Section finalMapSection = new UIKit.Section("Final Memory Map",
                "Choose a run button; allocation logic is not implemented yet.", finalMapBody);

        add(UIKit.split(JSplitPane.VERTICAL_SPLIT, inputs, finalMapSection, 0.5), BorderLayout.CENTER);
    }

    private JPanel createTableBody(JTable table, JButton addButton, JButton removeButton) {
        JPanel body = UIKit.transparent(new BorderLayout(0, 10));
        body.add(UIKit.scroll(table), BorderLayout.CENTER);
        body.add(UIKit.row(addButton, removeButton), BorderLayout.SOUTH);
        return body;
    }

    private void addMemoryBlock() {
        UIKit.stopEditing(memoryTable);
        int index = memoryModel.getRowCount();
        memoryModel.addRow(new Object[]{String.valueOf(index), "", "", "Free"});
    }

    private void addProcess() {
        UIKit.stopEditing(processTable);
        int number = processModel.getRowCount() + 1;
        processModel.addRow(new Object[]{String.valueOf(number), "P" + number, ""});
    }

    private void renumber(DefaultTableModel model, boolean oneBased) {
        for (int row = 0; row < model.getRowCount(); row++) {
            model.setValueAt(String.valueOf(row + (oneBased ? 1 : 0)), row, 0);
        }
    }
}
