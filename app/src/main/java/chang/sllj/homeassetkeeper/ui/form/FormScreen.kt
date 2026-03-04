package chang.sllj.homeassetkeeper.ui.form

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import chang.sllj.homeassetkeeper.R
import chang.sllj.homeassetkeeper.ui.util.toFormattedDate
import coil.compose.AsyncImage
import coil.request.ImageRequest
import java.io.File

/**
 * Add / Edit asset form.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormScreen(
    onNavigateBack: () -> Unit,
    onNavigateToCamera: () -> Unit,
    viewModel: FormViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var showPurchaseDatePicker by rememberSaveable { mutableStateOf(false) }
    var showWarrantyExpiryPicker by rememberSaveable { mutableStateOf(false) }
    var categoryExpanded by remember { mutableStateOf(false) }
    var locationExpanded by remember { mutableStateOf(false) }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) onNavigateToCamera()
    }

    LaunchedEffect(uiState.savedItemId) {
        if (uiState.savedItemId != null) onNavigateBack()
    }

    LaunchedEffect(uiState.saveError) {
        uiState.saveError?.let { snackbarHostState.showSnackbar(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.isEditMode) stringResource(R.string.form_title_edit) else stringResource(R.string.form_title_add)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                }
            )
        },
        floatingActionButton = {
            if (!uiState.isSaving) {
                ExtendedFloatingActionButton(
                    onClick = viewModel::save,
                    icon = { Icon(Icons.Filled.Save, null) },
                    text = { Text(stringResource(R.string.save)) }
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->

        if (uiState.isLoading) {
            Box(Modifier
                .fillMaxSize()
                .padding(innerPadding), Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // ── Photo strip ───────────────────────────────────────────────────
            item {
                PhotoStrip(
                    imagePaths = uiState.imagePaths,
                    onRemove = { index -> viewModel.onImageRemoved(index) },
                    onAddPhoto = {
                        cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                    }
                )
            }

            // ── Basic Info ────────────────────────────────────────────────────
            item { SectionLabel(stringResource(R.string.form_section_basic_info)) }

            item {
                FormTextField(
                    value = uiState.name,
                    onValueChange = viewModel::onNameChange,
                    label = stringResource(R.string.form_label_name_required),
                    error = uiState.nameError,
                    imeAction = ImeAction.Next
                )
            }

            // Category dropdown
            item {
                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = uiState.category,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.form_label_category)) },
                        placeholder = { Text(stringResource(R.string.form_placeholder_category)) },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded)
                        },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false }
                    ) {
                        FormViewModel.CATEGORIES.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat) },
                                onClick = {
                                    viewModel.onCategoryChange(cat)
                                    categoryExpanded = false
                                },
                                contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                            )
                        }
                    }
                }
            }

            item {
                FormTextField(
                    value = uiState.brand,
                    onValueChange = viewModel::onBrandChange,
                    label = stringResource(R.string.form_label_brand),
                    imeAction = ImeAction.Next
                )
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FormTextField(
                        value = uiState.modelNumber,
                        onValueChange = viewModel::onModelNumberChange,
                        label = stringResource(R.string.form_label_model),
                        modifier = Modifier.weight(1f),
                        imeAction = ImeAction.Next
                    )
                    FormTextField(
                        value = uiState.serialNumber,
                        onValueChange = viewModel::onSerialNumberChange,
                        label = stringResource(R.string.form_label_serial),
                        modifier = Modifier.weight(1f),
                        imeAction = ImeAction.Next
                    )
                }
            }

            // Location dropdown
            item {
                ExposedDropdownMenuBox(
                    expanded = locationExpanded,
                    onExpandedChange = { locationExpanded = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = uiState.location,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.form_label_location)) },
                        placeholder = { Text(stringResource(R.string.form_placeholder_location)) },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = locationExpanded)
                        },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = locationExpanded,
                        onDismissRequest = { locationExpanded = false }
                    ) {
                        FormViewModel.LOCATIONS.forEach { loc ->
                            DropdownMenuItem(
                                text = { Text(loc) },
                                onClick = {
                                    viewModel.onLocationChange(loc)
                                    locationExpanded = false
                                },
                                contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                            )
                        }
                    }
                }
            }

            // ── Warranty & Purchase ───────────────────────────────────────────
            item { SectionLabel(stringResource(R.string.form_section_warranty_purchase)) }

            // Warranty Expiry Date — required
            item {
                OutlinedTextField(
                    value = uiState.warrantyExpiryDateMs?.toFormattedDate() ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.form_label_warranty_expiry_required)) },
                    placeholder = { Text(stringResource(R.string.form_placeholder_date)) },
                    isError = uiState.warrantyExpiryError != null,
                    supportingText = uiState.warrantyExpiryError?.let {
                        { Text(it, color = MaterialTheme.colorScheme.error) }
                    },
                    trailingIcon = {
                        IconButton(onClick = { showWarrantyExpiryPicker = true }) {
                            Icon(Icons.Filled.DateRange, null)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Purchase Date and Price
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = uiState.purchaseDateMs?.toFormattedDate() ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.form_label_purchase_date)) },
                        trailingIcon = {
                            IconButton(onClick = { showPurchaseDatePicker = true }) {
                                Icon(Icons.Filled.DateRange, null)
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                    FormTextField(
                        value = uiState.purchasePriceText,
                        onValueChange = viewModel::onPurchasePriceChange,
                        label = stringResource(R.string.form_label_price),
                        error = uiState.priceError,
                        keyboardType = KeyboardType.Decimal,
                        modifier = Modifier.weight(1f),
                        imeAction = ImeAction.Next
                    )
                }
            }

            // ── Notes ─────────────────────────────────────────────────────────
            item {
                FormTextField(
                    value = uiState.notes,
                    onValueChange = viewModel::onNotesChange,
                    label = stringResource(R.string.form_label_notes),
                    singleLine = false,
                    imeAction = ImeAction.Default
                )
            }

            // ── Specifications ────────────────────────────────────────────────
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SectionLabel(stringResource(R.string.form_section_specifications))
                    TextButton(onClick = viewModel::addSpecification) {
                        Icon(Icons.Filled.Add, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.size(4.dp))
                        Text(stringResource(R.string.add))
                    }
                }
            }

            itemsIndexed(
                items = uiState.specifications,
                key = { _, spec -> spec.id }
            ) { _, spec ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = spec.key,
                        onValueChange = { viewModel.updateSpecificationKey(spec.id, it) },
                        label = { Text(stringResource(R.string.form_label_spec_key)) },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = spec.value,
                        onValueChange = { viewModel.updateSpecificationValue(spec.id, it) },
                        label = { Text(stringResource(R.string.form_label_spec_value)) },
                        modifier = Modifier.weight(1.5f),
                        singleLine = true
                    )
                    IconButton(onClick = { viewModel.removeSpecification(spec.id) }) {
                        Icon(
                            Icons.Filled.Close,
                            stringResource(R.string.remove),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }

    if (showWarrantyExpiryPicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = uiState.warrantyExpiryDateMs
        )
        DatePickerDialog(
            onDismissRequest = { showWarrantyExpiryPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.onWarrantyExpiryDateChange(datePickerState.selectedDateMillis)
                    showWarrantyExpiryPicker = false
                }) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showWarrantyExpiryPicker = false }) { Text(stringResource(R.string.cancel)) }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showPurchaseDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = uiState.purchaseDateMs
        )
        DatePickerDialog(
            onDismissRequest = { showPurchaseDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.onPurchaseDateChange(datePickerState.selectedDateMillis)
                    showPurchaseDatePicker = false
                }) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showPurchaseDatePicker = false }) { Text(stringResource(R.string.cancel)) }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun PhotoStrip(
    imagePaths: List<String>,
    onRemove: (index: Int) -> Unit,
    onAddPhoto: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        imagePaths.forEachIndexed { index, path ->
            PhotoThumbnail(
                path = path,
                onRemove = { onRemove(index) }
            )
        }
        if (imagePaths.size < 3) {
            AddPhotoTile(onClick = onAddPhoto)
        }
    }
}

@Composable
private fun PhotoThumbnail(path: String, onRemove: () -> Unit) {
    val context = LocalContext.current
    Box(modifier = Modifier.size(100.dp)) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(File(path))
                .crossfade(true)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(12.dp))
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp)
                .size(22.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.55f))
                .clickable(onClick = onRemove),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = stringResource(R.string.form_remove_photo),
                tint = Color.White,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddPhotoTile(onClick: () -> Unit) {
    OutlinedCard(
        onClick = onClick,
        modifier = Modifier.size(100.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Filled.AddAPhoto,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    stringResource(R.string.form_add_photo),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 4.dp)
    )
}

@Composable
private fun FormTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier.fillMaxWidth(),
    error: String? = null,
    placeholder: String? = null,
    singleLine: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = placeholder?.let { { Text(it) } },
        isError = error != null,
        supportingText = error?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
        singleLine = singleLine,
        maxLines = if (singleLine) 1 else 5,
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Words,
            keyboardType = keyboardType,
            imeAction = imeAction
        ),
        modifier = modifier
    )
}
