package com.pawcare.model;

import com.pawcare.model.enums.Gender;
import com.pawcare.model.enums.IndoorOutdoor;
import com.pawcare.model.enums.Species;

import java.time.LocalDate;

/** A feline patient. Adds the indoor/outdoor lifestyle flag. */
public class Cat extends Pet {

    private static final long serialVersionUID = 1L;

    private IndoorOutdoor indoorOrOutdoor;

    public Cat() {
        super();
        this.indoorOrOutdoor = IndoorOutdoor.INDOOR;
    }

    public Cat(String animalId, String name, int age, double weight, Gender gender, String breed,
               String ownerId, LocalDate registrationDate, MedicalInfo medicalInfo,
               IndoorOutdoor indoorOrOutdoor) {
        super(animalId, name, age, weight, gender, breed, ownerId, registrationDate, medicalInfo);
        this.indoorOrOutdoor = indoorOrOutdoor == null ? IndoorOutdoor.INDOOR : indoorOrOutdoor;
    }

    @Override
    public Species getSpecies() {
        return Species.CAT;
    }

    /** Feline-specific clinical plan. See {@link Animal#treatmentPlan()} for the polymorphism note. */
    @Override
    public String treatmentPlan() {
        return "Feline health assessment, dental and oral examination, indoor/outdoor risk "
                + "evaluation, deworming and vaccination review.";
    }

    @Override
    public String speciesDetail() {
        return "Lifestyle: " + indoorOrOutdoor.getLabel();
    }

    public IndoorOutdoor getIndoorOrOutdoor() {
        return indoorOrOutdoor;
    }

    public void setIndoorOrOutdoor(IndoorOutdoor indoorOrOutdoor) {
        this.indoorOrOutdoor = indoorOrOutdoor;
    }
}
