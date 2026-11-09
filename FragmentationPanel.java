import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.EnumMap;
import java.util.Map;

public class FragmentationPanel extends JPanel {
    public enum Fit {
        FIRST("First Fit"), BEST("Best Fit"), NEXT("Next Fit");

        final String label;

        Fit(String label) {
            this.label = label;
        }
    }

    private static final String[] RESULT_COLUMNS = {"Process", "Size", "Block", "Leftover"};

    private final JTextField blocksField;
    private final JTextField processesField;
    private final Map<Fit, DefaultTableModel> resultModels = new EnumMap<>(Fit.class);
    private final Map<Fit, JLabel> summaryLabels = new EnumMap<>(Fit.class);
    private final UIKit.Section resultsSection;

    public FragmentationPanel() {
        UIKit.setupModule(this);
        add(UIKit.header("Memory Fragmentation",
                "Compare First Fit, Best Fit and Next Fit placement for the same blocks and processes."),
                BorderLayout.NORTH);

        blocksField = UIKit.field("32GB, 48GB, 64GB, 16GB", "e.g. 100KB, 500KB, 200KB", 28);
        processesField = UIKit.field("15GB, 30GB, 40GB", "e.g. 212KB, 417KB", 28);
        JButton runBtn = UIKit.primary("Simulate allocation");
        runBtn.addActionListener(e -> simulate());

        JPanel configBody = UIKit.transparent(new BorderLayout(0, 12));
        configBody.add(UIKit.form(1, "Memory block sizes", blocksField, "Process sizes", processesField),
                BorderLayout.CENTER);
        configBody.add(UIKit.toolbar(UIKit.muted("Comma-separated. Sizes accept B, KB, MB, GB or TB."), runBtn),
                BorderLayout.SOUTH);

        JPanel columns = UIKit.transparent(new GridLayout(1, 3, 12, 0));
        for (Fit fit : Fit.values()) {
            columns.add(createFitColumn(fit));
        }
        resultsSection = new UIKit.Section("Allocation Results", columns);

        JPanel center = UIKit.transparent(new BorderLayout(0, 16));
        center.add(new UIKit.Section("Configuration", configBody), BorderLayout.NORTH);
        center.add(resultsSection, BorderLayout.CENTER);
        add(center, BorderLayout.CENTER);
    }

    private JComponent createFitColumn(Fit fit) {
        DefaultTableModel model = UIKit.model(RESULT_COLUMNS, false);
        resultModels.put(fit, model);
        JTable table = UIKit.table(model);

        JLabel title = new JLabel(fit.label);
        UIKit.styleClass(title, "semibold", Font.BOLD, 1f);
        JLabel summary = UIKit.muted("Fragmentation: —");
        summaryLabels.put(fit, summary);

        JPanel column = UIKit.transparent(new BorderLayout(0, 8));
        column.add(title, BorderLayout.NORTH);
        column.add(UIKit.scroll(table), BorderLayout.CENTER);
        column.add(summary, BorderLayout.SOUTH);
        return column;
    }

    private void simulate() {
        long[] blocks = UIKit.parseSizeList(blocksField, "Memory block sizes", this);
        if (blocks == null) return;
        long[] processes = UIKit.parseSizeList(processesField, "Process sizes", this);
        if (processes == null) return;
        resultsSection.setHint(blocks.length + " blocks \u00b7 " + processes.length + " processes");
        onSimulate(blocks, processes);
    }

    /**
     * Hook for the placement algorithms. Sizes are in bytes. For each strategy call
     * {@link #showResults(Fit, Object[][], String)} with rows of {Process, Size, Block, Leftover}.
     */
    protected void onSimulate(long[] blockBytes, long[] processBytes) {
    }

    public void showResults(Fit fit, Object[][] rows, String fragmentationSummary) {
        resultModels.get(fit).setDataVector(rows, RESULT_COLUMNS);
        summaryLabels.get(fit).setText(fragmentationSummary);
    }
}
