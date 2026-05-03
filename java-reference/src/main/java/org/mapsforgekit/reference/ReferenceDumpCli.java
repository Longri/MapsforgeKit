package org.mapsforgekit.reference;

/**
 * CLI entry point that reads a tile from a Mapsforge .map file using the
 * original Java implementation and dumps it as JSON.
 *
 * The Swift test suite consumes these JSON files as reference fixtures
 * to verify that the Swift parser produces equivalent output.
 *
 * Usage:
 *   ./gradlew run --args="path/to/file.map zoom x y output.json"
 *
 * This is a stub — implementation will be added as Phase 1 progresses.
 */
public final class ReferenceDumpCli {

    private ReferenceDumpCli() {
        // utility
    }

    public static void main(String[] args) {
        if (args.length < 4) {
            System.err.println("Usage: ReferenceDumpCli <map-file> <zoom> <x> <y> [output.json]");
            System.exit(1);
        }
        // TODO: implement using org.mapsforge.map.reader.MapFile
        System.out.println("ReferenceDumpCli — not yet implemented.");
    }
}
