package com.lumex.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lumex.app.exposure.Stops

private val ScreenHorizontalPadding = 20.dp
private val SectionSpacing = 16.dp
private val BottomReserveSpace = 96.dp

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
    var showDebug by rememberSaveable { mutableStateOf(false) }
    val controlsEnabled = state.controlsEnabled
    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(
                    start = ScreenHorizontalPadding,
                    end = ScreenHorizontalPadding,
                    top = ScreenHorizontalPadding,
                    bottom = BottomReserveSpace
                ),
            verticalArrangement = Arrangement.spacedBy(SectionSpacing)
        ) {
            // Clearance for centered punch-hole cameras.
            Spacer(modifier = Modifier.height(16.dp))
            ReadoutSection(
                caption = state.caption,
                headline = state.headline,
                detail = state.detail,
                showDebug = showDebug
            )

            // Needle scale: residual over/under exposure in EV.
            // When clipped, a red triangle at the scale end replaces the needle.
            EvScale(residualEv = state.residualEv?.toFloat(), clipped = state.clipped)

            ModeSelector(
                mode = state.mode,
                onSelectMode = onSelectMode,
                enabled = state.hasSensor
            )

            FixedParamSelector(
                mode = state.mode,
                aperture = state.aperture,
                shutterSeconds = state.shutterSeconds,
                onSelectAperture = onSelectAperture,
                onSelectShutter = onSelectShutter,
                enabled = controlsEnabled
            )

            HoldButton(
                holding = state.holding,
                enabled = controlsEnabled,
                onToggleHold = onToggleHold
            )

            // Breathing room: ISO is set once per roll, so it lives near the
            // bottom, just above the debug toggle — not glued to the lock button.
            Spacer(modifier = Modifier.height(56.dp))

            // Film ISO lives at the bottom: set once, rarely changed.
            Text(text = "Film ISO", style = MaterialTheme.typography.labelMedium)
            DialStrip(
                values = Stops.ISO_VALUES,
                selected = state.iso,
                onSelect = onSelectIso,
                labelFor = { it.toString() },
                enabled = state.hasSensor
            )
        }

        IconButton(
            onClick = { showDebug = !showDebug },
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.BugReport,
                contentDescription = "Toggle debug info",
                tint = if (showDebug) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ReadoutSection(
    caption: String,
    headline: String,
    detail: String,
    showDebug: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (caption.isNotEmpty()) {
            Text(
                text = caption,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = headline,
            style = MaterialTheme.typography.displayLarge
        )
        if (showDebug) {
            Text(
                text = detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ModeSelector(
    mode: PriorityMode,
    onSelectMode: (PriorityMode) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(text = "Mode", style = MaterialTheme.typography.labelMedium)
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            SegmentedButton(
                selected = mode == PriorityMode.APERTURE,
                onClick = { onSelectMode(PriorityMode.APERTURE) },
                enabled = enabled,
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                label = { Text("Av") }
            )
            SegmentedButton(
                selected = mode == PriorityMode.SHUTTER,
                onClick = { onSelectMode(PriorityMode.SHUTTER) },
                enabled = enabled,
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                label = { Text("Tv") }
            )
        }
    }
}

@Composable
private fun FixedParamSelector(
    mode: PriorityMode,
    aperture: Double,
    shutterSeconds: Double,
    onSelectAperture: (Double) -> Unit,
    onSelectShutter: (Double) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Fixed-parameter selector: continuous dial strip.
        if (mode == PriorityMode.APERTURE) {
            Text(text = "Aperture", style = MaterialTheme.typography.labelMedium)
            DialStrip(
                values = Stops.APERTURES,
                selected = aperture,
                onSelect = onSelectAperture,
                labelFor = Stops::formatAperture,
                enabled = enabled
            )
        } else {
            Text(text = "Shutter speed", style = MaterialTheme.typography.labelMedium)
            DialStrip(
                values = Stops.SHUTTER_SPEEDS,
                selected = shutterSeconds,
                onSelect = onSelectShutter,
                labelFor = Stops::formatShutter,
                enabled = enabled
            )
        }
    }
}

@Composable
private fun HoldButton(
    holding: Boolean,
    enabled: Boolean,
    onToggleHold: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Shutter-style hold button: open lock = live, closed lock = held.
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .alpha(if (enabled) 1f else 0.38f)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(
                    3.dp,
                    if (holding) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.outline,
                    CircleShape
                )
                .clickable(
                    enabled = enabled,
                    role = Role.Button,
                    onClick = onToggleHold
                )
                .semantics {
                    contentDescription = if (holding) "Release held reading" else "Hold reading"
                    stateDescription = if (holding) "Held" else "Live"
                },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(
                        if (holding) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.primaryContainer
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (holding) Icons.Filled.Lock else Icons.Filled.LockOpen,
                    contentDescription = null,
                    tint = if (holding) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(30.dp)
                )
            }
        }
    }
}

/**
 * Horizontal needle strip showing [residualEv] (stops of over/under exposure)
 * on a fixed ±3 scale with headroom to ±3.5.
 *
 * When [clipped], the exact value is off-scale: instead of a pegged needle,
 * a red triangle points outward at the corresponding end.
 */
@Composable
fun EvScale(
    residualEv: Float?,
    clipped: Boolean,
    modifier: Modifier = Modifier
) {
    val errorColor = MaterialTheme.colorScheme.error
    val scaleDescription = when {
        residualEv == null -> "No exposure reading"
        clipped && residualEv > 0f -> "Overexposed, off scale"
        clipped -> "Underexposed, off scale"
        else -> "Exposure error %.1f stops".format(residualEv)
    }
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .semantics {
                contentDescription = scaleDescription
                stateDescription = scaleDescription
            }
    ) {
        val half = maxWidth / 2
        val pxPerEv = half / 3.5f
        fun x(ev: Float): Dp = half + pxPerEv * ev

        if (clipped && residualEv != null) {
            val atRight = residualEv > 0f
            Canvas(
                modifier = Modifier
                    .size(22.dp)
                    .align(if (atRight) Alignment.CenterEnd else Alignment.CenterStart)
            ) {
                val w = size.width
                val h = size.height
                val triangle = Path().apply {
                    if (atRight) {
                        moveTo(0f, 0f)
                        lineTo(0f, h)
                        lineTo(w, h / 2f)
                    } else {
                        moveTo(w, 0f)
                        lineTo(w, h)
                        lineTo(0f, h / 2f)
                    }
                    close()
                }
                drawPath(triangle, color = errorColor)
            }
        } else if (residualEv != null) {
            // Needle behind ticks so they stay readable when pegged.
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

/**
 * Continuous dial strip: uniform-width values on one bar with vertical
 * separators. Selected value is emphasized; the strip starts scrolled
 * to the current selection.
 *
 * Values are expected to come from the same [Stops] table as [selected],
 * so equality is referential to table entries — no float tolerance needed.
 */
@Composable
private fun <T> DialStrip(
    values: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    labelFor: (T) -> String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val listState = rememberLazyListState()
    LaunchedEffect(values, selected) {
        val index = values.indexOf(selected)
        if (index > 0) listState.scrollToItem(index)
    }
    LazyRow(
        state = listState,
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.38f)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items(
            count = values.size,
            key = { index -> "$index:${values[index]}" }
        ) { index ->
            val value = values[index]
            val isSelected = value == selected
            Box(
                modifier = Modifier
                    .width(64.dp)
                    .clickable(enabled = enabled) { onSelect(value) },
                contentAlignment = Alignment.Center
            ) {
                if (index > 0) {
                    VerticalDivider(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .height(24.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                }
                Text(
                    text = labelFor(value),
                    modifier = Modifier.padding(vertical = 12.dp),
                    style = if (isSelected) MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ) else MaterialTheme.typography.bodyMedium,
                    color = if (isSelected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
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

@Preview(showBackground = true)
@Composable
private fun MeterScreenNoSensorPreview() {
    MaterialTheme {
        MeterScreen(
            state = MeterUiState(
                hasSensor = false,
                headline = "No sensor",
                detail = "This device has no ambient light sensor"
            ),
            onSelectIso = {},
            onSelectMode = {},
            onSelectAperture = {},
            onSelectShutter = {},
            onToggleHold = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MeterScreenTooDarkPreview() {
    MaterialTheme {
        MeterScreen(
            state = MeterUiState(
                lux = 0f,
                headline = "Too dark",
                detail = "Below sensor range"
            ),
            onSelectIso = {},
            onSelectMode = {},
            onSelectAperture = {},
            onSelectShutter = {},
            onToggleHold = {}
        )
    }
}
