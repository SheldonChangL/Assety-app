package chang.sllj.homeassetkeeper.ui.items

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import chang.sllj.homeassetkeeper.data.local.entity.ItemEntity
import chang.sllj.homeassetkeeper.ui.util.toFormattedDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemListScreen(
    onNavigateToDetail: (String) -> Unit,
    onNavigateToAddItem: () -> Unit,
    /**
     * Padding from the root Scaffold in MainActivity. Applied to the LazyColumn's
     * bottom content padding so items scroll above the BottomBar + FAB.
     */
    outerPadding: PaddingValues = PaddingValues(),
    viewModel: ItemListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showFilters by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Assets") },
                actions = {
                    IconButton(onClick = { showFilters = !showFilters }) {
                        Icon(Icons.Filled.FilterList, contentDescription = "Toggle filters")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding())
        ) {
            // ── Tab toggle for Active/Archived ────────────────────────────────
            TabRow(selectedTabIndex = if (uiState.showArchived) 1 else 0) {
                Tab(
                    selected = !uiState.showArchived,
                    onClick = { if (uiState.showArchived) viewModel.toggleShowArchived() },
                    text = { Text("Active") }
                )
                Tab(
                    selected = uiState.showArchived,
                    onClick = { if (!uiState.showArchived) viewModel.toggleShowArchived() },
                    text = { Text("Archived") }
                )
            }

            // ── Search bar ────────────────────────────────────────────────────
            OutlinedTextField(
                value         = uiState.searchQuery,
                onValueChange = viewModel::onSearchQueryChange,
                modifier      = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder  = { Text("Search assets…") },
                leadingIcon  = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                            Icon(Icons.Filled.Close, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true
            )

            // ── Filter chips ──────────────────────────────────────────────────
            AnimatedVisibility(visible = showFilters) {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    if (uiState.availableCategories.isNotEmpty()) {
                        Text(
                            "Category",
                            style    = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding        = PaddingValues(vertical = 4.dp)
                        ) {
                            items(uiState.availableCategories) { cat ->
                                FilterChip(
                                    selected = uiState.selectedCategory == cat,
                                    onClick  = {
                                        viewModel.onCategoryFilterChange(
                                            if (uiState.selectedCategory == cat) null else cat
                                        )
                                    },
                                    label = { Text(cat) }
                                )
                            }
                        }
                    }
                    if (uiState.availableLocations.isNotEmpty()) {
                        Text(
                            "Location",
                            style    = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding        = PaddingValues(vertical = 4.dp)
                        ) {
                            items(uiState.availableLocations) { loc ->
                                FilterChip(
                                    selected = uiState.selectedLocation == loc,
                                    onClick  = {
                                        viewModel.onLocationFilterChange(
                                            if (uiState.selectedLocation == loc) null else loc
                                        )
                                    },
                                    label = { Text(loc) }
                                )
                            }
                        }
                    }
                }
            }

            // ── Content ───────────────────────────────────────────────────────
            when {
                uiState.isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                uiState.items.isEmpty() -> {
                    EmptyItemList(
                        hasFilters = uiState.searchQuery.isNotEmpty() ||
                            uiState.selectedCategory != null ||
                            uiState.selectedLocation != null,
                        onAddItem  = onNavigateToAddItem,
                        isArchivedMode = uiState.showArchived
                    )
                }

                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(
                            start  = 16.dp,
                            end    = 16.dp,
                            top    = 8.dp,
                            bottom = outerPadding.calculateBottomPadding() + 8.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(uiState.items, key = { it.id }) { item ->
                            val dismissState = rememberSwipeToDismissBoxState(
                                confirmValueChange = { value ->
                                    if (value == SwipeToDismissBoxValue.EndToStart) {
                                        if (item.isArchived) {
                                            viewModel.unarchiveItem(item.id)
                                        } else {
                                            viewModel.archiveItem(item.id)
                                        }
                                        true
                                    } else {
                                        false
                                    }
                                }
                            )

                            SwipeToDismissBox(
                                state = dismissState,
                                backgroundContent = {
                                    val color = if (item.isArchived) Color(0xFF4CAF50) else Color(0xFFF44336)
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(color, MaterialTheme.shapes.medium)
                                            .padding(horizontal = 20.dp),
                                        contentAlignment = Alignment.CenterEnd
                                    ) {
                                        Icon(
                                            imageVector = if (item.isArchived) Icons.Default.Unarchive else Icons.Default.Archive,
                                            contentDescription = if (item.isArchived) "Unarchive" else "Archive",
                                            tint = Color.White
                                        )
                                    }
                                },
                                enableDismissFromStartToEnd = false
                            ) {
                                ItemCard(item = item, onClick = { onNavigateToDetail(item.id) })
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Sub-composables ───────────────────────────────────────────────────────────

@Composable
private fun ItemCard(item: ItemEntity, onClick: () -> Unit) {
    ElevatedCard(
        onClick  = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier          = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.Inventory2,
                contentDescription = null,
                modifier           = Modifier.size(40.dp),
                tint               = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text      = item.name,
                    style     = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines  = 1,
                    overflow  = TextOverflow.Ellipsis
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (item.brand.isNotBlank()) {
                        Text(
                            text  = item.brand,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                    if (item.category.isNotBlank()) {
                        Text(
                            text  = "· ${item.category}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }
                if (item.location.isNotBlank()) {
                    Text(
                        text  = item.location,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                        maxLines = 1
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                item.purchaseDateMs?.let { date ->
                    Text(
                        text  = date.toFormattedDate(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (item.isArchived) {
                    Text(
                        text  = "Archived",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyItemList(hasFilters: Boolean, onAddItem: () -> Unit, isArchivedMode: Boolean) {
    Box(
        modifier          = Modifier.fillMaxSize(),
        contentAlignment  = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier            = Modifier.padding(32.dp)
        ) {
            Icon(
                Icons.Filled.Inventory2,
                contentDescription = null,
                modifier           = Modifier.size(64.dp),
                tint               = MaterialTheme.colorScheme.outlineVariant
            )
            Spacer(modifier = Modifier.size(16.dp))
            val message = when {
                isArchivedMode -> "No archived assets"
                hasFilters -> "No assets match your filters"
                else -> "No assets yet"
            }
            Text(
                text  = message,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (!hasFilters && !isArchivedMode) {
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text  = "Tap + to add your first home asset.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
