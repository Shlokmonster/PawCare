package com.pawcare.gui.components;

import com.pawcare.theme.Theme;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;

/**
 * Factory for scroll panes with a thin, flat scrollbar.
 *
 * <p>The default scrollbar is wide and chunky; this one is a slim rounded thumb, which
 * suits the flat card layout far better.</p>
 */
public final class ScrollPanes {

    private static final int THICKNESS = 10;

    private ScrollPanes() {
    }

    /** Wraps a component in a borderless scroll pane with slim scrollbars. */
    public static JScrollPane create(JComponent view) {
        JScrollPane pane = new JScrollPane(view);
        pane.setBorder(BorderFactory.createEmptyBorder());
        pane.setViewportBorder(null);
        pane.setOpaque(false);
        pane.getViewport().setOpaque(true);
        pane.getViewport().setBackground(Theme.c().surface);

        pane.getVerticalScrollBar().setUI(new SlimScrollBarUI());
        pane.getHorizontalScrollBar().setUI(new SlimScrollBarUI());
        pane.getVerticalScrollBar().setPreferredSize(new Dimension(THICKNESS, 0));
        pane.getHorizontalScrollBar().setPreferredSize(new Dimension(0, THICKNESS));
        pane.getVerticalScrollBar().setUnitIncrement(18);
        pane.getHorizontalScrollBar().setUnitIncrement(18);
        pane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        return pane;
    }

    /** A scroll pane whose viewport shows the window background, used for page bodies. */
    public static JScrollPane transparent(JComponent view) {
        JScrollPane pane = create(view);
        pane.getViewport().setBackground(Theme.c().background);
        return pane;
    }

    /** Scrollbar with no arrow buttons and a rounded thumb. */
    private static final class SlimScrollBarUI extends BasicScrollBarUI {

        @Override
        protected JButton createDecreaseButton(int orientation) {
            return zeroButton();
        }

        @Override
        protected JButton createIncreaseButton(int orientation) {
            return zeroButton();
        }

        private JButton zeroButton() {
            JButton button = new JButton();
            Dimension zero = new Dimension(0, 0);
            button.setPreferredSize(zero);
            button.setMinimumSize(zero);
            button.setMaximumSize(zero);
            button.setFocusable(false);
            return button;
        }

        @Override
        protected void paintTrack(Graphics g, JComponent c, Rectangle bounds) {
            g.setColor(Theme.c().surfaceAlt);
            g.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
        }

        @Override
        protected void paintThumb(Graphics g, JComponent c, Rectangle bounds) {
            if (bounds.isEmpty() || !scrollbar.isEnabled()) {
                return;
            }
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(Theme.c().scrollThumb);
            if (scrollbar.getOrientation() == JScrollBar.VERTICAL) {
                g2.fillRoundRect(bounds.x + 3, bounds.y + 2, bounds.width - 6,
                        Math.max(8, bounds.height - 4), 6, 6);
            } else {
                g2.fillRoundRect(bounds.x + 2, bounds.y + 3,
                        Math.max(8, bounds.width - 4), bounds.height - 6, 6, 6);
            }
            g2.dispose();
        }
    }
}
