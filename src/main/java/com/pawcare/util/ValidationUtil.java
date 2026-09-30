package com.pawcare.util;

import com.pawcare.exception.InvalidAppointmentException;
import com.pawcare.exception.InvalidOwnerException;
import com.pawcare.exception.InvalidPetException;
import com.pawcare.exception.InvalidTreatmentException;
import com.pawcare.exception.InvalidVaccinationException;
import com.pawcare.model.Appointment;
import com.pawcare.model.Owner;
import com.pawcare.model.Pet;
import com.pawcare.model.Treatment;
import com.pawcare.model.Vaccination;
import com.pawcare.model.enums.AppointmentStatus;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Central home for every business rule that decides whether a record may be saved.
 *
 * <p>Keeping validation in one place means the dialogs, the services and the unit tests
 * all apply exactly the same rules. Each method collects <i>all</i> problems before
 * throwing, so a user filling in a form sees every mistake at once.</p>
 */
public final class ValidationUtil {

    /** Upper bound on a plausible pet age, used to catch typos such as 300. */
    public static final int MAX_PET_AGE = 60;
    /** Upper bound on a plausible pet weight in kilograms. */
    public static final double MAX_PET_WEIGHT = 200.0;

    /** Indian mobile number: optional +91 prefix followed by ten digits starting 6-9. */
    private static final Pattern PHONE_PATTERN =
            Pattern.compile("^(\\+91[\\-\\s]?)?[6-9]\\d{9}$");

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9._%+\\-]+@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,}$");

    private ValidationUtil() {
        // Utility class – never instantiated.
    }

    // ------------------------------------------------------------------
    // Shared helpers, also used by the dialogs for live field validation.
    // ------------------------------------------------------------------

    public static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    public static boolean isValidPhone(String phone) {
        return phone != null && PHONE_PATTERN.matcher(phone.trim()).matches();
    }

    public static boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email.trim()).matches();
    }

    /** @return a trimmed copy of the text, or an empty string when null. */
    public static String clean(String value) {
        return value == null ? "" : value.trim();
    }

    // ------------------------------------------------------------------
    // Record validation
    // ------------------------------------------------------------------

    /** Validates a pet. Throws {@link InvalidPetException} listing every problem found. */
    public static void validatePet(Pet pet) throws InvalidPetException {
        if (pet == null) {
            throw new InvalidPetException("Pet record cannot be empty.");
        }

        List<String> errors = new ArrayList<>();

        if (isBlank(pet.getName())) {
            errors.add("Pet name cannot be empty.");
        } else if (pet.getName().trim().length() < 2) {
            errors.add("Pet name must be at least 2 characters long.");
        }

        if (pet.getAge() < 0) {
            errors.add("Age cannot be negative.");
        } else if (pet.getAge() > MAX_PET_AGE) {
            errors.add("Age must be " + MAX_PET_AGE + " years or less.");
        }

        if (pet.getWeight() <= 0) {
            errors.add("Weight must be greater than zero.");
        } else if (pet.getWeight() > MAX_PET_WEIGHT) {
            errors.add("Weight must be " + (int) MAX_PET_WEIGHT + " kg or less.");
        }

        if (isBlank(pet.getBreed())) {
            errors.add("Breed cannot be empty.");
        }

        if (pet.getGender() == null) {
            errors.add("Gender must be selected.");
        }

        if (isBlank(pet.getOwnerId())) {
            errors.add("An owner must be assigned to the pet.");
        }

        if (pet.getRegistrationDate() == null) {
            errors.add("Registration date is required.");
        } else if (pet.getRegistrationDate().isAfter(LocalDate.now())) {
            errors.add("Registration date cannot be in the future.");
        }

        if (!errors.isEmpty()) {
            throw new InvalidPetException("Unable to save pet.", errors);
        }
    }

    /** Validates an owner. Throws {@link InvalidOwnerException} listing every problem found. */
    public static void validateOwner(Owner owner) throws InvalidOwnerException {
        if (owner == null) {
            throw new InvalidOwnerException("Owner record cannot be empty.");
        }

        List<String> errors = new ArrayList<>();

        if (isBlank(owner.getName())) {
            errors.add("Owner name cannot be empty.");
        } else if (owner.getName().trim().length() < 2) {
            errors.add("Owner name must be at least 2 characters long.");
        }

        if (isBlank(owner.getPhone())) {
            errors.add("Phone number is required.");
        } else if (!isValidPhone(owner.getPhone())) {
            errors.add("Phone must be a valid 10-digit Indian mobile number (optionally +91).");
        }

        if (isBlank(owner.getEmail())) {
            errors.add("Email is required.");
        } else if (!isValidEmail(owner.getEmail())) {
            errors.add("Email address is not valid.");
        }

        if (isBlank(owner.getAddress())) {
            errors.add("Address cannot be empty.");
        }

        // The emergency contact is optional, but if supplied it must be usable.
        if (!isBlank(owner.getEmergencyContact()) && !isValidPhone(owner.getEmergencyContact())) {
            errors.add("Emergency contact must be a valid 10-digit Indian mobile number.");
        }

        if (!errors.isEmpty()) {
            throw new InvalidOwnerException("Unable to save owner.", errors);
        }
    }

    /**
     * Validates an appointment.
     *
     * <p>A booking that is still {@code SCHEDULED} may not be dated in the past, but a
     * {@code COMPLETED} or {@code CANCELLED} record is allowed to be historical –
     * otherwise existing history could never be edited.</p>
     */
    public static void validateAppointment(Appointment appointment) throws InvalidAppointmentException {
        if (appointment == null) {
            throw new InvalidAppointmentException("Appointment record cannot be empty.");
        }

        List<String> errors = new ArrayList<>();

        if (isBlank(appointment.getPetId())) {
            errors.add("A pet must be selected.");
        }

        if (isBlank(appointment.getVeterinarianId())) {
            errors.add("A veterinarian must be selected.");
        }

        if (appointment.getAppointmentDate() == null) {
            errors.add("Appointment date is required.");
        }

        if (appointment.getAppointmentTime() == null) {
            errors.add("Appointment time is required.");
        }

        if (appointment.getStatus() == null) {
            errors.add("Appointment status is required.");
        }

        if (isBlank(appointment.getReason())) {
            errors.add("Reason for visit cannot be empty.");
        } else if (appointment.getReason().trim().length() < 3) {
            errors.add("Reason for visit must be at least 3 characters long.");
        }

        // A visit that has not happened yet cannot be booked in the past.
        if (appointment.getAppointmentDate() != null
                && appointment.getStatus() == AppointmentStatus.SCHEDULED
                && appointment.getAppointmentDate().isBefore(LocalDate.now())) {
            errors.add("A scheduled appointment cannot be in the past.");
        }

        if (!errors.isEmpty()) {
            throw new InvalidAppointmentException("Unable to save appointment.", errors);
        }
    }

    /** Validates a vaccination, including the ordering of the two dates. */
    public static void validateVaccination(Vaccination vaccination) throws InvalidVaccinationException {
        if (vaccination == null) {
            throw new InvalidVaccinationException("Vaccination record cannot be empty.");
        }

        List<String> errors = new ArrayList<>();

        if (isBlank(vaccination.getPetId())) {
            errors.add("A pet must be selected.");
        }

        if (isBlank(vaccination.getVeterinarianId())) {
            errors.add("A veterinarian must be selected.");
        }

        if (isBlank(vaccination.getVaccineName())) {
            errors.add("Vaccine name cannot be empty.");
        }

        if (vaccination.getVaccinationDate() == null) {
            errors.add("Vaccination date is required.");
        } else if (vaccination.getVaccinationDate().isAfter(LocalDate.now())) {
            errors.add("Vaccination date cannot be in the future.");
        }

        if (vaccination.getNextDueDate() == null) {
            errors.add("Next due date is required.");
        } else if (vaccination.getVaccinationDate() != null
                && vaccination.getNextDueDate().isBefore(vaccination.getVaccinationDate())) {
            errors.add("Next due date cannot be earlier than the vaccination date.");
        }

        if (!errors.isEmpty()) {
            throw new InvalidVaccinationException("Unable to save vaccination.", errors);
        }
    }

    /** Validates a treatment record. */
    public static void validateTreatment(Treatment treatment) throws InvalidTreatmentException {
        if (treatment == null) {
            throw new InvalidTreatmentException("Treatment record cannot be empty.");
        }

        List<String> errors = new ArrayList<>();

        if (isBlank(treatment.getPetId())) {
            errors.add("A pet must be selected.");
        }

        if (isBlank(treatment.getVeterinarianId())) {
            errors.add("A veterinarian must be selected.");
        }

        if (treatment.getDate() == null) {
            errors.add("Treatment date is required.");
        } else if (treatment.getDate().isAfter(LocalDate.now())) {
            errors.add("Treatment date cannot be in the future.");
        }

        if (isBlank(treatment.getDiagnosis())) {
            errors.add("Diagnosis cannot be empty.");
        }

        if (isBlank(treatment.getTreatment())) {
            errors.add("Treatment details cannot be empty.");
        }

        if (!errors.isEmpty()) {
            throw new InvalidTreatmentException("Unable to save treatment record.", errors);
        }
    }
}
