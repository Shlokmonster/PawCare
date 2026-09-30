package com.pawcare.service;

import com.pawcare.exception.EntityNotFoundException;
import com.pawcare.exception.InvalidPetException;
import com.pawcare.model.Pet;
import com.pawcare.model.enums.Species;
import com.pawcare.util.IDGenerator;
import com.pawcare.util.ValidationUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * In-memory store and business rules for pet records.
 *
 * <p>Two collections work together deliberately:</p>
 * <ul>
 *   <li>{@code ArrayList<Pet>} keeps the records in insertion order and supports the
 *       indexed iteration, searching and sorting that the tables need.</li>
 *   <li>{@code HashMap<String, Pet>} indexes the same objects by pet id so a lookup by
 *       id is a constant-time hash probe instead of a linear scan.</li>
 * </ul>
 *
 * <p>Both are updated together, so they can never drift apart.</p>
 */
public class PetService {

    /** Primary storage, ordered as records were added. */
    private final ArrayList<Pet> pets = new ArrayList<>();

    /** Secondary index: pet id -> pet, for O(1) lookup. */
    private final HashMap<String, Pet> petById = new HashMap<>();

    // ------------------------------------------------------------------
    // CREATE
    // ------------------------------------------------------------------

    /**
     * Validates and stores a new pet.
     *
     * @throws InvalidPetException when the record breaks a validation rule or the id is taken
     */
    public void addPet(Pet pet) throws InvalidPetException {
        ValidationUtil.validatePet(pet);

        if (petById.containsKey(pet.getAnimalId())) {
            throw new InvalidPetException("Unable to register pet.",
                    List.of("Pet id " + pet.getAnimalId() + " is already in use."));
        }

        pets.add(pet);
        petById.put(pet.getAnimalId(), pet);
        // Keep the generator ahead of anything already stored (relevant after loading data).
        IDGenerator.sync(IDGenerator.PET_PREFIX, pet.getAnimalId());
    }

    // ------------------------------------------------------------------
    // READ
    // ------------------------------------------------------------------

    /**
     * Fast id lookup through the HashMap.
     *
     * @return the pet, or an empty Optional when no such id exists
     */
    public Optional<Pet> findPetById(String petId) {
        if (petId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(petById.get(petId.trim()));
    }

    /** Same lookup, but failing loudly – convenient inside the GUI layer. */
    public Pet requirePet(String petId) throws EntityNotFoundException {
        return findPetById(petId).orElseThrow(
                () -> new EntityNotFoundException("No pet found with id " + petId + "."));
    }

    public String petName(String petId) {
        return findPetById(petId).map(Pet::getName).orElse("Unknown pet");
    }

    /** Defensive copy, so callers cannot mutate the internal list. */
    public List<Pet> getAllPets() {
        return new ArrayList<>(pets);
    }

    public int count() {
        return pets.size();
    }

    public boolean exists(String petId) {
        return petId != null && petById.containsKey(petId.trim());
    }

    public List<Pet> findByOwner(String ownerId) {
        if (ownerId == null) {
            return new ArrayList<>();
        }
        return pets.stream()
                .filter(p -> ownerId.equals(p.getOwnerId()))
                .collect(Collectors.toList());
    }

    public List<Pet> findBySpecies(Species species) {
        return pets.stream()
                .filter(p -> p.getSpecies() == species)
                .collect(Collectors.toList());
    }

    public Map<Species, Integer> countBySpecies() {
        Map<Species, Integer> counts = new java.util.EnumMap<>(Species.class);
        for (Species species : Species.values()) {
            counts.put(species, 0);
        }
        for (Pet pet : pets) {
            counts.merge(pet.getSpecies(), 1, Integer::sum);
        }
        return counts;
    }

    // ------------------------------------------------------------------
    // UPDATE / DELETE
    // ------------------------------------------------------------------

    /** Replaces the stored record that shares this pet's id. */
    public void updatePet(Pet pet) throws InvalidPetException {
        ValidationUtil.validatePet(pet);

        int index = indexOf(pet.getAnimalId());
        if (index < 0) {
            throw new InvalidPetException("Unable to update pet.",
                    List.of("Pet id " + pet.getAnimalId() + " does not exist."));
        }

        pets.set(index, pet);
        petById.put(pet.getAnimalId(), pet);
    }

    /**
     * Removes a pet.
     *
     * @return true when a record was removed
     */
    public boolean deletePet(String petId) {
        if (petId == null) {
            return false;
        }
        Pet removed = petById.remove(petId.trim());
        if (removed == null) {
            return false;
        }
        pets.remove(removed);
        return true;
    }

    private int indexOf(String petId) {
        for (int i = 0; i < pets.size(); i++) {
            if (pets.get(i).getAnimalId().equals(petId)) {
                return i;
            }
        }
        return -1;
    }

    // ------------------------------------------------------------------
    // Sorting – Comparator, List.sort and Collections.sort
    // ------------------------------------------------------------------

    /** Returns a new list sorted with the supplied comparator. */
    public List<Pet> sortedBy(Comparator<Pet> comparator) {
        List<Pet> copy = new ArrayList<>(pets);
        copy.sort(comparator);   // List.sort, the modern form
        return copy;
    }

    /** Demonstrates the classic Collections.sort API. */
    public List<Pet> sortedByName() {
        List<Pet> copy = new ArrayList<>(pets);
        Collections.sort(copy, Comparator.comparing(Pet::getName, String.CASE_INSENSITIVE_ORDER));
        return copy;
    }

    /** Sorting by a computed key: age descending, so the oldest animals come first. */
    public List<Pet> sortedByAgeDescending() {
        return sortedBy(Comparator.comparingInt(Pet::getAge).reversed());
    }

    // ------------------------------------------------------------------
    // Bulk operations used by loading and resetting
    // ------------------------------------------------------------------

    /** Replaces the whole dataset, rebuilding the HashMap index. */
    public void loadAll(List<Pet> loaded) {
        pets.clear();
        petById.clear();
        if (loaded == null) {
            return;
        }
        for (Pet pet : loaded) {
            if (pet == null || pet.getAnimalId() == null) {
                continue;
            }
            pets.add(pet);
            petById.put(pet.getAnimalId(), pet);
        }
        IDGenerator.sync(IDGenerator.PET_PREFIX,
                pets.stream().map(Pet::getAnimalId).collect(Collectors.toList()));
    }
}
