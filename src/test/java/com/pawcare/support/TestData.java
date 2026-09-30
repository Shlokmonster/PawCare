package com.pawcare.support;

import com.pawcare.model.Appointment;
import com.pawcare.model.Bird;
import com.pawcare.model.Cat;
import com.pawcare.model.Dog;
import com.pawcare.model.MedicalInfo;
import com.pawcare.model.Owner;
import com.pawcare.model.Treatment;
import com.pawcare.model.Vaccination;
import com.pawcare.model.Veterinarian;
import com.pawcare.model.enums.AppointmentStatus;
import com.pawcare.model.enums.Gender;
import com.pawcare.model.enums.IndoorOutdoor;
import com.pawcare.model.enums.TrainingLevel;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Builders shared by the unit tests.
 *
 * <p>Every builder returns a record that passes validation, so a test only has to change
 * the one field it is actually about and the failure it expects is the failure it gets.
 * Without this, each test would carry ten lines of irrelevant setup and a broken rule
 * elsewhere would make dozens of tests fail at once.</p>
 */
public final class TestData {

    private TestData() {
    }

    // ------------------------------------------------------------------
    // People
    // ------------------------------------------------------------------

    public static Owner owner(String ownerId) {
        return owner(ownerId, "Ravi Kumar");
    }

    public static Owner owner(String ownerId, String name) {
        return owner(ownerId, name, "9876543210", "ravi.kumar@example.com");
    }

    public static Owner owner(String ownerId, String name, String phone, String email) {
        return new Owner(ownerId, name, phone, email,
                "12 MG Road, Bengaluru 560001", "9876500000");
    }

    public static Veterinarian vet(String veterinarianId, String name) {
        return new Veterinarian(veterinarianId, name, "General Medicine",
                "9845567711", "vet@pawcare.example.com");
    }

    // ------------------------------------------------------------------
    // Patients – one builder per concrete species of the Animal hierarchy
    // ------------------------------------------------------------------

    public static Dog dog(String petId, String ownerId, String name) {
        return new Dog(petId, name, 3, 12.5, Gender.MALE, "Labrador",
                ownerId, LocalDate.now().minusMonths(4), new MedicalInfo(),
                TrainingLevel.INTERMEDIATE);
    }

    public static Cat cat(String petId, String ownerId, String name) {
        return new Cat(petId, name, 2, 4.2, Gender.FEMALE, "Persian",
                ownerId, LocalDate.now().minusMonths(2), new MedicalInfo(),
                IndoorOutdoor.INDOOR);
    }

    public static Bird bird(String petId, String ownerId, String name) {
        return new Bird(petId, name, 1, 0.45, Gender.UNKNOWN, "Cockatiel",
                ownerId, LocalDate.now().minusMonths(1), new MedicalInfo(), 24.0);
    }

    // ------------------------------------------------------------------
    // Records
    // ------------------------------------------------------------------

    public static Appointment appointment(String appointmentId, String petId, String veterinarianId,
                                          LocalDate date, LocalTime time, AppointmentStatus status) {
        return new Appointment(appointmentId, petId, veterinarianId, date, time,
                "Routine check-up", status, "");
    }

    public static Treatment treatment(String treatmentId, String petId, String veterinarianId,
                                      LocalDate date) {
        return new Treatment(treatmentId, petId, veterinarianId, date,
                "Ear infection", "Antibiotic course", "Amoxicillin", "");
    }

    public static Vaccination vaccination(String vaccinationId, String petId, String veterinarianId,
                                          String vaccineName, LocalDate given, LocalDate nextDue) {
        return new Vaccination(vaccinationId, petId, veterinarianId, vaccineName, given, nextDue, "");
    }

    /** A dose given a year ago and due on the supplied date. */
    public static Vaccination vaccinationDue(String vaccinationId, String petId, LocalDate nextDue) {
        return vaccination(vaccinationId, petId, "V001", "Rabies",
                LocalDate.now().minusYears(1), nextDue);
    }
}
