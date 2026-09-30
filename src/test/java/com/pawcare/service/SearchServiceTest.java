package com.pawcare.service;

import com.pawcare.exception.InvalidOwnerException;
import com.pawcare.exception.InvalidPetException;
import com.pawcare.model.Owner;
import com.pawcare.model.Pet;
import com.pawcare.model.enums.Species;
import com.pawcare.support.TestData;
import com.pawcare.util.IDGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The universal search.
 *
 * <p>Searching touches three services at once – the pet list, the owner index and the
 * species enum – so most of these tests exist to pin down which field a given scope is
 * allowed to look at. A scope that quietly searched everything would let a user typing a
 * pet id in "Owner name" get a result, which is worse than no result at all.</p>
 */
class SearchServiceTest {

    private PetService pets;
    private OwnerService owners;
    private SearchService search;

    @BeforeEach
    void setUp() throws InvalidPetException, InvalidOwnerException {
        IDGenerator.reset();
        pets = new PetService();
        owners = new OwnerService();
        search = new SearchService(pets, owners);

        owners.addOwner(TestData.owner("O001", "Aarav Sharma"));
        owners.addOwner(TestData.owner("O002", "Diya Patel", "9812345678", "diya.patel@example.com"));

        pets.addPet(TestData.dog("P001", "O001", "Bruno"));
        pets.addPet(TestData.cat("P002", "O001", "Whiskers"));
        pets.addPet(TestData.bird("P003", "O002", "Kiwi"));
    }

    private List<String> names(List<Pet> results) {
        return results.stream().map(Pet::getName).toList();
    }

    // ------------------------------------------------------------------
    // Blank and default behaviour
    // ------------------------------------------------------------------

    @Test
    @DisplayName("an empty query returns every pet, sorted by name")
    void aBlankQueryReturnsEverybody() {
        assertEquals(List.of("Bruno", "Kiwi", "Whiskers"), names(search.search("", SearchService.Scope.ALL)));
        assertEquals(List.of("Bruno", "Kiwi", "Whiskers"), names(search.search("   ", null)));
        assertEquals(List.of("Bruno", "Kiwi", "Whiskers"), names(search.search(null, SearchService.Scope.PET_NAME)));
    }

    @Test
    @DisplayName("results come back sorted by name whatever the query was")
    void resultsAreSortedByName() {
        // Both remaining pets contain an "i" and they were added newest-name-last, so the
        // answer can only come out in this order if the results really are sorted.
        assertEquals(List.of("Kiwi", "Whiskers"), names(search.search("i", SearchService.Scope.ALL)));
    }

    // ------------------------------------------------------------------
    // Scopes
    // ------------------------------------------------------------------

    @Test
    @DisplayName("the all-fields scope matches a partial name, ignoring capitals")
    void allFieldsFindsAPartialName() {
        assertEquals(List.of("Bruno"), names(search.search("bru", SearchService.Scope.ALL)));
        assertEquals(List.of("Bruno"), names(search.search("BRUNO", SearchService.Scope.ALL)));
        // "isk" sits in the middle of "Whiskers", so this also proves the match is a
        // substring search and not just a prefix search.
        assertEquals(List.of("Whiskers"), names(search.search("isk", SearchService.Scope.ALL)));
    }

    @Test
    @DisplayName("the all-fields scope also searches the owner's name")
    void allFieldsFindsByOwnerName() {
        assertEquals(List.of("Bruno", "Whiskers"),
                names(search.search("Aarav", SearchService.Scope.ALL)));
    }

    @Test
    @DisplayName("a null scope behaves like the all-fields scope")
    void aNullScopeMeansAllFields() {
        assertEquals(names(search.search("Bruno", SearchService.Scope.ALL)),
                names(search.search("Bruno", null)));
    }

    @Test
    @DisplayName("the pet-name scope does not match an id or an owner")
    void petNameScopeIsRestricted() {
        assertEquals(List.of("Bruno"), names(search.search("Bruno", SearchService.Scope.PET_NAME)));
        assertTrue(search.search("P001", SearchService.Scope.PET_NAME).isEmpty());
        assertTrue(search.search("Aarav", SearchService.Scope.PET_NAME).isEmpty());
    }

    @Test
    @DisplayName("the pet-id scope matches the id only")
    void petIdScope() {
        assertEquals(List.of("Bruno"), names(search.search("P001", SearchService.Scope.PET_ID)));
        assertEquals(List.of("Kiwi"), names(search.search("003", SearchService.Scope.PET_ID)));
        assertTrue(search.search("Bruno", SearchService.Scope.PET_ID).isEmpty());
        assertTrue(search.search("labrador", SearchService.Scope.PET_ID).isEmpty());
    }

    @Test
    @DisplayName("the owner-name scope resolves the owner of each pet")
    void ownerNameScope() {
        assertEquals(List.of("Bruno", "Whiskers"),
                names(search.search("sharma", SearchService.Scope.OWNER_NAME)));
        assertEquals(List.of("Kiwi"), names(search.search("Diya", SearchService.Scope.OWNER_NAME)));
        assertTrue(search.search("Bruno", SearchService.Scope.OWNER_NAME).isEmpty());
    }

    @Test
    @DisplayName("a pet whose owner has been removed simply never matches by owner")
    void anOrphanedPetMatchesNoOwnerName() throws InvalidPetException {
        pets.addPet(TestData.dog("P009", "O999", "Stray"));

        assertTrue(search.search("Aarav", SearchService.Scope.OWNER_NAME).stream()
                .noneMatch(p -> p.getAnimalId().equals("P009")));
        // The all-fields scope must not match it by owner either.
        assertTrue(search.search("Aarav", SearchService.Scope.ALL).stream()
                .noneMatch(p -> p.getAnimalId().equals("P009")));
    }

    @Test
    @DisplayName("the species scope matches the species label, not the enum constant")
    void speciesScope() {
        assertEquals(List.of("Bruno"), names(search.search("dog", SearchService.Scope.SPECIES)));
        assertEquals(List.of("Whiskers"), names(search.search("cat", SearchService.Scope.SPECIES)));
        assertEquals(List.of("Kiwi"), names(search.search("bird", SearchService.Scope.SPECIES)));
        assertTrue(search.search("labrador", SearchService.Scope.SPECIES).isEmpty());
    }

    @Test
    @DisplayName("the breed scope matches the breed only")
    void breedScope() {
        assertEquals(List.of("Bruno"), names(search.search("labrador", SearchService.Scope.BREED)));
        assertEquals(List.of("Whiskers"), names(search.search("persian", SearchService.Scope.BREED)));
        assertTrue(search.search("Bruno", SearchService.Scope.BREED).isEmpty());
    }

    @Test
    @DisplayName("every scope has a label for the drop-down")
    void scopeLabels() {
        assertEquals("All fields", SearchService.Scope.ALL.getLabel());
        assertEquals("Pet name", SearchService.Scope.PET_NAME.getLabel());
        assertEquals("Pet ID", SearchService.Scope.PET_ID.getLabel());
        assertEquals("Owner name", SearchService.Scope.OWNER_NAME.getLabel());
        assertEquals("Species", SearchService.Scope.SPECIES.getLabel());
        assertEquals("Breed", SearchService.Scope.BREED.getLabel());
        assertEquals("All fields", SearchService.Scope.ALL.toString());
    }

    @Test
    @DisplayName("a query that matches nothing returns an empty list, not an error")
    void noMatches() {
        assertTrue(search.search("zzzz", SearchService.Scope.ALL).isEmpty());
        assertTrue(search.search("zzzz", SearchService.Scope.BREED).isEmpty());
    }

    @Test
    @DisplayName("an empty pet list searches cleanly")
    void anEmptyClinic() {
        pets.loadAll(List.of());
        assertTrue(search.search("Bruno", SearchService.Scope.ALL).isEmpty());
        assertTrue(search.search("", SearchService.Scope.ALL).isEmpty());
    }

    // ------------------------------------------------------------------
    // Owner search
    // ------------------------------------------------------------------

    @Test
    @DisplayName("an empty owner query returns everybody, sorted by name")
    void ownerSearchWithNoQuery() {
        assertEquals(List.of("Aarav Sharma", "Diya Patel"),
                search.searchOwners("").stream().map(Owner::getName).toList());
        assertEquals(2, search.searchOwners(null).size());
    }

    @Test
    @DisplayName("owners can be found by name, id, phone or email")
    void ownerSearchLooksAtEveryContactField() {
        assertEquals(List.of("Aarav Sharma"),
                search.searchOwners("aarav").stream().map(Owner::getName).toList());
        assertEquals(List.of("Diya Patel"),
                search.searchOwners("O002").stream().map(Owner::getName).toList());
        assertEquals(List.of("Diya Patel"),
                search.searchOwners("9812345678").stream().map(Owner::getName).toList());
        assertEquals(List.of("Diya Patel"),
                search.searchOwners("diya.patel@").stream().map(Owner::getName).toList());
        assertTrue(search.searchOwners("nobody").isEmpty());
    }

    @Test
    @DisplayName("owner results are sorted by name even when the match order differs")
    void ownerResultsAreSorted() {
        List<String> found = search.searchOwners("a").stream().map(Owner::getName).toList();
        assertEquals(found.stream().sorted(String.CASE_INSENSITIVE_ORDER).toList(), found);
    }

    // ------------------------------------------------------------------
    // Small helpers used by the pages
    // ------------------------------------------------------------------

    @Test
    @DisplayName("describe names a pet and its species")
    void describe() {
        assertEquals("Bruno (Dog)", SearchService.describe(pets.findPetById("P001").orElseThrow()));
        assertEquals("Kiwi (Bird)", SearchService.describe(pets.findPetById("P003").orElseThrow()));
    }

    @Test
    @DisplayName("countSpecies counts the results of one species")
    void countSpecies() {
        List<Pet> results = pets.getAllPets();

        assertEquals(1L, SearchService.countSpecies(results, Species.DOG));
        assertEquals(1L, SearchService.countSpecies(results, Species.CAT));
        assertEquals(1L, SearchService.countSpecies(results, Species.BIRD));

        pets.deletePet("P001");
        assertEquals(0L, SearchService.countSpecies(pets.getAllPets(), Species.DOG));
    }
}
