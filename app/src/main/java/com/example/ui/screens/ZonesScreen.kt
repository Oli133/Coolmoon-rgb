package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Hub
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.model.HubZone
import com.example.model.IrRemoteKey
import com.example.model.ZoneKind
import com.example.ui.components.IrRemoteDeck
import com.example.ui.theme.EmeraldOnline
import com.example.ui.theme.ObsidianOutline
import com.example.ui.theme.ObsidianSurfaceElevated
import com.example.ui.viewmodel.CoolMoonUiState

@Composable
fun ZonesScreen(
    state: CoolMoonUiState,
    onToggleMbSync: () -> Unit,
    onSyncAllZones: () -> Unit,
    onUpdateZone: (Int, String, Int, Boolean, Boolean) -> Unit,
    onIrKeyClick: (IrRemoteKey) -> Unit,
    modifier: Modifier = Modifier
) {
    var editingZone by remember { mutableStateOf<HubZone?>(null) }

    val activePortCount = remember(state.zones) { state.zones.count { it.enabled } }
    val totalLeds = remember(state.zones) { state.zones.filter { it.enabled }.sumOf { it.ledCount } }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column {
                Text(
                    text = "10-PORT ARGB HUB",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Configure individual 5V 3-pin fan & strip channels or bridge to motherboard",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Hub,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Column {
                                Text(
                                    text = "$activePortCount / ${state.zones.size} Ports Active",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "$totalLeds addressable 5V LEDs mapped",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        FilledTonalButton(
                            onClick = onSyncAllZones,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("sync_all_zones_button")
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Sync,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Sync All")
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF0E1320))
                            .border(1.dp, ObsidianOutline.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Memory,
                                contentDescription = null,
                                tint = if (state.mbSyncEnabled) EmeraldOnline else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Column {
                                Text(
                                    text = "Motherboard 5V ARGB Sync",
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = "Pass lighting control to ASUS/MSI/Gigabyte 3-pin header",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = state.mbSyncEnabled,
                            onCheckedChange = { onToggleMbSync() },
                            modifier = Modifier.testTag("mb_sync_switch")
                        )
                    }
                }
            }
        }

        items(state.zones, key = { it.id }) { zone ->
            val zoneColor = if (!zone.syncWithMaster && zone.overrideColor != null) {
                zone.overrideColor.toComposeColor()
            } else {
                state.primaryColor.toComposeColor()
            }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { editingZone = zone }
                    .testTag("hub_zone_card_${zone.id}"),
                shape = MaterialTheme.shapes.medium,
                color = ObsidianSurfaceElevated
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, ObsidianOutline, MaterialTheme.shapes.medium)
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(if (zone.enabled) zoneColor else Color(0xFF334155))
                        )
                        Column {
                            Text(
                                text = zone.label,
                                style = MaterialTheme.typography.titleSmall,
                                color = if (zone.enabled) {
                                    MaterialTheme.colorScheme.onSurface
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                            val kindTag = if (zone.kind == ZoneKind.FAN_ARGB) "FAN" else "STRIP"
                            val syncTag = if (zone.syncWithMaster) "Master Sync" else "Custom Override"
                            Text(
                                text = "$kindTag • ${zone.ledCount} LEDs • $syncTag",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { editingZone = zone },
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Edit,
                                contentDescription = "Edit ${zone.label}",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = zone.enabled,
                            onCheckedChange = { enabled ->
                                onUpdateZone(zone.id, zone.label, zone.ledCount, enabled, zone.syncWithMaster)
                            },
                            modifier = Modifier.testTag("zone_toggle_${zone.id}")
                        )
                    }
                }
            }
        }

        item {
            IrRemoteDeck(
                hasIrHardware = state.hasIrHardware,
                onKeyClick = onIrKeyClick
            )
        }

        item { Spacer(modifier = Modifier.height(12.dp)) }
    }

    editingZone?.let { zone ->
        EditZoneDialog(
            zone = zone,
            onDismiss = { editingZone = null },
            onSave = { label, ledCount, enabled, syncWithMaster ->
                onUpdateZone(zone.id, label, ledCount, enabled, syncWithMaster)
                editingZone = null
            }
        )
    }
}

@Composable
private fun EditZoneDialog(
    zone: HubZone,
    onDismiss: () -> Unit,
    onSave: (String, Int, Boolean, Boolean) -> Unit
) {
    var label by remember(zone) { mutableStateOf(zone.label) }
    var ledCount by remember(zone) { mutableIntStateOf(zone.ledCount) }
    var enabled by remember(zone) { mutableStateOf(zone.enabled) }
    var syncWithMaster by remember(zone) { mutableStateOf(zone.syncWithMaster) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Configure Port #${zone.id}", style = MaterialTheme.typography.headlineSmall) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Port Label") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Column {
                    Text(
                        text = "Addressable LEDs: $ledCount",
                        style = MaterialTheme.typography.labelLarge
                    )
                    Slider(
                        value = ledCount.toFloat(),
                        onValueChange = { ledCount = it.toInt() },
                        valueRange = 8f..60f
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Port Enabled", style = MaterialTheme.typography.bodyLarge)
                    Switch(checked = enabled, onCheckedChange = { enabled = it })
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Sync with Master Rig", style = MaterialTheme.typography.bodyLarge)
                    Switch(checked = syncWithMaster, onCheckedChange = { syncWithMaster = it })
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSave(label, ledCount, enabled, syncWithMaster) }) {
                Text("Apply")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
