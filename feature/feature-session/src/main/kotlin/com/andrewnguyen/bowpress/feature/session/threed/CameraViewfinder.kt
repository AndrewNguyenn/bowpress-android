package com.andrewnguyen.bowpress.feature.session.threed

import android.content.Context
import android.content.pm.PackageManager
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Holds the CameraX `ImageCapture` use case for the 3D capture screen so the
 * shutter button can trigger a still without the screen knowing CameraX. A
 * [CameraViewfinder] binds the use case on enter and clears it on dispose.
 */
class BowPressCameraController {
    var imageCapture: ImageCapture? = null
        internal set

    /** Take a still — JPEG bytes, or null on any failure. */
    suspend fun capture(context: Context): ByteArray? {
        val ic = imageCapture ?: return null
        // suspendCancellableCoroutine (not plain suspendCoroutine) so a
        // cancelled scope unsuspends the caller immediately; there's no
        // invokeOnCancellation because ImageCapture.takePicture has no cancel
        // hook, and a late resume on a cancelled continuation is a safe no-op.
        return suspendCancellableCoroutine { cont ->
            ic.takePicture(
                ContextCompat.getMainExecutor(context),
                object : ImageCapture.OnImageCapturedCallback() {
                    override fun onCaptureSuccess(image: ImageProxy) {
                        // ImageCapture's default output format is JPEG, so the
                        // single plane holds the whole encoded image.
                        val buffer = image.planes[0].buffer
                        val bytes = ByteArray(buffer.remaining()).also { buffer.get(it) }
                        image.close()
                        cont.resume(bytes)
                    }

                    override fun onError(exception: ImageCaptureException) {
                        cont.resume(null)
                    }
                },
            )
        }
    }

    companion object {
        /** Whether this device has any camera at all. */
        fun deviceHasCamera(context: Context): Boolean =
            context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)
    }
}

/**
 * A live back-camera preview. Binds a CameraX `Preview` + `ImageCapture` to
 * the composition's lifecycle on enter and unbinds exactly those use cases on
 * dispose — no global `unbindAll`, so it never disturbs another camera user.
 */
@Composable
fun CameraViewfinder(
    controller: BowPressCameraController,
    modifier: Modifier = Modifier,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val previewView = remember {
        PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER }
    }

    DisposableEffect(lifecycleOwner, previewView) {
        val providerFuture = ProcessCameraProvider.getInstance(context)
        var boundProvider: ProcessCameraProvider? = null
        var boundPreview: Preview? = null
        var boundCapture: ImageCapture? = null

        providerFuture.addListener({
            val provider = runCatching { providerFuture.get() }.getOrNull()
                ?: return@addListener
            val preview = Preview.Builder().build().apply {
                setSurfaceProvider(previewView.surfaceProvider)
            }
            val imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()
            runCatching {
                provider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageCapture,
                )
                boundProvider = provider
                boundPreview = preview
                boundCapture = imageCapture
                controller.imageCapture = imageCapture
            }
        }, ContextCompat.getMainExecutor(context))

        onDispose {
            boundProvider?.let { provider ->
                val cases = listOfNotNull(boundPreview, boundCapture).toTypedArray()
                runCatching { provider.unbind(*cases) }
            }
            if (controller.imageCapture === boundCapture) controller.imageCapture = null
        }
    }

    AndroidView(modifier = modifier, factory = { previewView })
}
