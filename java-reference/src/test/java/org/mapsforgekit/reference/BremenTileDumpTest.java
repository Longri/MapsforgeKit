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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapsforge.map.reader.MapFile;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Reads tiles from {@code bremen.map} and writes a JSON fixture that the
 * Swift port can use as a reference for cross-language test comparisons.
 *
 * <p>The {@code .map} file is expected at the repository root (one directory
 * above {@code java-reference/}). It is intentionally not committed to git —
 * Mapsforge map files are large and licensing of OSM-derived data is best
 * handled by downloading on demand. See {@code java-reference/README.md} for
 * details on how to obtain a copy.
 *
 * <p>The generated fixture is written to
 * {@code java-reference/build/fixtures/bremen-tiles.json} so it can be picked
 * up by both manual inspection and the Swift test target.
 */
class BremenTileDumpTest {

    /** Tile size used by Mapsforge — fixed at 256 by the format spec. */
    private static final int TILE_SIZE = 256;

    /** Located by walking up from the working directory until {@code bremen.map} is found. */
    private static Path mapFilePath;

    /** Where the JSON fixture is written. */
    private static final Path FIXTURE_OUT =
            Paths.get("build", "fixtures", "expected-bremen-tiles.json");

    /**
     * Reference tiles that all sit over Bremen, Germany.
     *
     * <p>Coordinates were computed with the standard Slippy Map / Web Mercator
     * formulas for Bremen city centre (≈ 53.0793° N, 8.8017° E). Picked to
     * cover overview, city and detail zoom levels and to include neighbouring
     * tiles where it is interesting (zoom 14).
     */
    private static final ReferenceTile[] REFERENCE_TILES = new ReferenceTile[] {
            new ReferenceTile(12, 2148, 1332, "Bremen overview"),
            new ReferenceTile(14, 8592, 5331, "Bremen city centre"),
            new ReferenceTile(14, 8593, 5331, "Bremen city, east tile"),
            new ReferenceTile(16, 34370, 21324, "Bremen inner city detail"),
    };

    @BeforeAll
    static void locateMapFile() {
        mapFilePath = findMapFile("resources/bremen.map");
        assertNotNull(mapFilePath, "resources/bremen.map not found. Place the file in the repository root "
                + "(one level above java-reference/). See java-reference/README.md.");
    }

    @Test
    @DisplayName("Reads bremen.map and writes a JSON fixture covering several zoom levels")
    void writesJsonFixtureForBremenTiles() throws Exception {
        File file = mapFilePath.toFile();
        assertTrue(file.canRead(), "bremen.map must be readable: " + file.getAbsolutePath());

        MapFile mapFile = new MapFile(file);
        try {
            assertNotNull(mapFile.getMapFileInfo(), "MapFileInfo must not be null");

            TileExtractor extractor = new TileExtractor(mapFile);

            TileDump.Document doc = new TileDump.Document();
            doc.generatedBy = "MapsforgeKit/java-reference BremenTileDumpTest";
            doc.mapFile = file.getName();
            doc.fileVersion = mapFile.getMapFileInfo().fileVersion;
            doc.tiles = new ArrayList<>();

            for (ReferenceTile ref : REFERENCE_TILES) {
                TileDump.TileEntry entry = extractor.extract(
                        ref.zoom, ref.x, ref.y, TILE_SIZE);
                doc.tiles.add(entry);
                System.out.printf(
                        "  z=%2d x=%-6d y=%-6d  pois=%-4d ways=%-5d  (%s)%n",
                        ref.zoom, ref.x, ref.y,
                        entry.poiCount, entry.wayCount, ref.description);
            }

            // Sanity check — at least one of the city-centre tiles must
            // contain data, otherwise something is wrong with the map file
            // or the chosen tile coordinates.
            int totalElements = doc.tiles.stream()
                    .mapToInt(t -> t.poiCount + t.wayCount)
                    .sum();
            assertTrue(totalElements > 0,
                    "Expected at least some POIs or ways across the reference "
                            + "tiles — map file may be empty or coordinates wrong.");

            writeJson(doc, FIXTURE_OUT);
            System.out.println("Wrote fixture: " + FIXTURE_OUT.toAbsolutePath());
            assertTrue(Files.exists(FIXTURE_OUT), "Fixture file should exist after writing");

            Path expectedPath = Paths.get("../", "resources", "expected-bremen-tiles.json");
            Path actualPath = Paths.get("build", "fixtures", "bremen-tiles.json");
            Path reportPath = Paths.get("build", "fixtures", "bremen-tiles.diff.json");

            JsonFileComparator.Result result = JsonFileComparator.compare(expectedPath, actualPath, reportPath);

            if (!result.equal()) {
                fail("""
                bremen-tiles.json does not match the committed reference.
                
                %s
                Full RFC 6902 JSON Patch written to:
                  %s
                
                If the change is intentional, replace the reference at:
                  %s
                with the actual file at:
                  %s
                """.formatted(
                        result.describe(),
                        reportPath.toAbsolutePath(),
                        expectedPath.toAbsolutePath(),
                        actualPath.toAbsolutePath()));
            }




        }
        finally {
            mapFile.close();
        }
    }

    // ---------------------------------------------------------------------
    // helpers
    // ---------------------------------------------------------------------

    private static void writeJson(TileDump.Document doc, Path output) throws Exception {
        Files.createDirectories(output.getParent());
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        // Stable ordering helps when diffing output across runs / languages.
        mapper.enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);
        mapper.writeValue(output.toFile(), doc);
    }

    /**
     * Walks up from the current working directory looking for {@code name}.
     * This makes the test work whether it's run from {@code java-reference/}
     * (Gradle default) or the repo root.
     */
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

    private record ReferenceTile(int zoom, int x, int y, String description) { }
}
