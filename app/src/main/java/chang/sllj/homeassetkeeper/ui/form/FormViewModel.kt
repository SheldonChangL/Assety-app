package chang.sllj.homeassetkeeper.ui.form

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import chang.sllj.homeassetkeeper.data.local.entity.ItemEntity
import chang.sllj.homeassetkeeper.data.local.entity.SpecificationEntity
import chang.sllj.homeassetkeeper.data.local.entity.WarrantyReceiptEntity
import chang.sllj.homeassetkeeper.data.local.entity.WarrantyType
import chang.sllj.homeassetkeeper.data.repository.ItemRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID
import javax.inject.Inject

/**
 * Optional navigation argument. Present when editing an existing item; absent for new items.
 * Must match the NavGraph route definition.
 */
const val NAV_ARG_EDIT_ITEM_ID = "editItemId"

/** Maximum number of photos allowed per asset. */
private const val MAX_IMAGES = 3

@HiltViewModel
class FormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: ItemRepository
) : ViewModel() {

    /** Non-null → edit mode; null → create mode. */
    private val editItemId: String? = savedStateHandle[NAV_ARG_EDIT_ITEM_ID]

    private val _uiState = MutableStateFlow(
        FormUiState(purchaseDateMs = todayUtcMidnightMs())
    )
    val uiState: StateFlow<FormUiState> = _uiState.asStateFlow()

    init {
        if (editItemId != null) {
            loadExistingItem(editItemId)
        }
    }

    // ── Load (edit mode) ──────────────────────────────────────────────────────

    private fun loadExistingItem(itemId: String) {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val item       = repository.getItemById(itemId).firstOrNull()
            val specs      = repository.getSpecsForItem(itemId).firstOrNull() ?: emptyList()
            val warranties = repository.getWarrantiesForItem(itemId).firstOrNull() ?: emptyList()
            val primary    = warranties.firstOrNull()

            if (item == null) {
                _uiState.update { it.copy(isLoading = false, saveError = "Item not found.") }
                return@launch
            }

            _uiState.update { state ->
                state.copy(
                    isLoading         = false,
                    isEditMode        = true,
                    existingItemId    = item.id,
                    existingWarrantyId = primary?.id,
                    name              = item.name,
                    category          = item.category,
                    brand             = item.brand,
                    modelNumber       = item.modelNumber,
                    serialNumber      = item.serialNumber,
                    purchaseDateMs    = item.purchaseDateMs ?: todayUtcMidnightMs(),
                    purchasePriceText = item.purchasePriceCents
                        ?.let { cents -> "%.2f".format(cents / 100.0) }
                        ?: "",
                    location          = item.location,
                    notes             = item.notes,
                    imagePaths        = item.imagePaths.toImagePathList(),
                    warrantyExpiryDateMs = primary?.expiryDateMs,
                    specifications    = specs.mapIndexed { index, spec ->
                        SpecFieldState(
                            id        = spec.id,
                            key       = spec.key,
                            value     = spec.value,
                            sortOrder = index
                        )
                    }
                )
            }
        }
    }

    // ── Field change handlers ─────────────────────────────────────────────────

    fun onNameChange(name: String) = _uiState.update {
        it.copy(name = name, nameError = null)
    }

    /**
     * Updates the selected category and auto-populates spec rows when the form
     * has no existing specs. Changing category on a form that already has specs
     * leaves those specs intact so manual edits are never silently overwritten.
     */
    fun onCategoryChange(category: String) {
        _uiState.update { state ->
            val shouldAutoPopulate = state.specifications.isEmpty() && category != "Other"
            val newSpecs = if (shouldAutoPopulate) {
                (CATEGORY_SPEC_TEMPLATES[category] ?: emptyList())
                    .mapIndexed { index, key ->
                        SpecFieldState(
                            id        = UUID.randomUUID().toString(),
                            key       = key,
                            value     = "",
                            sortOrder = index
                        )
                    }
            } else {
                state.specifications
            }
            state.copy(category = category, specifications = newSpecs)
        }
    }

    fun onBrandChange(brand: String)             = _uiState.update { it.copy(brand = brand) }
    fun onModelNumberChange(model: String)        = _uiState.update { it.copy(modelNumber = model) }
    fun onSerialNumberChange(serial: String)      = _uiState.update { it.copy(serialNumber = serial) }
    fun onPurchaseDateChange(dateMs: Long?)        = _uiState.update { it.copy(purchaseDateMs = dateMs) }
    fun onLocationChange(location: String)        = _uiState.update { it.copy(location = location) }
    fun onNotesChange(notes: String)              = _uiState.update { it.copy(notes = notes) }
    fun onPurchasePriceChange(text: String)       = _uiState.update { it.copy(purchasePriceText = text, priceError = null) }

    fun onWarrantyExpiryDateChange(dateMs: Long?) = _uiState.update {
        it.copy(warrantyExpiryDateMs = dateMs, warrantyExpiryError = null)
    }

    // ── Multi-image management ────────────────────────────────────────────────

    /**
     * Appends [path] to [FormUiState.imagePaths] if fewer than [MAX_IMAGES] photos
     * have been captured. Silently ignored when the limit is already reached.
     */
    fun onImageAdded(path: String) {
        _uiState.update { state ->
            if (state.imagePaths.size >= MAX_IMAGES) state
            else state.copy(imagePaths = state.imagePaths + path)
        }
    }

    /** Removes the image at [index] from [FormUiState.imagePaths]. */
    fun onImageRemoved(index: Int) {
        _uiState.update { state ->
            state.copy(imagePaths = state.imagePaths.toMutableList().apply { removeAt(index) })
        }
    }

    fun onBrandScanCandidatesReceived(candidates: List<String>) {
        _uiState.update {
            it.copy(
                pendingBrandCandidates = candidates.distinct().take(3),
                pendingPurchaseDateCandidates = emptyList()
            )
        }
    }

    fun confirmBrandCandidate(brand: String) {
        _uiState.update {
            it.copy(
                brand = brand.trim(),
                pendingBrandCandidates = emptyList()
            )
        }
    }

    fun dismissBrandScanCandidates() {
        _uiState.update { it.copy(pendingBrandCandidates = emptyList()) }
    }

    fun onPurchaseDateScanCandidatesReceived(candidates: List<Long>) {
        _uiState.update {
            it.copy(
                pendingPurchaseDateCandidates = candidates.distinct().take(3),
                pendingBrandCandidates = emptyList()
            )
        }
    }

    fun confirmPurchaseDateCandidate(dateMs: Long) {
        _uiState.update {
            it.copy(
                purchaseDateMs = dateMs,
                pendingPurchaseDateCandidates = emptyList()
            )
        }
    }

    fun dismissPurchaseDateScanCandidates() {
        _uiState.update { it.copy(pendingPurchaseDateCandidates = emptyList()) }
    }

    // ── Specification management ───────────────────────────────────────────────

    fun addSpecification() {
        _uiState.update { state ->
            val nextOrder = (state.specifications.maxOfOrNull { it.sortOrder } ?: -1) + 1
            state.copy(
                specifications = state.specifications + SpecFieldState(
                    id        = UUID.randomUUID().toString(),
                    key       = "",
                    value     = "",
                    sortOrder = nextOrder
                )
            )
        }
    }

    fun updateSpecificationKey(specId: String, key: String) {
        _uiState.update { state ->
            state.copy(
                specifications = state.specifications.map { spec ->
                    if (spec.id == specId) spec.copy(key = key) else spec
                }
            )
        }
    }

    fun updateSpecificationValue(specId: String, value: String) {
        _uiState.update { state ->
            state.copy(
                specifications = state.specifications.map { spec ->
                    if (spec.id == specId) spec.copy(value = value) else spec
                }
            )
        }
    }

    fun removeSpecification(specId: String) {
        _uiState.update { state ->
            state.copy(specifications = state.specifications.filter { it.id != specId })
        }
    }

    // ── Save ──────────────────────────────────────────────────────────────────

    fun save() {
        val state = _uiState.value
        if (!validate(state)) return

        _uiState.update { it.copy(isSaving = true, saveError = null) }

        viewModelScope.launch {
            runCatching {
                val nowMs  = System.currentTimeMillis()
                val itemId = state.existingItemId ?: UUID.randomUUID().toString()

                val item = ItemEntity(
                    id                = itemId,
                    name              = state.name.trim(),
                    category          = state.category.trim(),
                    brand             = state.brand.trim(),
                    modelNumber       = state.modelNumber.trim(),
                    serialNumber      = state.serialNumber.trim(),
                    purchaseDateMs    = state.purchaseDateMs,
                    purchasePriceCents = parsePriceToCents(state.purchasePriceText),
                    location          = state.location.trim(),
                    notes             = state.notes.trim(),
                    imagePaths        = state.imagePaths.joinToString(","),
                    isArchived        = false,
                    createdAtMs       = if (state.isEditMode) {
                        repository.getItemByIdOnce(itemId)?.createdAtMs ?: nowMs
                    } else {
                        nowMs
                    },
                    updatedAtMs       = nowMs
                )

                val specs = state.specifications
                    .filter { it.key.isNotBlank() }
                    .mapIndexed { index, field ->
                        SpecificationEntity(
                            id        = field.id,
                            itemId    = itemId,
                            key       = field.key.trim(),
                            value     = field.value.trim(),
                            sortOrder = index
                        )
                    }

                repository.saveItemWithSpecs(item, specs)

                // Persist the warranty expiry date. warrantyExpiryDateMs is validated
                // non-null in validate(), so the !! is safe here.
                val warrantyId = state.existingWarrantyId ?: UUID.randomUUID().toString()
                repository.upsertWarranty(
                    WarrantyReceiptEntity(
                        id               = warrantyId,
                        itemId           = itemId,
                        warrantyType     = WarrantyType.MANUFACTURER,
                        providerName     = state.brand.trim(),
                        startDateMs      = state.purchaseDateMs ?: nowMs,
                        expiryDateMs     = state.warrantyExpiryDateMs!!,
                        receiptImagePath = "",
                        notes            = "",
                        isAlertEnabled   = true,
                        alertDaysBefore  = 30
                    )
                )

                _uiState.update { it.copy(isSaving = false, savedItemId = itemId) }

            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(isSaving = false, saveError = throwable.message ?: "Save failed.")
                }
            }
        }
    }

    // ── Validation ────────────────────────────────────────────────────────────

    private fun validate(state: FormUiState): Boolean {
        var valid = true

        if (state.name.isBlank()) {
            _uiState.update { it.copy(nameError = "Name is required.") }
            valid = false
        }

        if (state.warrantyExpiryDateMs == null) {
            _uiState.update { it.copy(warrantyExpiryError = "Warranty expiry date is required.") }
            valid = false
        }

        if (state.purchasePriceText.isNotBlank() && parsePriceToCents(state.purchasePriceText) == null) {
            _uiState.update { it.copy(priceError = "Enter a valid price (e.g. 12.99).") }
            valid = false
        }

        return valid
    }

    private fun parsePriceToCents(text: String): Long? {
        if (text.isBlank()) return null
        val amount = text.trim().toDoubleOrNull() ?: return null
        if (amount < 0) return null
        return (amount * 100).toLong()
    }

    companion object {
        /** Ordered list shown in the category dropdown. */
        val CATEGORIES: List<String> = listOf(
            "Appliances", "Electronics", "HVAC", "Lighting",
            "Plumbing", "Furniture", "Tools", "Other"
        )

        /** Predefined home location options shown in the location dropdown. */
        val LOCATIONS: List<String> = listOf(
            "Kitchen", "Living Room", "Dining Room", "Master Bedroom",
            "Bedroom", "Bathroom", "Laundry Room", "Garage",
            "Basement", "Attic", "Office / Study", "Backyard", "Other"
        )

        /**
         * Default spec keys pre-populated when a fresh form selects a category.
         * Keys are human-readable labels; values start empty for the user to fill in.
         */
        private val CATEGORY_SPEC_TEMPLATES: Map<String, List<String>> = mapOf(
            "Appliances"  to listOf("Wattage", "Dimensions", "Color", "Energy Rating"),
            "Electronics" to listOf("Screen Size", "RAM", "Storage", "Operating System"),
            "HVAC"        to listOf("BTU Rating", "Filter Size", "Refrigerant Type", "SEER Rating"),
            "Lighting"    to listOf("Bulb Type", "Wattage", "Color Temperature", "Lumens"),
            "Plumbing"    to listOf("Pipe Size", "Material", "Flow Rate (GPM)"),
            "Furniture"   to listOf("Material", "Dimensions", "Color"),
            "Tools"       to listOf("Power Source", "Voltage", "Blade / Bit Size")
        )

        /**
         * Returns today's date as epoch-milliseconds at UTC midnight.
         * Material3 DatePicker operates in UTC day-boundary timestamps.
         */
        fun todayUtcMidnightMs(): Long =
            LocalDate.now(ZoneOffset.UTC)
                .atStartOfDay(ZoneOffset.UTC)
                .toInstant()
                .toEpochMilli()
    }
}

// ── Private helpers ───────────────────────────────────────────────────────────

/**
 * Splits a comma-separated image-paths string into a [List].
 * Returns an empty list for blank input.
 */
private fun String.toImagePathList(): List<String> =
    if (isBlank()) emptyList() else split(",").filter(String::isNotBlank)
