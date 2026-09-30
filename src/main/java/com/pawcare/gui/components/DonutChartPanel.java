package com.pawcare.gui.components;

import com.pawcare.theme.Theme;

import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.util.ArrayList;
import java.util.List;

/**
 * A ring chart with a legend, drawn with Java2D.
 *
 * <p>Each {@link Slice} becomes an arc of the ring; the hole in the middle carries the
 * total. The legend shows the label, the count and the share, so the same panel can
 * present the species mix, the appointment states or the vaccination reminder states.</p>
 */
public class DonutChartPanel extends JPanel {

    /** One segment of the ring. */
    public static final class Slice {

        private final String label;
        private final int value;
        private final Color color;

        public Slice(String label, int value, Color color) {
            this.label = label;
            this.value = value;
            this.color = color;
        }
    }

    private static final int RING_THICKNESS = 20;
    private static final int LEGEND_WIDTH = 182;
    private static final int LEGEND_ROW = 26;

    private List<Slice> slices = new ArrayList<>();
    private String centreCaption = "Total";
    private String emptyMessage = "No data to chart yet";

    public DonutChartPanel() {
        setOpaque(false);
    }

    public void setSlices(List<Slice> slices) {
        this.slices = slices == null ? new ArrayList<>() : new ArrayList<>(slices);
        repaint();
    }

    /** Word under the total inside the ring, e.g. "patients". */
    public void setCentreCaption(String caption) {
        this.centreCaption = caption == null ? "" : caption;
        repaint();
    }

    public void setEmptyMessage(String message) {
        this.emptyMessage = message == null ? "" : message;
        repaint();
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(340, 230);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int width = getWidth();
        int height = getHeight();
        int total = total();

        if (slices.isEmpty() || total == 0) {
            g2.setFont(Theme.FONT_SMALL);
            g2.setColor(Theme.c().textMuted);
            FontMetrics metrics = g2.getFontMetrics();
            g2.drawString(emptyMessage, (width - metrics.stringWidth(emptyMessage)) / 2,
                    height / 2 + metrics.getAscent() / 2);
            g2.dispose();
            return;
        }

        // The ring occupies the left, the legend the right.
        int available = Math.max(80, width - LEGEND_WIDTH);
        int diameter = Math.min(available, height) - 24;
        int ringX = (available - diameter) / 2;
        int ringY = (height - diameter) / 2;

        double start = 90;   // begin at twelve o'clock and sweep clockwise
        for (Slice slice : slices) {
            if (slice.value <= 0) {
                continue;
            }
            double extent = 360.0 * slice.value / total;
            g2.setColor(slice.color);
            g2.setStroke(new BasicStroke(RING_THICKNESS, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
            // A tiny gap between segments keeps the individual colours readable.
            g2.draw(new Arc2D.Double(ringX + RING_THICKNESS / 2.0, ringY + RING_THICKNESS / 2.0,
                    diameter - RING_THICKNESS, diameter - RING_THICKNESS,
                    start, -Math.max(0.6, extent - 1.2), Arc2D.OPEN));
            start -= extent;
        }

        drawCentre(g2, ringX, ringY, diameter, total);
        drawLegend(g2, width - LEGEND_WIDTH, height, total);

        g2.dispose();
    }

    private void drawCentre(Graphics2D g2, int ringX, int ringY, int diameter, int total) {
        int centreX = ringX + diameter / 2;
        int centreY = ringY + diameter / 2;

        // A disc in the surface colour punches the hole in the ring.
        g2.setColor(Theme.c().surface);
        int hole = diameter - RING_THICKNESS * 2;
        g2.fill(new Ellipse2D.Double(centreX - hole / 2.0, centreY - hole / 2.0, hole, hole));

        g2.setFont(Theme.FONT_DISPLAY);
        g2.setColor(Theme.c().textPrimary);
        FontMetrics big = g2.getFontMetrics();
        String value = String.valueOf(total);
        g2.drawString(value, centreX - big.stringWidth(value) / 2,
                centreY + big.getAscent() / 2 - 2);

        if (!centreCaption.isEmpty()) {
            g2.setFont(Theme.FONT_TINY);
            g2.setColor(Theme.c().textMuted);
            FontMetrics small = g2.getFontMetrics();
            g2.drawString(centreCaption, centreX - small.stringWidth(centreCaption) / 2,
                    centreY + big.getAscent() / 2 + small.getHeight() + 1);
        }
    }

    private void drawLegend(Graphics2D g2, int left, int height, int total) {
        int rows = slices.size();
        int top = Math.max(6, (height - rows * LEGEND_ROW) / 2);

        for (int i = 0; i < rows; i++) {
            Slice slice = slices.get(i);
            int y = top + i * LEGEND_ROW;

            g2.setColor(slice.color);
            g2.fillRoundRect(left, y + 5, 10, 10, 3, 3);

            g2.setFont(Theme.FONT_SMALL);
            g2.setColor(Theme.c().textPrimary);
            g2.drawString(slice.label, left + 18, y + 14);

            int share = Math.round(slice.value * 100f / total);
            String count = slice.value + "  ·  " + share + "%";
            g2.setFont(Theme.FONT_TINY);
            g2.setColor(Theme.c().textSecondary);
            g2.drawString(count, left + LEGEND_WIDTH - 18 - g2.getFontMetrics().stringWidth(count),
                    y + 14);
        }
    }

    private int total() {
        int total = 0;
        for (Slice slice : slices) {
            total += Math.max(0, slice.value);
        }
        return total;
    }
}
