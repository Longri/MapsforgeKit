# Apple (Xcode Project)

This directory contains the Xcode project for MapsforgeKit (macOS and iOS).

## Setup

The Xcode project itself is created locally. To set it up:

1. Open Xcode.
2. **File → New → Project…**
3. Choose **Multiplatform → App** (or **macOS → App** if you only want macOS first; you can add an iOS target later).
4. Product Name: `MapsforgeKit`
5. Save the project **inside this `apple/` directory**, so the resulting layout is:
   ```
   apple/
   ├── MapsforgeKit.xcodeproj/
   └── MapsforgeKit/
       └── ...
   ```
6. Commit the new files.

## Notes

- `xcuserdata/` and `DerivedData/` are git-ignored (see root `.gitignore`).
- Build output and SwiftPM caches are also ignored.
- Use `MTKView` wrapped via `NSViewRepresentable` (macOS) and `UIViewRepresentable` (iOS) for the Metal renderer once Phase 4 starts.
