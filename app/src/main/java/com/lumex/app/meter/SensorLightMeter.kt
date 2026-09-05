package com.lumex.app.meter

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

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

    private var started = false

    @Synchronized
    override fun start() {
        if (started || lightSensor == null) return
        sensorManager?.registerListener(this, lightSensor, SensorManager.SENSOR_DELAY_NORMAL)
        started = true
    }

    @Synchronized
    override fun stop() {
        if (!started) return
        sensorManager?.unregisterListener(this)
        started = false
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_LIGHT) return
        _readings.value = LightReading(
            lux = event.values[0],
            timestampMillis = System.currentTimeMillis()
        )
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}
