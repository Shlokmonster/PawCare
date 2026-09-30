package com.pawcare.model.enums;

/** Whether a cat lives indoors, outdoors, or both. */
public enum IndoorOutdoor {

    INDOOR("Indoor"),
    OUTDOOR("Outdoor"),
    BOTH("Indoor & Outdoor");

    private final String label;

    IndoorOutdoor(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static IndoorOutdoor fromLabel(String label) {
        if (label != null) {
            for (IndoorOutdoor v : values()) {
                if (v.label.equalsIgnoreCase(label.trim())) {
                    return v;
                }
            }
        }
        return INDOOR;
    }

    @Override
    public String toString() {
        return label;
    }
}
