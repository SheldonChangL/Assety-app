package chang.sllj.homeassetkeeper.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import chang.sllj.homeassetkeeper.data.repository.ItemRepository
import chang.sllj.homeassetkeeper.domain.util.WarrantyCalculator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: ItemRepository
) : ViewModel() {

    companion object {
        /** Window used for both expiring-warranties and upcoming-maintenance sections. */
        private const val THIRTY_DAYS_MS = 30L * 24 * 60 * 60 * 1_000
    }

    /**
     * Snapshot of "now" taken once per ViewModel lifetime.
     *
     * Using a fixed instant keeps the dashboard stable across the session and
     * avoids spurious re-emissions as the clock ticks. The user refreshes the
     * screen by navigating away and back, which recreates the ViewModel.
     */
    private val sessionNowMs: Long = System.currentTimeMillis()

    /**
     * Single observable state for the dashboard screen.
     *
     * Three underlying Flows are combined:
     *  - all active items  (drives item count + category breakdown + name lookups)
     *  - active alerted warranties (filtered to the 30-day window)
     *  - upcoming pending maintenance tasks (scheduled within 30 days)
     *
     * [SharingStarted.WhileSubscribed(5_000)] keeps the upstream active for 5 s
     * after the last collector disappears, surviving configuration changes without
     * restarting the query pipeline.
     */
    val uiState: StateFlow<HomeUiState> = combine(
        repository.getAllActiveItems(),
        repository.getActiveAlertedWarranties(sessionNowMs),
        repository.getUpcomingMaintenanceFlow(
            fromMs = sessionNowMs,
            toMs = sessionNowMs + THIRTY_DAYS_MS
        )
    ) { items, warranties, maintenance ->

        // O(n) name lookup map built once per emission.
        val itemNameById = items.associateBy(keySelector = { it.id }, valueTransform = { it.name })

        val expiringWarranties = warranties
            .filter { WarrantyCalculator.isExpiringSoon(it.expiryDateMs, 30, sessionNowMs) }
            .map { warranty ->
                WarrantyWithItem(
                    warranty = warranty,
                    itemName = itemNameById[warranty.itemId].orEmpty(),
                    daysUntilExpiry = WarrantyCalculator.daysUntilExpiry(
                        warranty.expiryDateMs,
                        sessionNowMs
                    )
                )
            }

        val upcomingMaintenance = maintenance.map { log ->
            MaintenanceWithItem(
                log = log,
                itemName = itemNameById[log.itemId].orEmpty()
            )
        }

        val categoryBreakdown = items
            .groupBy { it.category }
            .map { (category, categoryItems) ->
                CategoryCount(category = category, count = categoryItems.size)
            }
            .sortedByDescending { it.count }

        val recentlyAdded = items
            .sortedByDescending { it.createdAtMs }
            .take(5)

        HomeUiState(
            isLoading = false,
            activeItemCount = items.size,
            expiringWarranties = expiringWarranties,
            upcomingMaintenance = upcomingMaintenance,
            categoryBreakdown = categoryBreakdown,
            recentlyAddedItems = recentlyAdded
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState(isLoading = true)
    )
}
