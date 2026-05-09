//
// MapsforgeKit — Native Mapsforge implementation for Apple platforms
// Copyright (C) 2026 MapsforgeKit contributors
//
// LGPL-3.0; see LICENSE.
//

import Foundation

/// Snapshot of the metadata stored in the header of a `.map` file.
///
/// Field set mirrors `org.mapsforge.map.reader.header.MapFileInfo` so the
/// Java reference snapshots round-trip 1:1 against the Swift port.
public struct MapFileInfo: Sendable {

    public let fileVersion: Int32
    public let fileSize: Int64

    /// Always `"Mercator"` for Mapsforge maps.
    public let projectionName: String

    /// Tile size used when the map was written, in pixels (256 / 512 / …).
    public let tilePixelSize: Int32

    public let boundingBox: BoundingBox

    /// Map creation date, in milliseconds since the Unix epoch.
    public let mapDate: Int64

    public let createdBy: String?
    public let comment: String?

    public let startPosition: LatLong?
    public let startZoomLevel: Int8?

    public let zoomLevelMin: Int8
    public let zoomLevelMax: Int8

    /// Comma-separated list of preferred languages (e.g. `"en,de"`).
    public let languagesPreference: String?

    public let poiTags: [Tag]
    public let wayTags: [Tag]

    public let numberOfSubFiles: Int8
    public let debugFile: Bool
}
