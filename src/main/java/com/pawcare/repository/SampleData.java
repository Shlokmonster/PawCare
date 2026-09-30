package com.pawcare.repository;

import com.pawcare.model.Appointment;
import com.pawcare.model.Bird;
import com.pawcare.model.Cat;
import com.pawcare.model.Dog;
import com.pawcare.model.MedicalInfo;
import com.pawcare.model.Owner;
import com.pawcare.model.Pet;
import com.pawcare.model.Treatment;
import com.pawcare.model.Vaccination;
import com.pawcare.model.Veterinarian;
import com.pawcare.model.enums.AppointmentStatus;
import com.pawcare.model.enums.Gender;
import com.pawcare.model.enums.IndoorOutdoor;
import com.pawcare.model.enums.TrainingLevel;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * The demo dataset created the first time the application runs.
 *
 * <p>Every date is expressed relative to {@link LocalDate#now()}, so the data stays
 * meaningful whenever the project is demonstrated: there is always a visit booked for
 * today, an appointment in the past, one in the future, a cancelled booking, an overdue
 * vaccination, one falling due shortly and one that is not due for months. That is what
 * makes the dashboard and the reminder panel show real, non-empty information during a
 * viva.</p>
 */
public final class SampleData {

    private SampleData() {
    }

    // ------------------------------------------------------------------
    // Clients
    // ------------------------------------------------------------------

    public static List<Owner> owners() {
        List<Owner> owners = new ArrayList<>();
        owners.add(new Owner("O001", "Aarav Sharma", "9876543210", "aarav.sharma@example.com",
                "14 Rose Villa, Andheri West, Mumbai 400058", "9820011223"));
        owners.add(new Owner("O002", "Diya Patel", "9823456781", "diya.patel@example.com",
                "7B Sunrise Apartments, Navrangpura, Ahmedabad 380009", "9898012345"));
        owners.add(new Owner("O003", "Rohan Iyer", "9812345672", "rohan.iyer@example.com",
                "22 Lake View Road, Koramangala, Bengaluru 560034", "9845098450"));
        owners.add(new Owner("O004", "Ananya Rao", "9765432183", "ananya.rao@example.com",
                "5 Palm Grove, Banjara Hills, Hyderabad 500034", "9701122334"));
        owners.add(new Owner("O005", "Kabir Menon", "9701234564", "kabir.menon@example.com",
                "31 Marine Lane, Fort Kochi, Kochi 682001", "9744556677"));
        return owners;
    }

    // ------------------------------------------------------------------
    // Clinic staff
    // ------------------------------------------------------------------

    public static List<Veterinarian> veterinarians() {
        List<Veterinarian> vets = new ArrayList<>();
        vets.add(new Veterinarian("V001", "Dr. Meera Nair", "General Medicine",
                "9845567711", "meera.nair@pawcare.example.com"));
        vets.add(new Veterinarian("V002", "Dr. Arjun Desai", "Surgery",
                "9845567722", "arjun.desai@pawcare.example.com"));
        vets.add(new Veterinarian("V003", "Dr. Sneha Kulkarni", "Dermatology",
                "9845567733", "sneha.kulkarni@pawcare.example.com"));
        vets.add(new Veterinarian("V004", "Dr. Vikram Joshi", "Avian & Exotic Pets",
                "9845567744", "vikram.joshi@pawcare.example.com"));
        return vets;
    }

    // ------------------------------------------------------------------
    // Patients – note the three concrete species of the Animal hierarchy
    // ------------------------------------------------------------------

    public static List<Pet> pets() {
        LocalDate today = LocalDate.now();
        List<Pet> pets = new ArrayList<>();

        pets.add(new Dog("P001", "Bruno", 4, 28.5, Gender.MALE, "Labrador Retriever",
                "O001", today.minusDays(420),
                new MedicalInfo("None recorded", "Mild hip dysplasia", "Responds well to handling."),
                TrainingLevel.ADVANCED));

        pets.add(new Cat("P002", "Luna", 3, 4.2, Gender.FEMALE, "Persian",
                "O001", today.minusDays(310),
                new MedicalInfo("Chicken protein", "None recorded", "Prefers a quiet examination room."),
                IndoorOutdoor.INDOOR));

        pets.add(new Bird("P003", "Kiwi", 2, 0.045, Gender.MALE, "Budgerigar",
                "O002", today.minusDays(240),
                new MedicalInfo("None recorded", "None recorded", "Very active during handling."),
                18.5));

        pets.add(new Dog("P004", "Max", 6, 34.0, Gender.MALE, "German Shepherd",
                "O003", today.minusDays(910),
                new MedicalInfo("None recorded", "Osteoarthritis in both knees", "Requires a ramp for the examination table."),
                TrainingLevel.INTERMEDIATE));

        pets.add(new Cat("P005", "Milo", 2, 3.8, Gender.MALE, "Siamese",
                "O003", today.minusDays(190),
                new MedicalInfo("None recorded", "None recorded", "Vocal during vaccination."),
                IndoorOutdoor.BOTH));

        pets.add(new Bird("P006", "Coco", 3, 0.09, Gender.FEMALE, "Cockatiel",
                "O004", today.minusDays(500),
                new MedicalInfo("None recorded", "History of vitamin A deficiency", "Needs a calcium supplement."),
                30.0));

        pets.add(new Dog("P007", "Bella", 5, 12.4, Gender.FEMALE, "Beagle",
                "O004", today.minusDays(640),
                new MedicalInfo("Pollen", "Atopic dermatitis", "Sensitive skin – use medicated shampoo."),
                TrainingLevel.BEGINNER));

        pets.add(new Cat("P008", "Simba", 1, 5.6, Gender.MALE, "Maine Coon",
                "O005", today.minusDays(120),
                new MedicalInfo("None recorded", "None recorded", "Growing rapidly – monitor weight."),
                IndoorOutdoor.INDOOR));

        return pets;
    }

    // ------------------------------------------------------------------
    // Diary
    // ------------------------------------------------------------------

    public static List<Appointment> appointments() {
        LocalDate today = LocalDate.now();
        List<Appointment> appointments = new ArrayList<>();

        appointments.add(new Appointment("A001", "P001", "V001",
                today.minusDays(12), LocalTime.of(10, 0),
                "Annual wellness examination", AppointmentStatus.COMPLETED,
                "Weight stable. Advised to continue the joint supplement."));

        appointments.add(new Appointment("A002", "P004", "V002",
                today.minusDays(6), LocalTime.of(11, 30),
                "Limping on the right hind leg", AppointmentStatus.COMPLETED,
                "Muscle strain confirmed by radiograph. Rest for two weeks."));

        appointments.add(new Appointment("A003", "P002", "V001",
                today.minusDays(2), LocalTime.of(9, 30),
                "Vomiting and loss of appetite", AppointmentStatus.COMPLETED,
                "Mild gastritis. Dietary management started."));

        appointments.add(new Appointment("A004", "P006", "V004",
                today, LocalTime.of(10, 0),
                "Beak overgrowth trimming", AppointmentStatus.SCHEDULED,
                "Owner to bring the bird in a travel cage."));

        appointments.add(new Appointment("A005", "P007", "V003",
                today, LocalTime.of(12, 0),
                "Skin allergy review", AppointmentStatus.SCHEDULED,
                "Continue the medicated bath twice a week."));

        appointments.add(new Appointment("A006", "P003", "V004",
                today.plusDays(3), LocalTime.of(15, 0),
                "Routine avian check-up", AppointmentStatus.SCHEDULED,
                "Follow-up on the dietary supplement."));

        appointments.add(new Appointment("A007", "P008", "V001",
                today.plusDays(7), LocalTime.of(11, 0),
                "Vaccination booster", AppointmentStatus.SCHEDULED,
                "Bring the previous vaccination card."));

        appointments.add(new Appointment("A008", "P005", "V002",
                today.plusDays(10), LocalTime.of(16, 30),
                "Dental scaling", AppointmentStatus.CANCELLED,
                "Cancelled by the owner – to be rescheduled."));

        return appointments;
    }

    // ------------------------------------------------------------------
    // Clinical history
    // ------------------------------------------------------------------

    public static List<Treatment> treatments() {
        LocalDate today = LocalDate.now();
        List<Treatment> treatments = new ArrayList<>();

        treatments.add(new Treatment("T001", "P001", "V001", today.minusDays(12),
                "Healthy – routine examination", "Full physical examination, weight and dental check",
                "None prescribed", "All parameters within normal range."));

        treatments.add(new Treatment("T002", "P004", "V002", today.minusDays(6),
                "Muscle strain, right hind limb", "Rest, cold compress and anti-inflammatory therapy",
                "Meloxicam 1.5 mg/ml oral suspension", "Re-examination after two weeks if lameness persists."));

        treatments.add(new Treatment("T003", "P002", "V001", today.minusDays(2),
                "Acute gastritis", "Fluid therapy and bland diet for five days",
                "Ranitidine oral syrup", "Owner advised to withhold rich food."));

        treatments.add(new Treatment("T004", "P003", "V004", today.minusDays(30),
                "Mild feather plucking", "Dietary supplementation and environmental enrichment",
                "Vitamin A supplement drops", "Add foraging toys to the cage."));

        treatments.add(new Treatment("T005", "P005", "V001", today.minusDays(45),
                "Ear mite infestation", "Ear cleaning followed by acaricide application",
                "Ivermectin otic drops", "Treat all in-contact animals."));

        treatments.add(new Treatment("T006", "P006", "V004", today.minusDays(60),
                "Vitamin A deficiency", "Diet correction with fresh vegetables and pellets",
                "Vitamin A injection (single dose)", "Review after ninety days."));

        treatments.add(new Treatment("T007", "P007", "V003", today.minusDays(20),
                "Atopic dermatitis", "Medicated bath and hypoallergenic diet",
                "Chlorhexidine medicated shampoo", "Avoid pollen exposure during walks."));

        treatments.add(new Treatment("T008", "P008", "V001", today.minusDays(15),
                "Routine deworming", "Oral deworming dose administered",
                "Praziquantel and pyrantel combination", "Repeat after three months."));

        treatments.add(new Treatment("T009", "P001", "V002", today.minusDays(90),
                "Dental tartar accumulation", "Scaling and polishing under sedation",
                "None prescribed", "Brush the teeth twice a week."));

        treatments.add(new Treatment("T010", "P004", "V001", today.minusDays(100),
                "Otitis externa, left ear", "Ear flush and topical antimicrobial therapy",
                "Otic suspension", "Keep the ear dry after bathing."));

        return treatments;
    }

    // ------------------------------------------------------------------
    // Vaccination card – covers overdue, due-soon and upcoming states
    // ------------------------------------------------------------------

    public static List<Vaccination> vaccinations() {
        LocalDate today = LocalDate.now();
        List<Vaccination> vaccinations = new ArrayList<>();

        // Overdue: the next dose date has already passed.
        vaccinations.add(new Vaccination("VAC001", "P001", "V001", "Rabies",
                today.minusDays(370), today.minusDays(5), "Annual booster – now overdue."));

        vaccinations.add(new Vaccination("VAC002", "P002", "V001", "Feline Viral Rhinotracheitis",
                today.minusDays(400), today.minusDays(40), "Reminder sent to the owner."));

        vaccinations.add(new Vaccination("VAC010", "P002", "V001", "Feline Panleukopenia",
                today.minusDays(390), today.minusDays(25), "Second overdue vaccine for this patient."));

        // Due soon: inside the reminder window.
        vaccinations.add(new Vaccination("VAC003", "P004", "V002", "Canine Distemper",
                today.minusDays(350), today.plusDays(12), "Book the booster appointment."));

        vaccinations.add(new Vaccination("VAC004", "P007", "V003", "Canine Parvovirus",
                today.minusDays(340), today.plusDays(25), "Owner prefers a weekend slot."));

        vaccinations.add(new Vaccination("VAC005", "P003", "V004", "Avian Polyomavirus",
                today.minusDays(300), today.plusDays(2), "Due this week."));

        // Upcoming: beyond the reminder window.
        vaccinations.add(new Vaccination("VAC006", "P005", "V001", "Feline Calicivirus",
                today.minusDays(200), today.plusDays(160), "On schedule."));

        vaccinations.add(new Vaccination("VAC007", "P008", "V001", "Feline Leukaemia",
                today.minusDays(100), today.plusDays(265), "First adult booster completed."));

        vaccinations.add(new Vaccination("VAC008", "P001", "V001", "Canine Parvovirus",
                today.minusDays(30), today.plusDays(335), "Given alongside the wellness visit."));

        vaccinations.add(new Vaccination("VAC009", "P006", "V004", "Psittacine Beak and Feather Disease",
                today.minusDays(180), today.plusDays(185), "No adverse reaction observed."));

        return vaccinations;
    }
}
