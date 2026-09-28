package com.belinze.lifeos.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.belinze.lifeos.data.datastore.AppPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class BootReceiver : BroadcastReceiver(), KoinComponent {

    private val notificationSync: NotificationSync by inject()
    private val prefs: AppPreferences by inject()

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
