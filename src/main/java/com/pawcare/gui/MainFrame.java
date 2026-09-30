package com.pawcare.gui;

import com.pawcare.exception.DataAccessException;
import com.pawcare.gui.dialogs.AboutDialog;
import com.pawcare.gui.dialogs.Dialogs;
import com.pawcare.gui.pages.AppointmentsPage;
import com.pawcare.gui.pages.DashboardPage;
import com.pawcare.gui.pages.OwnersPage;
import com.pawcare.gui.pages.PetsPage;
import com.pawcare.gui.pages.ReportsPage;
import com.pawcare.gui.pages.SearchPage;
import com.pawcare.gui.pages.SettingsPage;
import com.pawcare.gui.pages.TreatmentsPage;
import com.pawcare.gui.pages.VaccinationsPage;
import com.pawcare.service.ClinicService;
import com.pawcare.theme.IconFactory;
import com.pawcare.theme.Theme;
import com.pawcare.theme.ThemeManager;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The application window: a navigation rail, the current screen, and the wiring between
 * them.
 *
 * <p>Every screen is created once and kept in a {@link CardLayout}; navigating simply asks
 * the card layout to show another one and tells that page to reload its data. Switching is
 * therefore instant and no record is copied into the user interface.</p>
 *
 * <p>When the theme changes the whole interface is rebuilt, because Swing components keep
 * the colours they were constructed with. Rebuilding is cheap – the data lives in the
 * service layer – and it is the only reliable way to repaint a Swing application.</p>
 *
 * <p>The rebuild replaces the <i>children</i> of one permanent content pane rather than
 * calling {@link #setContentPane} again: setting a content pane on a window that is already
 * on screen throws, because the old pane is removed from a layered pane that has since been
 * rearranged. The rebuild is also pushed to the end of the event queue, so the control that
 * asked for the change – the theme drop-down on the Settings page – has finished dispatching
 * before its own screen is torn down.</p>
 */
public class MainFrame extends JFrame {

    private final ClinicService clinic;
    private final ThemeManager themes;

    /** The pages, keyed by the sidebar's navigation keys, in display order. */
    private final Map<String, Page> pages = new LinkedHashMap<>();

    /** The one and only content pane; only its children are replaced on a rebuild. */
    private final JPanel shell = new JPanel(new BorderLayout());

    private Sidebar sidebar;
    private JPanel pageHost;
    private CardLayout pageLayout;
    private String currentKey = Sidebar.DASHBOARD;

    public MainFrame(ClinicService clinic, ThemeManager themes) {
        this.clinic = clinic;
        this.themes = themes;

        setTitle(AboutDialog.APP_NAME + " — " + AboutDialog.APP_SUBTITLE);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setMinimumSize(new Dimension(1040, 700));
        setSize(new Dimension(1320, 840));
        setLocationRelativeTo(null);

        // Rebuilding on a theme change is what makes the light/dark switch work at all.
        themes.addListener(() -> SwingUtilities.invokeLater(this::buildUi));

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                shutdown();
            }
        });

        setContentPane(shell);
        buildUi();
    }

    // ------------------------------------------------------------------
    // Construction
    // ------------------------------------------------------------------

    /** Builds (or rebuilds) the whole window for the palette that is active right now. */
    private void buildUi() {
        pages.clear();
        shell.removeAll();

        setIconImage(IconFactory.pawImage(64, Theme.c().primary, Theme.c().textOnPrimary));

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Theme.c().background);

        sidebar = new Sidebar(this::navigate);
        root.add(sidebar, BorderLayout.WEST);

        pageLayout = new CardLayout();
        pageHost = new JPanel(pageLayout);
        pageHost.setOpaque(false);

        pages.put(Sidebar.DASHBOARD, new DashboardPage(clinic));
        pages.put(Sidebar.PETS, new PetsPage(clinic));
        pages.put(Sidebar.OWNERS, new OwnersPage(clinic));
        pages.put(Sidebar.APPOINTMENTS, new AppointmentsPage(clinic));
        pages.put(Sidebar.TREATMENTS, new TreatmentsPage(clinic));
        pages.put(Sidebar.VACCINATIONS, new VaccinationsPage(clinic));
        pages.put(Sidebar.SEARCH, new SearchPage(clinic));
        pages.put(Sidebar.REPORTS, new ReportsPage(clinic));
        pages.put(Sidebar.SETTINGS, new SettingsPage(clinic, themes, this::refreshAll));

        for (Map.Entry<String, Page> entry : pages.entrySet()) {
            pageHost.add(entry.getValue(), entry.getKey());
        }

        root.add(pageHost, BorderLayout.CENTER);
        shell.add(root, BorderLayout.CENTER);

        sidebar.setActive(currentKey);
        pageLayout.show(pageHost, currentKey);
        pages.get(currentKey).refresh();

        shell.revalidate();
        shell.repaint();
    }

    // ------------------------------------------------------------------
    // Navigation
    // ------------------------------------------------------------------

    /** Handles a click in the navigation rail. */
    public void navigate(String key) {
        if (Sidebar.ABOUT.equals(key)) {
            // About is a dialog, not a page, so the previous entry stays highlighted.
            new AboutDialog(this, clinic).setVisible(true);
            sidebar.setActive(currentKey);
            return;
        }
        if (!pages.containsKey(key)) {
            return;
        }
        currentKey = key;
        sidebar.setActive(key);
        pageLayout.show(pageHost, key);

        Page page = pages.get(key);
        page.refresh();
        page.scrollToTop();
    }

    /**
     * Reloads every screen.
     *
     * <p>Called after a change that affects more than one page – renaming the clinic, or
     * widening the vaccination reminder window, which reclassifies records everywhere.</p>
     */
    public void refreshAll() {
        for (Page page : pages.values()) {
            page.refresh();
        }
    }

    // ------------------------------------------------------------------
    // Start-up and shut-down
    // ------------------------------------------------------------------

    /**
     * Reports anything that went wrong while the data files were read.
     *
     * <p>Called once the window is on screen, so the message is never hidden behind a frame
     * that is still being laid out.</p>
     */
    public void reportStartupProblems() {
        List<String> warnings = clinic.getLoadWarnings();
        if (!warnings.isEmpty()) {
            Dialogs.warning(this, "Some data could not be read",
                    String.join("\n", warnings));
        }
    }

    private void shutdown() {
        try {
            clinic.persistAll();
            clinic.settings().save();
        } catch (DataAccessException e) {
            boolean quit = Dialogs.confirm(this, "Could not save your changes",
                    "PawCare could not write the data files:\n" + e.getMessage()
                            + "\n\nQuit anyway and lose the unsaved changes?",
                    "Quit anyway");
            if (!quit) {
                return;
            }
        }
        dispose();
        System.exit(0);
    }
}
