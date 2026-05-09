//
// MapsforgeKit — Native Mapsforge implementation for Apple platforms
// Copyright (C) 2026 MapsforgeKit contributors
//
// LGPL-3.0; see LICENSE.
//

import Foundation

/// A WGS84 coordinate. Mirrors `org.mapsforge.core.model.LatLong`.
public struct LatLong: Equatable, Sendable {
    public let latitude: Double
    public let longitude: Double

    public init(latitude: Double, longitude: Double) {
        self.latitude = latitude
        self.longitude = longitude
    }
}
