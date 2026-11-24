package com.example.model

import androidx.compose.ui.graphics.Color
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class RgbColor(
    val red: Int,
    val green: Int,
    val blue: Int
) {
    fun toComposeColor(): Color = Color(
        red = red.coerceIn(0, 255) / 255f,
        green = green.coerceIn(0, 255) / 255f,
        blue = blue.coerceIn(0, 255) / 255f
    )

    fun toHex(): String = "#%02X%02X%02X".format(
        red.coerceIn(0, 255),
        green.coerceIn(0, 255),
        blue.coerceIn(0, 255)
    )

    companion object {
        val Cyan = RgbColor(0, 240, 255)
        val Ultraviolet = RgbColor(168, 85, 247)
        val PlasmaCoral = RgbColor(255, 61, 127)
        val Emerald = RgbColor(34, 229, 138)
        val Amber = RgbColor(255, 168, 0)
        val FrostWhite = RgbColor(225, 242, 255)
        val Cobalt = RgbColor(37, 99, 235)
        val Crimson = RgbColor(239, 68, 68)

        fun fromComposeColor(color: Color): RgbColor = RgbColor(
            red = (color.red * 255f).toInt().coerceIn(0, 255),
            green = (color.green * 255f).toInt().coerceIn(0, 255),
            blue = (color.blue * 255f).toInt().coerceIn(0, 255)
        )
    }
}

enum class LightingEffect(
    val displayName: String,
    val shortCode: String,
    val protocolModeByte: Int,
    val supportsDualColor: Boolean,
    val description: String
) {
    STATIC("Static Glow", "STAT", 0x00, false, "Solid uniform phosphor illumination across every LED"),
    BREATHING("Breathing", "BRTH", 0x26, true, "Smooth sine-wave fade alternating between primary and secondary hues"),
    RAINBOW_WAVE("Rainbow Wave", "RNBW", 0x25, false, "360-degree addressable ARGB spectrum rotation"),
    DUAL_RING_CHASE("Dual-Ring Chase", "RING", 0x2A, true, "Counter-rotating outer ring and inner hub comet arcs"),
    COMET("Comet Trail", "CMET", 0x2C, true, "High-speed directional phosphor streak with fading tail"),
    MARQUEE("Marquee Sync", "MRQE", 0x2E, true, "Synchronized marquee pulses across all connected fan ports"),
    METEOR("Meteor Shower", "METR", 0x30, true, "Staggered crystalline bursts cascading across zones"),
    CANDLE_FLICKER("Plasma Flicker", "FLKR", 0x32, false, "Organic energy shimmer for ambient desk builds"),
    MOTHERBOARD_SYNC("M/B 5V Sync", "SYNC", 0xFE, false, "Pass-through control to motherboard 3-pin 5V ARGB header")
}

enum class FanCurveMode(
    val displayName: String,
    val targetRpm: Int,
    val dutyCyclePercent: Int
) {
    SILENT("Silent", 750, 35),
    BALANCED("Balanced", 1250, 58),
    PERFORMANCE("Performance", 1750, 82),
    FULL_BLAST("Full Blast", 2200, 100)
}

enum class ZoneKind {
    FAN_ARGB,
    LED_STRIP
}

@JsonClass(generateAdapter = true)
data class HubZone(
    val id: Int,
    val label: String,
    val kind: ZoneKind,
    val ledCount: Int,
    val enabled: Boolean = true,
    val syncWithMaster: Boolean = true,
    val overrideColor: RgbColor? = null
)

@JsonClass(generateAdapter = true)
data class LightingPreset(
    val id: String,
    val name: String,
    val tagline: String,
    val primaryColor: RgbColor,
    val secondaryColor: RgbColor,
    val effect: LightingEffect,
    val brightness: Int,
    val speed: Int,
    val fanCurve: FanCurveMode,
    val isBuiltIn: Boolean = false
)

enum class ConnectionMode(val label: String) {
    VIRTUAL_RIG("Virtual Rig"),
    BLE_GATT("Bluetooth LE"),
    WIFI_LAN("Wi-Fi LAN"),
    IR_BLASTER("IR Blaster")
}

enum class WifiProtocol(val label: String, val defaultPort: Int) {
    MAGIC_HOME_TCP("MagicHome / CoolMoon TCP", 5577),
    WLED_HTTP_JSON("WLED JSON HTTP", 80),
    WLED_UDP_DRGB("WLED UDP Realtime", 21324)
}

@JsonClass(generateAdapter = true)
data class WifiControllerConfig(
    val ipAddress: String = "192.168.1.50",
    val port: Int = 5577,
    val protocol: WifiProtocol = WifiProtocol.MAGIC_HOME_TCP
)

data class BleControllerDevice(
    val name: String,
    val address: String,
    val rssi: Int,
    val isLikelyRgbController: Boolean
)

data class PacketLogEntry(
    val id: Long,
    val timestamp: String,
    val transport: String,
    val summary: String,
    val hexPayload: String,
    val delivered: Boolean
)

enum class IrRemoteKey(
    val label: String,
    val shortLabel: String,
    val necCommandByte: Int,
    val accentHex: Long? = null
) {
    POWER_ON("Power On", "ON", 0x45),
    POWER_OFF("Power Off", "OFF", 0x47),
    MB_SYNC("M/B Sync", "M/B", 0x46),
    AUTO_CYCLE("Auto Mode", "AUTO", 0x44),
    SPEED_UP("Speed +", "SPD+", 0x40),
    SPEED_DOWN("Speed -", "SPD-", 0x43),
    BRIGHT_UP("Bright +", "BRT+", 0x07),
    BRIGHT_DOWN("Bright -", "BRT-", 0x15),
    MODE_NEXT("Mode +", "MOD+", 0x09),
    MODE_PREV("Mode -", "MOD-", 0x16),
    FAN_SPEED_UP("Fan RPM +", "FAN+", 0x19),
    FAN_SPEED_DOWN("Fan RPM -", "FAN-", 0x0D),
    COLOR_RED("Red", "R", 0x0C, 0xFFEF4444),
    COLOR_GREEN("Green", "G", 0x18, 0xFF22E58A),
    COLOR_BLUE("Blue", "B", 0x5E, 0xFF3B82F6),
    COLOR_WHITE("White", "W", 0x08, 0xFFE2F1FF),
    COLOR_CYAN("Cyan", "CYN", 0x1C, 0xFF00F0FF),
    COLOR_PURPLE("Violet", "VLT", 0x5A, 0xFFA855F7),
    COLOR_YELLOW("Amber", "AMB", 0x42, 0xFFFFB020),
    COLOR_PINK("Coral", "CRL", 0x52, 0xFFFF3D7F),
    RAINBOW("Rainbow", "RNBW", 0x4A)
}
