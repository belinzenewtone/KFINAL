package com.lifeos.sms

import android.util.Log

/**
 * Compose-side replacement for the React Native SmsReceiverModule bridge.
 *
 * The original SmsReceiverModule.kt (in modules/lifeos-sms/android/) is a React Native
 * ReactContextBaseJavaModule and is excluded from the :sms sourceSets in the Compose build.
 * SmsProcessWorker calls `SmsReceiverModule.instance?.emitNewTransaction(tx)` — this stub
 * satisfies that API without any RN dependency, routing events to [SmsEventBus] instead.
 *
 * The `companion object { var instance }` pattern is unchanged so SmsProcessWorker compiles
 * without modification.
 */
class SmsReceiverModule {

    /**
     * Called by SmsProcessWorker after a successful transaction insert.
     * Routes to SmsEventBus so Compose ViewModels can refresh their UI.
     */
    fun emitNewTransaction(tx: SmsParser.ParsedTransaction) {
        Log.d(TAG, "emitNewTransaction: ${tx.mpesaCode} amount=${tx.amount} cat=${tx.category}")
        SmsEventBus.notifyNewTransaction(tx)
    }

    /**
     * Called by SmsProcessWorker when a Fuliza charge arrives but the user has
     * not configured their Fuliza limit. Forwards the outstanding balance.
     */
    fun emitFulizaLimitNeeded(outstanding: Double, type: String) {
        Log.d(TAG, "emitFulizaLimitNeeded: outstanding=$outstanding type=$type")
        SmsEventBus.notifyFulizaLimitNeeded(outstanding)
    }

    companion object {
        private const val TAG = "LifeOS/SmsReceiverModule"

        /**
         * Singleton instance read by SmsProcessWorker. Set to a live instance in
         * [SmsService.initialize] so events are routed from parser startup onward.
         *
         * Intentionally initialised with a default instance — any SMS processed
         * before Application.onCreate() (edge cases on some OEMs) still emits events
         * through the bus rather than being silently dropped.
         */
        @JvmField
        var instance: SmsReceiverModule? = SmsReceiverModule()
    }
}
