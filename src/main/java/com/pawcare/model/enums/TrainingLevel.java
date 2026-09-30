package com.pawcare.model.enums;

/** Obedience / training level recorded for a dog. */
public enum TrainingLevel {

    BEGINNER("Beginner"),
    INTERMEDIATE("Intermediate"),
    ADVANCED("Advanced");

    private final String label;

    TrainingLevel(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static TrainingLevel fromLabel(String label) {
        if (label != null) {
            for (TrainingLevel t : values()) {
                if (t.label.equalsIgnoreCase(label.trim())) {
                    return t;
                }
            }
        }
        return BEGINNER;
    }

    @Override
    public String toString() {
        return label;
    }
}
