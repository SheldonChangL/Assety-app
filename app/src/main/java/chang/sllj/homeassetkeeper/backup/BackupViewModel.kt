package chang.sllj.homeassetkeeper.backup

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

/**
 * Drives the Backup & Restore settings screen.
 *
 * ## SAF integration pattern (Compose)
 * The ViewModel cannot hold an [android.app.Activity] reference and therefore cannot
 * launch SAF intents directly. Instead:
 * 1. The UI collects [events] and maps [BackupEvent.RequestExportUri] /
 *    [BackupEvent.RequestImportUri] to `ActivityResultLauncher.launch()` calls.
 * 2. The launcher callbacks pass the selected [Uri] back to [onExportUriSelected]
 *    or [onImportUriSelected].
 *
 * ## Threading
 * All [BackupManager] calls are dispatched on [Dispatchers.IO] to avoid blocking
 * the main thread during ZIP creation / file I/O.
 *
 * ## Composable wiring example
 * ```kotlin
 * val exportLauncher = rememberLauncherForActivityResult(
 *     ActivityResultContracts.CreateDocument("application/zip")
 * ) { uri -> uri?.let { viewModel.onExportUriSelected(it) } }
 *
 * val importLauncher = rememberLauncherForActivityResult(
 *     ActivityResultContracts.OpenDocument()
 * ) { uri -> uri?.let { viewModel.onImportUriSelected(it) } }
 *
 * LaunchedEffect(Unit) {
 *     viewModel.events.collect { event ->
 *         when (event) {
 *             is BackupEvent.RequestExportUri -> exportLauncher.launch(event.suggestedFileName)
 *             BackupEvent.RequestImportUri   -> importLauncher.launch(arrayOf("application/zip"))
 *             BackupEvent.ExportSuccess      -> snackbarHostState.showSnackbar("Backup exported.")
 *             BackupEvent.ImportSuccess      -> snackbarHostState.showSnackbar("Backup restored.")
 *         }
 *     }
 * }
 * ```
 */
@HiltViewModel
class BackupViewModel @Inject constructor(
    private val backupManager: BackupManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(BackupUiState())
    val uiState: StateFlow<BackupUiState> = _uiState.asStateFlow()

    private val _events = Channel<BackupEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    // ── Export ────────────────────────────────────────────────────────────────

    /**
     * Step 1 of export: emit a [BackupEvent.RequestExportUri] so the Composable
     * can launch the SAF document-creation picker with a pre-filled file name.
     */
    fun startExport() {
        if (_uiState.value.isExporting || _uiState.value.isImporting) return
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        viewModelScope.launch {
            _events.send(BackupEvent.RequestExportUri("HomeAssetKeeper_$timestamp.zip"))
        }
    }

    /**
     * Step 2 of export: called by the Composable with the [Uri] the user selected.
     * Builds the ZIP on [Dispatchers.IO] and writes it to the URI.
     */
    fun onExportUriSelected(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isExporting = true, error = null) }
            runCatching { backupManager.exportToUri(uri) }
                .onSuccess {
                    _uiState.update {
                        it.copy(isExporting = false, lastExportTimestampMs = System.currentTimeMillis())
                    }
                    _events.send(BackupEvent.ExportSuccess)
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(isExporting = false, error = e.message ?: "Export failed.")
                    }
                }
        }
    }

    // ── Import ────────────────────────────────────────────────────────────────

    /**
     * Step 1 of import: emit [BackupEvent.RequestImportUri] so the Composable
     * can launch the SAF document-picker filtered to `application/zip`.
     */
    fun startImport() {
        if (_uiState.value.isExporting || _uiState.value.isImporting) return
        viewModelScope.launch {
            _events.send(BackupEvent.RequestImportUri)
        }
    }

    /**
     * Step 2 of import: called by the Composable with the [Uri] the user selected.
     * Reads the ZIP on [Dispatchers.IO], restores data to Room, and re-copies images.
     */
    fun onImportUriSelected(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isImporting = true, error = null) }
            runCatching { backupManager.importFromUri(uri) }
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isImporting = false,
                            lastImportedBackupTimestampMs = System.currentTimeMillis()
                        )
                    }
                    _events.send(BackupEvent.ImportSuccess)
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(isImporting = false, error = e.message ?: "Restore failed.")
                    }
                }
        }
    }

    /** Clears the current error so the UI can dismiss the error message. */
    fun clearError() = _uiState.update { it.copy(error = null) }
}
