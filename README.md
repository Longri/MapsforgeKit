# MapsforgeKit

A native Swift and Metal implementation of [Mapsforge](https://github.com/mapsforge/mapsforge) for Apple platforms (macOS and iOS).

The goal is to read and render the original Mapsforge `.map` binary format and rendertheme XMLs natively on macOS and iOS, sharing a single codebase between both platforms.

See [ROADMAP.md](ROADMAP.md) for the planned phases and how to contribute.

## Repository Layout

```
.
├── apple/              Xcode project (Swift, Metal) for macOS and iOS
├── java-reference/     Java/Gradle project providing reference outputs for tests
├── README.md
├── ROADMAP.md
└── LICENSE
```

### `apple/`

The Xcode project containing the Swift implementation. Open `apple/MapsforgeKit.xcodeproj` in Xcode.

### `java-reference/`

A Gradle-based Java project used as a reference implementation. It produces deterministic outputs (e.g. JSON dumps of parsed tiles) that the Swift unit tests compare against, ensuring binary compatibility with the original Mapsforge format.

This is a developer-only tool; it is not a runtime dependency of the Swift code.

Open `java-reference/` in IntelliJ IDEA. Build with:

```bash
cd java-reference
./gradlew build
```

## Status

Early development. See [ROADMAP.md](ROADMAP.md) for current progress.

## License

TBD. See [LICENSE](LICENSE).
