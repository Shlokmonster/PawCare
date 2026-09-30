package com.pawcare.gui.components;

import com.pawcare.theme.Theme;
import com.pawcare.util.DateUtil;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.Component;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Draws date and time cells with the application's display formats.
 *
 * <p>A table column has to sort on the real value, not on the text the user sees. A date
 * column therefore hands the sorter a {@link LocalDate} – which is chronological – and
 * this renderer turns it into "30 Sep 2026" for display. Sorting by the printed string
 * would have ordered the months alphabetically.</p>
 *
 * <p>Row striping and the selection colours are reproduced here because installing a
 * renderer replaces the table's default one.</p>
 */
public class DateRenderer extends DefaultTableCellRenderer {

    public DateRenderer() {
        setOpaque(true);
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                   boolean hasFocus, int row, int column) {
        JLabel label = (JLabel) super.getTableCellRendererComponent(
                table, value, isSelected, hasFocus, row, column);

        label.setText(format(value));
        label.setFont(Theme.FONT_BODY);
        label.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));

        if (isSelected) {
            label.setBackground(table.getSelectionBackground());
            label.setForeground(table.getSelectionForeground());
        } else {
            label.setBackground(row % 2 == 0 ? Theme.c().surface : Theme.c().surfaceAlt);
            label.setForeground(Theme.c().textSecondary);
        }
        return label;
    }

    /** The display text for a temporal value; anything else is rendered as it is. */
    private static String format(Object value) {
        if (value instanceof LocalDate) {
            return DateUtil.format((LocalDate) value);
        }
        if (value instanceof LocalTime) {
            return DateUtil.format((LocalTime) value);
        }
        if (value instanceof LocalDateTime) {
            LocalDateTime moment = (LocalDateTime) value;
            return DateUtil.format(moment.toLocalDate(), moment.toLocalTime());
        }
        return value == null ? "—" : String.valueOf(value);
    }
}
