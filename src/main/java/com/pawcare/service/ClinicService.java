package com.pawcare.service;

import com.pawcare.exception.DataAccessException;
import com.pawcare.exception.InvalidOwnerException;
import com.pawcare.exception.PawCareException;
import com.pawcare.model.Appointment;
import com.pawcare.model.Owner;
import com.pawcare.model.Pet;
import com.pawcare.model.Treatment;
import com.pawcare.model.Vaccination;
import com.pawcare.repository.DataStore;
import com.pawcare.repository.SampleData;
import com.pawcare.util.AppSettings;
import com.pawcare.util.IDGenerator;

import java.util.ArrayList;
import java.util.List;

/**
 * The single entry point the user interface talks to.
 *
 * <p>It owns the individual services, wires them together, loads and saves the data
 * files, and wraps every mutating operation so that a successful change is written to
 * disk straight away. Screens therefore never touch {@link DataStore} directly, and a
 * user can close the window at any moment without losing work.</p>
 *
 * <p>Read-only queries are delegated to the specific service, which the screens obtain
 * from the getters below.</p>
 */
public class ClinicService {

    /** Functional interface used to load one dataset while keeping the recovery logic in one place. */
    @FunctionalInterface
    private interface Loader<T> {
        List<T> load() throws DataAccessException;
    }

    private final DataStore store;
    private final AppSettings settings;

    private final PetService petService = new PetService();
    private final OwnerService ownerService = new OwnerService();
    private final VeterinarianService veterinarianService = new VeterinarianService();
    private final AppointmentService appointmentService = new AppointmentService();
    private final TreatmentService treatmentService = new TreatmentService();
    private final VaccinationService vaccinationService;
    private final SearchService searchService;
    private final ReportService reportService;

    /** Problems encountered while loading, surfaced to the user once the window is up. */
    private final List<String> loadWarnings = new ArrayList<>();

    public ClinicService(DataStore store, AppSettings settings) {
        this.store = store;
        this.settings = settings;
        this.vaccinationService = new VaccinationService(settings);
        this.searchService = new SearchService(petService, ownerService);
        this.reportService = new ReportService(petService, ownerService, veterinarianService,
                appointmentService, treatmentService, vaccinationService);
    }

    // ------------------------------------------------------------------
    // Startup
    // ------------------------------------------------------------------

    /**
     * Loads every dataset, recovering gracefully from problems.
     *
     * <p>A missing file simply means "no records yet". A damaged file is set aside under
     * a {@code .corrupt-<timestamp>} name and reported through {@link #getLoadWarnings()},
     * so the application always starts and the user is told what happened.</p>
     */
    public void loadAll() {
        loadWarnings.clear();

        petService.loadAll(loadOrRecover(store::loadPets, DataStore.PETS_FILE));
        ownerService.loadAll(loadOrRecover(store::loadOwners, DataStore.OWNERS_FILE));
        veterinarianService.loadAll(loadOrRecover(store::loadVeterinarians, DataStore.VETERINARIANS_FILE));
        appointmentService.loadAll(loadOrRecover(store::loadAppointments, DataStore.APPOINTMENTS_FILE));
        treatmentService.loadAll(loadOrRecover(store::loadTreatments, DataStore.TREATMENTS_FILE));
        vaccinationService.loadAll(loadOrRecover(store::loadVaccinations, DataStore.VACCINATIONS_FILE));

        // Nothing stored at all: this is a first run, so seed the demo clinic.
        if (petService.count() == 0 && ownerService.count() == 0 && veterinarianService.count() == 0) {
            seedSampleData();
            try {
                persistAll();
            } catch (DataAccessException e) {
                loadWarnings.add("Demo data could not be saved: " + e.getMessage());
            }
        }
    }

    private <T> List<T> loadOrRecover(Loader<T> loader, String fileName) {
        try {
            return loader.load();
        } catch (DataAccessException e) {
            loadWarnings.add(e.getMessage() + " The file was set aside and the clinic started without it.");
            store.quarantine(fileName);
            return new ArrayList<>();
        }
    }

    /** Populates the demo dataset directly into the services. */
    private void seedSampleData() {
        IDGenerator.reset();
        try {
            for (Owner owner : SampleData.owners()) {
                ownerService.addOwner(owner);
            }
            for (var vet : SampleData.veterinarians()) {
                veterinarianService.add(vet);
            }
            for (Pet pet : SampleData.pets()) {
                petService.addPet(pet);
            }
            for (Appointment appointment : SampleData.appointments()) {
                appointmentService.add(appointment);
            }
            for (Treatment treatment : SampleData.treatments()) {
                treatmentService.addTreatment(treatment);
            }
            for (Vaccination vaccination : SampleData.vaccinations()) {
                vaccinationService.addVaccination(vaccination);
            }
        } catch (PawCareException e) {
            // The bundled sample data is valid by construction; if it ever is not, this
            // message makes the mistake immediately obvious during development.
            throw new IllegalStateException("The bundled sample data is invalid: " + e.getDetailedMessage(), e);
        }
    }

    /** Throws away everything and rebuilds the demo dataset from scratch. */
    public void resetDemoData() throws DataAccessException {
        store.deleteAll();

        IDGenerator.reset();
        petService.loadAll(new ArrayList<>());
        ownerService.loadAll(new ArrayList<>());
        veterinarianService.loadAll(new ArrayList<>());
        appointmentService.loadAll(new ArrayList<>());
        treatmentService.loadAll(new ArrayList<>());
        vaccinationService.loadAll(new ArrayList<>());
        IDGenerator.reset();

        seedSampleData();
        persistAll();
    }

    /** True when the clinic currently holds no pet records at all. */
    public boolean isEmpty() {
        return petService.count() == 0 && ownerService.count() == 0;
    }

    // ------------------------------------------------------------------
    // Persistence
    // ------------------------------------------------------------------

    public void persistPets() throws DataAccessException {
        store.savePets(petService.getAllPets());
    }

    public void persistOwners() throws DataAccessException {
        store.saveOwners(ownerService.getAllOwners());
    }

    public void persistAppointments() throws DataAccessException {
        store.saveAppointments(appointmentService.getAll());
    }

    public void persistTreatments() throws DataAccessException {
        // Flatten the per-pet linked lists into one list for storage; the service
        // rebuilds the maps when the data is read back.
        List<Treatment> flat = new ArrayList<>();
        treatmentService.historyByPet().values().forEach(flat::addAll);
        store.saveTreatments(flat);
    }

    public void persistVaccinations() throws DataAccessException {
        store.saveVaccinations(vaccinationService.getAll());
    }

    public void persistVeterinarians() throws DataAccessException {
        store.saveVeterinarians(veterinarianService.getAll());
    }

    /** Writes every dataset – used after a reset and when the window closes. */
    public void persistAll() throws DataAccessException {
        persistPets();
        persistOwners();
        persistVeterinarians();
        persistAppointments();
        persistTreatments();
        persistVaccinations();
    }

    // ------------------------------------------------------------------
    // Pet operations (validated, then saved immediately)
    // ------------------------------------------------------------------

    public void addPet(Pet pet) throws PawCareException {
        petService.addPet(pet);
        persistPets();
    }

    public void updatePet(Pet pet) throws PawCareException {
        petService.updatePet(pet);
        persistPets();
    }

    /**
     * Deletes a pet together with everything that belongs to it – appointments,
     * treatments and vaccinations – so no orphan records are left behind.
     */
    public void deletePet(String petId) throws PawCareException {
        appointmentService.deleteByPet(petId);
        treatmentService.removeByPet(petId);
        vaccinationService.deleteByPet(petId);
        petService.deletePet(petId);

        persistPets();
        persistAppointments();
        persistTreatments();
        persistVaccinations();
    }

    // ------------------------------------------------------------------
    // Owner operations
    // ------------------------------------------------------------------

    public void addOwner(Owner owner) throws PawCareException {
        ownerService.addOwner(owner);
        persistOwners();
    }

    public void updateOwner(Owner owner) throws PawCareException {
        ownerService.updateOwner(owner);
        persistOwners();
    }

    /**
     * Deletes an owner, but only when no pet is still registered to them. The check lives
     * here because it spans two services.
     */
    public void deleteOwner(String ownerId) throws PawCareException {
        List<Pet> registered = petService.findByOwner(ownerId);
        if (!registered.isEmpty()) {
            List<String> names = new ArrayList<>();
            for (Pet pet : registered) {
                names.add(pet.getName() + " (" + pet.getAnimalId() + ")");
            }
            throw new InvalidOwnerException("Unable to delete owner.",
                    List.of("This owner still has " + registered.size()
                            + " registered pet(s): " + String.join(", ", names)
                            + ". Reassign or delete them first."));
        }
        ownerService.deleteOwner(ownerId);
        persistOwners();
    }

    // ------------------------------------------------------------------
    // Appointment operations
    // ------------------------------------------------------------------

    public void addAppointment(Appointment appointment) throws PawCareException {
        appointmentService.add(appointment);
        persistAppointments();
    }

    public void updateAppointment(Appointment appointment) throws PawCareException {
        appointmentService.update(appointment);
        persistAppointments();
    }

    public void deleteAppointment(String appointmentId) throws PawCareException {
        appointmentService.delete(appointmentId);
        persistAppointments();
    }

    public void completeAppointment(String appointmentId) throws PawCareException {
        appointmentService.markCompleted(appointmentId);
        persistAppointments();
    }

    public void cancelAppointment(String appointmentId) throws PawCareException {
        appointmentService.cancel(appointmentId);
        persistAppointments();
    }

    // ------------------------------------------------------------------
    // Treatment and vaccination operations
    // ------------------------------------------------------------------

    public void addTreatment(Treatment treatment) throws PawCareException {
        treatmentService.addTreatment(treatment);
        persistTreatments();
    }

    public void deleteTreatment(String treatmentId) throws PawCareException {
        treatmentService.removeTreatment(treatmentId);
        persistTreatments();
    }

    public void addVaccination(Vaccination vaccination) throws PawCareException {
        vaccinationService.addVaccination(vaccination);
        persistVaccinations();
    }

    public void updateVaccination(Vaccination vaccination) throws PawCareException {
        vaccinationService.updateVaccination(vaccination);
        persistVaccinations();
    }

    public void deleteVaccination(String vaccinationId) throws PawCareException {
        vaccinationService.delete(vaccinationId);
        persistVaccinations();
    }

    /** Acknowledges a reminder; the flag is stored with the record. */
    public void markVaccinationReviewed(String vaccinationId, boolean reviewed) throws PawCareException {
        vaccinationService.markReviewed(vaccinationId, reviewed);
        persistVaccinations();
    }

    // ------------------------------------------------------------------
    // Accessors
    // ------------------------------------------------------------------

    public PetService pets() {
        return petService;
    }

    public OwnerService owners() {
        return ownerService;
    }

    public VeterinarianService veterinarians() {
        return veterinarianService;
    }

    public AppointmentService appointments() {
        return appointmentService;
    }

    public TreatmentService treatments() {
        return treatmentService;
    }

    public VaccinationService vaccinations() {
        return vaccinationService;
    }

    public SearchService search() {
        return searchService;
    }

    public ReportService reports() {
        return reportService;
    }

    public AppSettings settings() {
        return settings;
    }

    public DataStore store() {
        return store;
    }

    public List<String> getLoadWarnings() {
        return new ArrayList<>(loadWarnings);
    }
}
