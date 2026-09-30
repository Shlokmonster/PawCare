package com.pawcare.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * The id counters.
 *
 * <p>Ids are a static counter shared by the whole application, so every test starts from a
 * clean slate with {@link IDGenerator#reset()} – otherwise the order the tests happen to run
 * in would decide whether they pass.</p>
 */
class IDGeneratorTest {

    @BeforeEach
    void freshCounters() {
        IDGenerator.reset();
    }

    @Test
    @DisplayName("ids follow the P001 / A001 / VAC001 pattern")
    void everyPrefixHasItsOwnShape() {
        assertEquals("P001", IDGenerator.nextPetId());
        assertEquals("O001", IDGenerator.nextOwnerId());
        assertEquals("V001", IDGenerator.nextVeterinarianId());
        assertEquals("A001", IDGenerator.nextAppointmentId());
        assertEquals("T001", IDGenerator.nextTreatmentId());
        assertEquals("VAC001", IDGenerator.nextVaccinationId());
    }

    @Test
    @DisplayName("each call moves that prefix on by one")
    void countersAdvance() {
        assertEquals("P001", IDGenerator.nextPetId());
        assertEquals("P002", IDGenerator.nextPetId());
        assertEquals("P003", IDGenerator.nextPetId());
    }

    @Test
    @DisplayName("counters are independent of one another")
    void countersDoNotInterfere() {
        IDGenerator.nextPetId();
        IDGenerator.nextPetId();
        assertEquals("A001", IDGenerator.nextAppointmentId());
        assertEquals("P003", IDGenerator.nextPetId());
    }

    @Test
    @DisplayName("a number that runs past three digits is still readable")
    void wideNumbersAreNotTruncated() {
        for (int i = 0; i < 999; i++) {
            IDGenerator.nextPetId();
        }
        assertEquals("P1000", IDGenerator.nextPetId());
    }

    @Test
    @DisplayName("sync raises the counter so a reloaded id is never reissued")
    void syncRaisesTheCounter() {
        IDGenerator.sync(IDGenerator.PET_PREFIX, "P007");
        assertEquals("P008", IDGenerator.nextPetId());
    }

    @Test
    @DisplayName("sync ignores a foreign prefix")
    void syncIgnoresOtherPrefixes() {
        IDGenerator.sync(IDGenerator.PET_PREFIX, "O042");
        assertEquals("P001", IDGenerator.nextPetId());
    }

    @Test
    @DisplayName("sync never lowers a counter")
    void syncNeverGoesBackwards() {
        IDGenerator.nextPetId();
        IDGenerator.nextPetId();
        IDGenerator.nextPetId();          // counter is now 3
        IDGenerator.sync(IDGenerator.PET_PREFIX, "P002");
        assertEquals("P004", IDGenerator.nextPetId());
    }

    @Test
    @DisplayName("sync copes with ids it cannot parse")
    void unparsableIdsAreIgnored() {
        IDGenerator.sync(IDGenerator.PET_PREFIX, "P");
        // The cast is needed: sync(String, String) and sync(String, Collection) both
        // accept null, so a bare null would not compile.
        IDGenerator.sync(IDGenerator.PET_PREFIX, (String) null);
        IDGenerator.sync(null, "P009");
        assertEquals("P001", IDGenerator.nextPetId());
    }

    @Test
    @DisplayName("a prefix that also starts another prefix is handled correctly")
    void veAndVacDoNotCollide() {
        // "VAC004" also starts with "V", so the numeric part must be read after the
        // prefix that was actually asked for.
        IDGenerator.sync(IDGenerator.VACCINATION_PREFIX, "VAC004");
        assertEquals("VAC005", IDGenerator.nextVaccinationId());
        assertEquals("V001", IDGenerator.nextVeterinarianId());
    }

    @Test
    @DisplayName("sync re-seeds from a whole collection of loaded ids")
    void syncFromACollection() {
        IDGenerator.sync(IDGenerator.OWNER_PREFIX, List.of("O001", "O005", "O003"));
        assertEquals("O006", IDGenerator.nextOwnerId());
    }

    @Test
    @DisplayName("sync tolerates a null collection")
    void syncFromNull() {
        IDGenerator.sync(IDGenerator.OWNER_PREFIX, (java.util.Collection<String>) null);
        assertEquals("O001", IDGenerator.nextOwnerId());
    }

    @Test
    @DisplayName("reset restarts every counter")
    void resetClearsEverything() {
        IDGenerator.nextPetId();
        IDGenerator.nextVaccinationId();
        IDGenerator.reset();
        assertEquals("P001", IDGenerator.nextPetId());
        assertEquals("VAC001", IDGenerator.nextVaccinationId());
    }

    @Test
    @DisplayName("two freshly generated ids are never equal")
    void idsAreUnique() {
        assertNotEquals(IDGenerator.nextTreatmentId(), IDGenerator.nextTreatmentId());
    }
}
