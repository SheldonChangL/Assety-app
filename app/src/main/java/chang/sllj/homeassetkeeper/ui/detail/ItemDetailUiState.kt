package chang.sllj.homeassetkeeper.ui.detail

import chang.sllj.homeassetkeeper.data.local.entity.ItemEntity
import chang.sllj.homeassetkeeper.data.local.entity.MaintenanceLogEntity
import chang.sllj.homeassetkeeper.data.local.entity.SpecificationEntity
import chang.sllj.homeassetkeeper.data.local.entity.WarrantyReceiptEntity

data class ItemDetailUiState(
    val isLoading: Boolean = true,
    val item: ItemEntity? = null,
    val specifications: List<SpecificationEntity> = emptyList(),
    val warranties: List<WarrantyReceiptEntity> = emptyList(),
    val maintenanceLogs: List<MaintenanceLogEntity> = emptyList(),
    /** Non-null only when the item could not be found (e.g. deleted from another session). */
    val error: String? = null
)

/** One-time UI events emitted by [ItemDetailViewModel]. */
sealed class ItemDetailEvent {
    /** Navigate back to the list after a successful archive or delete. */
    object NavigateBack : ItemDetailEvent()
    data class ShowMessage(val message: String) : ItemDetailEvent()
}
