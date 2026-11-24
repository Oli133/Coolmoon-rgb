package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bluetooth
import androidx.compose.material.icons.outlined.BluetoothSearching
import androidx.compose.material.icons.outlined.Lan
import androidx.compose.material.icons.outlined.SettingsRemote
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.hardware.BleConnectionState
import com.example.model.BleControllerDevice
import com.example.model.ConnectionMode
import com.example.model.WifiProtocol
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.EmeraldOnline
import com.example.ui.theme.ObsidianCanvas
import com.example.ui.theme.ObsidianOutline
import com.example.ui.theme.ObsidianSurfaceElevated
import com.example.ui.viewmodel.CoolMoonUiState

@Composable
fun ConnectScreen(
    state: CoolMoonUiState,
    requiredBlePermissions: Array<String>,
    onRefreshCapabilities: () -> Unit,
    onStartBleScan: () -> Unit,
    onStopBleScan: () -> Unit,
    onConnectBle: (BleControllerDevice) -> Unit,
    onDisconnectBle: () -> Unit,
    onUpdateWifiConfig: (String, String, WifiProtocol) -> Unit,
    onTestWifi: () -> Unit,
    onSwitchMode: (ConnectionMode) -> Unit,
    modifier: Modifier = Modifier
) {
    var ipText by remember(state.wifiConfig.ipAddress) { mutableStateOf(state.wifiConfig.ipAddress) }
    var portText by remember(state.wifiConfig.port) { mutableStateOf(state.wifiConfig.port.toString()) }
    var selectedProtocol by remember(state.wifiConfig.protocol) { mutableStateOf(state.wifiConfig.protocol) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        onRefreshCapabilities()
        if (results.values.all { it }) {
            onStartBleScan()
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(156.dp)
                    .clip(MaterialTheme.shapes.large)
                    .border(1.dp, ObsidianOutline, MaterialTheme.shapes.large)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_coolmoon_hub_hero),
                    contentDescription = "CoolMoon RGB hardware hub controller",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    ObsidianCanvas.copy(alpha = 0.92f)
                                )
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                ) {
                    Text(
                        text = "HARDWARE CONNECT",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White
                    )
                    Text(
                        text = "Active Mode: ${state.activeConnectionMode.label}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // Hardware Diagnostics Row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CapabilityTile(
                    icon = Icons.Outlined.Bluetooth,
                    title = "BLE",
                    status = when {
                        !state.isBleSupported -> "Unavailable"
                        !state.isBluetoothEnabled -> "BT Off"
                        else -> "Ready"
                    },
                    isGood = state.isBleSupported && state.isBluetoothEnabled,
                    modifier = Modifier.weight(1f)
                )
                CapabilityTile(
                    icon = Icons.Outlined.Wifi,
                    title = "Wi-Fi LAN",
                    status = if (state.isWifiConnected) "Online" else "No Wi-Fi",
                    isGood = state.isWifiConnected,
                    modifier = Modifier.weight(1f)
                )
                CapabilityTile(
                    icon = Icons.Outlined.SettingsRemote,
                    title = "IR 38kHz",
                    status = if (state.hasIrHardware) "Emitter OK" else "No Blaster",
                    isGood = state.hasIrHardware,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // BLE GATT Scanner Card
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
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.BluetoothSearching,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Column {
                                Text(
                                    text = "Bluetooth LE Controllers",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "Supports CoolMoon BLE, ELK-BLEDOM & SP110E",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (state.isBleScanning) {
                            OutlinedButton(
                                onClick = onStopBleScan,
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Stop")
                            }
                        } else {
                            Button(
                                onClick = {
                                    if (state.hasBlePermissions) {
                                        onStartBleScan()
                                    } else {
                                        permissionLauncher.launch(requiredBlePermissions)
                                    }
                                },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("ble_scan_button")
                            ) {
                                Text("Scan BLE")
                            }
                        }
                    }

                    when (val conn = state.bleConnectionState) {
                        is BleConnectionState.Connected -> {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(EmeraldOnline.copy(alpha = 0.14f))
                                    .border(1.dp, EmeraldOnline.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Connected: ${conn.deviceName}",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = EmeraldOnline
                                    )
                                    Text(
                                        text = "${conn.address} • Char ${conn.characteristicUuid}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                OutlinedButton(onClick = onDisconnectBle) {
                                    Text("Disconnect")
                                }
                            }
                        }
                        is BleConnectionState.Connecting -> {
                            Text(
                                text = "Connecting to ${conn.deviceName}...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        is BleConnectionState.Error -> {
                            Text(
                                text = conn.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        BleConnectionState.Disconnected -> Unit
                    }

                    if (state.bleDevices.isEmpty()) {
                        Text(
                            text = if (state.isBleScanning) {
                                "Scanning nearby 2.4GHz BLE peripherals..."
                            } else {
                                "Tap Scan BLE to discover nearby CoolMoon or ARGB Bluetooth controllers."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            state.bleDevices.forEach { dev ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(ObsidianCanvas)
                                        .border(1.dp, ObsidianOutline, RoundedCornerShape(12.dp))
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = dev.name,
                                                style = MaterialTheme.typography.titleSmall,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            if (dev.isLikelyRgbController) {
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .background(MaterialTheme.colorScheme.primaryContainer)
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        text = "RGB",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                            }
                                        }
                                        Text(
                                            text = "${dev.address} • ${dev.rssi} dBm",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Button(
                                        onClick = { onConnectBle(dev) },
                                        modifier = Modifier.minimumInteractiveComponentSize()
                                    ) {
                                        Text("Pair")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Wi-Fi / LAN Bridge Card
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
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Lan,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Column {
                            Text(
                                text = "Wi-Fi / LAN Hub Bridge",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Direct TCP/HTTP/UDP control for MagicHome & WLED hubs",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        WifiProtocol.entries.forEach { proto ->
                            val selected = proto == selectedProtocol
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .minimumInteractiveComponentSize()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (selected) MaterialTheme.colorScheme.secondaryContainer else ObsidianCanvas
                                    )
                                    .border(
                                        1.dp,
                                        if (selected) MaterialTheme.colorScheme.secondary else ObsidianOutline,
                                        RoundedCornerShape(10.dp)
                                    )
                                    .clickable {
                                        selectedProtocol = proto
                                        portText = proto.defaultPort.toString()
                                        onUpdateWifiConfig(ipText, portText, proto)
                                    }
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = proto.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = ipText,
                            onValueChange = {
                                ipText = it
                                onUpdateWifiConfig(ipText, portText, selectedProtocol)
                            },
                            label = { Text("Hub IP") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(2f)
                                .testTag("wifi_ip_input")
                        )
                        OutlinedTextField(
                            value = portText,
                            onValueChange = {
                                portText = it
                                onUpdateWifiConfig(ipText, portText, selectedProtocol)
                            },
                            label = { Text("Port") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onTestWifi,
                            enabled = !state.isWifiBusy,
                            modifier = Modifier
                                .weight(1f)
                                .minimumInteractiveComponentSize()
                                .testTag("wifi_ping_button")
                        ) {
                            if (state.isWifiBusy) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            Text("Ping & Activate Wi-Fi")
                        }
                        OutlinedButton(
                            onClick = { onSwitchMode(ConnectionMode.VIRTUAL_RIG) },
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Text("Virtual Rig")
                        }
                    }

                    state.wifiStatusMessage?.let { msg ->
                        Text(
                            text = msg,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // Live Packet Inspector
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
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Terminal,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Protocol Packet Inspector",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

                    if (state.packetLog.isEmpty()) {
                        Text(
                            text = "Adjust any color, effect, or IR key to inspect live MagicHome, WLED, BLE, or NEC IR frames.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(ObsidianCanvas)
                                .border(1.dp, ObsidianOutline.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            state.packetLog.take(6).forEach { entry ->
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "${entry.timestamp} • ${entry.transport}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (entry.delivered) EmeraldOnline else AmberWarning
                                        )
                                        Text(
                                            text = entry.summary,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        text = entry.hexPayload,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(12.dp)) }
    }
}

@Composable
private fun CapabilityTile(
    icon: ImageVector,
    title: String,
    status: String,
    isGood: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = ObsidianSurfaceElevated
    ) {
        Column(
            modifier = Modifier
                .border(1.dp, ObsidianOutline, MaterialTheme.shapes.medium)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isGood) EmeraldOnline else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(if (isGood) EmeraldOnline else AmberWarning)
                )
                Text(
                    text = status,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
