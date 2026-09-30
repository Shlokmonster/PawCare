package com.pawcare.gui.dialogs;

import com.pawcare.exception.InvalidTreatmentException;
import com.pawcare.exception.PawCareException;
import com.pawcare.gui.components.ComboRenderers;
import com.pawcare.gui.components.ModernComboBox;
import com.pawcare.gui.components.ModernTextArea;
import com.pawcare.gui.components.ModernTextField;
import com.pawcare.model.Pet;
import com.pawcare.model.Treatment;
import com.pawcare.model.Veterinarian;
import com.pawcare.service.ClinicService;
import com.pawcare.util.DateUtil;
import com.pawcare.util.IDGenerator;

import java.awt.Window;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Records one entry in a pet's clinical history.
 *
 * <p>There is no edit mode: a treatment is a historical record of what was done on a
 * given day, so the application allows it to be added and removed but never rewritten.
 * That is a deliberate domain decision rather than a missing feature.</p>
 */
public class TreatmentDialog extends FormDialog {

    private final ClinicService clinic;

    private final ModernComboBox<Pet> petField;
    private final ModernComboBox<Veterinarian> vetField;
    private final ModernTextField dateField = new ModernTextField(DateUtil.DISPLAY_DATE_PATTERN);
    private final ModernTextField diagnosisField = new ModernTextField("e.g. Acute gastritis");
    private final ModernTextArea treatmentField =
            new ModernTextArea("What was done during the visit", 3);
    private final ModernTextField medicationField =
            new ModernTextField("e.g. Ranitidine oral syrup, or leave blank if none");
    private final ModernTextArea notesField =
            new ModernTextArea("Follow-up instructions, owner advice, observations", 3);

    /**
     * @param clinic     the service the record is saved through
     * @param presetPet  the pet to preselect, or null to use the first one
     */
    public TreatmentDialog(Window owner, ClinicService clinic, Pet presetPet) {
        super(owner, "Record a treatment",
                "Add an entry to the patient's clinical history.",
                "Save treatment");

        this.clinic = clinic;

        this.petField = new ModernComboBox<>(clinic.pets().sortedByName());
        this.petField.setRenderer(ComboRenderers.pets());

        this.vetField = new ModernComboBox<>(clinic.veterinarians().sortedByName());

        buildForm();
        populate(presetPet);
    }

    private void buildForm() {
        form().section("Visit");
        form().row("Patient", petField);
        form().row("Veterinarian", vetField);
        form().row("Date", dateField);
        form().hint("Date format: " + DateUtil.DISPLAY_DATE_PATTERN + ". A future date is not allowed.");

        form().section("Clinical details");
        form().row("Diagnosis", diagnosisField);
        form().stacked("Treatment given", treatmentField);
        form().row("Medication", medicationField);
        form().stacked("Notes", notesField);

        if (clinic.pets().count() == 0) {
            form().hint("No pets are registered yet — register a pet before recording a treatment.");
        }
        form().finish();
    }

    private void populate(Pet presetPet) {
        dateField.setText(DateUtil.toEditable(LocalDate.now()));

        if (vetField.getItemCount() > 0) {
            vetField.setSelectedIndex(0);
        }

        if (presetPet != null) {
            for (int i = 0; i < petField.getItemCount(); i++) {
                Pet candidate = petField.getItemAt(i);
                if (candidate != null && candidate.getAnimalId().equals(presetPet.getAnimalId())) {
                    petField.setSelectedIndex(i);
                    return;
                }
            }
        }
        if (petField.getItemCount() > 0) {
            petField.setSelectedIndex(0);
        }
    }

    @Override
    protected boolean onSave() throws PawCareException {
        clearErrors();

        List<String> problems = new ArrayList<>();

        LocalDate date = null;
        try {
            date = DateUtil.parseDate(dateField.getText());
        } catch (java.time.format.DateTimeParseException e) {
            problems.add("Date must look like " + DateUtil.DISPLAY_DATE_PATTERN + ".");
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
            throw new InvalidTreatmentException("Unable to save treatment record.", problems);
        }

        Treatment treatment = new Treatment(IDGenerator.nextTreatmentId(), pet.getAnimalId(),
                vet.getVeterinarianId(), date, text(diagnosisField), text(treatmentField),
                text(medicationField), text(notesField));

        // The service appends to the pet's LinkedList<Treatment> and validates the rest.
        clinic.addTreatment(treatment);
        toast("Treatment " + treatment.getTreatmentId() + " recorded for " + pet.getName());
        return true;
    }
}
