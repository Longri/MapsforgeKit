//
// MapsforgeKit — Native Mapsforge implementation for Apple platforms
// Copyright (C) 2026 MapsforgeKit contributors
//
// LGPL-3.0; see LICENSE.
//

import Foundation

public enum MapFileHeaderError: Error {
    case invalidMagicBytes
    case invalidHeaderSize(Int32)
    case unsupportedFileVersion(Int32)
    case truncated
    case invalidField(String)
}

/// Parses and exposes the header of a Mapsforge `.map` file.
///
/// Mirrors `org.mapsforge.map.reader.header.MapFileHeader` from the Java
/// reference implementation: it reads the on-disk header into memory once
/// and then hands out an immutable `MapFileInfo` value object.
public final class MapFileHeader {

    /// "mapsforge binary OSM" — 20 bytes.
    public static let magicBytes: [UInt8] = Array("mapsforge binary OSM".utf8)

    /// Mapsforge file format versions supported by this parser.
    public static let supportedFileVersions: ClosedRange<Int32> = 3...5

    public static let headerSizeMin: Int32 = 70
    public static let headerSizeMax: Int32 = 1_000_000

    // Header flag bits.
    private static let flagDebug: UInt8           = 0x80
    private static let flagStartPosition: UInt8   = 0x40
    private static let flagStartZoomLevel: UInt8  = 0x20
    private static let flagLanguagePref: UInt8    = 0x10
    private static let flagComment: UInt8         = 0x08
    private static let flagCreatedBy: UInt8       = 0x04

    private static let microdegreeFactor: Double = 1_000_000.0

    public let info: MapFileInfo

    /// Parses the header from `buffer`, which must already be positioned at
    /// the very start of the file.
    public init(buffer: ReadBuffer, fileSize: Int64) throws {
        // Phase 1: magic bytes + header size (24 bytes)
        guard try buffer.readFromFile(length: Self.magicBytes.count + 4) else {
            throw MapFileHeaderError.truncated
        }

        for expected in Self.magicBytes {
            if UInt8(bitPattern: buffer.readByte()) != expected {
                throw MapFileHeaderError.invalidMagicBytes
            }
        }

        let headerSize = buffer.readInt()
        guard headerSize >= Self.headerSizeMin && headerSize <= Self.headerSizeMax else {
            throw MapFileHeaderError.invalidHeaderSize(headerSize)
        }

        // Phase 2: read the remaining header bytes into the buffer.
        guard try buffer.readFromFile(length: Int(headerSize)) else {
            throw MapFileHeaderError.truncated
        }

        // File version
        let fileVersion = buffer.readInt()
        guard Self.supportedFileVersions.contains(fileVersion) else {
            throw MapFileHeaderError.unsupportedFileVersion(fileVersion)
        }

        // File size (as recorded in the header) — Mapsforge stores the total
        // file size redundantly here. We trust the on-disk value but pass the
        // actual file size in for cross-check / future validation.
        _ = fileSize
        let recordedFileSize = buffer.readLong()

        // Map creation date (ms since epoch)
        let mapDate = buffer.readLong()

        // Bounding box (microdegrees, signed int32)
        let minLat = Double(buffer.readInt()) / Self.microdegreeFactor
        let minLon = Double(buffer.readInt()) / Self.microdegreeFactor
        let maxLat = Double(buffer.readInt()) / Self.microdegreeFactor
        let maxLon = Double(buffer.readInt()) / Self.microdegreeFactor
        let bbox = BoundingBox(
            minLatitude: minLat,
            minLongitude: minLon,
            maxLatitude: maxLat,
            maxLongitude: maxLon
        )

        // Tile pixel size (uint16, BE) — Mapsforge stores it as a short.
        let tileSizeRaw = buffer.readShort()
        let tilePixelSize = Int32(UInt16(bitPattern: tileSizeRaw))

        // Projection name — only "Mercator" is defined.
        let projectionName = try buffer.readUTF8String()

        // Flags
        let flags = UInt8(bitPattern: buffer.readByte())
        let debugFile = (flags & Self.flagDebug) != 0
        let hasStartPosition = (flags & Self.flagStartPosition) != 0
        let hasStartZoom = (flags & Self.flagStartZoomLevel) != 0
        let hasLanguagePref = (flags & Self.flagLanguagePref) != 0
        let hasComment = (flags & Self.flagComment) != 0
        let hasCreatedBy = (flags & Self.flagCreatedBy) != 0

        // Optional start position
        var startPosition: LatLong? = nil
        if hasStartPosition {
            let lat = Double(buffer.readInt()) / Self.microdegreeFactor
            let lon = Double(buffer.readInt()) / Self.microdegreeFactor
            startPosition = LatLong(latitude: lat, longitude: lon)
        }

        // Optional start zoom level
        var startZoomLevel: Int8? = nil
        if hasStartZoom {
            startZoomLevel = buffer.readByte()
        }

        // Optional language preferences (since v4)
        var languagesPreference: String? = nil
        if hasLanguagePref {
            languagesPreference = try buffer.readUTF8String()
        }

        // Optional comment
        var comment: String? = nil
        if hasComment {
            comment = try buffer.readUTF8String()
        }

        // Optional createdBy
        var createdBy: String? = nil
        if hasCreatedBy {
            createdBy = try buffer.readUTF8String()
        }

        // POI tags: int16 count + n combined "key=value" strings
        let poiTags = try Self.readTagBlock(buffer)

        // Way tags
        let wayTags = try Self.readTagBlock(buffer)

        // Number of zoom intervals (sub files). We read them so the header
        // position is consistent for any later sub-file index parsing,
        // but only retain the count for now.
        let numberOfSubFiles = buffer.readByte()
        guard numberOfSubFiles > 0 else {
            throw MapFileHeaderError.invalidField("numberOfSubFiles must be > 0")
        }

        var minZoom = Int8.max
        var maxZoom = Int8.min
        for _ in 0..<Int(numberOfSubFiles) {
            _ = buffer.readByte()                           // base zoom
            let subMin = buffer.readByte()
            let subMax = buffer.readByte()
            _ = buffer.readLong()                           // sub-file start
            _ = buffer.readLong()                           // sub-file size
            if subMin < minZoom { minZoom = subMin }
            if subMax > maxZoom { maxZoom = subMax }
        }

        self.info = MapFileInfo(
            fileVersion: fileVersion,
            fileSize: recordedFileSize,
            projectionName: projectionName,
            tilePixelSize: tilePixelSize,
            boundingBox: bbox,
            mapDate: mapDate,
            createdBy: createdBy,
            comment: comment,
            startPosition: startPosition,
            startZoomLevel: startZoomLevel,
            zoomLevelMin: minZoom,
            zoomLevelMax: maxZoom,
            languagesPreference: languagesPreference,
            poiTags: poiTags,
            wayTags: wayTags,
            numberOfSubFiles: numberOfSubFiles,
            debugFile: debugFile
        )
    }

    private static func readTagBlock(_ buffer: ReadBuffer) throws -> [Tag] {
        let count = Int(buffer.readShort())
        guard count >= 0 else {
            throw MapFileHeaderError.invalidField("negative tag count: \(count)")
        }
        var tags: [Tag] = []
        tags.reserveCapacity(count)
        for _ in 0..<count {
            let combined = try buffer.readUTF8String()
            tags.append(Tag(combined: combined))
        }
        return tags
    }
}
