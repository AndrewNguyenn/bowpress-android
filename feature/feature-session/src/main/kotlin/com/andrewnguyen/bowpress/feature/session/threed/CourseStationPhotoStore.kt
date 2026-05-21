package com.andrewnguyen.bowpress.feature.session.threed

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File

/**
 * Mirrors iOS `CourseStationPhotoStore`. On-disk store for the two photos a
 * 3D-course station can carry — the `scene` the archer composed, and an
 * optional `arrow` close-up. Local-only JPEGs keyed by station id + slot;
 * only the `hasScenePhoto` / `hasArrowPhoto` flags travel in the DTO.
 */
object CourseStationPhotoStore {

    enum class Slot(val key: String) { SCENE("scene"), ARROW("arrow") }

    private fun directory(context: Context): File =
        File(context.filesDir, "course_station_photos").apply { mkdirs() }

    private fun file(context: Context, stationId: String, slot: Slot): File =
        File(directory(context), "${stationId}_${slot.key}.jpg")

    fun hasPhoto(context: Context, stationId: String, slot: Slot): Boolean =
        file(context, stationId, slot).exists()

    /** Persist raw JPEG bytes for a station slot. Returns false on failure. */
    fun save(context: Context, data: ByteArray, stationId: String, slot: Slot): Boolean =
        runCatching { file(context, stationId, slot).writeBytes(data); true }.getOrDefault(false)

    fun load(context: Context, stationId: String, slot: Slot): Bitmap? {
        val f = file(context, stationId, slot)
        if (!f.exists()) return null
        return runCatching { BitmapFactory.decodeFile(f.absolutePath) }.getOrNull()
    }

    /** Remove both photo slots for a station — used by the delete path. */
    fun deleteAll(context: Context, stationId: String) {
        Slot.entries.forEach { file(context, stationId, it).delete() }
    }
}
