package com.pawcare.gui.components;

import com.pawcare.model.Pet;
import com.pawcare.theme.Theme;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JLabel;
import javax.swing.JList;
import java.awt.Component;
import java.util.function.Function;

/**
 * List cell renderers that give combo box items a short, readable label.
 *
 * <p>A {@link Pet} prints its full one-line summary through {@code toString()}, which is
 * far too long for a drop-down. These renderers format the same objects for the width of
 * a form field – "Bruno (P001)" rather than
 * "Bruno • Dog • 4 yr • 28.5 kg • Labrador Retriever • Owner O001".</p>
 */
public final class ComboRenderers {

    private ComboRenderers() {
    }

    /** Renders any type using a supplied label function. */
    public static <T> DefaultListCellRenderer of(Function<T, String> labeller) {
        return new DefaultListCellRenderer() {
            @Override
            @SuppressWarnings("unchecked")
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(
                        list, value, index, isSelected, cellHasFocus);

                label.setText(value == null ? "—" : labeller.apply((T) value));
                label.setFont(Theme.FONT_BODY);
                label.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));

                if (isSelected) {
                    label.setBackground(Theme.c().selection);
                    label.setForeground(Theme.c().selectionText);
                } else {
                    label.setBackground(Theme.c().field);
                    label.setForeground(Theme.c().textPrimary);
                }
                // index -1 is the closed combo box; leaving it transparent keeps the
                // rounded frame painted by the combo box itself visible.
                label.setOpaque(index >= 0);
                return label;
            }
        };
    }

    /** "Bruno (P001)" – used wherever a pet has to be picked. */
    public static DefaultListCellRenderer pets() {
        return of((Pet pet) -> pet == null ? "—"
                : pet.getName() + " (" + pet.getAnimalId() + ")");
    }

    /** A renderer for the model objects that print themselves acceptably already. */
    public static DefaultListCellRenderer model() {
        return of(Object::toString);
    }
}
