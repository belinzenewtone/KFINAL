package com.belinze.lifeos.core.update.presentation

internal data class OtaDialogCallbacks(
    val onDismiss:       () -> Unit,
    val onLater:         () -> Unit,
    val onPrimaryAction: () -> Unit,
    val onCancelDownload: () -> Unit,
    val onWebsite:       () -> Unit,
)
