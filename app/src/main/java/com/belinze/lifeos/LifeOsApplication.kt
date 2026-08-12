package com.belinze.lifeos

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.lifeos.sms.SmsService
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Application entry point.
 *
 * @HiltAndroidApp triggers Hilt's code generation and sets up the component hierarchy.
 * Implements [Configuration.Provider] so Hilt can inject its WorkerFactory into WorkManager
 * (required for WorkManager workers that use @Inject constructor).
 */
@HiltAndroidApp
class LifeOsApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    /** Provided by SmsModule; initialised before any Activity or Worker runs. */
    @Inject
    lateinit var smsService: SmsService

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()

        // Arm the background SMS BroadcastReceiver, ensure the SmsReceiverModule
        // Compose stub is live, and schedule the periodic ingest sweep worker.
        smsService.initialize()
    }
}
