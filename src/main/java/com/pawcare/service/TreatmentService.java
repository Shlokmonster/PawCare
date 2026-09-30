package com.pawcare.service;

import com.pawcare.exception.InvalidTreatmentException;
import com.pawcare.model.Treatment;
import com.pawcare.util.IDGenerator;
import com.pawcare.util.ValidationUtil;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Keeps the clinical history of every pet.
 *
 * <p>History is stored as a {@code HashMap<String, LinkedList<Treatment>>}: one linked
 * list per pet id.</p>
 *
 * <p>A LinkedList is the right choice here because a medical history grows by appending
 * at the end, is walked from start to finish when it is displayed, and occasionally has
 * an individual entry removed. Appending to a linked list is O(1) – no array has to be
 * shifted or resized – and removing an element is a pure pointer update. That is
 * precisely the access pattern a treatment log has, whereas random access by index
 * (the ArrayList strength) is never needed.</p>
 */
public class TreatmentService {

    /** pet id -> that pet's treatment history, oldest entry first. */
    private final HashMap<String, LinkedList<Treatment>> historyByPet = new HashMap<>();

    /** treatment id -> treatment, so an entry can be found without scanning every list. */
    private final HashMap<String, Treatment> byId = new HashMap<>();

    // CREATE ----------------------------------------------------------

    /** Appends a validated treatment to the pet's history. */
    public void addTreatment(Treatment treatment) throws InvalidTreatmentException {
        ValidationUtil.validateTreatment(treatment);

        if (byId.containsKey(treatment.getTreatmentId())) {
            throw new InvalidTreatmentException("Unable to save treatment record.",
                    List.of("Treatment id " + treatment.getTreatmentId() + " already exists."));
        }

        historyByPet
                .computeIfAbsent(treatment.getPetId(), key -> new LinkedList<>())
                .addLast(treatment);   // O(1) append at the tail

        byId.put(treatment.getTreatmentId(), treatment);
        IDGenerator.sync(IDGenerator.TREATMENT_PREFIX, treatment.getTreatmentId());
    }

    // READ ------------------------------------------------------------

    /**
     * The pet's history in chronological order.
     *
     * <p>The list is copied and then explicitly sorted by date so the display order does
     * not depend on the order the records happened to be entered in.</p>
     */
    public LinkedList<Treatment> getTreatmentHistory(String petId) {
        LinkedList<Treatment> history = historyByPet.get(petId);
        if (history == null) {
            return new LinkedList<>();
        }
        LinkedList<Treatment> copy = new LinkedList<>(history);
        copy.sort(Treatment.BY_DATE);
        return copy;
    }

    public Optional<Treatment> findById(String treatmentId) {
        if (treatmentId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(byId.get(treatmentId.trim()));
    }

    /** Every treatment in the clinic, newest first – used by the dashboard and reports. */
    public List<Treatment> allNewestFirst() {
        List<Treatment> all = new ArrayList<>(byId.values());
        all.sort(Comparator.comparing(Treatment::getDate,
                Comparator.nullsLast(Comparator.naturalOrder())).reversed());
        return all;
    }

    /** The most recent treatments across all pets. */
    public List<Treatment> recent(int limit) {
        return allNewestFirst().stream().limit(limit).collect(Collectors.toList());
    }

    public int count() {
        return byId.size();
    }

    public int countForPet(String petId) {
        LinkedList<Treatment> history = historyByPet.get(petId);
        return history == null ? 0 : history.size();
    }

    /** A compact summary of the treatments recorded in the last thirty days. */
    public int countRecent(int days) {
        java.time.LocalDate cutoff = java.time.LocalDate.now().minusDays(days);
        return (int) byId.values().stream()
                .filter(t -> t.getDate() != null && !t.getDate().isBefore(cutoff))
                .count();
    }

    // DELETE ----------------------------------------------------------

    /**
     * Removes one treatment entry.
     *
     * @return true when the entry existed and was removed
     */
    public boolean removeTreatment(String treatmentId) {
        Treatment removed = byId.remove(treatmentId);
        if (removed == null) {
            return false;
        }
        LinkedList<Treatment> history = historyByPet.get(removed.getPetId());
        if (history != null) {
            history.remove(removed);   // pointer update, no shifting
            if (history.isEmpty()) {
                historyByPet.remove(removed.getPetId());
            }
        }
        return true;
    }

    /** Removes the whole history of a pet – used when a pet is deleted. */
    public int removeByPet(String petId) {
        LinkedList<Treatment> history = historyByPet.remove(petId);
        if (history == null) {
            return 0;
        }
        int removed = 0;
        for (Treatment treatment : history) {
            if (byId.remove(treatment.getTreatmentId()) != null) {
                removed++;
            }
        }
        return removed;
    }

    // BULK ------------------------------------------------------------

    /** Rebuilds both indexes from a flat list read off disk. */
    public void loadAll(List<Treatment> loaded) {
        historyByPet.clear();
        byId.clear();
        if (loaded == null) {
            return;
        }
        for (Treatment treatment : loaded) {
            if (treatment == null || treatment.getTreatmentId() == null
                    || treatment.getPetId() == null) {
                continue;
            }
            byId.put(treatment.getTreatmentId(), treatment);
            historyByPet
                    .computeIfAbsent(treatment.getPetId(), key -> new LinkedList<>())
                    .addLast(treatment);
        }
        IDGenerator.sync(IDGenerator.TREATMENT_PREFIX,
                byId.keySet().stream().collect(Collectors.toList()));
    }

    /** A read-only view of the underlying map, handy for reporting. */
    public Map<String, LinkedList<Treatment>> historyByPet() {
        return historyByPet;
    }
}
