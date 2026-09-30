package com.pawcare.model.enums;

/** Lifecycle state of an appointment. */
public enum AppointmentStatus {

    SCHEDULED("Scheduled"),
    COMPLETED("Completed"),
    CANCELLED("Cancelled");

    private final String label;

    AppointmentStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static AppointmentStatus fromLabel(String label) {
        if (label != null) {
            for (AppointmentStatus s : values()) {
                if (s.label.equalsIgnoreCase(label.trim())) {
                    return s;
                }
            }
        }
        return SCHEDULED;
    }

    @Override
    public String toString() {
        return label;
    }
}
