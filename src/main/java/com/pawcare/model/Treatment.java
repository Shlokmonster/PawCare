package com.pawcare.model;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.Objects;

/**
 * A single entry in a pet's medical history.
 *
 * <p>Treatments are held in a {@code LinkedList<Treatment>} per pet. The class
 * implements {@link Comparable} so that the natural order is chronological, and the
 * service layer additionally uses {@link Comparator}s when a different order is
 * required – a good illustration of the difference between the two.</p>
 */
public class Treatment implements Serializable, Comparable<Treatment> {

    private static final long serialVersionUID = 1L;

    /** Natural ordering: oldest first, ties broken by treatment id. */
    public static final Comparator<Treatment> BY_DATE =
            Comparator.comparing(Treatment::getDate, Comparator.nullsLast(Comparator.naturalOrder()))
                    .thenComparing(Treatment::getTreatmentId,
                            Comparator.nullsLast(Comparator.naturalOrder()));

    private String treatmentId;
    private String petId;
    private String veterinarianId;
    private LocalDate date;
    private String diagnosis;
    private String treatment;
    private String medication;
    private String notes;

    public Treatment() {
    }

    public Treatment(String treatmentId, String petId, String veterinarianId, LocalDate date,
                     String diagnosis, String treatment, String medication, String notes) {
        this.treatmentId = treatmentId;
        this.petId = petId;
        this.veterinarianId = veterinarianId;
        this.date = date;
        this.diagnosis = diagnosis;
        this.treatment = treatment;
        this.medication = medication;
        this.notes = notes;
    }

    public String displayInfo() {
        return date + " • " + diagnosis + " • " + treatment;
    }

    public String getTreatmentId() {
        return treatmentId;
    }

    public void setTreatmentId(String treatmentId) {
        this.treatmentId = treatmentId;
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

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public String getDiagnosis() {
        return diagnosis;
    }

    public void setDiagnosis(String diagnosis) {
        this.diagnosis = diagnosis;
    }

    public String getTreatment() {
        return treatment;
    }

    public void setTreatment(String treatment) {
        this.treatment = treatment;
    }

    public String getMedication() {
        return medication;
    }

    public void setMedication(String medication) {
        this.medication = medication;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    @Override
    public int compareTo(Treatment other) {
        return BY_DATE.compare(this, other);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Treatment)) {
            return false;
        }
        return Objects.equals(treatmentId, ((Treatment) other).treatmentId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(treatmentId);
    }

    @Override
    public String toString() {
        return treatmentId;
    }
}
