import javax.swing.*;
import javax.swing.plaf.nimbus.NimbusLookAndFeel;
import java.awt.*;

/**
 * Installs the application light look and feel.
 *
 * <p>FlatLaf Light is used when its jar is on the classpath. It is loaded by class name
 * so the app still compiles and runs without it. In that case the built-in Nimbus
 * look and feel is used instead.</p>
 */
public final class Theme {
    public static final Color ACCENT = new Color(0x2563EB);
    public static final Color SUCCESS = new Color(0x16A34A);
    public static final Color DANGER = new Color(0xDC2626);

    private static final String FLAT_LIGHT = "com.formdev.flatlaf.FlatLightLaf";
    private static boolean flatLaf;

    private Theme() {}

    /** Applies the look and feel and the project-wide component defaults. Call once at startup. */
    public static void install() {
        flatLaf = applyFlatLaf();
        if (!flatLaf) {
            applyNimbus();
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

    public static boolean isFlatLaf() {
        return flatLaf;
    }

    private static boolean applyFlatLaf() {
        try {
            UIManager.setLookAndFeel(FLAT_LIGHT);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /** Nimbus fallback when FlatLaf is not available. */
    private static void applyNimbus() {
        try {
            UIManager.setLookAndFeel(new NimbusLookAndFeel());
        } catch (Exception ignored) {
            // The default Metal look and feel stays in place.
        }
    }
}
