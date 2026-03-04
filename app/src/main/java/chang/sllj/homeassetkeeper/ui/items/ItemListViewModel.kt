package chang.sllj.homeassetkeeper.ui.items

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import chang.sllj.homeassetkeeper.data.local.entity.ItemEntity
import chang.sllj.homeassetkeeper.data.repository.ItemRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ItemListViewModel @Inject constructor(
    private val repository: ItemRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")

    val uiState: StateFlow<ItemListUiState> = combine(
        repository.getAllActiveItems(),
        _searchQuery
    ) { items, query ->
        val filtered = items.filter { item ->
            query.isBlank() ||
                item.name.contains(query, ignoreCase = true) ||
                item.brand.contains(query, ignoreCase = true) ||
                item.category.contains(query, ignoreCase = true) ||
                item.modelNumber.contains(query, ignoreCase = true)
        }

        ItemListUiState(
            isLoading = false,
            items = filtered,
            searchQuery = query
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ItemListUiState(isLoading = true)
    )

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun archiveItem(id: String) {
        viewModelScope.launch {
            repository.archiveItem(id, System.currentTimeMillis())
        }
    }

    fun unarchiveItem(id: String) {
        viewModelScope.launch {
            repository.unarchiveItem(id, System.currentTimeMillis())
        }
    }
}
