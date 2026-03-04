package chang.sllj.homeassetkeeper.camera

import androidx.camera.core.Preview
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
    data class CaptureSuccess(val imagePath: String) : CameraEvent()

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
    private val imageCaptureManager: ImageCaptureManager
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
    fun captureImage() {
        if (_uiState.value.isCapturing) return

        viewModelScope.launch {
            _uiState.update { it.copy(isCapturing = true, error = null) }

            val imagePath = runCatching { imageCaptureManager.captureImage() }
                .getOrElse { e ->
                    _uiState.update { it.copy(isCapturing = false, error = "Capture failed: ${e.message}") }
                    _events.send(CameraEvent.ShowError("Capture failed: ${e.message}"))
                    return@launch
                }

            _uiState.update { it.copy(isCapturing = false, lastCapturedImagePath = imagePath) }
            _events.send(CameraEvent.CaptureSuccess(imagePath))
        }
    }

    // ── Cleanup ───────────────────────────────────────────────────────────────

    override fun onCleared() {
        super.onCleared()
        imageCaptureManager.unbindAll()
    }
}
