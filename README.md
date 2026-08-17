# Sketchpad (MeCanvas)

A multiplatform drawing and note-taking application for Android tablets and Windows PC, built with Kotlin Multiplatform and Compose Multiplatform.

It supports pressure-sensitive stylus drawing, layers with blend modes, vector/raster export, and real-time canvas mirroring between a tablet and PC over a local network.

## Project Structure

The project is organized into three Gradle modules:

- `:shared` - Common domain models, math (Catmull-Rom splines, RDP polyline simplification), undo/redo command stack, and the SketchLink protocol.
- `:desktopApp` - Windows desktop application using Compose for Desktop (Skiko/Skia). Includes Windows Ink stylus support, hotkeys, and export tools.
- `:app` - Android application optimized for tablets with S-Pen/stylus input and White Canvas mode.

## Features

### Desktop (Windows)
- Skia-accelerated drawing canvas.
- Multiple layers with visibility, opacity, and blend modes (Normal, Multiply, Screen, Overlay).
- Export to SVG, PDF, PNG, JPEG, ICO, and `.sketchpad` project files.
- Windows Ink API integration (pen pressure and tilt).
- Keyboard shortcuts for tools, brush sizes, and actions.
- 10 UI themes (Dark, Light, AMOLED, Midnight, Cyberpunk, Dracula, etc.).

### Tablet (Android)
- White Canvas mode: hides all UI chrome to use the entire screen as a digitizer surface.
- S-Pen and active stylus pressure sensitivity.
- Low-latency drawing loop with palm rejection.

### SketchLink (Tablet to PC Mirroring)
- Streams stroke events over WebSocket (120 Hz) from the tablet to the PC.
- 4-digit PIN pairing over local Wi-Fi or USB (`adb forward`).
- Offline buffer (up to 30 seconds) in case of temporary connection drops.

## Desktop Shortcuts

| Key | Action |
| --- | --- |
| `B` | Brush tool |
| `E` | Eraser tool |
| `P` | Pen tool |
| `M` | Math / Graph tool |
| `S` | Select tool |
| `R` | Ruler guide |
| `[` / `]` | Decrease / increase brush size |
| `1` - `9` | Select color swatch |
| `Ctrl + Z` / `Ctrl + Y` | Undo / Redo |
| `Ctrl + S` | Export project |
| `Space + Drag` | Pan canvas |
| `F11` | Toggle fullscreen |

## Building & Running

### Requirements
- JDK 21
- Android SDK (API 34/35)

### Desktop App
Run locally:
```bash
./gradlew :desktopApp:run
```

Create standalone Windows package:
```bash
./gradlew :desktopApp:createDistributable
```
Output will be generated in `desktopApp/build/compose/binaries/main/app/`.

### Android App
Build release APK:
```bash
./gradlew :app:assembleRelease
```
Output will be generated in `app/build/outputs/apk/release/`.

### Running Tests
```bash
./gradlew test :app:testDebugUnitTest
```

## Documentation

- [Architecture](docs/ARCHITECTURE.md)
- [SketchLink Protocol](docs/PROTOCOL.md)
- [Developer API Reference](docs/API.md)
- [System Requirements](SYSTEM_REQUIREMENTS.md)
- [Changelog](CHANGELOG.md)

## License

Apache License 2.0. See LICENSE for details.
