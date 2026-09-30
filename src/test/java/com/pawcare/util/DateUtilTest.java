package com.pawcare.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Formatting and parsing of the dates and times the user sees and types. */
class DateUtilTest {

    private static final LocalDate DATE = LocalDate.of(2026, 9, 30);

    @Nested
    @DisplayName("Formatting")
    class Formatting {

        @Test
        void datesArePrintedInTheReadableForm() {
            assertEquals("30 Sep 2026", DateUtil.format(DATE));
        }

        @Test
        void aNullDateBecomesAnEmDashRatherThanTheWordNull() {
            // A table cell showing "null" looks like a bug; an em dash looks deliberate.
            assertEquals("—", DateUtil.format((LocalDate) null));
            assertEquals("—", DateUtil.format((LocalTime) null));
            assertEquals("—", DateUtil.formatLong(null));
        }

        @Test
        void timesUseATwelveHourClock() {
            assertEquals("10:30 AM", DateUtil.format(LocalTime.of(10, 30)));
            assertEquals("02:05 PM", DateUtil.format(LocalTime.of(14, 5)));
            assertEquals("12:00 AM", DateUtil.format(LocalTime.MIDNIGHT));
            assertEquals("12:00 PM", DateUtil.format(LocalTime.NOON));
        }

        @Test
        void aDateAndTimeCanBeJoined() {
            assertEquals("30 Sep 2026, 10:30 AM", DateUtil.format(DATE, LocalTime.of(10, 30)));
            assertEquals("30 Sep 2026", DateUtil.format(DATE, null));
        }

        @Test
        void theLongFormIsUsedInThePageHeaders() {
            assertEquals("Wednesday, 30 September 2026", DateUtil.formatLong(DATE));
        }

        @Test
        void relativeLabelsReadNaturally() {
            assertEquals("Today", DateUtil.relative(LocalDate.now()));
            assertEquals("Tomorrow", DateUtil.relative(LocalDate.now().plusDays(1)));
            assertEquals("Yesterday", DateUtil.relative(LocalDate.now().minusDays(1)));
            assertEquals("in 5 days", DateUtil.relative(LocalDate.now().plusDays(5)));
            assertEquals("3 days ago", DateUtil.relative(LocalDate.now().minusDays(3)));
            assertEquals("—", DateUtil.relative(null));
        }

        @Test
        void overdueLabelsAreSingularWhenTheyShouldBe() {
            assertEquals("Due today", DateUtil.overdueLabel(LocalDate.now()));
            assertEquals("Due in 1 day", DateUtil.overdueLabel(LocalDate.now().plusDays(1)));
            assertEquals("Due in 4 days", DateUtil.overdueLabel(LocalDate.now().plusDays(4)));
            assertEquals("1 day overdue", DateUtil.overdueLabel(LocalDate.now().minusDays(1)));
            assertEquals("9 days overdue", DateUtil.overdueLabel(LocalDate.now().minusDays(9)));
        }

        @Test
        void daysFromTodayCountsForwardsAndBackwards() {
            assertEquals(0, DateUtil.daysFromToday(LocalDate.now()));
            assertEquals(7, DateUtil.daysFromToday(LocalDate.now().plusDays(7)));
            assertEquals(-7, DateUtil.daysFromToday(LocalDate.now().minusDays(7)));
        }
    }

    @Nested
    @DisplayName("Parsing")
    class Parsing {

        @Test
        void theDisplayedPatternRoundTrips() {
            String typed = DateUtil.toEditable(DATE);
            assertEquals("30-09-2026", typed);
            assertEquals(DATE, DateUtil.parseDate(typed));
        }

        @Test
        void timesRoundTripAndLoseTheirSeconds() {
            LocalTime time = LocalTime.of(14, 30, 45);
            assertEquals("14:30", DateUtil.toEditable(time));
            assertEquals(LocalTime.of(14, 30), DateUtil.parseTime(DateUtil.toEditable(time)));
        }

        @Test
        void everyAcceptedDateSpellingIsUnderstood() {
            assertEquals(DATE, DateUtil.parseDate("30-09-2026"));
            assertEquals(DATE, DateUtil.parseDate("30/09/2026"));
            assertEquals(DATE, DateUtil.parseDate("30-9-2026"));
            assertEquals(DATE, DateUtil.parseDate("2026-09-30"));
            assertEquals(DATE, DateUtil.parseDate("30 Sep 2026"));
        }

        @Test
        void surroundingSpacesAreIgnored() {
            assertEquals(DATE, DateUtil.parseDate("  30-09-2026  "));
            assertEquals(LocalTime.of(9, 5), DateUtil.parseTime(" 9:05 "));
        }

        @Test
        void everyAcceptedTimeSpellingIsUnderstood() {
            assertEquals(LocalTime.of(14, 30), DateUtil.parseTime("14:30"));
            assertEquals(LocalTime.of(9, 5), DateUtil.parseTime("9:05"));
            assertEquals(LocalTime.of(14, 30), DateUtil.parseTime("2:30 PM"));
            assertEquals(LocalTime.of(9, 5), DateUtil.parseTime("9:05 am"));
            assertEquals(LocalTime.of(14, 30), DateUtil.parseTime("14:30:59"));
        }

        @Test
        void nonsenseIsRejectedRatherThanSilentlyGuessed() {
            assertThrows(DateTimeParseException.class, () -> DateUtil.parseDate("not a date"));
            assertThrows(DateTimeParseException.class, () -> DateUtil.parseDate("31-02-2026"));
            assertThrows(DateTimeParseException.class, () -> DateUtil.parseDate(""));
            assertThrows(DateTimeParseException.class, () -> DateUtil.parseDate(null));
            assertThrows(DateTimeParseException.class, () -> DateUtil.parseTime("half past two"));
            assertThrows(DateTimeParseException.class, () -> DateUtil.parseTime("25:00"));
        }

        @Test
        void theNullOrReturningVariantsNeverThrow() {
            assertNull(DateUtil.parseDateOrNull("not a date"));
            assertNull(DateUtil.parseDateOrNull(""));
            assertEquals(DATE, DateUtil.parseDateOrNull("30-09-2026"));
            assertNull(DateUtil.parseTimeOrNull("banana"));
            assertEquals(LocalTime.of(8, 0), DateUtil.parseTimeOrNull("08:00"));
        }

        @Test
        void theEditableFormOfNothingIsEmptyRatherThanAnEmDash() {
            // The em dash belongs in a table cell, not in a text field the user types into.
            assertEquals("", DateUtil.toEditable((LocalDate) null));
            assertEquals("", DateUtil.toEditable((LocalTime) null));
        }

        @Test
        void theHintPatternsMatchWhatTheApplicationPrints() {
            assertEquals("30-09-2026", DATE.format(
                    java.time.format.DateTimeFormatter.ofPattern(DateUtil.DISPLAY_DATE_PATTERN)));
            assertEquals("14:30", LocalTime.of(14, 30).format(
                    java.time.format.DateTimeFormatter.ofPattern(DateUtil.DISPLAY_TIME_PATTERN)));
        }
    }

    @Nested
    @DisplayName("Month and day names")
    class LocaleSafety {

        @Test
        void monthNamesAreAlwaysEnglish() {
            // The locale is pinned, so the output does not change with the machine's settings.
            assertTrue(DateUtil.format(LocalDate.of(2026, 1, 5)).contains("Jan"));
            assertTrue(DateUtil.formatLong(LocalDate.of(2026, 12, 25)).contains("December"));
        }
    }
}
