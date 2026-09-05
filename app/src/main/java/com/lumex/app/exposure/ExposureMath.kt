package com.lumex.app.exposure

import kotlin.math.log2
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * - EV values follow the APEX system: EV = log2(N^2 / t) at ISO 100.
 * - `residualEv` on solutions is the overexposure (+) / underexposure (-)
 *   in stops that results from using the snapped standard setting instead
 *   of the exact computed value. The needle scale displays this directly.
 */
object ExposureMath {

    /**
     * Incident-light calibration constant. C=250 lux aligns with the
     * Sunny-16 rule (100k lux bright sun -> ~EV 15 at ISO 100).
     * Per-device tuning happens via the `calibrationEv` offset, not here.
     */
    const val INCIDENT_CALIBRATION_CONSTANT = 250.0

    const val REFERENCE_ISO = 100

    /** EV at ISO 100 from incident illuminance in lux. */
    fun ev100FromLux(lux: Double, calibrationEv: Double = 0.0): Double {
        require(lux > 0.0 && lux.isFinite()) { "lux must be positive, was $lux" }
        require(calibrationEv.isFinite()) { "calibrationEv must be finite" }
        return log2(lux * REFERENCE_ISO / INCIDENT_CALIBRATION_CONSTANT) + calibrationEv
    }

    /**
     * Inverse of [ev100FromLux] (without calibration offset).
     * Lets future meter sources (e.g. camera-based) report equivalent lux.
     */
    fun luxFromEv100(ev100: Double): Double {
        require(ev100.isFinite()) { "ev100 must be finite" }
        return 2.0.pow(ev100) * INCIDENT_CALIBRATION_CONSTANT / REFERENCE_ISO
    }

    /** Shift an ISO-100 EV to the working film ISO. */
    fun evAtIso(ev100: Double, iso: Int): Double {
        require(ev100.isFinite()) { "ev100 must be finite" }
        require(iso > 0) { "iso must be positive, was $iso" }
        return ev100 + log2(iso.toDouble() / REFERENCE_ISO)
    }

    /** Exact exposure time (seconds) for aperture [aperture] at [ev]. */
    fun exposureTimeSeconds(aperture: Double, ev: Double): Double {
        require(aperture > 0.0 && aperture.isFinite()) { "aperture must be positive" }
        require(ev.isFinite()) { "ev must be finite" }
        return aperture * aperture / 2.0.pow(ev)
    }

    /** Exact aperture (f-number) for a shutter speed at [ev]. */
    fun apertureForShutter(shutterSeconds: Double, ev: Double): Double {
        require(shutterSeconds > 0.0 && shutterSeconds.isFinite()) { "shutter must be positive" }
        require(ev.isFinite()) { "ev must be finite" }
        return sqrt(2.0.pow(ev) * shutterSeconds)
    }

    fun nearestShutter(seconds: Double): Double = nearest(Stops.SHUTTER_SPEEDS, seconds)

    fun nearestAperture(fNumber: Double): Double = nearest(Stops.APERTURES, fNumber)

    data class ShutterSolution(
        val exactSeconds: Double,
        val snappedSeconds: Double,
        /** Stops of over (+) / under (-) exposure from using [snappedSeconds]. */
        val residualEv: Double,
        /** True when the exact value fell outside the standard scale. */
        val clipped: Boolean
    )

    data class ApertureSolution(
        val exactFNumber: Double,
        val snappedFNumber: Double,
        /** Stops of over (+) / under (-) exposure from using [snappedFNumber]. */
        val residualEv: Double,
        /** True when the exact value fell outside the standard scale. */
        val clipped: Boolean
    )

    /**
     * Aperture-priority: user fixes [aperture], we compute the shutter speed.
     * [ev] is the metered scene EV already shifted to the working ISO.
     */
    fun solveShutter(aperture: Double, ev: Double): ShutterSolution {
        val exact = exposureTimeSeconds(aperture, ev)
        val snapped = nearestShutter(exact)
        return ShutterSolution(
            exactSeconds = exact,
            snappedSeconds = snapped,
            residualEv = log2(snapped / exact),
            clipped = exact < Stops.SHUTTER_SPEEDS.min() || exact > Stops.SHUTTER_SPEEDS.max()
        )
    }

    /**
     * Shutter-priority: user fixes [shutterSeconds], we compute the aperture.
     * [ev] is the metered scene EV already shifted to the working ISO.
     */
    fun solveAperture(shutterSeconds: Double, ev: Double): ApertureSolution {
        val exact = apertureForShutter(shutterSeconds, ev)
        val snapped = nearestAperture(exact)
        return ApertureSolution(
            exactFNumber = exact,
            snappedFNumber = snapped,
            // Exposure scales with (exact/snapped)^2 for aperture changes.
            residualEv = 2.0 * log2(exact / snapped),
            clipped = exact < Stops.APERTURES.min() || exact > Stops.APERTURES.max()
        )
    }

    /** Nearest table entry by distance in stops (log domain). */
    private fun nearest(table: DoubleArray, value: Double): Double {
        require(value > 0.0 && value.isFinite()) { "value must be positive" }
        val logValue = kotlin.math.ln(value)
        return table.minBy { kotlin.math.abs(kotlin.math.ln(it) - logValue) }
    }
}
