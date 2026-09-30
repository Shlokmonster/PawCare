package com.pawcare.gui.components;

import com.pawcare.theme.Theme;

import javax.swing.BorderFactory;
import javax.swing.JTextArea;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/** A rounded, wrapping multi-line text box used for notes and clinical remarks. */
public class ModernTextArea extends JTextArea {

    private final String placeholder;

    public ModernTextArea(String placeholder, int rows) {
        super(rows, 20);
        this.placeholder = placeholder == null ? "" : placeholder;

        setLineWrap(true);
        setWrapStyleWord(true);
        setOpaque(false);
        setFont(Theme.FONT_BODY);
        setForeground(Theme.c().textPrimary);
        setCaretColor(Theme.c().textPrimary);
        setSelectionColor(Theme.c().selection);
        setSelectedTextColor(Theme.c().selectionText);
        setBorder(BorderFactory.createEmptyBorder(9, 12, 9, 12));
    }

    public String getPlaceholder() {
        return placeholder;
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension size = super.getPreferredSize();
        return new Dimension(size.width, Math.max(84, size.height));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int width = getWidth();
        int height = getHeight();
        int radius = Theme.RADIUS_CONTROL;

        g2.setColor(Theme.c().field);
        g2.fillRoundRect(0, 0, width - 1, height - 1, radius, radius);
        g2.setColor(Theme.c().border);
        g2.drawRoundRect(0, 0, width - 2, height - 2, radius, radius);
        g2.dispose();

        super.paintComponent(g);

        if (getText().isEmpty() && !placeholder.isEmpty()) {
            Graphics2D text = (Graphics2D) g.create();
            text.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            text.setColor(Theme.c().textMuted);
            text.setFont(getFont());
            FontMetrics metrics = text.getFontMetrics();
            text.drawString(placeholder, getInsets().left,
                    getInsets().top + metrics.getAscent());
            text.dispose();
        }
    }
}
