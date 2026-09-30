package com.pawcare.gui.components;

import com.pawcare.theme.Theme;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/** A small coloured pill used for statuses such as "Scheduled" or "Overdue". */
public class StatusBadge extends JLabel {

    private Color badgeBackground;
    private Color badgeForeground;

    public StatusBadge() {
        this("", Theme.c().textSecondary, Theme.c().surfaceAlt);
    }

    public StatusBadge(String text, Color foreground, Color background) {
        super(text);
        setFont(Theme.FONT_SMALL_MEDIUM);
        setOpaque(false);
        setHorizontalAlignment(CENTER);
        setBorder(BorderFactory.createEmptyBorder(3, 10, 3, 10));
        setColors(foreground, background);
    }

    /** Restyles the badge. Renderers call this for every cell they paint. */
    public final void setColors(Color foreground, Color background) {
        this.badgeForeground = foreground;
        this.badgeBackground = background;
        setForeground(foreground);
    }

    public void setTextAndColors(String text, Color foreground, Color background) {
        setText(text);
        setColors(foreground, background);
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension size = super.getPreferredSize();
        return new Dimension(Math.max(size.width, 54), Math.max(size.height, 22));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int width = getWidth();
        int height = getHeight();
        if (width > 0 && height > 0 && badgeBackground != null) {
            g2.setColor(badgeBackground);
            // A radius of half the height produces a pill shape.
            g2.fillRoundRect(0, 0, width, height, height, height);
        }
        g2.dispose();

        super.paintComponent(g);

        // A hairline in the same hue as the text keeps light badges legible.
        if (badgeForeground != null && width > 0 && height > 0) {
            Graphics2D border = (Graphics2D) g.create();
            border.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            border.setColor(new Color(badgeForeground.getRed(), badgeForeground.getGreen(),
                    badgeForeground.getBlue(), 60));
            border.drawRoundRect(0, 0, width - 1, height - 1, height, height);
            border.dispose();
        }
    }

    /** Measures the text so callers can align several badges in a row. */
    public int textWidth() {
        FontMetrics metrics = getFontMetrics(getFont());
        return metrics.stringWidth(getText());
    }
}
