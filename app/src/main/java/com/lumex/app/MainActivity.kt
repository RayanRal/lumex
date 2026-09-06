package com.lumex.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lumex.app.ui.MeterScreen
import com.lumex.app.ui.MeterViewModel
import com.lumex.app.ui.MeterViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val factory = remember { MeterViewModelFactory(application) }
                    val vm: MeterViewModel = viewModel(factory = factory)
                    val state by vm.uiState.collectAsStateWithLifecycle()
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
