package chang.sllj.homeassetkeeper.camera

/**
 * Immutable state for the camera capture screen.
 *
 * States:
 * 1. Idle ([isCapturing] = false) — viewfinder active, shutter enabled.
 * 2. Capturing ([isCapturing] = true) — shutter disabled, spinner shown.
 * 3. Error ([error] != null) — snackbar shown; user can retry.
 */
data class CameraUiState(
    /** True while [ImageCaptureManager.captureImage] is in flight. */
    val isCapturing: Boolean = false,

    /** Absolute path of the last successfully captured JPEG. Null if none yet. */
    val lastCapturedImagePath: String? = null,

    /** Human-readable error message. Non-null only when capture has failed. */
    val error: String? = null
)
