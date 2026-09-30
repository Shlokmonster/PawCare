package com.pawcare.model;

import com.pawcare.model.enums.Gender;
import com.pawcare.model.enums.Species;

import java.io.Serializable;
import java.util.Objects;

/**
 * Abstract superclass of every animal treated by the clinic.
 *
 * <p>This class demonstrates four core OOP concepts:</p>
 * <ul>
 *   <li><b>Abstraction</b> – it cannot be instantiated directly; only concrete species exist.</li>
 *   <li><b>Encapsulation</b> – every field is private and exposed through accessors.</li>
 *   <li><b>Inheritance</b> – {@link Pet}, and through it {@link Dog}, {@link Cat} and
 *       {@link Bird}, extend this class.</li>
 *   <li><b>Runtime polymorphism</b> – {@link #treatmentPlan()} is abstract, so a variable
 *       declared as {@code Animal} invokes the version belonging to the actual object.</li>
 * </ul>
 */
public abstract class Animal implements Serializable {

    private static final long serialVersionUID = 1L;

    private String animalId;
    private String name;
    private int age;          // completed years
    private double weight;    // kilograms
    private Gender gender;
    private String breed;

    /** No-argument constructor required by the Java serialization mechanism. */
    protected Animal() {
    }

    protected Animal(String animalId, String name, int age, double weight, Gender gender, String breed) {
        this.animalId = animalId;
        this.name = name;
        this.age = age;
        this.weight = weight;
        this.gender = gender;
        this.breed = breed;
    }

    // ------------------------------------------------------------------
    // Abstract behaviour: implemented differently by each species.
    // ------------------------------------------------------------------

    /** @return the species of this animal, resolved from its concrete class. */
    public abstract Species getSpecies();

    /**
     * Produces the species-specific clinical treatment plan.
     *
     * <p>Declaring this method abstract forces every subclass to provide its own
     * version. When the GUI calls {@code animal.treatmentPlan()} it does not know
     * (and does not need to know) which species it is holding – the JVM selects the
     * correct implementation at runtime. That is runtime polymorphism.</p>
     */
    public abstract String treatmentPlan();

    /**
     * Human-readable one-line summary. Overridden by {@link Pet} to add ownership
     * information, which is a second demonstration of method overriding.
     */
    public String displayInfo() {
        return String.format("%s • %s • %d yr • %.1f kg • %s",
                name, getSpecies().getLabel(), age, weight, breed);
    }

    // ------------------------------------------------------------------
    // Encapsulation: private fields, public accessors.
    // ------------------------------------------------------------------

    public String getAnimalId() {
        return animalId;
    }

    public void setAnimalId(String animalId) {
        this.animalId = animalId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public double getWeight() {
        return weight;
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }

    public Gender getGender() {
        return gender;
    }

    public void setGender(Gender gender) {
        this.gender = gender;
    }

    public String getBreed() {
        return breed;
    }

    public void setBreed(String breed) {
        this.breed = breed;
    }

    /** Two animals are the same animal when they share an id. */
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Animal)) {
            return false;
        }
        Animal that = (Animal) other;
        return Objects.equals(animalId, that.animalId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(animalId);
    }

    @Override
    public String toString() {
        return displayInfo();
    }
}
