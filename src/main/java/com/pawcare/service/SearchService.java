package com.pawcare.service;

import com.pawcare.model.Owner;
import com.pawcare.model.Pet;
import com.pawcare.model.enums.Species;
import com.pawcare.util.ValidationUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Universal pet search.
 *
 * <p>The user picks which field to search, or leaves the scope on {@code ALL} to search
 * every text field at once. Matching is case-insensitive and uses "contains" rather than
 * "equals", so typing {@code bru} finds <i>Bruno</i>.</p>
 *
 * <p>The service depends on {@link PetService} and {@link OwnerService} through
 * composition: searching by owner name has to resolve the owner of each pet, and that
 * is exactly the lookup the owner index provides.</p>
 */
public class SearchService {

    /** The fields a query can be matched against. */
    public enum Scope {

        ALL("All fields"),
        PET_NAME("Pet name"),
        PET_ID("Pet ID"),
        OWNER_NAME("Owner name"),
        SPECIES("Species"),
        BREED("Breed");

        private final String label;

        Scope(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    private final PetService petService;
    private final OwnerService ownerService;

    public SearchService(PetService petService, OwnerService ownerService) {
        this.petService = petService;
        this.ownerService = ownerService;
    }

    /**
     * Runs a search.
     *
     * @param query the text typed by the user; blank returns every pet
     * @param scope which field to match against
     * @return the matching pets, sorted by name
     */
    public List<Pet> search(String query, Scope scope) {
        Scope effectiveScope = scope == null ? Scope.ALL : scope;

        if (ValidationUtil.isBlank(query)) {
            return petService.sortedByName();
        }

        String needle = query.trim().toLowerCase();

        return petService.getAllPets().stream()
                .filter(pet -> matches(pet, needle, effectiveScope))
                .sorted((a, b) -> a.getName().compareToIgnoreCase(b.getName()))
                .collect(Collectors.toList());
    }

    /** Returns true when the pet satisfies the query in the chosen scope. */
    private boolean matches(Pet pet, String needle, Scope scope) {
        switch (scope) {
            case PET_NAME:
                return contains(pet.getName(), needle);
            case PET_ID:
                return contains(pet.getAnimalId(), needle);
            case BREED:
                return contains(pet.getBreed(), needle);
            case SPECIES:
                // Matching the enum label means "d" finds every dog.
                return contains(pet.getSpecies().getLabel(), needle);
            case OWNER_NAME:
                return contains(ownerName(pet), needle);
            case ALL:
            default:
                return contains(pet.getName(), needle)
                        || contains(pet.getAnimalId(), needle)
                        || contains(pet.getBreed(), needle)
                        || contains(pet.getSpecies().getLabel(), needle)
                        || contains(ownerName(pet), needle)
                        || contains(pet.getOwnerId(), needle);
        }
    }

    private String ownerName(Pet pet) {
        return ownerService.findById(pet.getOwnerId()).map(Owner::getName).orElse("");
    }

    private static boolean contains(String value, String needle) {
        return value != null && value.toLowerCase().contains(needle);
    }

    /** Free-text owner search used by the owner picker in the pet dialog. */
    public List<Owner> searchOwners(String query) {
        if (ValidationUtil.isBlank(query)) {
            return ownerService.sortedByName();
        }
        String needle = query.trim().toLowerCase();
        List<Owner> results = new ArrayList<>();
        for (Owner owner : ownerService.getAllOwners()) {
            boolean hit = contains(owner.getName(), needle)
                    || contains(owner.getOwnerId(), needle)
                    || contains(owner.getPhone(), needle)
                    || contains(owner.getEmail(), needle);
            if (hit) {
                results.add(owner);
            }
        }
        results.sort((a, b) -> a.getName().compareToIgnoreCase(b.getName()));
        return results;
    }

    /** Species label helper used by the search page's summary line. */
    public static String describe(Pet pet) {
        return pet.getName() + " (" + pet.getSpecies().getLabel() + ")";
    }

    /** Convenience for report tiles that need a species breakdown of search results. */
    public static long countSpecies(List<Pet> pets, Species species) {
        return pets.stream().filter(p -> p.getSpecies() == species).count();
    }
}
