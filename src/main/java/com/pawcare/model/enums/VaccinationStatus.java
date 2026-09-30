package com.pawcare.model.enums;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Reminder classification for a vaccination, derived from its next-due date.
 *
 * <p>The rules are centralised here so that the dashboard, the vaccination page and
 * the unit tests all agree on one definition:</p>
 * <ul>
 *   <li>OVERDUE  – nextDueDate is before today</li>
 *   <li>DUE_SOON – today &lt;= nextDueDate &lt;= today + reminder window</li>
 *   <li>UPCOMING – nextDueDate is beyond the reminder window</li>
 * </ul>
 */
public enum VaccinationStatus {

    OVERDUE("Overdue"),
    DUE_SOON("Due Soon"),
    UPCOMING("Upcoming");

    private final String label;

    VaccinationStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /**
     * Classifies a next-due date relative to today.
     *
     * @param nextDueDate  the date the next dose is required
     * @param today        the reference date (injected so tests stay deterministic)
     * @param reminderDays size of the "due soon" window, in days
     */
    public static VaccinationStatus of(LocalDate nextDueDate, LocalDate today, int reminderDays) {
        if (nextDueDate == null) {
            return UPCOMING;
        }
        if (nextDueDate.isBefore(today)) {
            return OVERDUE;
        }
        long days = ChronoUnit.DAYS.between(today, nextDueDate);
        return days <= reminderDays ? DUE_SOON : UPCOMING;
    }

    @Override
    public String toString() {
        return label;
    }
}
