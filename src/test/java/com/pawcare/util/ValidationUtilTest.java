package com.pawcare.util;

import com.pawcare.exception.InvalidAppointmentException;
import com.pawcare.exception.InvalidOwnerException;
import com.pawcare.exception.InvalidPetException;
import com.pawcare.exception.InvalidTreatmentException;
import com.pawcare.exception.InvalidVaccinationException;
import com.pawcare.model.Appointment;
import com.pawcare.model.Dog;
import com.pawcare.model.Owner;
import com.pawcare.model.Pet;
import com.pawcare.model.Treatment;
import com.pawcare.model.Vaccination;
import com.pawcare.model.enums.AppointmentStatus;
import com.pawcare.support.TestData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Every business rule that decides whether a record may be saved.
 *
 * <p>One nested class per entity keeps the failure output readable: a broken pet rule
 * reports itself under "Pets" instead of in a list of forty unrelated tests.</p>
 */
class ValidationUtilTest {

    @Nested
    @DisplayName("Pet validation")
    class PetValidation {

        @Test
        void aCompletePetPassesValidation() {
            assertDoesNotThrow(() -> ValidationUtil.validatePet(TestData.dog("P001", "O001", "Bruno")));
        }

        @Test
        void anEmptyRecordIsRejected() {
            InvalidPetException failure = assertThrows(InvalidPetException.class,
                    () -> ValidationUtil.validatePet(null));
            assertEquals("Pet record cannot be empty.", failure.getMessage());
        }

        @Test
        void theNameCannotBeBlank() {
            Dog pet = TestData.dog("P001", "O001", "   ");
            InvalidPetException failure = assertThrows(InvalidPetException.class,
                    () -> ValidationUtil.validatePet(pet));
            assertTrue(failure.getDetails().contains("Pet name cannot be empty."));
        }

        @Test
        void theNameMustBeAtLeastTwoCharacters() {
            Dog pet = TestData.dog("P001", "O001", "B");
            InvalidPetException failure = assertThrows(InvalidPetException.class,
                    () -> ValidationUtil.validatePet(pet));
            assertTrue(failure.getDetails().contains("Pet name must be at least 2 characters long."));
        }

        @Test
        void aNegativeAgeIsRejected() {
            Dog pet = TestData.dog("P001", "O001", "Bruno");
            pet.setAge(-1);
            InvalidPetException failure = assertThrows(InvalidPetException.class,
                    () -> ValidationUtil.validatePet(pet));
            assertTrue(failure.getDetails().contains("Age cannot be negative."));
        }

        @Test
        void anUnrealisticAgeIsRejected() {
            Dog pet = TestData.dog("P001", "O001", "Bruno");
            pet.setAge(ValidationUtil.MAX_PET_AGE + 1);
            InvalidPetException failure = assertThrows(InvalidPetException.class,
                    () -> ValidationUtil.validatePet(pet));
            assertTrue(failure.getDetails().contains("Age must be " + ValidationUtil.MAX_PET_AGE + " years or less."));
        }

        @Test
        void theWeightMustBeGreaterThanZero() {
            Dog pet = TestData.dog("P001", "O001", "Bruno");
            pet.setWeight(0);
            InvalidPetException failure = assertThrows(InvalidPetException.class,
                    () -> ValidationUtil.validatePet(pet));
            assertTrue(failure.getDetails().contains("Weight must be greater than zero."));
        }

        @Test
        void anUnrealisticWeightIsRejected() {
            Dog pet = TestData.dog("P001", "O001", "Bruno");
            pet.setWeight(ValidationUtil.MAX_PET_WEIGHT + 5);
            InvalidPetException failure = assertThrows(InvalidPetException.class,
                    () -> ValidationUtil.validatePet(pet));
            assertTrue(failure.getDetails().contains("Weight must be 200 kg or less."));
        }

        @Test
        void theBreedCannotBeBlank() {
            Dog pet = TestData.dog("P001", "O001", "Bruno");
            pet.setBreed("");
            InvalidPetException failure = assertThrows(InvalidPetException.class,
                    () -> ValidationUtil.validatePet(pet));
            assertTrue(failure.getDetails().contains("Breed cannot be empty."));
        }

        @Test
        void theGenderMustBeSelected() {
            Dog pet = TestData.dog("P001", "O001", "Bruno");
            pet.setGender(null);
            InvalidPetException failure = assertThrows(InvalidPetException.class,
                    () -> ValidationUtil.validatePet(pet));
            assertTrue(failure.getDetails().contains("Gender must be selected."));
        }

        @Test
        void aPetMustBelongToAnOwner() {
            Dog pet = TestData.dog("P001", "O001", "Bruno");
            pet.setOwnerId(null);
            InvalidPetException failure = assertThrows(InvalidPetException.class,
                    () -> ValidationUtil.validatePet(pet));
            assertTrue(failure.getDetails().contains("An owner must be assigned to the pet."));
        }

        @Test
        void aFutureRegistrationDateIsRejected() {
            Dog pet = TestData.dog("P001", "O001", "Bruno");
            pet.setRegistrationDate(LocalDate.now().plusDays(1));
            InvalidPetException failure = assertThrows(InvalidPetException.class,
                    () -> ValidationUtil.validatePet(pet));
            assertTrue(failure.getDetails().contains("Registration date cannot be in the future."));
        }

        @Test
        void everyProblemIsReportedInOneGo() {
            Dog pet = TestData.dog("P001", "O001", "");
            pet.setAge(200);
            pet.setWeight(-3);
            pet.setBreed(null);
            pet.setGender(null);
            pet.setOwnerId(null);

            InvalidPetException failure = assertThrows(InvalidPetException.class,
                    () -> ValidationUtil.validatePet(pet));
            // A user filling in a form should see every mistake at once, not one per attempt.
            assertEquals(6, failure.getDetails().size());
        }
    }

    @Nested
    @DisplayName("Owner validation")
    class OwnerValidation {

        @Test
        void aCompleteOwnerPassesValidation() {
            assertDoesNotThrow(() -> ValidationUtil.validateOwner(TestData.owner("O001")));
        }

        @Test
        void anEmptyRecordIsRejected() {
            assertThrows(InvalidOwnerException.class, () -> ValidationUtil.validateOwner(null));
        }

        @Test
        void theNameCannotBeBlank() {
            Owner owner = TestData.owner("O001", "  ");
            InvalidOwnerException failure = assertThrows(InvalidOwnerException.class,
                    () -> ValidationUtil.validateOwner(owner));
            assertTrue(failure.getDetails().contains("Owner name cannot be empty."));
        }

        @Test
        void theNameMustBeAtLeastTwoCharacters() {
            Owner owner = TestData.owner("O001", "A");
            InvalidOwnerException failure = assertThrows(InvalidOwnerException.class,
                    () -> ValidationUtil.validateOwner(owner));
            assertTrue(failure.getDetails().contains("Owner name must be at least 2 characters long."));
        }

        @Test
        void aTenDigitIndianMobileNumberIsAccepted() {
            assertTrue(ValidationUtil.isValidPhone("9876543210"));
            assertTrue(ValidationUtil.isValidPhone("+91 9876543210"));
            assertTrue(ValidationUtil.isValidPhone("+91-9876543210"));
        }

        @Test
        void aPhoneNumberThatIsNotAnIndianMobileIsRejected() {
            assertFalse(ValidationUtil.isValidPhone("1234567890"));   // does not start 6-9
            assertFalse(ValidationUtil.isValidPhone("98765"));        // too short
            assertFalse(ValidationUtil.isValidPhone("98765432101"));  // too long
            assertFalse(ValidationUtil.isValidPhone("abcdefghij"));
            assertFalse(ValidationUtil.isValidPhone(null));
        }

        @Test
        void anInvalidPhoneNumberIsReported() {
            Owner owner = TestData.owner("O001", "Ravi Kumar", "12345", "ravi.kumar@example.com");
            InvalidOwnerException failure = assertThrows(InvalidOwnerException.class,
                    () -> ValidationUtil.validateOwner(owner));
            assertTrue(failure.getDetails().contains(
                    "Phone must be a valid 10-digit Indian mobile number (optionally +91)."));
        }

        @Test
        void aMissingPhoneNumberIsReported() {
            Owner owner = TestData.owner("O001", "Ravi Kumar", "  ", "ravi.kumar@example.com");
            InvalidOwnerException failure = assertThrows(InvalidOwnerException.class,
                    () -> ValidationUtil.validateOwner(owner));
            assertTrue(failure.getDetails().contains("Phone number is required."));
        }

        @Test
        void emailAddressesAreChecked() {
            assertTrue(ValidationUtil.isValidEmail("ravi.kumar@example.com"));
            assertTrue(ValidationUtil.isValidEmail("a_b+tag@sub.domain.co.in"));
            assertFalse(ValidationUtil.isValidEmail("ravi at example.com"));
            assertFalse(ValidationUtil.isValidEmail("ravi@example"));
            assertFalse(ValidationUtil.isValidEmail(null));
        }

        @Test
        void anInvalidEmailIsReported() {
            Owner owner = TestData.owner("O001", "Ravi Kumar", "9876543210", "ravi(at)example.com");
            InvalidOwnerException failure = assertThrows(InvalidOwnerException.class,
                    () -> ValidationUtil.validateOwner(owner));
            assertTrue(failure.getDetails().contains("Email address is not valid."));
        }

        @Test
        void theAddressCannotBeBlank() {
            Owner owner = TestData.owner("O001");
            owner.setAddress("   ");
            InvalidOwnerException failure = assertThrows(InvalidOwnerException.class,
                    () -> ValidationUtil.validateOwner(owner));
            assertTrue(failure.getDetails().contains("Address cannot be empty."));
        }

        @Test
        void theEmergencyContactIsOptional() {
            Owner owner = TestData.owner("O001");
            owner.setEmergencyContact("");
            assertDoesNotThrow(() -> ValidationUtil.validateOwner(owner));
            owner.setEmergencyContact(null);
            assertDoesNotThrow(() -> ValidationUtil.validateOwner(owner));
        }

        @Test
        void anUnusableEmergencyContactIsRejected() {
            Owner owner = TestData.owner("O001");
            owner.setEmergencyContact("not-a-number");
            InvalidOwnerException failure = assertThrows(InvalidOwnerException.class,
                    () -> ValidationUtil.validateOwner(owner));
            assertTrue(failure.getDetails().contains(
                    "Emergency contact must be a valid 10-digit Indian mobile number."));
        }

        @Test
        void everyProblemIsReportedInOneGo() {
            Owner owner = new Owner("O001", "A", "123", "bad", "", "xyz");
            InvalidOwnerException failure = assertThrows(InvalidOwnerException.class,
                    () -> ValidationUtil.validateOwner(owner));
            assertEquals(5, failure.getDetails().size());
        }
    }

    @Nested
    @DisplayName("Appointment validation")
    class AppointmentValidation {

        private Appointment scheduled(LocalDate date) {
            return TestData.appointment("A001", "P001", "V001", date,
                    LocalTime.of(10, 30), AppointmentStatus.SCHEDULED);
        }

        @Test
        void aFutureScheduledVisitPassesValidation() {
            assertDoesNotThrow(() -> ValidationUtil.validateAppointment(scheduled(LocalDate.now().plusDays(3))));
        }

        @Test
        void anEmptyRecordIsRejected() {
            assertThrows(InvalidAppointmentException.class,
                    () -> ValidationUtil.validateAppointment(null));
        }

        @Test
        void aScheduledVisitCannotBeDatedInThePast() {
            InvalidAppointmentException failure = assertThrows(InvalidAppointmentException.class,
                    () -> ValidationUtil.validateAppointment(scheduled(LocalDate.now().minusDays(1))));
            assertTrue(failure.getDetails().contains("A scheduled appointment cannot be in the past."));
        }

        @Test
        void theRuleOnlyAppliesToVisitsThatAreStillScheduled() {
            // Existing history has to remain editable, so a past date is fine once the
            // visit is completed or cancelled.
            assertDoesNotThrow(() -> ValidationUtil.validateAppointment(
                    TestData.appointment("A001", "P001", "V001", LocalDate.now().minusDays(30),
                            LocalTime.of(10, 30), AppointmentStatus.COMPLETED)));
            assertDoesNotThrow(() -> ValidationUtil.validateAppointment(
                    TestData.appointment("A002", "P001", "V001", LocalDate.now().minusDays(30),
                            LocalTime.of(10, 30), AppointmentStatus.CANCELLED)));
        }

        @Test
        void aVisitTodayIsAccepted() {
            assertDoesNotThrow(() -> ValidationUtil.validateAppointment(scheduled(LocalDate.now())));
        }

        @Test
        void aPetMustBeChosen() {
            Appointment appointment = scheduled(LocalDate.now().plusDays(1));
            appointment.setPetId(null);
            InvalidAppointmentException failure = assertThrows(InvalidAppointmentException.class,
                    () -> ValidationUtil.validateAppointment(appointment));
            assertTrue(failure.getDetails().contains("A pet must be selected."));
        }

        @Test
        void aVeterinarianMustBeChosen() {
            Appointment appointment = scheduled(LocalDate.now().plusDays(1));
            appointment.setVeterinarianId(" ");
            InvalidAppointmentException failure = assertThrows(InvalidAppointmentException.class,
                    () -> ValidationUtil.validateAppointment(appointment));
            assertTrue(failure.getDetails().contains("A veterinarian must be selected."));
        }

        @Test
        void theDateAndTimeAreRequired() {
            Appointment appointment = scheduled(LocalDate.now().plusDays(1));
            appointment.setAppointmentDate(null);
            appointment.setAppointmentTime(null);
            InvalidAppointmentException failure = assertThrows(InvalidAppointmentException.class,
                    () -> ValidationUtil.validateAppointment(appointment));
            assertTrue(failure.getDetails().contains("Appointment date is required."));
            assertTrue(failure.getDetails().contains("Appointment time is required."));
        }

        @Test
        void theReasonMustDescribeTheVisit() {
            Appointment appointment = scheduled(LocalDate.now().plusDays(1));
            appointment.setReason("ab");
            InvalidAppointmentException failure = assertThrows(InvalidAppointmentException.class,
                    () -> ValidationUtil.validateAppointment(appointment));
            assertTrue(failure.getDetails().contains(
                    "Reason for visit must be at least 3 characters long."));
        }
    }

    @Nested
    @DisplayName("Vaccination validation")
    class VaccinationValidation {

        @Test
        void aCompleteRecordPassesValidation() {
            assertDoesNotThrow(() -> ValidationUtil.validateVaccination(
                    TestData.vaccinationDue("VAC001", "P001", LocalDate.now().plusDays(200))));
        }

        @Test
        void anEmptyRecordIsRejected() {
            assertThrows(InvalidVaccinationException.class,
                    () -> ValidationUtil.validateVaccination(null));
        }

        @Test
        void theNextDoseCannotPrecedeTheDoseThatWasGiven() {
            Vaccination vaccination = TestData.vaccination("VAC001", "P001", "V001", "Rabies",
                    LocalDate.now().minusDays(10), LocalDate.now().minusDays(40));
            InvalidVaccinationException failure = assertThrows(InvalidVaccinationException.class,
                    () -> ValidationUtil.validateVaccination(vaccination));
            assertTrue(failure.getDetails().contains(
                    "Next due date cannot be earlier than the vaccination date."));
        }

        @Test
        void theDoseItselfCannotBeGivenInTheFuture() {
            Vaccination vaccination = TestData.vaccination("VAC001", "P001", "V001", "Rabies",
                    LocalDate.now().plusDays(1), LocalDate.now().plusDays(400));
            InvalidVaccinationException failure = assertThrows(InvalidVaccinationException.class,
                    () -> ValidationUtil.validateVaccination(vaccination));
            assertTrue(failure.getDetails().contains("Vaccination date cannot be in the future."));
        }

        @Test
        void aDoseGivenTodayIsAccepted() {
            Vaccination vaccination = TestData.vaccination("VAC001", "P001", "V001", "Rabies",
                    LocalDate.now(), LocalDate.now().plusDays(365));
            assertDoesNotThrow(() -> ValidationUtil.validateVaccination(vaccination));
        }

        @Test
        void theVaccineNameIsRequired() {
            Vaccination vaccination = TestData.vaccination("VAC001", "P001", "V001", "  ",
                    LocalDate.now().minusDays(5), LocalDate.now().plusDays(300));
            InvalidVaccinationException failure = assertThrows(InvalidVaccinationException.class,
                    () -> ValidationUtil.validateVaccination(vaccination));
            assertTrue(failure.getDetails().contains("Vaccine name cannot be empty."));
        }

        @Test
        void bothDatesAreRequired() {
            Vaccination vaccination = TestData.vaccination("VAC001", "P001", "V001", "Rabies", null, null);
            InvalidVaccinationException failure = assertThrows(InvalidVaccinationException.class,
                    () -> ValidationUtil.validateVaccination(vaccination));
            assertTrue(failure.getDetails().contains("Vaccination date is required."));
            assertTrue(failure.getDetails().contains("Next due date is required."));
        }
    }

    @Nested
    @DisplayName("Treatment validation")
    class TreatmentValidation {

        @Test
        void aCompleteRecordPassesValidation() {
            assertDoesNotThrow(() -> ValidationUtil.validateTreatment(
                    TestData.treatment("T001", "P001", "V001", LocalDate.now().minusDays(2))));
        }

        @Test
        void anEmptyRecordIsRejected() {
            assertThrows(InvalidTreatmentException.class, () -> ValidationUtil.validateTreatment(null));
        }

        @Test
        void aTreatmentCannotBeDatedInTheFuture() {
            Treatment treatment = TestData.treatment("T001", "P001", "V001", LocalDate.now().plusDays(1));
            InvalidTreatmentException failure = assertThrows(InvalidTreatmentException.class,
                    () -> ValidationUtil.validateTreatment(treatment));
            assertTrue(failure.getDetails().contains("Treatment date cannot be in the future."));
        }

        @Test
        void theDiagnosisAndTheTreatmentAreBothRequired() {
            Treatment treatment = TestData.treatment("T001", "P001", "V001", LocalDate.now());
            treatment.setDiagnosis("  ");
            treatment.setTreatment(null);
            InvalidTreatmentException failure = assertThrows(InvalidTreatmentException.class,
                    () -> ValidationUtil.validateTreatment(treatment));
            assertTrue(failure.getDetails().contains("Diagnosis cannot be empty."));
            assertTrue(failure.getDetails().contains("Treatment details cannot be empty."));
        }

        @Test
        void thePetAndTheVeterinarianAreBothRequired() {
            Treatment treatment = TestData.treatment("T001", null, " ", LocalDate.now());
            InvalidTreatmentException failure = assertThrows(InvalidTreatmentException.class,
                    () -> ValidationUtil.validateTreatment(treatment));
            assertTrue(failure.getDetails().contains("A pet must be selected."));
            assertTrue(failure.getDetails().contains("A veterinarian must be selected."));
        }
    }

    @Nested
    @DisplayName("Shared helpers")
    class Helpers {

        @Test
        void blankTextIsRecognisedInEveryForm() {
            assertTrue(ValidationUtil.isBlank(null));
            assertTrue(ValidationUtil.isBlank(""));
            assertTrue(ValidationUtil.isBlank("   "));
            assertTrue(ValidationUtil.isBlank("\t"));
            assertFalse(ValidationUtil.isBlank("a"));
        }

        @Test
        void cleanTrimsAndNeverReturnsNull() {
            assertEquals("Bruno", ValidationUtil.clean("  Bruno "));
            assertEquals("", ValidationUtil.clean(null));
        }

        @Test
        void theDetailedMessageListsEveryProblem() {
            Owner owner = new Owner("O001", "A", "123", "bad", "", "xyz");
            InvalidOwnerException failure = assertThrows(InvalidOwnerException.class,
                    () -> ValidationUtil.validateOwner(owner));

            String detailed = failure.getDetailedMessage();
            assertTrue(detailed.startsWith("Unable to save owner."));
            assertTrue(detailed.contains("• "));
            assertTrue(detailed.contains("Owner name must be at least 2 characters long."));
        }

        @Test
        void aPetDeclaredAsTheSuperclassIsStillValidated() {
            // Validation takes a Pet, so it works through the polymorphic accessors.
            Pet pet = TestData.bird("P001", "O001", "Kiwi");
            assertDoesNotThrow(() -> ValidationUtil.validatePet(pet));
        }
    }
}
