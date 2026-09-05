package com.lumex.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.lumex.app.exposure.Stops

@Composable
fun MeterScreen(
    state: MeterUiState,
    onSelectIso: (Int) -> Unit,
    onSelectMode: (PriorityMode) -> Unit,
    onSelectAperture: (Double) -> Unit,
    onSelectShutter: (Double) -> Unit,
    onToggleHold: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Readout
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (state.caption.isNotEmpty()) {
                Text(
                    text = state.caption,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = state.headline,
                style = MaterialTheme.typography.displayLarge
            )
            Text(
                text = state.detail,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            if (state.clipped) {
                Text(
                    text = "Beyond scale — at limit",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }
            if (state.holding) {
                Text(
                    text = "HELD",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Needle scale: residual over/under exposure in EV.
        EvScale(residualEv = state.residualEv?.toFloat())

        // Mode
        Text(text = "You set", style = MaterialTheme.typography.labelMedium)
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            SegmentedButton(
                selected = state.mode == PriorityMode.APERTURE,
                onClick = { onSelectMode(PriorityMode.APERTURE) },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                label = { Text("F → T") }
            )
            SegmentedButton(
                selected = state.mode == PriorityMode.SHUTTER,
                onClick = { onSelectMode(PriorityMode.SHUTTER) },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                label = { Text("T → F") }
            )
        }

        // Fixed-parameter selector
        if (state.mode == PriorityMode.APERTURE) {
            Text(text = "Aperture", style = MaterialTheme.typography.labelMedium)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(Stops.APERTURES.toList()) { f ->
                    FilterChip(
                        selected = f == state.aperture,
                        onClick = { onSelectAperture(f) },
                        label = { Text(Stops.formatAperture(f)) }
                    )
                }
            }
        } else {
            Text(text = "Shutter speed", style = MaterialTheme.typography.labelMedium)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(Stops.SHUTTER_SPEEDS.toList()) { t ->
                    FilterChip(
                        selected = t == state.shutterSeconds,
                        onClick = { onSelectShutter(t) },
                        label = { Text(Stops.formatShutter(t)) }
                    )
                }
            }
        }

        // ISO
        Text(text = "Film ISO", style = MaterialTheme.typography.labelMedium)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(Stops.ISO_VALUES.toList()) { iso ->
                FilterChip(
                    selected = iso == state.iso,
                    onClick = { onSelectIso(iso) },
                    label = { Text(iso.toString()) }
                )
            }
        }

        Button(
            onClick = onToggleHold,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (state.holding) "Resume" else "Hold reading")
        }
    }
}

/**
 * Horizontal needle strip showing [residualEv] (stops of over/under exposure)
 * on a fixed ±3 scale with headroom to ±3.5.
 */
@Composable
fun EvScale(
    residualEv: Float?,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
    ) {
        val half = maxWidth / 2
        val pxPerEv = half / 3.5f
        fun x(ev: Float): androidx.compose.ui.unit.Dp = half + pxPerEv * ev

        // Needle behind ticks so they stay readable when pegged.
        if (residualEv != null) {
            val clamped = residualEv.coerceIn(-3.5f, 3.5f)
            Box(
                modifier = Modifier
                    .offset(x = x(clamped) - 2.dp)
                    .width(4.dp)
                    .height(20.dp)
                    .background(
                        MaterialTheme.colorScheme.primary,
                        RoundedCornerShape(2.dp)
                    )
            )
        }

        for (i in -3..3) {
            val f = i.toFloat()
            Box(
                modifier = Modifier
                    .offset(x = x(f) - 1.dp)
                    .width(2.dp)
                    .height(if (i == 0) 20.dp else 14.dp)
                    .background(
                        if (i == 0) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
            )
            Box(
                modifier = Modifier
                    .offset(x = x(f) - 16.dp, y = 26.dp)
                    .width(32.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                Text(
                    text = if (i > 0) "+$i" else "$i",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MeterScreenPreview() {
    MaterialTheme {
        MeterScreen(
            state = MeterUiState(
                lux = 800f,
                mode = PriorityMode.APERTURE,
                iso = 100,
                aperture = 8.0,
                caption = "Shutter speed",
                headline = "1/60",
                detail = "exact 1/84 · f/8 · 800 lx · EV 12.4 · ISO 100",
                residualEv = 0.4,
                clipped = false
            ),
            onSelectIso = {},
            onSelectMode = {},
            onSelectAperture = {},
            onSelectShutter = {},
            onToggleHold = {}
        )
    }
}
