package com.pawcare.util;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Produces the human-readable identifiers shown throughout the application.
 *
 * <p>Ids follow the pattern {@code P001}, {@code O001}, {@code V001}, {@code A001},
 * {@code T001} and {@code VAC001}. Counters live in a {@link HashMap} keyed by prefix.
 * After data is loaded from disk the counters are re-seeded from the existing ids, so
 * a restart never issues an id that is already in use.</p>
 */
public final class IDGenerator {

    public static final String PET_PREFIX = "P";
    public static final String OWNER_PREFIX = "O";
    public static final String VET_PREFIX = "V";
    public static final String APPOINTMENT_PREFIX = "A";
    public static final String TREATMENT_PREFIX = "T";
    public static final String VACCINATION_PREFIX = "VAC";

    /** Prefix -> last number issued. HashMap gives constant-time counter updates. */
    private static final Map<String, Integer> COUNTERS = new HashMap<>();

    private IDGenerator() {
    }

    private static synchronized String next(String prefix, int digits) {
        int value = COUNTERS.getOrDefault(prefix, 0) + 1;
        COUNTERS.put(prefix, value);
        return prefix + String.format("%0" + digits + "d", value);
    }

    public static String nextPetId() {
        return next(PET_PREFIX, 3);
    }

    public static String nextOwnerId() {
        return next(OWNER_PREFIX, 3);
    }

    public static String nextVeterinarianId() {
        return next(VET_PREFIX, 3);
    }

    public static String nextAppointmentId() {
        return next(APPOINTMENT_PREFIX, 3);
    }

    public static String nextTreatmentId() {
        return next(TREATMENT_PREFIX, 3);
    }

    public static String nextVaccinationId() {
        return next(VACCINATION_PREFIX, 3);
    }

    /**
     * Raises the counter for a prefix so it is at least as high as the supplied id.
     * Called once after loading data from disk.
     */
    public static synchronized void sync(String prefix, String existingId) {
        if (existingId == null || prefix == null || !existingId.startsWith(prefix)) {
            return;
        }
        String numericPart = existingId.substring(prefix.length()).replaceAll("\\D", "");
        if (numericPart.isEmpty()) {
            return;
        }
        try {
            int value = Integer.parseInt(numericPart);
            COUNTERS.merge(prefix, value, Math::max);
        } catch (NumberFormatException ignored) {
            // An id we cannot parse simply does not influence the counter.
        }
    }

    /** Convenience overload: re-seeds one prefix from a collection of existing ids. */
    public static void sync(String prefix, Collection<String> existingIds) {
        if (existingIds == null) {
            return;
        }
        for (String id : existingIds) {
            sync(prefix, id);
        }
    }

    /** Clears every counter – used before generating the demo dataset from scratch. */
    public static synchronized void reset() {
        COUNTERS.clear();
    }
}
