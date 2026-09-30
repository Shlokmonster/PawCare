package com.pawcare;

import com.pawcare.exception.DataAccessException;
import com.pawcare.gui.MainFrame;
import com.pawcare.repository.DataStore;
import com.pawcare.service.ClinicService;
import com.pawcare.theme.ThemeManager;
import com.pawcare.util.AppSettings;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import java.nio.file.Path;

/**
 * Entry point of PAWCARE.
 *
 * <p>Start-up order matters and is worth knowing for the viva:</p>
 * <ol>
 *   <li>text rendering hints are set, because they can only be changed before the toolkit
 *       starts;</li>
 *   <li>Swing is switched to the cross-platform (Metal) look and feel, which is the only one
 *       whose colours can be overridden reliably from {@link javax.swing.UIManager};</li>
 *   <li>the saved preferences are read, which decides the theme;</li>
 *   <li>the data files are loaded – and seeded on a first run;</li>
 *   <li>and finally the window is built, on the event dispatch thread.</li>
 * </ol>
 *
 * <p>All user interface work happens inside {@code invokeLater}, so every component is
 * created on the event dispatch thread. That is the single most important rule of Swing
 * programming.</p>
 */
public final class Main {

    /** Where the data files and the preferences live, relative to the working directory. */
    private static final Path DATA_DIRECTORY = Path.of("data");
    private static final Path SETTINGS_FILE = DATA_DIRECTORY.resolve("settings.properties");

    private Main() {
    }

    public static void main(String[] args) {
        // Anti-aliased text and a non-bold Metal font: both must be set before the
        // toolkit initialises, which is why they are the very first statements.
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");
        System.setProperty("swing.boldMetal", "false");

        SwingUtilities.invokeLater(Main::launch);
    }

    private static void launch() {
        installLookAndFeel();

        AppSettings settings = new AppSettings(SETTINGS_FILE);
        settings.load();

        // Applying the palette has to happen before any component is created, because a
        // Swing component captures its colours when it is constructed.
        ThemeManager themes = new ThemeManager(settings);

        DataStore store = new DataStore(DATA_DIRECTORY);
        try {
            store.ensureDirectory();
        } catch (DataAccessException e) {
            // Not fatal: the window still opens and every failed save is reported to the
            // user, so they can see what went wrong instead of the application vanishing.
            System.err.println("[PawCare] " + e.getMessage());
        }

        ClinicService clinic = new ClinicService(store, settings);
        clinic.loadAll();

        MainFrame frame = new MainFrame(clinic, themes);
        frame.setVisible(true);
        frame.reportStartupProblems();

        System.out.println("[PawCare] Ready. " + clinic.pets().count() + " patients, "
                + clinic.appointments().count() + " appointments. Data folder: "
                + store.getDataDirectory().toAbsolutePath());
    }

    /**
     * Installs the cross-platform look and feel.
     *
     * <p>It is chosen deliberately: it is drawn entirely in Java, so it looks the same on
     * every operating system and its colours can be replaced by {@link ThemeManager}.</p>
     */
    private static void installLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (ClassNotFoundException | InstantiationException
                 | IllegalAccessException | UnsupportedLookAndFeelException e) {
            // The application is fully functional on the default look and feel; only the
            // colour scheme would differ, so this is a warning rather than a failure.
            System.err.println("[PawCare] Could not install the cross-platform look and feel: "
                    + e.getMessage());
        }
    }
}
