package com.pawcare.gui.pages;

import com.pawcare.exception.DataAccessException;
import com.pawcare.gui.Page;
import com.pawcare.gui.components.Card;
import com.pawcare.gui.components.FormPanel;
import com.pawcare.gui.components.ModernButton;
import com.pawcare.gui.components.ModernComboBox;
import com.pawcare.gui.components.ModernTextField;
import com.pawcare.gui.dialogs.Dialogs;
import com.pawcare.model.enums.VaccinationStatus;
import com.pawcare.service.ClinicService;
import com.pawcare.theme.Theme;
import com.pawcare.theme.ThemeManager;
import com.pawcare.util.AppSettings;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.io.IOException;
import java.nio.file.Path;
import java.util.TreeSet;

/**
 * Preferences, the clinic profile, the reminder window and the stored data.
 *
 * <p>Every change on this page takes effect immediately and is written to disk, so there is
 * no "apply" button to forget. The theme choice and the clinic name live in
 * {@code settings.properties}; the reminder window decides how the vaccination states are
 * classified on every screen.</p>
 */
public class SettingsPage extends Page {

    /** The windows the user can choose from, in days. */
    private static final int[] REMINDER_OPTIONS = {7, 14, 30, 45, 60, 90};

    private final ThemeManager themes;
    /** Run after the data or the preferences change, so every screen reloads. */
    private final Runnable onDataChanged;

    private final ModernComboBox<String> themeCombo =
            new ModernComboBox<>(new String[]{"Light", "Dark"});
    private final ModernComboBox<Integer> reminderCombo;
    private final ModernTextField clinicNameField = new ModernTextField("Clinic name", 26, true);
    private final JLabel recordsLabel = new JLabel();
    private final JLabel folderLabel = new JLabel();

    public SettingsPage(ClinicService clinic, ThemeManager themes, Runnable onDataChanged) {
        super(clinic, "Settings",
                "Preferences, the clinic profile, the vaccination reminder window and the stored data.");

        this.themes = themes;
        this.onDataChanged = onDataChanged;

        // The selection is applied before the listener is attached, so building the page
        // never looks like the user changed something.
        themeCombo.setSelectedItem(themes.getMode().getLabel());
        themeCombo.addActionListener(e -> applyTheme());

        reminderCombo = new ModernComboBox<>(reminderOptions());
        reminderCombo.setSelectedItem(clinic.settings().getReminderDays());
        reminderCombo.addActionListener(e -> applyReminderWindow());

        clinicNameField.setText(clinic.settings().getClinicName());

        Path folder = clinic.store().getDataDirectory().toAbsolutePath();
        folderLabel.setText(folder.toString());
        folderLabel.setFont(Theme.FONT_SMALL);
        folderLabel.setForeground(Theme.c().textPrimary);
        folderLabel.setToolTipText(folder.toString());

        recordsLabel.setFont(Theme.FONT_SMALL);
        recordsLabel.setForeground(Theme.c().textSecondary);

        stack(row(buildAppearanceCard(), buildProfileCard()), Theme.SPACE_XL);
        stack(buildReminderCard(), Theme.SPACE_XL);
        stack(row(buildStorageCard(), buildResetCard()), 0);

        updateStorageSummary();
    }

    // ------------------------------------------------------------------
    // Cards
    // ------------------------------------------------------------------

    private Card buildAppearanceCard() {
        Card card = new Card("Appearance",
                "The theme is saved and restored the next time PawCare starts.");

        FormPanel form = new FormPanel();
        form.row("Theme", compact(themeCombo));
        form.hint("Switching the theme repaints every screen immediately.");
        card.content().add(form, java.awt.BorderLayout.CENTER);
        return card;
    }

    private Card buildProfileCard() {
        Card card = new Card("Clinic profile",
                "The clinic name is shown in the header of every screen.");

        ModernButton save = new ModernButton("Save name",
                ModernButton.Variant.PRIMARY, "check");
        save.addActionListener(e -> saveClinicName());

        ModernButton restore = new ModernButton("Restore default",
                ModernButton.Variant.SECONDARY, "refresh");
        restore.addActionListener(e -> {
            clinicNameField.setText(AppSettings.DEFAULT_CLINIC_NAME);
            saveClinicName();
        });

        FormPanel form = new FormPanel();
        form.row("Clinic name", clinicNameField);
        form.fullWidth(buttonRow(save, restore));
        card.content().add(form, java.awt.BorderLayout.CENTER);
        return card;
    }

    private Card buildReminderCard() {
        Card card = new Card("Vaccination reminders",
                "How far ahead of the next due date a dose starts being flagged.");

        JPanel days = new JPanel(new FlowLayout(FlowLayout.LEFT, Theme.SPACE_SM, 0));
        days.setOpaque(false);
        days.add(reminderCombo);

        JLabel unit = new JLabel("days before the next dose is due");
        unit.setFont(Theme.FONT_SMALL);
        unit.setForeground(Theme.c().textSecondary);
        days.add(unit);

        FormPanel form = new FormPanel();
        form.row("Warn me", days);
        form.fullWidth(ruleRow(VaccinationStatus.OVERDUE,
                "the next dose was due before today."));
        form.fullWidth(ruleRow(VaccinationStatus.DUE_SOON,
                "the next dose falls inside the window above."));
        form.fullWidth(ruleRow(VaccinationStatus.UPCOMING,
                "the next dose is further away than the window above."));
        card.content().add(form, java.awt.BorderLayout.CENTER);
        return card;
    }

    private Card buildStorageCard() {
        Card card = new Card("Storage",
                "Every record is kept in a Java object file inside the data folder.");

        ModernButton saveNow = new ModernButton("Save to disk now",
                ModernButton.Variant.SECONDARY, "check");
        saveNow.addActionListener(e -> saveNow());

        ModernButton open = new ModernButton("Open folder",
                ModernButton.Variant.SECONDARY, "folder");
        open.addActionListener(e -> openDataFolder());

        FormPanel form = new FormPanel();
        form.row("Data folder", folderLabel);
        form.row("Records", recordsLabel);
        form.fullWidth(buttonRow(saveNow, open));
        card.content().add(form, java.awt.BorderLayout.CENTER);
        return card;
    }

    private Card buildResetCard() {
        Card card = new Card("Demo data",
                "Throw away every record and rebuild the sample clinic.");

        ModernButton reset = new ModernButton("Reset demo data",
                ModernButton.Variant.DANGER, "refresh");
        reset.addActionListener(e -> resetDemoData());

        FormPanel form = new FormPanel();
        form.hint("Useful before a demonstration: the clinic is returned to the "
                + "eight patients and five owners that ship with PawCare.");
        form.fullWidth(buttonRow(reset));
        card.content().add(form, java.awt.BorderLayout.CENTER);
        return card;
    }

    // ------------------------------------------------------------------
    // Small builders
    // ------------------------------------------------------------------

    /** Keeps a control at its natural width instead of letting the form stretch it. */
    private static JComponent compact(JComponent field) {
        JPanel holder = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        holder.setOpaque(false);
        holder.add(field);
        return holder;
    }

    private static JComponent buttonRow(JComponent... buttons) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, Theme.SPACE_SM, 0));
        row.setOpaque(false);
        for (JComponent button : buttons) {
            row.add(button);
        }
        return row;
    }

    /** One "coloured dot – explanation" line describing a vaccination state. */
    private static JComponent ruleRow(VaccinationStatus status, String explanation) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, Theme.SPACE_SM, 2));
        row.setOpaque(false);
        row.add(new Dot(Theme.badgeText(status)));

        JLabel label = new JLabel(status.getLabel() + "  –  " + explanation);
        label.setFont(Theme.FONT_SMALL);
        label.setForeground(Theme.c().textSecondary);
        row.add(label);
        return row;
    }

    // ------------------------------------------------------------------
    // Data
    // ------------------------------------------------------------------

    @Override
    public void refresh() {
        refreshHeader();
        updateStorageSummary();
        // Do not overwrite what the user is in the middle of typing.
        if (!clinicNameField.isFocusOwner()) {
            clinicNameField.setText(clinic.settings().getClinicName());
        }
    }

    private void updateStorageSummary() {
        recordsLabel.setText(clinic.pets().count() + " patients  ·  "
                + clinic.owners().count() + " owners  ·  "
                + clinic.veterinarians().count() + " veterinarians  ·  "
                + clinic.appointments().count() + " appointments  ·  "
                + clinic.treatments().count() + " treatments  ·  "
                + clinic.vaccinations().count() + " vaccinations");
    }

    /**
     * The stored window is offered alongside the standard choices, so a value written by
     * hand into the properties file still shows up in the list.
     */
    private Integer[] reminderOptions() {
        TreeSet<Integer> values = new TreeSet<>();
        for (int option : REMINDER_OPTIONS) {
            values.add(option);
        }
        values.add(Math.max(1, clinic.settings().getReminderDays()));
        return values.toArray(new Integer[0]);
    }

    // ------------------------------------------------------------------
    // Actions
    // ------------------------------------------------------------------

    private void applyTheme() {
        Object selected = themeCombo.getSelectedItem();
        if (selected == null) {
            return;
        }
        ThemeManager.Mode mode = ThemeManager.Mode.fromLabel(selected.toString());
        if (mode == themes.getMode()) {
            return;
        }
        // Persists the choice and asks the main window to repaint every screen.
        themes.setMode(mode);
    }

    private void applyReminderWindow() {
        Object selected = reminderCombo.getSelectedItem();
        if (!(selected instanceof Integer)) {
            return;
        }
        int days = (Integer) selected;
        if (days == clinic.settings().getReminderDays()) {
            return;
        }
        clinic.settings().setReminderDays(days);
        clinic.settings().save();
        if (onDataChanged != null) {
            onDataChanged.run();
        }
        toast("Vaccinations are now flagged " + days + " days ahead");
    }

    private void saveClinicName() {
        clinic.settings().setClinicName(clinicNameField.getText());
        clinic.settings().save();
        // A blank name falls back to the default, so read back what was actually stored.
        clinicNameField.setText(clinic.settings().getClinicName());
        refreshHeader();
        if (onDataChanged != null) {
            onDataChanged.run();
        }
        toast("Clinic name saved");
    }

    private void saveNow() {
        try {
            clinic.persistAll();
            toast("All records written to " + clinic.store().getDataDirectory());
        } catch (DataAccessException e) {
            Dialogs.error(this, "Could not save the data", e);
        }
    }

    private void openDataFolder() {
        Path folder = clinic.store().getDataDirectory().toAbsolutePath();

        if (!Desktop.isDesktopSupported()
                || !Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
            Dialogs.warning(this, "Cannot open the folder",
                    "This platform does not let PawCare open a file browser.\n"
                            + "The folder is:\n" + folder);
            return;
        }
        try {
            Desktop.getDesktop().open(folder.toFile());
        } catch (IOException | IllegalArgumentException e) {
            Dialogs.error(this, "Could not open the folder",
                    folder + "\n" + e.getMessage());
        }
    }

    private void resetDemoData() {
        boolean confirmed = Dialogs.confirm(this, "Reset demo data",
                "This deletes every patient, owner, appointment, treatment and vaccination, "
                        + "and restores the sample clinic that ships with PawCare.\n"
                        + "This cannot be undone.",
                "Reset data");
        if (!confirmed) {
            return;
        }
        try {
            clinic.resetDemoData();
            refresh();
            if (onDataChanged != null) {
                onDataChanged.run();
            }
            toast("Demo data restored");
        } catch (DataAccessException e) {
            Dialogs.error(this, "Could not reset the data", e);
        }
    }

    /** The coloured bullet used by the vaccination rule lines. */
    private static final class Dot extends JComponent {

        private final Color colour;

        Dot(Color colour) {
            this.colour = colour;
            setPreferredSize(new Dimension(10, 10));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(colour);
            g2.fillOval(0, (getHeight() - 8) / 2, 8, 8);
            g2.dispose();
        }
    }
}
