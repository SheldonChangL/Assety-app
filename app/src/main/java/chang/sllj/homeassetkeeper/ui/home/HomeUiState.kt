package chang.sllj.homeassetkeeper.ui.home

import chang.sllj.homeassetkeeper.data.local.entity.ItemEntity
import chang.sllj.homeassetkeeper.data.local.entity.MaintenanceLogEntity
import chang.sllj.homeassetkeeper.data.local.entity.WarrantyReceiptEntity

data class HomeUiState(
    val isLoading: Boolean = true,
    val activeItemCount: Int = 0,
    val expiringWarranties: List<WarrantyWithItem> = emptyList(),
    val upcomingMaintenance: List<MaintenanceWithItem> = emptyList(),
    val categoryBreakdown: List<CategoryCount> = emptyList(),
    val recentlyAddedItems: List<ItemEntity> = emptyList()
)

data class WarrantyWithItem(
    val warranty: WarrantyReceiptEntity,
    val itemName: String,
    /** Whole days until expiry; negative if the warranty is already expired. */
    val daysUntilExpiry: Long
)

data class MaintenanceWithItem(
    val log: MaintenanceLogEntity,
    val itemName: String
)

data class CategoryCount(
    val category: String,
    val count: Int
)
