import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Draws a CPU-scheduling Gantt chart: one block per segment, process name inside the
 * block and the time marks along the bottom edge. Idle gaps are drawn as hatched-gray blocks.
 * Sits inside a scroll pane and stretches to the viewport when the chart is narrow.
 */
public class GanttChart extends JComponent implements Scrollable {
    private static final int PAD = 24;
    private static final int BLOCK_HEIGHT = 44;
    private static final int MIN_UNIT = 28;
    private static final Color[] PALETTE = {
            new Color(0x2563EB), new Color(0xD97706), new Color(0x16A34A), new Color(0x9333EA),
            new Color(0xDC2626), new Color(0x0891B2), new Color(0xDB2777), new Color(0x65A30D)
    };

    /** One block of the chart: CPU time [start, end) used by a process (row index), or -1 for idle. */
    public static final class Segment {
        public final int process;
        public final double start;
        public final double end;

        public Segment(int process, double start, double end) {
            this.process = process;
            this.start = start;
            this.end = end;
        }
    }

    private final List<Segment> segments = new ArrayList<>();
    private final List<String> names = new ArrayList<>();
    private double totalTime;

    public GanttChart() {
        setOpaque(false);
    }

    /** Replaces the chart contents. {@code processNames} is indexed like the segments' process field. */
    public void setSchedule(List<Segment> newSegments, List<String> processNames) {
        segments.clear();
        segments.addAll(newSegments);
        names.clear();
        names.addAll(processNames);
        totalTime = newSegments.isEmpty() ? 0 : newSegments.get(newSegments.size() - 1).end;
        revalidate();
        repaint();
    }

    @Override
    public Dimension getPreferredSize() {
        int width = (int) Math.min(Integer.MAX_VALUE,
                2.0 * PAD + Math.max(1, totalTime) * MIN_UNIT);
        return new Dimension(width, BLOCK_HEIGHT + 56);
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (segments.isEmpty() || totalTime == 0) {
            return;
        }
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setFont(getFont() != null ? getFont() : UIManager.getFont("Label.font"));
        FontMetrics metrics = g2.getFontMetrics();

        double unit = (getWidth() - 2.0 * PAD) / totalTime;
        int top = 16;
        double lastTickEnd = Double.MIN_VALUE;

        for (Segment segment : segments) {
            int x = PAD + (int) Math.round(segment.start * unit);
            int w = (int) Math.round((segment.end - segment.start) * unit);
            boolean idle = segment.process < 0;

            g2.setColor(idle ? UIKit.mix(UIKit.cardBackground(), UIKit.mutedTextColor(), 0.35f)
                    : PALETTE[segment.process % PALETTE.length]);
            g2.fillRoundRect(x, top, w, BLOCK_HEIGHT, 8, 8);
            g2.setColor(UIKit.cardBackground());
            g2.drawRoundRect(x, top, w, BLOCK_HEIGHT, 8, 8); // thin gap line between neighbours

            String label = idle ? "Idle" : names.get(segment.process);
            if (metrics.stringWidth(label) + 6 <= w) {
                g2.setColor(idle ? UIKit.textColor() : Color.WHITE);
                g2.drawString(label, x + (w - metrics.stringWidth(label)) / 2,
                        top + (BLOCK_HEIGHT + metrics.getAscent() - metrics.getDescent()) / 2);
            }

            // Time marks sit under the left edge of each block; the last block also gets its end mark.
            g2.setColor(UIKit.mutedTextColor());
            String startLabel = formatTime(segment.start);
            g2.drawString(startLabel, x - metrics.stringWidth(startLabel) / 2, top + BLOCK_HEIGHT + 18);
            lastTickEnd = segment.end;
        }
        String endLabel = formatTime(lastTickEnd);
        g2.setColor(UIKit.mutedTextColor());
        g2.drawString(endLabel, PAD + (int) Math.round(totalTime * unit) - metrics.stringWidth(endLabel) / 2,
                top + BLOCK_HEIGHT + 18);
        g2.dispose();
    }

    private static String formatTime(double time) {
        return new java.text.DecimalFormat("0.##########").format(time);
    }

    // ------------------------------------------------------------ Scrollable

    @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
    @Override public int getScrollableUnitIncrement(Rectangle r, int orientation, int direction) { return 24; }
    @Override public int getScrollableBlockIncrement(Rectangle r, int orientation, int direction) { return 120; }
    @Override public boolean getScrollableTracksViewportHeight() { return true; }

    /** Fill the viewport when the chart is narrower than it, otherwise scroll horizontally. */
    @Override
    public boolean getScrollableTracksViewportWidth() {
        return getParent() instanceof JViewport && getParent().getWidth() > getPreferredSize().width;
    }
}
