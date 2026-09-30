package com.pawcare.service;

import com.pawcare.exception.InvalidAppointmentException;
import com.pawcare.model.Appointment;
import com.pawcare.model.enums.AppointmentStatus;
import com.pawcare.support.TestData;
import com.pawcare.util.IDGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The clinic diary.
 *
 * <p>The backbone is a {@code TreeMap<LocalDate, ArrayList<Appointment>>}, so most of these
 * tests are about ordering and about what happens to that ordering when a record is edited,
 * cancelled or deleted. The dates are built relative to today, because the validation rule
 * refuses a scheduled visit in the past.</p>
 */
class AppointmentServiceTest {

    private static final LocalDate TODAY = LocalDate.now();
    private static final LocalDate TOMORROW = TODAY.plusDays(1);
    private static final LocalDate NEXT_WEEK = TODAY.plusDays(7);

    private AppointmentService appointments;

    @BeforeEach
    void setUp() {
        IDGenerator.reset();
        appointments = new AppointmentService();
    }

    private static Appointment visit(String id, String petId, String vetId,
                                     LocalDate date, LocalTime time, AppointmentStatus status) {
        return TestData.appointment(id, petId, vetId, date, time, status);
    }

    // ------------------------------------------------------------------
    // CREATE
    // ------------------------------------------------------------------

    @Test
    @DisplayName("a valid appointment is stored and can be found by id")
    void addAndFind() throws InvalidAppointmentException {
        appointments.add(visit("A001", "P001", "V001", TOMORROW, LocalTime.of(10, 0),
                AppointmentStatus.SCHEDULED));

        assertEquals(1, appointments.count());
        assertTrue(appointments.findById("A001").isPresent());
    }

    @Test
    @DisplayName("an invalid appointment is rejected and nothing is stored")
    void anInvalidAppointmentIsNotStored() {
        Appointment tooShort = visit("A001", "P001", "V001", TOMORROW, LocalTime.of(10, 0),
                AppointmentStatus.SCHEDULED);
        tooShort.setReason("hi");

        assertThrows(InvalidAppointmentException.class, () -> appointments.add(tooShort));
        assertEquals(0, appointments.count());
        assertTrue(appointments.findById("A001").isEmpty());
    }

    @Test
    @DisplayName("a duplicate appointment id is refused")
    void duplicateIdsAreRefused() throws InvalidAppointmentException {
        appointments.add(visit("A001", "P001", "V001", TOMORROW, LocalTime.of(10, 0),
                AppointmentStatus.SCHEDULED));

        assertThrows(InvalidAppointmentException.class,
                () -> appointments.add(visit("A001", "P002", "V001", NEXT_WEEK, LocalTime.of(11, 0),
                        AppointmentStatus.SCHEDULED)));
        assertEquals(1, appointments.count());
    }

    @Test
    @DisplayName("a scheduled visit cannot be created in the past")
    void aPastScheduledVisitIsRefused() {
        assertThrows(InvalidAppointmentException.class,
                () -> appointments.add(visit("A001", "P001", "V001", TODAY.minusDays(1),
                        LocalTime.of(10, 0), AppointmentStatus.SCHEDULED)));
        assertEquals(0, appointments.count());
    }

    // ------------------------------------------------------------------
    // The double-booking rule
    // ------------------------------------------------------------------

    @Test
    @DisplayName("the same veterinarian cannot be booked twice at the same moment")
    void doubleBookingIsRefused() throws InvalidAppointmentException {
        appointments.add(visit("A001", "P001", "V001", TOMORROW, LocalTime.of(10, 0),
                AppointmentStatus.SCHEDULED));

        InvalidAppointmentException failure = assertThrows(InvalidAppointmentException.class,
                () -> appointments.add(visit("A002", "P002", "V001", TOMORROW, LocalTime.of(10, 0),
                        AppointmentStatus.SCHEDULED)));
        assertTrue(failure.getDetails().get(0).contains("already has an appointment"));
        assertEquals(1, appointments.count());
    }

    @Test
    @DisplayName("a different time on the same day is fine")
    void aDifferentTimeIsFine() throws InvalidAppointmentException {
        appointments.add(visit("A001", "P001", "V001", TOMORROW, LocalTime.of(10, 0),
                AppointmentStatus.SCHEDULED));
        appointments.add(visit("A002", "P002", "V001", TOMORROW, LocalTime.of(11, 0),
                AppointmentStatus.SCHEDULED));

        assertEquals(2, appointments.count());
    }

    @Test
    @DisplayName("two veterinarians can see two patients at the same moment")
    void twoVetsCanShareASlot() throws InvalidAppointmentException {
        appointments.add(visit("A001", "P001", "V001", TOMORROW, LocalTime.of(10, 0),
                AppointmentStatus.SCHEDULED));
        appointments.add(visit("A002", "P002", "V002", TOMORROW, LocalTime.of(10, 0),
                AppointmentStatus.SCHEDULED));

        assertEquals(2, appointments.count());
    }

    @Test
    @DisplayName("a cancelled slot can be booked again")
    void aCancelledSlotIsFreeAgain() throws InvalidAppointmentException {
        Appointment cancelled = visit("A001", "P001", "V001", TOMORROW, LocalTime.of(10, 0),
                AppointmentStatus.SCHEDULED);
        appointments.add(cancelled);
        appointments.cancel("A001");

        appointments.add(visit("A002", "P002", "V001", TOMORROW, LocalTime.of(10, 0),
                AppointmentStatus.SCHEDULED));

        assertEquals(2, appointments.count());
        assertEquals(AppointmentStatus.SCHEDULED,
                appointments.findById("A002").orElseThrow().getStatus());
    }

    @Test
    @DisplayName("a cancelled booking never blocks anything")
    void aCancelledBookingIsNeverAConflict() throws InvalidAppointmentException {
        appointments.add(visit("A001", "P001", "V001", TOMORROW, LocalTime.of(10, 0),
                AppointmentStatus.SCHEDULED));
        appointments.cancel("A001");

        Appointment candidate = visit("A002", "P002", "V001", TOMORROW, LocalTime.of(10, 0),
                AppointmentStatus.SCHEDULED);
        assertFalse(appointments.hasConflict(candidate));
    }

    @Test
    @DisplayName("an appointment does not conflict with itself when edited")
    void anEditDoesNotClashWithItself() throws InvalidAppointmentException {
        Appointment appointment = visit("A001", "P001", "V001", TOMORROW, LocalTime.of(10, 0),
                AppointmentStatus.SCHEDULED);
        appointments.add(appointment);

        Appointment edited = visit("A001", "P001", "V001", TOMORROW, LocalTime.of(10, 0),
                AppointmentStatus.SCHEDULED);
        edited.setReason("Annual health check");

        // The check must ignore the record being replaced, or no edit could ever be saved.
        appointments.update(edited);
        assertEquals("Annual health check",
                appointments.findById("A001").orElseThrow().getReason());
    }

    // ------------------------------------------------------------------
    // The TreeMap ordering
    // ------------------------------------------------------------------

    @Test
    @DisplayName("appointments come back in date order however they were added")
    void theTreeMapKeepsDatesSorted() throws InvalidAppointmentException {
        appointments.add(visit("A003", "P001", "V001", TODAY.plusDays(20), LocalTime.of(9, 0),
                AppointmentStatus.SCHEDULED));
        appointments.add(visit("A001", "P001", "V001", TOMORROW, LocalTime.of(9, 0),
                AppointmentStatus.SCHEDULED));
        appointments.add(visit("A002", "P002", "V001", TODAY.plusDays(5), LocalTime.of(9, 0),
                AppointmentStatus.SCHEDULED));

        assertEquals(List.of("A001", "A002", "A003"),
                appointments.getAll().stream().map(Appointment::getAppointmentId).toList());
    }

    @Test
    @DisplayName("visits on the same day come back in time order")
    void sameDayVisitsAreOrderedByTime() throws InvalidAppointmentException {
        appointments.add(visit("A001", "P001", "V001", TOMORROW, LocalTime.of(16, 0),
                AppointmentStatus.SCHEDULED));
        appointments.add(visit("A002", "P002", "V001", TOMORROW, LocalTime.of(9, 0),
                AppointmentStatus.SCHEDULED));
        appointments.add(visit("A003", "P003", "V001", TOMORROW, LocalTime.of(12, 30),
                AppointmentStatus.SCHEDULED));

        assertEquals(List.of("A002", "A003", "A001"),
                appointments.on(TOMORROW).stream().map(Appointment::getAppointmentId).toList());
    }

    @Test
    @DisplayName("editing a date moves the record to another day in the calendar")
    void editingADateMovesTheRecord() throws InvalidAppointmentException {
        appointments.add(visit("A001", "P001", "V001", TOMORROW, LocalTime.of(10, 0),
                AppointmentStatus.SCHEDULED));

        Appointment moved = visit("A001", "P001", "V001", NEXT_WEEK, LocalTime.of(10, 0),
                AppointmentStatus.SCHEDULED);
        appointments.update(moved);

        assertEquals(0, appointments.countForDate(TOMORROW));
        assertEquals(1, appointments.countForDate(NEXT_WEEK));
        assertEquals(List.of(NEXT_WEEK), appointments.scheduledDates());
    }

    @Test
    @DisplayName("scheduledDates lists each day that has a booking, oldest first")
    void scheduledDatesAreDistinctAndSorted() throws InvalidAppointmentException {
        appointments.add(visit("A001", "P001", "V001", NEXT_WEEK, LocalTime.of(9, 0),
                AppointmentStatus.SCHEDULED));
        appointments.add(visit("A002", "P002", "V001", TOMORROW, LocalTime.of(9, 0),
                AppointmentStatus.SCHEDULED));
        appointments.add(visit("A003", "P003", "V001", TOMORROW, LocalTime.of(10, 0),
                AppointmentStatus.SCHEDULED));

        assertEquals(List.of(TOMORROW, NEXT_WEEK), appointments.scheduledDates());
        assertEquals(2, appointments.countForDate(TOMORROW));
    }

    @Test
    @DisplayName("a day with no bookings reports an empty list and a zero count")
    void anEmptyDay() {
        assertTrue(appointments.on(TODAY.plusDays(99)).isEmpty());
        assertEquals(0, appointments.countForDate(TODAY.plusDays(99)));
    }

    // ------------------------------------------------------------------
    // Status transitions and filters
    // ------------------------------------------------------------------

    @Test
    @DisplayName("a visit can be completed and cancelled")
    void statusTransitions() throws InvalidAppointmentException {
        appointments.add(visit("A001", "P001", "V001", TOMORROW, LocalTime.of(10, 0),
                AppointmentStatus.SCHEDULED));

        appointments.markCompleted("A001");
        assertEquals(AppointmentStatus.COMPLETED,
                appointments.findById("A001").orElseThrow().getStatus());

        appointments.cancel("A001");
        assertEquals(AppointmentStatus.CANCELLED,
                appointments.findById("A001").orElseThrow().getStatus());
    }

    @Test
    @DisplayName("changing the status of a visit that does not exist is an error")
    void statusTransitionsOnAMissingRecord() {
        assertThrows(InvalidAppointmentException.class, () -> appointments.markCompleted("A999"));
        assertThrows(InvalidAppointmentException.class, () -> appointments.cancel("A999"));
    }

    @Test
    @DisplayName("the status census counts every appointment and always has all three keys")
    void countByStatus() throws InvalidAppointmentException {
        appointments.add(visit("A001", "P001", "V001", TOMORROW, LocalTime.of(9, 0),
                AppointmentStatus.SCHEDULED));
        appointments.add(visit("A002", "P002", "V001", TOMORROW, LocalTime.of(10, 0),
                AppointmentStatus.SCHEDULED));
        appointments.add(visit("A003", "P003", "V001", TODAY.minusDays(10), LocalTime.of(10, 0),
                AppointmentStatus.COMPLETED));
        appointments.add(visit("A004", "P004", "V001", TODAY.minusDays(20), LocalTime.of(10, 0),
                AppointmentStatus.CANCELLED));

        Map<AppointmentStatus, Integer> counts = appointments.countByStatus();

        // A status nobody has yet still reports zero, so the dashboard chart has no gaps.
        assertEquals(3, counts.size());
        assertEquals(2, counts.get(AppointmentStatus.SCHEDULED).intValue());
        assertEquals(1, counts.get(AppointmentStatus.COMPLETED).intValue());
        assertEquals(1, counts.get(AppointmentStatus.CANCELLED).intValue());
    }

    @Test
    @DisplayName("only scheduled visits in the future count as upcoming")
    void upcomingSkipsPastAndCancelledVisits() throws InvalidAppointmentException {
        appointments.add(visit("A001", "P001", "V001", TOMORROW, LocalTime.of(9, 0),
                AppointmentStatus.SCHEDULED));
        appointments.add(visit("A002", "P002", "V001", NEXT_WEEK, LocalTime.of(9, 0),
                AppointmentStatus.SCHEDULED));
        appointments.add(visit("A003", "P003", "V001", TODAY.minusDays(3), LocalTime.of(9, 0),
                AppointmentStatus.COMPLETED));
        appointments.add(visit("A004", "P004", "V001", NEXT_WEEK, LocalTime.of(10, 0),
                AppointmentStatus.CANCELLED));

        List<Appointment> upcoming = appointments.upcoming(10);

        assertEquals(List.of("A001", "A002"),
                upcoming.stream().map(Appointment::getAppointmentId).toList());
    }

    @Test
    @DisplayName("the upcoming list respects its limit")
    void upcomingRespectsTheLimit() throws InvalidAppointmentException {
        for (int i = 1; i <= 5; i++) {
            appointments.add(visit("A00" + i, "P00" + i, "V001",
                    TODAY.plusDays(i), LocalTime.of(9, 0), AppointmentStatus.SCHEDULED));
        }
        assertEquals(3, appointments.upcoming(3).size());
    }

    @Test
    @DisplayName("today lists every booking made for today, whatever its status")
    void todayIncludesEveryStatus() throws InvalidAppointmentException {
        appointments.add(visit("A001", "P001", "V001", TODAY, LocalTime.of(9, 0),
                AppointmentStatus.SCHEDULED));
        appointments.add(visit("A002", "P002", "V002", TODAY, LocalTime.of(11, 0),
                AppointmentStatus.COMPLETED));
        appointments.add(visit("A003", "P003", "V003", TOMORROW, LocalTime.of(9, 0),
                AppointmentStatus.SCHEDULED));

        assertEquals(2, appointments.today().size());
        assertEquals("A001", appointments.today().get(0).getAppointmentId());
    }

    @Test
    @DisplayName("visits can be filtered by status, veterinarian and pet")
    void theFilters() throws InvalidAppointmentException {
        appointments.add(visit("A001", "P001", "V001", TOMORROW, LocalTime.of(9, 0),
                AppointmentStatus.SCHEDULED));
        appointments.add(visit("A002", "P001", "V002", TOMORROW, LocalTime.of(10, 0),
                AppointmentStatus.SCHEDULED));
        appointments.add(visit("A003", "P002", "V001", TODAY.minusDays(1), LocalTime.of(9, 0),
                AppointmentStatus.COMPLETED));

        assertEquals(2, appointments.byStatus(AppointmentStatus.SCHEDULED).size());
        assertEquals(1, appointments.byStatus(AppointmentStatus.COMPLETED).size());
        assertEquals(2, appointments.byVeterinarian("V001").size());
        assertEquals(1, appointments.byVeterinarian("V002").size());
        assertEquals(3, appointments.byVeterinarian(null).size());
        assertEquals(2, appointments.byPet("P001").size());
        assertTrue(appointments.byPet("P999").isEmpty());
        assertTrue(appointments.byPet(null).isEmpty());
    }

    @Test
    @DisplayName("a pet's next visit is the earliest scheduled one still ahead")
    void nextForPet() throws InvalidAppointmentException {
        appointments.add(visit("A001", "P001", "V001", NEXT_WEEK, LocalTime.of(9, 0),
                AppointmentStatus.SCHEDULED));
        appointments.add(visit("A002", "P001", "V002", TOMORROW, LocalTime.of(9, 0),
                AppointmentStatus.SCHEDULED));
        appointments.add(visit("A003", "P001", "V001", TODAY.minusDays(5), LocalTime.of(9, 0),
                AppointmentStatus.COMPLETED));

        assertEquals("A002", appointments.nextForPet("P001").orElseThrow().getAppointmentId());
        assertTrue(appointments.nextForPet("P999").isEmpty());
    }

    // ------------------------------------------------------------------
    // DELETE
    // ------------------------------------------------------------------

    @Test
    @DisplayName("deleting removes the visit from the calendar as well")
    void deleteClearsBothStructures() throws InvalidAppointmentException {
        appointments.add(visit("A001", "P001", "V001", TOMORROW, LocalTime.of(9, 0),
                AppointmentStatus.SCHEDULED));

        assertTrue(appointments.delete("A001"));

        assertEquals(0, appointments.count());
        assertTrue(appointments.findById("A001").isEmpty());
        assertEquals(0, appointments.countForDate(TOMORROW));
        assertTrue(appointments.scheduledDates().isEmpty());
    }

    @Test
    @DisplayName("deleting something that is not there reports that nothing happened")
    void deleteReportsWhetherItDidAnything() {
        assertFalse(appointments.delete("A999"));
        assertFalse(appointments.delete(null));
    }

    @Test
    @DisplayName("deleting a pet's visits removes them all and reports how many")
    void deleteByPet() throws InvalidAppointmentException {
        appointments.add(visit("A001", "P001", "V001", TOMORROW, LocalTime.of(9, 0),
                AppointmentStatus.SCHEDULED));
        appointments.add(visit("A002", "P001", "V002", NEXT_WEEK, LocalTime.of(9, 0),
                AppointmentStatus.SCHEDULED));
        appointments.add(visit("A003", "P002", "V001", TOMORROW, LocalTime.of(11, 0),
                AppointmentStatus.SCHEDULED));

        assertEquals(2, appointments.deleteByPet("P001"));

        assertEquals(1, appointments.count());
        assertTrue(appointments.byPet("P001").isEmpty());
        assertEquals(1, appointments.byPet("P002").size());
    }

    @Test
    @DisplayName("deleting the visits of a pet that has none is harmless")
    void deleteByPetWithNothingToDelete() {
        assertEquals(0, appointments.deleteByPet("P999"));
        assertEquals(0, appointments.deleteByPet(null));
    }

    // ------------------------------------------------------------------
    // Comparators and bulk loading
    // ------------------------------------------------------------------

    @Test
    @DisplayName("the exposed comparators order the list the way the table does")
    void theComparators() throws InvalidAppointmentException {
        appointments.add(visit("A001", "P001", "V002", NEXT_WEEK, LocalTime.of(9, 0),
                AppointmentStatus.SCHEDULED));
        appointments.add(visit("A002", "P002", "V001", TOMORROW, LocalTime.of(9, 0),
                AppointmentStatus.SCHEDULED));

        assertEquals(List.of("A002", "A001"),
                AppointmentService.sort(appointments.getAll(), AppointmentService.BY_DATE_THEN_TIME)
                        .stream().map(Appointment::getAppointmentId).toList());
        assertEquals(List.of("A002", "A001"),
                AppointmentService.sort(appointments.getAll(), AppointmentService.BY_VET)
                        .stream().map(Appointment::getAppointmentId).toList());
        assertEquals(2, AppointmentService.sort(appointments.getAll(),
                AppointmentService.BY_STATUS).size());
    }

    @Test
    @DisplayName("a time is normalised so seconds never break equality")
    void timesAreNormalised() {
        assertEquals(LocalTime.of(10, 30), AppointmentService.normalise(LocalTime.of(10, 30, 45, 123)));
        assertNull(AppointmentService.normalise(null));
    }

    @Test
    @DisplayName("loadAll replaces the calendar and skips broken records")
    void loadAllReplacesEverything() throws InvalidAppointmentException {
        appointments.add(visit("A001", "P001", "V001", TOMORROW, LocalTime.of(9, 0),
                AppointmentStatus.SCHEDULED));

        Appointment noId = visit("A002", "P002", "V001", NEXT_WEEK, LocalTime.of(9, 0),
                AppointmentStatus.SCHEDULED);
        noId.setAppointmentId(null);
        appointments.loadAll(List.of(visit("A009", "P003", "V001", NEXT_WEEK, LocalTime.of(9, 0),
                AppointmentStatus.SCHEDULED), noId));

        assertEquals(1, appointments.count());
        assertTrue(appointments.findById("A001").isEmpty());
        assertTrue(appointments.findById("A009").isPresent());
    }

    @Test
    @DisplayName("loadAll tolerates a null list and an empty calendar")
    void loadAllAcceptsNull() {
        appointments.loadAll(null);
        assertEquals(0, appointments.count());
        assertTrue(appointments.getAll().isEmpty());
    }

    @Test
    @DisplayName("loading seeds the id generator past the highest stored id")
    void loadAllSyncsTheIdGenerator() {
        appointments.loadAll(List.of(visit("A021", "P001", "V001", TOMORROW, LocalTime.of(9, 0),
                AppointmentStatus.SCHEDULED)));
        assertEquals("A022", IDGenerator.nextAppointmentId());
    }
}
