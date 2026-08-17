# Changelog

All notable changes to the Sketchpad (MeCanvas) project are documented in this file.

## [2.0.0] - 2026-08-17 - Multiplatform Release & Desktop Port

### Added
- **Kotlin Multiplatform Core (`:shared`)**:
  - Extracted shared domain entities (`CanvasEntity`, `PageEntity`, `LayerEntity`, `StrokeEntity`, `HslaColor`).
  - Implemented high-precision geometric math: Catmull-Rom cubic splines, RDP polyline simplification, and swept-circle eraser collision math.
  - Implemented universal Undo/Redo Command Pattern.
  - Ported academic computation engines (`MathExpressionEvaluator`, `FunctionPlotterEngine`, `LocalCodeAnalyzer`, `SpacedRepetitionScheduler`).
- **Native Windows Desktop Application (`:desktopApp`)**:
  - Built with Jetpack Compose Multiplatform 1.7.3 & Skiko GPU acceleration.
  - 10 custom visual themes (Midnight Obsidian, AMOLED Neon, Solar Flare, Cyberpunk Terminal, Dracula Studio, Nordic Frost, Academic Paper, Rose Gold Velvet, Forest Emerald, Deep Abyss).
  - Skiko drawing canvas supporting multi-layer rendering, blend modes (`Normal`, `Multiply`, `Screen`, `Overlay`), layer opacity, and symmetry axes (Horizontal, Vertical, Quad).
  - Multi-format export engine: Vector SVG, PDF (OpenPDF), High-res PNG/JPEG rasterizer, Windows `.ico` icons, and `.sketchpad` ZIP project bundles.
  - Windows Ink API stylus pressure and tilt support.
  - Comprehensive desktop keyboard shortcuts (`Ctrl+Z/Y`, `B/E/P/M/S/R`, `[`/`]`, `1-9`, `F11`, Space drag pan).
  - Packaged standalone self-contained portable distribution with bundled OpenJDK 21 LTS runtime.
- **SketchLink Tablet ↔ PC Real-Time Mirroring System**:
  - Embedded Ktor WebSocket server on PC (port 8765) with 4-digit PIN authentication.
  - 120Hz continuous packet streaming from Android tablet with latency tracking ($< 16\text{ ms}$).
  - 30-second offline buffer queue (up to 4000 packets) for uninterrupted drawing during network handovers.
- **Android Tablet "White Canvas Mode" (`:app`)**:
  - One-tap distraction-free canvas hiding all buttons, toolbars, and system navigation bars.
  - Translucent floating action button for quick exit back to full editor mode.
  - Quick topbar toggle for SketchLink PC pairing dialog.

### Changed
- Refactored Android `:app` to consume `:shared` domain entities and protocols.
- Updated Gradle configuration to Java 21 toolchains with clean packaging rules.

### Fixed
- Fixed layer backwards-compatibility when opening single-layer Android canvases on Desktop.
- Resolved Netty metadata collision in Android APK packaging.
- 100% unit test suite passing across all modules (`:shared`, `:desktopApp`, `:app`).
