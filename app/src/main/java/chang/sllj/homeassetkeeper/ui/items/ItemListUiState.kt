package chang.sllj.homeassetkeeper.ui.items

import chang.sllj.homeassetkeeper.data.local.entity.ItemEntity

data class ItemListUiState(
    val isLoading: Boolean = true,
    val items: List<ItemEntity> = emptyList(),
    val searchQuery: String = ""
)
