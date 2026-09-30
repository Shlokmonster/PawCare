package com.pawcare.model;

import com.pawcare.model.enums.Gender;
import com.pawcare.model.enums.Species;
import com.pawcare.model.enums.TrainingLevel;

import java.time.LocalDate;

/** A canine patient. Adds the training level to the common pet record. */
public class Dog extends Pet {

    private static final long serialVersionUID = 1L;

    private TrainingLevel trainingLevel;

    public Dog() {
        super();
        this.trainingLevel = TrainingLevel.BEGINNER;
    }

    public Dog(String animalId, String name, int age, double weight, Gender gender, String breed,
               String ownerId, LocalDate registrationDate, MedicalInfo medicalInfo,
               TrainingLevel trainingLevel) {
        super(animalId, name, age, weight, gender, breed, ownerId, registrationDate, medicalInfo);
        this.trainingLevel = trainingLevel == null ? TrainingLevel.BEGINNER : trainingLevel;
    }

    @Override
    public Species getSpecies() {
        return Species.DOG;
    }

    /** Canine-specific clinical plan. See {@link Animal#treatmentPlan()} for the polymorphism note. */
    @Override
    public String treatmentPlan() {
        return "Canine wellness examination, vaccination review, parasite prevention and "
                + "breed-specific assessment.";
    }

    @Override
    public String speciesDetail() {
        return "Training level: " + trainingLevel.getLabel();
    }

    public TrainingLevel getTrainingLevel() {
        return trainingLevel;
    }

    public void setTrainingLevel(TrainingLevel trainingLevel) {
        this.trainingLevel = trainingLevel;
    }
}
