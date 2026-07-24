Начинаем пошаговое выполнение плана рефакторинга, устранения архитектурных ошибок и имплементации ключевых академических модулей для **Sketchpad App**.

---

## 🛠 ЭТАП 1: Практическое исправление 4 основных багов

### 1. Исправление кнопки «Добавить» (`core` / `ui`)

Проблема заключалась в мутации существующего экземпляра списка внутри `StateFlow`, из-за чего Jetpack Compose считал состояние неизменившимся.

#### `DrawingState.kt`

```kotlin
package com.example.sketchpad.core.state

import androidx.compose.runtime.Immutable
import java.util.UUID

@Immutable
data class DrawingState(
    val elements: List<CanvasElement> = emptyList(),
    val selectedElementId: String? = null,
    val viewport: Viewport = Viewport(),
    val isRulerActive: Boolean = false
)

sealed interface CanvasElement {
    val id: String
    val zIndex: Int
    val isVisible: Boolean
}

data class ShapeElement(
    override val id: String = UUID.randomUUID().toString(),
    override val zIndex: Int = 0,
    override val isVisible: Boolean = true,
    val type: ShapeType,
    val bounds: CanvasRect
) : CanvasElement

enum class ShapeType { RECTANGLE, CIRCLE, TRIANGLE, ARROW }
data class CanvasRect(val left: Float, val top: Float, val right: Float, val bottom: Float)
data class Viewport(val width: Float = 1080f, val height: Float = 1920f, val scale: Float = 1.0f)

```

#### `CanvasEditorViewModel.kt`

```kotlin
package com.example.sketchpad.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.example.sketchpad.core.state.CanvasElement
import com.example.sketchpad.core.state.CanvasRect
import com.example.sketchpad.core.state.DrawingState
import com.example.sketchpad.core.state.ShapeElement
import com.example.sketchpad.core.state.ShapeType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class CanvasEditorViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(DrawingState())
    val uiState: StateFlow<DrawingState> = _uiState.asStateFlow()

    fun addShape(type: ShapeType) {
        _uiState.update { currentState ->
            val defaultSize = 200f
            val centerX = currentState.viewport.width / 2f
            val centerY = currentState.viewport.height / 2f

            val newShape = ShapeElement(
                type = type,
                zIndex = (currentState.elements.maxOfOrNull { it.zIndex } ?: 0) + 1,
                bounds = CanvasRect(
                    left = centerX - defaultSize / 2,
                    top = centerY - defaultSize / 2,
                    right = centerX + defaultSize / 2,
                    bottom = centerY + defaultSize / 2
                )
            )

            // Создание нового списка гарантирует смену ссылки и вызов Recomposition
            currentState.copy(
                elements = currentState.elements + newShape
            )
        }
    }
}

```

---

### 2. Исправление отображения фото и управление Z-Index (`ui` / `canvas`)

Создаем кеширующий загрузчик растра и рендерер с правильной сортировкой слоев.

#### `BitmapLoader.kt`

```kotlin
package com.example.sketchpad.ui.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BitmapLoader(private val context: Context) {
    private val memoryCache: LruCache<String, Bitmap>

    init {
        val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
        val cacheSize = maxMemory / 8
        memoryCache = object : LruCache<String, Bitmap>(cacheSize) {
            override fun sizeOf(key: String, bitmap: Bitmap): Int {
                return bitmap.byteCount / 1024
            }
        }
    }

    suspend fun loadBitmap(uriString: String, maxDimension: Int = 2048): Bitmap? = withContext(Dispatchers.IO) {
        memoryCache.get(uriString)?.let { return@withContext it }

        runCatching {
            val uri = Uri.parse(uriString)
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeStream(stream, null, options)

                options.inSampleSize = calculateInSampleSize(options, maxDimension, maxDimension)
                options.inJustDecodeBounds = false

                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    BitmapFactory.decodeStream(inputStream, null, options)?.also { loadedBitmap ->
                        memoryCache.put(uriString, loadedBitmap)
                    }
                }
            }
        }.getOrNull()
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height: Int, width: Int) = options.run { outHeight to outWidth }
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }
}

```

#### `CanvasRenderer.kt`

```kotlin
package com.example.sketchpad.ui.canvas

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.platform.LocalContext
import com.example.sketchpad.core.state.CanvasElement
import com.example.sketchpad.core.state.DrawingState
import com.example.sketchpad.ui.util.BitmapLoader

@Composable
fun MainCanvasRenderer(
    state: DrawingState,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val bitmapLoader = remember { BitmapLoader(context) }
    val loadedBitmaps = remember { mutableStateMapOf<String, ImageBitmap>() }

    // Подгрузка растровых изображений для UI
    LaunchedEffect(state.elements) {
        state.elements.filterIsInstance<ImageElement>().forEach { element ->
            if (!loadedBitmaps.containsKey(element.id)) {
                val bitmap = bitmapLoader.loadBitmap(element.imageUri)
                bitmap?.let { loadedBitmaps[element.id] = it.asImageBitmap() }
            }
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        // Отрисовка с обязательной сортировкой по Z-Index
        val sortedElements = state.elements.filter { it.isVisible }.sortedBy { it.zIndex }

        sortedElements.forEach { element ->
            when (element) {
                is ImageElement -> {
                    loadedBitmaps[element.id]?.let { imgBitmap ->
                        drawImage(
                            image = imgBitmap,
                            dstOffset = androidx.compose.ui.unit.IntOffset(element.bounds.left.toInt(), element.bounds.top.toInt()),
                            dstSize = androidx.compose.ui.unit.IntSize(
                                (element.bounds.right - element.bounds.left).toInt(),
                                (element.bounds.bottom - element.bounds.top).toInt()
                            )
                        )
                    }
                }
                is ShapeElement -> drawShape(element)
                is ChartElement -> drawChart(element)
            }
        }
    }
}

```

---

### 3. Исправление отрисовки графиков (`academic` / `ui`)

```kotlin
package com.example.sketchpad.academic.chart

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.sketchpad.core.state.CanvasElement
import com.example.sketchpad.core.state.CanvasRect

data class ChartElement(
    override val id: String,
    override val zIndex: Int,
    override val isVisible: Boolean = true,
    val points: List<Pair<Float, Float>>,
    val bounds: CanvasRect,
    val strokeColor: Color = Color.Blue
) : CanvasElement

fun DrawScope.drawChart(chart: ChartElement) {
    if (chart.points.isEmpty() || !chart.isVisible) return

    val width = chart.bounds.right - chart.bounds.left
    val height = chart.bounds.bottom - chart.bounds.top

    // Безопасная проверка: защита от деления на 0
    if (width <= 0f || height <= 0f) return

    val minX = chart.points.minOf { it.first }
    val maxX = chart.points.maxOf { it.first }
    val minY = chart.points.minOf { it.second }
    val maxY = chart.points.maxOf { it.second }

    val rangeX = if (maxX - minX == 0f) 1f else maxX - minX
    val rangeY = if (maxY - minY == 0f) 1f else maxY - minY

    val path = Path()
    chart.points.forEachIndexed { index, point ->
        val x = chart.bounds.left + ((point.first - minX) / rangeX) * width
        val y = chart.bounds.bottom - ((point.second - minY) / rangeY) * height

        if (index == 0) {
            path.moveTo(x, y)
        } else {
            path.lineTo(x, y)
        }
    }

    drawPath(
        path = path,
        color = chart.strokeColor,
        style = Stroke(width = 4f)
    )
}

```

---

### 4. Исправление конфликта жестов линейки и Canvas (`ui` / `gestures`)

```kotlin
package com.example.sketchpad.ui.gestures

import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.geometry.Offset

fun Modifier.rulerGestureDetector(
    isRulerActive: Boolean,
    rulerBounds: androidx.compose.ui.geometry.Rect,
    onRulerMove: (delta: Offset) -> Unit
): Modifier = if (!isRulerActive) this else this.pointerInput(rulerBounds) {
    awaitPointerEventScope {
        while (true) {
            // Использование PointerEventPass.Initial позволяет обработать событие ДО перехвата холстом
            val event = awaitPointerEvent(pass = PointerEventPass.Initial)
            val changes = event.changes

            if (changes.any { rulerBounds.contains(it.position) }) {
                val dragDelta = changes.first().position - changes.first().previousPosition
                if (dragDelta != Offset.Zero) {
                    onRulerMove(dragDelta)
                    changes.forEach { it.consume() } // Поглощаем событие для холста
                }
            }
        }
    }
}

```

---

## 🛡 ЭТАП 2: Архитектурная защита данных и безопасность

### 1. Атомарное сохранение на диск (`data` модуль)

```kotlin
package com.example.sketchpad.data.storage

import android.util.AtomicFile
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AtomicCanvasStorage(private val targetFile: File) {

    private val atomicFile = AtomicFile(targetFile)

    suspend fun saveCanvasData(data: ByteArray): Boolean = withContext(Dispatchers.IO) {
        var stream: FileOutputStream? = null
        try {
            stream = atomicFile.startWrite()
            stream.write(data)
            atomicFile.finishWrite(stream)
            true
        } catch (e: Exception) {
            stream?.let { atomicFile.failWrite(it) }
            false
        }
    }

    suspend fun readCanvasData(): ByteArray? = withContext(Dispatchers.IO) {
        runCatching {
            atomicFile.readFully()
        }.getOrNull()
    }
}

```

---

### 2. Конфигурация манифеста (`AndroidManifest.xml`)

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <!-- Разрешения для работы с медиафайлами в Android 13+ (Scoped Storage) -->
    <uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />
    <uses-permission android:name="android.permission.RECORD_AUDIO" />
    <uses-permission android:name="android.permission.INTERNET" />

    <application
        android:allowBackup="false"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:supportsRtl="true"
        android:usesCleartextTraffic="false"
        android:theme="@style/Theme.Sketchpad">

        <activity
            android:name=".ui.MainActivity"
            android:exported="true"
            android:configChanges="orientation|screenSize|keyboardHidden">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

    </application>
</manifest>

```

---

## 🚀 ЭТАП 3: Имплементация "Чит-фич" (Domination Pack)

### 1. Shape Perfecter (`brush` / `core`)

Алгоритм упрощения линий Рамера-Дугласа-Пекера и геометрического анализа для автоматического превращения кривой в ровный прямоугольник или круг.

```kotlin
package com.example.sketchpad.brush.recognizer

import androidx.compose.ui.geometry.Offset
import kotlin.math.hypot

data class Point(val x: Float, val y: Float)

object VectorShapeRecognizer {

    fun recognizeShape(points: List<Offset>): RecognizedShape? {
        if (points.size < 5) return null

        val simplified = ramerDouglasPeucker(points, epsilon = 10f)
        val isClosed = hypot(
            (points.first().x - points.last().x).toDouble(),
            (points.first().y - points.last().y).toDouble()
        ) < 50.0

        return when {
            isClosed && simplified.size in 4..6 -> RecognizedShape.RECTANGLE
            isClosed && simplified.size > 8 -> RecognizedShape.CIRCLE
            isClosed && simplified.size == 4 -> RecognizedShape.TRIANGLE
            !isClosed && simplified.size == 2 -> RecognizedShape.LINE
            else -> null
        }
    }

    private fun ramerDouglasPeucker(points: List<Offset>, epsilon: Float): List<Offset> {
        if (points.size < 3) return points

        var dmax = 0f
        var index = 0
        val end = points.size - 1

        for (i in 1 until end) {
            val d = perpendicularDistance(points[i], points[0], points[end])
            if (d > dmax) {
                index = i
                dmax = d
            }
        }

        return if (dmax > epsilon) {
            val recResults1 = ramerDouglasPeucker(points.subList(0, index + 1), epsilon)
            val recResults2 = ramerDouglasPeucker(points.subList(index, points.size), epsilon)
            recResults1.dropLast(1) + recResults2
        } else {
            listOf(points[0], points[end])
        }
    }

    private fun perpendicularDistance(point: Offset, lineStart: Offset, lineEnd: Offset): Float {
        val dx = lineEnd.x - lineStart.x
        val dy = lineEnd.y - lineStart.y
        val mag = hypot(dx.toDouble(), dy.toDouble()).toFloat()
        if (mag == 0f) return hypot((point.x - lineStart.x).toDouble(), (point.y - lineStart.y).toDouble()).toFloat()
        val p = ((point.x - lineStart.x) * dx + (point.y - lineStart.y) * dy) / (mag * mag)
        return if (p < 0) {
            hypot((point.x - lineStart.x).toDouble(), (point.y - lineStart.y).toDouble()).toFloat()
        } else if (p > 1) {
            hypot((point.x - lineEnd.x).toDouble(), (point.y - lineEnd.y).toDouble()).toFloat()
        } else {
            val projX = lineStart.x + p * dx
            val projY = lineStart.y + p * dy
            hypot((point.x - projX).toDouble(), (point.y - projY).toDouble()).toFloat()
        }
    }
}

enum class RecognizedShape { RECTANGLE, CIRCLE, TRIANGLE, LINE }

```

---

### 2. AI LaTeX Auto-Complete (`ai` / `academic`)

Модуль связи с Gemini API для превращения вырезанного участка холста в формулу LaTeX.

```kotlin
package com.example.sketchpad.ai.latex

import android.graphics.Bitmap
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LatexAutoCompleter(private val apiKey: String) {

    private val generativeModel by lazy {
        GenerativeModel(
            modelName = "gemini-1.5-flash",
            apiKey = apiKey
        )
    }

    suspend fun convertBitmapToLatex(croppedFormula: Bitmap): String? = withContext(Dispatchers.IO) {
        runCatching {
            val prompt = content {
                image(croppedFormula)
                text("Recognize the handwritten mathematical formula in this image and return strictly the raw LaTeX string. Do not include markdown formatting or extra text.")
            }
            val response = generativeModel.generateContent(prompt)
            response.text?.trim()?.removePrefix("```latex")?.removePrefix("```")?.removeSuffix("```")?.trim()
        }.getOrNull()
    }
}

```

---

## 🧪 ЭТАП 4: Модульное тестирование (Unit Testing)

#### `VectorShapeRecognizerTest.kt`

```kotlin
package com.example.sketchpad.brush.recognizer

import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertEquals
import org.junit.Test

class VectorShapeRecognizerTest {

    @Test
    fun testRecognizeRectangle() {
        // Имитация замкнутого прямоугольного контура
        val points = listOf(
            Offset(0f, 0f),
            Offset(100f, 0f),
            Offset(100f, 100f),
            Offset(0f, 100f),
            Offset(0f, 0f)
        )

        val result = VectorShapeRecognizer.recognizeShape(points)
        assertEquals(RecognizedShape.RECTANGLE, result)
    }

    @Test
    fun testRecognizeLine() {
        // Имитация прямой линии
        val points = listOf(
            Offset(0f, 0f),
            Offset(25f, 25f),
            Offset(50f, 50f),
            Offset(100f, 100f)
        )

        val result = VectorShapeRecognizer.recognizeShape(points)
        assertEquals(RecognizedShape.LINE, result)
    }
}

```

---

## 📊 Итог выполненной работы

1. **Рефакторинг состояний:** Внедрено создание иммутабельных коллекций в `CanvasEditorViewModel` для устранения сбоев Recomposition.
2. **Оптимизация слоев Canvas:** Настроен LRU-кеш для растра и явная сортировка по `zIndex` перед вызовом `DrawScope`.
3. **Безопасность расчетов:** Добавлены проверки на нулевые размеры во Viewport для графиков и исключено деление на ноль.
4. **Слои жестов:** Добавлена фаза `PointerEventPass.Initial` для изолирования управления линейкой.
5. **Интеграция фич:** Имплементированы модули распознавания геометрических форм и генерации LaTeX через Gemini API.