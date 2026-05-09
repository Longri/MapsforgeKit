//
// MapsforgeKit — Native Mapsforge implementation for Apple platforms
// Copyright (C) 2026 MapsforgeKit contributors
//
// This library is free software; you can redistribute it and/or modify it
// under the terms of the GNU Lesser General Public License as published by
// the Free Software Foundation; either version 3 of the License, or (at
// your option) any later version.
//

import Foundation

public enum ReadBufferError: Error {
    case unexpectedEndOfFile
    case invalidUTF8
    case invalidLength(Int)
}

/// Random-access reader for binary `.map` files.
///
/// Mirrors the role of `org.mapsforge.map.reader.ReadBuffer` in the Java
/// reference implementation. The buffer is filled from the underlying file
/// in explicit chunks (`readFromFile`) and parsed from memory afterwards;
/// this matches Mapsforge's two-phase approach where the header / a tile is
/// read into memory first and then decoded.
public final class ReadBuffer {

    private let handle: FileHandle
    private var bytes: [UInt8] = []
    private var pos: Int = 0

    public init(url: URL) throws {
        self.handle = try FileHandle(forReadingFrom: url)
    }

    deinit {
        try? handle.close()
    }

    // MARK: - File access

    /// Reads `length` bytes from the current file position into the internal
    /// buffer, replacing any previous contents. Returns `false` if the file
    /// did not contain enough bytes.
    @discardableResult
    public func readFromFile(length: Int) throws -> Bool {
        guard length >= 0 else { throw ReadBufferError.invalidLength(length) }
        let data = try handle.read(upToCount: length) ?? Data()
        guard data.count == length else { return false }
        bytes = Array(data)
        pos = 0
        return true
    }

    /// Absolute file offset (in bytes from the start of the file).
    public func filePosition() throws -> UInt64 {
        try handle.offset()
    }

    public func seek(toFileOffset offset: UInt64) throws {
        try handle.seek(toOffset: offset)
    }

    // MARK: - Buffer position

    public var bufferPosition: Int { pos }

    public func setBufferPosition(_ newPosition: Int) {
        pos = newPosition
    }

    public func skipBytes(_ count: Int) {
        pos += count
    }

    // MARK: - Primitive reads (big endian)

    public func readByte() -> Int8 {
        let v = bytes[pos]
        pos += 1
        return Int8(bitPattern: v)
    }

    public func readUByte() -> UInt8 {
        let v = bytes[pos]
        pos += 1
        return v
    }

    public func readShort() -> Int16 {
        let hi = UInt16(bytes[pos])
        let lo = UInt16(bytes[pos + 1])
        pos += 2
        return Int16(bitPattern: (hi << 8) | lo)
    }

    public func readInt() -> Int32 {
        let b0 = UInt32(bytes[pos])
        let b1 = UInt32(bytes[pos + 1])
        let b2 = UInt32(bytes[pos + 2])
        let b3 = UInt32(bytes[pos + 3])
        pos += 4
        return Int32(bitPattern: (b0 << 24) | (b1 << 16) | (b2 << 8) | b3)
    }

    public func readLong() -> Int64 {
        var v: UInt64 = 0
        for _ in 0..<8 {
            v = (v << 8) | UInt64(bytes[pos])
            pos += 1
        }
        return Int64(bitPattern: v)
    }

    // MARK: - VarInt (protobuf-compatible)

    /// Reads an unsigned variable-length integer (LEB128 / protobuf VarInt).
    public func readUnsignedInt() -> UInt32 {
        var result: UInt32 = 0
        var shift: UInt32 = 0
        while true {
            let b = bytes[pos]
            pos += 1
            result |= UInt32(b & 0x7F) << shift
            if (b & 0x80) == 0 { break }
            shift += 7
        }
        return result
    }

    /// Reads a signed variable-length integer using Mapsforge's sign-bit
    /// convention: the highest bit of the *last* (terminating) byte indicates
    /// the sign — *not* protobuf's zig-zag encoding.
    public func readSignedInt() -> Int32 {
        var result: UInt32 = 0
        var shift: UInt32 = 0
        while true {
            let b = bytes[pos]
            pos += 1
            if (b & 0x80) != 0 {
                // Continuation byte
                result |= UInt32(b & 0x7F) << shift
                shift += 7
            } else {
                // Terminating byte: bit 6 holds the sign.
                result |= UInt32(b & 0x3F) << shift
                let negative = (b & 0x40) != 0
                let magnitude = Int32(bitPattern: result)
                return negative ? -magnitude : magnitude
            }
        }
    }

    // MARK: - Strings

    /// Reads a UTF-8 string prefixed by an unsigned VarInt byte length.
    public func readUTF8String() throws -> String {
        let length = Int(readUnsignedInt())
        guard length >= 0, pos + length <= bytes.count else {
            throw ReadBufferError.unexpectedEndOfFile
        }
        let slice = bytes[pos..<(pos + length)]
        pos += length
        guard let s = String(bytes: slice, encoding: .utf8) else {
            throw ReadBufferError.invalidUTF8
        }
        return s
    }
}
