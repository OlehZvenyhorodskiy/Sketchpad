package com.example.data.db

import androidx.room.TypeConverter
import com.example.data.models.ChartElementEntity
import com.example.data.models.ImageElementEntity
import com.example.data.models.LayerEntity
import com.example.data.models.ShapeEntity
import com.example.data.models.StrokeEntity
import com.example.data.models.SyncMarker
import com.example.data.models.TextBlockEntity
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

class MoshiConverters {
    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val strokeListType = Types.newParameterizedType(List::class.java, StrokeEntity::class.java)
    private val strokeAdapter = moshi.adapter<List<StrokeEntity>>(strokeListType)

    private val shapeListType = Types.newParameterizedType(List::class.java, ShapeEntity::class.java)
    private val shapeAdapter = moshi.adapter<List<ShapeEntity>>(shapeListType)

    private val textBlockListType = Types.newParameterizedType(List::class.java, TextBlockEntity::class.java)
    private val textBlockAdapter = moshi.adapter<List<TextBlockEntity>>(textBlockListType)

    private val imageListType = Types.newParameterizedType(List::class.java, ImageElementEntity::class.java)
    private val imageAdapter = moshi.adapter<List<ImageElementEntity>>(imageListType)

    private val chartListType = Types.newParameterizedType(List::class.java, ChartElementEntity::class.java)
    private val chartAdapter = moshi.adapter<List<ChartElementEntity>>(chartListType)

    private val syncMarkerListType = Types.newParameterizedType(List::class.java, SyncMarker::class.java)
    private val syncMarkerAdapter = moshi.adapter<List<SyncMarker>>(syncMarkerListType)

    @TypeConverter
    fun strokeListToString(list: List<StrokeEntity>?): String {
        return strokeAdapter.toJson(list ?: emptyList())
    }

    @TypeConverter
    fun stringToStrokeList(json: String?): List<StrokeEntity> {
        if (json.isNullOrEmpty()) return emptyList()
        return try {
            strokeAdapter.fromJson(json) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    @TypeConverter
    fun shapeListToString(list: List<ShapeEntity>?): String {
        return shapeAdapter.toJson(list ?: emptyList())
    }

    @TypeConverter
    fun stringToShapeList(json: String?): List<ShapeEntity> {
        if (json.isNullOrEmpty()) return emptyList()
        return try {
            shapeAdapter.fromJson(json) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    @TypeConverter
    fun textBlockListToString(list: List<TextBlockEntity>?): String {
        return textBlockAdapter.toJson(list ?: emptyList())
    }

    @TypeConverter
    fun stringToTextBlockList(json: String?): List<TextBlockEntity> {
        if (json.isNullOrEmpty()) return emptyList()
        return try {
            textBlockAdapter.fromJson(json) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    @TypeConverter
    fun imageListToString(list: List<ImageElementEntity>?): String {
        return imageAdapter.toJson(list ?: emptyList())
    }

    @TypeConverter
    fun stringToImageList(json: String?): List<ImageElementEntity> {
        if (json.isNullOrEmpty()) return emptyList()
        return try {
            imageAdapter.fromJson(json) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    @TypeConverter
    fun chartListToString(list: List<ChartElementEntity>?): String {
        return chartAdapter.toJson(list ?: emptyList())
    }

    @TypeConverter
    fun stringToChartList(json: String?): List<ChartElementEntity> {
        if (json.isNullOrEmpty()) return emptyList()
        return try {
            chartAdapter.fromJson(json) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    @TypeConverter
    fun syncMarkerListToString(list: List<SyncMarker>?): String {
        return syncMarkerAdapter.toJson(list ?: emptyList())
    }

    @TypeConverter
    fun stringToSyncMarkerList(json: String?): List<SyncMarker> {
        if (json.isNullOrEmpty()) return emptyList()
        return try {
            syncMarkerAdapter.fromJson(json) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    private val layerListType = Types.newParameterizedType(List::class.java, LayerEntity::class.java)
    private val layerAdapter = moshi.adapter<List<LayerEntity>>(layerListType)

    @TypeConverter
    fun layerListToString(list: List<LayerEntity>?): String {
        return layerAdapter.toJson(list ?: emptyList())
    }

    @TypeConverter
    fun stringToLayerList(json: String?): List<LayerEntity> {
        if (json.isNullOrEmpty()) return emptyList()
        return try {
            layerAdapter.fromJson(json) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}
