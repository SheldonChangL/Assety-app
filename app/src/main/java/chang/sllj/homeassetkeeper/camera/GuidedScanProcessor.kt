package chang.sllj.homeassetkeeper.camera

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class GuidedScanProcessor @Inject constructor(
    private val ocrProcessor: OcrProcessor,
    private val ocrParser: OcrParser
) {

    suspend fun process(
        imagePath: String,
        scanMode: CameraScanMode,
        cropRect: NormalizedRect
    ): CameraCaptureResult = withContext(Dispatchers.Default) {
        require(scanMode != CameraScanMode.PHOTO) {
            "GuidedScanProcessor only supports OCR scan modes."
        }

        val uprightBitmap = loadUprightBitmap(imagePath)
        val croppedBitmap = uprightBitmap.crop(cropRect)

        try {
            val recognitions = ocrProcessor.recognize(croppedBitmap)
            when (scanMode) {
                CameraScanMode.BRAND -> {
                    val candidates = ocrParser.parseBrandCandidates(recognitions)
                    if (candidates.isEmpty()) {
                        throw IllegalStateException("No brand text found in the guided area.")
                    }
                    CameraCaptureResult.BrandScan(candidates)
                }

                CameraScanMode.PURCHASE_DATE -> {
                    val candidates = ocrParser.parsePurchaseDateCandidates(recognitions)
                    if (candidates.isEmpty()) {
                        throw IllegalStateException("No purchase date found in the guided area.")
                    }
                    CameraCaptureResult.PurchaseDateScan(candidates)
                }

                CameraScanMode.PHOTO -> error("Unsupported mode")
            }
        } finally {
            croppedBitmap.recycle()
            if (croppedBitmap !== uprightBitmap) {
                uprightBitmap.recycle()
            }
        }
    }

    private fun loadUprightBitmap(imagePath: String): Bitmap {
        val bitmap = BitmapFactory.decodeFile(imagePath)
            ?: throw IllegalStateException("Unable to decode captured image.")

        val exif = ExifInterface(imagePath)
        val orientation = exif.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        )

        val matrix = Matrix().apply {
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> postRotate(270f)
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> postScale(-1f, 1f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> postScale(1f, -1f)
            }
        }

        if (matrix.isIdentity) return bitmap

        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            .also { bitmap.recycle() }
    }

    private fun Bitmap.crop(rect: NormalizedRect): Bitmap {
        val left = (width * rect.left).toInt().coerceIn(0, width - 1)
        val top = (height * rect.top).toInt().coerceIn(0, height - 1)
        val right = (width * rect.right).toInt().coerceIn(left + 1, width)
        val bottom = (height * rect.bottom).toInt().coerceIn(top + 1, height)
        val cropWidth = (right - left).coerceAtLeast(1)
        val cropHeight = (bottom - top).coerceAtLeast(1)
        return Bitmap.createBitmap(this, left, top, cropWidth, cropHeight)
    }
}
