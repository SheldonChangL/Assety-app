package chang.sllj.homeassetkeeper.camera

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import com.google.mlkit.vision.text.devanagari.DevanagariTextRecognizerOptions
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Singleton
class OcrProcessor @Inject constructor() {

    private val recognizers by lazy {
        listOf(
            OcrScript.LATIN to TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS),
            OcrScript.CHINESE to TextRecognition.getClient(
                ChineseTextRecognizerOptions.Builder().build()
            ),
            OcrScript.DEVANAGARI to TextRecognition.getClient(
                DevanagariTextRecognizerOptions.Builder().build()
            ),
            OcrScript.JAPANESE to TextRecognition.getClient(
                JapaneseTextRecognizerOptions.Builder().build()
            ),
            OcrScript.KOREAN to TextRecognition.getClient(
                KoreanTextRecognizerOptions.Builder().build()
            )
        )
    }

    suspend fun recognize(bitmap: Bitmap): List<OcrRecognition> = coroutineScope {
        recognizers.map { (script, recognizer) ->
            async {
                runCatching {
                    val text = recognizer.processBitmap(bitmap)
                    OcrRecognition(script = script, text = text)
                }.getOrNull()
            }
        }.awaitAll().filterNotNull()
    }

    private suspend fun TextRecognizer.processBitmap(bitmap: Bitmap): Text =
        suspendCancellableCoroutine { continuation ->
            process(InputImage.fromBitmap(bitmap, 0))
            .addOnSuccessListener { text ->
                if (!continuation.isCompleted) {
                    continuation.resume(text)
                }
            }
            .addOnFailureListener { error ->
                if (!continuation.isCompleted) {
                    continuation.resumeWithException(error)
                }
            }
        }
}
