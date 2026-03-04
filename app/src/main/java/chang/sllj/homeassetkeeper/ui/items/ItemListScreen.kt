package chang.sllj.homeassetkeeper.ui.items

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import chang.sllj.homeassetkeeper.R
import chang.sllj.homeassetkeeper.data.local.entity.ItemEntity
import chang.sllj.homeassetkeeper.ui.util.toFormattedDate

@Composable
fun ItemListScreen(
    onNavigateToDetail: (String) -> Unit,
    onNavigateToAddItem: () -> Unit,
    outerPadding: PaddingValues = PaddingValues(),
    viewModel: ItemListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding())
        ) {
            TabRow(
                selectedTabIndex = if (uiState.showArchived) 1 else 0,
                containerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier.height(36.dp)
            ) {
                Tab(
                    selected = !uiState.showArchived,
                    onClick = { if (uiState.showArchived) viewModel.toggleShowArchived() },
                    text = { Text(stringResource(R.string.item_list_tab_active), style = MaterialTheme.typography.labelLarge) }
                )
                Tab(
                    selected = uiState.showArchived,
                    onClick = { if (!uiState.showArchived) viewModel.toggleShowArchived() },
                    text = { Text(stringResource(R.string.item_list_tab_archived), style = MaterialTheme.typography.labelLarge) }
                )
            }
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = viewModel::onSearchQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                placeholder = { Text(stringResource(R.string.item_list_search_placeholder)) },
                leadingIcon = { Icon(Icons.Filled.Search, null, modifier = Modifier.size(20.dp)) },
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                            Icon(Icons.Filled.Close, null)
                        }
                    }
                },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium
            )
            if (uiState.isLoading) {
                Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
            } else if (uiState.items.isEmpty()) {
                EmptyItemList(uiState.searchQuery.isNotEmpty())
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(
                        start = 12.dp,
                        end = 12.dp,
                        top = 4.dp,
                        bottom = outerPadding.calculateBottomPadding() + 4.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(uiState.items, key = { it.id }) { item ->
                        ItemCard(
                            item = item,
                            onClick = { onNavigateToDetail(item.id) },
                            onRestore = if (uiState.showArchived) {
                                { viewModel.unarchiveItem(item.id) }
                            } else null
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ItemCard(
    item: ItemEntity,
    onClick: () -> Unit,
    onRestore: (() -> Unit)? = null
) {
    ElevatedCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Filled.Inventory2,
                null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Column(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (item.brand.isNotBlank()) {
                        Text(
                            item.brand,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (item.category.isNotBlank()) {
                        Text(
                            stringResource(R.string.item_brand_category_separator, item.category),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            if (onRestore != null) {
                IconButton(onClick = onRestore) {
                    Icon(
                        imageVector = Icons.Filled.Unarchive,
                        contentDescription = stringResource(R.string.unarchive),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            } else {
                item.purchaseDateMs?.let {
                    Text(
                        it.toFormattedDate(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyItemList(hasFilters: Boolean) {
    Box(Modifier.fillMaxSize(), Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
            Icon(
                Icons.Filled.Inventory2,
                null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.outlineVariant
            )
            Text(
                text = if (hasFilters) stringResource(R.string.item_list_empty_no_matches) else stringResource(R.string.item_list_empty_no_assets),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
