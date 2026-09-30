package com.pawcare.gui.dialogs;

import com.pawcare.exception.InvalidPetException;
import com.pawcare.exception.PawCareException;
import com.pawcare.gui.components.FormPanel;
import com.pawcare.gui.components.ModernComboBox;
import com.pawcare.gui.components.ModernTextArea;
import com.pawcare.gui.components.ModernTextField;
import com.pawcare.model.Bird;
import com.pawcare.model.Cat;
import com.pawcare.model.Dog;
import com.pawcare.model.MedicalInfo;
import com.pawcare.model.Owner;
import com.pawcare.model.Pet;
import com.pawcare.model.enums.Gender;
import com.pawcare.model.enums.IndoorOutdoor;
import com.pawcare.model.enums.Species;
import com.pawcare.model.enums.TrainingLevel;
import com.pawcare.service.ClinicService;
import com.pawcare.util.DateUtil;
import com.pawcare.util.IDGenerator;

import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.CardLayout;
import java.awt.Window;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * The form used to register a new pet or edit an existing one.
 *
 * <p>The interesting part is the species row. Choosing <i>Dog</i>, <i>Cat</i> or
 * <i>Bird</i> swaps the extra field and retitles its label, and the record that is built
 * on save is a {@link Dog}, {@link Cat} or {@link Bird} accordingly. The dialog therefore
 * constructs objects of three different classes through one form – which is the
 * inheritance hierarchy made visible.</p>
 *
 * <p>Once a pet exists its species cannot change, because the concrete Java class of a
 * saved object cannot be swapped. The combo box is disabled while editing, which also
 * makes that rule obvious to the user.</p>
 */
public class PetDialog extends FormDialog {

    private static final String DOG_CARD = "dog";
    private static final String CAT_CARD = "cat";
    private static final String BIRD_CARD = "bird";

    private final ClinicService clinic;
    private final Pet existing;

    // Common fields -----------------------------------------------------
    private final ModernComboBox<Species> speciesField = ModernComboBox.of(Species.class);
    private final ModernTextField nameField = new ModernTextField("e.g. Bruno");
    private final ModernTextField ageField = new ModernTextField("e.g. 4");
    private final ModernTextField weightField = new ModernTextField("e.g. 12.5");
    private final ModernComboBox<Gender> genderField = ModernComboBox.of(Gender.class);
    private final ModernTextField breedField = new ModernTextField("e.g. Labrador Retriever");
    private final ModernComboBox<Owner> ownerField;
    private final ModernTextField registrationField =
            new ModernTextField(DateUtil.DISPLAY_DATE_PATTERN);
    private final ModernTextField allergiesField = new ModernTextField("e.g. Chicken protein");
    private final ModernTextField conditionsField = new ModernTextField("e.g. Hip dysplasia");
    private final ModernTextArea notesField =
            new ModernTextArea("Handling notes, temperament, anything the clinic should know", 3);

    // Species-specific fields -------------------------------------------
    private final JLabel speciesDetailLabel = FormPanel.createLabel("Training level");
    private final CardLayout speciesDetailLayout = new CardLayout();
    private final JPanel speciesDetailHolder = new JPanel(speciesDetailLayout);
    private final ModernComboBox<TrainingLevel> trainingField = ModernComboBox.of(TrainingLevel.class);
    private final ModernComboBox<IndoorOutdoor> lifestyleField = ModernComboBox.of(IndoorOutdoor.class);
    private final ModernTextField wingSpanField = new ModernTextField("e.g. 18.5");

    /**
     * @param clinic   the service the record is saved through
     * @param existing the pet being edited, or null to register a new one
     */
    public PetDialog(Window owner, ClinicService clinic, Pet existing) {
        super(owner,
                existing == null ? "Register a new pet" : "Edit " + existing.getName(),
                existing == null
                        ? "Record the patient's details and assign it to an owner."
                        : "Update the patient record. The species of a saved pet cannot change.",
                existing == null ? "Register pet" : "Save changes");

        this.clinic = clinic;
        this.existing = existing;

        this.ownerField = new ModernComboBox<>(clinic.owners().sortedByName());

        buildForm();
        populate();

        speciesField.addActionListener(e -> applySpecies((Species) speciesField.getSelectedItem()));
        applySpecies((Species) speciesField.getSelectedItem());
    }

    // ------------------------------------------------------------------
    // Form
    // ------------------------------------------------------------------

    private void buildForm() {
        form().section("Identification");
        form().row("Species", speciesField);
        form().row("Name", nameField);
        form().pair("Age & weight", ageField, weightField);
        form().hint("Age is in completed years; weight is in kilograms.");

        form().section("Classification");
        form().row("Gender", genderField);
        form().row("Breed", breedField);
        form().row(speciesDetailLabel, speciesDetailHolder);
        buildSpeciesCards();

        form().section("Ownership");
        form().row("Owner", ownerField);
        form().row("Registered on", registrationField);
        form().hint("Date format: " + DateUtil.DISPLAY_DATE_PATTERN
                + " — for example " + DateUtil.toEditable(LocalDate.now()));

        form().section("Medical background");
        form().row("Allergies", allergiesField);
        form().row("Chronic conditions", conditionsField);
        form().stacked("Notes", notesField);

        if (clinic.owners().count() == 0) {
            form().hint("No owners are registered yet — add an owner before saving a pet.");
        }
        form().finish();
    }

    /**
     * All three species-specific inputs live in one card panel; choosing a species shows
     * the matching card and retitles the label beside it.
     */
    private void buildSpeciesCards() {
        speciesDetailHolder.setOpaque(false);

        speciesDetailHolder.add(trainingField, DOG_CARD);
        speciesDetailHolder.add(lifestyleField, CAT_CARD);
        speciesDetailHolder.add(wingSpanField, BIRD_CARD);
    }

    private void applySpecies(Species species) {
        Species effective = species == null ? Species.DOG : species;
        switch (effective) {
            case CAT:
                speciesDetailLabel.setText("Lifestyle");
                speciesDetailLayout.show(speciesDetailHolder, CAT_CARD);
                break;
            case BIRD:
                speciesDetailLabel.setText("Wing span (cm)");
                speciesDetailLayout.show(speciesDetailHolder, BIRD_CARD);
                break;
            case DOG:
            default:
                speciesDetailLabel.setText("Training level");
                speciesDetailLayout.show(speciesDetailHolder, DOG_CARD);
                break;
        }
    }

    /** Copies the existing record into the fields when editing. */
    private void populate() {
        if (existing == null) {
            speciesField.setSelectedItem(Species.DOG);
            genderField.setSelectedItem(Gender.MALE);
            registrationField.setText(DateUtil.toEditable(LocalDate.now()));
            if (ownerField.getItemCount() > 0) {
                ownerField.setSelectedIndex(0);
            }
            return;
        }

        speciesField.setSelectedItem(existing.getSpecies());
        // The Java class of a saved pet is fixed, so the species is shown but locked.
        speciesField.setEnabled(false);

        nameField.setText(existing.getName());
        ageField.setText(String.valueOf(existing.getAge()));
        weightField.setText(String.valueOf(existing.getWeight()));
        genderField.setSelectedItem(existing.getGender());
        breedField.setText(existing.getBreed());
        registrationField.setText(DateUtil.toEditable(existing.getRegistrationDate()));

        selectOwner(existing.getOwnerId());

        MedicalInfo medical = existing.getMedicalInfo();
        if (medical != null) {
            allergiesField.setText(medical.getAllergies());
            conditionsField.setText(medical.getChronicConditions());
            notesField.setText(medical.getNotes());
        }

        // Read the subclass-specific value through the concrete type.
        if (existing instanceof Dog) {
            trainingField.setSelectedItem(((Dog) existing).getTrainingLevel());
        } else if (existing instanceof Cat) {
            lifestyleField.setSelectedItem(((Cat) existing).getIndoorOrOutdoor());
        } else if (existing instanceof Bird) {
            wingSpanField.setText(String.valueOf(((Bird) existing).getWingSpan()));
        }
    }

    private void selectOwner(String ownerId) {
        for (int i = 0; i < ownerField.getItemCount(); i++) {
            Owner candidate = ownerField.getItemAt(i);
            if (candidate != null && candidate.getOwnerId().equals(ownerId)) {
                ownerField.setSelectedIndex(i);
                return;
            }
        }
    }

    // ------------------------------------------------------------------
    // Saving
    // ------------------------------------------------------------------

    @Override
    protected boolean onSave() throws PawCareException {
        clearErrors();

        // Field-level problems are collected first so the user sees them all together,
        // exactly as the service-layer validation does.
        List<String> problems = new ArrayList<>();

        Integer age = parseInteger(ageField.getText(), "Age", problems);
        Double weight = parseDecimal(weightField.getText(), "Weight", problems);
        LocalDate registration = parseDate(registrationField.getText(), problems);

        Species species = (Species) speciesField.getSelectedItem();
        if (species == null) {
            problems.add("Species must be selected.");
            species = Species.DOG;
        }

        TrainingLevel training = (TrainingLevel) trainingField.getSelectedItem();
        IndoorOutdoor lifestyle = (IndoorOutdoor) lifestyleField.getSelectedItem();
        Double wingSpan = null;
        if (species == Species.BIRD) {
            wingSpan = parseDecimal(wingSpanField.getText(), "Wing span", problems);
        }

        Owner owner = (Owner) ownerField.getSelectedItem();
        if (owner == null) {
            problems.add("An owner must be selected. Register an owner first if the list is empty.");
        }

        if (!problems.isEmpty()) {
            throw new InvalidPetException("Unable to save pet.", problems);
        }

        String petId = existing == null ? IDGenerator.nextPetId() : existing.getAnimalId();
        MedicalInfo medical = new MedicalInfo(text(allergiesField), text(conditionsField),
                text(notesField));

        Pet pet = createPet(species, petId, age, weight, registration, owner.getOwnerId(),
                medical, training, lifestyle, wingSpan);

        if (existing == null) {
            clinic.addPet(pet);
            toast(pet.getName() + " registered as " + pet.getAnimalId());
        } else {
            clinic.updatePet(pet);
            toast(pet.getName() + " updated");
        }
        return true;
    }

    /**
     * Builds the right concrete subclass for the chosen species.
     *
     * <p>This is the only place in the user interface where a species is inspected to
     * decide which object to <i>create</i>. Everywhere else the application works through
     * the {@link Pet} / {@code Animal} reference and lets polymorphism decide.</p>
     */
    private Pet createPet(Species species, String petId, int age, double weight,
                          LocalDate registration, String ownerId, MedicalInfo medical,
                          TrainingLevel training, IndoorOutdoor lifestyle, Double wingSpan) {
        Gender gender = (Gender) genderField.getSelectedItem();
        String name = text(nameField);
        String breed = text(breedField);

        switch (species) {
            case CAT:
                return new Cat(petId, name, age, weight, gender, breed, ownerId, registration,
                        medical, lifestyle);
            case BIRD:
                return new Bird(petId, name, age, weight, gender, breed, ownerId, registration,
                        medical, wingSpan == null ? 0 : wingSpan);
            case DOG:
            default:
                return new Dog(petId, name, age, weight, gender, breed, ownerId, registration,
                        medical, training);
        }
    }

    // ------------------------------------------------------------------
    // Field parsing
    // ------------------------------------------------------------------

    private static Integer parseInteger(String raw, String fieldName, List<String> problems) {
        if (raw == null || raw.trim().isEmpty()) {
            problems.add(fieldName + " is required.");
            return null;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            problems.add(fieldName + " must be a whole number.");
            return null;
        }
    }

    private static Double parseDecimal(String raw, String fieldName, List<String> problems) {
        if (raw == null || raw.trim().isEmpty()) {
            problems.add(fieldName + " is required.");
            return null;
        }
        try {
            return Double.parseDouble(raw.trim());
        } catch (NumberFormatException e) {
            problems.add(fieldName + " must be a number, for example 12.5");
            return null;
        }
    }

    private static LocalDate parseDate(String raw, List<String> problems) {
        try {
            return DateUtil.parseDate(raw);
        } catch (java.time.format.DateTimeParseException e) {
            problems.add("Registration date must look like " + DateUtil.DISPLAY_DATE_PATTERN
                    + ", for example " + DateUtil.toEditable(LocalDate.now()) + ".");
            return null;
        }
    }
}
