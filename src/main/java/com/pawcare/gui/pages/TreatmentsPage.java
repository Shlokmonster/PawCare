package com.pawcare.gui.pages;

import com.pawcare.exception.PawCareException;
import com.pawcare.gui.Page;
import com.pawcare.gui.components.ActionColumn;
import com.pawcare.gui.components.Card;
import com.pawcare.gui.components.Column;
import com.pawcare.gui.components.DateRenderer;
import com.pawcare.gui.components.ListCard;
import com.pawcare.gui.components.ModernButton;
import com.pawcare.gui.components.ModernTable;
import com.pawcare.gui.components.SearchField;
import com.pawcare.gui.components.SectionHeader;
import com.pawcare.gui.components.StatCard;
import com.pawcare.gui.dialogs.Dialogs;
import com.pawcare.gui.dialogs.TreatmentDialog;
import com.pawcare.model.Pet;
import com.pawcare.model.Treatment;
import com.pawcare.service.ClinicService;
import com.pawcare.theme.Theme;
import com.pawcare.util.DateUtil;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

/**
 * The clinical log: what was diagnosed and done, and when.
 *
 * <p>Treatments can be recorded and removed but never rewritten. A treatment is the
 * record of what happened on a day, so editing it afterwards would falsify the history;
 * that is a deliberate domain rule, not a missing form.</p>
 *
 * <p>Selecting a row loads that patient's complete history from the
 * {@code LinkedList<Treatment>} the service keeps per pet, which is a chronological
 * walk over a linked list – the access pattern the collection was chosen for.</p>
 */
public class TreatmentsPage extends Page {

    /** View index of the column holding the row actions. */
    private static final int ACTION_COLUMN = 7;
    /** Heights of the two stacked cards. The page scrolls rather than squashing them. */
    private static final int TABLE_HEIGHT = 340;
    private static final int HISTORY_HEIGHT = 280;

    private final SearchField search = new SearchField("Search by patient, diagnosis, treatment or id");
    private final JLabel summary = new JLabel();
    private final ModernTable<Treatment> table;

    private final SectionHeader historyHeader = new SectionHeader("Treatment history",
            "Select a row in the table above to load that patient's full history");
    private final ListCard historyCard;

    private final StatCard totalCard =
            new StatCard("Treatment records", "0", "logged in total", "treatments", Theme.c().primary);
    private final StatCard recentCard =
            new StatCard("Last 30 days", "0", "recent entries", "clock", Theme.c().accent);
    private final StatCard patientsCard =
            new StatCard("Patients treated", "0", "with at least one entry", "paw", Theme.c().info);

    public TreatmentsPage(ClinicService clinic) {
        super(clinic, "Treatments",
                "The clinical history of every patient. Entries are added and removed, never rewritten.");

        ModernButton record = new ModernButton("Record treatment",
                ModernButton.Variant.PRIMARY, "plus");
        record.addActionListener(e -> openForm(null));
        header().addAction(record);

        table = new ModernTable<>(buildColumns());
        table.setEmptyIcon("treatments");
        table.setEmptyMessage("No treatments match the current search");
        ActionColumn.install(table, ACTION_COLUMN, this::onRowAction, "Delete");
        table.getSelectionModel().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) {
                showHistory(table.getSelectedObject());
            }
        });

        historyCard = new ListCard("Patient history",
                "Oldest entry first, read from the patient's linked list",
                "No patient selected",
                "Select a treatment row above to load that patient's complete history.");

        search.onChange(this::applyFilters);

        summary.setFont(Theme.FONT_TINY);
        summary.setForeground(Theme.c().textSecondary);

        stack(row(totalCard, recentCard, patientsCard), Theme.SPACE_XL);
        stack(buildToolbar(), Theme.SPACE_MD);
        stack(fixed(buildTableCard(), TABLE_HEIGHT), Theme.SPACE_XL);
        stack(historyHeader, Theme.SPACE_MD);
        stack(fixed(historyCard, HISTORY_HEIGHT), 0);
    }

    // ------------------------------------------------------------------
    // Static layout
    // ------------------------------------------------------------------

    private List<Column<Treatment>> buildColumns() {
        List<Column<Treatment>> columns = new ArrayList<>();
        columns.add(new Column<>("ID", Treatment::getTreatmentId, 70));
        columns.add(new Column<>("Date", Treatment::getDate, 115, new DateRenderer()));
        columns.add(new Column<>("Patient",
                treatment -> clinic.pets().petName(treatment.getPetId()), 150));
        columns.add(new Column<>("Veterinarian",
                treatment -> clinic.veterinarians().vetName(treatment.getVeterinarianId()), 160));
        columns.add(new Column<>("Diagnosis", Treatment::getDiagnosis, 210));
        columns.add(new Column<>("Treatment", Treatment::getTreatment, 240));
        columns.add(new Column<>("Medication", Treatment::getMedication, 180));
        columns.add(new Column<>("Actions", treatment -> "", 100));
        return columns;
    }

    private JComponent buildToolbar() {
        JPanel bar = new JPanel(new BorderLayout(Theme.SPACE_MD, 0));
        bar.setOpaque(false);
        bar.add(search, BorderLayout.CENTER);

        ModernButton clear = new ModernButton("Clear", ModernButton.Variant.SECONDARY, "refresh");
        clear.addActionListener(e -> {
            search.clear();
            applyFilters();
        });

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, Theme.SPACE_SM, 0));
        right.setOpaque(false);
        right.add(clear);
        bar.add(right, BorderLayout.EAST);
        return bar;
    }

    private Card buildTableCard() {
        Card card = new Card("Clinical log",
                "Newest first. Click a row to load the patient's full history below.",
                summary);
        card.content().add(scroll(table, 340), BorderLayout.CENTER);
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
        showHistory(table.getSelectedObject());
    }

    private void updateSummaryCards() {
        int total = clinic.treatments().count();
        totalCard.setValue(String.valueOf(total));
        totalCard.setCaption(total == 1 ? "entry on file" : "entries on file");

        int recent = clinic.treatments().countRecent(30);
        recentCard.setValue(String.valueOf(recent));
        recentCard.setCaption("since " + DateUtil.format(LocalDate.now().minusDays(30)));

        int patients = 0;
        for (Pet pet : clinic.pets().getAllPets()) {
            if (clinic.treatments().countForPet(pet.getAnimalId()) > 0) {
                patients++;
            }
        }
        patientsCard.setValue(String.valueOf(patients));
        patientsCard.setCaption("of " + clinic.pets().count() + " registered patients");
    }

    private void applyFilters() {
        List<Treatment> rows = new ArrayList<>();
        for (Treatment treatment : clinic.treatments().allNewestFirst()) {
            if (matchesSearch(treatment)) {
                rows.add(treatment);
            }
        }
        int total = clinic.treatments().count();
        summary.setText(rows.size() + " of " + total + (total == 1 ? " entry" : " entries"));
        table.setRows(rows);
    }

    private boolean matchesSearch(Treatment treatment) {
        String query = search.getText();
        if (query == null || query.isBlank()) {
            return true;
        }
        String needle = query.trim().toLowerCase(Locale.ENGLISH);
        return contains(clinic.pets().petName(treatment.getPetId()), needle)
                || contains(clinic.veterinarians().vetName(treatment.getVeterinarianId()), needle)
                || contains(treatment.getDiagnosis(), needle)
                || contains(treatment.getTreatment(), needle)
                || contains(treatment.getMedication(), needle)
                || contains(treatment.getTreatmentId(), needle);
    }

    private static boolean contains(String value, String needle) {
        return value != null && value.toLowerCase(Locale.ENGLISH).contains(needle);
    }

    /**
     * Loads one patient's history and rebuilds the list card beneath the table.
     *
     * <p>The table hands back the row object it holds – a {@link Treatment} – so the pet
     * it belongs to is looked up from the record's own pet id rather than passed in.</p>
     */
    private void showHistory(Treatment selected) {
        historyCard.clearRows();

        Pet pet = selected == null
                ? null
                : clinic.pets().findPetById(selected.getPetId()).orElse(null);

        if (pet == null) {
            historyHeader.setSubtitle("Select a row in the table above to load that patient's full history");
            historyCard.emptyState().setText("No patient selected",
                    "Select a treatment row above to load that patient's complete history.");
            historyCard.refresh();
            return;
        }

        LinkedList<Treatment> history = clinic.treatments().getTreatmentHistory(pet.getAnimalId());
        historyHeader.setSubtitle(pet.getName() + " (" + pet.getAnimalId() + ")  ·  "
                + history.size() + (history.size() == 1 ? " entry" : " entries")
                + "  ·  oldest first");

        historyCard.emptyState().setText("No treatments recorded",
                pet.getName() + " has no treatment entries yet.");

        for (Treatment treatment : history) {
            historyCard.addRow("treatments", Theme.c().accent,
                    treatment.getDiagnosis(),
                    treatment.getTreatment()
                            + "  ·  " + clinic.veterinarians().vetName(treatment.getVeterinarianId()),
                    DateUtil.format(treatment.getDate()),
                    null);
        }
        historyCard.refresh();
    }

    // ------------------------------------------------------------------
    // Actions
    // ------------------------------------------------------------------

    private void onRowAction(String action, int modelRow) {
        Treatment treatment = table.getEntityModel().getRow(modelRow);
        if (treatment != null && "Delete".equals(action)) {
            delete(treatment);
        }
    }

    private void openForm(Pet presetPet) {
        if (clinic.pets().count() == 0) {
            Dialogs.warning(this, "No patients registered",
                    "Register a patient before recording a treatment.");
            return;
        }
        TreatmentDialog dialog = new TreatmentDialog(window(), clinic, presetPet);
        dialog.setVisible(true);
        if (dialog.isSaved()) {
            refresh();
            toast("Treatment recorded in the patient's history");
        }
    }

    private void delete(Treatment treatment) {
        String subject = "the treatment record of "
                + DateUtil.format(treatment.getDate()) + " for "
                + clinic.pets().petName(treatment.getPetId());
        if (!Dialogs.confirmDelete(this, subject)) {
            return;
        }
        try {
            clinic.deleteTreatment(treatment.getTreatmentId());
            refresh();
            toast("Treatment entry removed");
        } catch (PawCareException e) {
            Dialogs.error(this, "Unable to delete the treatment", e);
        }
    }
}
