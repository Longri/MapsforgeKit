//
// MapsforgeKit — Native Mapsforge implementation for Apple platforms
// Copyright (C) 2026 MapsforgeKit contributors
//
// LGPL-3.0; see LICENSE.
//

import Foundation

/// An OSM tag (key/value pair) as stored in the `.map` POI / way tag tables.
/// Mirrors `org.mapsforge.core.model.Tag`.
public struct Tag: Equatable, Sendable, CustomStringConvertible {
    public let key: String
    public let value: String

    public init(key: String, value: String) {
        self.key = key
        self.value = value
    }

    /// Parses a `key=value` pair as encoded in the `.map` binary format.
    /// If no `=` is present the whole string is treated as the key.
    public init(combined: String) {
        if let eq = combined.firstIndex(of: "=") {
            self.key = String(combined[..<eq])
            self.value = String(combined[combined.index(after: eq)...])
        } else {
            self.key = combined
            self.value = ""
        }
    }

    /// Matches Java's `Tag.toString()` exactly so reference snapshots line up.
    public var description: String {
        "key=\(key), value=\(value)"
    }
}
