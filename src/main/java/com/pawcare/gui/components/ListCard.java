package com.pawcare.gui.components;

import com.pawcare.theme.Colors;
import com.pawcare.theme.IconFactory;
import com.pawcare.theme.Theme;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Rectangle;

/**
 * A card that shows a short list of records – the dashboard's "Today's appointments",
 * "Recent treatments" and "Vaccination alerts" panels are all built from this.
 *
 * <p>Rows are composed as icon, title, description and an optional badge. When nothing is
 * added, the card falls back to an {@link EmptyStatePanel} so the panel never looks
 * broken.</p>
 */
public class ListCard extends Card {

    private static final String ROWS = "rows";
    private static final String EMPTY = "empty";

    private final JPanel rowsPanel = new ScrollableRows();
    private final JPanel body = new JPanel(new CardLayout());
    private final EmptyStatePanel emptyState;
    private int rowCount = 0;

    public ListCard(String title, String subtitle, String emptyTitle, String emptyMessage) {
        super(title, subtitle);

        rowsPanel.setOpaque(false);
        rowsPanel.setLayout(new BoxLayout(rowsPanel, BoxLayout.Y_AXIS));

        JScrollPane scroll = ScrollPanes.create(rowsPanel);
        scroll.getViewport().setBackground(Theme.c().surface);
        scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        emptyState = new EmptyStatePanel("check", emptyTitle, emptyMessage);

        body.setOpaque(false);
        body.add(scroll, ROWS);
        body.add(emptyState, EMPTY);

        content().add(body, BorderLayout.CENTER);
    }

    /** Removes every row and prepares the card for a fresh set. */
    public void clearRows() {
        rowsPanel.removeAll();
        rowCount = 0;
    }

    /**
     * Adds one row.
     *
     * @param iconName   glyph shown in the tinted tile
     * @param accent     colour of the tile
     * @param primary    main line, e.g. the pet's name
     * @param secondary  supporting line, e.g. the reason and time
     * @param trailing   right-aligned text, e.g. a date; may be null
     * @param badgeValue value rendered as a badge, or null for no badge
     */
    public void addRow(String iconName, java.awt.Color accent, String primary, String secondary,
                       String trailing, Object badgeValue) {
        if (rowCount > 0) {
            rowsPanel.add(divider());
        }
        rowsPanel.add(buildRow(iconName, accent, primary, secondary, trailing, badgeValue));
        rowCount++;
    }

    /** Shows the empty state instead of the rows when nothing was added. */
    public void refresh() {
        ((CardLayout) body.getLayout()).show(body, rowCount == 0 ? EMPTY : ROWS);
        rowsPanel.revalidate();
        rowsPanel.repaint();
        body.revalidate();
        body.repaint();
    }

    /** Allows the empty state text to be replaced before {@link #refresh()}. */
    public EmptyStatePanel emptyState() {
        return emptyState;
    }

    public int rowCount() {
        return rowCount;
    }

    private JPanel buildRow(String iconName, java.awt.Color accent, String primary, String secondary,
                            String trailing, Object badgeValue) {
        JPanel row = new JPanel(new BorderLayout(Theme.SPACE_MD, 0));
        row.setOpaque(false);
        row.setBorder(BorderFactory.createEmptyBorder(Theme.SPACE_SM, 0, Theme.SPACE_SM, 0));

        // Tinted icon tile.
        RoundedPanel tile = new RoundedPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        tile.setRadius(10);
        tile.setCardBackground(Colors.alpha(accent, 34));
        tile.setPreferredSize(new Dimension(36, 36));
        JLabel icon = new JLabel(IconFactory.of(iconName, 17, accent));
        icon.setBorder(BorderFactory.createEmptyBorder(9, 0, 0, 0));
        tile.add(icon);

        JPanel tileHolder = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        tileHolder.setOpaque(false);
        tileHolder.add(tile);
        row.add(tileHolder, BorderLayout.WEST);

        // Text.
        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));

        JLabel primaryLabel = new JLabel(primary);
        primaryLabel.setFont(Theme.FONT_BODY_MEDIUM);
        primaryLabel.setForeground(Theme.c().textPrimary);
        primaryLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        text.add(primaryLabel);

        if (secondary != null && !secondary.isBlank()) {
            text.add(Box.createVerticalStrut(2));
            JLabel secondaryLabel = new JLabel(secondary);
            secondaryLabel.setFont(Theme.FONT_TINY);
            secondaryLabel.setForeground(Theme.c().textSecondary);
            secondaryLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            text.add(secondaryLabel);
        }
        row.add(text, BorderLayout.CENTER);

        // Trailing date and badge.
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, Theme.SPACE_SM, 0));
        right.setOpaque(false);

        if (trailing != null && !trailing.isBlank()) {
            JLabel trailingLabel = new JLabel(trailing, SwingConstants.RIGHT);
            trailingLabel.setFont(Theme.FONT_TINY);
            trailingLabel.setForeground(Theme.c().textMuted);
            right.add(trailingLabel);
        }
        if (badgeValue != null) {
            right.add(Badges.badge(badgeValue));
        }
        row.add(right, BorderLayout.EAST);

        return row;
    }

    private JPanel divider() {
        JPanel line = new JPanel();
        line.setBackground(Theme.c().divider);
        line.setPreferredSize(new Dimension(0, 1));
        line.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        line.setOpaque(true);
        return line;
    }

    /**
     * The column the rows are stacked in.
     *
     * <p>It tracks the width of the scroll pane's viewport, so a row is never wider than
     * the card and no horizontal scrollbar appears beside the vertical one.</p>
     */
    private static final class ScrollableRows extends JPanel implements Scrollable {

        ScrollableRows() {
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
