package com.pawcare.model.enums;

/** Gender of a registered animal. */
public enum Gender {

    MALE("Male"),
    FEMALE("Female"),
    UNKNOWN("Unknown");

    private final String label;

    Gender(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static Gender fromLabel(String label) {
        if (label != null) {
            for (Gender g : values()) {
                if (g.label.equalsIgnoreCase(label.trim())) {
                    return g;
                }
            }
        }
        return UNKNOWN;
    }

    @Override
    public String toString() {
        return label;
    }
}
