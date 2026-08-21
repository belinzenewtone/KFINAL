package com.belinze.lifeos.util

import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator

/**
 * Haptic feedback helper — 1:1 port of src/services/haptics.ts.
 *
 * Fires a vibration pulse only when the user has enabled haptic feedback in
 * Settings (mirrors the RN `haptic()` gate). Kinds map to the RN Vibration
 * fallback patterns so behaviour matches on Android:
 *
 *   light  → 10ms single pulse (impactAsync Light)
 *   medium → 20ms
 *   heavy  → 40ms
 *   success → [0, 15, 60, 15]
 *   warning → [0, 30, 60, 30]
 *   error   → [0, 40, 60, 40, 60, 40]
 *
 * Plain object (not Hilt-managed) — [init] is called from Application.onCreate
 * so call sites can fire pulses without DI ceremony.
 */
object Haptics {
    private var appContext: Context? = null

    /** Set by the app at startup from the persisted preference; updated by Settings. */
    @Volatile
    var enabled: Boolean = true

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    fun light() = pulse(LIGHT)

    fun medium() = pulse(MEDIUM)

    fun heavy() = pulse(HEAVY)

    fun success() = pulse(SUCCESS)

    fun warning() = pulse(WARNING)

    fun error() = pulse(ERROR)

    private fun pulse(pattern: LongArray) {
        if (!enabled) return
        val context = appContext ?: return
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator ?: return
        try {
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
        } catch (_: Exception) {
            // Haptics are best-effort; never crash on unsupported devices.
        }
    }

    private val LIGHT   = longArrayOf(10)
    private val MEDIUM  = longArrayOf(20)
    private val HEAVY   = longArrayOf(40)
    private val SUCCESS = longArrayOf(0, 15, 60, 15)
    private val WARNING = longArrayOf(0, 30, 60, 30)
    private val ERROR   = longArrayOf(0, 40, 60, 40, 60, 40)
}
