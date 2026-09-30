package com.pawcare.service;

import com.pawcare.exception.EntityNotFoundException;
import com.pawcare.exception.InvalidOwnerException;
import com.pawcare.model.Owner;
import com.pawcare.util.IDGenerator;
import com.pawcare.util.ValidationUtil;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * In-memory store for pet owners.
 *
 * <p>Uses the same {@code ArrayList} + {@code HashMap} pairing as {@link PetService}:
 * the list preserves order for the table, the map makes lookup by owner id instant.</p>
 */
public class OwnerService {

    private final ArrayList<Owner> owners = new ArrayList<>();
    private final HashMap<String, Owner> ownerById = new HashMap<>();

    // CREATE ----------------------------------------------------------

    public void addOwner(Owner owner) throws InvalidOwnerException {
        ValidationUtil.validateOwner(owner);

        if (ownerById.containsKey(owner.getOwnerId())) {
            throw new InvalidOwnerException("Unable to save owner.",
                    List.of("Owner id " + owner.getOwnerId() + " is already in use."));
        }

        owners.add(owner);
        ownerById.put(owner.getOwnerId(), owner);
        IDGenerator.sync(IDGenerator.OWNER_PREFIX, owner.getOwnerId());
    }

    // READ ------------------------------------------------------------

    /** Constant-time lookup by owner id. */
    public Optional<Owner> findById(String ownerId) {
        if (ownerId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(ownerById.get(ownerId.trim()));
    }

    public Owner requireOwner(String ownerId) throws EntityNotFoundException {
        return findById(ownerId).orElseThrow(
                () -> new EntityNotFoundException("No owner found with id " + ownerId + "."));
    }

    /** Friendly owner name for tables, tolerating a missing owner id. */
    public String ownerName(String ownerId) {
        return findById(ownerId).map(Owner::getName).orElse("Unassigned");
    }

    public List<Owner> getAllOwners() {
        return new ArrayList<>(owners);
    }

    public List<Owner> searchByName(String keyword) {
        if (ValidationUtil.isBlank(keyword)) {
            return getAllOwners();
        }
        String needle = keyword.trim().toLowerCase();
        return owners.stream()
                .filter(o -> o.getName() != null && o.getName().toLowerCase().contains(needle))
                .collect(Collectors.toList());
    }

    public List<Owner> sortedByName() {
        List<Owner> copy = new ArrayList<>(owners);
        copy.sort(Comparator.comparing(Owner::getName, String.CASE_INSENSITIVE_ORDER));
        return copy;
    }

    public int count() {
        return owners.size();
    }

    public boolean exists(String ownerId) {
        return ownerId != null && ownerById.containsKey(ownerId.trim());
    }

    // UPDATE / DELETE -------------------------------------------------

    public void updateOwner(Owner owner) throws InvalidOwnerException {
        ValidationUtil.validateOwner(owner);

        int index = indexOf(owner.getOwnerId());
        if (index < 0) {
            throw new InvalidOwnerException("Unable to save owner.",
                    List.of("Owner id " + owner.getOwnerId() + " does not exist."));
        }

        owners.set(index, owner);
        ownerById.put(owner.getOwnerId(), owner);
    }

    public boolean deleteOwner(String ownerId) {
        if (ownerId == null) {
            return false;
        }
        Owner removed = ownerById.remove(ownerId.trim());
        if (removed == null) {
            return false;
        }
        owners.remove(removed);
        return true;
    }

    private int indexOf(String ownerId) {
        for (int i = 0; i < owners.size(); i++) {
            if (owners.get(i).getOwnerId().equals(ownerId)) {
                return i;
            }
        }
        return -1;
    }

    // BULK ------------------------------------------------------------

    public void loadAll(List<Owner> loaded) {
        owners.clear();
        ownerById.clear();
        if (loaded == null) {
            return;
        }
        for (Owner owner : loaded) {
            if (owner == null || owner.getOwnerId() == null) {
                continue;
            }
            owners.add(owner);
            ownerById.put(owner.getOwnerId(), owner);
        }
        IDGenerator.sync(IDGenerator.OWNER_PREFIX,
                owners.stream().map(Owner::getOwnerId).collect(Collectors.toList()));
    }
}
