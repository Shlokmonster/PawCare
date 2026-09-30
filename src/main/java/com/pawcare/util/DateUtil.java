package com.pawcare.util;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

/** Formatting helpers that keep dates and times consistent across every screen. */
public final class DateUtil {

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter DATE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a", Locale.ENGLISH);
    private static final DateTimeFormatter TIME_FORMAT =
            DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH);
    private static final DateTimeFormatter LONG_DATE_FORMAT =
            DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.ENGLISH);

    private DateUtil() {
    }

    /** e.g. {@code 30 Sep 2026}. Returns an em dash for null so tables never show "null". */
    public static String format(LocalDate date) {
        return date == null ? "—" : date.format(DATE_FORMAT);
    }

    public static String format(LocalDate date, LocalTime time) {
        if (date == null) {
            return "—";
        }
        return time == null ? format(date) : date.format(DATE_FORMAT) + ", " + format(time);
    }

    public static String formatDateTime(LocalDate date, LocalTime time) {
        return date == null ? "—" : date.atTime(time == null ? LocalTime.MIDNIGHT : time).format(DATE_TIME_FORMAT);
    }

    /** e.g. {@code 10:30 AM}. */
    public static String format(LocalTime time) {
        return time == null ? "—" : time.format(TIME_FORMAT);
    }

    /** e.g. {@code Wednesday, 30 September 2026} – used in the page headers. */
    public static String formatLong(LocalDate date) {
        return date == null ? "—" : date.format(LONG_DATE_FORMAT);
    }

    /** Days from today to the given date; negative when the date has passed. */
    public static long daysFromToday(LocalDate date) {
        return date == null ? 0 : ChronoUnit.DAYS.between(LocalDate.now(), date);
    }

    /** Friendly relative label such as "Today", "Tomorrow" or "in 5 days". */
    public static String relative(LocalDate date) {
        if (date == null) {
            return "—";
        }
        long days = daysFromToday(date);
        if (days == 0) {
            return "Today";
        }
        if (days == 1) {
            return "Tomorrow";
        }
        if (days == -1) {
            return "Yesterday";
        }
        return days > 0 ? "in " + days + " days" : Math.abs(days) + " days ago";
    }

    /** Short urgency label used by the vaccination list, e.g. "3 days overdue". */
    public static String overdueLabel(LocalDate date) {
        if (date == null) {
            return "—";
        }
        long days = daysFromToday(date);
        if (days < 0) {
            return Math.abs(days) + (Math.abs(days) == 1 ? " day overdue" : " days overdue");
        }
        if (days == 0) {
            return "Due today";
        }
        return "Due in " + days + (days == 1 ? " day" : " days");
    }

    // ------------------------------------------------------------------
    // Parsing – used by the dialogs, which accept typed dates and times
    // ------------------------------------------------------------------

    /**
     * Formats accepted by {@link #parseDate(String)}, in the order they are tried.
     *
     * <p>Accepting several spellings means a user can type whichever form they are used
     * to; {@link #DISPLAY_DATE_PATTERN} is the one the application itself prints, so it is
     * tried first.</p>
     *
     * <p>The year is written {@code uuuu} rather than {@code yyyy} because these patterns are
     * used with {@link ResolverStyle#STRICT}. {@code yyyy} is the <i>year of an era</i> and
     * cannot be resolved to a date on its own once lenient resolution is switched off;
     * {@code uuuu} is the plain proleptic year and parses exactly the same text.</p>
     */
    private static final String[] DATE_PATTERNS = {
            "dd-MM-uuuu", "dd/MM/uuuu", "d-M-uuuu", "d/M/uuuu",
            "uuuu-MM-dd", "dd MMM uuuu", "d MMM uuuu"
    };

    private static final String[] TIME_PATTERNS = {
            "HH:mm", "H:mm", "hh:mm a", "h:mm a", "HH:mm:ss"
    };

    /** The pattern shown in the hint under every date field. */
    public static final String DISPLAY_DATE_PATTERN = "dd-MM-yyyy";
    /** The pattern shown in the hint under every time field. */
    public static final String DISPLAY_TIME_PATTERN = "HH:mm";

    /**
     * Parses a date typed by the user.
     *
     * <p>Resolution is {@link ResolverStyle#STRICT}, which means an impossible date is
     * rejected instead of being rounded to a real one. With the default ({@code SMART})
     * the formatter would quietly turn "31-02-2026" into 28 February and the clinic would
     * store a date nobody typed; under {@code STRICT} the pattern simply does not match and
     * the caller gets a {@link DateTimeParseException} to show the user.</p>
     *
     * @return the parsed date
     * @throws DateTimeParseException when none of the accepted patterns match
     */
    public static LocalDate parseDate(String text) {
        if (text == null || text.trim().isEmpty()) {
            throw new DateTimeParseException("Date is empty", "", 0);
        }
        String cleaned = text.trim();
        for (String pattern : DATE_PATTERNS) {
            try {
                return LocalDate.parse(cleaned, DateTimeFormatter
                        .ofPattern(pattern, Locale.ENGLISH)
                        .withResolverStyle(ResolverStyle.STRICT));
            } catch (DateTimeParseException ignored) {
                // Try the next accepted spelling.
            }
        }
        throw new DateTimeParseException("Unrecognised date: " + cleaned, cleaned, 0);
    }

    /**
     * Parses a time typed by the user.
     *
     * <p>Like {@link #parseDate(String)} this resolves strictly, so {@code 25:00} is refused
     * rather than wrapped round to 01:00.</p>
     *
     * @return the parsed time, with seconds and nanoseconds cleared so that two equal
     *         times always compare equal
     * @throws DateTimeParseException when none of the accepted patterns match
     */
    public static LocalTime parseTime(String text) {
        if (text == null || text.trim().isEmpty()) {
            throw new DateTimeParseException("Time is empty", "", 0);
        }
        String cleaned = text.trim().toUpperCase(Locale.ENGLISH);
        for (String pattern : TIME_PATTERNS) {
            try {
                return LocalTime.parse(cleaned, DateTimeFormatter
                                .ofPattern(pattern, Locale.ENGLISH)
                                .withResolverStyle(ResolverStyle.STRICT))
                        .withSecond(0).withNano(0);
            } catch (DateTimeParseException ignored) {
                // Try the next accepted spelling.
            }
        }
        throw new DateTimeParseException("Unrecognised time: " + cleaned, cleaned, 0);
    }

    /** Parses a date, returning null instead of throwing – handy for optional fields. */
    public static LocalDate parseDateOrNull(String text) {
        try {
            return parseDate(text);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    /** Parses a time, returning null instead of throwing. */
    public static LocalTime parseTimeOrNull(String text) {
        try {
            return parseTime(text);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    /** The editable form of a date, e.g. {@code 30-09-2026}. */
    public static String toEditable(LocalDate date) {
        return date == null ? "" : date.format(
                DateTimeFormatter.ofPattern(DISPLAY_DATE_PATTERN, Locale.ENGLISH));
    }

    /** The editable form of a time, e.g. {@code 14:30}. */
    public static String toEditable(LocalTime time) {
        return time == null ? "" : time.format(
                DateTimeFormatter.ofPattern(DISPLAY_TIME_PATTERN, Locale.ENGLISH));
    }
}
