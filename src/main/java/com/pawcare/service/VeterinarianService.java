package com.pawcare.service;

import com.pawcare.exception.EntityNotFoundException;
import com.pawcare.model.Veterinarian;
import com.pawcare.util.IDGenerator;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * In-memory store for the clinic's veterinarians.
 *
 * <p>Veterinarians are reference data: the application ships with a fixed panel of
 * doctors, so this service only needs read access plus the ability to add a doctor
 * should the clinic grow.</p>
 */
public class VeterinarianService {

    private final ArrayList<Veterinarian> veterinarians = new ArrayList<>();
    private final HashMap<String, Veterinarian> byId = new HashMap<>();

    public void add(Veterinarian veterinarian) {
        veterinarians.add(veterinarian);
        byId.put(veterinarian.getVeterinarianId(), veterinarian);
        IDGenerator.sync(IDGenerator.VET_PREFIX, veterinarian.getVeterinarianId());
    }

    /** Constant-time lookup by veterinarian id. */
    public Optional<Veterinarian> findById(String veterinarianId) {
        if (veterinarianId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(byId.get(veterinarianId.trim()));
    }

    public Veterinarian requireVet(String veterinarianId) throws EntityNotFoundException {
        return findById(veterinarianId).orElseThrow(
                () -> new EntityNotFoundException("No veterinarian found with id " + veterinarianId + "."));
    }

    public String vetName(String veterinarianId) {
        return findById(veterinarianId).map(Veterinarian::getName).orElse("Unassigned");
    }

    public List<Veterinarian> getAll() {
        return new ArrayList<>(veterinarians);
    }

    public List<Veterinarian> sortedByName() {
        List<Veterinarian> copy = new ArrayList<>(veterinarians);
        copy.sort(Comparator.comparing(Veterinarian::getName, String.CASE_INSENSITIVE_ORDER));
        return copy;
    }

    public int count() {
        return veterinarians.size();
    }

    public void loadAll(List<Veterinarian> loaded) {
        veterinarians.clear();
        byId.clear();
        if (loaded == null) {
            return;
        }
        for (Veterinarian vet : loaded) {
            if (vet == null || vet.getVeterinarianId() == null) {
                continue;
            }
            veterinarians.add(vet);
            byId.put(vet.getVeterinarianId(), vet);
        }
        IDGenerator.sync(IDGenerator.VET_PREFIX,
                veterinarians.stream().map(Veterinarian::getVeterinarianId).collect(Collectors.toList()));
    }
}
