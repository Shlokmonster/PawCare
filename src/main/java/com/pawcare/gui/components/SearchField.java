package com.pawcare.gui.components;

import com.pawcare.theme.IconFactory;
import com.pawcare.theme.Theme;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.List;

/**
 * A rounded search box: a magnifier glyph followed by a borderless text field.
 *
 * <p>Typing notifies the registered listeners immediately, which is what makes the
 * search and filter controls feel live.</p>
 */
public class SearchField extends RoundedPanel {

    private final ModernTextField field;
    private final List<Runnable> changeListeners = new ArrayList<>();

    public SearchField(String placeholder) {
        this(placeholder, 16);
    }

    public SearchField(String placeholder, int columns) {
        super(new BorderLayout(Theme.SPACE_SM, 0));
        setRadius(Theme.RADIUS_CONTROL);
        setCardBackground(Theme.c().field);
        setBorderColor(Theme.c().border);
        setBorder(BorderFactory.createEmptyBorder(0, Theme.SPACE_MD, 0, Theme.SPACE_MD));

        JLabel icon = new JLabel(IconFactory.of("search", 15, Theme.c().textMuted));
        add(icon, BorderLayout.WEST);

        field = new ModernTextField(placeholder, columns, false);
        add(field, BorderLayout.CENTER);

        field.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                fireChanged();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                fireChanged();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                fireChanged();
            }
        });
    }

    private void fireChanged() {
        for (Runnable listener : new ArrayList<>(changeListeners)) {
            listener.run();
        }
    }

    /** Registers a callback invoked on every keystroke. */
    public void onChange(Runnable listener) {
        changeListeners.add(listener);
    }

    public String getText() {
        return field.getText();
    }

    public void setText(String text) {
        field.setText(text);
    }

    public void clear() {
        field.setText("");
    }

    public void requestFieldFocus() {
        field.requestFocusInWindow();
    }
}
