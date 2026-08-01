# Completion audit: canvas raster and graph fixes

## Prompt-to-artifact checklist

- Square graph cells: `ChartElementEntity.squarePixelsPerUnit()` normalizes legacy X/Y spacing; chart creation and rendering use one physical unit size. Regression: `CanvasInteractionMathTest.legacy chart normalises square cells while resize keeps global origin fixed`.
- Stable axes, ticks, and resize: `InteractiveCanvas.kt` keeps logical grid indices anchored to the graph origin, while `resizeFramePreservingOrigin()` is used by both preview and persistence. Regression: `CanvasInteractionMathTest.every unrotated corner keeps its diagonal corner fixed` and tick-value tests.
- Correct graph-owned marks and selection: `StrokeEntity.isAttachedToChart()` requires explicit ownership or every legacy control point inside the chart. Graph move/resize/copy paths use it. Regression: `CanvasInteractionMathTest.legacy chart attachment requires every saved point to be inside the graph`.
- Paint-style small eraser: `RasterStrokeCompositor.drawRasterStroke()` renders at one logical canvas pixel per bitmap pixel, clears masks before display scaling, and draws with nearest-neighbour filtering. The pixel-erasure path keeps `StrokeEntity` source geometry intact. Regression: `DrawingEngineEraserTest.two pixel raster mask cuts inside every thick brush without splitting its stroke`.
- Fast eraser swipe persistence and layered ink: `DrawingEngine.doesEraserPathAffectStroke()` detects swept segments; masks are stored per affected stroke. Regressions cover swept hit detection and newer ink over an erased corridor.
- Existing pages and exports: `ExportManager.kt` uses the shared raster compositor for PNG/PDF/high-resolution export, and SVG emits graph geometry and per-stroke transparency masks. Chart copy remaps child strokes and eraser marks.
- Delivery: `app-debug.apk` was built and installed to Android serial `89b62a34` using `adb install -r`, preserving application data.

## Verification evidence

- `./gradlew.bat --no-daemon testDebugUnitTest assembleDebug` — exit 0, `BUILD SUCCESSFUL` after the scoped cleanup.
- Targeted Robolectric tests for `DrawingEngineEraserTest` and `CanvasInteractionMathTest` — exit 0.
- `git diff --check` for the touched implementation and test files — no whitespace errors (only repository CRLF conversion notices).
- Independent architect review — `APPROVE — Architectural Status: CLEAR`.
- `adb -s 89b62a34 install -r app/build/outputs/apk/debug/app-debug.apk` — `Success`; `pm path com.aistudio.mecanvas.noteapp` confirmed the installed package.

## Remaining manual smoke check

The physical-device tactile check is intentionally left to the tablet user: erase a 2 px channel in a 20 px stroke at 100%, 200%, and 400%, then grow/shrink a chart using all four corners. This is a confirmation step, not a known blocker.
