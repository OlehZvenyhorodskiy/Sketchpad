# Design

## Source of truth

- Status: Active
- Last refreshed: 2026-08-01
- Primary product surfaces: canvas home, canvas editor, appearance settings, export share sheet.
- Evidence reviewed: `app/src/main/java/com/example/ui/editor/InteractiveCanvas.kt`, `CanvasEditorScreen.kt`, `CanvasEditorViewModel.kt`, `ui/components/TopFloatingToolbar.kt`, `ui/components/AudioWaveformVisualizer.kt`, `ui/home/ThemeSettingsScreen.kt`, and `ui/theme/ThemeSpec.kt`.

## Brand

- Personality: calm, tactile, study-first notebook.
- Trust signals: direct manipulation, durable local state, visible tool state, and no surprising loss of handwriting.
- Avoid: controls that look interactive but have no effect; a canvas UI that consumes the working area; mobile panels that imply the wrong alignment.

## Product goals

- Goals: make stylus writing, pixel-level erasing, graph work, and lecture capture dependable on a tablet.
- Non-goals: imitate a desktop vector editor or require a network connection for essential drawing.
- Success signals: one-pixel erasing can shave a thick mark, graph axes stay on their cells as the frame is resized, and handwriting on a graph follows the graph when it moves.

## Personas and jobs

- Primary personas: tablet students using a stylus, including left-handed writers.
- User jobs: create lecture notes, correct handwriting precisely, annotate coordinate grids, record an explanation, and send finished notes to another app.
- Key contexts of use: one-handed tablet use, accidental palm contact, constrained landscape screen space, and offline study sessions.

## Information architecture

- Primary navigation: home → canvas editor → appearance settings.
- Core routes/screens: `home`, `editor/{canvasId}`, and `theme_settings` in `MainActivity.kt`.
- Content hierarchy: page content is always primary; drawing toolbar, recording state, and export are secondary overlays.

## Design principles

- Preserve direct-manipulation expectations: a pixel eraser is a clear mask, not destructive subdivision of a stroke.
- Keep coordinate-space objects stable: resizing a graph reveals cells instead of recentering its coordinate origin.
- Let temporary input states be obvious: recording includes a live waveform and stylus mode rejects incidental touch.
- Tradeoffs: graph attachments favour a stroke explicitly started on a graph; older notes fall back to geometric containment for compatibility.

## Visual language

- Color: Material 3 surface hierarchy with user-selectable accent and named theme palettes.
- Typography: compact labels and generous touch targets suitable for a tablet.
- Spacing/layout rhythm: floating tools are content-sized and centred; keep empty space around the canvas, not inside tool strips.
- Shape/radius/elevation: rounded, low-noise floating panels; high-contrast theme uses square, flat controls deliberately.
- Motion: recording pulse and waveform may animate; no animation may defer the actual ink/eraser result.
- Imagery/iconography: standard Material icons with text content descriptions.

## Components

- Existing components to reuse: `InteractiveCanvas`, `TopFloatingToolbar`, `VerticalFloatingSidePanel`, `AudioWaveformVisualizer`, `ThemeSettingsScreen`.
- New/changed components: pixel-erasure masks, graph origin metadata, graph-attached strokes, and anti-palm preference.
- Variants and states: pixel vs object eraser; stylus vs touch input; recording vs idle; 10 named themes.
- Token/component ownership: `ThemeSpec.kt` owns theme palettes; `UserPreferencesRepository.kt` owns persistent appearance and input preferences.

## Accessibility

- Target standard: practical tablet accessibility with clear content descriptions and high-contrast option.
- Keyboard/focus behavior: editor retains Ctrl shortcuts for undo, redo, export, copy, and paste.
- Contrast/readability: all themes provide Material on-surface colours; High contrast is a dedicated accessible option.
- Screen-reader semantics: toolbar buttons use localized content descriptions.
- Reduced motion and sensory considerations: waveform is supplemental; recording state remains visible without relying on motion.

## Responsive behavior

- Supported breakpoints/devices: Android tablets in portrait and landscape.
- Layout adaptations: toolbar remains horizontally scrollable when needed, but its panel stays content-sized and centred.
- Touch/hover differences: stylus has priority during writing; two touch pointers retain pan/zoom.

## Interaction states

- Loading: canvas content may load from local persistence without replacing in-memory edits.
- Empty: canvas can start blank or with a selected paper pattern.
- Error: audio permission denial is explained by a toast; export uses Android's chooser.
- Success: completed erasure updates immediately while the gesture is active.
- Disabled: locked layers do not accept paint or eraser masks.
- Offline/slow network: drawing, themes, export, and recording are local-first.

## Content voice

- Tone: concise, helpful, and study-oriented.
- Terminology: use “canvas”, “graph”, “pixel eraser”, and “anti-palm detection” consistently.
- Microcopy rules: describe the outcome of a control, not its implementation.

## Implementation constraints

- Framework/styling system: Kotlin, Jetpack Compose, Material 3, Room, and DataStore.
- Design-token constraints: extend `AppThemeStyle` and `ThemeSpecs`; do not introduce a competing theme system.
- Performance constraints: pointer movement must not wait for database persistence; eraser masks are simplified before persistence.
- Compatibility constraints: model additions have defaults so existing Moshi JSON notes remain readable.
- Test/screenshot expectations: unit tests cover eraser geometry and axis tick generation; rebuild the debug APK after interaction changes.

## Open questions

- [ ] Confirm on a physical stylus device whether the manufacturer reports palm contacts as `Touch` or `Palm`; owner: product; impact: anti-palm tuning.
- [ ] Decide whether graph labels should be independently draggable in a future graph-editing mode; owner: product; impact: graph data model.
