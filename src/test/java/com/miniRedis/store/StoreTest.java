package com.miniRedis.store;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class StoreTest {

    private Store store;

    @BeforeEach
    void setUp() {
        store = new Store();
    }

    @Test
    void testSetAndGet() {
        store.set("key1", "val1", null);
        assertEquals("val1", store.get("key1"));
        assertTrue(store.exists("key1"));
    }

    @Test
    void testDelete() {
        store.set("key1", "val1", null);
        assertTrue(store.delete("key1"));
        assertNull(store.get("key1"));
        assertFalse(store.exists("key1"));
    }

    @Test
    void testIncr() {
        assertEquals(1, store.incr("counter"));
        assertEquals(2, store.incr("counter"));
        assertEquals(3, store.incr("counter"));
        assertEquals("3", store.get("counter"));
    }

    @Test
    void testExpiryAndTtl() throws InterruptedException {
        // Expire in 1 second
        store.set("temp", "value", System.currentTimeMillis() + 1000);
        assertTrue(store.exists("temp"));
        assertTrue(store.ttl("temp") >= 0);

        // Wait for key to expire
        Thread.sleep(1100);

        // Lazy expiry should trigger
        assertNull(store.get("temp"));
        assertFalse(store.exists("temp"));
        assertEquals(-2, store.ttl("temp"));
    }

    @Test
    void testExplicitExpireCommand() {
        store.set("session", "abc", null);
        assertEquals(-1, store.ttl("session"));

        boolean success = store.expire("session", 60);
        assertTrue(success);
        assertTrue(store.ttl("session") > 0);
    }
}
