package com.lumex.app.meter

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.SystemClock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicBoolean

/**
 * [LightMeter] backed by the ambient light sensor (TYPE_LIGHT).
 * Needs no runtime permission. Pass an application context.
 */
class SensorLightMeter(context: Context) : LightMeter, SensorEventListener {

    private val sensorManager =
        context.applicationContext.getSystemService(SensorManager::class.java)
    private val lightSensor: Sensor? =
        sensorManager?.getDefaultSensor(Sensor.TYPE_LIGHT)

    override val hasSensor: Boolean
        get() = lightSensor != null

    private val _readings = MutableStateFlow<LightReading?>(null)
    override val readings: StateFlow<LightReading?> = _readings.asStateFlow()

    private val started = AtomicBoolean(false)

    override fun start() {
        if (!started.compareAndSet(false, true)) return
        val sensor = lightSensor
        if (sensor == null) {
            started.set(false)
            return
        }
        sensorManager?.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL)
    }

    override fun stop() {
        if (!started.compareAndSet(true, false)) return
        sensorManager?.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_LIGHT) return
        if (event.values.isEmpty()) return
        val lux = event.values[0]
        if (!lux.isFinite()) return
        // Skip duplicates so a stable scene doesn't recompose the UI at sensor rate.
        // StateFlow dedupes equal values, but a fresh timestamp would defeat that.
        if (_readings.value?.lux == lux) return
        _readings.value = LightReading(
            lux = lux,
            // Monotonic clock: immune to NTP / timezone / user changes.
            // Base is millis since boot (SystemClock.elapsedRealtime).
            timestampMillis = SystemClock.elapsedRealtime()
        )
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}
