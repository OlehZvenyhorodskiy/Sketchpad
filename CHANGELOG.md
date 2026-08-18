# 📝 CHANGELOG: Sketchpad Pro

All notable changes to the Sketchpad codebase across Android and Windows Desktop platforms are documented in this file.

---

## [2.0.0] - 2026-08-18 (Windows Desktop Port & Full Parity Release)

### 🌟 Added
- **Full Windows Desktop UI (100% Parity with Android):**
  - Complete recreation of all 30+ UI components in Compose Multiplatform.
  - Native Windows OS `MenuBar` (File, Edit, View, Tools, Layers, Academic, AI, Help) with global shortcuts (Ctrl+Z, Ctrl+Y, Ctrl+S, Ctrl+N, F11, etc.).
  - Animated `LoadingScreen` with smooth transitions on startup.
  - Dual-action `ExitProtectionDialog` preventing data loss when closing the application with unsaved changes.
- **Drawing Engine & Brush Ecosystem (17 Tools):**
  - Ported all 17 drawable tools: Pen, Pencil, Ink Pen, Fountain Pen, Marker, Airbrush, Crayon, Watercolor, Laser, Pointer, Selector (Single & Lasso), Eraser (Object & Pixel), Fill Bucket, Eyedropper, Ruler, Text, and Pixel Brush.
  - 9 Brush Presets and custom width/opacity side panels.
- **Theming & Neumorphism:**
  - `NeumorphicModifier.kt` delivering light and dark soft dual-shadow aesthetics.
  - `ThemedPanel.kt` supporting all 10 theme styles (Default, Forest, Sunset, Cyberpunk, Lavender, Sepia, Monochrome, Slate, Deep Blue, Crimson).
- **Academic & Study Suite:**
  - Integrated `CodeLab` code editor and executor for Python, C, and C++ with AST error analysis.
  - Integrated `StudyDeck` flashcard manager with SM-2 spaced repetition review algorithm.
  - Integrated `FunctionPlotterEngine` for vector math chart generation.
- **Multi-Provider Multimodal AI Assistant:**
  - Floating draggable `DesktopAiWindow` with multimodal vision analysis of active canvas drawings.
  - Multi-provider switching for Google Gemini, OpenAI GPT-4o, Anthropic Claude 3.5 Sonnet, DeepSeek, and Local LLM (Ollama/Custom).
  - Secure credential persistence in `%APPDATA%/Sketchpad/ai_config.json`.
- **Autosave & Multi-Layer Engine:**
  - 30-second atomic background autosave to `%APPDATA%/Sketchpad/autosave/` with `.bak` safety backups.
  - Interactive multi-layer management (add, select, reorder, rename, opacity slider, blend modes, visibility toggle).
  - Desktop audio recorder using standard `javax.sound` with live RMS amplitude visualizer and stroke sync markers.
- **Packaging & Export:**
  - Multi-format exporter (`DesktopExportManager.kt`) for PNG, JPEG, SVG, PDF, and native `.sketchpad` project files.
  - Standalone runnable UberJar built at `desktopApp/build/compose/jars/Sketchpad-windows-x64-2.0.0.jar`.

### ⚡ Optimized
- Replaced legacy empty desktop canvas stub with full interactive `DesktopCanvasEditorScreen`.
- Accelerated multi-layer rendering with Catmull-Rom spline curves and Ramer-Douglas-Peucker polyline simplification.

### 🧪 Tests & Quality Assurance
- 100% passing test suite across `:shared` and `:desktopApp`.
- Documented full audit and parity reports in `INVENTORY_REPORT.md`, `VISUAL_MAP.md`, `TEST_REPORT.md`, `docs/MIGRATION_GUIDE.md`, and `docs/FEATURE_PARITY_MATRIX.md`.
