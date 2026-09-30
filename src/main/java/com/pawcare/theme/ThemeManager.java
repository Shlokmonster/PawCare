package com.pawcare.theme;

import com.pawcare.util.AppSettings;

import javax.swing.UIManager;
import java.util.ArrayList;
import java.util.List;

/**
 * Owns the active {@link Theme.Mode}, persists it and notifies interested parties.
 *
 * <p>Switching a Swing application's theme at runtime is awkward because components
 * capture their colours when they are constructed. Rather than writing a full theming
 * engine, the window registers a listener here and simply rebuilds its screens – the
 * data lives in the service layer, so rebuilding is cheap and always correct.</p>
 */
public class ThemeManager {

    public enum Mode {
        LIGHT("Light"),
        DARK("Dark");

        private final String label;

        Mode(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }

        public static Mode fromLabel(String label) {
            return DARK.label.equalsIgnoreCase(label) ? DARK : LIGHT;
        }
    }

    private final AppSettings settings;
    private final List<Runnable> listeners = new ArrayList<>();
    private Mode mode;

    public ThemeManager(AppSettings settings) {
        this.settings = settings;
        this.mode = Mode.fromLabel(settings.getTheme());
        apply();
    }

    public Mode getMode() {
        return mode;
    }

    public boolean isDark() {
        return mode == Mode.DARK;
    }

    /** Changes the theme, persists the choice and rebuilds every registered screen. */
    public void setMode(Mode newMode) {
        if (newMode == null || newMode == mode) {
            return;
        }
        this.mode = newMode;
        settings.setTheme(newMode.name().toLowerCase());
        settings.save();
        apply();
        notifyListeners();
    }

    public void toggle() {
        setMode(mode == Mode.DARK ? Mode.LIGHT : Mode.DARK);
    }

    /** Registers a callback invoked after every theme change. */
    public void addListener(Runnable listener) {
        listeners.add(listener);
    }

    private void notifyListeners() {
        for (Runnable listener : new ArrayList<>(listeners)) {
            listener.run();
        }
    }

    /** Installs the palette for the current mode and refreshes Swing's global defaults. */
    private void apply() {
        Theme.use(mode == Mode.DARK ? Palette.dark() : Palette.light());
        applyUiDefaults();
    }

    /**
     * Pushes the palette into Swing's global UI defaults.
     *
     * <p>The application runs on the cross-platform (Metal) look and feel precisely so
     * these keys can be overridden reliably; the result is applied to components created
     * after this call, which is why the window rebuilds on a theme change.</p>
     */
    private void applyUiDefaults() {
        Palette p = Theme.c();

        UIManager.put("Panel.background", p.background);
        UIManager.put("Panel.foreground", p.textPrimary);
        UIManager.put("OptionPane.background", p.surface);
        UIManager.put("OptionPane.messageForeground", p.textPrimary);
        UIManager.put("Label.foreground", p.textPrimary);
        UIManager.put("Label.background", p.background);

        UIManager.put("Table.background", p.surface);
        UIManager.put("Table.foreground", p.textPrimary);
        UIManager.put("Table.gridColor", p.divider);
        UIManager.put("Table.selectionBackground", p.selection);
        UIManager.put("Table.selectionForeground", p.selectionText);
        UIManager.put("TableHeader.background", p.surfaceAlt);
        UIManager.put("TableHeader.foreground", p.textSecondary);

        UIManager.put("TextField.background", p.field);
        UIManager.put("TextField.foreground", p.textPrimary);
        UIManager.put("TextField.caretForeground", p.textPrimary);
        UIManager.put("TextArea.background", p.field);
        UIManager.put("TextArea.foreground", p.textPrimary);
        UIManager.put("FormattedTextField.background", p.field);
        UIManager.put("FormattedTextField.foreground", p.textPrimary);

        UIManager.put("ComboBox.background", p.field);
        UIManager.put("ComboBox.foreground", p.textPrimary);
        UIManager.put("ComboBox.selectionBackground", p.selection);
        UIManager.put("ComboBox.selectionForeground", p.selectionText);

        UIManager.put("List.background", p.field);
        UIManager.put("List.foreground", p.textPrimary);
        UIManager.put("List.selectionBackground", p.selection);
        UIManager.put("List.selectionForeground", p.selectionText);

        UIManager.put("ScrollPane.background", p.surface);
        UIManager.put("ScrollBar.background", p.surface);
        UIManager.put("ScrollBar.thumb", p.scrollThumb);
        UIManager.put("ScrollBar.track", p.surfaceAlt);

        UIManager.put("CheckBox.background", p.surface);
        UIManager.put("CheckBox.foreground", p.textPrimary);
        UIManager.put("RadioButton.background", p.surface);
        UIManager.put("RadioButton.foreground", p.textPrimary);

        UIManager.put("Spinner.background", p.field);
        UIManager.put("Spinner.foreground", p.textPrimary);
        UIManager.put("ToolTip.background", p.surface);
        UIManager.put("ToolTip.foreground", p.textPrimary);
        UIManager.put("Separator.foreground", p.divider);
        UIManager.put("Viewport.background", p.surface);
    }
}
