import javax.swing.*;
import javax.swing.plaf.nimbus.NimbusLookAndFeel;
import java.awt.*;

/**
 * Installs the application look and feel and handles light/dark switching.
 *
 * <p>FlatLaf (dark by default) is used when its jar is on the classpath. It is loaded by
 * class name so the app still compiles and runs without it. In that case the built-in
 * Nimbus look and feel is used instead, and a hand-made dark palette is applied on top
 * of it, so the light/dark button works with or without FlatLaf.</p>
 */
public final class Theme {
    public static final Color ACCENT = new Color(0x2563EB);
    public static final Color SUCCESS = new Color(0x16A34A);
    public static final Color DANGER = new Color(0xDC2626);

    private static final String FLAT_DARK = "com.formdev.flatlaf.FlatDarkLaf";
    private static final String FLAT_LIGHT = "com.formdev.flatlaf.FlatLightLaf";

    /** Nimbus colour keys that make up the dark palette (null restores the light defaults). */
    private static final String[] NIMBUS_KEYS = {
            "control", "info", "nimbusBase", "nimbusLightBackground", "nimbusSelectionBackground",
            "nimbusSelectedText", "nimbusDisabledText", "nimbusFocus", "text",
            "Table.alternateRowColor", "TextField.background", "ComboBox.background"
    };

    private static boolean dark = true;
    private static boolean flatLaf;

    private Theme() {}

    /** Applies the look and feel and the project-wide component defaults. Call once at startup. */
    public static void install() {
        flatLaf = applyFlatLaf(dark);
        if (!flatLaf) {
            applyNimbus(dark);
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

    /** Switches between light and dark and refreshes every component in the window. */
    public static void toggle(Window window) {
        dark = !dark;
        if (flatLaf) {
            applyFlatLaf(dark);
        } else {
            applyNimbus(dark);
        }
        for (Window open : Window.getWindows()) {
            SwingUtilities.updateComponentTreeUI(open);
        }
        window.repaint();
    }

    public static boolean isDark() {
        return dark;
    }

    public static boolean isFlatLaf() {
        return flatLaf;
    }

    private static boolean applyFlatLaf(boolean useDark) {
        try {
            UIManager.setLookAndFeel(useDark ? FLAT_DARK : FLAT_LIGHT);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /** Nimbus fallback: overrides the palette keys first, then (re)installs the look and feel. */
    private static void applyNimbus(boolean useDark) {
        for (String key : NIMBUS_KEYS) {
            UIManager.put(key, null);
        }
        if (useDark) {
            UIManager.put("control", new Color(0x2B2D31));
            UIManager.put("info", new Color(0x3A3D42));
            UIManager.put("nimbusBase", new Color(0x1E3A8A));
            UIManager.put("nimbusLightBackground", new Color(0x1F2023));
            UIManager.put("nimbusSelectionBackground", ACCENT);
            UIManager.put("nimbusSelectedText", Color.WHITE);
            UIManager.put("nimbusDisabledText", new Color(0x8A8D93));
            UIManager.put("nimbusFocus", ACCENT);
            UIManager.put("text", new Color(0xE6E6E6));
            UIManager.put("Table.alternateRowColor", new Color(0x26282C));
        }
        try {
            UIManager.setLookAndFeel(new NimbusLookAndFeel());
        } catch (Exception ignored) {
            // The default Metal look and feel stays in place.
        }
    }
}
