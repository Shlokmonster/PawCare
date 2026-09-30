package com.pawcare.model.enums;

/**
 * A pet's vaccination-derived status, shown in the Status column of the pet table.
 *
 * <p>It is derived rather than stored: it is calculated from the pet's vaccination
 * records each time the table is built, so it can never go stale.</p>
 */
public enum PetHealthStatus {

    OVERDUE("Overdue"),
    DUE_SOON("Due soon"),
    UP_TO_DATE("Up to date"),
    NO_RECORDS("No records");

    private final String label;

    PetHealthStatus(String label) {
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
