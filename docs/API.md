# Sketchpad Developer API Reference (v2.0.0)

## 1. Module Overview

| Module | Target Platform | Description |
|---|---|---|
| `:shared` | JVM / Android / Desktop (KMP) | Core domain entities, math geometry, academic engines, network protocol |
| `:desktopApp` | Windows PC (Compose Multiplatform) | Skiko rendering view, export engine, hotkey system, Windows Ink API |
| `:app` | Android Tablet (Jetpack Compose) | Tablet UI, Room database, AI vision assistant, White Canvas client |

---

## 2. Shared Module (`:shared`)

### 2.1 `com.example.shared.math.DrawingMath`
High-performance geometric calculations and curve interpolation:

```kotlin
object DrawingMath {
    // Generates smooth Catmull-Rom cubic splines through control points
    fun catmullRomSpline(points: List<StrokePoint>, segmentsPerStep: Int = 4): List<StrokePoint>

    // Simplifies dense polylines using Ramer-Douglas-Peucker algorithm
    fun ramerDouglasPeucker(points: List<StrokePoint>, epsilon: Float = 1.0f): List<StrokePoint>

    // Evaluates point-to-segment distance for swept-circle eraser collision
    fun distanceToSegment(p: StrokePoint, a: StrokePoint, b: StrokePoint): Float

    // Creates symmetric copies of active strokes across Horizontal, Vertical, or Quad axes
    fun generateSymmetricStrokes(
        stroke: StrokeEntity, 
        mode: SymmetryMode, 
        canvasWidth: Float, 
        canvasHeight: Float
    ): List<StrokeEntity>
}
```

### 2.2 `com.example.shared.academic.*`
Academic computation, code syntax parsing, and spaced repetition engines:

- **`MathExpressionEvaluator`**: Parses and evaluates arithmetic and trigonometric expressions (e.g. `2 * pi * sin(pi/4)`).
- **`FunctionPlotterEngine`**: Samples mathematical curves $f(x)$ over $[x_{\min}, x_{\max}]$ generating normalized `(x, y)` coordinate series.
- **`LocalCodeAnalyzer`**: Performs local keyword tokenization and syntax highlighting for Kotlin, Python, C/C++, Java, and Rust.
- **`SpacedRepetitionScheduler`**: Implements SuperMemo SM-2 algorithm for academic flashcard recall intervals.

---

## 3. Desktop Application (`:desktopApp`)

### 3.1 `com.example.desktop.export.DesktopExportManager`
Multi-format vector and raster serialization:

```kotlin
object DesktopExportManager {
    // Export single or multi-page canvas to Scalable Vector Graphics (SVG)
    fun exportToSvg(page: PageEntity, targetFile: File)

    // Export canvas pages to vector-grade PDF document
    fun exportToPdf(pages: List<PageEntity>, targetFile: File, title: String)

    // Export canvas layer composite to high-res PNG or JPEG bitmap
    fun exportToRaster(page: PageEntity, targetFile: File, format: String, scale: Float = 2.0f)

    // Export portable .sketchpad ZIP project bundle
    fun exportToSketchpadBundle(canvas: CanvasEntity, targetFile: File)

    // Import and parse .sketchpad ZIP project bundle
    fun importFromSketchpadBundle(sourceFile: File): CanvasEntity?
}
```

### 3.2 `com.example.desktop.DesktopViewModel`
Central state holder managing desktop canvas state, undo/redo command stack, theme transitions, and SketchLink server connections:

- `currentPage: StateFlow<PageEntity>`
- `currentTheme: StateFlow<DesktopThemeSpec>`
- `isSketchLinkRunning: StateFlow<Boolean>`
- `pairingPin: StateFlow<String>`
- `connectedClientsCount: StateFlow<Int>`
- `canUndo: StateFlow<Boolean>` / `canRedo: StateFlow<Boolean>`

---

## 4. Android Application (`:app`)

### 4.1 `com.example.ui.editor.CanvasEditorViewModel`
Android UI state coordinator managing tablet canvas interaction:

- `isWhiteCanvasMode: StateFlow<Boolean>`: Toggle state for White Canvas distraction-free mode.
- `sketchLinkClient: SketchLinkClient`: Embedded WebSocket client streaming stylus motion events to desktop workstation.
- `toggleWhiteCanvasMode()`: Enters/exits White Canvas mode.
- `connectToSketchLink(host: String, port: Int, pin: String)`: Establishes low-latency pairing session with PC.
