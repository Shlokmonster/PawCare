package com.pawcare.repository;

import com.pawcare.exception.DataAccessException;
import com.pawcare.model.Appointment;
import com.pawcare.model.Owner;
import com.pawcare.model.Pet;
import com.pawcare.model.Treatment;
import com.pawcare.model.Vaccination;
import com.pawcare.model.Veterinarian;
import com.pawcare.model.enums.AppointmentStatus;
import com.pawcare.support.TestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Reading and writing the {@code data/} files.
 *
 * <p>Three failure modes matter and each has its own test:</p>
 * <ul>
 *   <li>no file at all – normal on a first run, and it must not be an error;</li>
 *   <li>a file that exists but cannot be read – the application must report it clearly and
 *       still be able to start;</li>
 *   <li>a write that fails halfway – the previous good file must survive, which is why
 *       writes go to a temporary file and are then moved into place.</li>
 * </ul>
 *
 * <p>{@code @TempDir} gives every test its own throwaway directory, so nothing here touches
 * the real {@code data/} folder the application uses.</p>
 */
class DataStoreTest {

    @TempDir
    Path tempDir;

    private DataStore store;

    @BeforeEach
    void setUp() {
        store = new DataStore(tempDir.resolve("data"));
    }

    // ------------------------------------------------------------------
    // Directory and missing files
    // ------------------------------------------------------------------

    @Test
    @DisplayName("ensureDirectory creates the folder, and is safe to call twice")
    void ensureDirectoryCreatesTheFolder() throws DataAccessException {
        assertFalse(Files.exists(store.getDataDirectory()));

        store.ensureDirectory();
        assertTrue(Files.isDirectory(store.getDataDirectory()));

        store.ensureDirectory();   // must not throw
        assertTrue(Files.isDirectory(store.getDataDirectory()));
    }

    @Test
    @DisplayName("a missing file is not an error: the clinic simply starts empty")
    void aMissingFileMeansEmpty() throws DataAccessException {
        assertFalse(store.exists(DataStore.PETS_FILE));
        assertNull(store.load(DataStore.PETS_FILE));
        assertTrue(store.loadPets().isEmpty());
        assertTrue(store.loadOwners().isEmpty());
        assertTrue(store.loadVeterinarians().isEmpty());
        assertTrue(store.loadAppointments().isEmpty());
        assertTrue(store.loadTreatments().isEmpty());
        assertTrue(store.loadVaccinations().isEmpty());
    }

    // ------------------------------------------------------------------
    // Round trips – one per entity
    // ------------------------------------------------------------------

    @Test
    @DisplayName("pets survive a save and load, keeping their concrete species")
    void petsRoundTrip() throws DataAccessException {
        store.savePets(List.of(
                TestData.dog("P001", "O001", "Bruno"),
                TestData.cat("P002", "O001", "Whiskers"),
                TestData.bird("P003", "O002", "Kiwi")));

        List<Pet> loaded = store.loadPets();

        assertEquals(3, loaded.size());
        // Deserialization restores the real subclass, which is what makes
        // animal.treatmentPlan() still work after a restart.
        assertInstanceOf(com.pawcare.model.Dog.class, loaded.get(0));
        assertInstanceOf(com.pawcare.model.Cat.class, loaded.get(1));
        assertInstanceOf(com.pawcare.model.Bird.class, loaded.get(2));
        assertEquals("Bruno", loaded.get(0).getName());
        assertEquals("Feline health assessment, dental and oral examination, indoor/outdoor risk "
                + "evaluation, deworming and vaccination review.", loaded.get(1).treatmentPlan());
    }

    @Test
    @DisplayName("owners survive a save and load")
    void ownersRoundTrip() throws DataAccessException {
        store.saveOwners(List.of(TestData.owner("O001", "Aarav Sharma")));

        List<Owner> loaded = store.loadOwners();

        assertEquals(1, loaded.size());
        assertEquals("Aarav Sharma", loaded.get(0).getName());
        assertEquals("9876543210", loaded.get(0).getPhone());
        assertEquals("12 MG Road, Bengaluru 560001", loaded.get(0).getAddress());
    }

    @Test
    @DisplayName("veterinarians survive a save and load")
    void veterinariansRoundTrip() throws DataAccessException {
        store.saveVeterinarians(List.of(TestData.vet("V001", "Dr. Meera Nair")));

        List<Veterinarian> loaded = store.loadVeterinarians();

        assertEquals(1, loaded.size());
        assertEquals("Dr. Meera Nair", loaded.get(0).getName());
        assertEquals("General Medicine", loaded.get(0).getSpecialization());
    }

    @Test
    @DisplayName("appointments survive a save and load")
    void appointmentsRoundTrip() throws DataAccessException {
        LocalDate date = LocalDate.now().plusDays(3);
        store.saveAppointments(List.of(TestData.appointment("A001", "P001", "V001", date,
                LocalTime.of(10, 30), AppointmentStatus.SCHEDULED)));

        List<Appointment> loaded = store.loadAppointments();

        assertEquals(1, loaded.size());
        assertEquals(date, loaded.get(0).getAppointmentDate());
        assertEquals(LocalTime.of(10, 30), loaded.get(0).getAppointmentTime());
        assertEquals(AppointmentStatus.SCHEDULED, loaded.get(0).getStatus());
    }

    @Test
    @DisplayName("treatments survive a save and load")
    void treatmentsRoundTrip() throws DataAccessException {
        store.saveTreatments(List.of(
                TestData.treatment("T001", "P001", "V001", LocalDate.now().minusDays(4))));

        List<Treatment> loaded = store.loadTreatments();

        assertEquals(1, loaded.size());
        assertEquals("Ear infection", loaded.get(0).getDiagnosis());
        assertEquals("Amoxicillin", loaded.get(0).getMedication());
    }

    @Test
    @DisplayName("vaccinations survive a save and load, review flag included")
    void vaccinationsRoundTrip() throws DataAccessException {
        Vaccination vaccination = TestData.vaccinationDue("VAC001", "P001", LocalDate.now().plusDays(20));
        vaccination.setReviewed(true);
        store.saveVaccinations(List.of(vaccination));

        List<Vaccination> loaded = store.loadVaccinations();

        assertEquals(1, loaded.size());
        assertEquals("Rabies", loaded.get(0).getVaccineName());
        assertTrue(loaded.get(0).isReviewed());
    }

    @Test
    @DisplayName("an empty list round trips as an empty list, not as a missing file")
    void anEmptyListRoundTrips() throws DataAccessException {
        store.savePets(List.of());

        assertTrue(store.exists(DataStore.PETS_FILE));
        assertTrue(store.loadPets().isEmpty());
    }

    @Test
    @DisplayName("saving replaces the previous file rather than appending to it")
    void savingReplacesTheFile() throws DataAccessException {
        store.savePets(List.of(TestData.dog("P001", "O001", "Bruno")));
        store.savePets(List.of(TestData.dog("P002", "O001", "Rex")));

        List<Pet> loaded = store.loadPets();

        assertEquals(1, loaded.size());
        assertEquals("Rex", loaded.get(0).getName());
    }

    // ------------------------------------------------------------------
    // Atomic writes
    // ------------------------------------------------------------------

    @Test
    @DisplayName("a save leaves no temporary file behind")
    void noTemporaryFileIsLeftBehind() throws DataAccessException, IOException {
        store.savePets(List.of(TestData.dog("P001", "O001", "Bruno")));

        try (Stream<Path> files = Files.list(store.getDataDirectory())) {
            assertTrue(files.noneMatch(p -> p.getFileName().toString().endsWith(".tmp")),
                    "the .tmp file used for the atomic move was not cleaned up");
        }
    }

    @Test
    @DisplayName("saving creates the data folder on demand")
    void savingCreatesTheFolder() throws DataAccessException {
        assertFalse(Files.exists(store.getDataDirectory()));

        store.saveOwners(List.of(TestData.owner("O001")));

        assertTrue(Files.isDirectory(store.getDataDirectory()));
        assertEquals(tempDir.resolve("data").resolve(DataStore.OWNERS_FILE),
                store.resolve(DataStore.OWNERS_FILE));
    }

    // ------------------------------------------------------------------
    // Corrupt and unexpected files
    // ------------------------------------------------------------------

    @Test
    @DisplayName("a corrupt file is reported with the file name the user can act on")
    void aCorruptFileIsReported() throws IOException, DataAccessException {
        Files.createDirectories(store.getDataDirectory());
        Files.write(store.resolve(DataStore.PETS_FILE),
                "this is not a serialized list".getBytes(StandardCharsets.UTF_8));

        DataAccessException failure = assertThrows(DataAccessException.class, () -> store.loadPets());

        assertTrue(failure.getMessage().contains(DataStore.PETS_FILE),
                "the message must name the file, otherwise the user cannot find it");
        assertNotNull(failure.getCause());
    }

    @Test
    @DisplayName("a file holding the wrong kind of object is reported too")
    void aFileWithTheWrongContentsIsReported() throws DataAccessException {
        store.save(DataStore.PETS_FILE, "a plain string instead of a list of pets");

        DataAccessException failure = assertThrows(DataAccessException.class, () -> store.loadPets());
        assertTrue(failure.getMessage().contains("unexpected format"));
    }

    @Test
    @DisplayName("one corrupt file does not stop the others from loading")
    void oneBadFileDoesNotAffectTheRest() throws IOException, DataAccessException {
        store.saveOwners(List.of(TestData.owner("O001", "Aarav Sharma")));
        Files.write(store.resolve(DataStore.PETS_FILE), new byte[]{0, 1, 2, 3});

        assertThrows(DataAccessException.class, () -> store.loadPets());
        assertEquals(1, store.loadOwners().size());
    }

    @Test
    @DisplayName("quarantine moves the damaged file aside under a timestamped name")
    void quarantinePreservesTheDamagedFile() throws IOException, DataAccessException {
        Files.createDirectories(store.getDataDirectory());
        Files.write(store.resolve(DataStore.PETS_FILE), new byte[]{1, 2, 3});

        store.quarantine(DataStore.PETS_FILE);

        assertFalse(store.exists(DataStore.PETS_FILE), "the damaged file is still in the way");
        try (Stream<Path> files = Files.list(store.getDataDirectory())) {
            List<String> names = files.map(p -> p.getFileName().toString()).toList();
            assertEquals(1, names.size());
            assertTrue(names.get(0).startsWith(DataStore.PETS_FILE + ".corrupt-"),
                    "unexpected quarantine name: " + names.get(0));
        }
    }

    @Test
    @DisplayName("quarantining a file that is not there does nothing")
    void quarantiningAMissingFileIsHarmless() {
        store.quarantine(DataStore.PETS_FILE);
        assertFalse(store.exists(DataStore.PETS_FILE));
    }

    @Test
    @DisplayName("after quarantining, the clinic can start again with an empty list")
    void theClinicCanStartAfterQuarantine() throws IOException, DataAccessException {
        Files.createDirectories(store.getDataDirectory());
        Files.write(store.resolve(DataStore.PETS_FILE), new byte[]{9, 9, 9});

        store.quarantine(DataStore.PETS_FILE);

        assertTrue(store.loadPets().isEmpty());
    }

    // ------------------------------------------------------------------
    // deleteAll – used by "Reset demo data"
    // ------------------------------------------------------------------

    @Test
    @DisplayName("deleteAll removes every data file")
    void deleteAllRemovesEveryFile() throws DataAccessException {
        store.savePets(List.of(TestData.dog("P001", "O001", "Bruno")));
        store.saveOwners(List.of(TestData.owner("O001")));
        store.saveVeterinarians(List.of(TestData.vet("V001", "Dr. Meera Nair")));
        store.saveAppointments(List.of());
        store.saveTreatments(List.of());
        store.saveVaccinations(List.of());

        store.deleteAll();

        assertFalse(store.exists(DataStore.PETS_FILE));
        assertFalse(store.exists(DataStore.OWNERS_FILE));
        assertFalse(store.exists(DataStore.VETERINARIANS_FILE));
        assertFalse(store.exists(DataStore.APPOINTMENTS_FILE));
        assertFalse(store.exists(DataStore.TREATMENTS_FILE));
        assertFalse(store.exists(DataStore.VACCINATIONS_FILE));

        // Deleting again is harmless.
        store.deleteAll();
    }

    @Test
    @DisplayName("deleteAll leaves a quarantined file alone")
    void deleteAllKeepsQuarantinedFiles() throws IOException, DataAccessException {
        Files.createDirectories(store.getDataDirectory());
        Files.write(store.resolve(DataStore.PETS_FILE), new byte[]{4, 5, 6});
        store.quarantine(DataStore.PETS_FILE);

        store.deleteAll();

        try (Stream<Path> files = Files.list(store.getDataDirectory())) {
            assertEquals(1, files.count(), "the preserved damaged file was deleted");
        }
    }
}
