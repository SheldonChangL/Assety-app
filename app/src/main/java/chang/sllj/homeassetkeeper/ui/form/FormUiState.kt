package chang.sllj.homeassetkeeper.ui.form

/**
 * Immutable snapshot of the Add/Edit item form.
 *
 * Monetary input uses a raw [purchasePriceText] string (what the user typed,
 * e.g. "12.99") so the TextField is never disrupted by programmatic reformatting.
 * [FormViewModel] converts it to cents on save.
 *
 * [imagePaths] holds up to three absolute JPEG paths captured via the camera.
 * They are stored in [ItemEntity.imagePaths] as a comma-separated string.
 *
 * [savedItemId] is set once save completes successfully. The composable observes
 * this field and triggers navigation back when it becomes non-null.
 */
data class FormUiState(
    val isLoading: Boolean = false,

    // ── Metadata ──────────────────────────────────────────────────────────────
    val isEditMode: Boolean = false,
    val existingItemId: String? = null,
    /** ID of the first manufacturer warranty loaded in edit mode; null for new items. */
    val existingWarrantyId: String? = null,

    // ── Form fields ───────────────────────────────────────────────────────────
    val name: String = "",
    val nameError: String? = null,
    val category: String = "",
    val brand: String = "",
    val modelNumber: String = "",
    val serialNumber: String = "",
    val purchaseDateMs: Long? = null,           // set to today in ViewModel init
    val purchasePriceText: String = "",
    val priceError: String? = null,
    val location: String = "",
    val notes: String = "",

    /** Up to three absolute JPEG paths captured by the camera. */
    val imagePaths: List<String> = emptyList(),

    // ── Warranty (required) ───────────────────────────────────────────────────
    /** Epoch-ms of the warranty expiry date. Required — validated before save. */
    val warrantyExpiryDateMs: Long? = null,
    val warrantyExpiryError: String? = null,

    // ── Dynamic specifications ────────────────────────────────────────────────
    val specifications: List<SpecFieldState> = emptyList(),

    // ── Submit state ──────────────────────────────────────────────────────────
    val isSaving: Boolean = false,
    val savedItemId: String? = null,
    val saveError: String? = null
)

/**
 * Represents one key-value specification row while the form is being edited.
 * [id] is a local UUID used to track and mutate individual rows before they
 * are persisted as [SpecificationEntity] objects.
 */
data class SpecFieldState(
    val id: String,
    val key: String,
    val value: String,
    val sortOrder: Int
)
