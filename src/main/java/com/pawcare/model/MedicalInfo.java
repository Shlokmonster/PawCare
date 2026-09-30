package com.pawcare.model;

import java.io.Serializable;

/**
 * Medical background of a pet.
 *
 * <p>This class demonstrates <b>composition</b>: a {@link Pet} is not a kind of
 * medical record, it <i>has</i> one. Keeping the medical details in their own object
 * means they can be validated, replaced or extended without touching the animal
 * hierarchy.</p>
 */
public class MedicalInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    private String allergies;
    private String chronicConditions;
    private String notes;

    public MedicalInfo() {
        this("None recorded", "None recorded", "");
    }

    public MedicalInfo(String allergies, String chronicConditions, String notes) {
        this.allergies = allergies;
        this.chronicConditions = chronicConditions;
        this.notes = notes;
    }

    public String getAllergies() {
        return allergies;
    }

    public void setAllergies(String allergies) {
        this.allergies = allergies;
    }

    public String getChronicConditions() {
        return chronicConditions;
    }

    public void setChronicConditions(String chronicConditions) {
        this.chronicConditions = chronicConditions;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    /** Short summary used by the pet profile screen. */
    public String summary() {
        return "Allergies: " + safe(allergies)
                + " | Chronic conditions: " + safe(chronicConditions);
    }

    private static String safe(String value) {
        return (value == null || value.isBlank()) ? "None recorded" : value;
    }

    @Override
    public String toString() {
        return summary();
    }
}
