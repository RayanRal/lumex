package com.lumex.app.meter

import kotlinx.coroutines.flow.StateFlow

/**
 * A single light measurement as illuminance in lux.
 *
 * Sources that natively measure EV rather than lux (e.g. a future
 * camera-based meter) must convert via
 * `ExposureMath.luxFromEv100(...)` so all implementations stay
 * interchangeable.
 */
data class LightReading(
    val lux: Float,
    val timestampMillis: Long
)

/**
 * Source of live light measurements.
 *
 * v1 implementation: [SensorLightMeter] (ambient light sensor, no permission).
 * Future: a camera-based meter implementing this same interface — the
 * exposure solver and UI only ever see [LightReading].
 */
interface LightMeter {

    /** False on devices without the required hardware (reading stays null). */
    val hasSensor: Boolean

    /** Latest reading, or null if none yet / sensor missing. */
    val readings: StateFlow<LightReading?>

    fun start()

    fun stop()
}
