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
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;

/**
 * A vertical bar chart drawn with Java2D.
 *
 * <p>The project uses no charting library, so this panel paints the bars, the gridlines,
 * the labels and the values itself. It is fed a list of {@link Bar} objects and works out
 * the scale from the largest value, which means the same class draws the species
 * distribution, the appointment states and the weekly visit count.</p>
 */
public class BarChartPanel extends JPanel {

    /** One bar: a label, a value and the colour it is drawn in. */
    public static final class Bar {

        private final String label;
        private final int value;
        private final Color color;

        public Bar(String label, int value, Color color) {
            this.label = label;
            this.value = value;
            this.color = color;
        }
    }

    /** Number of horizontal gridlines drawn behind the bars. */
    private static final int GRID_LINES = 4;

    private static final int AXIS_HEIGHT = 42;
    private static final int TOP_PADDING = 22;
    private static final int BAR_GAP = 18;

    private List<Bar> bars = new ArrayList<>();
    private String valueSuffix = "";
    private String emptyMessage = "No data to chart yet";

    public BarChartPanel() {
        setOpaque(false);
    }

    /** Replaces the data and repaints. */
    public void setBars(List<Bar> bars) {
        this.bars = bars == null ? new ArrayList<>() : new ArrayList<>(bars);
        repaint();
    }

    /** Text appended after every value, e.g. " visits". */
    public void setValueSuffix(String suffix) {
        this.valueSuffix = suffix == null ? "" : suffix;
    }

    /** Message shown when there is nothing to draw. */
    public void setEmptyMessage(String message) {
        this.emptyMessage = message == null ? "" : message;
        repaint();
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(320, 220);
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

        int baseline = height - AXIS_HEIGHT;
        int top = TOP_PADDING;

        if (bars.isEmpty() || maxValue() == 0) {
            drawCentredMessage(g2, emptyMessage, width, height);
            g2.dispose();
            return;
        }

        // Gridlines and their value labels give the bars a scale to be read against.
        int max = maxValue();
        g2.setStroke(new BasicStroke(1f));
        g2.setFont(Theme.FONT_TINY);
        FontMetrics tiny = g2.getFontMetrics();

        for (int i = 0; i <= GRID_LINES; i++) {
            int y = baseline - (int) Math.round((baseline - top) * (i / (double) GRID_LINES));
            g2.setColor(Theme.c().chartGrid);
            g2.drawLine(34, y, width - 6, y);

            g2.setColor(Theme.c().textMuted);
            String value = String.valueOf(Math.round(max * (i / (double) GRID_LINES)));
            g2.drawString(value, 30 - tiny.stringWidth(value), y + 4);
        }

        int chartLeft = 40;
        int chartWidth = width - chartLeft - 10;
        int step = chartWidth / Math.max(1, bars.size());
        int barWidth = Math.min(58, Math.max(16, step - BAR_GAP));

        g2.setFont(Theme.FONT_SMALL_MEDIUM);
        FontMetrics labelMetrics = g2.getFontMetrics();

        for (int i = 0; i < bars.size(); i++) {
            Bar bar = bars.get(i);
            int centre = chartLeft + step * i + step / 2;
            int left = centre - barWidth / 2;
            int barHeight = (int) Math.round((baseline - top) * (bar.value / (double) max));
            int barTop = baseline - barHeight;

            // The bar itself, with rounded top corners.
            g2.setColor(bar.color);
            g2.fill(new RoundRectangle2D.Double(left, barTop,
                    barWidth, Math.max(3, barHeight), 7, 7));

            // The value above the bar.
            g2.setColor(Theme.c().textPrimary);
            String value = bar.value + valueSuffix;
            g2.drawString(value, centre - labelMetrics.stringWidth(value) / 2, barTop - 7);

            // The label below the baseline, wrapped onto two lines when it is long.
            g2.setFont(Theme.FONT_TINY);
            FontMetrics captionMetrics = g2.getFontMetrics();
            g2.setColor(Theme.c().textSecondary);
            drawWrappedLabel(g2, captionMetrics, bar.label, centre, baseline + 16, step - 4);
            g2.setFont(Theme.FONT_SMALL_MEDIUM);
            labelMetrics = g2.getFontMetrics();
        }

        // The baseline is drawn last so it sits over the bars.
        g2.setColor(Theme.c().border);
        g2.setStroke(new BasicStroke(1f));
        g2.drawLine(chartLeft - 6, baseline, width - 6, baseline);

        g2.dispose();
    }

    private int maxValue() {
        int max = 0;
        for (Bar bar : bars) {
            max = Math.max(max, bar.value);
        }
        return max;
    }

    private void drawCentredMessage(Graphics2D g2, String message, int width, int height) {
        g2.setFont(Theme.FONT_SMALL);
        g2.setColor(Theme.c().textMuted);
        FontMetrics metrics = g2.getFontMetrics();
        g2.drawString(message, (width - metrics.stringWidth(message)) / 2,
                height / 2 + metrics.getAscent() / 2);
    }

    /** Splits a long label over two lines so neighbouring bars never overlap. */
    private void drawWrappedLabel(Graphics2D g2, FontMetrics metrics, String text,
                                  int centre, int y, int available) {
        if (text == null) {
            return;
        }
        if (metrics.stringWidth(text) <= available || !text.contains(" ")) {
            g2.drawString(text, centre - metrics.stringWidth(text) / 2, y);
            return;
        }

        int split = text.lastIndexOf(' ', text.length() / 2);
        if (split <= 0) {
            split = text.indexOf(' ');
        }
        if (split <= 0) {
            g2.drawString(text, centre - metrics.stringWidth(text) / 2, y);
            return;
        }

        String first = text.substring(0, split);
        String second = text.substring(split + 1);

        g2.drawString(first, centre - metrics.stringWidth(first) / 2, y);
        g2.drawString(second, centre - metrics.stringWidth(second) / 2, y + metrics.getHeight());
    }
}
