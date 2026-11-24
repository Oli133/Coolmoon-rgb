package com.example.hardware

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.example.model.BleControllerDevice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

sealed interface BleConnectionState {
    data object Disconnected : BleConnectionState
    data class Connecting(val deviceName: String, val address: String) : BleConnectionState
    data class Connected(val deviceName: String, val address: String, val characteristicUuid: String) : BleConnectionState
    data class Error(val message: String) : BleConnectionState
}

class BleControllerManager(private val context: Context) {

    private val bluetoothManager: BluetoothManager? =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter?
        get() = bluetoothManager?.adapter

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _discoveredDevices = MutableStateFlow<List<BleControllerDevice>>(emptyList())
    val discoveredDevices: StateFlow<List<BleControllerDevice>> = _discoveredDevices.asStateFlow()

    private val _connectionState = MutableStateFlow<BleConnectionState>(BleConnectionState.Disconnected)
    val connectionState: StateFlow<BleConnectionState> = _connectionState.asStateFlow()

    private var activeGatt: BluetoothGatt? = null
    private var writableCharacteristic: BluetoothGattCharacteristic? = null

    private val knownRgbCharUuids = setOf(
        UUID.fromString("0000fff3-0000-1000-8000-00805f9b34fb"),
        UUID.fromString("0000ffe9-0000-1000-8000-00805f9b34fb"),
        UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb"),
        UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e")
    )

    private val rgbNameHints = listOf(
        "COOLMOON", "ARGB", "RGB", "LED", "ELK", "BLEDOM", "SP105", "SP110", "MAGIC", "QHM", "FAN"
    )

    val isBleSupported: Boolean
        get() = context.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE) &&
            bluetoothAdapter != null

    val isBluetoothEnabled: Boolean
        get() = bluetoothAdapter?.isEnabled == true

    fun requiredPermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT
            )
        } else {
            arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
    }

    fun hasBlePermissions(): Boolean {
        return requiredPermissions().all { perm ->
            ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED
        }
    }

    private val scanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device ?: return
            val name = result.scanRecord?.deviceName ?: device.name ?: "Unnamed BLE Device"
            val address = device.address ?: return
            val isLikelyRgb = rgbNameHints.any { hint ->
                name.uppercase().contains(hint)
            }
            val item = BleControllerDevice(
                name = name,
                address = address,
                rssi = result.rssi,
                isLikelyRgbController = isLikelyRgb
            )
            val current = _discoveredDevices.value.toMutableList()
            val existingIdx = current.indexOfFirst { it.address == address }
            if (existingIdx >= 0) {
                current[existingIdx] = item
            } else {
                current.add(item)
            }
            _discoveredDevices.value = current.sortedWith(
                compareByDescending<BleControllerDevice> { it.isLikelyRgbController }
                    .thenByDescending { it.rssi }
            )
        }

        override fun onScanFailed(errorCode: Int) {
            _isScanning.value = false
            _connectionState.value = BleConnectionState.Error("BLE scan failed (code $errorCode)")
        }
    }

    @SuppressLint("MissingPermission")
    fun startScan(): Result<Unit> {
        if (!hasBlePermissions()) {
            return Result.failure(IllegalStateException("Bluetooth permissions not granted"))
        }
        val adapter = bluetoothAdapter
            ?: return Result.failure(IllegalStateException("Bluetooth hardware unavailable"))
        if (!adapter.isEnabled) {
            return Result.failure(IllegalStateException("Bluetooth is turned off"))
        }
        val scanner = adapter.bluetoothLeScanner
            ?: return Result.failure(IllegalStateException("BLE scanner unavailable"))

        _discoveredDevices.value = emptyList()
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()
        return try {
            scanner.startScan(null, settings, scanCallback)
            _isScanning.value = true
            Result.success(Unit)
        } catch (e: SecurityException) {
            _isScanning.value = false
            Result.failure(e)
        } catch (e: Exception) {
            _isScanning.value = false
            Result.failure(e)
        }
    }

    @SuppressLint("MissingPermission")
    fun stopScan() {
        if (!_isScanning.value) return
        runCatching {
            if (hasBlePermissions()) {
                bluetoothAdapter?.bluetoothLeScanner?.stopScan(scanCallback)
            }
        }
        _isScanning.value = false
    }

    @SuppressLint("MissingPermission")
    fun connect(device: BleControllerDevice) {
        if (!hasBlePermissions()) {
            _connectionState.value = BleConnectionState.Error("Bluetooth Connect permission required")
            return
        }
        stopScan()
        disconnect()
        val btDevice = bluetoothAdapter?.getRemoteDevice(device.address)
        if (btDevice == null) {
            _connectionState.value = BleConnectionState.Error("Could not resolve BLE device ${device.address}")
            return
        }
        _connectionState.value = BleConnectionState.Connecting(device.name, device.address)

        activeGatt = btDevice.connectGatt(context, false, object : BluetoothGattCallback() {
            override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                if (newState == BluetoothProfile.STATE_CONNECTED && status == BluetoothGatt.GATT_SUCCESS) {
                    gatt.discoverServices()
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    writableCharacteristic = null
                    _connectionState.value = BleConnectionState.Disconnected
                    runCatching { gatt.close() }
                } else if (status != BluetoothGatt.GATT_SUCCESS) {
                    writableCharacteristic = null
                    _connectionState.value = BleConnectionState.Error("GATT connection error ($status)")
                    runCatching { gatt.close() }
                }
            }

            override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                if (status != BluetoothGatt.GATT_SUCCESS) {
                    _connectionState.value = BleConnectionState.Error("Service discovery failed ($status)")
                    return
                }
                val allChars = gatt.services.flatMap { it.characteristics }
                val preferred = allChars.firstOrNull { it.uuid in knownRgbCharUuids }
                    ?: allChars.firstOrNull { char ->
                        (char.properties and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) != 0 ||
                            (char.properties and BluetoothGattCharacteristic.PROPERTY_WRITE) != 0
                    }

                if (preferred != null) {
                    writableCharacteristic = preferred
                    _connectionState.value = BleConnectionState.Connected(
                        deviceName = device.name,
                        address = device.address,
                        characteristicUuid = preferred.uuid.toString().take(8).uppercase()
                    )
                } else {
                    _connectionState.value = BleConnectionState.Error(
                        "Connected to ${device.name}, but no writable RGB GATT characteristic found"
                    )
                }
            }
        })
    }

    @SuppressLint("MissingPermission")
    fun sendPayload(payload: ByteArray): Boolean {
        val gatt = activeGatt ?: return false
        val char = writableCharacteristic ?: return false
        if (!hasBlePermissions()) return false

        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val writeType = if ((char.properties and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) != 0) {
                    BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
                } else {
                    BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                }
                gatt.writeCharacteristic(char, payload, writeType) == BluetoothGatt.GATT_SUCCESS
            } else {
                @Suppress("DEPRECATION")
                char.value = payload
                @Suppress("DEPRECATION")
                gatt.writeCharacteristic(char)
            }
        } catch (_: Exception) {
            false
        }
    }

    @SuppressLint("MissingPermission")
    fun disconnect() {
        writableCharacteristic = null
        runCatching {
            activeGatt?.disconnect()
            activeGatt?.close()
        }
        activeGatt = null
        _connectionState.value = BleConnectionState.Disconnected
    }
}
