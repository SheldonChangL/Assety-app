package chang.sllj.homeassetkeeper.camera

/**
 * Camera flow variants supported by the shared capture screen.
 *
 * PHOTO stores a normal asset image. BRAND and PURCHASE_DATE capture a
 * temporary image, crop the guided frame area, and run OCR on-device.
 */
enum class CameraScanMode(val routeValue: String) {
    PHOTO("photo"),
    BRAND("brand"),
    PURCHASE_DATE("purchase_date");

    companion object {
        fun fromRouteValue(value: String?): CameraScanMode =
            entries.firstOrNull { it.routeValue == value } ?: PHOTO
    }
}

data class NormalizedRect(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
) {
    init {
        require(left in 0f..1f && top in 0f..1f && right in 0f..1f && bottom in 0f..1f) {
            "NormalizedRect values must be between 0 and 1."
        }
        require(left < right && top < bottom) {
            "NormalizedRect requires left < right and top < bottom."
        }
    }

    val widthFraction: Float get() = right - left
    val heightFraction: Float get() = bottom - top
}

sealed class CameraCaptureResult {
    data class Photo(val imagePath: String) : CameraCaptureResult()
    data class BrandScan(val candidates: List<String>) : CameraCaptureResult()
    data class PurchaseDateScan(val candidates: List<Long>) : CameraCaptureResult()
}
