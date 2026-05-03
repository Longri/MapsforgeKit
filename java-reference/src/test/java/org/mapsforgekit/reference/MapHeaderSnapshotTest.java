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
import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.Tag;
import org.mapsforge.map.reader.MapFile;
import org.mapsforge.map.reader.header.MapFileInfo;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Reads the full header of {@code bremen.map} via Mapsforge's
 * {@link MapFileInfo} and:
 *
 * <ol>
 *   <li>Writes a complete, human-readable dump of every header field to
 *       {@code build/fixtures/bremen-header.txt}. Useful for inspection,
 *       sharing in issues, and as a Swift-port reference.
 *   <li>Compares a curated subset of <b>structural</b> fields (those that
 *       describe the format and bounds, not the build timestamp or file
 *       size) against a committed snapshot in
 *       {@code src/test/resources/expected/bremen-header.txt}. If the
 *       snapshot is missing it is created on the first run; if it differs
 *       the test fails with a diff.
 * </ol>
 *
 * <p>Why split full dump and selective snapshot?
 * <br>The full dump always changes between map builds (creation date, file
 * size, OSM data timestamp). Asserting on it would produce false alarms
 * every time you fetch a fresh bremen.map. The selective snapshot focuses
 * on fields whose change really would be relevant (bounding box, tile size,
 * projection, zoom intervals, etc.) — the kind of change that should pause
 * the developer and trigger a conscious decision.
 */
class MapHeaderSnapshotTest {

    private static Path mapFilePath;

    private static final Path FULL_DUMP_PATH =
            Paths.get("build", "fixtures", "bremen-header.txt");

    private static final Path STRUCTURAL_SNAPSHOT_PATH =
            Paths.get("../", "resources", "expected-bremen-header.txt");

    @BeforeAll
    static void locateMapFile() {
        mapFilePath = findMapFile("resources/bremen.map");
        assumeTrue(mapFilePath != null,
                "resources/bremen.map not found — see java-reference/README.md.");
    }

    @Test
    @DisplayName("Reads bremen.map header, writes a full dump and verifies structural fields")
    void readsAndSnapshotsBremenMapHeader() throws Exception {
        File file = mapFilePath.toFile();
        MapFile mapFile = new MapFile(file);
        try {
            MapFileInfo info = mapFile.getMapFileInfo();
            assertNotNull(info, "MapFileInfo must not be null");

            // 1. Always write the full, human-readable dump.
            String fullDump = formatFullHeader(info);
            Files.createDirectories(FULL_DUMP_PATH.getParent());
            Files.writeString(FULL_DUMP_PATH, fullDump, StandardCharsets.UTF_8);
            System.out.println("Wrote full header dump: " + FULL_DUMP_PATH.toAbsolutePath());

            // 2. Compare the structural subset against the committed snapshot.
            String currentStructural = formatStructuralHeader(info);


            if (!Files.exists(STRUCTURAL_SNAPSHOT_PATH)) {
                Files.createDirectories(STRUCTURAL_SNAPSHOT_PATH.getParent());
                Files.writeString(STRUCTURAL_SNAPSHOT_PATH, currentStructural, StandardCharsets.UTF_8);
                fail("No header snapshot existed yet — wrote one to "
                        + STRUCTURAL_SNAPSHOT_PATH.toAbsolutePath()
                        + ". Review it and commit it. Re-run the test; it should pass.");
            }

            String expected = Files.readString(STRUCTURAL_SNAPSHOT_PATH, StandardCharsets.UTF_8);

            assertEquals(expected, fullDump,"""
                        bremen.map full header has changed.

                        This usually means the reference map file was rebuilt with a different
                        bounding box, tile size, projection, or zoom interval configuration —
                        all things that would invalidate previously generated tile fixtures.

                        Action:
                          1. Review the diff below and decide whether the change is intentional.
                          2. If yes, replace the snapshot at:
                                %s
                             and re-generate any dependent tile fixtures.

                        Diff (- expected / + actual):
                        %s
                        """);

//            if (!expected.equals(fullDump)) {
//                fail("""
//                        bremen.map full header has changed.
//
//                        This usually means the reference map file was rebuilt with a different
//                        bounding box, tile size, projection, or zoom interval configuration —
//                        all things that would invalidate previously generated tile fixtures.
//
//                        Action:
//                          1. Review the diff below and decide whether the change is intentional.
//                          2. If yes, replace the snapshot at:
//                                %s
//                             and re-generate any dependent tile fixtures.
//
//                        Diff (- expected / + actual):
//                        %s
//                        """.formatted(STRUCTURAL_SNAPSHOT_PATH, simpleDiff(expected, currentStructural)));
//            }
        } finally {
            mapFile.close();
        }
    }

    // ---------------------------------------------------------------------
    // formatting
    // ---------------------------------------------------------------------

    /**
     * Full, human-readable dump including volatile fields. Written to
     * build/fixtures/ on every test run for inspection.
     */
    private static String formatFullHeader(MapFileInfo info) {
        StringBuilder sb = new StringBuilder();
        sb.append("# Full Mapsforge header dump for bremen.map\n");
        sb.append("# Generated by MapHeaderSnapshotTest. Not normative — see\n");
        sb.append("# expected/bremen-header.txt for the structural snapshot.\n");
        sb.append('\n');

        sb.append("File format\n");
        sb.append("  fileVersion              = ").append(info.fileVersion).append('\n');
        sb.append("  fileSize                 = ").append(info.fileSize).append('\n');
        sb.append("  projectionName           = ").append(info.projectionName).append('\n');
        sb.append("  tilePixelSize            = ").append(info.tilePixelSize).append('\n');
        sb.append('\n');

        sb.append("Build metadata\n");
        sb.append("  mapDate                  = ")
                .append(info.mapDate)
                .append("  (")
                .append(new Date(info.mapDate))
                .append(")\n");
        sb.append("  createdBy                = ").append(info.createdBy).append('\n');
        sb.append("  comment                  = ").append(info.comment).append('\n');
        sb.append('\n');

        sb.append("Geographic coverage\n");
        sb.append("  boundingBox              = ").append(formatBBox(info.boundingBox)).append('\n');
        sb.append("  startPosition            = ").append(formatLatLong(info.startPosition)).append('\n');
        sb.append("  startZoomLevel           = ").append(info.startZoomLevel).append('\n');
        sb.append('\n');

        sb.append("Zoom levels\n");
        sb.append("  zoomLevelMin             = ").append(info.zoomLevelMin).append('\n');
        sb.append("  zoomLevelMax             = ").append(info.zoomLevelMax).append('\n');
        sb.append('\n');

        sb.append("Languages and POI/Way tags\n");
        sb.append("  languagesPreference      = ").append(info.languagesPreference).append('\n');
        sb.append("  numberOfPoiTags          = ").append(countTags(info.poiTags)).append('\n');
        sb.append("  numberOfWayTags          = ").append(countTags(info.wayTags)).append('\n');
        sb.append('\n');

        sb.append("POI tags (first 300)\n");
        appendTagSample(sb, info.poiTags, 300);
        sb.append('\n');

        sb.append("Way tags (first 300)\n");
        appendTagSample(sb, info.wayTags, 300);

        return sb.toString();
    }

    /**
     * Selective dump of fields whose change is structurally significant.
     * Volatile fields like file size, build date and OSM-import timestamp
     * are intentionally excluded.
     */
    private static String formatStructuralHeader(MapFileInfo info) {
        StringBuilder sb = new StringBuilder();
        sb.append("# Structural header snapshot for bremen.map.\n");
        sb.append("# Excludes volatile fields (mapDate, fileSize, comment, createdBy).\n");
        sb.append("# Regenerate by deleting this file and re-running MapHeaderSnapshotTest.\n");
        sb.append('\n');

        sb.append("fileVersion         = ").append(info.fileVersion).append('\n');
        sb.append("projectionName      = ").append(info.projectionName).append('\n');
        sb.append("tilePixelSize       = ").append(info.tilePixelSize).append('\n');
        sb.append("boundingBox         = ").append(formatBBox(info.boundingBox)).append('\n');
        sb.append("startPosition       = ").append(formatLatLong(info.startPosition)).append('\n');
        sb.append("startZoomLevel      = ").append(info.startZoomLevel).append('\n');
        sb.append("zoomLevelMin        = ").append(info.zoomLevelMin).append('\n');
        sb.append("zoomLevelMax        = ").append(info.zoomLevelMax).append('\n');
        sb.append("languagesPreference = ").append(info.languagesPreference).append('\n');
        sb.append("numberOfPoiTags     = ").append(countTags(info.poiTags)).append('\n');
        sb.append("numberOfWayTags     = ").append(countTags(info.wayTags)).append('\n');

        return sb.toString();
    }

    // ---------------------------------------------------------------------
    // helpers
    // ---------------------------------------------------------------------

    private static String formatBBox(BoundingBox bbox) {
        if (bbox == null) return "null";
        return String.format("minLat=%.6f minLon=%.6f maxLat=%.6f maxLon=%.6f",
                bbox.minLatitude, bbox.minLongitude, bbox.maxLatitude, bbox.maxLongitude);
    }

    private static String formatLatLong(LatLong p) {
        if (p == null) return "null";
        return String.format("lat=%.6f lon=%.6f", p.latitude, p.longitude);
    }

    private static int countTags(Tag[] tags) {
        return tags == null ? 0 : tags.length;
    }

    private static void appendTagSample(StringBuilder sb, Tag[] tags, int max) {
        if (tags == null) {
            sb.append("  (none)\n");
            return;
        }
        int limit = Math.min(tags.length, max);
        for (int i = 0; i < limit; i++) {
            sb.append(String.format("  [%3d] %s%n", i, tags[i]));
        }
        if (tags.length > limit) {
            sb.append("  ... (").append(tags.length - limit).append(" more)\n");
        }
    }

    private static String simpleDiff(String expected, String actual) {
        String[] e = expected.split("\n", -1);
        String[] a = actual.split("\n", -1);
        int max = Math.max(e.length, a.length);
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < max; i++) {
            String el = i < e.length ? e[i] : null;
            String al = i < a.length ? a[i] : null;
            if (el == null) {
                out.append("+ ").append(al).append('\n');
            } else if (al == null) {
                out.append("- ").append(el).append('\n');
            } else if (!el.equals(al)) {
                out.append("- ").append(el).append('\n');
                out.append("+ ").append(al).append('\n');
            }
        }
        return out.toString();
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