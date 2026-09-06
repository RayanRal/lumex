package com.lumex.app.ui

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.lumex.app.meter.LightMeter
import com.lumex.app.meter.SensorLightMeter

/**
 * Creates [MeterViewModel] with a [LightMeter].
 *
 * @param meterFactory seam for tests / future camera meter; defaults to the
 *   ambient-light sensor. Takes [Application] so the meter gets an
 *   application context (no activity leak).
 */
class MeterViewModelFactory(
    private val app: Application,
    private val meterFactory: (Application) -> LightMeter = ::SensorLightMeter
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MeterViewModel::class.java)) {
            return MeterViewModel(meterFactory(app)) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
