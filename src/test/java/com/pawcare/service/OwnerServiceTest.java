package com.pawcare.service;

import com.pawcare.exception.EntityNotFoundException;
import com.pawcare.exception.InvalidOwnerException;
import com.pawcare.model.Owner;
import com.pawcare.support.TestData;
import com.pawcare.util.IDGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** CRUD, lookup and searching for the clinic's clients. */
class OwnerServiceTest {

    private OwnerService owners;

    @BeforeEach
    void setUp() {
        IDGenerator.reset();
        owners = new OwnerService();
    }

    @Test
    @DisplayName("a valid owner is stored and found by id")
    void addAndFind() throws InvalidOwnerException {
        owners.addOwner(TestData.owner("O001", "Aarav Sharma"));

        assertEquals(1, owners.count());
        assertEquals("Aarav Sharma", owners.findById("O001").orElseThrow().getName());
        assertTrue(owners.exists("O001"));
    }

    @Test
    @DisplayName("an invalid owner is rejected and never stored")
    void anInvalidOwnerIsNotStored() {
        Owner badPhone = TestData.owner("O001", "Aarav Sharma", "12345", "aarav@example.com");

        assertThrows(InvalidOwnerException.class, () -> owners.addOwner(badPhone));
        assertEquals(0, owners.count());
        assertFalse(owners.exists("O001"));
    }

    @Test
    @DisplayName("a duplicate owner id is refused")
    void duplicateIdsAreRefused() throws InvalidOwnerException {
        owners.addOwner(TestData.owner("O001", "Aarav Sharma"));
        assertThrows(InvalidOwnerException.class,
                () -> owners.addOwner(TestData.owner("O001", "Someone Else")));
        assertEquals(1, owners.count());
    }

    @Test
    @DisplayName("an unknown id gives a friendly name rather than an exception")
    void ownerNameFallsBack() throws InvalidOwnerException {
        owners.addOwner(TestData.owner("O001", "Aarav Sharma"));

        assertEquals("Aarav Sharma", owners.ownerName("O001"));
        assertEquals("Unassigned", owners.ownerName("O999"));
        assertEquals("Unassigned", owners.ownerName(null));
    }

    @Test
    @DisplayName("requireOwner throws for a missing record")
    void requireOwnerThrows() throws InvalidOwnerException, EntityNotFoundException {
        owners.addOwner(TestData.owner("O001", "Aarav Sharma"));

        assertEquals("Aarav Sharma", owners.requireOwner("O001").getName());
        assertThrows(EntityNotFoundException.class, () -> owners.requireOwner("O999"));
    }

    @Test
    @DisplayName("getAllOwners hands out a copy")
    void getAllOwnersIsDefensive() throws InvalidOwnerException {
        owners.addOwner(TestData.owner("O001", "Aarav Sharma"));
        List<Owner> copy = owners.getAllOwners();
        copy.clear();
        assertEquals(1, owners.count());
    }

    @Test
    @DisplayName("updating an owner keeps the record count the same")
    void updateReplacesTheRecord() throws InvalidOwnerException {
        owners.addOwner(TestData.owner("O001", "Aarav Sharma"));

        Owner moved = TestData.owner("O001", "Aarav Sharma");
        moved.setAddress("9 Residency Road, Bengaluru 560025");
        moved.setPhone("9812345678");
        owners.updateOwner(moved);

        assertEquals(1, owners.count());
        assertEquals("9 Residency Road, Bengaluru 560025",
                owners.findById("O001").orElseThrow().getAddress());
    }

    @Test
    @DisplayName("updating an owner who does not exist is an error")
    void updateRejectsAnUnknownId() {
        assertThrows(InvalidOwnerException.class,
                () -> owners.updateOwner(TestData.owner("O404", "Ghost")));
    }

    @Test
    @DisplayName("an invalid update changes nothing")
    void anInvalidUpdateChangesNothing() throws InvalidOwnerException {
        owners.addOwner(TestData.owner("O001", "Aarav Sharma"));

        Owner broken = TestData.owner("O001", "Aarav Sharma");
        broken.setEmail("not-an-email");

        assertThrows(InvalidOwnerException.class, () -> owners.updateOwner(broken));
        assertEquals("ravi.kumar@example.com", owners.findById("O001").orElseThrow().getEmail());
    }

    @Test
    @DisplayName("deleting removes the owner from the index as well as the list")
    void deleteClearsBothStructures() throws InvalidOwnerException {
        owners.addOwner(TestData.owner("O001", "Aarav Sharma"));
        owners.addOwner(TestData.owner("O002", "Diya Patel"));

        assertTrue(owners.deleteOwner("O001"));

        assertEquals(1, owners.count());
        assertFalse(owners.exists("O001"));
        assertEquals("Diya Patel", owners.getAllOwners().get(0).getName());
    }

    @Test
    @DisplayName("deleting something that is not there reports that nothing happened")
    void deleteReportsWhetherItDidAnything() {
        assertFalse(owners.deleteOwner("O999"));
        assertFalse(owners.deleteOwner(null));
    }

    @Test
    @DisplayName("searching matches part of a name and ignores capitals")
    void searchByName() throws InvalidOwnerException {
        owners.addOwner(TestData.owner("O001", "Aarav Sharma"));
        owners.addOwner(TestData.owner("O002", "Diya Patel"));
        owners.addOwner(TestData.owner("O003", "Aarav Menon"));

        assertEquals(2, owners.searchByName("aarav").size());
        assertEquals(1, owners.searchByName("Patel").size());
        assertTrue(owners.searchByName("Nobody").isEmpty());
    }

    @Test
    @DisplayName("an empty search returns everybody rather than nobody")
    void anEmptySearchReturnsEverything() throws InvalidOwnerException {
        owners.addOwner(TestData.owner("O001", "Aarav Sharma"));
        owners.addOwner(TestData.owner("O002", "Diya Patel"));

        assertEquals(2, owners.searchByName("").size());
        assertEquals(2, owners.searchByName("   ").size());
        assertEquals(2, owners.searchByName(null).size());
    }

    @Test
    @DisplayName("owners sort by name ignoring capitals")
    void sortedByName() throws InvalidOwnerException {
        owners.addOwner(TestData.owner("O001", "diya patel"));
        owners.addOwner(TestData.owner("O002", "Aarav Sharma"));
        owners.addOwner(TestData.owner("O003", "Kabir Menon"));

        assertEquals(List.of("Aarav Sharma", "diya patel", "Kabir Menon"),
                owners.sortedByName().stream().map(Owner::getName).toList());
    }

    @Test
    @DisplayName("loadAll replaces the dataset and skips broken records")
    void loadAllReplacesEverything() throws InvalidOwnerException {
        owners.addOwner(TestData.owner("O001", "Aarav Sharma"));

        Owner noId = TestData.owner("O002", "No Id");
        noId.setOwnerId(null);
        owners.loadAll(List.of(TestData.owner("O009", "Loaded Owner"), noId));

        assertEquals(1, owners.count());
        assertFalse(owners.exists("O001"));
        assertTrue(owners.exists("O009"));
    }

    @Test
    @DisplayName("loading seeds the id generator past the highest stored id")
    void loadAllSyncsTheIdGenerator() {
        owners.loadAll(List.of(TestData.owner("O012", "Aarav Sharma")));
        assertEquals("O013", IDGenerator.nextOwnerId());
    }
}
