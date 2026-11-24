package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AiLightingPlan
import com.example.data.CoolMoonPreferencesRepository
import com.example.data.LightingArchitectRepository
import com.example.data.PlaceholderLightingArchitectRepository
import com.example.data.defaultCoolMoonZones
import com.example.hardware.BleConnectionState
import com.example.hardware.BleControllerManager
import com.example.hardware.CoolMoonProtocolEncoder
import com.example.hardware.IrRemoteTransmitter
import com.example.hardware.WifiControllerClient
import com.example.model.BleControllerDevice
import com.example.model.ConnectionMode
import com.example.model.FanCurveMode
import com.example.model.HubZone
import com.example.model.IrRemoteKey
import com.example.model.LightingEffect
import com.example.model.LightingPreset
import com.example.model.PacketLogEntry
import com.example.model.RgbColor
import com.example.model.WifiControllerConfig
import com.example.model.WifiProtocol
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

sealed interface AiArchitectUiState {
    data object Idle : AiArchitectUiState
    data object Thinking : AiArchitectUiState
    data class Success(val plan: AiLightingPlan) : AiArchitectUiState
    data class Error(val message: String) : AiArchitectUiState
}

data class CoolMoonUiState(
    val isPowerOn: Boolean = true,
    val primaryColor: RgbColor = RgbColor.Cyan,
    val secondaryColor: RgbColor = RgbColor.Ultraviolet,
    val editingSecondaryColor: Boolean = false,
    val effect: LightingEffect = LightingEffect.DUAL_RING_CHASE,
    val brightness: Int = 88,
    val speed: Int = 62,
    val fanCurve: FanCurveMode = FanCurveMode.BALANCED,
    val directionCw: Boolean = true,
    val mbSyncEnabled: Boolean = false,
    val selectedZoneId: Int? = null,
    val zones: List<HubZone> = defaultCoolMoonZones(),
    val presets: List<LightingPreset> = emptyList(),
    val activeConnectionMode: ConnectionMode = ConnectionMode.VIRTUAL_RIG,
    val wifiConfig: WifiControllerConfig = WifiControllerConfig(),
    val wifiStatusMessage: String? = null,
    val isWifiBusy: Boolean = false,
    val isBleSupported: Boolean = false,
    val isBluetoothEnabled: Boolean = false,
    val hasBlePermissions: Boolean = false,
    val isBleScanning: Boolean = false,
    val bleDevices: List<BleControllerDevice> = emptyList(),
    val bleConnectionState: BleConnectionState = BleConnectionState.Disconnected,
    val hasIrHardware: Boolean = false,
    val isWifiConnected: Boolean = false,
    val packetLog: List<PacketLogEntry> = emptyList(),
    val lastStatusBanner: String? = null,
    val aiQuery: String = "",
    val aiState: AiArchitectUiState = AiArchitectUiState.Idle,
    val isAiFirebaseConnected: Boolean = false
)

class CoolMoonViewModel(application: Application) : AndroidViewModel(application) {

    private val prefsRepo = CoolMoonPreferencesRepository(application)
    private val bleManager = BleControllerManager(application)
    private val wifiClient = WifiControllerClient()
    private val irTransmitter = IrRemoteTransmitter(application)
    private val architectRepo: LightingArchitectRepository = PlaceholderLightingArchitectRepository()

    private val _uiState = MutableStateFlow(
        CoolMoonUiState(
            isBleSupported = bleManager.isBleSupported,
            isBluetoothEnabled = bleManager.isBluetoothEnabled,
            hasBlePermissions = bleManager.hasBlePermissions(),
            hasIrHardware = irTransmitter.hasIrHardware,
            isWifiConnected = checkWifiNetwork(application),
            isAiFirebaseConnected = architectRepo.isFirebaseConnected
        )
    )
    val uiState: StateFlow<CoolMoonUiState> = _uiState.asStateFlow()

    private var dispatchJob: Job? = null
    private var scanTimerJob: Job? = null
    private var packetCounter = 1L
    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.US)

    init {
        viewModelScope.launch {
            prefsRepo.snapshotFlow.collect { snapshot ->
                _uiState.update { state ->
                    state.copy(
                        isPowerOn = snapshot.isPowerOn,
                        primaryColor = snapshot.primaryColor,
                        secondaryColor = snapshot.secondaryColor,
                        effect = snapshot.effect,
                        brightness = snapshot.brightness,
                        speed = snapshot.speed,
                        fanCurve = snapshot.fanCurve,
                        directionCw = snapshot.directionCw,
                        mbSyncEnabled = snapshot.mbSyncEnabled,
                        zones = snapshot.zones,
                        presets = snapshot.presets,
                        wifiConfig = snapshot.wifiConfig
                    )
                }
            }
        }
        viewModelScope.launch {
            bleManager.isScanning.collect { scanning ->
                _uiState.update { it.copy(isBleScanning = scanning) }
            }
        }
        viewModelScope.launch {
            bleManager.discoveredDevices.collect { devices ->
                _uiState.update { it.copy(bleDevices = devices) }
            }
        }
        viewModelScope.launch {
            bleManager.connectionState.collect { conn ->
                _uiState.update { state ->
                    val mode = if (conn is BleConnectionState.Connected) {
                        ConnectionMode.BLE_GATT
                    } else if (state.activeConnectionMode == ConnectionMode.BLE_GATT && conn is BleConnectionState.Disconnected) {
                        ConnectionMode.VIRTUAL_RIG
                    } else {
                        state.activeConnectionMode
                    }
                    state.copy(bleConnectionState = conn, activeConnectionMode = mode)
                }
            }
        }
    }

    fun refreshHardwareCapabilities() {
        val ctx = getApplication<Application>()
        _uiState.update {
            it.copy(
                isBleSupported = bleManager.isBleSupported,
                isBluetoothEnabled = bleManager.isBluetoothEnabled,
                hasBlePermissions = bleManager.hasBlePermissions(),
                hasIrHardware = irTransmitter.hasIrHardware,
                isWifiConnected = checkWifiNetwork(ctx)
            )
        }
    }

    fun requiredBlePermissions(): Array<String> = bleManager.requiredPermissions()

    fun togglePower() {
        val next = !_uiState.value.isPowerOn
        _uiState.update { it.copy(isPowerOn = next) }
        persistAndDispatch(immediate = true, sendPowerOnly = true)
    }

    fun setEditingSecondaryColor(editingSecondary: Boolean) {
        _uiState.update { it.copy(editingSecondaryColor = editingSecondary) }
    }

    fun selectColor(color: RgbColor) {
        val state = _uiState.value
        val zoneId = state.selectedZoneId
        if (zoneId != null) {
            val updatedZones = state.zones.map { z ->
                if (z.id == zoneId) z.copy(syncWithMaster = false, overrideColor = color) else z
            }
            _uiState.update { it.copy(zones = updatedZones) }
            viewModelScope.launch { prefsRepo.saveZones(updatedZones) }
            scheduleHardwareDispatch(debounced = true)
            return
        }

        _uiState.update {
            if (it.editingSecondaryColor && it.effect.supportsDualColor) {
                it.copy(secondaryColor = color)
            } else {
                it.copy(primaryColor = color)
            }
        }
        persistAndDispatch(immediate = false)
    }

    fun selectEffect(effect: LightingEffect) {
        _uiState.update {
            it.copy(
                effect = effect,
                mbSyncEnabled = effect == LightingEffect.MOTHERBOARD_SYNC,
                editingSecondaryColor = if (!effect.supportsDualColor) false else it.editingSecondaryColor
            )
        }
        persistAndDispatch(immediate = true)
    }

    fun updateBrightness(value: Int) {
        _uiState.update { it.copy(brightness = value.coerceIn(5, 100)) }
        persistAndDispatch(immediate = false)
    }

    fun updateSpeed(value: Int) {
        _uiState.update { it.copy(speed = value.coerceIn(5, 100)) }
        persistAndDispatch(immediate = false)
    }

    fun toggleDirection() {
        _uiState.update { it.copy(directionCw = !it.directionCw) }
        persistAndDispatch(immediate = true)
    }

    fun selectFanCurve(mode: FanCurveMode) {
        _uiState.update { it.copy(fanCurve = mode) }
        persistAndDispatch(immediate = true)
    }

    fun toggleMbSync() {
        val next = !_uiState.value.mbSyncEnabled
        _uiState.update {
            it.copy(
                mbSyncEnabled = next,
                effect = if (next) LightingEffect.MOTHERBOARD_SYNC else LightingEffect.DUAL_RING_CHASE
            )
        }
        persistAndDispatch(immediate = true)
    }

    fun selectZoneForPreview(zoneId: Int?) {
        _uiState.update { it.copy(selectedZoneId = zoneId) }
    }

    fun updateZone(zoneId: Int, label: String, ledCount: Int, enabled: Boolean, syncWithMaster: Boolean) {
        val updated = _uiState.value.zones.map { z ->
            if (z.id == zoneId) {
                z.copy(
                    label = label.ifBlank { z.label },
                    ledCount = ledCount.coerceIn(4, 120),
                    enabled = enabled,
                    syncWithMaster = syncWithMaster,
                    overrideColor = if (syncWithMaster) null else (z.overrideColor ?: _uiState.value.primaryColor)
                )
            } else z
        }
        _uiState.update { it.copy(zones = updated) }
        viewModelScope.launch { prefsRepo.saveZones(updated) }
    }

    fun syncAllZonesToMaster() {
        val updated = _uiState.value.zones.map { it.copy(syncWithMaster = true, overrideColor = null) }
        _uiState.update { it.copy(zones = updated, selectedZoneId = null) }
        viewModelScope.launch { prefsRepo.saveZones(updated) }
        scheduleHardwareDispatch(debounced = false)
    }

    fun applyPreset(preset: LightingPreset) {
        _uiState.update {
            it.copy(
                isPowerOn = true,
                primaryColor = preset.primaryColor,
                secondaryColor = preset.secondaryColor,
                effect = preset.effect,
                brightness = preset.brightness,
                speed = preset.speed,
                fanCurve = preset.fanCurve,
                mbSyncEnabled = preset.effect == LightingEffect.MOTHERBOARD_SYNC,
                selectedZoneId = null,
                lastStatusBanner = "Loaded preset: ${preset.name}"
            )
        }
        persistAndDispatch(immediate = true)
    }

    fun saveCurrentAsPreset(name: String, tagline: String) {
        val state = _uiState.value
        val newPreset = LightingPreset(
            id = "custom_${UUID.randomUUID().toString().take(8)}",
            name = name.trim().ifBlank { "Custom Scene" },
            tagline = tagline.trim().ifBlank { "${state.effect.displayName} • ${state.primaryColor.toHex()}" },
            primaryColor = state.primaryColor,
            secondaryColor = state.secondaryColor,
            effect = state.effect,
            brightness = state.brightness,
            speed = state.speed,
            fanCurve = state.fanCurve,
            isBuiltIn = false
        )
        val updated = state.presets + newPreset
        _uiState.update {
            it.copy(
                presets = updated,
                lastStatusBanner = "Saved preset \"${newPreset.name}\""
            )
        }
        viewModelScope.launch { prefsRepo.saveCustomPresets(updated) }
    }

    fun deletePreset(presetId: String) {
        val updated = _uiState.value.presets.filterNot { it.id == presetId && !it.isBuiltIn }
        _uiState.update { it.copy(presets = updated) }
        viewModelScope.launch { prefsRepo.saveCustomPresets(updated) }
    }

    fun triggerIrKey(key: IrRemoteKey) {
        val result = irTransmitter.transmitKey(key)
        appendPacketLog(
            transport = "IR 38kHz",
            summary = key.label,
            hexPayload = result.necSummary,
            delivered = result.transmitted
        )

        // Reflect remote command immediately on the live rig
        when (key) {
            IrRemoteKey.POWER_ON -> _uiState.update { it.copy(isPowerOn = true) }
            IrRemoteKey.POWER_OFF -> _uiState.update { it.copy(isPowerOn = false) }
            IrRemoteKey.MB_SYNC -> toggleMbSync()
            IrRemoteKey.AUTO_CYCLE -> selectEffect(LightingEffect.RAINBOW_WAVE)
            IrRemoteKey.SPEED_UP -> updateSpeed(_uiState.value.speed + 12)
            IrRemoteKey.SPEED_DOWN -> updateSpeed(_uiState.value.speed - 12)
            IrRemoteKey.BRIGHT_UP -> updateBrightness(_uiState.value.brightness + 12)
            IrRemoteKey.BRIGHT_DOWN -> updateBrightness(_uiState.value.brightness - 12)
            IrRemoteKey.MODE_NEXT -> {
                val all = LightingEffect.entries
                val next = all[(all.indexOf(_uiState.value.effect) + 1) % all.size]
                selectEffect(next)
            }
            IrRemoteKey.MODE_PREV -> {
                val all = LightingEffect.entries
                val idx = all.indexOf(_uiState.value.effect)
                val prev = all[(idx - 1 + all.size) % all.size]
                selectEffect(prev)
            }
            IrRemoteKey.FAN_SPEED_UP -> {
                val all = FanCurveMode.entries
                val next = all[(all.indexOf(_uiState.value.fanCurve) + 1).coerceAtMost(all.lastIndex)]
                selectFanCurve(next)
            }
            IrRemoteKey.FAN_SPEED_DOWN -> {
                val all = FanCurveMode.entries
                val prev = all[(all.indexOf(_uiState.value.fanCurve) - 1).coerceAtLeast(0)]
                selectFanCurve(prev)
            }
            IrRemoteKey.COLOR_RED -> applyStaticIrColor(RgbColor.Crimson)
            IrRemoteKey.COLOR_GREEN -> applyStaticIrColor(RgbColor.Emerald)
            IrRemoteKey.COLOR_BLUE -> applyStaticIrColor(RgbColor.Cobalt)
            IrRemoteKey.COLOR_WHITE -> applyStaticIrColor(RgbColor.FrostWhite)
            IrRemoteKey.COLOR_CYAN -> applyStaticIrColor(RgbColor.Cyan)
            IrRemoteKey.COLOR_PURPLE -> applyStaticIrColor(RgbColor.Ultraviolet)
            IrRemoteKey.COLOR_YELLOW -> applyStaticIrColor(RgbColor.Amber)
            IrRemoteKey.COLOR_PINK -> applyStaticIrColor(RgbColor.PlasmaCoral)
            IrRemoteKey.RAINBOW -> selectEffect(LightingEffect.RAINBOW_WAVE)
        }

        _uiState.update { it.copy(lastStatusBanner = result.message) }
    }

    private fun applyStaticIrColor(color: RgbColor) {
        _uiState.update {
            it.copy(
                isPowerOn = true,
                primaryColor = color,
                effect = LightingEffect.STATIC,
                mbSyncEnabled = false
            )
        }
        persistAndDispatch(immediate = true)
    }

    fun startBleScan() {
        refreshHardwareCapabilities()
        val result = bleManager.startScan()
        if (result.isFailure) {
            _uiState.update {
                it.copy(lastStatusBanner = result.exceptionOrNull()?.message ?: "Cannot start BLE scan")
            }
            return
        }
        scanTimerJob?.cancel()
        scanTimerJob = viewModelScope.launch {
            delay(10_000L)
            bleManager.stopScan()
        }
    }

    fun stopBleScan() {
        scanTimerJob?.cancel()
        bleManager.stopScan()
    }

    fun connectBleDevice(device: BleControllerDevice) {
        bleManager.connect(device)
    }

    fun disconnectBle() {
        bleManager.disconnect()
        _uiState.update { it.copy(activeConnectionMode = ConnectionMode.VIRTUAL_RIG) }
    }

    fun updateWifiConfig(ip: String, portText: String, protocol: WifiProtocol) {
        val port = portText.toIntOrNull()?.coerceIn(1, 65535) ?: protocol.defaultPort
        val updated = WifiControllerConfig(ipAddress = ip.trim(), port = port, protocol = protocol)
        _uiState.update { it.copy(wifiConfig = updated) }
        viewModelScope.launch { prefsRepo.saveWifiConfig(updated) }
    }

    fun testWifiConnection() {
        val cfg = _uiState.value.wifiConfig
        viewModelScope.launch {
            _uiState.update { it.copy(isWifiBusy = true, wifiStatusMessage = "Connecting to ${cfg.ipAddress}:${cfg.port}...") }
            val result = wifiClient.pingController(cfg)
            _uiState.update {
                it.copy(
                    isWifiBusy = false,
                    wifiStatusMessage = result.message,
                    activeConnectionMode = if (result.success) ConnectionMode.WIFI_LAN else it.activeConnectionMode
                )
            }
            if (result.success) {
                scheduleHardwareDispatch(debounced = false)
            }
        }
    }

    fun switchConnectionMode(mode: ConnectionMode) {
        _uiState.update { it.copy(activeConnectionMode = mode) }
    }

    fun updateAiQuery(query: String) {
        _uiState.update { it.copy(aiQuery = query) }
    }

    fun runAiLightingArchitect(overrideQuery: String? = null) {
        val query = (overrideQuery ?: _uiState.value.aiQuery).trim()
        if (query.isEmpty()) return
        if (overrideQuery != null) {
            _uiState.update { it.copy(aiQuery = overrideQuery) }
        }
        val activeZones = _uiState.value.zones.count { it.enabled }
        viewModelScope.launch {
            _uiState.update { it.copy(aiState = AiArchitectUiState.Thinking) }
            val result = architectRepo.designLightingPlan(query, activeZones)
            _uiState.update { state ->
                result.fold(
                    onSuccess = { plan -> state.copy(aiState = AiArchitectUiState.Success(plan)) },
                    onFailure = { err -> state.copy(aiState = AiArchitectUiState.Error(err.localizedMessage ?: "Architect error")) }
                )
            }
        }
    }

    fun applyAiPlan(plan: AiLightingPlan) {
        _uiState.update {
            it.copy(
                isPowerOn = true,
                primaryColor = plan.primaryColor,
                secondaryColor = plan.secondaryColor,
                effect = plan.effect,
                brightness = plan.brightness,
                speed = plan.speed,
                fanCurve = plan.fanCurve,
                mbSyncEnabled = plan.effect == LightingEffect.MOTHERBOARD_SYNC,
                selectedZoneId = null,
                lastStatusBanner = "Applied plan: ${plan.planTitle}"
            )
        }
        persistAndDispatch(immediate = true)
    }

    fun clearStatusBanner() {
        _uiState.update { it.copy(lastStatusBanner = null) }
    }

    private fun persistAndDispatch(immediate: Boolean, sendPowerOnly: Boolean = false) {
        val s = _uiState.value
        viewModelScope.launch {
            prefsRepo.saveActiveState(
                isPowerOn = s.isPowerOn,
                primaryColor = s.primaryColor,
                secondaryColor = s.secondaryColor,
                effect = s.effect,
                brightness = s.brightness,
                speed = s.speed,
                fanCurve = s.fanCurve,
                directionCw = s.directionCw,
                mbSyncEnabled = s.mbSyncEnabled
            )
        }
        scheduleHardwareDispatch(debounced = !immediate, sendPowerOnly = sendPowerOnly)
    }

    private fun scheduleHardwareDispatch(debounced: Boolean, sendPowerOnly: Boolean = false) {
        dispatchJob?.cancel()
        dispatchJob = viewModelScope.launch {
            if (debounced) delay(90L)
            val s = _uiState.value

            when (s.activeConnectionMode) {
                ConnectionMode.BLE_GATT -> {
                    val payload = if (sendPowerOnly) {
                        CoolMoonProtocolEncoder.encodeBleElkPower(s.isPowerOn)
                    } else if (s.effect == LightingEffect.STATIC) {
                        CoolMoonProtocolEncoder.encodeBleElkColor(s.primaryColor, s.brightness)
                    } else {
                        CoolMoonProtocolEncoder.encodeMagicHomeEffect(s.effect, s.speed)
                    }
                    val ok = bleManager.sendPayload(payload)
                    appendPacketLog(
                        transport = "BLE GATT",
                        summary = if (sendPowerOnly) "Power ${if (s.isPowerOn) "ON" else "OFF"}" else s.effect.displayName,
                        hexPayload = CoolMoonProtocolEncoder.toHexString(payload),
                        delivered = ok
                    )
                }
                ConnectionMode.WIFI_LAN -> {
                    when (s.wifiConfig.protocol) {
                        WifiProtocol.MAGIC_HOME_TCP -> {
                            val payload = if (sendPowerOnly) {
                                CoolMoonProtocolEncoder.encodeMagicHomePower(s.isPowerOn)
                            } else if (s.effect == LightingEffect.STATIC) {
                                CoolMoonProtocolEncoder.encodeMagicHomeColor(s.primaryColor, s.brightness)
                            } else {
                                CoolMoonProtocolEncoder.encodeMagicHomeEffect(s.effect, s.speed)
                            }
                            val res = wifiClient.sendBytesTcp(s.wifiConfig, payload)
                            appendPacketLog(
                                transport = "TCP :${s.wifiConfig.port}",
                                summary = s.effect.displayName,
                                hexPayload = CoolMoonProtocolEncoder.toHexString(payload),
                                delivered = res.success
                            )
                        }
                        WifiProtocol.WLED_HTTP_JSON -> {
                            val json = CoolMoonProtocolEncoder.encodeWledJson(
                                isOn = s.isPowerOn,
                                primary = s.primaryColor,
                                secondary = s.secondaryColor,
                                effect = s.effect,
                                brightnessPercent = s.brightness,
                                speedPercent = s.speed
                            )
                            val res = wifiClient.sendWledJson(s.wifiConfig, json)
                            appendPacketLog(
                                transport = "WLED HTTP",
                                summary = s.effect.displayName,
                                hexPayload = json.take(46),
                                delivered = res.success
                            )
                        }
                        WifiProtocol.WLED_UDP_DRGB -> {
                            val pixels = List(16) {
                                if (s.isPowerOn) s.primaryColor else RgbColor(0, 0, 0)
                            }
                            val payload = CoolMoonProtocolEncoder.encodeWledUdpDrgb(pixels)
                            val res = wifiClient.sendUdpPacket(s.wifiConfig, payload)
                            appendPacketLog(
                                transport = "UDP :${s.wifiConfig.port}",
                                summary = "16-LED DRGB Frame",
                                hexPayload = CoolMoonProtocolEncoder.toHexString(payload.take(10).toByteArray()) + " ...",
                                delivered = res.success
                            )
                        }
                    }
                }
                ConnectionMode.VIRTUAL_RIG, ConnectionMode.IR_BLASTER -> {
                    val previewFrame = if (sendPowerOnly) {
                        CoolMoonProtocolEncoder.encodeMagicHomePower(s.isPowerOn)
                    } else if (s.effect == LightingEffect.STATIC) {
                        CoolMoonProtocolEncoder.encodeMagicHomeColor(s.primaryColor, s.brightness)
                    } else {
                        CoolMoonProtocolEncoder.encodeMagicHomeEffect(s.effect, s.speed)
                    }
                    appendPacketLog(
                        transport = "Virtual Rig",
                        summary = if (sendPowerOnly) "Power ${if (s.isPowerOn) "ON" else "OFF"}" else "${s.effect.shortCode} ${s.primaryColor.toHex()}",
                        hexPayload = CoolMoonProtocolEncoder.toHexString(previewFrame),
                        delivered = true
                    )
                }
            }
        }
    }

    private fun appendPacketLog(
        transport: String,
        summary: String,
        hexPayload: String,
        delivered: Boolean
    ) {
        val entry = PacketLogEntry(
            id = packetCounter++,
            timestamp = timeFormat.format(Date()),
            transport = transport,
            summary = summary,
            hexPayload = hexPayload,
            delivered = delivered
        )
        _uiState.update { state ->
            state.copy(packetLog = (listOf(entry) + state.packetLog).take(20))
        }
    }

    private fun checkWifiNetwork(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }

    override fun onCleared() {
        bleManager.stopScan()
        bleManager.disconnect()
        super.onCleared()
    }
}
