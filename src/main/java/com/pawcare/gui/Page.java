package com.pawcare.gui;

import com.pawcare.gui.components.PageHeader;
import com.pawcare.gui.components.ScrollPanes;
import com.pawcare.gui.components.Toast;
import com.pawcare.service.ClinicService;
import com.pawcare.theme.Theme;
import com.pawcare.util.DateUtil;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.Window;
import java.time.LocalDate;

/**
 * Base class of every screen in the application.
 *
 * <p>It supplies the two things every page shares: the heading strip along the top, and a
 * vertically stacked, scrollable body. A subclass only has to build its cards and hand
 * them to {@link #stack(JComponent)}.</p>
 *
 * <p>{@link #stack} pins each block to its preferred height. Without that, the vertical
 * box layout would stretch the last card to the bottom of the window, which looks wrong
 * on a form and disastrous on a chart. {@link #stackGrowing} is the deliberate exception:
 * the one card per page that holds a long table is allowed to take the leftover space.</p>
 */
public abstract class Page extends JPanel {

    protected final ClinicService clinic;

    private final PageHeader header;
    private final JPanel body = new ScrollableBody();
    private final JScrollPane scroll;

    protected Page(ClinicService clinic, String title, String description) {
        this.clinic = clinic;

        setOpaque(false);
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(
                Theme.SPACE_XL, Theme.SPACE_XL, 0, Theme.SPACE_XL));

        header = new PageHeader(title, description);
        add(header, BorderLayout.NORTH);

        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBorder(BorderFactory.createEmptyBorder(0, 0, Theme.SPACE_XL, 0));

        scroll = ScrollPanes.transparent(body);
        add(scroll, BorderLayout.CENTER);

        refreshHeader();
    }

    // ------------------------------------------------------------------
    // API for subclasses
    // ------------------------------------------------------------------

    /** The page heading, for buttons that live in the header. */
    protected PageHeader header() {
        return header;
    }

    /** The scrollable body. Prefer {@link #stack(JComponent)}. */
    protected JPanel body() {
        return body;
    }

    /**
     * Rewrites the clinic name and date line in the header.
     *
     * <p>Called when the clinic is renamed in Settings, so the change is reflected on
     * every screen without restarting the application.</p>
     */
    public void refreshHeader() {
        header.setMeta(clinic.settings().getClinicName()
                + "  ·  " + DateUtil.formatLong(LocalDate.now()));
    }

    /**
     * Adds a block to the page at its preferred height, with a gap underneath.
     *
     * @return the component, so a caller can keep a reference in one expression
     */
    protected <T extends JComponent> T stack(T component) {
        return stack(component, Theme.SPACE_MD);
    }

    /** Adds a block with an explicit gap underneath. */
    protected <T extends JComponent> T stack(T component, int gap) {
        component.setAlignmentX(Component.LEFT_ALIGNMENT);
        Dimension preferred = component.getPreferredSize();
        component.setMaximumSize(new Dimension(Integer.MAX_VALUE, preferred.height));
        body.add(component);
        if (gap > 0) {
            body.add(Box.createVerticalStrut(gap));
        }
        return component;
    }

    /**
     * Adds a block that is allowed to grow, used for the one card per page that holds a
     * long table or list. Everything else stays at its natural height.
     */
    protected <T extends JComponent> T stackGrowing(T component) {
        component.setAlignmentX(Component.LEFT_ALIGNMENT);
        component.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        body.add(component);
        body.add(Box.createVerticalStrut(Theme.SPACE_MD));
        return component;
    }

    /** Removes every block, used when a page is rebuilt or reset. */
    protected void clearBody() {
        body.removeAll();
    }

    /** Finishes a rebuild: re-lays out the body and returns the view to the top. */
    protected void rebuildFinished() {
        body.revalidate();
        body.repaint();
        scrollToTop();
    }

    // ------------------------------------------------------------------
    // Shared layout helpers
    // ------------------------------------------------------------------

    /**
     * Lays components out in equal-width columns.
     *
     * <p>The row is the standard way to put stat cards, charts or lists side by side.
     * Because the page body tracks the viewport width, the cells simply divide whatever
     * width the window has.</p>
     */
    protected static JPanel row(Component... cells) {
        JPanel panel = new JPanel(new GridLayout(1, Math.max(1, cells.length), Theme.SPACE_MD, 0));
        panel.setOpaque(false);
        for (Component cell : cells) {
            panel.add(cell);
        }
        return panel;
    }

    /**
     * Pins a component's height.
     *
     * <p>The width is left at zero on purpose: inside {@link #row} the cells are sized by
     * the grid, and in the page body the width comes from the viewport.</p>
     */
    protected static <T extends JComponent> T fixed(T component, int height) {
        component.setPreferredSize(new Dimension(0, height));
        return component;
    }

    /** Wraps a component – usually a table – in a scroll pane of a sensible height. */
    protected static JScrollPane scroll(JComponent view, int preferredHeight) {
        JScrollPane pane = ScrollPanes.create(view);
        pane.setPreferredSize(new Dimension(0, preferredHeight));
        return pane;
    }

    /** The window this page is displayed in, or null before it has been shown. */
    protected Window window() {
        return SwingUtilities.getWindowAncestor(this);
    }

    /** The frame this page is displayed in, used as the host for toasts. */
    protected JFrame frame() {
        Window ancestor = window();
        return ancestor instanceof JFrame ? (JFrame) ancestor : null;
    }

    /** A short confirmation message in the corner of the window. */
    protected void toast(String message) {
        JFrame host = frame();
        if (host != null) {
            Toast.success(host, message);
        }
    }

    /** A short failure message in the corner of the window. */
    protected void toastError(String message) {
        JFrame host = frame();
        if (host != null) {
            Toast.error(host, message);
        }
    }

    // ------------------------------------------------------------------
    // Lifecycle
    // ------------------------------------------------------------------

    /**
     * Called by the main window every time the page is shown.
     *
     * <p>Pages read the services here rather than caching values, so returning to a screen
     * always shows the current data. The default implementation simply rebuilds nothing.</p>
     */
    public void refresh() {
        // Pages with nothing to refresh simply inherit this no-op.
    }

    /** Scrolls the body back to the top – used after switching pages. */
    public void scrollToTop() {
        scroll.getVerticalScrollBar().setValue(0);
        scroll.getHorizontalScrollBar().setValue(0);
    }

    /**
     * The scrollable body.
     *
     * <p>It reports that it tracks the viewport's width, so the page is never wider than
     * the window and no horizontal scrollbar ever appears. Vertically it behaves normally
     * and scrolls when the content is taller than the window.</p>
     */
    private static final class ScrollableBody extends JPanel implements Scrollable {

        ScrollableBody() {
            super();
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 24;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
            int size = orientation == SwingConstants.VERTICAL ? visibleRect.height : visibleRect.width;
            return Math.max(40, size - 40);
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }
}
