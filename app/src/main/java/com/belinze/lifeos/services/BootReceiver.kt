package com.belinze.lifeos.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.belinze.lifeos.data.datastore.AppPreferences
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Re-arms all AlarmManager alarms after device reboot.
 * Android cancels all scheduled alarms on reboot; this receiver restores them
 * by delegating to NotificationSync.syncAll() — the same reconciliation that
 * runs on every normal app start.
 */
@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {
    @Inject lateinit var notificationSync: NotificationSync

    @Inject lateinit var prefs: AppPreferences

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != "android.intent.action.QUICKBOOT_POWERON") {
                return
            }

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val state = prefs.state.first()
                notificationSync.syncAll(state)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
