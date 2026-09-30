package com.pawcare.gui.components;

import com.pawcare.theme.IconFactory;
import com.pawcare.theme.Theme;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableRowSorter;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/**
 * The application's table: flat, striped, generously spaced and sorted by comparator.
 *
 * <p>Three details make it look deliberate rather than default:</p>
 * <ul>
 *   <li>the grid is switched off and replaced by alternating row colours;</li>
 *   <li>the header is painted by a custom renderer instead of the look and feel; and</li>
 *   <li>an empty table explains itself rather than showing a blank rectangle.</li>
 * </ul>
 *
 * <p>Sorting is wired up with an explicit {@link java.util.Comparator} for every column,
 * so clicking a heading always sorts on the value's real type – dates chronologically,
 * numbers numerically – rather than on the rendered text.</p>
 *
 * @param <T> the type of row object the table displays
 */
public class ModernTable<T> extends JTable {

    private final EntityTableModel<T> entityModel;
    private String emptyMessage = "No records to display";
    private String emptyIcon = "search";

    /**
     * Convenience constructor: wraps the column definitions in an
     * {@link EntityTableModel} so a page can simply write
     * {@code new ModernTable<>(buildColumns())}.
     */
    public ModernTable(java.util.List<Column<T>> columns) {
        this(new EntityTableModel<>(columns));
    }

    public ModernTable(EntityTableModel<T> model) {
        super(model);
        this.entityModel = model;

        setRowHeight(Theme.TABLE_ROW_HEIGHT);
        setShowGrid(false);
        setIntercellSpacing(new Dimension(0, 0));
        setFillsViewportHeight(true);
        setBackground(Theme.c().surface);
        setForeground(Theme.c().textPrimary);
        setSelectionBackground(Theme.c().selection);
        setSelectionForeground(Theme.c().selectionText);
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        setFont(Theme.FONT_BODY);
        setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
        setFocusable(true);

        styleHeader();
        setDefaultRenderer(Object.class, new StripedCellRenderer());
        installSorter();
        applyColumnWidths();
    }

    // ------------------------------------------------------------------
    // Setup
    // ------------------------------------------------------------------

    private void styleHeader() {
        getTableHeader().setReorderingAllowed(false);
        getTableHeader().setFont(Theme.FONT_SMALL_MEDIUM);
        getTableHeader().setBackground(Theme.c().surfaceAlt);
        getTableHeader().setForeground(Theme.c().textSecondary);
        getTableHeader().setPreferredSize(new Dimension(0, 38));
        getTableHeader().setDefaultRenderer(new HeaderRenderer());
    }

    private void installSorter() {
        TableRowSorter<EntityTableModel<T>> sorter = new TableRowSorter<>(entityModel);
        for (int i = 0; i < entityModel.getColumnCount(); i++) {
            sorter.setComparator(i, entityModel.getColumn(i).comparator());
            sorter.setSortable(i, true);
        }
        setRowSorter(sorter);
    }

    private void applyColumnWidths() {
        for (int i = 0; i < entityModel.getColumnCount() && i < getColumnModel().getColumnCount(); i++) {
            TableColumn column = getColumnModel().getColumn(i);
            Column<T> definition = entityModel.getColumn(i);
            column.setPreferredWidth(definition.getWidth());
            column.setMinWidth(Math.min(60, definition.getWidth()));
            if (definition.getRenderer() != null) {
                column.setCellRenderer(definition.getRenderer());
            }
        }
    }

    // ------------------------------------------------------------------
    // Behaviour
    // ------------------------------------------------------------------

    /** Swaps the rows and keeps any active header sort. */
    public void setRows(java.util.List<T> rows) {
        entityModel.setRows(rows);
        applyColumnWidths();
    }

    public EntityTableModel<T> getEntityModel() {
        return entityModel;
    }

    /** The row object behind the selected line, or null when nothing is selected. */
    public T getSelectedObject() {
        int viewRow = getSelectedRow();
        if (viewRow < 0) {
            return null;
        }
        return entityModel.getRow(convertRowIndexToModel(viewRow));
    }

    public T getObjectAt(int viewRow) {
        if (viewRow < 0 || viewRow >= getRowCount()) {
            return null;
        }
        return entityModel.getRow(convertRowIndexToModel(viewRow));
    }

    /** Message shown when the table has no rows. */
    public void setEmptyMessage(String message) {
        this.emptyMessage = message;
        repaint();
    }

    /** Icon shown above the empty message. */
    public void setEmptyIcon(String iconName) {
        this.emptyIcon = iconName;
        repaint();
    }

    @Override
    public boolean isCellEditable(int row, int column) {
        return false;
    }

    // ------------------------------------------------------------------
    // Painting
    // ------------------------------------------------------------------

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (getRowCount() > 0) {
            return;
        }

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int centreX = getWidth() / 2;
        int centreY = Math.max(90, getHeight() / 2);

        Icon icon = IconFactory.of(emptyIcon, 34, Theme.c().textMuted);
        icon.paintIcon(this, g2, centreX - icon.getIconWidth() / 2, centreY - 58);

        g2.setFont(Theme.FONT_BODY);
        g2.setColor(Theme.c().textMuted);
        FontMetrics metrics = g2.getFontMetrics();
        g2.drawString(emptyMessage, centreX - metrics.stringWidth(emptyMessage) / 2, centreY + 4);

        g2.dispose();
    }

    // ------------------------------------------------------------------
    // Renderers
    // ------------------------------------------------------------------

    /** Header cells: left aligned, padded and quiet. */
    private static final class HeaderRenderer extends DefaultTableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            JLabel label = (JLabel) super.getTableCellRendererComponent(
                    table, value, isSelected, hasFocus, row, column);
            label.setFont(Theme.FONT_SMALL_MEDIUM);
            label.setForeground(Theme.c().textSecondary);
            label.setBackground(Theme.c().surfaceAlt);
            label.setOpaque(true);
            label.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
            label.setHorizontalAlignment(LEFT);
            label.setIcon(null);
            return label;
        }
    }

    /** Body cells: alternate row colour, consistent padding, no grid lines. */
    private static final class StripedCellRenderer extends DefaultTableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            JLabel label = (JLabel) super.getTableCellRendererComponent(
                    table, value, isSelected, hasFocus, row, column);

            label.setFont(Theme.FONT_BODY);
            label.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
            label.setOpaque(true);

            if (isSelected) {
                label.setBackground(table.getSelectionBackground());
                label.setForeground(table.getSelectionForeground());
            } else {
                label.setBackground(row % 2 == 0 ? Theme.c().surface : Theme.c().surfaceAlt);
                label.setForeground(Theme.c().textPrimary);
            }
            return label;
        }
    }
}
