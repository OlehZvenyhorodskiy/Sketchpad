# 🔍 ДЕТАЛЬНИЙ АУДИТ ПРОЄКТУ SKETCHPAD (MECANVAS)
**Версія:** 1.0.0 → 2.0.0 (KMP + Compose Multiplatform Ready)  
**Дата аудиту:** 17 серпня 2026 р.  
**Цільові платформи:** Android (Tablet-optimized) & Windows 10/11 x64 (Native Desktop)

---

## 1. СТРУКТУРА МОДУЛІВ ТА ПАКЕТІВ (`app/src/main/java`)

```
com.example/
├── MainActivity.kt                       # Головна Activity, Compose Navigation Graph, Edge-to-Edge
├── academic/                             # Академічні та STEM інструменти
│   ├── FunctionPlotterEngine.kt          # Побудова 2D графіків математичних функцій, адаптивний семплінг
│   ├── HandwritingLatexConverter.kt      # Розпізнавання рукописних математичних виразів у LaTeX
│   ├── MathExpressionEvaluator.kt        # Парсер та обчислювач математичних формул
│   ├── ShapeRecognizerEngine.kt          # Геометричне розпізнавання фігур з рукописних штрихів
│   ├── SmartLectureRecorder.kt           # Синхронізація аудіозапису лекції зі штрихами на полотні
│   ├── code/
│   │   └── LocalCodeAnalyzer.kt          # Локальний статичний аналізатор коду (Python, C, C++)
│   └── study/
│       └── SpacedRepetitionScheduler.kt  # SM-2 / Leitner алгоритм інтервального повторення карток
├── ai/                                   # Інтеграція штучного інтелекту
│   ├── AiModelDefaults.kt                # Конфігурації моделей (Gemini, OpenAI, Claude, DeepSeek, Ollama)
│   ├── AiProvider.kt                     # Реєстр провайдерів AI, безпечне керування ключами та ендпоінтами
│   ├── GeminiAssistantService.kt         # Сервіс Gemini (Streaming chat, Vision по полотну, SharedPreferences)
│   ├── HandwritingOcrService.kt          # OCR розпізнавання рукописного тексту
│   └── latex/
│       └── LatexAutoCompleter.kt         # Автодоповнення синтаксису LaTeX
├── audio/                                # Аудіопідсистема
│   └── AudioRecorderManager.kt           # Запис через MediaRecorder, відтворення, амплітуда для візуалізатора
├── brush/                                # Двигун пензлів та пресетів
│   ├── BrushEngine.kt                    # Генерація stroke-точок, варіація ширини, згладжування
│   ├── BrushPresets.kt                   # Вбудовані пресети (Fine, Medium, HB, 2B, Marker, Calligraphy, Spray, Dashed)
│   ├── BrushProfile.kt                   # Модель профілю пензля та криві тиску (Linear, Ease-in, Heavy, Light)
│   └── recognizer/
│       └── VectorShapeRecognizer.kt      # Векторизація та підгонка полігонів
├── core/                                 # Ядро малювання та математики
│   ├── drawing/
│   │   ├── DrawingState.kt               # DrawingEngine: Catmull-Rom сплайни, RDP спрощення, ластик, фігури, лінійка
│   │   ├── PathSmoothing.kt              # Catmull-Rom інтерполяція кривих
│   │   ├── PixelCanvas.kt                # Растровий піксель-арт двигун
│   │   └── PressureProcessor.kt          # Обробка та згладжування сили натиску / швидкості
│   ├── gesture/
│   │   ├── MultiTouchGestureHandler.kt   # 2-пальцевий pan/zoom/rotate
│   │   └── PalmRejectionFilter.kt        # Фільтрація долоні, розділення stylus та finger
│   └── render/
│       ├── CanvasRenderCache.kt          # Кешування шарів у Bitmap, dirty-rectangles
│       └── LayerCompositor.kt            # Композитинг шарів із режимами BlendMode (Multiply, Screen тощо)
├── data/                                 # Шар даних (Database, Models, Repositories)
│   ├── db/
│   │   ├── AppDatabase.kt                # Room Database (canvases, pages, audio, brushes, study_decks, references)
│   │   ├── CanvasDao.kt                  # DAO для проектів та сторінок
│   │   ├── CanvasReferenceDao.kt         # DAO для міжполотняних посилань
│   │   ├── CustomBrushDao.kt             # DAO для кастомних пензлів
│   │   ├── MoshiConverters.kt            # JSON конвертери для Room (Moshi)
│   │   └── StudyDeckDao.kt               # DAO для карток повторення
│   ├── models/
│   │   ├── CanvasModels.kt               # CanvasEntity, PageEntity, LayerEntity, StrokeEntity, HslaColor, ToolType...
│   │   ├── CanvasReferenceModels.kt      # Моделі закладок, навігаційних переходів між нотатками
│   │   ├── CanvasWidget.kt               # Базові інтерфейси інтерактивних віджетів
│   │   ├── CodeBlockModels.kt            # Модель блоку коду (Python, C, C++), вивід консолі, діагностика
│   │   ├── ColorPalette.kt               # Палітри кольорів (Classic, Pastel, Neon, Academic...)
│   │   ├── CommandModels.kt              # Command Pattern для Undo/Redo
│   │   ├── CustomBrushEntity.kt          # Сутність кастомного пензля в БД
│   │   └── StudyDeckModels.kt            # Сутності колод та карток
│   ├── repository/
│   │   ├── CanvasReferenceRepository.kt  # Репозиторій навігації між полотнами
│   │   ├── CanvasRepository.kt           # Репозиторій полотен, сторінок, thumbnail'ів
│   │   ├── CollaborationRepository.kt    # Репозиторій для спільної роботи через сокети
│   │   ├── StudyDeckRepository.kt        # Репозиторій для колод карток
│   │   └── UserPreferencesRepository.kt  # DataStore збереження налаштувань
│   └── storage/
│       ├── AtomicCanvasStorage.kt        # Атомарний файловий I/O із захистом від пошкоджень
│       └── SettingsBackupManager.kt      # Експорт/імпорт налаштувань та резервних копій
├── di/
│   └── AppModule.kt                      # Manual Dependency Injection (Service Locator)
├── drive/                                # Експорт та синхронізація
│   ├── ExportManager.kt                  # Експорт PNG, SVG, PDF, інтеграція з Obsidian Vault
│   └── SyncManager.kt                    # Синхронізація з хмарою (Google Drive)
├── localization/
│   └── AppLanguage.kt                    # Локалізація (EN, UK, NL, DE, FR)
└── ui/                                   # Презентаційний шар (Compose UI)
    ├── components/                       # 30 UI компонентів (Toolbars, Sheets, Dialogs, Overlays)
    ├── editor/                           # Редактор полотна (CanvasEditorScreen, ViewModel, InteractiveCanvas, BrushEditor)
    ├── home/                             # Головний екран (HomeScreen, HomeViewModel, ThemeSettingsScreen)
    ├── localization/                     # LanguageSetupScreen
    ├── theme/                            # Стилі тем (ThemeSpec, NeumorphicModifier, ThemedPanel, Color, Type)
    └── util/                             # Утиліти (BitmapLoader з EXIF обробкою)
```

---

## 2. ІСНУЮЧІ ТЕМИ ТА КОЛЬОРОВІ ПАЛІТРИ

### Теми (`AppThemeStyle.kt` & `ThemeSpec.kt`):
1. **`SYSTEM_DEFAULT`** — Світла/Темна Material 3 адаптивна тема (акцент `#38BDF8`).
2. **`PAPER_NOTEBOOK`** — Текстура паперового блокнота, бежеві тони (`#F5F1E8`, `#8C6D46`), радіус скруглення 8dp.
3. **`NEUMORPHISM`** — М'який подвійний тіньовий неоморфізм для світлого/темного режиму (`#E0E5EC` / `#242830`).
4. **`AMOLED_BLACK`** — Справжній глибокий чорний колір (`#000000`) для економії батареї на OLED/AMOLED екранах.
5. **`CHALKBOARD`** — Стиль шкільної зеленої дошки (`#1B3A2F`) з жовтими та крейдяними акцентами (`#FACC15`).
6. **`SEPIA_EINK`** — Сепія-режим для читання/малювання, що імітує електронні чорнила (`#EDE4D3`, `#4A3F35`).
7. **`MIDNIGHT_INDIGO`** — Глибокий нічний синій/індиго (`#0D1020`, `#151A32`, `#AFC6FF`).
8. **`FOREST_STUDY`** — Природний лісовий зелений стиль (`#F5F8F1`, `#166534`).
9. **`ROSE_QUARTZ`** — Пастельний кварцовий/рожевий стиль (`#FFF7FA`, `#9D174D`).
10. **`HIGH_CONTRAST`** — Спеціальний режим високої контрастності для Accessibility (WCAG AAA, чисто чорно-білий/жовтий).

### Палітри (`ColorPalette.kt`):
- **HSLA Color Model**: `HslaColor(hue: 0..360, saturation: 0..1, lightness: 0..1, alpha: 0..1)` з безшовною конвертацією в Compose `Color` та ARGB Int.
- **Готові палітри**: Classic, Pastel, Neon, Academic, Vintage, Manga, Architectural.

---

## 3. ІНСТРУМЕНТИ ТА ПЕНЗЛІ

### Інструменти (`ToolType`):
- **`PEN`** — Стандартне гладке перо (з регулюванням згладжування Catmull-Rom).
- **`PENCIL`** — Олівець з текстурою зернистості (jitter), реагує на нахил (tilt) та тиск.
- **`INK_PEN`** — Чорнильне перо з ефектом накопичення фарби (flow = 0.8).
- **`FOUNTAIN_PEN`** — Каліграфічне перо з кутовим зрізом.
- **`MARKER`** — Напівпрозорий маркер/хайлайтер (квадратний/скошений кінець, фіксований alpha = 0.38).
- **`AIRBRUSH`** / **`WATERCOLOR_BRUSH`** / **`CRAYON`** — Художні пензлі з розкидом часток.
- **`LASER`** — Лазерна указка (анімований штрих, що зникає з часом).
- **`POINTER`** — Вказівник для презентацій та співпраці.
- **`SELECTOR`** (Lasso / Box) — Виділення елементів із рамкою трансформації (переміщення, масштабування, поворот, копіювання/вставка).
- **`ERASER`** (Object / Pixel) — Векторний розумний ластик:
  * *Object Eraser*: видалення всього штриха дотиком.
  * *Pixel Eraser*: обрізання сплайнів по перетину кола з RDP-спрощенням без затримок.
- **`FILL`** — Заливка замкнених областей.
- **`EYEDROPPER`** — Піпетка для зчитування кольору з полотна в HSLA/HEX.
- **`RULER`** (Ruler, Protractor, Compass) — Інтерактивна лінійка, транспортир, циркуль з магнітним прилипанням штриха до краю.
- **`TEXT`** — Текстові блоки з підтримкою форматування, шрифтів, кольорів, вирівнювання.
- **`PIXEL`** — Піксель-арт режим з сіткою та точним піксельним розташуванням.

### Геометричні фігури (`ShapeType`):
Circle, Square/Rectangle, Triangle, Arrow, Bold Arrow, Star (5-point), Hexagon, Pentagon, Cloud, Speech Bubble.

---

## 4. ФУНКЦІОНАЛЬНІ МОЖЛИВОСТІ

1. **Багатошаровість (Layer System):**
   - Необмежена кількість шарів (`LayerEntity`).
   - Керування прозорістю (opacity), видимістю (visibility), блокуванням (lock).
   - Підтримка режимів змішування (`BlendMode.NORMAL`, `MULTIPLY`, `SCREEN`, `OVERLAY`).
2. **Undo / Redo (Command Pattern + Snapshot Fallback):**
   - Черга команд `CanvasCommand` (AddStroke, Erase, MoveElements, Insert, Delete, LayerChange).
   - Стек глибиною 100 кроків із захистом від race condition через `Mutex`.
3. **Експорт:**
   - **Raster**: PNG (з OOM-захистом через `safeScale`, обрізкою `cropRect`, підтримкою до 4096px).
   - **Vector**: SVG (чисті векторні path, rect, ellipse, text, кодові блоки), PDF (посторінковий векторний через `PdfDocument`).
   - **Obsidian Vault**: Експорт нотаток з frontmatter метаданими + Markdown посиланнями `![[attachment]]` + AI-підсумками.
4. **Академічні та STEM інструменти:**
   - **Graph Plotter**: побудова 2D математичних графіків ($y = \sin(x)$, $y = x^2 - 4x + 3$).
   - **LaTeX Converter & Autocomplete**: рукописне введення формул → чистий LaTeX.
   - **Code Lab**: запуск та аналіз блоків коду (Python, C, C++) безпосередньо на полотні з виводом консолі.
   - **Spaced Repetition / Study Decks**: генерація flashcards та алгоритм інтервального повторення.
5. **Аудіо-синхронізація (Smart Lecture Recorder):**
   - Запис аудіо під час малювання.
   - Збереження timestamp кожного штриха (`StrokePoint.timestampMs`) та прив'язка до таймкоду аудіодоріжки.
   - Клік на штрих переміщує аудіоплеєр на момент його створення.
6. **Штучний інтелект (AI Multimodal):**
   - Провайдери: Gemini 1.5/2.0 Flash & Pro, OpenAI GPT-4o, Claude 3.5, DeepSeek R1, Ollama.
   - Плаваюче вікно асистента + повноцінний BottomSheet чат.
   - Multimodal Vision: передача знімка виділеної області або всього полотна для розв'язання задач, перевірки коду, генерації конспектів.

---

## 5. ТАБЛИЦЯ ВІДПОВІДНОСТЕЙ (FEATURE → FILE → STATUS)

| Фіча / Модуль | Основний файл(и) | Стан в Android | Готовність до KMP / Windows |
| :--- | :--- | :---: | :---: |
| **Theme Engine** | `ui/theme/ThemeSpec.kt`, `ThemedPanel.kt` | ✅ Production | 🟢 100% Compose Multiplatform сумісний |
| **Brush Engine & Presets** | `brush/BrushEngine.kt`, `BrushPresets.kt` | ✅ Production | 🟢 Чистий Kotlin, спільний для shared |
| **Drawing Engine & Splines** | `core/drawing/DrawingState.kt`, `PathSmoothing.kt` | ✅ Production | 🟢 Чиста геометрія (потребує Skia Path на Desktop) |
| **Vector Shape Recognition** | `brush/recognizer/VectorShapeRecognizer.kt` | ✅ Production | 🟢 Чистий Kotlin алгоритм |
| **Layers & Compositing** | `core/render/LayerCompositor.kt`, `CanvasModels.kt` | ✅ Production | 🟢 Спільні моделі, Compose Canvas blend |
| **Ruler / Protractor / Compass**| `ui/components/*OverlayComponent.kt` | ✅ Production | 🟢 Compose Multiplatform ready |
| **Lasso Selection & Transform** | `ui/components/LassoSelectionOverlay.kt` | ✅ Production | 🟢 Чиста векторна математика |
| **Undo/Redo System** | `data/models/CommandModels.kt`, `CanvasEditorViewModel.kt`| ✅ Production | 🟢 Чистий Kotlin MVI / Command Pattern |
| **Raster Export (PNG/JPEG)** | `drive/ExportManager.kt` | ⚠️ Android Canvas | 🟡 Потрібен Skia Image / ImageIO на Desktop |
| **Vector Export (SVG/PDF)** | `drive/ExportManager.kt` | ⚠️ Android PdfDoc | 🟡 SVG спільний; PDF через Skia/iText на Desktop |
| **Obsidian Vault Sync** | `drive/ExportManager.kt` | ✅ Production (SAF) | 🟡 На Windows — прямий доступ до файлової системи |
| **Code Lab / Runner** | `academic/code/LocalCodeAnalyzer.kt` | ✅ Production | 🟢 На Windows можна додати виконання через ProcessBuilder |
| **Math Function Plotter** | `academic/FunctionPlotterEngine.kt` | ✅ Production | 🟢 Чистий Kotlin алгоритм |
| **LaTeX OCR & Completion** | `academic/HandwritingLatexConverter.kt` | ✅ Production | 🟢 REST / KMP ready |
| **Audio Lecture Sync** | `audio/AudioRecorderManager.kt` | ⚠️ Android Media | 🟡 На Windows потрібен Java Sound / FFmpeg / PortAudio |
| **AI Assistant (Gemini/OpenAI)** | `ai/GeminiAssistantService.kt`, `AiProvider.kt` | ⚠️ EncryptedSharedPrefs | 🟡 На Windows DPAPI/Ktor Client замість Android Crypto |
| **Database & Persistence** | `data/db/AppDatabase.kt` | ⚠️ Room Android | 🟡 Room KMP або SQLDelight у спільний модуль |
| **Preferences DataStore** | `data/repository/UserPreferencesRepository.kt` | ✅ DataStore KMP | 🟢 DataStore підтримує KMP Desktop |

---

## 6. АНАЛІЗ БАГІВ, ДЕПРЕКАЦІЙ ТА RACE CONDITIONS

1. **Android Cryptography Deprecations в AI модулі:**
   - `MasterKeys.getOrCreate()` та `EncryptedSharedPreferences.create()` застаріли в AndroidX Security.
   - *Виправлення:* Використовувати `MasterKey.Builder` на Android, а для Windows — Windows Data Protection API (DPAPI) або AES-256 GCM з локальним сховищем.
2. **Room Database Destructive Migration:**
   - `AppDatabase.kt:168`: `fallbackToDestructiveMigration()` без параметрів застарів.
   - *Виправлення:* Явні міграції схем для безпеки користувацьких даних при оновленні.
3. **Icons AutoMirrored Deprecations:**
   - У кількох компонентах (`HomeScreen.kt`, `TopFloatingToolbar.kt`, `TextInputDialog.kt`, `StudyDeckDialog.kt`, `RulerOverlayComponent.kt`, `CanvasEditorScreen.kt`) використовуються застарілі `Icons.Filled.ArrowBack`, `Undo`, `Redo`, `Backspace`, `ShowChart`.
   - *Виправлення:* Перевести на `Icons.AutoMirrored.Filled.*`.
4. **Android-Specific залежності в Core:**
   - `ExportManager.kt`, `BitmapLoader.kt` та `CanvasEditorViewModel.kt` напряму посилаються на `android.graphics.Bitmap`, `android.graphics.Canvas`, `android.graphics.Path`, `android.net.Uri`, `android.content.Context`.
   - *Виправлення при міграції на KMP:* Ізолювати платформо-незалежні інтерфейси `PlatformBitmap`, `PlatformCanvas`, `PlatformFileSystem` через `expect/actual` або Skia (`org.jetbrains.skia.*`).

---

## 7. РЕЗУЛЬТАТИ ТЕСТУВАННЯ ТА МАТРИЦЯ ПОКРИТТЯ

### Результати прогону `./gradlew testDebugUnitTest`:
| Тестовий набір (Test Suite) | Кількість тестів | Помилки | Збої | Пропущено | Час (с) |
| :--- | :---: | :---: | :---: | :---: | :---: |
| `LocalCodeAnalyzerTest` | 6 | 0 | 0 | 0 | 0.014 |
| `MathExpressionEvaluatorTest` | 4 | 0 | 0 | 0 | 0.003 |
| `SpacedRepetitionSchedulerTest` | 7 | 0 | 0 | 0 | 0.002 |
| `VectorShapeRecognizerTest` | 2 | 0 | 0 | 0 | 0.003 |
| `DrawingEngineEraserTest` | 4 | 0 | 0 | 0 | 0.005 |
| `PathSmoothingTest` | 2 | 0 | 0 | 0 | 0.053 |
| `CanvasReferenceModelsTest` | 4 | 0 | 0 | 0 | 0.006 |
| `CanvasNamingTest` | 2 | 0 | 0 | 0 | 0.001 |
| `CanvasReferenceRepositoryTest` | 1 | 0 | 0 | 0 | 0.389 |
| `ExampleRobolectricTest` | 1 | 0 | 0 | 0 | 5.449 |
| `ExampleUnitTest` | 1 | 0 | 0 | 0 | 0.001 |
| `GreetingScreenshotTest` (Roborazzi) | 1 | 0 | 0 | 0 | 4.994 |
| `CanvasInteractionMathTest` | 4 | 0 | 0 | 0 | 0.007 |
| `LassoSelectionTest` | 3 | 0 | 0 | 0 | 0.002 |
| `SelectionClipboardTest` | 3 | 0 | 0 | 0 | 0.003 |
| **РАЗОМ** | **40** | **0** | **0** | **0** | **10.93s** |

**Статус тестів:** 🟢 **100% PASS** (40/40 тестів успішні).

---

## 8. АРХІТЕКТУРНИЙ ПЛАН ПОРТУВАННЯ НА KMP + WINDOWS

### Модульна структура KMP:
1. **`:shared` (Kotlin Multiplatform Module):**
   - **Domain & State**: Всі моделі (`CanvasModels`, `HslaColor`, `ToolType`, `BrushProfile`, `PageEntity`, `LayerEntity`), MVI ViewModels, Command Pattern.
   - **Drawing Engine**: Математика сплайнів, Catmull-Rom, RDP, алгоритми ластика, лінійки, розпізнавання фігур.
   - **Compose UI**: Всі UI-компоненти, теми (`ThemeSpec`, `ThemedPanel`), тулбар, палітри, діалоги, `InteractiveCanvas`.
   - **Cross-Platform Storage**: DataStore Preferences, Ktor HTTP Client (AI / Sync), Koin DI.
   - **Expect/Actual Interfaces**:
     * `PlatformCanvasRenderer` (Android Compose Canvas vs Desktop Skia Canvas).
     * `PlatformAudioEngine` (Android MediaRecorder vs Desktop JavaSound/PortAudio).
     * `PlatformSecurity` (Android KeyStore/MasterKey vs Windows DPAPI/Credential Vault).
     * `PlatformExport` (Android PDF/PNG vs Desktop Skia/ImageIO/iText).
2. **`:androidApp` (Android Target):**
   - Специфічний код Android (Activities, Tablet Canvas-Only Mode, USB ADB Reverse listener, Bluetooth SPP).
3. **`:desktopApp` (Windows Desktop Target):**
   - Compose for Desktop (Skiko/Skia).
   - Windows Native Ink & Pen Events (Wacom/Surface Pen pressure & tilt).
   - Global Hotkeys, Native Titlebar, Jump List, File Associations (`.sketchpad`, `.svg`, `.png`).
   - WebSocket Server (порт 8765) для протоколу **SketchLink** (стрімінг з планшета).
   - jpackage конфігурація для генерації `Sketchpad-Windows.exe` з вбудованим JBR (zero-dependency).

---

## 9. ВІДПОВІДІ НА ПОПЕРЕДНІ ЗАПИТАННЯ ТА СЕРЕДОВИЩЕ
- **JDK:** Встановлено **OpenJDK 21.0.11 LTS** (Temurin 64-Bit) з нативним **`jpackage.exe`** (`C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot\bin\jpackage.exe`).
- **Android SDK:** Встановлено (`C:\Users\ItzZ0nk\AppData\Local\Android\Sdk`), доступні `adb.exe`, compileSdk 36, targetSdk 36.
- **Gradle & Kotlin:** Gradle 9.x, Kotlin 2.2.10, Compose Multiplatform сумісні.
- **CI/CD:** Підготуємо конфігурацію GitHub Actions у `.github/workflows/build.yml` для автоматичного складання APK та Windows EXE/ZIP.
