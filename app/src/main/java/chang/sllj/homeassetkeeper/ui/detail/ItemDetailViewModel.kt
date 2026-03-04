package chang.sllj.homeassetkeeper.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import chang.sllj.homeassetkeeper.data.local.entity.MaintenanceLogEntity
import chang.sllj.homeassetkeeper.data.local.entity.WarrantyReceiptEntity
import chang.sllj.homeassetkeeper.data.repository.ItemRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Navigation argument key — must match the NavGraph route definition. */
const val NAV_ARG_ITEM_ID = "itemId"

@HiltViewModel
class ItemDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: ItemRepository
) : ViewModel() {

    private val itemId: String = checkNotNull(savedStateHandle[NAV_ARG_ITEM_ID]) {
        "ItemDetailViewModel requires a non-null '$NAV_ARG_ITEM_ID' navigation argument."
    }

    // ── UI State ──────────────────────────────────────────────────────────────

    /**
     * Combines four independent Flows into a single observable state.
     * Room re-emits each Flow whenever its backing table changes, so the
     * detail screen always reflects the latest database values.
     */
    val uiState: StateFlow<ItemDetailUiState> = combine(
        repository.getItemById(itemId),
        repository.getSpecsForItem(itemId),
        repository.getWarrantiesForItem(itemId),
        repository.getLogsForItem(itemId)
    ) { item, specs, warranties, logs ->
        when (item) {
            null -> ItemDetailUiState(
                isLoading = false,
                error = "Item no longer exists."
            )
            else -> ItemDetailUiState(
                isLoading = false,
                item = item,
                specifications = specs,
                warranties = warranties,
                maintenanceLogs = logs
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ItemDetailUiState(isLoading = true)
    )

    // ── One-time events (navigation, snackbars) ───────────────────────────────

    private val _events = Channel<ItemDetailEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    // ── User actions ──────────────────────────────────────────────────────────

    fun archiveItem() {
        viewModelScope.launch {
            runCatching {
                repository.archiveItem(itemId, System.currentTimeMillis())
                _events.send(ItemDetailEvent.NavigateBack)
            }.onFailure {
                _events.send(ItemDetailEvent.ShowMessage("Failed to archive item."))
            }
        }
    }

    fun deleteItem() {
        viewModelScope.launch {
            runCatching {
                val item = uiState.value.item ?: return@launch
                repository.deleteItem(item)
                _events.send(ItemDetailEvent.NavigateBack)
            }.onFailure {
                _events.send(ItemDetailEvent.ShowMessage("Failed to delete item."))
            }
        }
    }

    fun upsertWarranty(warranty: WarrantyReceiptEntity) {
        viewModelScope.launch {
            runCatching { repository.upsertWarranty(warranty) }
                .onFailure { _events.send(ItemDetailEvent.ShowMessage("Failed to save warranty.")) }
        }
    }

    fun deleteWarranty(warranty: WarrantyReceiptEntity) {
        viewModelScope.launch {
            runCatching { repository.deleteWarranty(warranty) }
                .onFailure { _events.send(ItemDetailEvent.ShowMessage("Failed to delete warranty.")) }
        }
    }

    fun upsertMaintenanceLog(log: MaintenanceLogEntity) {
        viewModelScope.launch {
            runCatching { repository.upsertMaintenanceLog(log) }
                .onFailure { _events.send(ItemDetailEvent.ShowMessage("Failed to save log.")) }
        }
    }

    fun deleteMaintenanceLog(log: MaintenanceLogEntity) {
        viewModelScope.launch {
            runCatching { repository.deleteMaintenanceLog(log) }
                .onFailure { _events.send(ItemDetailEvent.ShowMessage("Failed to delete log.")) }
        }
    }
}
