package com.pawcare.model.enums;

import com.pawcare.support.TestData;
import org.junit.jupiter.api.DisplayName;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * The vaccination reminder rule.
 *
 * <p>The reference date is injected rather than read from the clock, so these tests give the
 * same answer in 2026 and in 2036. That is the whole reason
 * {@link VaccinationStatus#of(LocalDate, LocalDate, int)} takes {@code today} as a
 * parameter.</p>
 */
class VaccinationStatusTest {

    /** A fixed "today" so the boundaries below are exact. */
    private static final LocalDate TODAY = LocalDate.of(2026, 9, 30);
    private static final int WINDOW = 30;

    private static VaccinationStatus classify(LocalDate nextDue) {
        return VaccinationStatus.of(nextDue, TODAY, WINDOW);
    }

    @Test
    @DisplayName("a dose due before today is OVERDUE")
    void yesterdayIsOverdue() {
        assertSame(VaccinationStatus.OVERDUE, classify(TODAY.minusDays(1)));
        assertSame(VaccinationStatus.OVERDUE, classify(TODAY.minusYears(2)));
    }

    @Test
    @DisplayName("a dose due today is DUE_SOON, not OVERDUE")
    void todayIsDueSoon() {
        assertSame(VaccinationStatus.DUE_SOON, classify(TODAY));
    }

    @Test
    @DisplayName("the window is inclusive at both ends")
    void windowEdges() {
        assertSame(VaccinationStatus.DUE_SOON, classify(TODAY.plusDays(1)));
        assertSame(VaccinationStatus.DUE_SOON, classify(TODAY.plusDays(WINDOW)));
        assertSame(VaccinationStatus.UPCOMING, classify(TODAY.plusDays(WINDOW + 1)));
    }

    @Test
    @DisplayName("a record with no next-due date is never a reminder")
    void aMissingDateIsUpcoming() {
        assertSame(VaccinationStatus.UPCOMING, VaccinationStatus.of(null, TODAY, WINDOW));
    }

    @Test
    @DisplayName("a wider window pulls more doses into DUE_SOON")
    void theWindowChangesTheClassification() {
        LocalDate tenDaysAway = TODAY.plusDays(10);

        assertSame(VaccinationStatus.UPCOMING, VaccinationStatus.of(tenDaysAway, TODAY, 7));
        assertSame(VaccinationStatus.DUE_SOON, VaccinationStatus.of(tenDaysAway, TODAY, 14));
    }

    @Test
    @DisplayName("a zero-day window still flags today")
    void aZeroWindowStillFlagsToday() {
        assertSame(VaccinationStatus.DUE_SOON, VaccinationStatus.of(TODAY, TODAY, 0));
        assertSame(VaccinationStatus.UPCOMING, VaccinationStatus.of(TODAY.plusDays(1), TODAY, 0));
    }

    @Test
    @DisplayName("every state carries the label the interface shows")
    void labels() {
        assertEquals("Overdue", VaccinationStatus.OVERDUE.getLabel());
        assertEquals("Due Soon", VaccinationStatus.DUE_SOON.getLabel());
        assertEquals("Upcoming", VaccinationStatus.UPCOMING.getLabel());
        assertEquals("Due Soon", VaccinationStatus.DUE_SOON.toString());
    }

    @Test
    @DisplayName("a Vaccination delegates the rule to the enum")
    void theModelDelegatesToTheEnum() {
        assertSame(VaccinationStatus.OVERDUE,
                TestData.vaccinationDue("VAC001", "P001", TODAY.minusDays(5)).statusOn(TODAY, WINDOW));
        assertSame(VaccinationStatus.DUE_SOON,
                TestData.vaccinationDue("VAC002", "P001", TODAY.plusDays(5)).statusOn(TODAY, WINDOW));
        assertSame(VaccinationStatus.UPCOMING,
                TestData.vaccinationDue("VAC003", "P001", TODAY.plusDays(300)).statusOn(TODAY, WINDOW));
    }
}
