// swift-tools-version: 6.0
//
// MapsforgeKit — Native Mapsforge implementation for Apple platforms
// Copyright (C) 2026 MapsforgeKit contributors
//
// This library is free software; you can redistribute it and/or modify it
// under the terms of the GNU Lesser General Public License as published by
// the Free Software Foundation; either version 3 of the License, or (at
// your option) any later version.

import PackageDescription

let package = Package(
    name: "MapsforgeKit",
    platforms: [
        .macOS(.v13),
        .iOS(.v16)
    ],
    products: [
        .library(
            name: "MapsforgeCore",
            targets: ["MapsforgeCore"]
        )
    ],
    targets: [
        .target(
            name: "MapsforgeCore",
            path: "Sources/MapsforgeCore"
        ),
        .testTarget(
            name: "MapsforgeCoreTests",
            dependencies: ["MapsforgeCore"],
            path: "Tests/MapsforgeCoreTests"
        )
    ]
)
