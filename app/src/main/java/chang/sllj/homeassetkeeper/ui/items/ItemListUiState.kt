package chang.sllj.homeassetkeeper.ui.items

import chang.sllj.homeassetkeeper.data.local.entity.ItemEntity

data class ItemListUiState(
    val isLoading: Boolean = true,
    val items: List<ItemEntity> = emptyList(),
    val searchQuery: String = "",
    val selectedCategory: String? = null,
    val selectedLocation: String? = null,
    val availableCategories: List<String> = emptyList(),
    val availableLocations: List<String> = emptyList(),
    val showArchived: Boolean = false
)
