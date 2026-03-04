package chang.sllj.homeassetkeeper.ui.items
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import chang.sllj.homeassetkeeper.data.repository.ItemRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ItemListViewModel @Inject constructor(private val repository: ItemRepository) : ViewModel() {
    private val _searchQuery = MutableStateFlow("")
    private val _showArchived = MutableStateFlow(false)
    val uiState: StateFlow<ItemListUiState> = combine(
        _showArchived.flatMapLatest { archived ->
            if (archived) repository.getArchivedItems() else repository.getAllActiveItems()
        },
        _searchQuery,
        _showArchived
    ) { items, query, archived ->
        val filtered = items.filter { item ->
            query.isBlank() || item.name.contains(query, ignoreCase = true) ||
                    item.brand.contains(query, ignoreCase = true) ||
                    item.category.contains(query, ignoreCase = true) ||
                    item.modelNumber.contains(query, ignoreCase = true)
        }
        ItemListUiState(isLoading = false, items = filtered, searchQuery = query, showArchived = archived)
    }.stateIn(scope = viewModelScope, started = SharingStarted.WhileSubscribed(5_000), initialValue = ItemListUiState(isLoading = true))
    fun onSearchQueryChange(query: String) { _searchQuery.value = query }
    fun toggleShowArchived() { _showArchived.update { !it } }
    fun archiveItem(id: String) {
        viewModelScope.launch { repository.archiveItem(id, System.currentTimeMillis()) }
    }
    fun unarchiveItem(id: String) {
        viewModelScope.launch { repository.unarchiveItem(id, System.currentTimeMillis()) }
    }
}
