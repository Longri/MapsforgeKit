//
// MapsforgeKit — Native Mapsforge implementation for Apple platforms
// Copyright (C) 2026 MapsforgeKit contributors
//
// LGPL-3.0; see LICENSE.
//

import Foundation

public enum MapFileError: Error {
    case fileNotFound(URL)
    case unreadable(URL)
}

/// Read-only entry point for a Mapsforge `.map` file.
///
/// Mirrors `org.mapsforge.map.reader.MapFile`: holds the parsed header
/// (`mapFileHeader`) and exposes the public `MapFileInfo` snapshot. POI / way
/// reading on top of the sub-file index is added in later phases.
public final class MapFile {

    private let mapFileHeader: MapFileHeader
    private let buffer: ReadBuffer
    private let fileURL: URL

    /// Parsed header metadata.
    public var mapFileInfo: MapFileInfo {
        mapFileHeader.info
    }

    public convenience init(url: URL) throws {
        let fm = FileManager.default
        guard fm.fileExists(atPath: url.path) else {
            throw MapFileError.fileNotFound(url)
        }
        guard fm.isReadableFile(atPath: url.path) else {
            throw MapFileError.unreadable(url)
        }

        let attrs = try fm.attributesOfItem(atPath: url.path)
        let fileSize = (attrs[.size] as? NSNumber)?.int64Value ?? 0

        let buffer = try ReadBuffer(url: url)
        let header = try MapFileHeader(buffer: buffer, fileSize: fileSize)
        self.init(url: url, buffer: buffer, header: header)
    }

    private init(url: URL, buffer: ReadBuffer, header: MapFileHeader) {
        self.fileURL = url
        self.buffer = buffer
        self.mapFileHeader = header
    }
}
