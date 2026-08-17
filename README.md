# Sketchpad (MeCanvas) — Multiplatform Tablet & Desktop Studio

[![Kotlin Multiplatform](https://img.shields.io/badge/Kotlin-Multiplatform-blue.svg?logo=kotlin)](https://kotlinlang.org/)
[![Compose Multiplatform](https://img.shields.io/badge/Compose-Multiplatform%201.7.3-8A2BE2.svg?logo=jetpackcompose)](https://www.jetbrains.com/lp/compose-multiplatform/)
[![Platform](https://img.shields.io/badge/Platform-Android%20%7C%20Windows-green.svg)](https://github.com/OlehZvenyhorodskiy/Sketchpad)
[![Tests](https://img.shields.io/badge/Tests-100%25%20Passing-brightgreen.svg)]()

**Sketchpad (MeCanvas)** is an academic sketchpad, mathematics whiteboard, and digital illustration studio engineered for tablet creators and desktop professionals. Built with **Kotlin Multiplatform (KMP)** and **Compose Multiplatform**, it delivers a seamless ecosystem across Android tablets and Windows PC workstations.

---

## 🚀 Key Features

### 💻 Windows Native Desktop Version (`:desktopApp`)
- **Skiko GPU-Accelerated Canvas**: Butter-smooth vector drawing with Catmull-Rom spline curves.
- **10 Curated Themes**: Midnight Obsidian, AMOLED Neon, Solar Flare, Cyberpunk Terminal, Dracula Studio, Nordic Frost, Academic Paper, Rose Gold Velvet, Forest Emerald, Deep Abyss.
- **Layers & Blend Modes**: Multi-layer stack with opacity, visibility locks, and Porter-Duff blend modes (`Multiply`, `Screen`, `Overlay`).
- **Comprehensive Hotkeys**: Instant switching (`B`, `E`, `P`, `M`, `S`, `R`), brush size controls (`[` / `]`), undo/redo (`Ctrl+Z` / `Ctrl+Y`), fullscreen (`F11`), and space-drag panning.
- **Multi-Format Export Engine**: Export to **SVG**, vector **PDF** (via OpenPDF), high-res **PNG/JPEG** (up to 4x scaling), Windows **.ico**, and portable **`.sketchpad`** ZIP project bundles.
- **Stylus & Digitizer Support**: Full Windows Ink API integration supporting active pen pressure and tilt angles.

### 📱 Android Tablet Version (`:app`)
- **White Canvas Mode**: One-tap distraction-free canvas hiding all buttons and system navigation for pure stylus sketching.
- **SketchLink Streaming**: Casts in-flight stylus strokes in real time to the desktop workstation at 120Hz.
- **AI Academic Assistant**: Powered by Firebase & Gemini vision for equation solving and diagram analysis.
- **Low-Latency Pen Input**: Hardware-accelerated palm rejection and velocity-adaptive stroke dynamics.

### ⚡ SketchLink (Tablet ↔ PC Real-Time Mirroring)
- **120 Hz Continuous Dispatch**: High-speed stroke streaming with sub-16ms latency.
- **PIN-Secured Handshake**: Instant 4-digit code pairing over local Wi-Fi or USB tethering (`adb forward`).
- **Offline Resilience**: Automatically buffers up to 30 seconds of strokes during network drops, ensuring zero lost strokes.

---

## 📦 Production Binaries

Ready-to-run release binaries are generated in the `releases/` directory:

| Binary | Platform | Size | Description |
|---|---|---|---|
| **`releases/Sketchpad-Android.apk`** | Android 8.0+ Tablet | ~19 MB | Production Android APK with White Canvas & SketchLink client |
| **`releases/Sketchpad-Windows.exe`** | Windows 10/11 x64 | ~540 KB | Standalone executable launcher |
| **`releases/Sketchpad-v2.0.0-windows-x64.zip`** | Windows 10/11 x64 | ~116 MB | Full portable standalone distribution with bundled OpenJDK 21 LTS |

---

## 🛠️ Build & Development Commands

### Prerequisites
- **JDK 21 LTS** (e.g. Eclipse Adoptium Temurin 21)
- **Android SDK** (API 35)

### Running Desktop App Locally
```bash
./gradlew :desktopApp:run
```

### Creating Desktop Standalone Distributable
```bash
./gradlew :desktopApp:createDistributable
```

### Building Android Release APK
```bash
./gradlew :app:assembleRelease
```

### Running Complete Test Suite
```bash
./gradlew test :app:testDebugUnitTest
```

---

## 📚 Technical Documentation

- [Architecture & Design Guide](file:///c:/Projects/00util/docs/ARCHITECTURE.md)
- [SketchLink Protocol Specification](file:///c:/Projects/00util/docs/PROTOCOL.md)
- [Developer API Reference](file:///c:/Projects/00util/docs/API.md)
- [System Requirements](file:///c:/Projects/00util/SYSTEM_REQUIREMENTS.md)
- [Changelog](file:///c:/Projects/00util/CHANGELOG.md)

---

## 📄 License
Copyright © 2026 Oleh Zvenyhorodskiy. Released under the Apache 2.0 License.
