package main.java.networktool.storage;

import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

class HostOwnershipTest {

    HostOwnership ownership = HostOwnership.getInstance();
    final String NET = TestConstants.TEST_PREFIX + "ownership_net";
    final String IP  = TestConstants.IP_1;

    @AfterEach
    void cleanup() {
        ownership.removeOwner(NET, IP, "user3");
        ownership.removeOwner(NET, IP, "user4");
    }

    @Test void addOwner_thenIsOwnedBy_true() {
        ownership.addOwner(NET, IP, "user3");
        assertTrue(ownership.isOwnedBy(NET, IP, "user3"));
    }

    @Test void isOwnedBy_falseForOtherUser() {
        ownership.addOwner(NET, IP, "user3");
        assertFalse(ownership.isOwnedBy(NET, IP, "user4"));
    }

    @Test void twoOwners_bothTracked() {
        ownership.addOwner(NET, IP, "user3");
        ownership.addOwner(NET, IP, "user4");
        assertEquals(2, ownership.getOwners(NET, IP).size());
    }

    @Test void removeOwner_singleOwner_returnsOrphaned() {
        ownership.addOwner(NET, IP, "user3");
        assertTrue(ownership.removeOwner(NET, IP, "user3"));
    }

    @Test void removeOwner_remainingOwner_notOrphaned() {
        ownership.addOwner(NET, IP, "user3");
        ownership.addOwner(NET, IP, "user4");
        assertFalse(ownership.removeOwner(NET, IP, "user3"));
        assertTrue(ownership.isOwnedBy(NET, IP, "user4"));
    }

    @Test void removeOwner_unknownKey_returnsTrue() {
        assertTrue(ownership.removeOwner(NET, "99.99.99.99", "ghost"));
    }

    @Test void getOwners_unknown_empty() {
        assertTrue(ownership.getOwners(NET, "1.2.3.4").isEmpty());
    }

    @Test void addOwner_nullArgs_doesNotThrow() {
        assertDoesNotThrow(() -> ownership.addOwner(null, null, null));
    }

    @Test void addOwner_blankUsername_ignored() {
        ownership.addOwner(NET, IP, "  ");
        assertTrue(ownership.getOwners(NET, IP).isEmpty());
    }

    @Test void getInstance_isSingleton() {
        assertSame(HostOwnership.getInstance(), HostOwnership.getInstance());
    }

    @Test void removeAllForNetwork_clearsEntries() {
        ownership.addOwner(NET, IP, "user3");
        ownership.removeAllForNetwork(NET);
        assertTrue(ownership.getOwners(NET, IP).isEmpty());
    }
}