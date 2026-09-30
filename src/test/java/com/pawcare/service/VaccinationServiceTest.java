package com.pawcare.service;

import com.pawcare.exception.InvalidVaccinationException;
import com.pawcare.model.Vaccination;
import com.pawcare.model.enums.PetHealthStatus;
import com.pawcare.model.enums.VaccinationStatus;
import com.pawcare.support.TestData;
import com.pawcare.util.AppSettings;
import com.pawcare.util.IDGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Vaccination records and the reminders derived from them.
 *
 * <p>The reminder window is read live from {@link AppSettings}, which is the point of the
 * last section: widening the window on the Settings page has to reclassify records that are
 * already stored, without anything being reloaded.</p>
 */
class VaccinationServiceTest {

    private static final LocalDate TODAY = LocalDate.now();

    @TempDir
    Path tempDir;

    private AppSettings settings;
    private VaccinationService vaccinations;

    @BeforeEach
    void setUp() {
        IDGenerator.reset();
        settings = new AppSettings(tempDir.resolve("settings.properties"));
        settings.load();
        vaccinations = new VaccinationService(settings);
    }

    private static Vaccination dose(String id, String petId, LocalDate nextDue) {
        return TestData.vaccinationDue(id, petId, nextDue);
    }

    // ------------------------------------------------------------------
    // CREATE
    // ------------------------------------------------------------------

    @Test
    @DisplayName("a valid vaccination is stored and can be found by id")
    void addAndFind() throws InvalidVaccinationException {
        vaccinations.addVaccination(dose("VAC001", "P001", TODAY.plusDays(200)));

        assertEquals(1, vaccinations.count());
        assertTrue(vaccinations.findById("VAC001").isPresent());
        assertTrue(vaccinations.findById(" VAC001 ").isPresent());
        assertTrue(vaccinations.findById(null).isEmpty());
    }

    @Test
    @DisplayName("an invalid vaccination is rejected and nothing is stored")
    void anInvalidVaccinationIsNotStored() {
        Vaccination broken = TestData.vaccination("VAC001", "P001", "V001", "Rabies",
                TODAY.minusDays(10), TODAY.minusDays(40));   // next dose before the dose given

        assertThrows(InvalidVaccinationException.class, () -> vaccinations.addVaccination(broken));
        assertEquals(0, vaccinations.count());
    }

    @Test
    @DisplayName("a duplicate vaccination id is refused")
    void duplicateIdsAreRefused() throws InvalidVaccinationException {
        vaccinations.addVaccination(dose("VAC001", "P001", TODAY.plusDays(200)));

        assertThrows(InvalidVaccinationException.class,
                () -> vaccinations.addVaccination(dose("VAC001", "P002", TODAY.plusDays(100))));
        assertEquals(1, vaccinations.count());
    }

    // ------------------------------------------------------------------
    // Grouping by pet
    // ------------------------------------------------------------------

    @Test
    @DisplayName("records are grouped by pet")
    void recordsAreGroupedByPet() throws InvalidVaccinationException {
        vaccinations.addVaccination(dose("VAC001", "P001", TODAY.plusDays(200)));
        vaccinations.addVaccination(dose("VAC002", "P001", TODAY.plusDays(100)));
        vaccinations.addVaccination(dose("VAC003", "P002", TODAY.plusDays(50)));

        assertEquals(2, vaccinations.forPet("P001").size());
        assertEquals(1, vaccinations.forPet("P002").size());
        assertTrue(vaccinations.forPet("P999").isEmpty());
        assertTrue(vaccinations.forPet(null).isEmpty());
    }

    @Test
    @DisplayName("a pet's records come back with the most urgent dose first")
    void forPetIsOrderedByNextDue() throws InvalidVaccinationException {
        vaccinations.addVaccination(dose("VAC001", "P001", TODAY.plusDays(300)));
        vaccinations.addVaccination(dose("VAC002", "P001", TODAY.minusDays(5)));
        vaccinations.addVaccination(dose("VAC003", "P001", TODAY.plusDays(10)));

        assertEquals(List.of("VAC002", "VAC003", "VAC001"),
                vaccinations.forPet("P001").stream()
                        .map(Vaccination::getVaccinationId).toList());
    }

    @Test
    @DisplayName("the latest dose given to a pet is the one with the newest date")
    void latestForPet() throws InvalidVaccinationException {
        vaccinations.addVaccination(TestData.vaccination("VAC001", "P001", "V001", "Rabies",
                TODAY.minusYears(2), TODAY.minusYears(1)));
        vaccinations.addVaccination(TestData.vaccination("VAC002", "P001", "V001", "Distemper",
                TODAY.minusMonths(2), TODAY.plusMonths(10)));

        assertEquals("VAC002", vaccinations.latestForPet("P001").orElseThrow().getVaccinationId());
        assertTrue(vaccinations.latestForPet("P999").isEmpty());
    }

    // ------------------------------------------------------------------
    // Classification and reminders
    // ------------------------------------------------------------------

    @Test
    @DisplayName("a record is classified with the configured window")
    void statusUsesTheConfiguredWindow() throws InvalidVaccinationException {
        Vaccination overdue = dose("VAC001", "P001", TODAY.minusDays(3));
        Vaccination dueSoon = dose("VAC002", "P002", TODAY.plusDays(1));
        Vaccination upcoming = dose("VAC003", "P003", TODAY.plusDays(200));
        vaccinations.addVaccination(overdue);
        vaccinations.addVaccination(dueSoon);
        vaccinations.addVaccination(upcoming);

        assertSame(VaccinationStatus.OVERDUE, vaccinations.statusOf(overdue));
        assertSame(VaccinationStatus.DUE_SOON, vaccinations.statusOf(dueSoon));
        assertSame(VaccinationStatus.UPCOMING, vaccinations.statusOf(upcoming));
    }

    @Test
    @DisplayName("alerts are only the records that need action, most urgent first")
    void alerts() throws InvalidVaccinationException {
        vaccinations.addVaccination(dose("VAC001", "P001", TODAY.plusDays(200)));
        vaccinations.addVaccination(dose("VAC002", "P002", TODAY.plusDays(5)));
        vaccinations.addVaccination(dose("VAC003", "P003", TODAY.minusDays(10)));

        assertEquals(List.of("VAC003", "VAC002"),
                vaccinations.alerts().stream().map(Vaccination::getVaccinationId).toList());
    }

    @Test
    @DisplayName("filtering by status returns the matching records")
    void withStatus() throws InvalidVaccinationException {
        vaccinations.addVaccination(dose("VAC001", "P001", TODAY.minusDays(1)));
        vaccinations.addVaccination(dose("VAC002", "P002", TODAY.plusDays(5)));
        vaccinations.addVaccination(dose("VAC003", "P003", TODAY.plusDays(200)));

        assertEquals(1, vaccinations.withStatus(VaccinationStatus.OVERDUE).size());
        assertEquals(1, vaccinations.withStatus(VaccinationStatus.DUE_SOON).size());
        assertEquals(1, vaccinations.withStatus(VaccinationStatus.UPCOMING).size());
    }

    @Test
    @DisplayName("the census counts every record and always has all three keys")
    void countByStatus() throws InvalidVaccinationException {
        vaccinations.addVaccination(dose("VAC001", "P001", TODAY.minusDays(1)));
        vaccinations.addVaccination(dose("VAC002", "P002", TODAY.plusDays(3)));
        vaccinations.addVaccination(dose("VAC003", "P003", TODAY.plusDays(200)));
        vaccinations.addVaccination(dose("VAC004", "P004", TODAY.plusDays(400)));

        Map<VaccinationStatus, Integer> counts = vaccinations.countByStatus();

        assertEquals(3, counts.size());
        assertEquals(1, counts.get(VaccinationStatus.OVERDUE).intValue());
        assertEquals(1, counts.get(VaccinationStatus.DUE_SOON).intValue());
        assertEquals(2, counts.get(VaccinationStatus.UPCOMING).intValue());
        assertEquals(1, vaccinations.countOverdue());
        assertEquals(1, vaccinations.countDueSoon());
    }

    @Test
    @DisplayName("a pet with no records at all is reported separately from a healthy one")
    void aPetWithNoRecordsIsNotCalledHealthy() throws InvalidVaccinationException {
        vaccinations.addVaccination(dose("VAC001", "P001", TODAY.plusDays(300)));

        assertSame(PetHealthStatus.NO_RECORDS, vaccinations.statusForPet("P999"));
        assertSame(PetHealthStatus.UP_TO_DATE, vaccinations.statusForPet("P001"));
    }

    @Test
    @DisplayName("the worst record decides a pet's overall status")
    void theWorstRecordWins() throws InvalidVaccinationException {
        // Up to date plus one overdue record must report OVERDUE, not UP_TO_DATE.
        vaccinations.addVaccination(dose("VAC001", "P001", TODAY.plusDays(300)));
        vaccinations.addVaccination(dose("VAC002", "P001", TODAY.minusDays(2)));

        assertSame(PetHealthStatus.OVERDUE, vaccinations.statusForPet("P001"));
    }

    @Test
    @DisplayName("a due-soon record makes the pet due soon")
    void aDueSoonRecordIsReported() throws InvalidVaccinationException {
        vaccinations.addVaccination(dose("VAC001", "P001", TODAY.plusDays(300)));
        vaccinations.addVaccination(dose("VAC002", "P001", TODAY.plusDays(2)));

        assertSame(PetHealthStatus.DUE_SOON, vaccinations.statusForPet("P001"));
    }

    @Test
    @DisplayName("the number of pets needing attention counts pets, not records")
    void petsNeedingAttentionCountsPets() throws InvalidVaccinationException {
        vaccinations.addVaccination(dose("VAC001", "P001", TODAY.minusDays(1)));
        vaccinations.addVaccination(dose("VAC002", "P001", TODAY.plusDays(2)));
        vaccinations.addVaccination(dose("VAC003", "P002", TODAY.plusDays(200)));

        // Two records for P001 but only one pet needs attention.
        assertEquals(2, vaccinations.alerts().size());
        assertEquals(1, vaccinations.petsNeedingAttention());
    }

    // ------------------------------------------------------------------
    // The reminder window
    // ------------------------------------------------------------------

    @Test
    @DisplayName("the window comes from the saved preference")
    void theWindowComesFromSettings() {
        assertEquals(AppSettings.DEFAULT_REMINDER_DAYS, vaccinations.getReminderDays());
        settings.setReminderDays(14);
        assertEquals(14, vaccinations.getReminderDays());
    }

    @Test
    @DisplayName("widening the window reclassifies records that are already stored")
    void changingTheWindowReclassifiesEverything() throws InvalidVaccinationException {
        Vaccination inTwentyDays = dose("VAC001", "P001", TODAY.plusDays(20));
        vaccinations.addVaccination(inTwentyDays);

        settings.setReminderDays(7);
        assertSame(VaccinationStatus.UPCOMING, vaccinations.statusOf(inTwentyDays));
        assertEquals(0, vaccinations.alerts().size());

        settings.setReminderDays(30);
        assertSame(VaccinationStatus.DUE_SOON, vaccinations.statusOf(inTwentyDays));
        assertEquals(1, vaccinations.alerts().size());

        // And the pet table follows the same rule.
        assertSame(PetHealthStatus.DUE_SOON, vaccinations.statusForPet("P001"));
    }

    // ------------------------------------------------------------------
    // Review flag, update and delete
    // ------------------------------------------------------------------

    @Test
    @DisplayName("a reminder can be acknowledged and unacknowledged")
    void markReviewed() throws InvalidVaccinationException {
        vaccinations.addVaccination(dose("VAC001", "P001", TODAY.minusDays(2)));

        assertTrue(vaccinations.markReviewed("VAC001", true));
        assertTrue(vaccinations.findById("VAC001").orElseThrow().isReviewed());

        assertTrue(vaccinations.markReviewed("VAC001", false));
        assertFalse(vaccinations.findById("VAC001").orElseThrow().isReviewed());

        assertFalse(vaccinations.markReviewed("VAC999", true));
    }

    @Test
    @DisplayName("updating a record keeps the pet grouping correct")
    void updateKeepsTheGroupingCorrect() throws InvalidVaccinationException {
        vaccinations.addVaccination(dose("VAC001", "P001", TODAY.plusDays(200)));

        Vaccination moved = dose("VAC001", "P002", TODAY.plusDays(200));
        vaccinations.updateVaccination(moved);

        assertEquals(1, vaccinations.count());
        assertTrue(vaccinations.forPet("P001").isEmpty());
        assertEquals(1, vaccinations.forPet("P002").size());
    }

    @Test
    @DisplayName("updating a record that does not exist is an error")
    void updateRejectsAnUnknownId() {
        assertThrows(InvalidVaccinationException.class,
                () -> vaccinations.updateVaccination(dose("VAC404", "P001", TODAY.plusDays(10))));
    }

    @Test
    @DisplayName("deleting removes the record from every index")
    void deleteClearsEveryIndex() throws InvalidVaccinationException {
        vaccinations.addVaccination(dose("VAC001", "P001", TODAY.plusDays(200)));

        assertTrue(vaccinations.delete("VAC001"));

        assertEquals(0, vaccinations.count());
        assertTrue(vaccinations.findById("VAC001").isEmpty());
        assertTrue(vaccinations.forPet("P001").isEmpty());
        assertFalse(vaccinations.delete("VAC001"));
    }

    @Test
    @DisplayName("deleting a pet's records reports how many went with it")
    void deleteByPet() throws InvalidVaccinationException {
        vaccinations.addVaccination(dose("VAC001", "P001", TODAY.plusDays(200)));
        vaccinations.addVaccination(dose("VAC002", "P001", TODAY.plusDays(100)));
        vaccinations.addVaccination(dose("VAC003", "P002", TODAY.plusDays(50)));

        assertEquals(2, vaccinations.deleteByPet("P001"));

        assertEquals(1, vaccinations.count());
        assertTrue(vaccinations.forPet("P001").isEmpty());
        assertEquals(1, vaccinations.forPet("P002").size());
        assertEquals(0, vaccinations.deleteByPet("P999"));
        assertEquals(0, vaccinations.deleteByPet(null));
    }

    // ------------------------------------------------------------------
    // Bulk loading
    // ------------------------------------------------------------------

    @Test
    @DisplayName("loadAll replaces everything and rebuilds the indexes")
    void loadAllRebuildsEveryIndex() throws InvalidVaccinationException {
        vaccinations.addVaccination(dose("VAC001", "P001", TODAY.plusDays(200)));

        vaccinations.loadAll(List.of(
                dose("VAC009", "P005", TODAY.minusDays(4)),
                dose("VAC010", "P005", TODAY.plusDays(9))));

        assertEquals(2, vaccinations.count());
        assertTrue(vaccinations.findById("VAC001").isEmpty());
        assertTrue(vaccinations.forPet("P001").isEmpty());
        assertEquals(2, vaccinations.forPet("P005").size());
        assertTrue(vaccinations.findById("VAC009").isPresent());
    }

    @Test
    @DisplayName("loadAll skips records with no id and tolerates a null list")
    void loadAllToleratesBrokenInput() {
        Vaccination noId = dose("VAC001", "P001", TODAY.plusDays(10));
        noId.setVaccinationId(null);
        vaccinations.loadAll(List.of(noId, dose("VAC002", "P002", TODAY.plusDays(10))));

        assertEquals(1, vaccinations.count());

        vaccinations.loadAll(null);
        assertEquals(0, vaccinations.count());
        assertTrue(vaccinations.getAll().isEmpty());
    }

    @Test
    @DisplayName("loading seeds the id generator past the highest stored id")
    void loadAllSyncsTheIdGenerator() {
        vaccinations.loadAll(List.of(dose("VAC031", "P001", TODAY.plusDays(10))));
        assertEquals("VAC032", IDGenerator.nextVaccinationId());
    }
}
