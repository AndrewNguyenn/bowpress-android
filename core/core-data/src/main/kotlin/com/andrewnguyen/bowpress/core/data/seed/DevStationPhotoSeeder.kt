package com.andrewnguyen.bowpress.core.data.seed

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import com.andrewnguyen.bowpress.core.designsystem.coursemap.CourseStationPhotoStore
import com.andrewnguyen.bowpress.core.model.CourseStation
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * DEBUG-only seeder that fills [CourseStationPhotoStore] with synthetic
 * photos for the mock 3D-course stations.
 *
 * The mock 3D courses in [DevMockData] flag each station with
 * `hasScenePhoto` / `hasArrowPhoto`, but the photo store is file-backed and
 * is only written when a real capture happens — so on a fresh emulator the
 * 3D-course station bottom sheet (`CourseStationSheet` Full tier) would show
 * empty photo tiles. This seeder renders stand-in JPEGs with
 * [android.graphics.Bitmap] + [Canvas] so the sheet shows real images.
 *
 * Mirrors the iOS `ThreeDSyntheticImage` + `MainTabView` 3D-seed loop.
 *
 * Idempotent: a slot is skipped if [CourseStationPhotoStore.hasPhoto] is
 * already true. The photo files persist on disk, so seeding once per install
 * is sufficient; a fresh install re-seeds because the files are gone.
 */
@Singleton
class DevStationPhotoSeeder @Inject constructor() {

    /**
     * For every mock 3D-course [station], render + persist a SCENE photo
     * (and an ARROW photo where flagged) into [CourseStationPhotoStore].
     */
    fun seed(context: Context, stations: List<CourseStation>) {
        stations.forEach { station ->
            if (station.hasScenePhoto &&
                !CourseStationPhotoStore.hasPhoto(context, station.id, CourseStationPhotoStore.Slot.SCENE)
            ) {
                renderScene().jpegBytes()?.let { bytes ->
                    CourseStationPhotoStore.save(
                        context, bytes, station.id, CourseStationPhotoStore.Slot.SCENE,
                    )
                }
            }
            if (station.hasArrowPhoto &&
                !CourseStationPhotoStore.hasPhoto(context, station.id, CourseStationPhotoStore.Slot.ARROW)
            ) {
                renderArrow().jpegBytes()?.let { bytes ->
                    CourseStationPhotoStore.save(
                        context, bytes, station.id, CourseStationPhotoStore.Slot.ARROW,
                    )
                }
            }
        }
    }

    private fun Bitmap.jpegBytes(): ByteArray? =
        runCatching {
            ByteArrayOutputStream().use { out ->
                compress(Bitmap.CompressFormat.JPEG, 75, out)
                out.toByteArray()
            }
        }.getOrNull().also { recycle() }

    /**
     * A stylized "scene through the trees" frame — the editorial stand-in
     * for a camera capture of the foam target down-range. Mirrors iOS
     * `ThreeDSyntheticImage.scene()`.
     */
    private fun renderScene(width: Int = 360, height: Int = 480): Bitmap {
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Forest gradient sky-to-floor.
        paint.shader = LinearGradient(
            0f, 0f, 0f, height.toFloat(),
            intArrayOf(0xFF3C4A40.toInt(), 0xFF566A55.toInt(), 0xFF404A3B.toInt()),
            floatArrayOf(0f, 0.55f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        paint.shader = null

        // Tree silhouettes.
        paint.color = 0x8C1F2A26.toInt()
        for ((fx, fy) in listOf(0.13f to 0.58f, 0.27f to 0.66f, 0.78f to 0.57f, 0.9f to 0.64f)) {
            val x = fx * width
            val y = fy * height
            val tree = Path().apply {
                moveTo(x, y)
                lineTo(x - width * 0.05f, y + height * 0.10f)
                lineTo(x + width * 0.05f, y + height * 0.10f)
                close()
            }
            canvas.drawPath(tree, paint)
        }

        // The foam target down-range.
        paint.color = 0xFFA89570.toInt()
        canvas.drawRect(
            width * 0.44f, height * 0.46f,
            width * 0.56f, height * 0.59f,
            paint,
        )
        return bmp
    }

    /**
     * A stylized "arrow in the target" close-up. Mirrors iOS
     * `ThreeDSyntheticImage.arrow()`.
     */
    private fun renderArrow(size: Int = 360): Bitmap {
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Foam backing.
        paint.color = 0xFFA89570.toInt()
        canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), paint)

        // Concentric scoring rings.
        val cx = size / 2f
        val cy = size / 2f
        for ((frac, hex) in listOf(0.40f to 0xFFC4A87C.toInt(), 0.27f to 0xFF9A8259.toInt(), 0.15f to 0xFF7A6644.toInt())) {
            paint.color = hex
            canvas.drawCircle(cx, cy, size * frac, paint)
        }

        // Arrow shaft.
        paint.color = 0xFF1F2A26.toInt()
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = size * 0.025f
        canvas.drawLine(cx - 8f, cy + 8f, size * 0.82f, size * 0.16f, paint)
        return bmp
    }
}
