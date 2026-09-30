package com.pawcare.service;

import com.pawcare.exception.DataAccessException;
import com.pawcare.exception.InvalidOwnerException;
import com.pawcare.exception.PawCareException;
import com.pawcare.model.Pet;
import com.pawcare.model.enums.AppointmentStatus;
import com.pawcare.model.enums.Species;
import com.pawcare.repository.DataStore;
import com.pawcare.support.TestData;
import com.pawcare.util.AppSettings;
import com.pawcare.util.IDGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The service that ties everything together: seeding, persistence across a restart,
 * cascading deletes, and recovery from a damaged data file.
 *
 * <p>These are the tests closest to what a user actually experiences. Each one builds a
 * clinic in a temporary folder, closes it, and opens a new one over the same folder – which
 * is exactly what happens when the application is closed and started again.</p>
 */
class ClinicServiceTest {

    private static final int SAMPLE_PETS = 8;
    private static final int SAMPLE_OWNERS = 5;
    private static final int SAMPLE_VETS = 4;
    private static final int SAMPLE_APPOINTMENTS = 8;
    private static final int SAMPLE_TREATMENTS = 10;
    private static final int SAMPLE_VACCINATIONS = 10;

    @TempDir
    Path tempDir;

    private Path dataDirectory;
    private AppSettings settings;

    @BeforeEach
    void setUp() {
        dataDirectory = tempDir.resolve("data");
        settings = new AppSettings(dataDirectory.resolve("settings.properties"));
        settings.load();
    }

    /** Opens a clinic over the shared data folder – the equivalent of starting the app. */
    private ClinicService open() {
        DataStore store = new DataStore(dataDirectory);
        ClinicService clinic = new ClinicService(store, settings);
        clinic.loadAll();
        return clinic;
    }

    // ------------------------------------------------------------------
    // First run
    // ------------------------------------------------------------------

    @Test
    @DisplayName("a first run seeds the demo clinic")
    void aFirstRunSeedsTheDemoData() {
        ClinicService clinic = open();

        assertEquals(SAMPLE_PETS, clinic.pets().count());
        assertEquals(SAMPLE_OWNERS, clinic.owners().count());
        assertEquals(SAMPLE_VETS, clinic.veterinarians().count());
        assertEquals(SAMPLE_APPOINTMENTS, clinic.appointments().count());
        assertEquals(SAMPLE_TREATMENTS, clinic.treatments().count());
        assertEquals(SAMPLE_VACCINATIONS, clinic.vaccinations().count());
        assertTrue(clinic.getLoadWarnings().isEmpty());
    }

    @Test
    @DisplayName("the demo clinic covers every species and every reminder state")
    void theDemoClinicIsWorthDemonstrating() {
        ClinicService clinic = open();

        // Polymorphism: three concrete species behind one abstract type.
        assertEquals(3, clinic.pets().findBySpecies(Species.DOG).size());
        assertEquals(3, clinic.pets().findBySpecies(Species.CAT).size());
        assertEquals(2, clinic.pets().findBySpecies(Species.BIRD).size());

        // The dashboard has something to show in every panel.
        assertFalse(clinic.appointments().today().isEmpty(), "no visit is booked for today");
        assertFalse(clinic.appointments().upcoming(5).isEmpty(), "no upcoming visit");
        assertFalse(clinic.reports().recentTreatments(5).isEmpty(), "no recent treatment");
        assertTrue(clinic.vaccinations().countOverdue() > 0, "no overdue vaccination");
        assertTrue(clinic.vaccinations().countDueSoon() > 0, "no vaccination falling due");
    }

    @Test
    @DisplayName("the demo data is written to disk at once, not saved on exit")
    void theFirstRunIsPersistedImmediately() {
        open();

        assertTrue(Files.exists(dataDirectory.resolve(DataStore.PETS_FILE)));
        assertTrue(Files.exists(dataDirectory.resolve(DataStore.OWNERS_FILE)));
        assertTrue(Files.exists(dataDirectory.resolve(DataStore.VETERINARIANS_FILE)));
        assertTrue(Files.exists(dataDirectory.resolve(DataStore.APPOINTMENTS_FILE)));
        assertTrue(Files.exists(dataDirectory.resolve(DataStore.TREATMENTS_FILE)));
        assertTrue(Files.exists(dataDirectory.resolve(DataStore.VACCINATIONS_FILE)));
    }

    @Test
    @DisplayName("a second start loads the stored clinic instead of seeding a new one")
    void theDemoDataIsNotSeededTwice() {
        open();
        ClinicService reopened = open();

        assertEquals(SAMPLE_PETS, reopened.pets().count());
        assertEquals(SAMPLE_OWNERS, reopened.owners().count());
        assertEquals(SAMPLE_TREATMENTS, reopened.treatments().count());
    }

    // ------------------------------------------------------------------
    // Persistence across a restart
    // ------------------------------------------------------------------

    @Test
    @DisplayName("an added pet is still there after a restart")
    void anAddedPetSurvivesARestart() throws PawCareException {
        ClinicService clinic = open();
        clinic.addPet(TestData.dog("P009", "O001", "Rex"));

        ClinicService reopened = open();

        assertEquals(SAMPLE_PETS + 1, reopened.pets().count());
        assertEquals("Rex", reopened.pets().findPetById("P009").orElseThrow().getName());
    }

    @Test
    @DisplayName("an added owner is still there after a restart")
    void anAddedOwnerSurvivesARestart() throws PawCareException {
        ClinicService clinic = open();
        clinic.addOwner(TestData.owner("O006", "Nisha Verma"));

        ClinicService reopened = open();

        assertEquals(SAMPLE_OWNERS + 1, reopened.owners().count());
        assertEquals("Nisha Verma", reopened.owners().ownerName("O006"));
    }

    @Test
    @DisplayName("an added appointment is still there after a restart")
    void anAddedAppointmentSurvivesARestart() throws PawCareException {
        ClinicService clinic = open();
        clinic.addAppointment(TestData.appointment("A009", "P001", "V001",
                LocalDate.now().plusDays(1), LocalTime.of(9, 0), AppointmentStatus.SCHEDULED));

        ClinicService reopened = open();

        assertEquals(SAMPLE_APPOINTMENTS + 1, reopened.appointments().count());
        assertTrue(reopened.appointments().findById("A009").isPresent());
    }

    @Test
    @DisplayName("an added treatment and vaccination are still there after a restart")
    void addedHistorySurvivesARestart() throws PawCareException {
        ClinicService clinic = open();
        clinic.addTreatment(TestData.treatment("T011", "P001", "V001", LocalDate.now().minusDays(1)));
        clinic.addVaccination(TestData.vaccinationDue("VAC011", "P001", LocalDate.now().plusDays(300)));

        ClinicService reopened = open();

        assertEquals(SAMPLE_TREATMENTS + 1, reopened.treatments().count());
        assertEquals(SAMPLE_VACCINATIONS + 1, reopened.vaccinations().count());
        assertTrue(reopened.treatments().findById("T011").isPresent());
        assertTrue(reopened.vaccinations().findById("VAC011").isPresent());
    }

    @Test
    @DisplayName("an edited record keeps its edit after a restart")
    void anEditSurvivesARestart() throws PawCareException {
        ClinicService clinic = open();
        Pet bruno = clinic.pets().requirePet("P001");
        bruno.setName("Bruno Junior");
        clinic.updatePet(bruno);

        ClinicService reopened = open();

        assertEquals("Bruno Junior", reopened.pets().requirePet("P001").getName());
    }

    @Test
    @DisplayName("an acknowledged reminder keeps its flag after a restart")
    void theReviewFlagSurvivesARestart() throws PawCareException {
        ClinicService clinic = open();
        clinic.markVaccinationReviewed("VAC001", true);

        ClinicService reopened = open();

        assertTrue(reopened.vaccinations().findById("VAC001").orElseThrow().isReviewed());
    }

    @Test
    @DisplayName("the treatment history is rebuilt per pet after a restart")
    void theTreatmentHistoryIsRebuilt() {
        ClinicService clinic = open();

        // P001 has T001 and T009 in the demo data; they must come back grouped under P001.
        assertEquals(2, clinic.treatments().countForPet("P001"));
        assertEquals(SAMPLE_TREATMENTS, clinic.treatments().count());
    }

    // ------------------------------------------------------------------
    // Cascading deletes
    // ------------------------------------------------------------------

    @Test
    @DisplayName("deleting a pet also removes its appointments, treatments and vaccinations")
    void deletingAPetCascades() throws PawCareException {
        ClinicService clinic = open();

        int appointmentsBefore = clinic.appointments().count();
        int vaccinationsBefore = clinic.vaccinations().count();
        int p001Appointments = clinic.appointments().byPet("P001").size();
        int p001Vaccinations = clinic.vaccinations().forPet("P001").size();
        int p001Treatments = clinic.treatments().countForPet("P001");

        assertTrue(p001Appointments > 0 && p001Vaccinations > 0 && p001Treatments > 0,
                "the demo data no longer gives P001 any history to cascade");

        clinic.deletePet("P001");

        assertEquals(SAMPLE_PETS - 1, clinic.pets().count());
        assertTrue(clinic.pets().findPetById("P001").isEmpty());
        assertEquals(appointmentsBefore - p001Appointments, clinic.appointments().count());
        assertEquals(vaccinationsBefore - p001Vaccinations, clinic.vaccinations().count());
        assertEquals(0, clinic.treatments().countForPet("P001"));
    }

    @Test
    @DisplayName("the cascade is written to disk too")
    void theCascadeIsPersisted() throws PawCareException {
        ClinicService clinic = open();
        clinic.deletePet("P001");

        ClinicService reopened = open();

        assertEquals(SAMPLE_PETS - 1, reopened.pets().count());
        assertTrue(reopened.appointments().byPet("P001").isEmpty());
        assertTrue(reopened.vaccinations().forPet("P001").isEmpty());
        assertEquals(0, reopened.treatments().countForPet("P001"));
    }

    @Test
    @DisplayName("an owner who still has a pet cannot be deleted")
    void anOwnerWithPetsCannotBeDeleted() {
        ClinicService clinic = open();

        InvalidOwnerException failure = assertThrows(InvalidOwnerException.class,
                () -> clinic.deleteOwner("O001"));

        // The message has to say which pets are in the way, or the user cannot act on it.
        assertTrue(failure.getDetailedMessage().contains("registered pet"));
        assertTrue(failure.getDetailedMessage().contains("P001"));
        assertEquals(SAMPLE_OWNERS, clinic.owners().count());
    }

    @Test
    @DisplayName("an owner with no pets can be deleted, and it sticks")
    void anOwnerWithoutPetsCanBeDeleted() throws PawCareException {
        ClinicService clinic = open();
        clinic.deletePet("P001");
        clinic.deletePet("P002");   // O001's two pets

        clinic.deleteOwner("O001");

        assertEquals(SAMPLE_OWNERS - 1, clinic.owners().count());
        assertFalse(clinic.owners().exists("O001"));

        ClinicService reopened = open();
        assertFalse(reopened.owners().exists("O001"));
    }

    // ------------------------------------------------------------------
    // Reset
    // ------------------------------------------------------------------

    @Test
    @DisplayName("reset restores the demo clinic exactly")
    void resetRestoresTheDemoData() throws PawCareException, DataAccessException {
        ClinicService clinic = open();
        clinic.deletePet("P001");
        clinic.addPet(TestData.dog("P009", "O001", "Rex"));

        clinic.resetDemoData();

        assertEquals(SAMPLE_PETS, clinic.pets().count());
        assertEquals(SAMPLE_OWNERS, clinic.owners().count());
        assertEquals(SAMPLE_VETS, clinic.veterinarians().count());
        assertEquals(SAMPLE_APPOINTMENTS, clinic.appointments().count());
        assertEquals(SAMPLE_TREATMENTS, clinic.treatments().count());
        assertEquals(SAMPLE_VACCINATIONS, clinic.vaccinations().count());
        assertTrue(clinic.pets().findPetById("P001").isPresent());
        assertTrue(clinic.pets().findPetById("P009").isEmpty());
    }

    @Test
    @DisplayName("the reset is persisted, so it survives a restart")
    void theResetIsPersisted() throws PawCareException, DataAccessException {
        ClinicService clinic = open();
        clinic.deletePet("P001");
        clinic.resetDemoData();

        ClinicService reopened = open();

        assertEquals(SAMPLE_PETS, reopened.pets().count());
        assertEquals(SAMPLE_APPOINTMENTS, reopened.appointments().count());
    }

    @Test
    @DisplayName("after a reset the id generator starts cleanly from the demo ids")
    void theResetRestartsTheIdGenerator() throws DataAccessException {
        ClinicService clinic = open();
        clinic.resetDemoData();

        assertEquals(SAMPLE_PETS, clinic.pets().count());
        // A new pet must not collide with one of the demo ids.
        assertTrue(clinic.pets().findPetById("P009").isEmpty());
        assertEquals("P009", IDGenerator.nextPetId());
    }

    // ------------------------------------------------------------------
    // Damaged data
    // ------------------------------------------------------------------

    @Test
    @DisplayName("a damaged file is set aside and the clinic still starts")
    void aDamagedFileIsQuarantinedAndTheClinicStarts() throws IOException {
        open();   // create the demo data first
        Files.write(dataDirectory.resolve(DataStore.PETS_FILE), new byte[]{1, 2, 3});

        ClinicService recovered = open();

        // The window opens, the user is told, and the other datasets are still there.
        assertFalse(recovered.getLoadWarnings().isEmpty());
        assertEquals(0, recovered.pets().count());
        assertEquals(SAMPLE_OWNERS, recovered.owners().count());

        try (Stream<Path> files = Files.list(dataDirectory)) {
            assertTrue(files.anyMatch(p -> p.getFileName().toString().startsWith("pets.dat.corrupt-")),
                    "the damaged file was not preserved for inspection");
        }
    }

    @Test
    @DisplayName("the startup warning names the file that could not be read")
    void theWarningNamesTheFile() throws IOException {
        open();
        Files.write(dataDirectory.resolve(DataStore.TREATMENTS_FILE), new byte[]{7, 7, 7});

        ClinicService recovered = open();

        assertTrue(recovered.getLoadWarnings().stream()
                        .anyMatch(w -> w.contains(DataStore.TREATMENTS_FILE)),
                "the warning does not say which file was damaged: " + recovered.getLoadWarnings());
    }

    @Test
    @DisplayName("a clinic whose pets were lost is not re-seeded, so the owners are not duplicated")
    void aPartlyDamagedClinicIsNotReSeeded() throws IOException {
        open();
        Files.write(dataDirectory.resolve(DataStore.PETS_FILE), new byte[]{0});

        ClinicService recovered = open();

        assertEquals(0, recovered.pets().count());
        assertEquals(SAMPLE_OWNERS, recovered.owners().count(),
                "the demo data was seeded again over an existing clinic");
    }

    @Test
    @DisplayName("the warning list is a copy, so a caller cannot alter it")
    void getLoadWarningsIsDefensive() {
        ClinicService clinic = open();
        clinic.getLoadWarnings().add("something the caller made up");
        assertTrue(clinic.getLoadWarnings().isEmpty());
    }

    // ------------------------------------------------------------------
    // Accessors
    // ------------------------------------------------------------------

    @Test
    @DisplayName("the accessors hand out the very services the clinic uses")
    void theAccessorsAreWiredUp() throws PawCareException {
        ClinicService clinic = open();

        // Editing through the clinic must be visible through the sub-service. That is only
        // true if the accessor returns the same object rather than a copy.
        Pet bruno = clinic.pets().requirePet("P001");
        bruno.setName("Renamed Through The Clinic");

        assertEquals("Renamed Through The Clinic", clinic.pets().requirePet("P001").getName());
        assertEquals(SAMPLE_PETS, clinic.reports().totalPets());
        assertEquals(SAMPLE_OWNERS, clinic.reports().totalOwners());
        assertEquals(SAMPLE_VETS, clinic.reports().totalVeterinarians());
    }

    @Test
    @DisplayName("the report totals are computed from the live data, not hard-coded")
    void reportTotalsFollowTheData() throws PawCareException {
        ClinicService clinic = open();
        assertEquals(SAMPLE_PETS, clinic.reports().totalPets());

        clinic.addPet(TestData.dog("P009", "O001", "Rex"));
        assertEquals(SAMPLE_PETS + 1, clinic.reports().totalPets());

        clinic.deletePet("P009");
        assertEquals(SAMPLE_PETS, clinic.reports().totalPets());
    }

    @Test
    @DisplayName("isEmpty reports a clinic with no patients and no owners")
    void isEmpty() throws DataAccessException {
        ClinicService clinic = open();
        assertFalse(clinic.isEmpty());

        clinic.resetDemoData();
        assertFalse(clinic.isEmpty());

        ClinicService empty = new ClinicService(new DataStore(tempDir.resolve("nowhere")), settings);
        assertTrue(empty.isEmpty());
    }

    @Test
    @DisplayName("persistAll can be called twice in a row without harm")
    void persistAllIsIdempotent() throws DataAccessException {
        ClinicService clinic = open();
        clinic.persistAll();
        clinic.persistAll();

        ClinicService reopened = open();
        assertEquals(SAMPLE_PETS, reopened.pets().count());
        assertEquals(SAMPLE_TREATMENTS, reopened.treatments().count());
    }

    @Test
    @DisplayName("the settings object is shared, so a preference change is visible everywhere")
    void settingsAreShared() {
        ClinicService clinic = open();

        settings.setReminderDays(7);
        assertEquals(7, clinic.vaccinations().getReminderDays());
        assertEquals(7, clinic.settings().getReminderDays());
    }

    @Test
    @DisplayName("the stored data keeps its concrete types, so the plans still differ")
    void concreteTypesSurviveARestart() throws PawCareException {
        ClinicService clinic = open();
        ClinicService reopened = open();

        Pet dog = reopened.pets().requirePet("P001");
        Pet bird = reopened.pets().requirePet("P003");

        assertTrue(dog instanceof com.pawcare.model.Dog);
        assertTrue(bird instanceof com.pawcare.model.Bird);
        assertTrue(dog.treatmentPlan().startsWith("Canine"));
        assertTrue(bird.treatmentPlan().startsWith("Avian"));
    }

    @Test
    @DisplayName("an owner cannot be deleted through the clinic when the id is unknown")
    void deletingAnUnknownOwnerIsHarmless() throws PawCareException {
        ClinicService clinic = open();

        clinic.deleteOwner("O999");

        assertEquals(SAMPLE_OWNERS, clinic.owners().count());
    }

    @Test
    @DisplayName("deleting a pet that does not exist changes nothing")
    void deletingAnUnknownPetIsHarmless() throws PawCareException {
        ClinicService clinic = open();

        clinic.deletePet("P999");

        assertEquals(SAMPLE_PETS, clinic.pets().count());
    }
}
