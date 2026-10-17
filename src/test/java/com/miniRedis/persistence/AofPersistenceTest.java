package com.miniRedis.persistence;

import java.io.File;
import java.io.IOException;
import java.util.List;

import com.miniRedis.command.CommandDispatcher;
import com.miniRedis.store.Store;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class AofPersistenceTest {

    private final String testAofPath = "test_appendonly.aof";
    private File testFile;

    @BeforeEach
    void setUp() {
        testFile = new File(testAofPath);
        if (testFile.exists()) {
            testFile.delete();
        }
    }

    @AfterEach
    void tearDown() {
        if (testFile.exists()) {
            testFile.delete();
        }
    }

    @Test
    void testAofWriteAndReplay() throws IOException {
        // Step 1: Write commands using AofWriter
        AofWriter writer = new AofWriter(testAofPath);
        writer.append(List.of("SET", "user:id", "42"));
        writer.append(List.of("INCR", "page_views"));
        writer.append(List.of("INCR", "page_views"));
        writer.append(List.of("EXPIRE", "user:id", "3600"));

        assertTrue(testFile.exists(), "AOF file must be created on disk");
        assertTrue(testFile.length() > 0, "AOF file must contain written bytes");

        // Step 2: Replay into a completely fresh Store
        Store freshStore = new Store();
        assertNull(freshStore.get("user:id"), "Fresh store starts empty");

        CommandDispatcher dispatcher = new CommandDispatcher();
        AofReplayer replayer = new AofReplayer(testAofPath, dispatcher);
        replayer.replay(freshStore);

        // Step 3: Verify restored state
        assertEquals("42", freshStore.get("user:id"));
        assertEquals("2", freshStore.get("page_views"));
        assertTrue(freshStore.ttl("user:id") > 0, "TTL should be active");
    }
}
