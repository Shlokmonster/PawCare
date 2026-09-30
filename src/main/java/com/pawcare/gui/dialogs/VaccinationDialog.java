package com.pawcare.gui.dialogs;

import com.pawcare.exception.InvalidVaccinationException;
import com.pawcare.exception.PawCareException;
import com.pawcare.gui.components.ComboRenderers;
import com.pawcare.gui.components.ModernComboBox;
import com.pawcare.gui.components.ModernTextArea;
import com.pawcare.gui.components.ModernTextField;
import com.pawcare.model.Pet;
import com.pawcare.model.Vaccination;
import com.pawcare.model.Veterinarian;
import com.pawcare.service.ClinicService;
import com.pawcare.util.DateUtil;
import com.pawcare.util.IDGenerator;

import java.awt.Window;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Records a vaccination dose and the date the next one falls due.
 *
 * <p>The next-due date is what drives the entire reminder system: it decides whether the
 * record shows as <i>Overdue</i>, <i>Due Soon</i> or <i>Upcoming</i> on the vaccination
 * page, on the dashboard and in the pet's profile. The field hint states the one ordering
 * rule the data must satisfy.</p>
 */
public class VaccinationDialog extends FormDialog {

    private final ClinicService clinic;
    private final Vaccination existing;

    private final ModernComboBox<Pet> petField;
    private final ModernComboBox<Veterinarian> vetField;
    private final ModernTextField vaccineField = new ModernTextField("e.g. Rabies");
    private final ModernTextField givenField = new ModernTextField(DateUtil.DISPLAY_DATE_PATTERN);
    private final ModernTextField dueField = new ModernTextField(DateUtil.DISPLAY_DATE_PATTERN);
    private final ModernTextArea notesField =
            new ModernTextArea("Batch number, reactions, reminders sent", 3);

    public VaccinationDialog(Window owner, ClinicService clinic, Vaccination existing, Pet presetPet) {
        super(owner,
                existing == null ? "Record a vaccination" : "Edit vaccination " + existing.getVaccinationId(),
                existing == null
                        ? "Log a dose and the date the next one is due."
                        : "Correct the details of this vaccination record.",
                existing == null ? "Save vaccination" : "Save changes");

        this.clinic = clinic;
        this.existing = existing;

        this.petField = new ModernComboBox<>(clinic.pets().sortedByName());
        this.petField.setRenderer(ComboRenderers.pets());

        this.vetField = new ModernComboBox<>(clinic.veterinarians().sortedByName());

        buildForm();
        populate(presetPet);
    }

    private void buildForm() {
        form().section("Vaccination");
        form().row("Patient", petField);
        form().row("Veterinarian", vetField);
        form().row("Vaccine", vaccineField);
        form().pair("Given on / next due", givenField, dueField);
        form().hint("Format " + DateUtil.DISPLAY_DATE_PATTERN
                + ". The next due date must be on or after the date the dose was given.");
        form().stacked("Notes", notesField);

        if (clinic.pets().count() == 0) {
            form().hint("No pets are registered yet — register a pet before recording a vaccination.");
        }
        form().finish();
    }

    private void populate(Pet presetPet) {
        if (existing == null) {
            givenField.setText(DateUtil.toEditable(LocalDate.now()));
            // A sensible default: most vaccines in the demo clinic are annual.
            dueField.setText(DateUtil.toEditable(LocalDate.now().plusYears(1)));
            selectFirst(vetField);

            if (!selectPet(presetPet == null ? null : presetPet.getAnimalId())) {
                selectFirst(petField);
            }
            return;
        }

        selectPet(existing.getPetId());
        selectVet(existing.getVeterinarianId());
        vaccineField.setText(existing.getVaccineName());
        givenField.setText(DateUtil.toEditable(existing.getVaccinationDate()));
        dueField.setText(DateUtil.toEditable(existing.getNextDueDate()));
        notesField.setText(existing.getNotes());
    }

    private boolean selectPet(String petId) {
        if (petId == null) {
            return false;
        }
        for (int i = 0; i < petField.getItemCount(); i++) {
            Pet candidate = petField.getItemAt(i);
            if (candidate != null && candidate.getAnimalId().equals(petId)) {
                petField.setSelectedIndex(i);
                return true;
            }
        }
        return false;
    }

    private void selectVet(String vetId) {
        for (int i = 0; i < vetField.getItemCount(); i++) {
            Veterinarian candidate = vetField.getItemAt(i);
            if (candidate != null && candidate.getVeterinarianId().equals(vetId)) {
                vetField.setSelectedIndex(i);
                return;
            }
        }
    }

    private static void selectFirst(ModernComboBox<?> combo) {
        if (combo.getItemCount() > 0) {
            combo.setSelectedIndex(0);
        }
    }

    @Override
    protected boolean onSave() throws PawCareException {
        clearErrors();

        List<String> problems = new ArrayList<>();

        LocalDate given = null;
        try {
            given = DateUtil.parseDate(givenField.getText());
        } catch (java.time.format.DateTimeParseException e) {
            problems.add("The date the dose was given must look like "
                    + DateUtil.DISPLAY_DATE_PATTERN + ".");
        }

        LocalDate due = null;
        try {
            due = DateUtil.parseDate(dueField.getText());
        } catch (java.time.format.DateTimeParseException e) {
            problems.add("The next due date must look like " + DateUtil.DISPLAY_DATE_PATTERN + ".");
        }

        Pet pet = (Pet) petField.getSelectedItem();
        if (pet == null) {
            problems.add("A patient must be selected.");
        }

        Veterinarian vet = (Veterinarian) vetField.getSelectedItem();
        if (vet == null) {
            problems.add("A veterinarian must be selected.");
        }

        if (!problems.isEmpty()) {
            throw new InvalidVaccinationException("Unable to save vaccination.", problems);
        }

        String vaccinationId = existing == null
                ? IDGenerator.nextVaccinationId() : existing.getVaccinationId();

        Vaccination vaccination = new Vaccination(vaccinationId, pet.getAnimalId(),
                vet.getVeterinarianId(), text(vaccineField), given, due, text(notesField));

        if (existing != null) {
            // Keep the acknowledgement flag the user may already have set.
            vaccination.setReviewed(existing.isReviewed());
            clinic.updateVaccination(vaccination);
            toast("Vaccination " + vaccinationId + " updated");
        } else {
            clinic.addVaccination(vaccination);
            toast(vaccination.getVaccineName() + " recorded for " + pet.getName());
        }
        return true;
    }
}
