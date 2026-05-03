# MapsforgeKit

A native Swift and Metal implementation of [Mapsforge](https://github.com/mapsforge/vtm) for Apple platforms (macOS and iOS).

The goal is to read and render the original Mapsforge `.map` binary format and rendertheme XMLs natively on macOS and iOS, sharing a single codebase between both platforms.

See [ROADMAP.md](ROADMAP.md) for the planned phases and how to contribute.

# AUTHORS

MapsforgeKit is developed and maintained by:

- [Longri](https://github.com/Longri) — initial author and maintainer

Contributors (in order of first contribution):

- (your name could be here!)

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

MapsforgeKit is licensed under the **GNU Lesser General Public License v3.0** (LGPL-3.0).
See [LICENSE](LICENSE) for the full text and [COPYING](COPYING) for the GPL v3.0
that it references.

In short:

- You can use MapsforgeKit in your own applications, including commercial and
  closed-source ones, by linking to it as a library.
- If you modify MapsforgeKit itself, your modifications must also be released
  under LGPL-3.0.
- This is the same license used by the original
  [Mapsforge](https://github.com/mapsforge/mapsforge) project, which makes it
  straightforward to port code or interoperate between the two.

This is an independent project and is not officially affiliated with the
Mapsforge project. Mapsforge is a registered concept developed by its
respective authors; this port aims to honor its design and remain
binary-compatible with its `.map` file format and rendertheme XML format.