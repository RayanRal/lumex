package com.lumex.app.ui

data class MeterUiState(
    val hasSensor: Boolean = true,
    val lux: Float? = null,
    val holding: Boolean = false,
    val mode: PriorityMode = PriorityMode.APERTURE,
    val iso: Int = 100,
    val aperture: Double = 8.0,
    val shutterSeconds: Double = 1.0 / 128,
    val caption: String = "",
    val headline: String = "···",
    val detail: String = "Point the phone at the scene",
    val residualEv: Double? = null,
    val clipped: Boolean = false
) {
    /** True when the meter produced a live or held exposure solution. */
    val hasSolution: Boolean get() = residualEv != null

    /** True when controls should be interactive (sensor present and light readable). */
    val controlsEnabled: Boolean get() = hasSensor && lux != null && lux > 0f
}
