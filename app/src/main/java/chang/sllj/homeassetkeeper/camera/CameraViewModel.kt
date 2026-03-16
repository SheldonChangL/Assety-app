package chang.sllj.homeassetkeeper.camera

import androidx.camera.core.Preview
import androidx.camera.core.MeteringPointFactory
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** One-time events sent from [CameraViewModel] to [chang.sllj.homeassetkeeper.ui.camera.CameraScreen]. */
sealed class CameraEvent {
    /**
     * Emitted immediately after [ImageCaptureManager.captureImage] succeeds.
     * The screen navigates back and writes [imagePath] to the calling form's
     * [androidx.navigation.NavBackStackEntry.savedStateHandle].
     */
    data class CaptureSuccess(val result: CameraCaptureResult) : CameraEvent()

    data class ShowError(val message: String) : CameraEvent()
}

/**
 * Drives the camera capture screen.
 *
 * ## Flow
 * 1. The Composable calls [bindCamera] once it has a [Preview.SurfaceProvider].
 * 2. User taps the shutter → UI calls [captureImage].
 * 3. [ImageCaptureManager.captureImage] saves the JPEG; on success a
 *    [CameraEvent.CaptureSuccess] event is sent so the screen can navigate back
 *    and deliver the path to [chang.sllj.homeassetkeeper.ui.form.FormViewModel.onImageAdded].
 * 4. [onCleared] releases the camera hardware.
 */
@HiltViewModel
class CameraViewModel @Inject constructor(
    private val imageCaptureManager: ImageCaptureManager,
    private val guidedScanProcessor: GuidedScanProcessor
) : ViewModel() {

    private val _uiState = MutableStateFlow(CameraUiState())
    val uiState: StateFlow<CameraUiState> = _uiState.asStateFlow()

    private val _events = Channel<CameraEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    // ── Camera lifecycle ──────────────────────────────────────────────────────

    fun bindCamera(
        lifecycleOwner: LifecycleOwner,
        surfaceProvider: Preview.SurfaceProvider,
        lensFacing: Int = androidx.camera.core.CameraSelector.LENS_FACING_BACK
    ) {
        viewModelScope.launch {
            runCatching {
                imageCaptureManager.bindCamera(lifecycleOwner, surfaceProvider, lensFacing)
            }.onFailure { e ->
                _events.send(CameraEvent.ShowError("Camera initialisation failed: ${e.message}"))
            }
        }
    }

    // ── Capture ───────────────────────────────────────────────────────────────

    /**
     * Captures one image and emits [CameraEvent.CaptureSuccess] with the saved path.
     * Guards against double-taps: subsequent calls while a capture is in-flight are ignored.
     */
    fun captureImage(scanMode: CameraScanMode, cropRect: NormalizedRect?) {
        if (_uiState.value.isCapturing) return

        viewModelScope.launch {
            _uiState.update { it.copy(isCapturing = true, error = null) }

            val imagePath = runCatching {
                imageCaptureManager.captureImage(isTemporary = scanMode != CameraScanMode.PHOTO)
            }
                .getOrElse { e ->
                    _uiState.update { it.copy(isCapturing = false, error = "Capture failed: ${e.message}") }
                    _events.send(CameraEvent.ShowError("Capture failed: ${e.message}"))
                    return@launch
                }

            val result = runCatching {
                when (scanMode) {
                    CameraScanMode.PHOTO -> CameraCaptureResult.Photo(imagePath)
                    else -> guidedScanProcessor.process(
                        imagePath = imagePath,
                        scanMode = scanMode,
                        cropRect = requireNotNull(cropRect) {
                            "Guided scan modes require a crop rect."
                        }
                    )
                }
            }.onFailure {
                if (scanMode != CameraScanMode.PHOTO) {
                    runCatching { java.io.File(imagePath).delete() }
                }
            }.getOrElse { e ->
                _uiState.update { it.copy(isCapturing = false, error = "Scan failed: ${e.message}") }
                _events.send(CameraEvent.ShowError("Scan failed: ${e.message}"))
                return@launch
            }

            _uiState.update {
                it.copy(
                    isCapturing = false,
                    lastCapturedImagePath = (result as? CameraCaptureResult.Photo)?.imagePath
                )
            }
            if (scanMode != CameraScanMode.PHOTO) {
                runCatching { java.io.File(imagePath).delete() }
            }
            _events.send(CameraEvent.CaptureSuccess(result))
        }
    }

    fun tapToFocus(x: Float, y: Float, meteringPointFactory: MeteringPointFactory) {
        viewModelScope.launch {
            runCatching {
                imageCaptureManager.tapToFocus(x, y, meteringPointFactory)
            }.onFailure { e ->
                _events.send(CameraEvent.ShowError("Focus failed: ${e.message}"))
            }
        }
    }

    // ── Cleanup ───────────────────────────────────────────────────────────────

    override fun onCleared() {
        super.onCleared()
        imageCaptureManager.unbindAll()
    }
}
