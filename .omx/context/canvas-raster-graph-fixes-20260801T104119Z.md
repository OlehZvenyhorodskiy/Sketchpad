# Ralph context: canvas raster and graph fixes

## Task statement

Fix the regressions demonstrated on the Mi tablet: graph cells are not square, axis tick marks disappear during zoom, graph selection/resize becomes broken after resizing, a dragged lower-left corner affects the wrong side, and the eraser must behave like Microsoft Paint by removing only raster pixels inside thick marks.

## Desired outcome

- Graph grid cells remain visually square at every viewport zoom.
- Axis ticks remain attached to visible unit lines and do not vanish merely because the canvas zoom changed.
- A chart remains selectable and resizable after arbitrary grow/shrink cycles.
- Each corner resize keeps the diagonally opposite corner fixed and follows the dragged corner.
- Chart origin and unit scale remain stable while resizing reveals or hides cells.
- Marks drawn on a graph translate with it, but do not unexpectedly scale during resize.
- Every drawing tool produces a raster-composited visual result that a small eraser can cut through pixel-by-pixel, including inside a single thick mark.
- Existing pages remain loadable, editable, and exportable.

## Known facts and evidence

- Screenshot `codex-clipboard-9a1edcde-8688-47f1-8cee-59296db42b6c.png` shows a selected chart whose logical selection bounds extend far beyond the rendered grid, leaving orphan dots and a broken interaction target.
- Screenshot `codex-clipboard-a0fb274b-52b1-49bb-85dc-cb419539acd6.png` shows resize handles and content diverging after a lower-left resize; the grid also appears anisotropic.
- Screenshot `codex-clipboard-d9bb78bc-5fe8-4ccc-bcc7-e1350337bd9f.png` is the Microsoft Paint reference: a thick black raster region has narrow white eraser cuts inside it without deleting the whole mark.
- Current model stores vector `StrokeEntity` paths plus persistent `EraserMark` masks; rendering uses clear compositing.
- Current charts persist `pixelsPerUnitX`, `pixelsPerUnitY`, and local origin offsets.
- The worktree already contains user-owned and earlier uncommitted edits; they must be preserved.
- No `AGENTS.md` was found in the repository.

## Constraints

- Android/Jetpack Compose project.
- Preserve existing document data and install with `adb install -r`; never uninstall because that may erase app data.
- Use targeted edits only and do not reset unrelated dirty changes.
- Validate with unit tests, debug assembly, and a real-device smoke check where automation permits.

## Unknowns and open questions

- Whether legacy saved pages contain already-invalid chart bounds/origins requiring normalization at render/selection time.
- Whether the unwanted eraser behavior is caused by mask geometry, layer compositing, or stroke rendering order.
- Whether device UI automation can cover stylus paths; otherwise deterministic renderer/unit tests must cover geometry and mask semantics.

## Likely codebase touchpoints

- `app/src/main/java/com/example/ui/editor/InteractiveCanvas.kt`
- `app/src/main/java/com/example/ui/editor/CanvasEditorViewModel.kt`
- `app/src/main/java/com/example/data/models/CanvasModels.kt`
- `app/src/main/java/com/example/drive/ExportManager.kt`
- drawing/eraser tests under `app/src/test`

