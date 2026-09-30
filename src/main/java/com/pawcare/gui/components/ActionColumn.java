package com.pawcare.gui.components;

import com.pawcare.theme.Theme;

import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.Arrays;
import java.util.List;

/**
 * Turns a table column into a row of clickable text actions, such as
 * {@code View  Edit  Delete}.
 *
 * <p>Swing tables have no concept of a button inside a cell, so the actions are painted by
 * a custom renderer and a mouse listener works out which word was clicked by measuring the
 * text with the same font metrics the renderer used. Both the renderer and the hit test
 * share {@link #PADDING} and {@link #GAP}, which is what keeps the two in agreement.</p>
 */
public final class ActionColumn {

    /** Left inset of the first action inside the cell. */
    private static final int PADDING = 14;
    /** Horizontal gap between two actions. */
    private static final int GAP = 18;
    /** Both the renderer and the hit test measure with this font. */
    private static final java.awt.Font ACTION_FONT = Theme.FONT_SMALL_MEDIUM;

    /** Receives the action name and the model row it applies to. */
    @FunctionalInterface
    public interface Handler {
        void onAction(String action, int modelRow);
    }

    private ActionColumn() {
    }

    /**
     * Installs the action column.
     *
     * @param table       the table to decorate
     * @param columnIndex the view index of the column that should hold the actions
     * @param handler     invoked with the clicked action and its model row
     * @param actions     the action labels, in display order
     */
    public static void install(JTable table, int columnIndex, Handler handler, String... actions) {
        List<String> labels = Arrays.asList(actions);

        table.getColumnModel().getColumn(columnIndex).setCellRenderer(new ActionRenderer(labels));
        table.getColumnModel().getColumn(columnIndex).setPreferredWidth(totalWidth(table, labels));

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int viewColumn = table.columnAtPoint(e.getPoint());
                if (viewColumn != columnIndex) {
                    return;
                }
                int viewRow = table.rowAtPoint(e.getPoint());
                if (viewRow < 0) {
                    return;
                }
                String action = hitTest(table, labels, e.getX());
                if (action != null) {
                    handler.onAction(action, table.convertRowIndexToModel(viewRow));
                }
            }
        });

        // Show a hand cursor while the pointer is over an action, so the row reads as
        // interactive rather than as plain text.
        table.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                boolean overAction = table.columnAtPoint(e.getPoint()) == columnIndex
                        && table.rowAtPoint(e.getPoint()) >= 0
                        && hitTest(table, labels, e.getX()) != null;
                table.setCursor(Cursor.getPredefinedCursor(
                        overAction ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
            }
        });
    }

    private static int totalWidth(JTable table, List<String> labels) {
        FontMetrics metrics = table.getFontMetrics(ACTION_FONT);
        int width = PADDING * 2;
        for (String label : labels) {
            width += metrics.stringWidth(label) + GAP;
        }
        return width;
    }

    /** Returns the action whose label contains the given x offset, or null. */
    private static String hitTest(JTable table, List<String> labels, int x) {
        FontMetrics metrics = table.getFontMetrics(ACTION_FONT);
        int cursor = PADDING;
        for (String label : labels) {
            int width = metrics.stringWidth(label);
            if (x >= cursor - GAP / 2 && x <= cursor + width + GAP / 2) {
                return label;
            }
            cursor += width + GAP;
        }
        return null;
    }

    /** Colours the action by intent: destructive actions read red, positive ones green. */
    private static Color colourFor(String action) {
        String normalised = action.toLowerCase();
        if (normalised.contains("delete") || normalised.contains("cancel") || normalised.contains("remove")) {
            return Theme.c().danger;
        }
        if (normalised.contains("complete") || normalised.contains("review")) {
            return Theme.c().success;
        }
        return Theme.c().primary;
    }

    /** Paints the action labels, and the row stripe behind them. */
    private static final class ActionRenderer extends JPanel implements TableCellRenderer {

        private final List<String> labels;

        ActionRenderer(List<String> labels) {
            this.labels = labels;
            setOpaque(true);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            setBackground(isSelected
                    ? table.getSelectionBackground()
                    : (row % 2 == 0 ? Theme.c().surface : Theme.c().surfaceAlt));
            return this;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setFont(ACTION_FONT);

            FontMetrics metrics = g2.getFontMetrics();
            int y = (getHeight() - metrics.getHeight()) / 2 + metrics.getAscent();

            int x = PADDING;
            for (String label : labels) {
                g2.setColor(colourFor(label));
                g2.drawString(label, x, y);
                x += metrics.stringWidth(label) + GAP;
            }
            g2.dispose();
        }
    }
}
