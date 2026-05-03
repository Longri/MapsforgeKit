# MapsforgeKit – Roadmap

An open-source effort to bring [Mapsforge](https://github.com/mapsforge/mapsforge) to Apple platforms. The goal is a native Swift and Metal implementation that is compatible with the original `.map` file format and Mapsforge rendertheme XMLs, running on both macOS and iOS from a shared codebase.

This document outlines the planned phases of the project. Contributions are welcome – see [Contributing](#contributing) below.

## Goals

- Parse and render the Mapsforge Binary Map File Format (`.map`)
- Support Mapsforge rendertheme XMLs (e.g. OpenAndroMaps, Elevate)
- Native Swift implementation with Metal as the rendering backend
- Run on macOS and iOS from a shared codebase
- Stay close to the original Mapsforge architecture to ease maintenance and interoperability

## Non-Goals

- No support for other map formats (MBTiles, PMTiles, Mapbox Vector Tiles)
- No port of the full mapsforge-map-writer toolchain (creating `.map` files)
- No support for platforms outside the Apple ecosystem

## Tech Stack

- **Language:** Swift
- **Renderer:** Metal (OpenGL is deprecated on Apple platforms)
- **UI integration:** SwiftUI with `MTKView` via `NSViewRepresentable` / `UIViewRepresentable`
- **Build system:** Swift Package Manager
- **Target platforms:** macOS and iOS

## Architecture

The project is split into multiple Swift packages to maximize platform independence and testability:

- **`MapsforgeCore`** – `.map` format parser, rendertheme parser, data model. Platform-independent, no UI dependencies.
- **`MapsforgeRenderer`** – Metal-based renderer. Runs on macOS and iOS.
- **App targets** – integrate the packages and provide platform-specific UI and gesture handling.

This separation allows the core parsing logic to be unit-tested in isolation and reused outside of a rendering context (e.g. for CLI tools or data inspection).

---

## Phases

### Phase 1 – `.map` Format Parser

**Goal:** Open a `.map` file and extract tiles with POIs and ways. No UI, no rendering – purely a library.

**Tasks:**

- Set up the `MapsforgeCore` Swift package
- Implement `ReadBuffer`: VarInt (signed/unsigned, zigzag), bytes, UTF-8 strings, big-endian integers
- Parse `MapFileHeader`: magic bytes, version, bounding box, tile size, projection, flags, tag mappings, zoom intervals
- Sub-file index and tile lookup
- `PoiReader` and `WayReader` (delta encoding, tile-relative coordinates)
- Integration tests comparing output to the Java reference implementation

**Deliverable:** A working library exposing `MapDatabase(fileURL:).readMapData(tile:)`, with unit tests.

---

### Phase 2 – Rendertheme XML Parser

**Goal:** Parse Mapsforge rendertheme XML files into a queryable rule set.

**Tasks:**

- XML parsing using Foundation's `XMLParser`
- Build the rule tree (nested `<rule>` elements with tag matching)
- Model render instructions: `<line>`, `<area>`, `<caption>`, `<symbol>`, `<pathText>`, `<circle>`
- Handle zoom-level filters and tag matching attributes (`k`, `v`, `cat`, `closed`, `e`)
- Load symbol resources (PNG/SVG references)
- Provide a matching API: given a POI/way with tags, return the applicable render instructions

**Deliverable:** Theme parser with unit tests. Input: theme XML and tag list. Output: list of render instructions to apply.

---

### Phase 3 – Metal Foundations

**Goal:** Establish Metal rendering primitives needed by the project. Done in isolation from Mapsforge to keep concerns separate.

**Tasks:**

- Basic Metal setup: command buffers, pipeline states, MSL shaders
- 2D rendering primitives: triangles, textured quads, line shader with width
- Texture atlas handling
- `MTKView` integration with SwiftUI on both macOS and iOS

**Deliverable:** A small standalone Metal sample (e.g. polygon renderer with pan/zoom) that demonstrates the building blocks needed for tile rendering.

---

### Phase 4 – Tile Rendering

**Goal:** Render geometry from `.map` tiles using Metal. Polygons and lines first, no text yet.

**Tasks:**

- Set up the `MapsforgeRenderer` Swift package
- Polygon triangulation (e.g. earcut algorithm)
- Wide-line rendering (custom shader; Metal does not provide thick lines natively)
- Tile cache and viewport management
- Apply parsed render instructions from Phase 2 to the geometry
- Pan and zoom gestures on macOS and iOS

**Deliverable:** A running app that renders a `.map` file with a basic theme (without text labels).

---

### Phase 5 – Text and Labels

**Goal:** Text rendering and intelligent label placement. The most challenging phase.

**Tasks:**

- Text rendering via Core Text and a signed distance field (SDF) atlas
- Caption rendering (point labels with anchors)
- Path text (labels along a line, e.g. street names)
- Label placement algorithm with collision avoidance
- Symbol rendering (icons from the theme)
- Label prioritization: render important labels first, drop less important ones on collision

**Deliverable:** A fully rendered map with labels, comparable to the Android version of Mapsforge.

---

## Status

- [ ] Phase 1 – `.map` Format Parser  <== IN PROGRESS
- [ ] Phase 2 – Rendertheme XML Parser
- [ ] Phase 3 – Metal Foundations
- [ ] Phase 4 – Tile Rendering
- [ ] Phase 5 – Text and Labels

---

## Contributing

Contributions are welcome at any phase. Whether you have experience with Mapsforge internals, Swift, Metal, or just want to help with tooling, documentation, or testing – there is room to participate.

Good entry points:

- **Phase 1 and 2** are pure Swift work and well suited for contributors with backend or systems programming background. No graphics knowledge needed.
- **Phase 3 onwards** involves Metal and graphics programming.
- **Documentation, examples, and test data** (small `.map` files for testing) are valuable at every stage.

Before opening a pull request, please open an issue to discuss the change, especially for larger architectural decisions.

## References

- [mapsforge/mapsforge](https://github.com/mapsforge/mapsforge) – the original Java implementation, including the binary format specification in the `docs` folder
- [mapsforge/vtm](https://github.com/mapsforge/vtm) – Vector Tile Map, the OpenGL-based renderer that informs much of the rendering approach
- [The Swift Programming Language](https://docs.swift.org/swift-book/) – official Swift book
- [Apple Metal documentation](https://developer.apple.com/metal/)

## License

MapsforgeKit is licensed under the **GNU Lesser General Public License v3.0** (LGPL-3.0), matching the license of the original Mapsforge project. See [LICENSE](LICENSE) for the full text and [COPYING](COPYING) for the GPL v3.0 that it references.

This means MapsforgeKit can be linked into applications, including commercial and closed-source ones, while modifications to MapsforgeKit itself must be released under LGPL-3.0. Using the same license as upstream Mapsforge keeps porting code and interoperating between the two projects straightforward.