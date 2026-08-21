package com.belinze.lifeos.core.update

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Process-singleton channel that lets any screen request an OTA check
 * without prop-drilling through the navigation graph.
 *
 * Only [OtaUpdatePromptHost] (in MainScaffold) ever *reads* this;
 * every other call-site writes via [requestCheck].
 */
object OtaSharedTrigger {
    private val _manualTrigger = MutableStateFlow(0)
    val manualTrigger: StateFlow<Int> = _manualTrigger

    /** Increment the trigger so the host re-checks even if it already ran once. */
    fun requestCheck() {
        _manualTrigger.value++
    }
}
