package chang.sllj.homeassetkeeper.camera

import com.google.mlkit.vision.text.Text

enum class OcrScript {
    LATIN,
    CHINESE,
    DEVANAGARI,
    JAPANESE,
    KOREAN
}

data class OcrRecognition(
    val script: OcrScript,
    val text: Text
)
