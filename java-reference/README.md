# Java Reference

This directory contains a Java implementation that uses the **original Mapsforge library** to produce reference outputs (JSON dumps of parsed tiles, theme matching results, etc.).

These outputs serve as **test fixtures** for the Swift unit tests. The Swift parser is considered correct when its output matches the Java reference for the same input.

## Why?

Reimplementing the Mapsforge `.map` binary format is detail-heavy. Rather than testing the Swift code against hand-crafted expectations, we generate expected outputs directly from the canonical Java implementation. This gives us strong confidence that the Swift port stays binary-compatible with real-world Mapsforge data.

## Tech Stack

- Java 17
- Gradle (Kotlin DSL)
- JUnit 5
- Original Mapsforge libraries from Maven Central
- Jackson for JSON output

## Usage

Open the directory in IntelliJ IDEA (`File → Open` → select the `java-reference` folder).

### Build

```bash
./gradlew build
```

### Run tests

```bash
./gradlew test
```

### Generate a reference dump

```bash
./gradlew run --args="path/to/file.map 14 8800 5380 fixture.json"
```

The resulting `fixture.json` is then committed to `apple/MapsforgeKit/Tests/Fixtures/` (or wherever the Swift tests live) and consumed by the Swift test suite.

## Adding the Gradle Wrapper

After cloning the repo for the first time, generate the Gradle wrapper once:

```bash
gradle wrapper --gradle-version 8.10
```

Then commit `gradlew`, `gradlew.bat`, and `gradle/wrapper/`. From that point on, contributors only need a JDK installed — no separate Gradle installation.

## Project Layout

```
java-reference/
├── build.gradle.kts
├── settings.gradle.kts
├── src/
│   ├── main/java/org/mapsforgekit/reference/   ← CLI + helpers
│   └── test/
│       ├── java/org/mapsforgekit/reference/    ← unit tests
│       └── resources/                           ← small .map test files
└── README.md
```
