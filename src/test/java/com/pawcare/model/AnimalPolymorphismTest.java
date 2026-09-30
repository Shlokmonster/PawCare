package com.pawcare.model;

import com.pawcare.model.enums.Species;
import com.pawcare.model.enums.TrainingLevel;
import com.pawcare.support.TestData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The object-oriented core of the project: the {@code Animal -> Pet -> Dog/Cat/Bird}
 * hierarchy.
 *
 * <p>These tests are written the way the application uses the classes – through a variable of
 * the superclass type – because that is what polymorphism means. Nothing here asks
 * "is this a dog?"; the JVM picks the right method by itself, and the tests prove it.</p>
 */
class AnimalPolymorphismTest {

    /** The same objects seen through the superclass, which is how the service layer holds them. */
    private static List<Animal> patients() {
        return new ArrayList<>(List.of(
                TestData.dog("P001", "O001", "Bruno"),
                TestData.cat("P002", "O002", "Whiskers"),
                TestData.bird("P003", "O003", "Kiwi")));
    }

    // ------------------------------------------------------------------
    // Runtime polymorphism
    // ------------------------------------------------------------------

    @Test
    @DisplayName("treatmentPlan() returns a different plan for each concrete species")
    void eachSpeciesHasItsOwnPlan() {
        List<Animal> animals = patients();

        String dog = animals.get(0).treatmentPlan();
        String cat = animals.get(1).treatmentPlan();
        String bird = animals.get(2).treatmentPlan();

        assertTrue(dog.startsWith("Canine"));
        assertTrue(cat.startsWith("Feline"));
        assertTrue(bird.startsWith("Avian"));

        assertNotEquals(dog, cat);
        assertNotEquals(cat, bird);
        assertNotEquals(dog, bird);
    }

    @Test
    @DisplayName("getSpecies() is resolved from the object, not from the variable's type")
    void speciesComesFromTheObject() {
        List<Animal> animals = patients();
        assertEquals(Species.DOG, animals.get(0).getSpecies());
        assertEquals(Species.CAT, animals.get(1).getSpecies());
        assertEquals(Species.BIRD, animals.get(2).getSpecies());
    }

    @Test
    @DisplayName("an abstract superclass can still be used as the variable type")
    void theSuperclassTypeWorksForEverySpecies() {
        // Animal cannot be instantiated, which is exactly what makes the loop above safe:
        // every entry really is one of the three concrete species.
        int plans = 0;
        for (Animal animal : patients()) {
            assertFalse(animal.treatmentPlan().isEmpty());
            assertTrue(animal instanceof Animal);
            assertTrue(animal instanceof Pet);
            plans++;
        }
        assertEquals(3, plans);
    }

    @Test
    @DisplayName("each animal reports the plan belonging to its own species")
    void theRightPlanIsChosenWithoutAnIf() {
        // The anti-pattern this project avoids would look like:
        //     if (animal.getSpecies() == Species.DOG) { plan = dogPlan; } else if (...) { ... }
        // The call below needs no such branch: the JVM dispatches to the subclass itself.
        List<Animal> animals = patients();
        assertTrue(animals.get(0).treatmentPlan().startsWith("Canine"));
        assertTrue(animals.get(1).treatmentPlan().startsWith("Feline"));
        assertTrue(animals.get(2).treatmentPlan().startsWith("Avian"));
    }

    // ------------------------------------------------------------------
    // Overriding
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Pet.displayInfo() extends the version inherited from Animal")
    void displayInfoIsOverriddenAndExtended() {
        Dog bruno = TestData.dog("P001", "O001", "Bruno");

        assertEquals("Bruno • Dog • 3 yr • 12.5 kg • Labrador • Owner O001", bruno.displayInfo());
        // The Animal part is still there, only with the owner appended.
        assertTrue(bruno.displayInfo().startsWith("Bruno • Dog • 3 yr • 12.5 kg • Labrador"));
        assertTrue(bruno.displayInfo().endsWith("Owner O001"));
        assertTrue(bruno.displayInfo().contains("Owner O001"));
    }

    @Test
    @DisplayName("a pet with no owner says so instead of printing null")
    void anUnassignedOwnerIsDescribedInWords() {
        Dog stray = TestData.dog("P001", null, "Bruno");
        assertTrue(stray.displayInfo().endsWith("Owner unassigned"));
        assertFalse(stray.displayInfo().contains("null"));
    }

    @Test
    @DisplayName("toString() follows displayInfo()")
    void toStringUsesDisplayInfo() {
        Dog bruno = TestData.dog("P001", "O001", "Bruno");
        assertEquals(bruno.displayInfo(), bruno.toString());
    }

    @Test
    @DisplayName("speciesDetail() describes the attribute each species adds")
    void speciesDetailIsSpeciesSpecific() {
        assertEquals("Training level: Intermediate",
                TestData.dog("P001", "O001", "Bruno").speciesDetail());
        assertEquals("Lifestyle: Indoor",
                TestData.cat("P002", "O002", "Whiskers").speciesDetail());
        assertEquals("Wing span: 24.0 cm",
                TestData.bird("P003", "O003", "Kiwi").speciesDetail());
    }

    @Test
    @DisplayName("a subclass attribute can be changed and is reflected in the detail line")
    void subclassAttributesAreMutable() {
        Dog bruno = TestData.dog("P001", "O001", "Bruno");
        bruno.setTrainingLevel(TrainingLevel.ADVANCED);
        assertEquals("Training level: Advanced", bruno.speciesDetail());
        assertEquals(TrainingLevel.ADVANCED, bruno.getTrainingLevel());
    }

    // ------------------------------------------------------------------
    // Equality
    // ------------------------------------------------------------------

    @Test
    @DisplayName("two records with the same id are the same record")
    void equalityUsesTheId() {
        Dog first = TestData.dog("P001", "O001", "Bruno");
        Dog second = TestData.dog("P001", "O001", "Bruno");
        first.setName("Bruno the Second");     // a rename must not create a new pet

        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
    }

    @Test
    @DisplayName("different ids mean different animals")
    void differentIdsAreDifferentAnimals() {
        Dog bruno = TestData.dog("P001", "O001", "Bruno");
        Dog other = TestData.dog("P002", "O001", "Bruno");
        assertNotEquals(bruno, other);
    }

    @Test
    @DisplayName("the id alone decides identity, even across species")
    void identityIgnoresTheSpecies() {
        // Slightly odd, but it is what lets a HashMap<String, Pet> be indexed by id and
        // still behave predictably when a record is edited.
        Dog asDog = TestData.dog("P001", "O001", "Bruno");
        Cat asCat = TestData.cat("P001", "O001", "Bruno");
        assertEquals(asDog, asCat);
    }

    @Test
    @DisplayName("a pet never equals a non-animal")
    void equalsHandlesForeignTypes() {
        Dog bruno = TestData.dog("P001", "O001", "Bruno");
        assertNotEquals(bruno, null);
        assertNotEquals(bruno, "P001");
    }

    // ------------------------------------------------------------------
    // Ordering
    // ------------------------------------------------------------------

    @Test
    @DisplayName("treatments sort chronologically through Comparable")
    void treatmentsSortByDate() {
        List<Treatment> history = new ArrayList<>(List.of(
                TestData.treatment("T003", "P001", "V001", LocalDate.of(2026, 5, 20)),
                TestData.treatment("T001", "P001", "V001", LocalDate.of(2024, 1, 10)),
                TestData.treatment("T002", "P001", "V001", LocalDate.of(2025, 8, 2))));

        Collections.sort(history);   // uses compareTo -> BY_DATE

        assertEquals(List.of("T001", "T002", "T003"),
                history.stream().map(Treatment::getTreatmentId).toList());
    }

    @Test
    @DisplayName("two treatments on the same day are ordered by id, so the order is stable")
    void sameDayTreatmentsAreOrderedById() {
        Treatment first = TestData.treatment("T001", "P001", "V001", LocalDate.of(2026, 1, 1));
        Treatment second = TestData.treatment("T002", "P001", "V001", LocalDate.of(2026, 1, 1));

        assertTrue(first.compareTo(second) < 0);
        assertTrue(second.compareTo(first) > 0);
        assertTrue(first.compareTo(first) == 0);
    }

    @Test
    @DisplayName("vaccinations sort by the next-due date, with undated records last")
    void vaccinationsSortByNextDue() {
        Vaccination undated = TestData.vaccination("VAC000", "P001", "V001", "Rabies",
                LocalDate.now(), null);
        List<Vaccination> doses = new ArrayList<>(List.of(
                TestData.vaccinationDue("VAC002", "P001", LocalDate.of(2026, 12, 1)),
                undated,
                TestData.vaccinationDue("VAC001", "P001", LocalDate.of(2026, 10, 1))));

        doses.sort(Vaccination.BY_NEXT_DUE);

        assertEquals(List.of("VAC001", "VAC002", "VAC000"),
                doses.stream().map(Vaccination::getVaccinationId).toList());
        assertSame(undated, doses.get(2));
    }
}
