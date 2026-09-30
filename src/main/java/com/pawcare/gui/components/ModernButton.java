package com.pawcare.gui.components;

import com.pawcare.theme.Colors;
import com.pawcare.theme.IconFactory;
import com.pawcare.theme.Theme;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/**
 * The application's button, offering five visual variants on a rounded surface.
 *
 * <p>The look and feel supplied button chrome is switched off
 * ({@code contentAreaFilled(false)}, {@code borderPainted(false)}) and the shape is
 * painted here instead, so hover and press states use the palette directly.</p>
 */
public class ModernButton extends JButton {

    /** Visual weight and semantic colour of the button. */
    public enum Variant {
        PRIMARY, SECONDARY, GHOST, DANGER, SUCCESS
    }

    private final Variant variant;
    private final String iconName;
    private final int iconSize;

    public ModernButton(String text) {
        this(text, Variant.PRIMARY, null);
    }

    public ModernButton(String text, Variant variant) {
        this(text, variant, null);
    }

    public ModernButton(String text, Variant variant, String iconName) {
        this(text, variant, iconName, 15);
    }

    public ModernButton(String text, Variant variant, String iconName, int iconSize) {
        super(text);
        this.variant = variant;
        this.iconName = iconName;
        this.iconSize = iconSize;

        setFont(Theme.FONT_BUTTON);
        setFocusPainted(false);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setOpaque(false);
        setRolloverEnabled(true);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setBorder(BorderFactory.createEmptyBorder(0, 18, 0, 18));
        applyForeground();
    }

    // ------------------------------------------------------------------
    // Factory helpers
    // ------------------------------------------------------------------

    public static ModernButton primary(String text, String iconName) {
        return new ModernButton(text, Variant.PRIMARY, iconName);
    }

    public static ModernButton secondary(String text, String iconName) {
        return new ModernButton(text, Variant.SECONDARY, iconName);
    }

    public static ModernButton danger(String text, String iconName) {
        return new ModernButton(text, Variant.DANGER, iconName);
    }

    /** A square, borderless icon button used in card headers and toolbars. */
    public static ModernButton icon(String iconName, String tooltip) {
        ModernButton button = new ModernButton("", Variant.GHOST, iconName, 17);
        button.setToolTipText(tooltip);
        button.setBorder(BorderFactory.createEmptyBorder(0, 9, 0, 9));
        button.setPreferredSize(new Dimension(34, 30));
        return button;
    }

    /** A text link styled as a button, used for secondary actions. */
    public static ModernButton link(String text, String iconName) {
        return new ModernButton(text, Variant.GHOST, iconName);
    }

    // ------------------------------------------------------------------
    // Colours
    // ------------------------------------------------------------------

    /** Keeps the glyph in step with the label when the button is disabled. */
    private void applyForeground() {
        Color foreground = foregroundFor();
        setForeground(foreground);
        if (iconName != null) {
            setIcon(IconFactory.of(iconName, iconSize, foreground));
        }
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        setCursor(Cursor.getPredefinedCursor(
                enabled ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
        applyForeground();
        repaint();
    }

    private Color foregroundFor() {
        if (!isEnabled()) {
            return Theme.c().textMuted;
        }
        switch (variant) {
            case PRIMARY:
            case DANGER:
            case SUCCESS:
                return Theme.c().textOnPrimary;
            case SECONDARY:
                return Theme.c().textPrimary;
            case GHOST:
            default:
                return Theme.c().textSecondary;
        }
    }

    private Color backgroundFor() {
        if (!isEnabled()) {
            return Theme.c().surfaceAlt;
        }
        boolean pressed = getModel().isPressed();
        boolean hover = getModel().isRollover();

        switch (variant) {
            case PRIMARY:
                return pressed ? Colors.darken(Theme.c().primary, 0.18)
                        : hover ? Theme.c().primaryHover : Theme.c().primary;
            case DANGER:
                return pressed ? Colors.darken(Theme.c().danger, 0.18)
                        : hover ? Colors.darken(Theme.c().danger, 0.08) : Theme.c().danger;
            case SUCCESS:
                return pressed ? Colors.darken(Theme.c().success, 0.18)
                        : hover ? Colors.darken(Theme.c().success, 0.08) : Theme.c().success;
            case SECONDARY:
                return hover ? Theme.c().surfaceAlt : Theme.c().surface;
            case GHOST:
            default:
                return hover ? Theme.c().surfaceAlt : null;
        }
    }

    private Color borderFor() {
        if (!isEnabled() || variant == Variant.SECONDARY) {
            return Theme.c().border;
        }
        return null;
    }

    // ------------------------------------------------------------------
    // Painting
    // ------------------------------------------------------------------

    @Override
    public Dimension getPreferredSize() {
        Dimension size = super.getPreferredSize();
        return new Dimension(size.width, Math.max(Theme.CONTROL_HEIGHT, size.height));
    }

    @Override
    public Dimension getMinimumSize() {
        return getPreferredSize();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int width = getWidth();
        int height = getHeight();
        int radius = Theme.RADIUS_CONTROL;

        Color background = backgroundFor();
        if (background != null && width > 1 && height > 1) {
            g2.setColor(background);
            g2.fillRoundRect(0, 0, width - 1, height - 1, radius, radius);
        }

        Color border = borderFor();
        if (border != null && width > 1 && height > 1) {
            g2.setColor(border);
            g2.drawRoundRect(0, 0, width - 2, height - 2, radius, radius);
        }

        g2.dispose();

        // Paints the icon and the label on top of the shape drawn above.
        super.paintComponent(g);
    }
}
