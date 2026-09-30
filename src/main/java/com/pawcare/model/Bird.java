package com.pawcare.model;

import com.pawcare.model.enums.Gender;
import com.pawcare.model.enums.Species;

import java.time.LocalDate;

/** An avian patient. Adds the wing span measurement. */
public class Bird extends Pet {

    private static final long serialVersionUID = 1L;

    private double wingSpan; // centimetres

    public Bird() {
        super();
        this.wingSpan = 0;
    }

    public Bird(String animalId, String name, int age, double weight, Gender gender, String breed,
                String ownerId, LocalDate registrationDate, MedicalInfo medicalInfo,
                double wingSpan) {
        super(animalId, name, age, weight, gender, breed, ownerId, registrationDate, medicalInfo);
        this.wingSpan = wingSpan;
    }

    @Override
    public Species getSpecies() {
        return Species.BIRD;
    }

    /** Avian-specific clinical plan. See {@link Animal#treatmentPlan()} for the polymorphism note. */
    @Override
    public String treatmentPlan() {
        return "Avian wellness check, feather and beak condition assessment, wing-span and "
                + "flight-muscle evaluation, dietary and cage-hygiene guidance.";
    }

    @Override
    public String speciesDetail() {
        return String.format("Wing span: %.1f cm", wingSpan);
    }

    public double getWingSpan() {
        return wingSpan;
    }

    public void setWingSpan(double wingSpan) {
        this.wingSpan = wingSpan;
    }
}
