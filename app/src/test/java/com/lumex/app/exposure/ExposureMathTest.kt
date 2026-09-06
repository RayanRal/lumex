package com.lumex.app.exposure

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExposureMathTest {

    private val delta = 1e-9

    // --- lux <-> EV -------------------------------------------------------

    @Test
    fun `bright sun is about EV15 at ISO 100`() {
        // 100 000 lux incident -> log2(100000*100/250) ~= 15.29
        assertEquals(15.29, ExposureMath.ev100FromLux(100_000.0), 0.01)
    }

    @Test
    fun `2_5 lux is EV0 at ISO 100`() {
        assertEquals(0.0, ExposureMath.ev100FromLux(2.5), delta)
    }

    @Test
    fun `each halving of light is one stop`() {
        assertEquals(-1.0, ExposureMath.ev100FromLux(1.25), delta)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `zero lux is rejected`() {
        ExposureMath.ev100FromLux(0.0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `negative lux is rejected`() {
        ExposureMath.ev100FromLux(-5.0)
    }

    @Test
    fun `calibration offset shifts EV`() {
        assertEquals(1.0, ExposureMath.ev100FromLux(2.5, calibrationEv = 1.0), delta)
    }

    @Test
    fun `lux round-trips through EV`() {
        val lux = 12_345.0
        assertEquals(lux, ExposureMath.luxFromEv100(ExposureMath.ev100FromLux(lux)), 1e-6)
    }

    @Test
    fun `ISO doubling adds one stop`() {
        assertEquals(16.0, ExposureMath.evAtIso(15.0, 200), delta)
        assertEquals(14.0, ExposureMath.evAtIso(15.0, 50), delta)
    }

    // --- Sunny-16 sanity ---------------------------------------------------

    @Test
    fun `sunny 16 - f16 at EV15 gives 1 over 125`() {
        // Exact: 16^2 / 2^15 = 1/128 s, labelled "1/125".
        val solution = ExposureMath.solveShutter(aperture = 16.0, ev = 15.0)
        assertEquals(1.0 / 128, solution.exactSeconds, delta)
        assertEquals(1.0 / 128, solution.snappedSeconds, delta)
        assertEquals(0.0, solution.residualEv, delta)
        assertFalse(solution.clipped)
        assertEquals("1/125", Stops.formatShutter(solution.snappedSeconds))
    }

    @Test
    fun `sunny 16 mirrored - 1 over 125 at EV15 gives f16`() {
        val solution = ExposureMath.solveAperture(shutterSeconds = 1.0 / 128, ev = 15.0)
        assertEquals(16.0, solution.exactFNumber, 1e-6)
        assertEquals(16.0, solution.snappedFNumber, delta)
        assertEquals(0.0, solution.residualEv, delta)
        assertFalse(solution.clipped)
    }

    // --- snapping ----------------------------------------------------------

    @Test
    fun `aperture snaps to nearest full stop`() {
        // 6.7 is ~0.18 stops above f/5.6 and ~0.18 below f/8 — lands on f/8.
        assertEquals(8.0, ExposureMath.nearestAperture(6.7), delta)
        assertEquals(5.6, ExposureMath.nearestAperture(5.0), delta)
    }

    @Test
    fun `shutter snaps to nearest full stop`() {
        assertEquals(1.0 / 64, ExposureMath.nearestShutter(1.0 / 70), delta)
    }

    @Test
    fun `residual shows overexposure when snapped slower`() {
        // Ideal is ~1/84 s; snapped 1/64 is ~+0.4 stops slow (more light).
        val solution = ExposureMath.solveShutter(aperture = 8.0, ev = 12.4)
        assertEquals(1.0 / 64, solution.snappedSeconds, delta)
        assertTrue(solution.residualEv > 0.0)
    }

    // --- range clipping ----------------------------------------------------

    @Test
    fun `blinding light clips at fastest shutter`() {
        val solution = ExposureMath.solveShutter(aperture = 1.0, ev = 25.0)
        assertEquals(1.0 / 4096, solution.snappedSeconds, delta)
        assertTrue(solution.clipped)
    }

    @Test
    fun `darkness clips at slowest shutter`() {
        val solution = ExposureMath.solveShutter(aperture = 32.0, ev = -4.0)
        assertEquals(4.0, solution.snappedSeconds, delta)
        assertTrue(solution.clipped)
    }

    @Test
    fun `darkness clips at widest aperture`() {
        val solution = ExposureMath.solveAperture(shutterSeconds = 4.0, ev = -4.0)
        assertEquals(1.0, solution.snappedFNumber, delta)
        assertTrue(solution.clipped)
    }

    // --- labels ------------------------------------------------------------

    @Test
    fun `shutter labels use conventional values`() {
        assertEquals("1/125", Stops.formatShutter(1.0 / 128))
        assertEquals("1/60", Stops.formatShutter(1.0 / 64))
        assertEquals("1/2", Stops.formatShutter(1.0 / 2))
        assertEquals("1s", Stops.formatShutter(1.0))
        assertEquals("2s", Stops.formatShutter(2.0))
    }

    @Test
    fun `aperture labels`() {
        assertEquals("f/16", Stops.formatAperture(16.0))
        assertEquals("f/1.4", Stops.formatAperture(1.4))
        assertEquals("f/5.6", Stops.formatAperture(5.6))
    }

    // --- input validation --------------------------------------------------

    @Test(expected = IllegalArgumentException::class)
    fun `infinite lux is rejected`() {
        ExposureMath.ev100FromLux(Double.POSITIVE_INFINITY)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `NaN lux is rejected`() {
        ExposureMath.ev100FromLux(Double.NaN)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `NaN calibration is rejected`() {
        ExposureMath.ev100FromLux(100.0, calibrationEv = Double.NaN)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `NaN ev100 is rejected by luxFromEv100`() {
        ExposureMath.luxFromEv100(Double.NaN)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `infinite ev100 is rejected by luxFromEv100`() {
        ExposureMath.luxFromEv100(Double.POSITIVE_INFINITY)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `non-positive ISO is rejected`() {
        ExposureMath.evAtIso(10.0, 0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `NaN ev is rejected by evAtIso`() {
        ExposureMath.evAtIso(Double.NaN, 100)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `non-positive aperture is rejected`() {
        ExposureMath.exposureTimeSeconds(0.0, 10.0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `non-positive shutter is rejected`() {
        ExposureMath.apertureForShutter(-1.0, 10.0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `nearest shutter rejects non-positive`() {
        ExposureMath.nearestShutter(0.0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `nearest aperture rejects NaN`() {
        ExposureMath.nearestAperture(Double.NaN)
    }

    // --- label edge cases ----------------------------------------------------

    @Test
    fun `shutter fallback formats non-detent values`() {
        // 1/100s is not a full-stop detent: falls back to nearest 1/N label.
        assertEquals("1/100", Stops.formatShutter(1.0 / 100))
        // Non-detent long exposure formats with one decimal, no float noise.
        assertEquals("1.5s", Stops.formatShutter(1.5))
        assertEquals("4s", Stops.formatShutter(4.0))
    }

    @Test
    fun `shutter detent tolerates float noise`() {
        // Computed 1/128 rarely bit-equals the literal; tolerance keeps the label.
        assertEquals("1/125", Stops.formatShutter(1.0 / 128 * (1 + 1e-6)))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `shutter rejects non-positive`() {
        Stops.formatShutter(0.0)
    }

    @Test
    fun `aperture formats whole stops without decimal`() {
        assertEquals("f/8", Stops.formatAperture(8.0))
        assertEquals("f/2.8", Stops.formatAperture(2.8))
    }

    // --- solver semantics ----------------------------------------------------

    @Test
    fun `aperture residual is negative when snapped narrower`() {
        // Exact is ~f/20.2, snapped f/22 lets in less light -> underexposure.
        val solution = ExposureMath.solveAperture(shutterSeconds = 1.0 / 80, ev = 15.0)
        assertEquals(22.0, solution.snappedFNumber, delta)
        assertTrue(solution.residualEv < 0.0)
    }

    @Test
    fun `scale bounds match table extremes`() {
        assertEquals(Stops.SHUTTER_SPEEDS.min(), Stops.MIN_SHUTTER_SECONDS, delta)
        assertEquals(Stops.SHUTTER_SPEEDS.max(), Stops.MAX_SHUTTER_SECONDS, delta)
        assertEquals(Stops.APERTURES.min(), Stops.MIN_APERTURE, delta)
        assertEquals(Stops.APERTURES.max(), Stops.MAX_APERTURE, delta)
    }
}
