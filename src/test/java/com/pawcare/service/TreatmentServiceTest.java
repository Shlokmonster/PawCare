package com.pawcare.service;

import com.pawcare.exception.InvalidTreatmentException;
import com.pawcare.model.Treatment;
import com.pawcare.support.TestData;
import com.pawcare.util.IDGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.LinkedList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The per-pet treatment log, held as a {@code HashMap<String, LinkedList<Treatment>>}.
 *
 * <p>The behaviour worth testing is the ordering: entries are appended in the order they are
 * entered, but the history is handed back in chronological order, because a medical record
 * that depends on the order somebody typed it in would be useless.</p>
 */
class TreatmentServiceTest {

    private static final LocalDate TODAY = LocalDate.now();

    private TreatmentService treatments;

    @BeforeEach
    void setUp() {
        IDGenerator.reset();
        treatments = new TreatmentService();
    }

    private static Treatment entry(String id, String petId, LocalDate date) {
        return TestData.treatment(id, petId, "V001", date);
    }

    // ------------------------------------------------------------------
    // CREATE
    // ------------------------------------------------------------------

    @Test
    @DisplayName("a valid treatment is stored and can be found by id")
    void addAndFind() throws InvalidTreatmentException {
        treatments.addTreatment(entry("T001", "P001", TODAY.minusDays(3)));

        assertEquals(1, treatments.count());
        assertTrue(treatments.findById("T001").isPresent());
        assertEquals(1, treatments.countForPet("P001"));
    }

    @Test
    @DisplayName("an invalid treatment is rejected and nothing is stored")
    void anInvalidTreatmentIsNotStored() {
        Treatment broken = entry("T001", "P001", TODAY.minusDays(1));
        broken.setDiagnosis("  ");

        assertThrows(InvalidTreatmentException.class, () -> treatments.addTreatment(broken));
        assertEquals(0, treatments.count());
        assertEquals(0, treatments.countForPet("P001"));
    }

    @Test
    @DisplayName("a treatment cannot be dated in the future")
    void aFutureTreatmentIsRefused() {
        assertThrows(InvalidTreatmentException.class,
                () -> treatments.addTreatment(entry("T001", "P001", TODAY.plusDays(1))));
        assertEquals(0, treatments.count());
    }

    @Test
    @DisplayName("a duplicate treatment id is refused")
    void duplicateIdsAreRefused() throws InvalidTreatmentException {
        treatments.addTreatment(entry("T001", "P001", TODAY.minusDays(3)));

        assertThrows(InvalidTreatmentException.class,
                () -> treatments.addTreatment(entry("T001", "P002", TODAY.minusDays(2))));
        assertEquals(1, treatments.count());
    }

    @Test
    @DisplayName("two pets keep two separate histories")
    void historiesAreKeptPerPet() throws InvalidTreatmentException {
        treatments.addTreatment(entry("T001", "P001", TODAY.minusDays(30)));
        treatments.addTreatment(entry("T002", "P001", TODAY.minusDays(10)));
        treatments.addTreatment(entry("T003", "P002", TODAY.minusDays(5)));

        assertEquals(2, treatments.countForPet("P001"));
        assertEquals(1, treatments.countForPet("P002"));
        assertEquals(3, treatments.count());
    }

    // ------------------------------------------------------------------
    // READ
    // ------------------------------------------------------------------

    @Test
    @DisplayName("a pet's history comes back in chronological order, not entry order")
    void historyIsChronological() throws InvalidTreatmentException {
        // Entered newest first on purpose.
        treatments.addTreatment(entry("T003", "P001", TODAY.minusDays(2)));
        treatments.addTreatment(entry("T001", "P001", TODAY.minusDays(90)));
        treatments.addTreatment(entry("T002", "P001", TODAY.minusDays(40)));

        LinkedList<Treatment> history = treatments.getTreatmentHistory("P001");

        assertEquals(List.of("T001", "T002", "T003"),
                history.stream().map(Treatment::getTreatmentId).toList());
    }

    @Test
    @DisplayName("the history really is a LinkedList")
    void theHistoryIsALinkedList() throws InvalidTreatmentException {
        treatments.addTreatment(entry("T001", "P001", TODAY.minusDays(1)));

        Object history = treatments.getTreatmentHistory("P001");
        assertInstanceOf(LinkedList.class, history);
    }

    @Test
    @DisplayName("the returned history is a copy, so a caller cannot alter the log")
    void theHistoryIsDefensive() throws InvalidTreatmentException {
        treatments.addTreatment(entry("T001", "P001", TODAY.minusDays(1)));

        LinkedList<Treatment> history = treatments.getTreatmentHistory("P001");
        history.clear();

        assertEquals(1, treatments.countForPet("P001"));
    }

    @Test
    @DisplayName("a pet with no history reports an empty list, not a null")
    void anUnknownPetHasAnEmptyHistory() {
        assertTrue(treatments.getTreatmentHistory("P999").isEmpty());
        assertEquals(0, treatments.countForPet("P999"));
        assertTrue(treatments.findById("T999").isEmpty());
        assertTrue(treatments.findById(null).isEmpty());
    }

    @Test
    @DisplayName("the clinic-wide list is newest first")
    void allNewestFirst() throws InvalidTreatmentException {
        treatments.addTreatment(entry("T001", "P001", TODAY.minusDays(60)));
        treatments.addTreatment(entry("T002", "P002", TODAY.minusDays(1)));
        treatments.addTreatment(entry("T003", "P003", TODAY.minusDays(20)));

        assertEquals(List.of("T002", "T003", "T001"),
                treatments.allNewestFirst().stream().map(Treatment::getTreatmentId).toList());
        assertEquals(List.of("T002", "T003"),
                treatments.recent(2).stream().map(Treatment::getTreatmentId).toList());
    }

    @Test
    @DisplayName("the recent count only includes entries inside the window")
    void countRecent() throws InvalidTreatmentException {
        treatments.addTreatment(entry("T001", "P001", TODAY.minusDays(2)));
        treatments.addTreatment(entry("T002", "P001", TODAY.minusDays(10)));
        treatments.addTreatment(entry("T003", "P001", TODAY.minusDays(90)));

        assertEquals(3, treatments.countRecent(365));
        assertEquals(2, treatments.countRecent(30));
        assertEquals(0, treatments.countRecent(0));
    }

    @Test
    @DisplayName("the underlying map is keyed by pet id")
    void theUnderlyingIndexIsKeyedByPet() throws InvalidTreatmentException {
        treatments.addTreatment(entry("T001", "P001", TODAY.minusDays(1)));
        treatments.addTreatment(entry("T002", "P002", TODAY.minusDays(1)));

        assertEquals(2, treatments.historyByPet().size());
        assertEquals(1, treatments.historyByPet().get("P001").size());
    }

    // ------------------------------------------------------------------
    // DELETE
    // ------------------------------------------------------------------

    @Test
    @DisplayName("removing an entry updates the history and the id index")
    void removeUpdatesBothStructures() throws InvalidTreatmentException {
        treatments.addTreatment(entry("T001", "P001", TODAY.minusDays(30)));
        treatments.addTreatment(entry("T002", "P001", TODAY.minusDays(10)));

        assertTrue(treatments.removeTreatment("T001"));

        assertEquals(1, treatments.count());
        assertEquals(1, treatments.countForPet("P001"));
        assertTrue(treatments.findById("T001").isEmpty());
        assertEquals("T002", treatments.getTreatmentHistory("P001").get(0).getTreatmentId());
    }

    @Test
    @DisplayName("removing something that is not there reports that nothing happened")
    void removeReportsWhetherItDidAnything() {
        assertFalse(treatments.removeTreatment("T999"));
    }

    @Test
    @DisplayName("removing a pet's last entry cleans up the empty list")
    void removingTheLastEntryRemovesTheEmptyHistory() throws InvalidTreatmentException {
        treatments.addTreatment(entry("T001", "P001", TODAY.minusDays(1)));

        treatments.removeTreatment("T001");

        assertFalse(treatments.historyByPet().containsKey("P001"),
                "an empty history was left behind in the map");
    }

    @Test
    @DisplayName("removing a whole pet reports how many entries went with it")
    void removeByPet() throws InvalidTreatmentException {
        treatments.addTreatment(entry("T001", "P001", TODAY.minusDays(30)));
        treatments.addTreatment(entry("T002", "P001", TODAY.minusDays(10)));
        treatments.addTreatment(entry("T003", "P002", TODAY.minusDays(1)));

        assertEquals(2, treatments.removeByPet("P001"));

        assertEquals(1, treatments.count());
        assertEquals(0, treatments.countForPet("P001"));
        assertEquals(0, treatments.removeByPet("P001"));
        assertEquals(0, treatments.removeByPet("P999"));
    }

    // ------------------------------------------------------------------
    // Bulk loading
    // ------------------------------------------------------------------

    @Test
    @DisplayName("loadAll rebuilds the histories from a flat list")
    void loadAllRebuildsEverything() throws InvalidTreatmentException {
        treatments.addTreatment(entry("T001", "P001", TODAY.minusDays(1)));

        treatments.loadAll(List.of(
                entry("T009", "P005", TODAY.minusDays(4)),
                entry("T010", "P005", TODAY.minusDays(2)),
                entry("T011", "P006", TODAY.minusDays(3))));

        assertEquals(3, treatments.count());
        assertTrue(treatments.findById("T001").isEmpty());
        assertEquals(2, treatments.countForPet("P005"));
        // The loaded entries are ordered oldest first even though the list was flat.
        assertEquals(List.of("T009", "T010"),
                treatments.getTreatmentHistory("P005").stream()
                        .map(Treatment::getTreatmentId).toList());
    }

    @Test
    @DisplayName("loadAll skips records that could not be filed under a pet")
    void loadAllSkipsBrokenRecords() {
        Treatment noPet = entry("T001", null, TODAY.minusDays(1));
        treatments.loadAll(List.of(noPet, entry("T002", "P001", TODAY.minusDays(2))));

        assertEquals(1, treatments.count());
        assertTrue(treatments.findById("T002").isPresent());
    }

    @Test
    @DisplayName("loadAll tolerates a null list")
    void loadAllAcceptsNull() {
        treatments.loadAll(null);
        assertEquals(0, treatments.count());
        assertTrue(treatments.historyByPet().isEmpty());
    }

    @Test
    @DisplayName("loading seeds the id generator past the highest stored id")
    void loadAllSyncsTheIdGenerator() {
        treatments.loadAll(List.of(entry("T014", "P001", TODAY.minusDays(1))));
        assertEquals("T015", IDGenerator.nextTreatmentId());
    }
}
