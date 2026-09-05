package com.lumex.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lumex.app.ui.MeterScreen
import com.lumex.app.ui.MeterViewModel
import com.lumex.app.ui.MeterViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val vm: MeterViewModel = viewModel(
                        factory = MeterViewModelFactory(application)
                    )
                    val state by vm.uiState.collectAsState()
                    MeterScreen(
                        state = state,
                        onSelectIso = vm::selectIso,
                        onSelectMode = vm::selectMode,
                        onSelectAperture = vm::selectAperture,
                        onSelectShutter = vm::selectShutter,
                        onToggleHold = vm::toggleHold
                    )
                }
            }
        }
    }
}
