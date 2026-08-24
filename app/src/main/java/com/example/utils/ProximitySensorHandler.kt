package com.example.utils

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.PowerManager
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Handles device Proximity Sensor during voice & video calls.
 * 1. Acquires PROXIMITY_SCREEN_OFF_WAKE_LOCK to turn off the hardware display when held to ear.
 * 2. Monitors Sensor.TYPE_PROXIMITY to prevent accidental screen touches and conserve battery.
 * 3. Automatically disables proximity locking when speakerphone is activated or call ends.
 */
class ProximitySensorHandler(private val context: Context) : SensorEventListener {

    private val TAG = "ProximitySensorHandler"

    private val powerManager: PowerManager? = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
    private val sensorManager: SensorManager? = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val proximitySensor: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_PROXIMITY)

    private var wakeLock: PowerManager.WakeLock? = null
    private var isListening = false
    private var isSpeakerEnabled = false

    private val _isNear = MutableStateFlow(false)
    val isNear: StateFlow<Boolean> = _isNear.asStateFlow()

    init {
        try {
            if (powerManager != null && powerManager.isWakeLockLevelSupported(PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK)) {
                wakeLock = powerManager.newWakeLock(
                    PowerManager.PROXIMITY_SCREEN_OFF_WAKE_LOCK,
                    "venzoind:proximity_screen_off_lock"
                )
                wakeLock?.setReferenceCounted(false)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Proximity WakeLock: ${e.message}")
        }
    }

    /**
     * Call when a call starts or screen is opened
     */
    fun start(isSpeakerOn: Boolean = false) {
        isSpeakerEnabled = isSpeakerOn
        if (!isListening) {
            isListening = true
            proximitySensor?.let { sensor ->
                sensorManager?.registerListener(
                    this,
                    sensor,
                    SensorManager.SENSOR_DELAY_NORMAL
                )
            }
            manageWakeLock()
            Log.d(TAG, "ProximitySensorHandler started (Speaker: $isSpeakerOn)")
        }
    }

    /**
     * Updates speakerphone state dynamically during call
     */
    fun setSpeakerOn(speakerOn: Boolean) {
        isSpeakerEnabled = speakerOn
        manageWakeLock()
    }

    /**
     * Call when call ends or screen unmounts
     */
    fun stop() {
        if (isListening) {
            isListening = false
            sensorManager?.unregisterListener(this)
            releaseWakeLock()
            _isNear.value = false
            Log.d(TAG, "ProximitySensorHandler stopped")
        }
    }

    private fun manageWakeLock() {
        if (isListening && !isSpeakerEnabled) {
            acquireWakeLock()
        } else {
            releaseWakeLock()
        }
    }

    private fun acquireWakeLock() {
        try {
            if (wakeLock != null && wakeLock?.isHeld == false) {
                wakeLock?.acquire(3 * 60 * 60 * 1000L /* 3 hours max timeout */)
                Log.d(TAG, "Proximity WakeLock acquired")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error acquiring WakeLock: ${e.message}")
        }
    }

    private fun releaseWakeLock() {
        try {
            if (wakeLock != null && wakeLock?.isHeld == true) {
                wakeLock?.release(PowerManager.RELEASE_FLAG_WAIT_FOR_NO_PROXIMITY)
                Log.d(TAG, "Proximity WakeLock released")
            }
        } catch (e: Exception) {
            try {
                if (wakeLock?.isHeld == true) {
                    wakeLock?.release()
                }
            } catch (ignored: Exception) {}
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || event.sensor.type != Sensor.TYPE_PROXIMITY) return
        val distance = event.values.firstOrNull() ?: return
        val maxRange = proximitySensor?.maximumRange ?: 5f

        // Near if distance < max range and distance < 5cm (or binary near < 1cm)
        val near = distance < maxRange && distance < 4.0f
        _isNear.value = near
        Log.d(TAG, "Proximity sensor value: $distance (max: $maxRange) -> isNear: $near")
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // No-op
    }
}
