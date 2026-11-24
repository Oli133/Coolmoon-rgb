package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Air
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.BrightnessHigh
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.ConnectionMode
import com.example.model.FanCurveMode
import com.example.model.LightingEffect
import com.example.model.RgbColor
import com.example.ui.components.ColorWheelControlDeck
import com.example.ui.components.CoolMoonRigVisualizer
import com.example.ui.theme.EmeraldOnline
import com.example.ui.theme.ObsidianCanvas
import com.example.ui.theme.ObsidianOutline
import com.example.ui.theme.ObsidianSurfaceElevated
import com.example.ui.viewmodel.CoolMoonUiState

@Composable
fun StudioScreen(
    state: CoolMoonUiState,
    onTogglePower: () -> Unit,
    onSelectZone: (Int?) -> Unit,
    onSelectEditingSecondary: (Boolean) -> Unit,
    onSelectColor: (RgbColor) -> Unit,
    onSelectEffect: (LightingEffect) -> Unit,
    onBrightnessChange: (Int) -> Unit,
    onSpeedChange: (Int) -> Unit,
    onToggleDirection: () -> Unit,
    onSelectFanCurve: (FanCurveMode) -> Unit,
    onSavePreset: (String, String) -> Unit,
    onNavigateToConnect: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showSaveDialog by remember { mutableStateOf(false) }

    val selectedZoneLabel = remember(state.selectedZoneId, state.zones) {
        state.zones.firstOrNull { it.id == state.selectedZoneId }?.label
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isWide = maxWidth >= 720.dp

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                StudioHeaderBar(
                    isPowerOn = state.isPowerOn,
                    connectionMode = state.activeConnectionMode,
                    onTogglePower = onTogglePower,
                    onOpenSavePreset = { showSaveDialog = true },
                    onConnectionClick = onNavigateToConnect
                )
            }

            if (isWide) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            CoolMoonRigVisualizer(
                                isPowerOn = state.isPowerOn,
                                primaryColor = state.primaryColor,
                                secondaryColor = state.secondaryColor,
                                effect = state.effect,
                                brightness = state.brightness,
                                speed = state.speed,
                                fanCurve = state.fanCurve,
                                directionCw = state.directionCw,
                                zones = state.zones,
                                selectedZoneId = state.selectedZoneId,
                                onSelectZone = onSelectZone
                            )
                            IntensityAndAirflowDeck(
                                brightness = state.brightness,
                                speed = state.speed,
                                directionCw = state.directionCw,
                                fanCurve = state.fanCurve,
                                onBrightnessChange = onBrightnessChange,
                                onSpeedChange = onSpeedChange,
                                onToggleDirection = onToggleDirection,
                                onSelectFanCurve = onSelectFanCurve
                            )
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            ColorWheelControlDeck(
                                primaryColor = state.primaryColor,
                                secondaryColor = state.secondaryColor,
                                editingSecondary = state.editingSecondaryColor,
                                supportsDualColor = state.effect.supportsDualColor,
                                activeZoneLabel = selectedZoneLabel,
                                onSelectEditingSecondary = onSelectEditingSecondary,
                                onColorSelected = onSelectColor
                            )
                            EffectsMatrixCard(
                                activeEffect = state.effect,
                                onSelectEffect = onSelectEffect
                            )
                        }
                    }
                }
            } else {
                item {
                    CoolMoonRigVisualizer(
                        isPowerOn = state.isPowerOn,
                        primaryColor = state.primaryColor,
                        secondaryColor = state.secondaryColor,
                        effect = state.effect,
                        brightness = state.brightness,
                        speed = state.speed,
                        fanCurve = state.fanCurve,
                        directionCw = state.directionCw,
                        zones = state.zones,
                        selectedZoneId = state.selectedZoneId,
                        onSelectZone = onSelectZone
                    )
                }

                item {
                    ColorWheelControlDeck(
                        primaryColor = state.primaryColor,
                        secondaryColor = state.secondaryColor,
                        editingSecondary = state.editingSecondaryColor,
                        supportsDualColor = state.effect.supportsDualColor,
                        activeZoneLabel = selectedZoneLabel,
                        onSelectEditingSecondary = onSelectEditingSecondary,
                        onColorSelected = onSelectColor
                    )
                }

                item {
                    IntensityAndAirflowDeck(
                        brightness = state.brightness,
                        speed = state.speed,
                        directionCw = state.directionCw,
                        fanCurve = state.fanCurve,
                        onBrightnessChange = onBrightnessChange,
                        onSpeedChange = onSpeedChange,
                        onToggleDirection = onToggleDirection,
                        onSelectFanCurve = onSelectFanCurve
                    )
                }

                item {
                    EffectsMatrixCard(
                        activeEffect = state.effect,
                        onSelectEffect = onSelectEffect
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }
        }
    }

    if (showSaveDialog) {
        SavePresetDialog(
            onDismiss = { showSaveDialog = false },
            onConfirm = { name, tagline ->
                onSavePreset(name, tagline)
                showSaveDialog = false
            }
        )
    }
}

@Composable
private fun StudioHeaderBar(
    isPowerOn: Boolean,
    connectionMode: ConnectionMode,
    onTogglePower: () -> Unit,
    onOpenSavePreset: () -> Unit,
    onConnectionClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "COOLMOON STUDIO",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onConnectionClick)
                    .padding(vertical = 2.dp)
                    .testTag("connection_status_pill")
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(
                            if (connectionMode == ConnectionMode.VIRTUAL_RIG) {
                                MaterialTheme.colorScheme.secondary
                            } else {
                                EmeraldOnline
                            }
                        )
                )
                Text(
                    text = if (connectionMode == ConnectionMode.VIRTUAL_RIG) {
                        "Virtual Rig Preview • Tap to Pair"
                    } else {
                        "${connectionMode.label} Active"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onOpenSavePreset,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .clip(CircleShape)
                    .background(ObsidianSurfaceElevated)
                    .border(1.dp, ObsidianOutline, CircleShape)
                    .testTag("save_preset_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.BookmarkAdd,
                    contentDescription = "Save current lighting as preset",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Button(
                onClick = onTogglePower,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("master_power_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isPowerOn) MaterialTheme.colorScheme.primary else Color(0xFF283044),
                    contentColor = if (isPowerOn) ObsidianCanvas else MaterialTheme.colorScheme.onSurface
                ),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.PowerSettingsNew,
                    contentDescription = "Toggle master RGB power",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isPowerOn) "ON" else "OFF",
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun IntensityAndAirflowDeck(
    brightness: Int,
    speed: Int,
    directionCw: Boolean,
    fanCurve: FanCurveMode,
    onBrightnessChange: (Int) -> Unit,
    onSpeedChange: (Int) -> Unit,
    onToggleDirection: () -> Unit,
    onSelectFanCurve: (FanCurveMode) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = ObsidianSurfaceElevated
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, ObsidianOutline, MaterialTheme.shapes.large)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Brightness
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.BrightnessHigh,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Luminance",
                            style = MaterialTheme.typography.titleSmall
                        )
                    }
                    Text(
                        text = "$brightness%",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Slider(
                    value = brightness.toFloat(),
                    onValueChange = { onBrightnessChange(it.toInt()) },
                    valueRange = 5f..100f,
                    modifier = Modifier.testTag("brightness_slider"),
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary
                    )
                )
            }

            // Speed + Direction
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Speed,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Animation Tempo",
                            style = MaterialTheme.typography.titleSmall
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "$speed%",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        FilledTonalButton(
                            onClick = onToggleDirection,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("direction_toggle_button"),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Refresh,
                                contentDescription = "Toggle animation direction",
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (directionCw) "CW" else "CCW",
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }
                Slider(
                    value = speed.toFloat(),
                    onValueChange = { onSpeedChange(it.toInt()) },
                    valueRange = 5f..100f,
                    modifier = Modifier.testTag("speed_slider"),
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.secondary,
                        activeTrackColor = MaterialTheme.colorScheme.secondary
                    )
                )
            }

            // Fan PWM Curve
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Air,
                        contentDescription = null,
                        tint = EmeraldOnline,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Fan PWM Curve",
                        style = MaterialTheme.typography.titleSmall
                    )
                }
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FanCurveMode.entries.forEach { mode ->
                        val selected = mode == fanCurve
                        Box(
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (selected) EmeraldOnline.copy(alpha = 0.16f) else ObsidianCanvas
                                )
                                .border(
                                    1.dp,
                                    if (selected) EmeraldOnline else ObsidianOutline,
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable { onSelectFanCurve(mode) }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                .testTag("fan_curve_${mode.name.lowercase()}")
                        ) {
                            Column {
                                Text(
                                    text = mode.displayName,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (selected) EmeraldOnline else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${mode.targetRpm} RPM",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EffectsMatrixCard(
    activeEffect: LightingEffect,
    onSelectEffect: (LightingEffect) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = ObsidianSurfaceElevated
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, ObsidianOutline, MaterialTheme.shapes.large)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "ARGB Effect Engine",
                    style = MaterialTheme.typography.titleMedium
                )
            }
            Text(
                text = activeEffect.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            val rows = LightingEffect.entries.chunked(2)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                rows.forEach { pair ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        pair.forEach { effect ->
                            val selected = effect == activeEffect
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .minimumInteractiveComponentSize()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(
                                        if (selected) {
                                            MaterialTheme.colorScheme.primaryContainer
                                        } else {
                                            ObsidianCanvas
                                        }
                                    )
                                    .border(
                                        1.dp,
                                        if (selected) MaterialTheme.colorScheme.primary else ObsidianOutline,
                                        RoundedCornerShape(14.dp)
                                    )
                                    .clickable { onSelectEffect(effect) }
                                    .padding(horizontal = 12.dp, vertical = 10.dp)
                                    .testTag("effect_${effect.name.lowercase()}")
                            ) {
                                Column {
                                    Text(
                                        text = effect.shortCode,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = effect.displayName,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                        if (pair.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SavePresetDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var tagline by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Save CoolMoon Preset", style = MaterialTheme.typography.headlineSmall) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Preset Name") },
                    placeholder = { Text("e.g. Frostbite Desk") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("preset_name_input")
                )
                OutlinedTextField(
                    value = tagline,
                    onValueChange = { tagline = it },
                    label = { Text("Notes (optional)") },
                    placeholder = { Text("Dual-ring cyan at 60% speed") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name, tagline) },
                modifier = Modifier.testTag("confirm_save_preset")
            ) {
                Text("Save Preset")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
