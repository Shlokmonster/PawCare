package com.pawcare.gui.pages;

import com.pawcare.exception.PawCareException;
import com.pawcare.gui.Page;
import com.pawcare.gui.components.ActionColumn;
import com.pawcare.gui.components.BadgeRenderer;
import com.pawcare.gui.components.Card;
import com.pawcare.gui.components.Column;
import com.pawcare.gui.components.DateRenderer;
import com.pawcare.gui.components.EntityTableModel;
import com.pawcare.gui.components.ModernButton;
import com.pawcare.gui.components.ModernComboBox;
import com.pawcare.gui.components.ModernTable;
import com.pawcare.gui.components.SearchField;
import com.pawcare.gui.dialogs.Dialogs;
import com.pawcare.gui.dialogs.PetDetailsDialog;
import com.pawcare.gui.dialogs.PetDialog;
import com.pawcare.model.Pet;
import com.pawcare.model.enums.PetHealthStatus;
import com.pawcare.model.enums.Species;
import com.pawcare.service.ClinicService;
import com.pawcare.service.SearchService;
import com.pawcare.theme.Theme;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * The patient register: every pet the clinic treats.
 *
 * <p>Full create, read, update and delete is available here. Deleting a pet also removes
 * its appointments, treatments and vaccinations, which the confirmation text states
 * explicitly because the effect reaches beyond the row being clicked.</p>
 *
 * <p>The search box and the two filters are combined each time one of them changes, so a
 * user can narrow the register by species and vaccination state at the same time as
 * typing a name.</p>
 */
public class PetsPage extends Page {

    /** View index of the column holding the row actions. */
    private static final int ACTION_COLUMN = 9;
    /** Height of the table card. The page scrolls rather than squashing the table. */
    private static final int TABLE_HEIGHT = 430;

    private final SearchField search = new SearchField("Search by name, id, breed or owner");
    private final ModernComboBox<String> speciesFilter =
            new ModernComboBox<>(new String[]{"All species", "Dog", "Cat", "Bird"});
    private final ModernComboBox<String> healthFilter = new ModernComboBox<>(new String[]{
            "Any vaccination state", "Overdue", "Due soon", "Up to date", "No records"});
    private final JLabel summary = new JLabel();
    private final ModernTable<Pet> table;

    public PetsPage(ClinicService clinic) {
        super(clinic, "Pets",
                "Every patient registered with the clinic, its owner and its vaccination state.");

        ModernButton register = new ModernButton("Register pet",
                ModernButton.Variant.PRIMARY, "plus");
        register.addActionListener(e -> openForm(null));
        header().addAction(register);

        table = new ModernTable<>(buildColumns());
        table.setEmptyIcon("paw");
        table.setEmptyMessage("No patients match the current search and filters");
        ActionColumn.install(table, ACTION_COLUMN, this::onRowAction, "View", "Edit", "Delete");
        installDoubleClick();

        search.onChange(this::applyFilters);
        speciesFilter.addActionListener(e -> applyFilters());
        healthFilter.addActionListener(e -> applyFilters());

        summary.setFont(Theme.FONT_TINY);
        summary.setForeground(Theme.c().textSecondary);

        stack(buildToolbar(), Theme.SPACE_MD);
        stack(fixed(buildTableCard(), TABLE_HEIGHT), 0);
    }

    // ------------------------------------------------------------------
    // Static layout
    // ------------------------------------------------------------------

    private List<Column<Pet>> buildColumns() {
        List<Column<Pet>> columns = new ArrayList<>();
        columns.add(new Column<>("ID", Pet::getAnimalId, 70));
        columns.add(new Column<>("Name", Pet::getName, 150));
        columns.add(new Column<>("Species", Pet::getSpecies, 90));
        columns.add(new Column<>("Breed", Pet::getBreed, 160));
        columns.add(new Column<>("Age", Pet::getAge, 60));
        columns.add(new Column<>("Weight (kg)", Pet::getWeight, 95));
        columns.add(new Column<>("Owner",
                pet -> clinic.owners().ownerName(pet.getOwnerId()), 150));
        columns.add(new Column<>("Vaccination",
                pet -> clinic.vaccinations().statusForPet(pet.getAnimalId()), 130,
                new BadgeRenderer()));
        columns.add(new Column<>("Registered", Pet::getRegistrationDate, 110,
                new DateRenderer()));
        columns.add(new Column<>("Actions", pet -> "", 200));
        return columns;
    }

    private JComponent buildToolbar() {
        JPanel bar = new JPanel(new BorderLayout(Theme.SPACE_MD, 0));
        bar.setOpaque(false);
        bar.add(search, BorderLayout.CENTER);

        ModernButton clear = new ModernButton("Clear", ModernButton.Variant.SECONDARY, "refresh");
        clear.addActionListener(e -> {
            search.clear();
            speciesFilter.setSelectedIndex(0);
            healthFilter.setSelectedIndex(0);
            applyFilters();
        });

        JPanel filters = new JPanel(new FlowLayout(FlowLayout.RIGHT, Theme.SPACE_SM, 0));
        filters.setOpaque(false);
        filters.add(speciesFilter);
        filters.add(healthFilter);
        filters.add(clear);
        bar.add(filters, BorderLayout.EAST);
        return bar;
    }

    private Card buildTableCard() {
        Card card = new Card("Patient register",
                "Click a heading to sort. Use the row actions to view, edit or delete.",
                summary);
        card.content().add(scroll(table, 420), BorderLayout.CENTER);
        return card;
    }

    private void installDoubleClick() {
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() != 2 || table.columnAtPoint(e.getPoint()) == ACTION_COLUMN) {
                    return;
                }
                Pet pet = table.getObjectAt(table.rowAtPoint(e.getPoint()));
                if (pet != null) {
                    openDetails(pet);
                }
            }
        });
    }

    // ------------------------------------------------------------------
    // Data
    // ------------------------------------------------------------------

    @Override
    public void refresh() {
        refreshHeader();
        applyFilters();
    }

    /** Runs the search and both filters together and hands the result to the table. */
    private void applyFilters() {
        List<Pet> rows = clinic.search().search(search.getText(), SearchService.Scope.ALL);

        Species species = selectedSpecies();
        if (species != null) {
            List<Pet> narrowed = new ArrayList<>();
            for (Pet pet : rows) {
                if (pet.getSpecies() == species) {
                    narrowed.add(pet);
                }
            }
            rows = narrowed;
        }

        PetHealthStatus health = selectedHealth();
        if (health != null) {
            List<Pet> narrowed = new ArrayList<>();
            for (Pet pet : rows) {
                if (clinic.vaccinations().statusForPet(pet.getAnimalId()) == health) {
                    narrowed.add(pet);
                }
            }
            rows = narrowed;
        }

        int total = clinic.pets().count();
        summary.setText(rows.size() + " of " + total + (total == 1 ? " patient" : " patients"));
        table.setRows(rows);
    }

    private Species selectedSpecies() {
        int index = speciesFilter.getSelectedIndex();
        return index <= 0 ? null : Species.fromLabel(speciesFilter.getItemAt(index));
    }

    private PetHealthStatus selectedHealth() {
        switch (healthFilter.getSelectedIndex()) {
            case 1:
                return PetHealthStatus.OVERDUE;
            case 2:
                return PetHealthStatus.DUE_SOON;
            case 3:
                return PetHealthStatus.UP_TO_DATE;
            case 4:
                return PetHealthStatus.NO_RECORDS;
            default:
                return null;
        }
    }

    // ------------------------------------------------------------------
    // Actions
    // ------------------------------------------------------------------

    private void onRowAction(String action, int modelRow) {
        Pet pet = table.getEntityModel().getRow(modelRow);
        if (pet == null) {
            return;
        }
        switch (action) {
            case "View":
                openDetails(pet);
                break;
            case "Edit":
                openForm(pet);
                break;
            case "Delete":
                delete(pet);
                break;
            default:
                break;
        }
    }

    private void openDetails(Pet pet) {
        new PetDetailsDialog(window(), clinic, pet).setVisible(true);
    }

    private void openForm(Pet existing) {
        if (existing == null && clinic.owners().count() == 0) {
            Dialogs.warning(this, "No owners registered",
                    "A pet must belong to an owner. Add an owner on the Owners screen first.");
            return;
        }
        PetDialog dialog = new PetDialog(window(), clinic, existing);
        dialog.setVisible(true);
        if (dialog.isSaved()) {
            refresh();
            toast(existing == null ? "Patient registered" : "Patient record updated");
        }
    }

    private void delete(Pet pet) {
        int appointments = clinic.appointments().byPet(pet.getAnimalId()).size();
        int treatments = clinic.treatments().countForPet(pet.getAnimalId());
        int vaccinations = clinic.vaccinations().forPet(pet.getAnimalId()).size();

        String subject = pet.getName() + " (" + pet.getAnimalId() + "), together with "
                + (appointments + treatments + vaccinations)
                + " related appointment, treatment and vaccination records";
        if (!Dialogs.confirmDelete(this, subject)) {
            return;
        }
        try {
            clinic.deletePet(pet.getAnimalId());
            refresh();
            toast(pet.getName() + " was removed from the register");
        } catch (PawCareException e) {
            Dialogs.error(this, "Unable to delete patient", e);
        }
    }
}
