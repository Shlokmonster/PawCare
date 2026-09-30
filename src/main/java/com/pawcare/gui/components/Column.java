package com.pawcare.gui.components;

import javax.swing.table.TableCellRenderer;
import java.util.Comparator;
import java.util.function.Function;

/**
 * Declarative description of one table column.
 *
 * <p>A column knows its heading, how to pull its value out of a row object, how wide it
 * should be, how it should be drawn and how its values should be compared when the user
 * clicks the header.</p>
 *
 * <p>Describing columns as data means every table in the application is built the same
 * way, and adding a column is a single line rather than a new table model class.</p>
 *
 * @param <T> the type of row object the table displays
 */
public class Column<T> {

    private final String header;
    private final Function<T, Object> value;
    private final int width;
    private final TableCellRenderer renderer;
    private Comparator<Object> comparator;

    public Column(String header, Function<T, Object> value, int width) {
        this(header, value, width, null);
    }

    public Column(String header, Function<T, Object> value, int width, TableCellRenderer renderer) {
        this.header = header;
        this.value = value;
        this.width = width;
        this.renderer = renderer;
        this.comparator = naturalOrder();
    }

    /** Replaces the default comparison, e.g. to sort a status column by urgency. */
    public Column<T> withComparator(Comparator<Object> comparator) {
        this.comparator = comparator;
        return this;
    }

    public String getHeader() {
        return header;
    }

    public int getWidth() {
        return width;
    }

    public TableCellRenderer getRenderer() {
        return renderer;
    }

    /** Extracts this column's value from a row. */
    public Object valueOf(T row) {
        return value == null || row == null ? null : value.apply(row);
    }

    public Comparator<Object> comparator() {
        return comparator;
    }

    /**
     * Default comparison: natural order for comparable values, falling back to a
     * case-insensitive text comparison so mixed content still sorts predictably.
     * Nulls always sort last.
     */
    private static Comparator<Object> naturalOrder() {
        return (a, b) -> {
            if (a == null && b == null) {
                return 0;
            }
            if (a == null) {
                return 1;
            }
            if (b == null) {
                return -1;
            }
            if (a instanceof Comparable && a.getClass().equals(b.getClass())) {
                @SuppressWarnings({"unchecked", "rawtypes"})
                int result = ((Comparable) a).compareTo(b);
                return result;
            }
            return String.valueOf(a).compareToIgnoreCase(String.valueOf(b));
        };
    }
}
