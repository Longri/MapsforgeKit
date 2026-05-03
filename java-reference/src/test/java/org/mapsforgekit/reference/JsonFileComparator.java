/*
 * MapsforgeKit — Native Mapsforge implementation for Apple platforms
 * Copyright (C) 2026 MapsforgeKit contributors
 *
 * Licensed under the GNU Lesser General Public License v3.0.
 */

package org.mapsforgekit.reference;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flipkart.zjsonpatch.JsonDiff;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Compares two large JSON files efficiently.
 *
 * <p>Strategy:
 * <ol>
 *   <li>SHA-256 each file. If hashes match, we're done — files are byte-identical.
 *   <li>Otherwise parse both with Jackson and compute a structural diff
 *       using zjsonpatch (RFC 6902 JSON Patch).
 *   <li>Write the full diff to {@code reportPath} for inspection.
 *   <li>Return a {@link Result} containing operation count and a short summary
 *       of the first few operations for use in the test failure message.
 * </ol>
 */
public final class JsonFileComparator {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private JsonFileComparator() { }

    public static Result compare(Path expected, Path actual, Path reportPath) throws IOException {
        // 1. Hash check — fast path for identical files.
        String expectedHash = sha256(expected);
        String actualHash = sha256(actual);
        if (expectedHash.equals(actualHash)) {
            return new Result(true, 0, expectedHash, "");
        }

        // 2. Structural diff.
        JsonNode expectedTree = MAPPER.readTree(expected.toFile());
        JsonNode actualTree = MAPPER.readTree(actual.toFile());
        JsonNode patch = JsonDiff.asJson(expectedTree, actualTree);

        // 3. Write full diff to disk.
        if (reportPath != null) {
            Files.createDirectories(reportPath.getParent());
            MAPPER.writerWithDefaultPrettyPrinter().writeValue(reportPath.toFile(), patch);
        }

        // 4. Build short summary for the test message — first N operations.
        int opCount = patch.size();
        StringBuilder summary = new StringBuilder();
        int previewLimit = Math.min(opCount, 10);
        for (int i = 0; i < previewLimit; i++) {
            JsonNode op = patch.get(i);
            summary.append("  [").append(i).append("] ")
                    .append(op.get("op").asText())
                    .append(" at ")
                    .append(op.get("path").asText());
            JsonNode value = op.get("value");
            if (value != null) {
                String s = value.toString();
                if (s.length() > 120) s = s.substring(0, 117) + "...";
                summary.append(" → ").append(s);
            }
            summary.append('\n');
        }
        if (opCount > previewLimit) {
            summary.append("  ... and ").append(opCount - previewLimit)
                    .append(" more operations (see full diff in report).\n");
        }

        return new Result(false, opCount, null, summary.toString());
    }

    private static String sha256(Path file) throws IOException {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            try (var in = Files.newInputStream(file)) {
                byte[] buf = new byte[64 * 1024];
                int n;
                while ((n = in.read(buf)) > 0) {
                    md.update(buf, 0, n);
                }
            }
            return HexFormat.of().formatHex(md.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    public record Result(boolean equal, int diffOperationCount, String sharedHash, String summary) {
        public String describe() {
            if (equal) {
                return "JSON files are byte-identical (sha256=" + sharedHash + ")";
            }
            return "JSON files differ in " + diffOperationCount + " operation(s):\n" + summary;
        }
    }
}