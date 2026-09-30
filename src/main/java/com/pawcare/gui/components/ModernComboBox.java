package com.pawcare.gui.components;

import com.pawcare.theme.Theme;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.plaf.basic.BasicComboBoxUI;
import java.awt.BasicStroke;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.util.List;

/**
 * A combo box with a rounded outline and a hand-drawn chevron.
 *
 * <p>The default look and feel draws a bevelled, gradient button that would not match the
 * rest of the interface, so the arrow button and the background are replaced while the
 * stock popup behaviour is kept.</p>
 */
public class ModernComboBox<E> extends JComboBox<E> {

    public ModernComboBox() {
        super();
        init();
    }

    public ModernComboBox(E[] items) {
        super(items);
        init();
    }

    public ModernComboBox(List<E> items) {
        super();
        javax.swing.DefaultComboBoxModel<E> model = new javax.swing.DefaultComboBoxModel<>();
        for (E item : items) {
            model.addElement(item);
        }
        setModel(model);
        init();
    }

    private void init() {
        setUI(new ModernComboBoxUI());
        setOpaque(false);
        setFont(Theme.FONT_BODY);
        setForeground(Theme.c().textPrimary);
        setBackground(Theme.c().field);
        setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 2));
        // The renderer is installed after setUI, otherwise the look and feel would
        // replace it during installation.
        setRenderer(new ItemRenderer());
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension size = super.getPreferredSize();
        return new Dimension(Math.max(size.width, 120), Math.max(Theme.CONTROL_HEIGHT, size.height));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int width = getWidth();
        int height = getHeight();
        int radius = Theme.RADIUS_CONTROL;

        g2.setColor(isEnabled() ? Theme.c().field : Theme.c().surfaceAlt);
        g2.fillRoundRect(0, 0, width - 1, height - 1, radius, radius);

        g2.setColor(isFocusOwner() ? Theme.c().primary : Theme.c().border);
        g2.drawRoundRect(0, 0, width - 2, height - 2, radius, radius);

        g2.dispose();
        super.paintComponent(g);
    }

    /** Replaces the bevelled arrow button and suppresses the default background fill. */
    private static final class ModernComboBoxUI extends BasicComboBoxUI {

        @Override
        protected JButton createArrowButton() {
            JButton button = new JButton() {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                            RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(Theme.c().textSecondary);
                    g2.setStroke(new BasicStroke(1.7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    int cx = getWidth() / 2;
                    int cy = getHeight() / 2;
                    g2.drawLine(cx - 4, cy - 2, cx, cy + 2);
                    g2.drawLine(cx, cy + 2, cx + 4, cy - 2);
                    g2.dispose();
                }
            };
            button.setBorder(BorderFactory.createEmptyBorder());
            button.setContentAreaFilled(false);
            button.setFocusable(false);
            button.setOpaque(false);
            button.setPreferredSize(new Dimension(26, 26));
            return button;
        }

        @Override
        public void paintCurrentValueBackground(Graphics g, Rectangle bounds, boolean hasFocus) {
            // Intentionally empty: the combo box paints its own rounded background.
        }
    }

    /** Pads the items and applies the palette instead of the look and feel colours. */
    private static final class ItemRenderer extends DefaultListCellRenderer {

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                      boolean isSelected, boolean cellHasFocus) {
            JLabel label = (JLabel) super.getListCellRendererComponent(
                    list, value, index, isSelected, cellHasFocus);

            label.setFont(Theme.FONT_BODY);
            label.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));

            if (isSelected) {
                label.setBackground(Theme.c().selection);
                label.setForeground(Theme.c().selectionText);
            } else {
                label.setBackground(Theme.c().field);
                label.setForeground(Theme.c().textPrimary);
            }

            // index -1 is the closed combo box: leave it transparent so the rounded
            // frame painted by the combo box itself stays visible.
            label.setOpaque(index >= 0);
            return label;
        }
    }

    /** Utility used by the pages to build a combo box from an enum's values. */
    public static <T extends Enum<T>> ModernComboBox<T> of(Class<T> enumType) {
        return new ModernComboBox<>(enumType.getEnumConstants());
    }
}
