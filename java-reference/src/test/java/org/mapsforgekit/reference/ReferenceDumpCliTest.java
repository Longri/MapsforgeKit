package org.mapsforgekit.reference;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Smoke test to verify the project is wired up correctly.
 * Real reference tests will be added during Phase 1.
 */
class ReferenceDumpCliTest {

    @Test
    void cliClassExists() {
        assertNotNull(ReferenceDumpCli.class);
    }
}
