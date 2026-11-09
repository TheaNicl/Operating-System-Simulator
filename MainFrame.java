import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

public class MainFrame extends JFrame {
    private static final String WELCOME = "Welcome";
    private static final String[] MODULES = {
            "CPU Scheduling", "Banker's Algorithm", "Paging", "Page Replacement",
            "Segmentation", "Fragmentation", "Disk Scheduling", "Multiprogramming"
    };
    private static final String[] DESCRIPTIONS = {
            "FCFS, SJF, Priority and Round Robin with Gantt chart.",
            "Allocation and maximum matrices with safe-sequence check.",
            "Logical to physical address translation via page table.",
            "FIFO, LRU and Optimal with a step-by-step frame trace.",
            "Base and limit table for logical segments.",
            "First, Best and Next Fit block allocation.",
            "Six seek algorithms with animated head movement.",
            "Job queue and chronological execution log."
    };

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel mainContainer = new JPanel(cardLayout);
    private final JComboBox<String> moduleSelector;
    private final JButton themeButton = new JButton();

    public MainFrame() {
        setTitle("Operating System\u2013 System Simulator");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(960, 640));
        setSize(1200, 800);
        setLocationRelativeTo(null);

        String[] items = new String[MODULES.length + 1];
        items[0] = WELCOME;
        for (int i = 0; i < MODULES.length; i++) {
            items[i + 1] = (i + 1) + ". " + MODULES[i];
        }
        moduleSelector = new JComboBox<>(items);
        moduleSelector.setMaximumRowCount(items.length);
        moduleSelector.getAccessibleContext().setAccessibleName("Select OS module");
        moduleSelector.addActionListener(e -> {
            String selected = (String) moduleSelector.getSelectedItem();
            if (selected != null) {
                cardLayout.show(mainContainer, selected);
            }
        });

        setJMenuBar(createMenuBar());
        add(createTopBar(), BorderLayout.NORTH);

        JPanel[] panels = {
                new CPUSchedulingPanel(), new BankersPanel(), new PagingPanel(), new PageReplacementPanel(),
                new SegmentationPanel(), new FragmentationPanel(), new DiskSchedulingPanel(),
                new MultiprogrammingPanel()
        };
        mainContainer.add(createWelcomePanel(), WELCOME);
        for (int i = 0; i < panels.length; i++) {
            mainContainer.add(panels[i], items[i + 1]);
        }
        add(mainContainer, BorderLayout.CENTER);
    }

    private void showModule(int index) {
        moduleSelector.setSelectedIndex(index);
    }

    private JComponent createTopBar() {
        JLabel appName = new JLabel("OS Simulator");
        UIKit.styleClass(appName, "h3", Font.BOLD, 1.25f);
        JPanel brand = UIKit.row(appName, UIKit.muted("Program Flow System Simulator"));

        JLabel selectorLabel = new JLabel("Module");
        selectorLabel.setLabelFor(moduleSelector);
        moduleSelector.setPreferredSize(new Dimension(230, moduleSelector.getPreferredSize().height));

        themeButton.setEnabled(Theme.isFlatLaf());
        themeButton.addActionListener(e -> toggleTheme());
        updateThemeButton();

        JPanel bar = new JPanel(new BorderLayout(16, 0));
        bar.setBorder(new UIKit.BottomLineBorder(new Insets(10, 24, 10, 24)));
        bar.add(brand, BorderLayout.WEST);
        bar.add(UIKit.row(selectorLabel, moduleSelector, themeButton), BorderLayout.EAST);
        return bar;
    }

    private JMenuBar createMenuBar() {
        int shortcut = InputEvent.CTRL_DOWN_MASK;
        JMenuBar menuBar = new JMenuBar();

        JMenu file = new JMenu("File");
        file.setMnemonic(KeyEvent.VK_F);
        JMenuItem exit = new JMenuItem("Exit");
        exit.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, shortcut));
        exit.addActionListener(e -> dispose());
        file.add(exit);

        JMenu view = new JMenu("View");
        view.setMnemonic(KeyEvent.VK_V);
        JMenuItem toggleTheme = new JMenuItem("Toggle light / dark theme");
        toggleTheme.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_T, shortcut));
        toggleTheme.setEnabled(Theme.isFlatLaf());
        toggleTheme.addActionListener(e -> toggleTheme());
        view.add(toggleTheme);

        JMenu modules = new JMenu("Modules");
        modules.setMnemonic(KeyEvent.VK_M);
        for (int i = 0; i <= MODULES.length; i++) {
            final int index = i;
            JMenuItem item = new JMenuItem(i == 0 ? WELCOME : (i + ". " + MODULES[i - 1]));
            item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_0 + i, shortcut));
            item.addActionListener(e -> showModule(index));
            modules.add(item);
            if (i == 0) {
                modules.addSeparator();
            }
        }

        menuBar.add(file);
        menuBar.add(view);
        menuBar.add(modules);
        return menuBar;
    }

    private void toggleTheme() {
        Theme.toggle(this);
        updateThemeButton();
    }

    private void updateThemeButton() {
        themeButton.setText(Theme.isDark() ? "Light mode" : "Dark mode");
        themeButton.setToolTipText("Switch theme (Ctrl+T)");
    }

    private JComponent createWelcomePanel() {
        JPanel grid = UIKit.transparent(new GridLayout(0, 4, 12, 12));
        for (int i = 0; i < MODULES.length; i++) {
            grid.add(createModuleCard(i));
        }

        JPanel content = UIKit.transparent(new BorderLayout(0, 24));
        content.add(UIKit.header("Welcome to the System Simulator",
                "Explore eight core operating-system concepts. Pick a module to get started."), BorderLayout.NORTH);
        content.add(grid, BorderLayout.CENTER);
        content.add(UIKit.muted("Tip: Ctrl+1 to Ctrl+8 jumps straight to a module, Ctrl+0 returns here."),
                BorderLayout.SOUTH);

        JPanel page = new JPanel(new BorderLayout());
        page.setBorder(BorderFactory.createEmptyBorder(32, 32, 32, 32));
        page.add(content, BorderLayout.NORTH);

        JScrollPane scroll = UIKit.scroll(page);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        return scroll;
    }

    private JButton createModuleCard(int index) {
        JLabel number = UIKit.muted(String.format("%02d", index + 1));
        JLabel title = new JLabel(MODULES[index]);
        UIKit.styleClass(title, "h4", Font.BOLD, 1.1f);
        JLabel description = UIKit.muted("<html>" + DESCRIPTIONS[index] + "</html>");

        JButton card = new JButton();
        card.setLayout(new BorderLayout(0, 6));
        card.putClientProperty("FlatLaf.style", "arc: 14");
        card.setBorder(BorderFactory.createCompoundBorder(card.getBorder(),
                BorderFactory.createEmptyBorder(12, 12, 12, 12)));
        card.setPreferredSize(new Dimension(220, 132));
        card.getAccessibleContext().setAccessibleName(MODULES[index]);
        card.setToolTipText("Open " + MODULES[index]);

        JPanel top = UIKit.transparent(new BorderLayout(0, 4));
        top.add(number, BorderLayout.NORTH);
        top.add(title, BorderLayout.CENTER);
        card.add(top, BorderLayout.NORTH);
        card.add(description, BorderLayout.CENTER);
        card.addActionListener(e -> showModule(index + 1));
        return card;
    }

    public static void main(String[] args) {
        System.setProperty("apple.laf.useScreenMenuBar", "true");
        SwingUtilities.invokeLater(() -> {
            Theme.install();
            new MainFrame().setVisible(true);
        });
    }
}
