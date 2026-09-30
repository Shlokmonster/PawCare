package com.pawcare.gui.components;

import com.pawcare.theme.Theme;

import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

/**
 * Draws a {@link StatusBadge} inside a table cell.
 *
 * <p>The renderer also paints the row stripe and the selection colour, because the badge
 * itself is transparent and would otherwise leave a hole in the row background.</p>
 *
 * <p>One instance handles every status column: the value it is given is resolved through
 * {@link Badges}, so tables and dashboard lists always agree on what a status looks
 * like.</p>
 */
public class BadgeRenderer extends JPanel implements TableCellRenderer {

    private final StatusBadge badge = new StatusBadge();

    public BadgeRenderer() {
        super(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.anchor = GridBagConstraints.WEST;
        constraints.weightx = 1;
        constraints.fill = GridBagConstraints.NONE;
        add(badge, constraints);
        setOpaque(true);
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                   boolean hasFocus, int row, int column) {
        setBackground(isSelected
                ? table.getSelectionBackground()
                : (row % 2 == 0 ? Theme.c().surface : Theme.c().surfaceAlt));

        Badges.Spec spec = Badges.resolve(value);
        badge.setTextAndColors(spec.text, spec.foreground, spec.background);
        return this;
    }
}
