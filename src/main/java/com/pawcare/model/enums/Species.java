package com.pawcare.model.enums;

/**
 * The species supported by the clinic.
 *
 * <p>An enum is used instead of raw Strings so that an invalid species can never be
 * represented. The animal hierarchy reports its species through this enum.</p>
 */
public enum Species {

    DOG("Dog"),
    CAT("Cat"),
    BIRD("Bird");

    private final String label;

    Species(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /** Resolves a species from its display label, defaulting to DOG when unknown. */
    public static Species fromLabel(String label) {
        if (label != null) {
            for (Species s : values()) {
                if (s.label.equalsIgnoreCase(label.trim())) {
                    return s;
                }
            }
        }
        return DOG;
    }

    @Override
    public String toString() {
        return label;
    }
}
