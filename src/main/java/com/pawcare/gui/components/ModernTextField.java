package com.pawcare.gui.components;

import com.pawcare.theme.Theme;

import javax.swing.BorderFactory;
import javax.swing.JTextField;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

/**
 * A text field with a rounded outline and a placeholder line.
 *
 * <p>{@code framed = false} produces a borderless field so it can sit inside a container
 * that paints the frame itself, which is how {@link SearchField} is composed.</p>
 */
public class ModernTextField extends JTextField {

    private final String placeholder;
    private final boolean framed;

    public ModernTextField(String placeholder) {
        this(placeholder, 0, true);
    }

    public ModernTextField(String placeholder, boolean framed) {
        this(placeholder, 0, framed);
    }

    public ModernTextField(String placeholder, int columns, boolean framed) {
        super(columns);
        this.placeholder = placeholder == null ? "" : placeholder;
        this.framed = framed;

        setOpaque(false);
        setFont(Theme.FONT_BODY);
        setForeground(Theme.c().textPrimary);
        setCaretColor(Theme.c().textPrimary);
        setSelectionColor(Theme.c().selection);
        setSelectedTextColor(Theme.c().selectionText);
        setBorder(framed
                ? BorderFactory.createEmptyBorder(7, 12, 7, 12)
                : BorderFactory.createEmptyBorder(7, 0, 7, 0));

        addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                repaint();
            }

            @Override
            public void focusLost(FocusEvent e) {
                repaint();
            }
        });
    }

    public String getPlaceholder() {
        return placeholder;
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension size = super.getPreferredSize();
        return new Dimension(size.width, Math.max(Theme.CONTROL_HEIGHT, size.height));
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (framed) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int width = getWidth();
            int height = getHeight();
            int radius = Theme.RADIUS_CONTROL;

            g2.setColor(Theme.c().field);
            g2.fillRoundRect(0, 0, width - 1, height - 1, radius, radius);

            g2.setColor(isFocusOwner() ? Theme.c().primary : Theme.c().border);
            g2.drawRoundRect(0, 0, width - 2, height - 2, radius, radius);

            g2.dispose();
        }

        super.paintComponent(g);

        // The placeholder is painted after the text so an empty field still shows a hint.
        if (getText().isEmpty() && !placeholder.isEmpty()) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setColor(Theme.c().textMuted);
            g2.setFont(getFont());
            FontMetrics metrics = g2.getFontMetrics();
            int x = getInsets().left;
            int y = (getHeight() - metrics.getHeight()) / 2 + metrics.getAscent();
            g2.drawString(placeholder, x, y);
            g2.dispose();
        }
    }
}
