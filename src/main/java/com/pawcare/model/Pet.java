package com.pawcare.model;

import com.pawcare.model.enums.Gender;

import java.time.LocalDate;

/**
 * A registered patient of the clinic.
 *
 * <p>{@code Pet} sits between {@link Animal} and the concrete species, forming a
 * three-level hierarchy: {@code Animal -> Pet -> Dog/Cat/Bird}. It contributes the
 * information that only makes sense for an owned animal – the owner, the registration
 * date and the medical record – while leaving {@link Animal#treatmentPlan()} abstract
 * for the species to implement.</p>
 *
 * <p>The owner is associated by id rather than by object reference. That keeps the
 * serialized data normalised: renaming an owner updates one record instead of every
 * pet that points at them.</p>
 */
public abstract class Pet extends Animal {

    private static final long serialVersionUID = 1L;

    private String ownerId;
    private LocalDate registrationDate;
    private MedicalInfo medicalInfo;

    protected Pet() {
        super();
        this.medicalInfo = new MedicalInfo();
        this.registrationDate = LocalDate.now();
    }

    protected Pet(String animalId, String name, int age, double weight, Gender gender, String breed,
                  String ownerId, LocalDate registrationDate, MedicalInfo medicalInfo) {
        super(animalId, name, age, weight, gender, breed);
        this.ownerId = ownerId;
        this.registrationDate = registrationDate;
        this.medicalInfo = medicalInfo == null ? new MedicalInfo() : medicalInfo;
    }

    /**
     * Overrides {@link Animal#displayInfo()} to append the owning client.
     * Calling {@code super.displayInfo()} reuses the parent's formatting instead of
     * duplicating it.
     */
    @Override
    public String displayInfo() {
        return super.displayInfo() + " • Owner " + (ownerId == null ? "unassigned" : ownerId);
    }

    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    public LocalDate getRegistrationDate() {
        return registrationDate;
    }

    public void setRegistrationDate(LocalDate registrationDate) {
        this.registrationDate = registrationDate;
    }

    public MedicalInfo getMedicalInfo() {
        return medicalInfo;
    }

    public void setMedicalInfo(MedicalInfo medicalInfo) {
        this.medicalInfo = medicalInfo == null ? new MedicalInfo() : medicalInfo;
    }

    /** Species-specific attribute shown in the pet profile, e.g. "Training level: Advanced". */
    public abstract String speciesDetail();
}
