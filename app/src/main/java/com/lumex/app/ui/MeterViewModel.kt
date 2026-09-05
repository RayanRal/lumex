package com.lumex.app.ui

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lumex.app.exposure.ExposureMath
import com.lumex.app.exposure.Stops
import com.lumex.app.meter.LightMeter
import com.lumex.app.meter.LightReading
import com.lumex.app.meter.SensorLightMeter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.util.Locale

enum class PriorityMode { APERTURE, SHUTTER }

private data class MeterParams(
    val iso: Int,
    val mode: PriorityMode,
    val aperture: Double,
    val shutter: Double
)

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
)

class MeterViewModel(private val meter: LightMeter) : ViewModel() {

    private val _iso = MutableStateFlow(100)
    private val _mode = MutableStateFlow(PriorityMode.APERTURE)
    private val _aperture = MutableStateFlow(8.0)
    private val _shutter = MutableStateFlow(1.0 / 128)
    private val _held = MutableStateFlow<LightReading?>(null)

    val uiState: StateFlow<MeterUiState> = combine(
        _iso, _mode, _aperture, _shutter
    ) { iso: Int, mode: PriorityMode, aperture: Double, shutter: Double ->
        MeterParams(iso, mode, aperture, shutter)
    }.combine(_held) { params: MeterParams, held: LightReading? ->
        params to held
    }.combine(meter.readings) { paramsAndHeld: Pair<MeterParams, LightReading?>, live: LightReading? ->
        val (params, held) = paramsAndHeld
        buildUi(params.iso, params.mode, params.aperture, params.shutter, held, live, meter.hasSensor)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        buildUi(100, PriorityMode.APERTURE, 8.0, 1.0 / 128, null, null, meter.hasSensor)
    )

    init {
        meter.start()
    }

    override fun onCleared() {
        meter.stop()
    }

    fun selectIso(iso: Int) {
        _iso.value = iso
    }

    fun selectMode(mode: PriorityMode) {
        _mode.value = mode
    }

    fun selectAperture(fNumber: Double) {
        _aperture.value = fNumber
    }

    fun selectShutter(seconds: Double) {
        _shutter.value = seconds
    }

    fun toggleHold() {
        _held.value = if (_held.value != null) null else meter.readings.value
    }

    private fun buildUi(
        iso: Int,
        mode: PriorityMode,
        aperture: Double,
        shutter: Double,
        held: LightReading?,
        live: LightReading?,
        hasSensor: Boolean
    ): MeterUiState {
        if (!hasSensor) {
            return MeterUiState(
                hasSensor = false,
                mode = mode, iso = iso, aperture = aperture, shutterSeconds = shutter,
                headline = "No sensor",
                detail = "This device has no ambient light sensor"
            )
        }
        val lux = held?.lux ?: live?.lux
        if (lux == null) {
            return MeterUiState(
                mode = mode, iso = iso, aperture = aperture, shutterSeconds = shutter,
                headline = "···",
                detail = "Point the phone at the scene"
            )
        }
        if (lux <= 0f) {
            return MeterUiState(
                lux = lux, holding = held != null,
                mode = mode, iso = iso, aperture = aperture, shutterSeconds = shutter,
                headline = "Too dark",
                detail = "Below sensor range"
            )
        }
        val ev = ExposureMath.evAtIso(ExposureMath.ev100FromLux(lux.toDouble()), iso)
        val lightInfo = "${formatLux(lux)} · EV ${formatEv(ev)} · ISO $iso"
        return when (mode) {
            PriorityMode.APERTURE -> {
                val solution = ExposureMath.solveShutter(aperture, ev)
                MeterUiState(
                    lux = lux, holding = held != null,
                    mode = mode, iso = iso, aperture = aperture, shutterSeconds = shutter,
                    caption = "Shutter speed",
                    headline = Stops.formatShutter(solution.snappedSeconds),
                    detail = "exact ${Stops.formatShutter(solution.exactSeconds)}" +
                        " · ${Stops.formatAperture(aperture)} · $lightInfo",
                    residualEv = solution.residualEv,
                    clipped = solution.clipped
                )
            }
            PriorityMode.SHUTTER -> {
                val solution = ExposureMath.solveAperture(shutter, ev)
                MeterUiState(
                    lux = lux, holding = held != null,
                    mode = mode, iso = iso, aperture = aperture, shutterSeconds = shutter,
                    caption = "Aperture",
                    headline = Stops.formatAperture(solution.snappedFNumber),
                    detail = "exact ${formatExactAperture(solution.exactFNumber)}" +
                        " · ${Stops.formatShutter(shutter)} · $lightInfo",
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
        String.format(Locale.US, "f/%.1f", fNumber)
}

class MeterViewModelFactory(private val app: Application) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return MeterViewModel(SensorLightMeter(app)) as T
    }
}
