import javax.swing.*;
import java.awt.*;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.Arrays;

/** Classic disk-head chart: track numbers along the top axis, one row per request, animated line. */
public class HeadMovementChart extends JComponent {
    private static final int PAD_LEFT = 24;
    private static final int PAD_RIGHT = 24;
    private static final int PAD_TOP = 34;
    private static final int PAD_BOTTOM = 16;

    private int[] sequence = new int[0];
    private int maxTrack = 199;
    private float progress;
    private final Timer timer;

    public HeadMovementChart() {
        setOpaque(false);
        setPreferredSize(new Dimension(420, 260));
        setMinimumSize(new Dimension(200, 160));
        timer = new Timer(16, e -> {
            progress = Math.min(sequence.length - 1, progress + 0.06f);
            if (progress >= sequence.length - 1) {
                ((Timer) e.getSource()).stop();
            }
            repaint();
        });
    }

    public void setSequence(int[] newSequence, int newMaxTrack, boolean animate) {
        timer.stop();
        sequence = newSequence.clone();
        maxTrack = Math.max(1, newMaxTrack);
        progress = animate ? 0 : Math.max(0, sequence.length - 1);
        setToolTipText("Head movement order: " + Arrays.toString(sequence));
        if (animate && sequence.length > 1) {
            timer.start();
        }
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setFont(getFont() != null ? getFont() : UIManager.getFont("Label.font"));

        int width = getWidth() - PAD_LEFT - PAD_RIGHT;
        int height = getHeight() - PAD_TOP - PAD_BOTTOM;
        FontMetrics metrics = g2.getFontMetrics();

        if (sequence.length < 2 || width <= 0 || height <= 0) {
            g2.setColor(UIKit.mutedTextColor());
            String message = "Run an algorithm to plot head movement.";
            g2.drawString(message, (getWidth() - metrics.stringWidth(message)) / 2, getHeight() / 2);
            g2.dispose();
            return;
        }

        Color grid = UIKit.borderColor();
        Color muted = UIKit.mutedTextColor();
        Color text = UIKit.textColor();

        g2.setColor(grid);
        g2.drawLine(PAD_LEFT, PAD_TOP, PAD_LEFT + width, PAD_TOP);
        for (int i = 0; i <= 4; i++) {
            int track = Math.round(maxTrack * i / 4f);
            int x = xFor(track, width);
            g2.setColor(grid);
            g2.drawLine(x, PAD_TOP - 4, x, PAD_TOP + height);
            g2.setColor(muted);
            String label = String.valueOf(track);
            g2.drawString(label, x - metrics.stringWidth(label) / 2, PAD_TOP - 10);
        }

        float rowHeight = height / (float) (sequence.length - 1);
        int completed = (int) Math.floor(progress);
        float partial = progress - completed;

        g2.setStroke(new BasicStroke(2.25f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(Theme.ACCENT);
        for (int i = 0; i < completed && i + 1 < sequence.length; i++) {
            g2.draw(new Line2D.Float(point(i, width, rowHeight), point(i + 1, width, rowHeight)));
        }
        if (completed + 1 < sequence.length && partial > 0) {
            Point2D from = point(completed, width, rowHeight);
            Point2D to = point(completed + 1, width, rowHeight);
            g2.draw(new Line2D.Double(from.getX(), from.getY(),
                    from.getX() + (to.getX() - from.getX()) * partial,
                    from.getY() + (to.getY() - from.getY()) * partial));
        }

        int visiblePoints = Math.min(sequence.length, completed + 1);
        for (int i = 0; i < visiblePoints; i++) {
            Point2D p = point(i, width, rowHeight);
            int radius = i == 0 ? 6 : 4;
            g2.setColor(i == 0 ? Theme.SUCCESS : Theme.ACCENT);
            g2.fillOval((int) p.getX() - radius, (int) p.getY() - radius, radius * 2, radius * 2);
            g2.setColor(text);
            String label = String.valueOf(sequence[i]);
            boolean rightSide = p.getX() < PAD_LEFT + width - 40;
            int labelX = rightSide ? (int) p.getX() + 8 : (int) p.getX() - 8 - metrics.stringWidth(label);
            g2.drawString(label, labelX, (int) p.getY() + metrics.getAscent() / 2 - 1);
        }
        g2.dispose();
    }

    private Point2D point(int index, int width, float rowHeight) {
        return new Point2D.Float(xFor(sequence[index], width), PAD_TOP + 8 + index * (rowHeight - 8f / (sequence.length - 1)));
    }

    private int xFor(int track, int width) {
        return PAD_LEFT + Math.round(width * (track / (float) maxTrack));
    }
}
