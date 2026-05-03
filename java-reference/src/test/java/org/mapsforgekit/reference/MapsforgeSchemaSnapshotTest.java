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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.Tag;
import org.mapsforge.core.model.Tile;
import org.mapsforge.map.datastore.MapReadResult;
import org.mapsforge.map.datastore.PointOfInterest;
import org.mapsforge.map.datastore.Way;
import org.mapsforge.map.reader.header.MapFileInfo;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Early-warning test that fails when the public field structure of Mapsforge's
 * data model changes. Whenever Mapsforge adds, renames, or removes a field on
 * one of the watched classes, this test fails with a diff that tells the
 * developer exactly what changed — so the change can be evaluated and, if
 * relevant, mirrored in the Swift port.
 *
 * <p>Strategy:
 * <ol>
 *   <li>For each watched class, collect all non-static, non-synthetic fields.
 *   <li>Format them as {@code modifier type name} lines, sorted by name.
 *   <li>Compare the result with a committed reference snapshot in
 *       {@code src/test/resources/expected/mapsforge-schema.txt}.
 *   <li>If the snapshot file does not exist yet, write it (first run).
 *   <li>If it exists and differs, fail the test and print a unified-style diff.
 * </ol>
 *
 * <p>To accept a Mapsforge change after reviewing it, delete the snapshot file
 * and re-run the test. The new snapshot will be regenerated.
 */
class MapsforgeSchemaSnapshotTest {

    /**
     * Mapsforge classes whose structure we want to monitor.
     * Add classes here as the project grows (e.g. theme-related classes
     * once Phase 2 starts).
     */
    private static final Class<?>[] WATCHED_CLASSES = new Class<?>[] {
            // Core geometry / model
            BoundingBox.class,
            LatLong.class,
            Tag.class,
            Tile.class,
            // Tile reading result
            MapReadResult.class,
            PointOfInterest.class,
            Way.class,
            // File metadata
            MapFileInfo.class,
    };

    private static final Path SNAPSHOT_PATH =
            Paths.get("../", "resources", "mapsforge-schema.txt");

    @Test
    @DisplayName("Mapsforge public field schema matches committed snapshot")
    void mapsforgeSchemaIsUnchanged() throws Exception {
        String currentSchema = buildSchema(WATCHED_CLASSES);

        if (!Files.exists(SNAPSHOT_PATH)) {
            Files.createDirectories(SNAPSHOT_PATH.getParent());
            Files.writeString(SNAPSHOT_PATH, currentSchema, StandardCharsets.UTF_8);
            fail("No schema snapshot existed yet — wrote one to "
                    + SNAPSHOT_PATH.toAbsolutePath()
                    + ". Review it and commit it. Re-run the test; it should now pass.");
        }

        String expectedSchema = Files.readString(SNAPSHOT_PATH, StandardCharsets.UTF_8);

        if (!expectedSchema.equals(currentSchema)) {
            String diff = simpleDiff(expectedSchema, currentSchema);
            fail("""
                    Mapsforge schema has changed compared to the committed snapshot.

                    This usually means a Mapsforge update added, renamed, or removed a
                    field on one of the watched classes. Please:

                      1. Review the diff below.
                      2. Decide whether the change is relevant for the Swift port.
                      3. Update TileExtractor / TileDump as needed.
                      4. Replace the snapshot at:
                           %s
                         with the current output and commit the new version.

                    Diff (- expected / + actual):
                    %s
                    """.formatted(SNAPSHOT_PATH, diff));
        }

        // Also verify the snapshot covers every watched class — sanity guard
        // against a future mistake of removing a class from WATCHED_CLASSES.
        for (Class<?> c : WATCHED_CLASSES) {
            assertEquals(true, currentSchema.contains("class " + c.getName()),
                    "Schema must include " + c.getName());
        }
    }

    // ---------------------------------------------------------------------
    // schema extraction
    // ---------------------------------------------------------------------

    private static String buildSchema(Class<?>[] classes) {
        StringBuilder sb = new StringBuilder();
        sb.append("# Mapsforge public-field schema snapshot.\n");
        sb.append("# Generated automatically by MapsforgeSchemaSnapshotTest.\n");
        sb.append("# Do not edit by hand — regenerate by deleting and re-running the test.\n");
        sb.append("\n");

        Class<?>[] sorted = classes.clone();
        Arrays.sort(sorted, Comparator.comparing(Class::getName));

        for (Class<?> c : sorted) {
            sb.append("class ").append(c.getName()).append("\n");

            List<Field> fields = new ArrayList<>();
            for (Field f : c.getDeclaredFields()) {
                if (f.isSynthetic()) continue;
                if (Modifier.isStatic(f.getModifiers())) continue;
                fields.add(f);
            }
            fields.sort(Comparator.comparing(Field::getName));

            for (Field f : fields) {
                sb.append("    ")
                        .append(Modifier.toString(f.getModifiers()))
                        .append(' ')
                        .append(typeToString(f.getGenericType()))
                        .append(' ')
                        .append(f.getName())
                        .append('\n');
            }
            sb.append('\n');
        }
        return sb.toString();
    }

    private static String typeToString(java.lang.reflect.Type t) {
        // Use getTypeName() — gives stable output like "java.util.List<org.mapsforge.core.model.Tag>"
        return t.getTypeName();
    }

    /**
     * Very small, human-readable diff for the failure message.
     * Not a full unified-diff implementation, but enough to spot the change quickly.
     */
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
}
