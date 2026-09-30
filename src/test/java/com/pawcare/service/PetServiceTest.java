package com.pawcare.service;

import com.pawcare.exception.EntityNotFoundException;
import com.pawcare.exception.InvalidPetException;
import com.pawcare.model.Dog;
import com.pawcare.model.Pet;
import com.pawcare.model.enums.Species;
import com.pawcare.support.TestData;
import com.pawcare.util.IDGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * CRUD, searching, sorting and the id index of the pet store.
 *
 * <p>The interesting cases are the ones that keep the {@code ArrayList} and the
 * {@code HashMap} in step: if a delete removed a pet from the list but left it in the map,
 * {@code findPetById} would keep returning a pet the table no longer shows.</p>
 */
class PetServiceTest {

    private PetService pets;

    @BeforeEach
    void setUp() {
        IDGenerator.reset();
        pets = new PetService();
    }

    private static Dog dog(String id, String ownerId, String name) {
        return TestData.dog(id, ownerId, name);
    }

    // ------------------------------------------------------------------
    // CREATE
    // ------------------------------------------------------------------

    @Test
    @DisplayName("a valid pet is stored and can be found by id")
    void addAndFind() throws InvalidPetException {
        pets.addPet(dog("P001", "O001", "Bruno"));

        assertEquals(1, pets.count());
        assertTrue(pets.findPetById("P001").isPresent());
        assertEquals("Bruno", pets.findPetById("P001").orElseThrow().getName());
    }

    @Test
    @DisplayName("the HashMap index answers a lookup for an id that was never stored")
    void unknownIdIsEmpty() {
        assertTrue(pets.findPetById("P999").isEmpty());
        assertTrue(pets.findPetById(null).isEmpty());
    }

    @Test
    @DisplayName("surrounding spaces in a typed id do not break the lookup")
    void lookupToleratesSpaces() throws InvalidPetException {
        pets.addPet(dog("P001", "O001", "Bruno"));
        assertTrue(pets.findPetById("  P001 ").isPresent());
        assertTrue(pets.exists("P001"));
        assertTrue(pets.exists(" P001 "));
    }

    @Test
    @DisplayName("an invalid pet is rejected and never reaches the store")
    void anInvalidPetIsNotStored() {
        Dog nameless = dog("P001", "O001", "");

        assertThrows(InvalidPetException.class, () -> pets.addPet(nameless));
        // The important half of the test: nothing was half-added.
        assertEquals(0, pets.count());
        assertFalse(pets.exists("P001"));
    }

    @Test
    @DisplayName("a duplicate id is refused")
    void duplicateIdsAreRefused() throws InvalidPetException {
        pets.addPet(dog("P001", "O001", "Bruno"));

        InvalidPetException failure = assertThrows(InvalidPetException.class,
                () -> pets.addPet(dog("P001", "O001", "Bruno the Second")));
        assertTrue(failure.getDetails().get(0).contains("already in use"));
        assertEquals(1, pets.count());
    }

    @Test
    @DisplayName("adding a pet moves the id generator past it")
    void addingSyncsTheIdGenerator() throws InvalidPetException {
        pets.addPet(dog("P007", "O001", "Bruno"));
        assertEquals("P008", IDGenerator.nextPetId());
    }

    // ------------------------------------------------------------------
    // READ
    // ------------------------------------------------------------------

    @Test
    @DisplayName("an unknown id gives a friendly name rather than an exception")
    void petNameFallsBack() throws InvalidPetException {
        pets.addPet(dog("P001", "O001", "Bruno"));
        assertEquals("Bruno", pets.petName("P001"));
        assertEquals("Unknown pet", pets.petName("P999"));
    }

    @Test
    @DisplayName("requirePet throws when the record is missing")
    void requirePetThrows() throws InvalidPetException, EntityNotFoundException {
        pets.addPet(dog("P001", "O001", "Bruno"));

        assertEquals("Bruno", pets.requirePet("P001").getName());
        assertThrows(EntityNotFoundException.class, () -> pets.requirePet("P999"));
    }

    @Test
    @DisplayName("getAllPets hands out a copy, so a caller cannot alter the store")
    void getAllPetsIsDefensive() throws InvalidPetException {
        pets.addPet(dog("P001", "O001", "Bruno"));

        List<Pet> copy = pets.getAllPets();
        copy.clear();

        assertEquals(1, pets.count());
    }

    @Test
    @DisplayName("pets can be found by their owner")
    void findByOwner() throws InvalidPetException {
        pets.addPet(dog("P001", "O001", "Bruno"));
        pets.addPet(dog("P002", "O001", "Rex"));
        pets.addPet(dog("P003", "O002", "Simba"));

        assertEquals(2, pets.findByOwner("O001").size());
        assertEquals(1, pets.findByOwner("O002").size());
        assertTrue(pets.findByOwner("O999").isEmpty());
        assertTrue(pets.findByOwner(null).isEmpty());
    }

    @Test
    @DisplayName("pets can be found by species, and the species comes from the class")
    void findBySpecies() throws InvalidPetException {
        pets.addPet(dog("P001", "O001", "Bruno"));
        pets.addPet(TestData.cat("P002", "O001", "Whiskers"));
        pets.addPet(TestData.bird("P003", "O002", "Kiwi"));

        assertEquals(1, pets.findBySpecies(Species.DOG).size());
        assertEquals("Bruno", pets.findBySpecies(Species.DOG).get(0).getName());
        assertEquals(1, pets.findBySpecies(Species.CAT).size());
        assertEquals(1, pets.findBySpecies(Species.BIRD).size());
    }

    @Test
    @DisplayName("the species census counts every pet and always has all three keys")
    void countBySpecies() throws InvalidPetException {
        pets.addPet(dog("P001", "O001", "Bruno"));
        pets.addPet(dog("P002", "O001", "Rex"));
        pets.addPet(TestData.cat("P003", "O002", "Whiskers"));

        Map<Species, Integer> counts = pets.countBySpecies();

        assertEquals(3, counts.size());
        assertEquals(2, counts.get(Species.DOG).intValue());
        assertEquals(1, counts.get(Species.CAT).intValue());
        // A species nobody owns yet still reports zero, so the chart has no gaps.
        assertEquals(0, counts.get(Species.BIRD).intValue());
    }

    // ------------------------------------------------------------------
    // UPDATE
    // ------------------------------------------------------------------

    @Test
    @DisplayName("updating replaces the record in both structures")
    void updateReplacesTheRecord() throws InvalidPetException {
        pets.addPet(dog("P001", "O001", "Bruno"));

        Dog renamed = dog("P001", "O001", "Bruno");
        renamed.setName("Bruno Junior");
        renamed.setAge(5);
        pets.updatePet(renamed);

        assertEquals(1, pets.count());
        assertEquals("Bruno Junior", pets.findPetById("P001").orElseThrow().getName());
        assertEquals(5, pets.findPetById("P001").orElseThrow().getAge());
    }

    @Test
    @DisplayName("updating a pet that does not exist is an error")
    void updateRejectsAnUnknownId() {
        Dog stranger = dog("P404", "O001", "Ghost");
        InvalidPetException failure = assertThrows(InvalidPetException.class,
                () -> pets.updatePet(stranger));
        assertTrue(failure.getDetails().get(0).contains("does not exist"));
    }

    @Test
    @DisplayName("an invalid update is refused and leaves the original in place")
    void anInvalidUpdateChangesNothing() throws InvalidPetException {
        pets.addPet(dog("P001", "O001", "Bruno"));

        Dog broken = dog("P001", "O001", "Bruno");
        broken.setWeight(-2);

        assertThrows(InvalidPetException.class, () -> pets.updatePet(broken));
        assertEquals("Bruno", pets.findPetById("P001").orElseThrow().getName());
        assertTrue(pets.findPetById("P001").orElseThrow().getWeight() > 0);
    }

    // ------------------------------------------------------------------
    // DELETE
    // ------------------------------------------------------------------

    @Test
    @DisplayName("deleting removes the pet from the list and from the index")
    void deleteClearsBothStructures() throws InvalidPetException {
        pets.addPet(dog("P001", "O001", "Bruno"));
        pets.addPet(dog("P002", "O001", "Rex"));

        assertTrue(pets.deletePet("P001"));

        assertEquals(1, pets.count());
        assertFalse(pets.exists("P001"), "the HashMap index still knows the deleted pet");
        assertTrue(pets.findPetById("P001").isEmpty());
        assertEquals("Rex", pets.getAllPets().get(0).getName());
    }

    @Test
    @DisplayName("deleting something that is not there reports that nothing happened")
    void deleteReportsWhetherItDidAnything() {
        assertFalse(pets.deletePet("P999"));
        assertFalse(pets.deletePet(null));
    }

    @Test
    @DisplayName("a deleted id can be used again")
    void anIdIsFreeAfterDeletion() throws InvalidPetException {
        pets.addPet(dog("P001", "O001", "Bruno"));
        pets.deletePet("P001");
        pets.addPet(dog("P001", "O001", "Someone Else"));
        assertEquals("Someone Else", pets.findPetById("P001").orElseThrow().getName());
    }

    // ------------------------------------------------------------------
    // SORTING
    // ------------------------------------------------------------------

    @Test
    @DisplayName("sorting by name ignores capitals")
    void sortedByName() throws InvalidPetException {
        pets.addPet(dog("P001", "O001", "bruno"));
        pets.addPet(dog("P002", "O001", "Alpha"));
        pets.addPet(dog("P003", "O001", "zeus"));

        assertEquals(List.of("Alpha", "bruno", "zeus"),
                pets.sortedByName().stream().map(Pet::getName).toList());
    }

    @Test
    @DisplayName("sorting by age puts the oldest animal first")
    void sortedByAgeDescending() throws InvalidPetException {
        Dog youngest = dog("P001", "O001", "Puppy");
        youngest.setAge(1);
        Dog oldest = dog("P002", "O001", "Elder");
        oldest.setAge(14);
        Dog middle = dog("P003", "O001", "Middle");
        middle.setAge(6);

        pets.addPet(youngest);
        pets.addPet(oldest);
        pets.addPet(middle);

        assertEquals(List.of("Elder", "Middle", "Puppy"),
                pets.sortedByAgeDescending().stream().map(Pet::getName).toList());
    }

    @Test
    @DisplayName("a custom comparator can sort by any key")
    void sortedByAnArbitraryComparator() throws InvalidPetException {
        pets.addPet(dog("P003", "O001", "Bruno"));
        pets.addPet(dog("P001", "O001", "Rex"));
        pets.addPet(dog("P002", "O001", "Simba"));

        assertEquals(List.of("P001", "P002", "P003"),
                pets.sortedBy(Comparator.comparing(Pet::getAnimalId))
                        .stream().map(Pet::getAnimalId).toList());
    }

    @Test
    @DisplayName("sorting an empty store is not an error")
    void sortingNothing() {
        assertTrue(pets.sortedByName().isEmpty());
        assertTrue(pets.sortedByAgeDescending().isEmpty());
    }

    // ------------------------------------------------------------------
    // Bulk loading
    // ------------------------------------------------------------------

    @Test
    @DisplayName("loadAll replaces the whole dataset and rebuilds the index")
    void loadAllRebuildsTheIndex() throws InvalidPetException {
        pets.addPet(dog("P001", "O001", "Bruno"));

        Pet loaded = dog("P009", "O002", "Loaded");
        pets.loadAll(List.of(loaded));

        assertEquals(1, pets.count());
        assertFalse(pets.exists("P001"), "the old record survived the reload");
        assertSame(loaded, pets.findPetById("P009").orElseThrow());
    }

    @Test
    @DisplayName("loadAll skips records with no id instead of storing a broken row")
    void loadAllSkipsBrokenRecords() throws InvalidPetException {
        Pet noId = dog("P001", "O001", "Bruno");
        noId.setAnimalId(null);
        pets.loadAll(List.of(dog("P002", "O001", "Fine"), noId));

        assertEquals(1, pets.count());
        assertTrue(pets.exists("P002"));
    }

    @Test
    @DisplayName("loadAll tolerates a null list, which is what a first run produces")
    void loadAllAcceptsNull() {
        pets.loadAll(null);
        assertEquals(0, pets.count());
    }

    @Test
    @DisplayName("loading data seeds the id generator past the highest stored id")
    void loadAllSyncsTheIdGenerator() {
        pets.loadAll(List.of(dog("P042", "O001", "Bruno"), dog("P007", "O001", "Rex")));
        assertEquals("P043", IDGenerator.nextPetId());
    }

    @Test
    @DisplayName("a pet keeps its concrete type through the store")
    void theConcreteTypeSurvivesStorage() throws InvalidPetException {
        pets.addPet(TestData.cat("P001", "O001", "Whiskers"));

        Pet found = pets.findPetById("P001").orElseThrow();
        assertTrue(found instanceof com.pawcare.model.Cat);
        assertEquals(Species.CAT, found.getSpecies());
        assertTrue(found.treatmentPlan().startsWith("Feline"));
    }

    @Test
    @DisplayName("a registration date cannot be in the future")
    void aFutureRegistrationIsRefused() {
        Dog pet = dog("P001", "O001", "Bruno");
        pet.setRegistrationDate(LocalDate.now().plusDays(1));
        assertThrows(InvalidPetException.class, () -> pets.addPet(pet));
        assertEquals(0, pets.count());
    }
}
