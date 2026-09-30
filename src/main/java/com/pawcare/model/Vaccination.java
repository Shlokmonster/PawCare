package com.pawcare.model;

import com.pawcare.model.enums.VaccinationStatus;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.Objects;

/** A vaccination dose given to a pet, together with the date the next dose is due. */
public class Vaccination implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Ordering used when the vaccination table is sorted by urgency. */
    public static final Comparator<Vaccination> BY_NEXT_DUE =
            Comparator.comparing(Vaccination::getNextDueDate,
                    Comparator.nullsLast(Comparator.naturalOrder()));

    private String vaccinationId;
    private String petId;
    private String veterinarianId;
    private String vaccineName;
    private LocalDate vaccinationDate;
    private LocalDate nextDueDate;
    private String notes;

    /** Set when the clinic has acknowledged the reminder for this record. */
    private boolean reviewed;

    public Vaccination() {
    }

    public Vaccination(String vaccinationId, String petId, String veterinarianId, String vaccineName,
                       LocalDate vaccinationDate, LocalDate nextDueDate, String notes) {
        this.vaccinationId = vaccinationId;
        this.petId = petId;
        this.veterinarianId = veterinarianId;
        this.vaccineName = vaccineName;
        this.vaccinationDate = vaccinationDate;
        this.nextDueDate = nextDueDate;
        this.notes = notes;
    }

    /**
     * Classifies this record against a reference date and reminder window.
     * Delegates to the enum so the rule lives in exactly one place.
     */
    public VaccinationStatus statusOn(LocalDate today, int reminderDays) {
        return VaccinationStatus.of(nextDueDate, today, reminderDays);
    }

    public String displayInfo() {
        return vaccineName + " • given " + vaccinationDate + " • next due " + nextDueDate;
    }

    public String getVaccinationId() {
        return vaccinationId;
    }

    public void setVaccinationId(String vaccinationId) {
        this.vaccinationId = vaccinationId;
    }

    public String getPetId() {
        return petId;
    }

    public void setPetId(String petId) {
        this.petId = petId;
    }

    public String getVeterinarianId() {
        return veterinarianId;
    }

    public void setVeterinarianId(String veterinarianId) {
        this.veterinarianId = veterinarianId;
    }

    public String getVaccineName() {
        return vaccineName;
    }

    public void setVaccineName(String vaccineName) {
        this.vaccineName = vaccineName;
    }

    public LocalDate getVaccinationDate() {
        return vaccinationDate;
    }

    public void setVaccinationDate(LocalDate vaccinationDate) {
        this.vaccinationDate = vaccinationDate;
    }

    public LocalDate getNextDueDate() {
        return nextDueDate;
    }

    public void setNextDueDate(LocalDate nextDueDate) {
        this.nextDueDate = nextDueDate;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public boolean isReviewed() {
        return reviewed;
    }

    public void setReviewed(boolean reviewed) {
        this.reviewed = reviewed;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Vaccination)) {
            return false;
        }
        return Objects.equals(vaccinationId, ((Vaccination) other).vaccinationId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(vaccinationId);
    }

    @Override
    public String toString() {
        return vaccinationId;
    }
}
