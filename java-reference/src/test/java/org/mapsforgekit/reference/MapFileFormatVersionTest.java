/*
 * MapsforgeKit — Native Mapsforge implementation for Apple platforms
 * Copyright (C) 2026 MapsforgeKit contributors
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 */

package org.mapsforgekit.reference;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapsforge.map.reader.MapFile;
import org.mapsforge.map.reader.header.MapFileInfo;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Pins the Mapsforge binary file format version we currently support.
 *
 * <p>The {@code .map} file format has a numeric version embedded in its
 * header (currently {@code 5}). When the upstream format evolves to v6 or
 * beyond, this test fails immediately, prompting the developer to:
 * <ol>
 *   <li>Read the Mapsforge release notes to learn what changed.
 *   <li>Decide whether to support the new version in the Swift port.
 *   <li>Update the constant here once the new version has been reviewed.
 * </ol>
 *
 * <p>This guards against the silent case where Mapsforge updates a map writer
 * to produce a new format version, our test maps are regenerated, and the
 * Swift port quietly stops working because it never learned the new layout.
 */
class MapFileFormatVersionTest {

    /**
     * Currently expected binary format version of {@code .map} files.
     * Update this constant only after consciously reviewing the new format.
     */
    private static final int EXPECTED_FILE_VERSION = 5;

    private static Path mapFilePath;

    @BeforeAll
    static void locateMapFile() {
        mapFilePath = findMapFile("resources/bremen.map");
        assumeTrue(mapFilePath != null,
                "resources/bremen.map not found — see java-reference/README.md.");
    }

    @Test
    @DisplayName("bremen.map uses the expected Mapsforge binary format version")
    void mapFileHasExpectedFormatVersion() throws Exception {
        File file = mapFilePath.toFile();
        MapFile mapFile = new MapFile(file);
        try {
            MapFileInfo info = mapFile.getMapFileInfo();
            assertNotNull(info, "MapFileInfo must not be null");
            assertEquals(EXPECTED_FILE_VERSION, info.fileVersion,
                    """
                    The Mapsforge binary file format version has changed.

                    The .map file reports version %d, but this project currently
                    supports version %d. This usually means the upstream
                    mapsforge-map-writer was updated to produce a new format.

                    Action:
                      1. Read the Mapsforge changelog at
                         https://github.com/mapsforge/mapsforge/blob/master/docs/Changelog.md
                      2. Review what changed in the binary layout.
                      3. Update the Swift parser if necessary.
                      4. Bump EXPECTED_FILE_VERSION in MapFileFormatVersionTest.
                    """.formatted(info.fileVersion, EXPECTED_FILE_VERSION));
        } finally {
            mapFile.close();
        }
    }

    private static Path findMapFile(String name) {
        Path current = Paths.get("").toAbsolutePath();
        for (int i = 0; i < 5 && current != null; i++) {
            Path candidate = current.resolve(name);
            if (Files.isRegularFile(candidate)) {
                return candidate;
            }
            current = current.getParent();
        }
        return null;
    }
}
