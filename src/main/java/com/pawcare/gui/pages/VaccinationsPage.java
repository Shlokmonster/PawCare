package com.pawcare.gui.pages;

import com.pawcare.exception.PawCareException;
import com.pawcare.gui.Page;
import com.pawcare.gui.components.ActionColumn;
import com.pawcare.gui.components.BadgeRenderer;
import com.pawcare.gui.components.Card;
import com.pawcare.gui.components.Column;
import com.pawcare.gui.components.DateRenderer;
import com.pawcare.gui.components.ModernButton;
import com.pawcare.gui.components.ModernComboBox;
import com.pawcare.gui.components.ModernTable;
import com.pawcare.gui.components.SearchField;
import com.pawcare.gui.components.StatCard;
import com.pawcare.gui.dialogs.Dialogs;
import com.pawcare.gui.dialogs.VaccinationDialog;
import com.pawcare.model.Vaccination;
import com.pawcare.model.enums.VaccinationStatus;
import com.pawcare.service.ClinicService;
import com.pawcare.theme.Theme;
import com.pawcare.util.DateUtil;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The vaccination register and the clinic's reminder list.
 *
 * <p>The state of every record is derived, never stored. {@link VaccinationStatus} holds
 * the single definition of the rule:</p>
 * <ul>
 *   <li><b>Overdue</b> – the next dose was due before today;</li>
 *   <li><b>Due Soon</b> – it falls inside the reminder window configured in Settings;</li>
 *   <li><b>Upcoming</b> – it is further away than that window.</li>
 * </ul>
 *
 * <p>Changing the reminder window on the Settings page therefore reclassifies this whole
 * page immediately, because nothing here caches the result.</p>
 */
public class VaccinationsPage extends Page {

    /** View index of the column holding the row actions. */
    private static final int ACTION_COLUMN = 7;
    /** Height of the table card. The page scrolls rather than squashing the table. */
    private static final int TABLE_HEIGHT = 420;

    private final SearchField search = new SearchField("Search by patient, vaccine or id");
    private final ModernComboBox<String> statusFilter = new ModernComboBox<>(
            new String[]{"All states", "Overdue", "Due soon", "Upcoming"});
    private final ModernComboBox<String> reminderFilter = new ModernComboBox<>(
            new String[]{"All reminders", "Acknowledged", "Pending"});
    private final JLabel summary = new JLabel();
    private final ModernTable<Vaccination> table;

    private final StatCard totalCard =
            new StatCard("Vaccination records", "0", "doses on file", "vaccinations", Theme.c().primary);
    private final StatCard overdueCard =
            new StatCard("Overdue", "0", "doses missed", "bell", Theme.c().danger);
    private final StatCard dueSoonCard =
            new StatCard("Due soon", "0", "inside the reminder window", "clock", Theme.c().warning);
    private final StatCard petsCard =
            new StatCard("Patients affected", "0", "need a reminder", "paw", Theme.c().accent);

    public VaccinationsPage(ClinicService clinic) {
        super(clinic, "Vaccinations",
                "Every dose on record and the reminder state derived from its next due date.");

        ModernButton record = new ModernButton("Record vaccination",
                ModernButton.Variant.PRIMARY, "plus");
        record.addActionListener(e -> openForm(null, null));
        header().addAction(record);

        table = new ModernTable<>(buildColumns());
        table.setEmptyIcon("vaccinations");
        table.setEmptyMessage("No vaccination records match the current filters");
        ActionColumn.install(table, ACTION_COLUMN, this::onRowAction,
                "Toggle reminder", "Edit", "Delete");

        search.onChange(this::applyFilters);
        statusFilter.addActionListener(e -> applyFilters());
        reminderFilter.addActionListener(e -> applyFilters());

        summary.setFont(Theme.FONT_TINY);
        summary.setForeground(Theme.c().textSecondary);

        stack(row(totalCard, overdueCard, dueSoonCard, petsCard), Theme.SPACE_XL);
        stack(buildToolbar(), Theme.SPACE_MD);
        stack(fixed(buildTableCard(), TABLE_HEIGHT), 0);
    }

    // ------------------------------------------------------------------
    // Static layout
    // ------------------------------------------------------------------

    private List<Column<Vaccination>> buildColumns() {
        List<Column<Vaccination>> columns = new ArrayList<>();
        columns.add(new Column<>("ID", Vaccination::getVaccinationId, 85));
        columns.add(new Column<>("Vaccine", Vaccination::getVaccineName, 150));
        columns.add(new Column<>("Patient",
                vaccination -> clinic.pets().petName(vaccination.getPetId()), 150));
        columns.add(new Column<>("Given on", Vaccination::getVaccinationDate, 115,
                new DateRenderer()));
        columns.add(new Column<>("Next due", Vaccination::getNextDueDate, 115,
                new DateRenderer()));
        columns.add(new Column<>("State", clinic.vaccinations()::statusOf, 110,
                new BadgeRenderer()));
        columns.add(new Column<>("Reminder",
                vaccination -> vaccination.isReviewed() ? "Acknowledged" : "Pending", 130));
        columns.add(new Column<>("Actions", vaccination -> "", 250));
        return columns;
    }

    private JComponent buildToolbar() {
        JPanel bar = new JPanel(new BorderLayout(Theme.SPACE_MD, 0));
        bar.setOpaque(false);
        bar.add(search, BorderLayout.CENTER);

        ModernButton clear = new ModernButton("Clear", ModernButton.Variant.SECONDARY, "refresh");
        clear.addActionListener(e -> {
            search.clear();
            statusFilter.setSelectedIndex(0);
            reminderFilter.setSelectedIndex(0);
            applyFilters();
        });

        JPanel filters = new JPanel(new FlowLayout(FlowLayout.RIGHT, Theme.SPACE_SM, 0));
        filters.setOpaque(false);
        filters.add(statusFilter);
        filters.add(reminderFilter);
        filters.add(clear);
        bar.add(filters, BorderLayout.EAST);
        return bar;
    }

    private Card buildTableCard() {
        Card card = new Card("Vaccination register",
                "The reminder window is " + clinic.vaccinations().getReminderDays()
                        + " days. Change it on the Settings page.",
                summary);
        card.content().add(scroll(table, 380), BorderLayout.CENTER);
        return card;
    }

    // ------------------------------------------------------------------
    // Data
    // ------------------------------------------------------------------

    @Override
    public void refresh() {
        refreshHeader();
        updateSummaryCards();
        applyFilters();
    }

    private void updateSummaryCards() {
        int total = clinic.vaccinations().count();
        int overdue = clinic.vaccinations().countOverdue();
        int dueSoon = clinic.vaccinations().countDueSoon();

        totalCard.setValue(String.valueOf(total));
        totalCard.setCaption("over " + clinic.pets().count()
                + (clinic.pets().count() == 1 ? " patient" : " patients"));

        overdueCard.setValue(String.valueOf(overdue));
        overdueCard.setCaption(overdue == 0 ? "nothing missed" : "act on these first");

        dueSoonCard.setValue(String.valueOf(dueSoon));
        dueSoonCard.setCaption("due within " + clinic.vaccinations().getReminderDays() + " days");

        int affected = clinic.vaccinations().petsNeedingAttention();
        petsCard.setValue(String.valueOf(affected));
        petsCard.setCaption(affected == 1 ? "patient to contact" : "patients to contact");
    }

    private void applyFilters() {
        List<Vaccination> rows = new ArrayList<>();
        for (Vaccination vaccination : clinic.vaccinations().getAll()) {
            if (matchesSearch(vaccination) && matchesStatus(vaccination) && matchesReminder(vaccination)) {
                rows.add(vaccination);
            }
        }
        rows.sort(Vaccination.BY_NEXT_DUE);

        int total = clinic.vaccinations().count();
        summary.setText(rows.size() + " of " + total + (total == 1 ? " record" : " records"));
        table.setRows(rows);
    }

    private boolean matchesSearch(Vaccination vaccination) {
        String query = search.getText();
        if (query == null || query.isBlank()) {
            return true;
        }
        String needle = query.trim().toLowerCase(Locale.ENGLISH);
        return contains(clinic.pets().petName(vaccination.getPetId()), needle)
                || contains(vaccination.getVaccineName(), needle)
                || contains(vaccination.getVaccinationId(), needle)
                || contains(vaccination.getNotes(), needle);
    }

    private boolean matchesStatus(Vaccination vaccination) {
        VaccinationStatus status = clinic.vaccinations().statusOf(vaccination);
        switch (statusFilter.getSelectedIndex()) {
            case 1:
                return status == VaccinationStatus.OVERDUE;
            case 2:
                return status == VaccinationStatus.DUE_SOON;
            case 3:
                return status == VaccinationStatus.UPCOMING;
            default:
                return true;
        }
    }

    private boolean matchesReminder(Vaccination vaccination) {
        switch (reminderFilter.getSelectedIndex()) {
            case 1:
                return vaccination.isReviewed();
            case 2:
                return !vaccination.isReviewed();
            default:
                return true;
        }
    }

    private static boolean contains(String value, String needle) {
        return value != null && value.toLowerCase(Locale.ENGLISH).contains(needle);
    }

    // ------------------------------------------------------------------
    // Actions
    // ------------------------------------------------------------------

    private void onRowAction(String action, int modelRow) {
        Vaccination vaccination = table.getEntityModel().getRow(modelRow);
        if (vaccination == null) {
            return;
        }
        switch (action) {
            case "Toggle reminder":
                toggleReminder(vaccination);
                break;
            case "Edit":
                openForm(vaccination, null);
                break;
            case "Delete":
                delete(vaccination);
                break;
            default:
                break;
        }
    }

    /** Acknowledges the reminder, or reopens it when it had already been acknowledged. */
    private void toggleReminder(Vaccination vaccination) {
        boolean acknowledge = !vaccination.isReviewed();
        try {
            clinic.markVaccinationReviewed(vaccination.getVaccinationId(), acknowledge);
            refresh();
            toast(acknowledge
                    ? "Reminder acknowledged for " + clinic.pets().petName(vaccination.getPetId())
                    : "Reminder re-opened for " + clinic.pets().petName(vaccination.getPetId()));
        } catch (PawCareException e) {
            Dialogs.error(this, "Unable to update the reminder", e);
        }
    }

    private void openForm(Vaccination existing, com.pawcare.model.Pet presetPet) {
        if (clinic.pets().count() == 0) {
            Dialogs.warning(this, "No patients registered",
                    "Register a patient before recording a vaccination.");
            return;
        }
        VaccinationDialog dialog = new VaccinationDialog(window(), clinic, existing, presetPet);
        dialog.setVisible(true);
        if (dialog.isSaved()) {
            refresh();
            toast(existing == null ? "Vaccination recorded" : "Vaccination record updated");
        }
    }

    private void delete(Vaccination vaccination) {
        String subject = "the " + vaccination.getVaccineName() + " record for "
                + clinic.pets().petName(vaccination.getPetId())
                + " (next due " + DateUtil.format(vaccination.getNextDueDate()) + ")";
        if (!Dialogs.confirmDelete(this, subject)) {
            return;
        }
        try {
            clinic.deleteVaccination(vaccination.getVaccinationId());
            refresh();
            toast("Vaccination record deleted");
        } catch (PawCareException e) {
            Dialogs.error(this, "Unable to delete the vaccination", e);
        }
    }
}
