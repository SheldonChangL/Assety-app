package chang.sllj.homeassetkeeper.camera

import android.content.Context
import android.os.Environment
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Wraps CameraX lifecycle management and image capture in a coroutine-friendly API.
 *
 * ## Threading model
 * - [bindCamera] and [captureImage] must be called from the Main dispatcher.
 *   Both are naturally called from [CameraViewModel] which runs on [Dispatchers.Main].
 * - [ProcessCameraProvider] is a system singleton per context; this class is
 *   @Singleton to ensure only one binding exists at any time.
 *
 * ## Lifecycle
 * The caller ([CameraViewModel]) is responsible for calling [unbindAll] in
 * `onCleared()` so the camera is released when the screen is destroyed.
 */
@Singleton
class ImageCaptureManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private var imageCapture: ImageCapture? = null
    private var cameraProvider: ProcessCameraProvider? = null

    /**
     * Initialises the camera preview and image-capture use cases, then binds them
     * to [lifecycleOwner]. Any previously-bound use cases are released first.
     *
     * [surfaceProvider] is obtained from [androidx.camera.view.PreviewView.surfaceProvider]
     * and feeds the live viewfinder displayed in the camera Composable.
     *
     * @param lensFacing [CameraSelector.LENS_FACING_BACK] (default) or FRONT.
     * @return The bound [Camera] instance, which can be used for torch/zoom control.
     */
    suspend fun bindCamera(
        lifecycleOwner: LifecycleOwner,
        surfaceProvider: Preview.SurfaceProvider,
        lensFacing: Int = CameraSelector.LENS_FACING_BACK
    ): Camera {
        val provider = awaitCameraProvider()
        cameraProvider = provider

        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(lensFacing)
            .build()

        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(surfaceProvider)
        }

        val capture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
            .build()
            .also { imageCapture = it }

        // Release any existing binding before re-binding.
        provider.unbindAll()

        return provider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, capture)
    }

    /**
     * Captures a single JPEG image and saves it to
     * [Context.getExternalFilesDir]/Pictures (falls back to internal storage if
     * external is unavailable).
     *
     * EXIF orientation metadata is written by CameraX automatically; no rotation
     * correction is needed before passing the file to [OcrProcessor].
     *
     * @return Absolute path of the saved image file.
     * @throws ImageCaptureException if the capture fails for any hardware or I/O reason.
     * @throws IllegalStateException if [bindCamera] has not been called first.
     */
    suspend fun captureImage(): String = suspendCancellableCoroutine { continuation ->
        val capture = requireNotNull(imageCapture) {
            "Camera is not bound. Call bindCamera() before captureImage()."
        }

        val file = createImageFile()
        val outputOptions = ImageCapture.OutputFileOptions.Builder(file).build()

        capture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    continuation.resume(file.absolutePath)
                }

                override fun onError(exception: ImageCaptureException) {
                    if (!continuation.isCompleted) {
                        continuation.resumeWithException(exception)
                    }
                }
            }
        )

        // CameraX does not support cancelling an in-flight capture. If the coroutine
        // is cancelled (e.g. user navigates away), delete the partially-written file.
        continuation.invokeOnCancellation {
            runCatching { file.delete() }
        }
    }

    /**
     * Releases all CameraX use cases. Must be called when the camera screen is
     * destroyed (i.e. from [CameraViewModel.onCleared]).
     */
    fun unbindAll() {
        cameraProvider?.unbindAll()
        imageCapture = null
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Bridges [ProcessCameraProvider.getInstance]'s [ListenableFuture] to a
     * suspend function without requiring the `concurrent-futures-ktx` artifact.
     */
    private suspend fun awaitCameraProvider(): ProcessCameraProvider =
        suspendCancellableCoroutine { continuation ->
            val future = ProcessCameraProvider.getInstance(context)
            future.addListener(
                {
                    runCatching { future.get() }
                        .onSuccess { if (!continuation.isCompleted) continuation.resume(it) }
                        .onFailure { if (!continuation.isCompleted) continuation.resumeWithException(it) }
                },
                ContextCompat.getMainExecutor(context)
            )
        }

    private fun createImageFile(): File {
        val dir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            ?: context.filesDir
        dir.mkdirs()
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        return File(dir, "IMG_$timestamp.jpg")
    }
}
