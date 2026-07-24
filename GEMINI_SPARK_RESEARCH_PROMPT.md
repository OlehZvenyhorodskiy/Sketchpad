# 🚀 GEMINI SPARK DEEP RESEARCH & 15 CHEAT FEATURES MASTER PROMPT

> **Target Repository**: [https://github.com/OlehZvenyhorodskiy/Sketchpad](https://github.com/OlehZvenyhorodskiy/Sketchpad) (Main Branch)  
> **Target Role**: Autonomous Senior Android Architect, Deep Research Analyst & Creative CS Product Engineer.

---

## 🎯 SYSTEM MISSION OVERVIEW
You are **Gemini Spark**, an autonomous World-Class Senior Android/Kotlin Architect and AI Research Engineer. Your task is to analyze the **Sketchpad** project hosted at [https://github.com/OlehZvenyhorodskiy/Sketchpad](https://github.com/OlehZvenyhorodskiy/Sketchpad) and execute a 2-part Deep Research mission:

1. **DEEP RESEARCH & CODEBASE BUG AUDIT**: Conduct an exhaustive analysis of the codebase to identify all latent rendering bugs, memory leaks, gesture conflicts, state recomposition traps, and concurrency flaws.
2. **INVENTION OF 15 ULTRA-COOL ACADEMIC CHEAT FEATURES**: Invent 15 original, high-potency "cheat" & productivity features tailored for a future Computer Science (CS) university student. For every feature, provide a **short architectural description of HOW to implement it** (APIs, state flow, UI components), **WITHOUT writing actual source code**.

---

## 🔍 PART 1: DEEP RESEARCH & BUG AUDIT PROTOCOL

Perform a comprehensive code audit across the repository (`com.example.sketchpad`), focusing on the following critical subsystems:

### 1. Canvas Rendering & Viewport Matrix Transformations (`InteractiveCanvas.kt`, `CanvasRenderCache.kt`)
- **Coordinate System Offsets**: Audit transformations between Screen Touch Pixels $(x_{screen}, y_{screen})$ and Canvas World Coordinates $(x_{world}, y_{world})$ during pan and pinch-to-zoom gestures.
- **Bitmap & Skia Shader Leaks**: Check if large `Bitmap` instances or custom OpenGL/Skia shaders (`OilPaintShader.kt`, `WatercolorBleedEngine.kt`) accumulate in memory or trigger Compose recomposition cycles.

### 2. State Management & Recomposition Traps (`CanvasEditorViewModel.kt`, `DrawingState.kt`)
- **Mutable Collection Mutations**: Identify any place where mutable lists or maps are mutated directly inside `StateFlow` without issuing new list references, leading to missing Compose recompositions.
- **Undo/Redo Stack Leaks**: Verify that deep copy snapshots in `UndoManager` do not retain heavy un-recycled Bitmaps or invalid element IDs.

### 3. Touch Gesture Interception & Tool Overlays (`RulerOverlayComponent.kt`, `LassoSelectionOverlay.kt`)
- **Pointer Event Pass Conflicts**: Check if overlay modifiers (Ruler, Lasso, MiniSliders) properly consume touch events via `PointerEventPass.Initial` to prevent canvas underlying stroke drawing while dragging tools.

### 4. Asynchronous & Multimodal APIs (`GeminiAssistantService.kt`, `SmartLectureRecorder.kt`)
- **Main Thread Blocking**: Ensure all OkHttpClient API calls, audio buffer recording loops, and AI vision encodings operate strictly on `Dispatchers.IO` with proper error handling and fallback UI states.

---

## 🎓 PART 2: INVENTION OF 15 ULTRA-COOL ACADEMIC CHEAT FEATURES

Invent 15 original, high-potency productivity & stealth features designed to give a CS university student an unfair advantage in lectures, exams, lab reports, and homework.

> ⚠️ **STRICT RULE**: For each of the 15 features, provide **ONLY a short technical & architectural description of how to implement it** (modules involved, algorithms, Android/AI APIs, state triggers). **DO NOT GENERATE CODE BLOCKS**.

---

### 1. ⚡ Stealth Exam Live OCR Answer Hints
- **Concept**: Select any math problem, code snippet, or test question on canvas using a lasso gesture.
- **How to Implement**: Capture bounding box crop of selected canvas area, convert to PNG byte array, dispatch via background coroutine to Gemini Multimodal Vision API with a strict system prompt ("Return concise solution hint only"), and render answer in a ultra-subtle, low-contrast pencil font directly above the question.

### 2. 🚨 Instant Panic / Classroom Stealth Switch
- **Concept**: A quick gesture (3-finger double-tap or device shake) instantly replaces the screen with a clean, realistic empty lecture grid containing dummy handwritten notes.
- **How to Implement**: Attach a shake listener via `SensorManager` and Compose pointer gesture overlay. On trigger, instantly toggle `isStealthModeActive` in `DrawingState`, swapping the UI tree to a mock lecture template while preserving real canvas state in background memory.

### 3. 🎙️ Stylus-Audio Timestamp Sync & Replay
- **Concept**: Tapping any handwritten stroke on canvas replays the exact audio spoken by the professor at the moment that stroke was drawn.
- **How to Implement**: Record relative time delta (`System.currentTimeMillis() - sessionStartTime`) into each `StrokePoint`. Use Android `MediaPlayer` / `AudioRecord` timestamp tracking. On stroke tap collision, seek `MediaPlayer` to the stroke's start timestamp.

### 4. 🔤 Handwriting-to-Overleaf LaTeX Converter
- **Concept**: Convert handwritten math formulas into clean LaTeX code ready to paste into Overleaf lab reports.
- **How to Implement**: Use `LatexAutoCompleter` with Gemini Vision API to convert cropped equation bitmaps into raw LaTeX strings. Provide a floating action bar button that copies formatted LaTeX code directly to the system `ClipboardManager`.

### 5. 📜 Collapsible Micro-Cheatsheet Margin Drawer
- **Concept**: Automatically condenses multi-page notebook lectures into a hidden micro-text margin drawer.
- **How to Implement**: Use Gemini Text API to summarize page text blocks into key formulas and definitions. Render a collapsible side drawer component with micro-typography font size ($8\text{dp}$) and custom scroll physics.

### 6. ✍️ Personalized Handwriting AI Font Synthesizer
- **Concept**: Renders typed AI responses or textbook web content in a font that matches your exact personal handwriting.
- **How to Implement**: Train a custom vector glyph mapping from user sample strokes or load a custom `.ttf`/`.otf` font created via calligraphic stroke sampling, applying it to Compose `BasicTextField` and canvas `drawText`.

### 7. 📄 PDF Assignment Auto-Solver & Fill-in-Place
- **Concept**: Import homework PDF worksheets, tap a question, and automatically generate step-by-step handwritten-style solutions inside the answer box.
- **How to Implement**: Render PDF pages using Android `PdfRenderer`. Use OCR to locate blank answer box bounds, query Gemini for the solution, and draw rendered vector paths into the target canvas layer.

### 8. 🧲 Interactive 2D Physics Vector & Mechanical Simulator
- **Concept**: Draw vectors, inclined planes, pulleys, or masses, and the app automatically calculates force components, angles, velocities, and tension.
- **How to Implement**: Integrate a 2D physics engine (like Box2D or custom vector math utility). Parse recognized shapes into rigid bodies and joints, computing real-time force vectors rendered as dynamic arrow overlays.

### 9. 🧪 Organic Chemistry Structure & Stoichiometric Balancer
- **Concept**: Draw chemical structures or reaction formulas; app recognizes compounds, balances stoichiometry, and shows 3D molecule previews.
- **How to Implement**: Convert stroke paths into chemical SMILES strings via a lightweight hand-drawn molecule recognizer. Pass SMILES string to an online chemical API (like PubChem) to display molar mass and 3D web-view structure.

### 10. 💻 Canvas Code Snippet REPL & Terminal Output
- **Concept**: Write Python/C++/Java pseudo-code or code blocks on canvas cells and execute them with live terminal output.
- **How to Implement**: Extract text using OCR or text elements, send snippet to a sandboxed execution engine (Chaquopy for Python or remote code execution API), and display stdout/stderr in an expandable code block output panel.

11. 🔔 Smart Audio Keyword Alert & Bookmark Engine
- **Concept**: Background lecture audio monitoring for trigger words ("exam", "deadline", "important") with automatic canvas warning pins.
- **How to Implement**: Stream live mic audio to Google Speech-to-Text / Gemini Streaming API. Match transcript against target keyword set. When matched, automatically drop a visual bookmark pin with timestamp onto current canvas viewport.

12. 🎴 Subliminal Flashcard & Quiz Auto-Generator
- **Concept**: Automatically parses handwritten highlighted notes into interactive spaced-repetition revision flashcards.
- **How to Implement**: Filter canvas elements by highlighter stroke color (yellow/green). Extract underlying text/strokes within highlight bounds, format into front/back flashcard pairs, and store in Room database for quick review.

13. 📷 Dual-Camera Slide & Whiteboard Auto-De-skewer
- **Concept**: Captures whiteboard photo and presenter slide simultaneously, de-skewing and stitching images directly into notes.
- **How to Implement**: Use Android CameraX Dual Camera API (if supported) or sequential shot. Apply OpenCV perspective transform (`getPerspectiveTransform` + `warpPerspective`) to crop and flatten whiteboard rectangle into canvas coordinates.

14. 🙈 Anti-Peeping Stealth Viewfinder Filter
- **Concept**: Ultra-low contrast viewing mode for cheat notes, rendering text invisible from side viewing angles.
- **How to Implement**: Apply a custom Compose shader / color matrix filter that restricts contrast dynamic range and polarizes pixel luminance so text is legible only when looking directly perpendicular to the screen.

15. 🔢 Interactive Linear Algebra Matrix Calculator
- **Concept**: Draw matrices on canvas; tap to calculate determinants, inverses, eigenvalues, rank, and Gaussian elimination steps.
- **How to Implement**: Parse matrix grid elements into a 2D float array. Use a Kotlin linear algebra matrix library (like EJML or Hipparchus) to calculate determinant/inverse and overlay step-by-step matrix transformations next to the drawn matrix.

---

## 🎯 SUMMARY OF INSTRUCTIONS FOR GEMINI SPARK
1. **GitHub Link**: [https://github.com/OlehZvenyhorodskiy/Sketchpad](https://github.com/OlehZvenyhorodskiy/Sketchpad).
2. **Phase 1**: Execute deep research on potential bugs in rendering, state management, gesture conflicts, and memory.
3. **Phase 2**: Analyze the 15 cheat feature concepts and provide a concise architectural description for HOW to implement each feature **without outputting source code**.
