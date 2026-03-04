package chang.sllj.homeassetkeeper.backup

/**
 * Immutable state for the Backup & Restore settings screen.
 */
data class BackupUiState(
    val isExporting: Boolean = false,
    val isImporting: Boolean = false,
    /** Epoch-ms timestamp of the last successful export; null if none this session. */
    val lastExportTimestampMs: Long? = null,
    /** Non-null after a successful restore; epoch-ms of the backup's [BackupSerializer.BackupData.exportedAtMs]. */
    val lastImportedBackupTimestampMs: Long? = null,
    /** Human-readable error from the most recent failed operation. */
    val error: String? = null
)

/** One-time events emitted by [BackupViewModel] to drive UI side-effects. */
sealed class BackupEvent {
    /**
     * Signals that the UI should launch [android.content.Intent.ACTION_CREATE_DOCUMENT]
     * with [suggestedFileName] as the default file name.
     */
    data class RequestExportUri(val suggestedFileName: String) : BackupEvent()

    /**
     * Signals that the UI should launch [android.content.Intent.ACTION_OPEN_DOCUMENT]
     * to let the user pick a `.zip` backup file.
     */
    object RequestImportUri : BackupEvent()

    object ExportSuccess : BackupEvent()
    object ImportSuccess : BackupEvent()
}
