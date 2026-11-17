import javax.swing.*;
import java.awt.*;

/**
 * Installs FlatLaf (dark by default) with project-wide defaults.
 * FlatLaf is loaded by class name so the app still compiles and runs
 * (with Nimbus) if the FlatLaf jar is missing from the classpath.
 */
public final class Theme {
    public static final Color ACCENT = new Color(0x2563EB);
    public static final Color SUCCESS = new Color(0x16A34A);
    public static final Color DANGER = new Color(0xDC2626);

    private static final String FLAT_DARK = "com.formdev.flatlaf.FlatDarkLaf";
    private static final String FLAT_LIGHT = "com.formdev.flatlaf.FlatLightLaf";

    private static boolean dark = true;
    private static boolean flatLaf;

    private Theme() {}

    public static void install() {
        flatLaf = applyLookAndFeel(dark);
        if (!flatLaf) {
            try {
                UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
            } catch (Exception ignored) {
                // Default Metal look and feel stays in place.
            }
        }

        // UIManager.put() writes user defaults, which survive look-and-feel switches.
        UIManager.put("Component.arc", 10);
        UIManager.put("Button.arc", 10);
        UIManager.put("TextComponent.arc", 8);
        UIManager.put("Component.focusWidth", 1);
        UIManager.put("ScrollBar.showButtons", false);
        UIManager.put("ScrollBar.thumbArc", 999);
        UIManager.put("ScrollBar.thumbInsets", new Insets(2, 2, 2, 2));
        UIManager.put("ScrollBar.width", 12);
        UIManager.put("Table.rowHeight", 30);
        UIManager.put("Table.showHorizontalLines", true);
        UIManager.put("Table.showVerticalLines", false);
        UIManager.put("Table.intercellSpacing", new Dimension(0, 1));
        UIManager.put("SplitPane.dividerSize", 10);
        UIManager.put("SplitPaneDivider.style", "plain");
    }

    public static void toggle(Window window) {
        if (!flatLaf) {
            return;
        }
        dark = !dark;
        applyLookAndFeel(dark);
        SwingUtilities.updateComponentTreeUI(window);
    }

    public static boolean isDark() {
        return dark;
    }

    public static boolean isFlatLaf() {
        return flatLaf;
    }

    private static boolean applyLookAndFeel(boolean useDark) {
        try {
            UIManager.setLookAndFeel(useDark ? FLAT_DARK : FLAT_LIGHT);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
