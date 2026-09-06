package com.lumex.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lumex.app.exposure.ExposureMath
import com.lumex.app.exposure.Stops
import com.lumex.app.meter.LightMeter
import com.lumex.app.meter.LightReading
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.util.Locale

/** User's exposure inputs; combined with live/held readings into [MeterUiState]. */
private data class MeterParams(
    val iso: Int,
    val mode: PriorityMode,
    val aperture: Double,
    val shutter: Double
)

class MeterViewModel(private val meter: LightMeter) : ViewModel() {

    companion object {
        const val DEFAULT_ISO = 100
        const val DEFAULT_APERTURE = 8.0
        const val DEFAULT_SHUTTER_SECONDS = 1.0 / 128
        val DEFAULT_MODE = PriorityMode.APERTURE
    }

    private val _iso = MutableStateFlow(DEFAULT_ISO)
    private val _mode = MutableStateFlow(DEFAULT_MODE)
    private val _aperture = MutableStateFlow(DEFAULT_APERTURE)
    private val _shutter = MutableStateFlow(DEFAULT_SHUTTER_SECONDS)
    private val _held = MutableStateFlow<LightReading?>(null)

    private val paramsFlow = combine(_iso, _mode, _aperture, _shutter) {
            iso: Int, mode: PriorityMode, aperture: Double, shutter: Double ->
        MeterParams(iso, mode, aperture, shutter)
    }

    val uiState: StateFlow<MeterUiState> = combine(
        paramsFlow, _held, meter.readings
    ) { params: MeterParams, held: LightReading?, live: LightReading? ->
        buildUi(params, held, live, meter.hasSensor)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        buildUi(
            MeterParams(DEFAULT_ISO, DEFAULT_MODE, DEFAULT_APERTURE, DEFAULT_SHUTTER_SECONDS),
            held = null,
            live = null,
            hasSensor = meter.hasSensor
        )
    )

    init {
        meter.start()
    }

    override fun onCleared() {
        meter.stop()
    }

    fun selectIso(iso: Int) {
        require(iso > 0) { "iso must be positive, was $iso" }
        _iso.value = iso
    }

    fun selectMode(mode: PriorityMode) {
        _mode.value = mode
    }

    fun selectAperture(fNumber: Double) {
        require(fNumber.isFinite() && fNumber > 0.0) { "aperture must be positive and finite" }
        _aperture.value = fNumber
    }

    fun selectShutter(seconds: Double) {
        require(seconds.isFinite() && seconds > 0.0) { "shutter must be positive and finite" }
        _shutter.value = seconds
    }

    fun toggleHold() {
        _held.value = if (_held.value != null) null else meter.readings.value
    }

    private fun buildUi(
        params: MeterParams,
        held: LightReading?,
        live: LightReading?,
        hasSensor: Boolean
    ): MeterUiState {
        if (!hasSensor) {
            return MeterUiState(
                hasSensor = false,
                mode = params.mode, iso = params.iso,
                aperture = params.aperture, shutterSeconds = params.shutter,
                headline = "No sensor",
                detail = "This device has no ambient light sensor"
            )
        }
        val lux = held?.lux ?: live?.lux
        if (lux == null) {
            return MeterUiState(
                mode = params.mode, iso = params.iso,
                aperture = params.aperture, shutterSeconds = params.shutter,
                headline = "···",
                detail = "Point the phone at the scene"
            )
        }
        if (lux <= 0f) {
            return MeterUiState(
                lux = lux, holding = held != null,
                mode = params.mode, iso = params.iso,
                aperture = params.aperture, shutterSeconds = params.shutter,
                headline = "Too dark",
                detail = "Below sensor range"
            )
        }
        val ev = ExposureMath.evAtIso(ExposureMath.ev100FromLux(lux.toDouble()), params.iso)
        val lightInfo = "${formatLux(lux)} · EV ${formatEv(ev)} · ISO ${params.iso}"
        return when (params.mode) {
            PriorityMode.APERTURE -> {
                val solution = ExposureMath.solveShutter(params.aperture, ev)
                MeterUiState(
                    lux = lux, holding = held != null,
                    mode = params.mode, iso = params.iso,
                    aperture = params.aperture, shutterSeconds = params.shutter,
                    caption = "Shutter speed",
                    headline = Stops.formatShutter(solution.snappedSeconds),
                    detail = "exact ${Stops.formatShutter(solution.exactSeconds)}" +
                        " · ${Stops.formatAperture(params.aperture)} · $lightInfo",
                    residualEv = solution.residualEv,
                    clipped = solution.clipped
                )
            }
            PriorityMode.SHUTTER -> {
                val solution = ExposureMath.solveAperture(params.shutter, ev)
                MeterUiState(
                    lux = lux, holding = held != null,
                    mode = params.mode, iso = params.iso,
                    aperture = params.aperture, shutterSeconds = params.shutter,
                    caption = "Aperture",
                    headline = Stops.formatAperture(solution.snappedFNumber),
                    detail = "exact ${formatExactAperture(solution.exactFNumber)}" +
                        " · ${Stops.formatShutter(params.shutter)} · $lightInfo",
                    residualEv = solution.residualEv,
                    clipped = solution.clipped
                )
            }
        }
    }

    private fun formatLux(lux: Float): String =
        if (lux < 100f) String.format(Locale.US, "%.1f lx", lux)
        else String.format(Locale.US, "%.0f lx", lux)

    private fun formatEv(ev: Double): String =
        String.format(Locale.US, "%.1f", ev)

    private fun formatExactAperture(fNumber: Double): String =
        Stops.formatAperture(fNumber)
}
