package com.lumex.app.exposure

import kotlin.math.roundToInt

/**
 * Standard full-stop scales for aperture, shutter speed and ISO.
 *
 * Shutter values are exact powers of two (seconds); [formatShutter] maps them
 * to the conventional camera labels (1/16s is labelled "1/15", etc.).
 */
object Stops {

    /** Full-stop apertures (f-numbers). */
    val APERTURES = doubleArrayOf(
        1.0, 1.4, 2.0, 2.8, 4.0, 5.6, 8.0, 11.0, 16.0, 22.0, 32.0
    )

    /** Full-stop shutter speeds, in seconds (exact powers of two). */
    val SHUTTER_SPEEDS = doubleArrayOf(
        4.0, 2.0, 1.0,
        1.0 / 2, 1.0 / 4, 1.0 / 8, 1.0 / 16,
        1.0 / 32, 1.0 / 64, 1.0 / 128, 1.0 / 256,
        1.0 / 512, 1.0 / 1024, 1.0 / 2048, 1.0 / 4096
    )

    /** Standard full-stop ISO values. */
    val ISO_VALUES = intArrayOf(50, 100, 200, 400, 800, 1600, 3200, 6400)

    private val SHUTTER_LABELS: Map<Double, String> = mapOf(
        4.0 to "4s",
        2.0 to "2s",
        1.0 to "1s",
        1.0 / 2 to "1/2",
        1.0 / 4 to "1/4",
        1.0 / 8 to "1/8",
        1.0 / 16 to "1/15",
        1.0 / 32 to "1/30",
        1.0 / 64 to "1/60",
        1.0 / 128 to "1/125",
        1.0 / 256 to "1/250",
        1.0 / 512 to "1/500",
        1.0 / 1024 to "1/1000",
        1.0 / 2048 to "1/2000",
        1.0 / 4096 to "1/4000"
    )

    fun formatAperture(fNumber: Double): String =
        if (fNumber % 1.0 == 0.0) "f/${fNumber.toInt()}" else "f/$fNumber"

    fun formatShutter(seconds: Double): String {
        SHUTTER_LABELS[seconds]?.let { return it }
        // Fallback for non-detent values (shouldn't normally happen).
        return if (seconds >= 1.0) "${seconds}s" else "1/${(1.0 / seconds).roundToInt()}"
    }
}
