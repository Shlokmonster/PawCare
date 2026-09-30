package com.pawcare.repository;

import com.pawcare.exception.DataAccessException;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Reads and writes the clinic's data files using Java serialization.
 *
 * <p>One file per entity type is kept inside a {@code data/} directory that is created
 * on demand:</p>
 * <pre>
 *   data/pets.dat         data/treatments.dat
 *   data/owners.dat       data/vaccinations.dat
 *   data/appointments.dat data/veterinarians.dat
 * </pre>
 *
 * <p>The layer stores flat lists. Index structures such as the pet {@code HashMap} or
 * the appointment {@code TreeMap} are rebuilt by the services after loading, so the
 * files never contain redundant derived state.</p>
 *
 * <p>Failure handling is deliberate: a missing file is normal on first run and yields an
 * empty list, while a corrupt file is moved aside with a {@code .corrupt} suffix so the
 * application can still start and the damaged data is preserved for inspection.</p>
 */
public class DataStore {

    public static final String PETS_FILE = "pets.dat";
    public static final String OWNERS_FILE = "owners.dat";
    public static final String APPOINTMENTS_FILE = "appointments.dat";
    public static final String TREATMENTS_FILE = "treatments.dat";
    public static final String VACCINATIONS_FILE = "vaccinations.dat";
    public static final String VETERINARIANS_FILE = "veterinarians.dat";

    private static final DateTimeFormatter STAMP =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private final Path dataDirectory;

    public DataStore(Path dataDirectory) {
        this.dataDirectory = dataDirectory;
    }

    public Path getDataDirectory() {
        return dataDirectory;
    }

    /** Creates the data directory if it does not exist yet. */
    public void ensureDirectory() throws DataAccessException {
        try {
            Files.createDirectories(dataDirectory);
        } catch (IOException e) {
            throw new DataAccessException(
                    "Could not create the data directory at " + dataDirectory + ".", e);
        }
    }

    public boolean exists(String fileName) {
        return Files.exists(resolve(fileName));
    }

    public Path resolve(String fileName) {
        return dataDirectory.resolve(fileName);
    }

    // ------------------------------------------------------------------
    // Generic primitives
    // ------------------------------------------------------------------

    /** Serializes any serializable object to {@code data/<fileName>}. */
    public void save(String fileName, Object data) throws DataAccessException {
        ensureDirectory();
        Path target = resolve(fileName);
        Path temporary = resolve(fileName + ".tmp");
        try (OutputStream out = Files.newOutputStream(temporary);
             ObjectOutputStream objectOut = new ObjectOutputStream(out)) {
            objectOut.writeObject(data);
        } catch (IOException e) {
            throw new DataAccessException("Could not save " + fileName + ".", e);
        }

        // Write to a temporary file first, then move it into place. A crash midway
        // therefore leaves the previous good file untouched.
        try {
            Files.move(temporary, target,
                    StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            throw new DataAccessException("Could not replace " + fileName + ".", e);
        }
    }

    /**
     * Deserializes {@code data/<fileName>}.
     *
     * @return the stored object, or {@code null} when the file does not exist yet
     * @throws DataAccessException when the file exists but cannot be read – the caller
     *                             decides whether to start fresh
     */
    public Object load(String fileName) throws DataAccessException {
        Path source = resolve(fileName);
        if (!Files.exists(source)) {
            return null;
        }
        try (ObjectInputStream in = new ObjectInputStream(Files.newInputStream(source))) {
            return in.readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new DataAccessException("The file " + fileName + " is unreadable or corrupt.", e);
        }
    }

    /** Moves a damaged file aside so the application can start with clean data. */
    public void quarantine(String fileName) {
        Path source = resolve(fileName);
        if (!Files.exists(source)) {
            return;
        }
        Path target = resolve(fileName + ".corrupt-" + LocalDateTime.now().format(STAMP));
        try {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
            System.err.println("[PawCare] Damaged file preserved as " + target.getFileName());
        } catch (IOException e) {
            System.err.println("[PawCare] Could not quarantine " + fileName + ": " + e.getMessage());
        }
    }

    /** Deletes every data file – used by "Reset demo data". */
    public void deleteAll() throws DataAccessException {
        String[] files = {PETS_FILE, OWNERS_FILE, APPOINTMENTS_FILE,
                TREATMENTS_FILE, VACCINATIONS_FILE, VETERINARIANS_FILE};
        for (String file : files) {
            try {
                Files.deleteIfExists(resolve(file));
            } catch (IOException e) {
                throw new DataAccessException("Could not delete " + file + ".", e);
            }
        }
    }

    // ------------------------------------------------------------------
    // Typed convenience methods – one pair per entity
    // ------------------------------------------------------------------

    public void savePets(List<?> pets) throws DataAccessException {
        save(PETS_FILE, new java.util.ArrayList<>(pets));
    }

    @SuppressWarnings("unchecked")
    public List<com.pawcare.model.Pet> loadPets() throws DataAccessException {
        Object data = load(PETS_FILE);
        if (data == null) {
            return new java.util.ArrayList<>();
        }
        if (!(data instanceof List)) {
            throw new DataAccessException("The pets data file has an unexpected format.");
        }
        return (List<com.pawcare.model.Pet>) data;
    }

    public void saveOwners(List<?> owners) throws DataAccessException {
        save(OWNERS_FILE, new java.util.ArrayList<>(owners));
    }

    @SuppressWarnings("unchecked")
    public List<com.pawcare.model.Owner> loadOwners() throws DataAccessException {
        Object data = load(OWNERS_FILE);
        if (data == null) {
            return new java.util.ArrayList<>();
        }
        if (!(data instanceof List)) {
            throw new DataAccessException("The owners data file has an unexpected format.");
        }
        return (List<com.pawcare.model.Owner>) data;
    }

    public void saveVeterinarians(List<?> veterinarians) throws DataAccessException {
        save(VETERINARIANS_FILE, new java.util.ArrayList<>(veterinarians));
    }

    @SuppressWarnings("unchecked")
    public List<com.pawcare.model.Veterinarian> loadVeterinarians() throws DataAccessException {
        Object data = load(VETERINARIANS_FILE);
        if (data == null) {
            return new java.util.ArrayList<>();
        }
        if (!(data instanceof List)) {
            throw new DataAccessException("The veterinarians data file has an unexpected format.");
        }
        return (List<com.pawcare.model.Veterinarian>) data;
    }

    public void saveAppointments(List<?> appointments) throws DataAccessException {
        save(APPOINTMENTS_FILE, new java.util.ArrayList<>(appointments));
    }

    @SuppressWarnings("unchecked")
    public List<com.pawcare.model.Appointment> loadAppointments() throws DataAccessException {
        Object data = load(APPOINTMENTS_FILE);
        if (data == null) {
            return new java.util.ArrayList<>();
        }
        if (!(data instanceof List)) {
            throw new DataAccessException("The appointments data file has an unexpected format.");
        }
        return (List<com.pawcare.model.Appointment>) data;
    }

    public void saveTreatments(List<?> treatments) throws DataAccessException {
        save(TREATMENTS_FILE, new java.util.ArrayList<>(treatments));
    }

    @SuppressWarnings("unchecked")
    public List<com.pawcare.model.Treatment> loadTreatments() throws DataAccessException {
        Object data = load(TREATMENTS_FILE);
        if (data == null) {
            return new java.util.ArrayList<>();
        }
        if (!(data instanceof List)) {
            throw new DataAccessException("The treatments data file has an unexpected format.");
        }
        return (List<com.pawcare.model.Treatment>) data;
    }

    public void saveVaccinations(List<?> vaccinations) throws DataAccessException {
        save(VACCINATIONS_FILE, new java.util.ArrayList<>(vaccinations));
    }

    @SuppressWarnings("unchecked")
    public List<com.pawcare.model.Vaccination> loadVaccinations() throws DataAccessException {
        Object data = load(VACCINATIONS_FILE);
        if (data == null) {
            return new java.util.ArrayList<>();
        }
        if (!(data instanceof List)) {
            throw new DataAccessException("The vaccinations data file has an unexpected format.");
        }
        return (List<com.pawcare.model.Vaccination>) data;
    }
}
