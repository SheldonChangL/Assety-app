package chang.sllj.homeassetkeeper.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import chang.sllj.homeassetkeeper.R
import chang.sllj.homeassetkeeper.data.local.entity.MaintenanceLogEntity
import chang.sllj.homeassetkeeper.data.local.entity.WarrantyReceiptEntity
import chang.sllj.homeassetkeeper.data.local.entity.WarrantyType
import chang.sllj.homeassetkeeper.domain.util.WarrantyCalculator
import chang.sllj.homeassetkeeper.ui.theme.ErrorRed
import chang.sllj.homeassetkeeper.ui.theme.SuccessGreen
import chang.sllj.homeassetkeeper.ui.theme.WarningAmber
import chang.sllj.homeassetkeeper.ui.util.toCurrencyString
import chang.sllj.homeassetkeeper.ui.util.toDaysLabel
import chang.sllj.homeassetkeeper.ui.util.toFormattedDate
import coil.compose.AsyncImage
import coil.request.ImageRequest
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemDetailScreen(
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (String) -> Unit,
    viewModel: ItemDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }
    var lightboxImagePath by remember { mutableStateOf<String?>(null) }
    val nowMs = remember { System.currentTimeMillis() }

    // Collect one-time events (NavigateBack, ShowMessage)
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is ItemDetailEvent.NavigateBack     -> onNavigateBack()
                is ItemDetailEvent.ShowMessage      ->
                    snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.item?.name ?: stringResource(R.string.detail_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                },
                actions = {
                    uiState.item?.let { item ->
                        IconButton(onClick = { onNavigateToEdit(item.id) }) {
                            Icon(Icons.Filled.Edit, stringResource(R.string.edit))
                        }
                        IconButton(onClick = {
                            if (item.isArchived) {
                                viewModel.unarchiveItem()
                            } else {
                                viewModel.archiveItem()
                            }
                        }) {
                            Icon(
                                imageVector = if (item.isArchived) Icons.Filled.Unarchive else Icons.Filled.Archive,
                                contentDescription = if (item.isArchived) stringResource(R.string.unarchive) else stringResource(R.string.archive)
                            )
                        }
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Filled.Delete, stringResource(R.string.delete), tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->

        when {
            uiState.isLoading -> {
                Box(Modifier.fillMaxSize().padding(innerPadding), Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            uiState.error != null -> {
                Box(Modifier.fillMaxSize().padding(innerPadding), Alignment.Center) {
                    Text(uiState.error!!, color = MaterialTheme.colorScheme.error)
                }
            }

            uiState.item != null -> {
                val item = uiState.item!!
                val imagePaths = item.imagePaths.split(",").filter { it.isNotBlank() }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = innerPadding.calculateTopPadding()),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // ── Hero Image Carousel ───────────────────────────────────
                    if (imagePaths.isNotEmpty()) {
                        item {
                            val pagerState = rememberPagerState(pageCount = { imagePaths.size })
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(16f / 9f)
                                    .background(Color.Black)
                            ) {
                                HorizontalPager(
                                    state = pagerState,
                                    modifier = Modifier.fillMaxSize()
                                ) { page ->
                                    val path = imagePaths[page]
                                    AsyncImage(
                                        model = ImageRequest.Builder(LocalContext.current)
                                            .data(File(path))
                                            .build(),
                                        contentDescription = stringResource(R.string.detail_asset_image_desc, page),
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clickable { lightboxImagePath = path }
                                    )
                                }
                                
                                // Simple Pager Indicator
                                if (imagePaths.size > 1) {
                                    Row(
                                        Modifier
                                            .height(32.dp)
                                            .fillMaxWidth()
                                            .align(Alignment.BottomCenter)
                                            .padding(bottom = 8.dp),
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        repeat(imagePaths.size) { iteration ->
                                            val color = if (pagerState.currentPage == iteration) Color.White else Color.White.copy(alpha = 0.5f)
                                            Box(
                                                modifier = Modifier
                                                    .padding(2.dp)
                                                    .clip(CircleShape)
                                                    .background(color)
                                                    .size(8.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // ── Details card ──────────────────────────────────────────
                    item {
                        ElevatedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(stringResource(R.string.detail_section_details), style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold)
                                Spacer(Modifier.height(12.dp))
                                DetailRow(stringResource(R.string.detail_label_category), item.category)
                                if (item.brand.isNotBlank())        DetailRow(stringResource(R.string.detail_label_brand), item.brand)
                                if (item.modelNumber.isNotBlank())  DetailRow(stringResource(R.string.detail_label_model), item.modelNumber)
                                if (item.serialNumber.isNotBlank()) DetailRow(stringResource(R.string.detail_label_serial), item.serialNumber)
                                if (item.location.isNotBlank())     DetailRow(stringResource(R.string.detail_label_location), item.location)
                                item.purchaseDateMs?.let { DetailRow(stringResource(R.string.detail_label_purchased), it.toFormattedDate()) }
                                item.purchasePriceCents?.let {
                                    DetailRow(stringResource(R.string.detail_label_price), it.toCurrencyString())
                                }
                                if (item.notes.isNotBlank()) {
                                    Spacer(Modifier.height(8.dp))
                                    HorizontalDivider()
                                    Spacer(Modifier.height(8.dp))
                                    Text(stringResource(R.string.detail_label_notes), style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(item.notes, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }

                    // ── Specifications ────────────────────────────────────────
                    if (uiState.specifications.isNotEmpty()) {
                        item {
                            ElevatedCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(stringResource(R.string.detail_section_specifications),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold)
                                    Spacer(Modifier.height(12.dp))
                                    uiState.specifications.forEach { spec ->
                                        DetailRow(spec.key, spec.value)
                                    }
                                }
                            }
                        }
                    }

                    // ── Warranties ────────────────────────────────────────────
                    item {
                        Box(Modifier.padding(horizontal = 16.dp)) {
                            SectionHeader(
                                stringResource(R.string.detail_section_warranties),
                                if (uiState.warranties.isEmpty()) stringResource(R.string.detail_empty_recorded) else null
                            )
                        }
                    }
                    items(uiState.warranties, key = { it.id }) { warranty ->
                        Box(Modifier.padding(horizontal = 16.dp)) {
                            WarrantyCard(warranty = warranty, nowMs = nowMs)
                        }
                    }

                    // ── Maintenance logs ──────────────────────────────────────
                    item {
                        Box(Modifier.padding(horizontal = 16.dp)) {
                            SectionHeader(
                                stringResource(R.string.detail_section_maintenance),
                                if (uiState.maintenanceLogs.isEmpty()) stringResource(R.string.detail_empty_recorded) else null
                            )
                        }
                    }
                    items(uiState.maintenanceLogs, key = { it.id }) { log ->
                        Box(Modifier.padding(horizontal = 16.dp)) {
                            MaintenanceLogCard(log = log)
                        }
                    }
                }
            }
        }
    }

    // ── Lightbox Dialog ──────────────────────────────────────────────────────
    lightboxImagePath?.let { path ->
        ZoomableImageDialog(
            imagePath = path,
            onDismiss = { lightboxImagePath = null }
        )
    }

    // ── Delete confirmation dialog ─────────────────────────────────────────────
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.detail_delete_title)) },
            text  = { Text(stringResource(R.string.detail_delete_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.deleteItem()
                    }
                ) { Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
}

// ── Sub-composables ───────────────────────────────────────────────────────────

@Composable
private fun DetailRow(label: String, value: String) {
    if (value.isBlank()) return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(110.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun SectionHeader(title: String, emptyText: String?) {
    Column {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        if (emptyText != null) {
            Text(emptyText, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun WarrantyCard(warranty: WarrantyReceiptEntity, nowMs: Long) {
    val daysLeft = WarrantyCalculator.daysUntilExpiry(warranty.expiryDateMs, nowMs)
    val statusColor: Color = when {
        daysLeft < 0L  -> ErrorRed
        daysLeft <= 30 -> WarningAmber
        else           -> SuccessGreen
    }
    val typeName = when (warranty.warrantyType) {
        WarrantyType.MANUFACTURER    -> stringResource(R.string.warranty_type_manufacturer)
        WarrantyType.EXTENDED        -> stringResource(R.string.warranty_type_extended)
        WarrantyType.STORE_PROTECTION -> stringResource(R.string.warranty_type_store_protection)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Receipt, null, tint = statusColor)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(typeName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                if (warranty.providerName.isNotBlank()) {
                    Text(warranty.providerName, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(
                    stringResource(R.string.detail_warranty_expires, warranty.expiryDateMs.toFormattedDate()),
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Icon(
                    if (daysLeft < 0) Icons.Filled.Warning else Icons.Filled.Receipt,
                    null,
                    modifier = Modifier.size(16.dp),
                    tint = statusColor
                )
                Text(
                    text = daysLeft.toDaysLabel(context = LocalContext.current),
                    style = MaterialTheme.typography.labelMedium,
                    color = statusColor,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun MaintenanceLogCard(log: MaintenanceLogEntity) {
    val isPending = log.completedDateMs == null

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.Build,
                null,
                tint = if (isPending) WarningAmber else SuccessGreen
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(log.description, style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold)
                log.scheduledDateMs?.let {
                    Text(stringResource(R.string.detail_maintenance_scheduled, it.toFormattedDate()),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                log.completedDateMs?.let {
                    Text(stringResource(R.string.detail_maintenance_completed, it.toFormattedDate()),
                        style = MaterialTheme.typography.bodySmall,
                        color = SuccessGreen)
                }
                log.costCents?.let {
                    Text(stringResource(R.string.detail_maintenance_cost, it.toCurrencyString()),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (isPending) {
                Text(stringResource(R.string.detail_maintenance_pending), style = MaterialTheme.typography.labelSmall, color = WarningAmber)
            }
        }
    }
}
