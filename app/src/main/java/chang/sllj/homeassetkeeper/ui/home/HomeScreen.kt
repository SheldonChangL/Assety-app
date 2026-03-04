package chang.sllj.homeassetkeeper.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import chang.sllj.homeassetkeeper.ui.theme.ErrorRed
import chang.sllj.homeassetkeeper.ui.theme.SuccessGreen
import chang.sllj.homeassetkeeper.ui.theme.WarningAmber
import chang.sllj.homeassetkeeper.ui.util.toFormattedDate
import chang.sllj.homeassetkeeper.ui.util.toDaysLabel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToItems: () -> Unit,
    onNavigateToDetail: (String) -> Unit,
    /**
     * Padding from the root Scaffold in MainActivity that includes clearance
     * for the BottomNavigationBar, the global FAB, and the system navigation bar.
     * Applied to the LazyColumn's bottom content padding so the last item is never
     * obscured by the floating UI.
     */
    outerPadding: PaddingValues = PaddingValues(),
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val layoutDirection = LocalLayoutDirection.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Home Asset Keeper") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        // FAB is intentionally absent here — it lives in MainActivity's root Scaffold
        // and is wired to navigate to the Add Asset form from any bottom-nav tab.
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->

        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding()),
            contentPadding = PaddingValues(
                start  = innerPadding.calculateStartPadding(layoutDirection) + 16.dp,
                end    = innerPadding.calculateEndPadding(layoutDirection) + 16.dp,
                top    = 16.dp,
                // Use the outer Scaffold's bottom padding so content scrolls above
                // the BottomBar + FAB + system navigation bar.
                bottom = outerPadding.calculateBottomPadding() + 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Summary card ──────────────────────────────────────────────────
            item {
                SummaryCard(
                    activeItemCount    = uiState.activeItemCount,
                    expiringCount      = uiState.expiringWarranties.size,
                    onBrowseItems      = onNavigateToItems
                )
            }

            // ── Expiring warranties ────────────────────────────────────────────
            if (uiState.expiringWarranties.isNotEmpty()) {
                item {
                    SectionHeader(
                        title = "Expiring Warranties",
                        count = uiState.expiringWarranties.size
                    )
                }
                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(horizontal = 0.dp)
                    ) {
                        items(uiState.expiringWarranties, key = { it.warranty.id }) { item ->
                            WarrantyExpiryCard(
                                item = item,
                                onClick = { onNavigateToDetail(item.warranty.itemId) }
                            )
                        }
                    }
                }
            }

            // ── Upcoming maintenance ───────────────────────────────────────────
            if (uiState.upcomingMaintenance.isNotEmpty()) {
                item {
                    SectionHeader(
                        title = "Upcoming Maintenance",
                        count = uiState.upcomingMaintenance.size
                    )
                }
                items(uiState.upcomingMaintenance, key = { it.log.id }) { item ->
                    MaintenanceCard(
                        item = item,
                        onClick = { onNavigateToDetail(item.log.itemId) }
                    )
                }
            }

            // ── Category overview ──────────────────────────────────────────────
            if (uiState.categoryBreakdown.isNotEmpty()) {
                item {
                    SectionHeader(title = "By Category", count = null)
                }
                item {
                    CategoryBreakdownRow(categories = uiState.categoryBreakdown)
                }
            }

            // ── Empty state ───────────────────────────────────────────────────
            if (uiState.activeItemCount == 0) {
                item { EmptyDashboard() }
            }
        }
    }
}

// ── Sub-composables ───────────────────────────────────────────────────────────

@Composable
private fun SummaryCard(
    activeItemCount: Int,
    expiringCount: Int,
    onBrowseItems: () -> Unit
) {
    ElevatedCard(
        onClick = onBrowseItems,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Inventory2,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "$activeItemCount",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "active assets",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (expiringCount > 0) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Filled.Warning,
                        contentDescription = null,
                        tint = WarningAmber
                    )
                    Text(
                        text = "$expiringCount expiring",
                        style = MaterialTheme.typography.labelSmall,
                        color = WarningAmber
                    )
                }
            } else {
                Icon(
                    Icons.Filled.CheckCircle,
                    contentDescription = "All warranties OK",
                    tint = SuccessGreen
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, count: Int?) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f)
        )
        if (count != null) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun WarrantyExpiryCard(item: WarrantyWithItem, onClick: () -> Unit) {
    val daysColor: Color = when {
        item.daysUntilExpiry < 0L  -> ErrorRed
        item.daysUntilExpiry <= 7L -> WarningAmber
        else                       -> MaterialTheme.colorScheme.onSurface
    }

    Card(
        onClick = onClick,
        modifier = Modifier.width(200.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = item.itemName,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = item.warranty.warrantyType.name.lowercase()
                    .replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = item.daysUntilExpiry.toDaysLabel(),
                style = MaterialTheme.typography.labelLarge,
                color = daysColor,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = item.warranty.expiryDateMs.toFormattedDate(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MaintenanceCard(item: MaintenanceWithItem, onClick: () -> Unit) {
    ElevatedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.Build,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.itemName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = item.log.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
            item.log.scheduledDateMs?.let { scheduled ->
                Text(
                    text = scheduled.toFormattedDate(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CategoryBreakdownRow(categories: List<CategoryCount>) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(categories, key = { it.category }) { cat ->
            SuggestionChip(
                onClick = {},
                label = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Category, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("${cat.category}  ${cat.count}")
                    }
                }
            )
        }
    }
}

@Composable
private fun EmptyDashboard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Filled.Inventory2,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.outlineVariant
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "No assets yet",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Tap + to add your first home asset.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
