package com.pawcare.gui.components;

import com.pawcare.theme.Colors;
import com.pawcare.theme.Theme;

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.RenderingHints;

/**
 * A {@link JPanel} that paints a rounded background, an optional hairline border and an
 * optional soft shadow.
 *
 * <p>Swing panels are always rectangular. Rather than fight the look and feel, this class
 * stays non-opaque and paints the shape itself, which is what gives the application its
 * card-based appearance.</p>
 */
public class RoundedPanel extends JPanel {

    private int radius = Theme.RADIUS_CARD;
    private Color cardBackground = Theme.c().surface;
    private Color borderColor = null;
    private boolean shadow = false;
    private int shadowSize = 7;
    private int shadowOffset = 2;

    public RoundedPanel() {
        setOpaque(false);
    }

    public RoundedPanel(LayoutManager layout) {
        super(layout);
        setOpaque(false);
    }

    // ------------------------------------------------------------------
    // Appearance
    // ------------------------------------------------------------------

    public void setRadius(int radius) {
        this.radius = radius;
        repaint();
    }

    public int getRadius() {
        return radius;
    }

    public void setCardBackground(Color color) {
        this.cardBackground = color;
        repaint();
    }

    public Color getCardBackground() {
        return cardBackground;
    }

    public void setBorderColor(Color color) {
        this.borderColor = color;
        repaint();
    }

    public void setShadow(boolean shadow) {
        this.shadow = shadow;
        repaint();
    }

    public void setShadowSize(int shadowSize) {
        this.shadowSize = shadowSize;
        repaint();
    }

    /** Reserves space at the bottom right so the shadow is never clipped. */
    @Override
    public Insets getInsets() {
        Insets base = super.getInsets();
        if (!shadow) {
            return base;
        }
        return new Insets(base.top, base.left,
                base.bottom + shadowSize + shadowOffset,
                base.right + shadowSize + shadowOffset);
    }

    // ------------------------------------------------------------------
    // Painting
    // ------------------------------------------------------------------

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int reserved = shadow ? shadowSize + shadowOffset : 0;
        int cardWidth = getWidth() - reserved;
        int cardHeight = getHeight() - reserved;

        if (cardWidth <= 0 || cardHeight <= 0) {
            g2.dispose();
            return;
        }

        if (shadow) {
            // Concentric rounded rectangles, faint at the outside and stronger near the
            // card, approximate a blurred drop shadow closely enough for a flat design.
            Color shadowColour = Theme.c().shadow;
            for (int i = shadowSize; i >= 1; i--) {
                int alpha = Math.max(3, shadowColour.getAlpha() / (i + 2));
                g2.setColor(Colors.alpha(shadowColour, alpha));
                g2.fillRoundRect(shadowOffset, shadowOffset,
                        cardWidth + i - 1, cardHeight + i - 1,
                        radius + i, radius + i);
            }
        }

        if (cardBackground != null) {
            g2.setColor(cardBackground);
            g2.fillRoundRect(0, 0, cardWidth - 1, cardHeight - 1, radius, radius);
        }

        if (borderColor != null) {
            g2.setColor(borderColor);
            g2.drawRoundRect(0, 0, cardWidth - 2, cardHeight - 2, radius, radius);
        }

        g2.dispose();
        super.paintComponent(g);
    }
}
