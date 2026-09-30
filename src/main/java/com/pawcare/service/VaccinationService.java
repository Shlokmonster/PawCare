package com.pawcare.service;

import com.pawcare.exception.InvalidVaccinationException;
import com.pawcare.model.Vaccination;
import com.pawcare.model.enums.PetHealthStatus;
import com.pawcare.model.enums.VaccinationStatus;
import com.pawcare.util.AppSettings;
import com.pawcare.util.IDGenerator;
import com.pawcare.util.ValidationUtil;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Vaccination records and the reminder logic built on top of them.
 *
 * <p>Records live in an {@code ArrayList}, with a {@code HashMap<String, ArrayList>}
 * index by pet id so a pet's vaccination card can be fetched without scanning the whole
 * clinic. Reminder classification is delegated to {@link VaccinationStatus} so the rule
 * has exactly one definition, and the "due soon" window comes from the user's saved
 * preference rather than a hard-coded constant.</p>
 */
public class VaccinationService {

    private final ArrayList<Vaccination> vaccinations = new ArrayList<>();
    private final HashMap<String, ArrayList<Vaccination>> byPet = new HashMap<>();
    private final HashMap<String, Vaccination> byId = new HashMap<>();

    private final AppSettings settings;

    public VaccinationService(AppSettings settings) {
        this.settings = settings;
    }

    /** The reminder window in days – configurable from the Settings page. */
    public int getReminderDays() {
        return settings.getReminderDays();
    }

    // CREATE / UPDATE / DELETE ----------------------------------------

    public void addVaccination(Vaccination vaccination) throws InvalidVaccinationException {
        ValidationUtil.validateVaccination(vaccination);

        if (byId.containsKey(vaccination.getVaccinationId())) {
            throw new InvalidVaccinationException("Unable to save vaccination.",
                    List.of("Vaccination id " + vaccination.getVaccinationId() + " already exists."));
        }

        vaccinations.add(vaccination);
        byId.put(vaccination.getVaccinationId(), vaccination);
        byPet.computeIfAbsent(vaccination.getPetId(), key -> new ArrayList<>()).add(vaccination);
        IDGenerator.sync(IDGenerator.VACCINATION_PREFIX, vaccination.getVaccinationId());
    }

    public void updateVaccination(Vaccination vaccination) throws InvalidVaccinationException {
        ValidationUtil.validateVaccination(vaccination);

        if (!byId.containsKey(vaccination.getVaccinationId())) {
            throw new InvalidVaccinationException("Unable to update vaccination.",
                    List.of("Vaccination id " + vaccination.getVaccinationId() + " does not exist."));
        }

        byId.put(vaccination.getVaccinationId(), vaccination);
        int index = indexOf(vaccination.getVaccinationId());
        if (index >= 0) {
            vaccinations.set(index, vaccination);
        }
        reindexByPet();
    }

    /** Records that the clinic has acknowledged a reminder. */
    public boolean markReviewed(String vaccinationId, boolean reviewed) {
        Vaccination vaccination = byId.get(vaccinationId);
        if (vaccination == null) {
            return false;
        }
        vaccination.setReviewed(reviewed);
        return true;
    }

    public boolean delete(String vaccinationId) {
        Vaccination removed = byId.remove(vaccinationId);
        if (removed == null) {
            return false;
        }
        vaccinations.remove(removed);
        reindexByPet();
        return true;
    }

    public int deleteByPet(String petId) {
        List<Vaccination> doomed = vaccinations.stream()
                .filter(v -> petId != null && petId.equals(v.getPetId()))
                .collect(Collectors.toList());
        doomed.forEach(v -> {
            vaccinations.remove(v);
            byId.remove(v.getVaccinationId());
        });
        reindexByPet();
        return doomed.size();
    }

    // READ ------------------------------------------------------------

    public List<Vaccination> getAll() {
        return new ArrayList<>(vaccinations);
    }

    public Optional<Vaccination> findById(String vaccinationId) {
        if (vaccinationId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(byId.get(vaccinationId.trim()));
    }

    /** Every vaccination belonging to one pet. */
    public List<Vaccination> forPet(String petId) {
        ArrayList<Vaccination> list = byPet.get(petId);
        if (list == null) {
            return new ArrayList<>();
        }
        List<Vaccination> copy = new ArrayList<>(list);
        copy.sort(Vaccination.BY_NEXT_DUE);
        return copy;
    }

    /** The most recent dose given to a pet, shown on the pet profile. */
    public Optional<Vaccination> latestForPet(String petId) {
        return forPet(petId).stream()
                .filter(v -> v.getVaccinationDate() != null)
                .max(Comparator.comparing(Vaccination::getVaccinationDate));
    }

    /** Classifies one record using the configured reminder window. */
    public VaccinationStatus statusOf(Vaccination vaccination) {
        return vaccination.statusOn(LocalDate.now(), getReminderDays());
    }

    public List<Vaccination> withStatus(VaccinationStatus status) {
        return vaccinations.stream()
                .filter(v -> statusOf(v) == status)
                .sorted(Vaccination.BY_NEXT_DUE)
                .collect(Collectors.toList());
    }

    /**
     * Records that need attention now: everything overdue plus everything falling inside
     * the reminder window, ordered by how soon it is due.
     */
    public List<Vaccination> alerts() {
        return vaccinations.stream()
                .filter(v -> {
                    VaccinationStatus status = statusOf(v);
                    return status == VaccinationStatus.OVERDUE || status == VaccinationStatus.DUE_SOON;
                })
                .sorted(Vaccination.BY_NEXT_DUE)
                .collect(Collectors.toList());
    }

    /** Counts per status, in enum declaration order, ready for the summary cards. */
    public Map<VaccinationStatus, Integer> countByStatus() {
        Map<VaccinationStatus, Integer> counts = new EnumMap<>(VaccinationStatus.class);
        for (VaccinationStatus status : VaccinationStatus.values()) {
            counts.put(status, 0);
        }
        for (Vaccination vaccination : vaccinations) {
            counts.merge(statusOf(vaccination), 1, Integer::sum);
        }
        return counts;
    }

    public int count() {
        return vaccinations.size();
    }

    public int countOverdue() {
        return countByStatus().getOrDefault(VaccinationStatus.OVERDUE, 0);
    }

    public int countDueSoon() {
        return countByStatus().getOrDefault(VaccinationStatus.DUE_SOON, 0);
    }

    /**
     * Derives the status shown for a pet in the pet table.
     *
     * <p>The worst state wins: any overdue record makes the pet "Overdue", otherwise a
     * record inside the reminder window makes it "Due soon", and a pet with no records at
     * all is reported separately rather than being called healthy.</p>
     */
    public PetHealthStatus statusForPet(String petId) {
        List<Vaccination> records = forPet(petId);
        if (records.isEmpty()) {
            return PetHealthStatus.NO_RECORDS;
        }
        boolean dueSoon = false;
        for (Vaccination vaccination : records) {
            VaccinationStatus status = statusOf(vaccination);
            if (status == VaccinationStatus.OVERDUE) {
                return PetHealthStatus.OVERDUE;
            }
            if (status == VaccinationStatus.DUE_SOON) {
                dueSoon = true;
            }
        }
        return dueSoon ? PetHealthStatus.DUE_SOON : PetHealthStatus.UP_TO_DATE;
    }

    /** Number of pets that have at least one overdue or due-soon vaccination. */
    public int petsNeedingAttention() {
        return (int) alerts().stream().map(Vaccination::getPetId).distinct().count();
    }

    private int indexOf(String vaccinationId) {
        for (int i = 0; i < vaccinations.size(); i++) {
            if (vaccinations.get(i).getVaccinationId().equals(vaccinationId)) {
                return i;
            }
        }
        return -1;
    }

    private void reindexByPet() {
        byPet.clear();
        for (Vaccination vaccination : vaccinations) {
            byPet.computeIfAbsent(vaccination.getPetId(), key -> new ArrayList<>()).add(vaccination);
        }
    }

    // BULK ------------------------------------------------------------

    public void loadAll(List<Vaccination> loaded) {
        vaccinations.clear();
        byId.clear();
        byPet.clear();
        if (loaded != null) {
            for (Vaccination vaccination : loaded) {
                if (vaccination == null || vaccination.getVaccinationId() == null) {
                    continue;
                }
                vaccinations.add(vaccination);
                byId.put(vaccination.getVaccinationId(), vaccination);
            }
        }
        reindexByPet();
        IDGenerator.sync(IDGenerator.VACCINATION_PREFIX,
                byId.keySet().stream().collect(Collectors.toList()));
    }
}
