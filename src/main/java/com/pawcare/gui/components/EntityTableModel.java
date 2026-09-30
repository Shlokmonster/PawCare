package com.pawcare.gui.components;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

/**
 * A single reusable {@link javax.swing.table.TableModel} driven by a list of
 * {@link Column} definitions.
 *
 * <p>Instead of writing one table model per entity, every screen supplies its columns and
 * its rows here. The model is read-only: records are changed through the services and the
 * table is rebuilt, which keeps the display and the stored data in step.</p>
 *
 * @param <T> the type of row object the table displays
 */
public class EntityTableModel<T> extends AbstractTableModel {

    private final List<Column<T>> columns;
    private List<T> rows = new ArrayList<>();

    public EntityTableModel(List<Column<T>> columns) {
        this.columns = new ArrayList<>(columns);
    }

    /** Replaces the contents, which also triggers a re-sort if a header sort is active. */
    public void setRows(List<T> newRows) {
        this.rows = newRows == null ? new ArrayList<>() : new ArrayList<>(newRows);
        fireTableDataChanged();
    }

    public List<T> getRows() {
        return new ArrayList<>(rows);
    }

    public T getRow(int modelRow) {
        if (modelRow < 0 || modelRow >= rows.size()) {
            return null;
        }
        return rows.get(modelRow);
    }

    public Column<T> getColumn(int index) {
        return columns.get(index);
    }

    public List<Column<T>> getColumns() {
        return new ArrayList<>(columns);
    }

    @Override
    public int getRowCount() {
        return rows.size();
    }

    @Override
    public int getColumnCount() {
        return columns.size();
    }

    @Override
    public String getColumnName(int column) {
        return columns.get(column).getHeader();
    }

    /**
     * Always {@code Object.class}: the sorters in this application are given explicit
     * comparators per column, so the class does not need to describe the value type.
     */
    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return Object.class;
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return false;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        return columns.get(columnIndex).valueOf(rows.get(rowIndex));
    }
}
