package chang.sllj.homeassetkeeper.ui.items

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import chang.sllj.homeassetkeeper.data.repository.ItemRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private data class FilterState(
    val query: String = "",
    val category: String? = null,
    val location: String? = null,
    val showArchived: Boolean = false
)

@HiltViewModel
class ItemListViewModel @Inject constructor(
    private val repository: ItemRepository
) : ViewModel() {

    private val _filter = MutableStateFlow(FilterState())

    val uiState: StateFlow<ItemListUiState> = combine(
        repository.getAllActiveItems(),
        repository.getArchivedItems(),
        repository.getAllCategories(),
        repository.getAllLocations(),
        _filter
    ) { activeItems, archivedItems, categories, locations, filter ->

        // Switch source based on the tab selection
        val sourceItems = if (filter.showArchived) archivedItems else activeItems

        val filtered = sourceItems.filter { item ->
            val matchesQuery = filter.query.isBlank() ||
                item.name.contains(filter.query, ignoreCase = true) ||
                item.brand.contains(filter.query, ignoreCase = true) ||
                item.category.contains(filter.query, ignoreCase = true) ||
                item.modelNumber.contains(filter.query, ignoreCase = true)

            val matchesCategory = filter.category == null || item.category == filter.category
            val matchesLocation = filter.location == null || item.location == filter.location

            matchesQuery && matchesCategory && matchesLocation
        }

        ItemListUiState(
            isLoading = false,
            items = filtered,
            searchQuery = filter.query,
            selectedCategory = filter.category,
            selectedLocation = filter.location,
            availableCategories = categories,
            availableLocations = locations,
            showArchived = filter.showArchived
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ItemListUiState(isLoading = true)
    )

    fun onSearchQueryChange(query: String) = _filter.update { it.copy(query = query) }
    fun onCategoryFilterChange(category: String?) = _filter.update { it.copy(category = category) }
    fun onLocationFilterChange(location: String?) = _filter.update { it.copy(location = location) }
    fun toggleShowArchived() = _filter.update { it.copy(showArchived = !it.showArchived) }
    
    fun unarchiveItem(id: String) {
        viewModelScope.launch {
            repository.unarchiveItem(id, System.currentTimeMillis())
        }
    }

    fun archiveItem(id: String) {
        viewModelScope.launch {
            repository.archiveItem(id, System.currentTimeMillis())
        }
    }

    fun clearFilters() = _filter.update { FilterState() }
}
