package com.lumex.app.exposure

import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Standard full-stop scales for aperture, shutter speed and ISO.
 *
 * Shutter values are exact powers of two (seconds); [formatShutter] maps them
 * to the conventional camera labels (1/16s is labelled "1/15", etc.).
 *
 * Lists are immutable snapshots — callers cannot mutate global state.
 */
object Stops {

    /** Full-stop apertures (f-numbers). */
    val APERTURES: List<Double> = listOf(
        1.0, 1.4, 2.0, 2.8, 4.0, 5.6, 8.0, 11.0, 16.0, 22.0, 32.0
    )

    /** Full-stop shutter speeds, in seconds (exact powers of two). */
    val SHUTTER_SPEEDS: List<Double> = listOf(
        4.0, 2.0, 1.0,
        1.0 / 2, 1.0 / 4, 1.0 / 8, 1.0 / 16,
        1.0 / 32, 1.0 / 64, 1.0 / 128, 1.0 / 256,
        1.0 / 512, 1.0 / 1024, 1.0 / 2048, 1.0 / 4096
    )

    /** Standard full-stop ISO values. */
    val ISO_VALUES: List<Int> = listOf(50, 100, 200, 400, 800, 1600, 3200, 6400)

    /** Bounds of the standard scales (cached so solvers don't scan per reading). */
    val MIN_APERTURE: Double = APERTURES.min()
    val MAX_APERTURE: Double = APERTURES.max()
    val MIN_SHUTTER_SECONDS: Double = SHUTTER_SPEEDS.min()
    val MAX_SHUTTER_SECONDS: Double = SHUTTER_SPEEDS.max()

    /**
     * Shutter detents paired with their conventional labels.
     * Position in this list is the source of truth — no [Double] map keys,
     * so lookup never depends on floating-point equality of computed values.
     */
    private val SHUTTER_DETENTS: List<Pair<Double, String>> = listOf(
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

    /** Relative tolerance for matching a value to a detent (0.1%). */
    private const val DETENT_TOLERANCE = 1e-3

    fun formatAperture(fNumber: Double): String {
        require(fNumber.isFinite()) { "fNumber must be finite" }
        return if (isWholeStop(fNumber)) {
            "f/${fNumber.roundToInt()}"
        } else {
            // One decimal is the camera convention (f/1.4, f/5.6); never raw Double.toString().
            String.format(Locale.US, "f/%.1f", fNumber)
        }
    }

    fun formatShutter(seconds: Double): String {
        require(seconds.isFinite() && seconds > 0.0) { "shutter must be positive and finite" }
        detentLabel(seconds)?.let { return it }
        // Fallback for non-detent values (shouldn't normally happen).
        return if (seconds >= 1.0) {
            "${trimTrailingZero(String.format(Locale.US, "%.1f", seconds))}s"
        } else {
            "1/${(1.0 / seconds).roundToInt()}"
        }
    }

    /** Conventional label when [seconds] matches a detent within tolerance, else null. */
    private fun detentLabel(seconds: Double): String? =
        SHUTTER_DETENTS.firstOrNull { (detent, _) ->
            abs(detent - seconds) <= DETENT_TOLERANCE * detent
        }?.second

    private fun isWholeStop(fNumber: Double): Boolean =
        abs(fNumber - fNumber.roundToInt()) < 1e-9

    private fun trimTrailingZero(formatted: String): String =
        if (formatted.endsWith(".0")) formatted.dropLast(2) else formatted
}
