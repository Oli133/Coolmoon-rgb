package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.FanCurveMode
import com.example.model.HubZone
import com.example.model.LightingEffect
import com.example.model.LightingPreset
import com.example.model.RgbColor
import com.example.model.WifiControllerConfig
import com.example.model.WifiProtocol
import com.example.model.ZoneKind
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.coolMoonDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "coolmoon_rgb_prefs"
)

data class PersistedRigSnapshot(
    val isPowerOn: Boolean = true,
    val primaryColor: RgbColor = RgbColor.Cyan,
    val secondaryColor: RgbColor = RgbColor.Ultraviolet,
    val effect: LightingEffect = LightingEffect.DUAL_RING_CHASE,
    val brightness: Int = 88,
    val speed: Int = 62,
    val fanCurve: FanCurveMode = FanCurveMode.BALANCED,
    val directionCw: Boolean = true,
    val mbSyncEnabled: Boolean = false,
    val zones: List<HubZone> = defaultCoolMoonZones(),
    val presets: List<LightingPreset> = builtInCoolMoonPresets(),
    val wifiConfig: WifiControllerConfig = WifiControllerConfig()
)

fun defaultCoolMoonZones(): List<HubZone> = listOf(
    HubZone(1, "Fan 1 • Front Top", ZoneKind.FAN_ARGB, 16, enabled = true),
    HubZone(2, "Fan 2 • Front Mid", ZoneKind.FAN_ARGB, 16, enabled = true),
    HubZone(3, "Fan 3 • Front Low", ZoneKind.FAN_ARGB, 16, enabled = true),
    HubZone(4, "Fan 4 • Rear Exhaust", ZoneKind.FAN_ARGB, 16, enabled = true),
    HubZone(5, "Fan 5 • AIO Rad L", ZoneKind.FAN_ARGB, 24, enabled = true),
    HubZone(6, "Fan 6 • AIO Rad R", ZoneKind.FAN_ARGB, 24, enabled = true),
    HubZone(7, "Fan 7 • Bottom Intake", ZoneKind.FAN_ARGB, 16, enabled = false),
    HubZone(8, "Fan 8 • Aux Port", ZoneKind.FAN_ARGB, 16, enabled = false),
    HubZone(9, "LED 1 • Chassis Strip", ZoneKind.LED_STRIP, 30, enabled = true),
    HubZone(10, "LED 2 • GPU Bracket", ZoneKind.LED_STRIP, 18, enabled = true)
)

fun builtInCoolMoonPresets(): List<LightingPreset> = listOf(
    LightingPreset(
        id = "builtin_arctic_frost",
        name = "Arctic Frost",
        tagline = "Glacial cyan & ice-white dual-ring rotation",
        primaryColor = RgbColor(0, 240, 255),
        secondaryColor = RgbColor(190, 238, 255),
        effect = LightingEffect.DUAL_RING_CHASE,
        brightness = 90,
        speed = 55,
        fanCurve = FanCurveMode.BALANCED,
        isBuiltIn = true
    ),
    LightingPreset(
        id = "builtin_cyber_pulse",
        name = "Cyberpunk Pulse",
        tagline = "Electric ultraviolet & plasma coral comet streaks",
        primaryColor = RgbColor(168, 85, 247),
        secondaryColor = RgbColor(255, 61, 127),
        effect = LightingEffect.COMET,
        brightness = 95,
        speed = 74,
        fanCurve = FanCurveMode.PERFORMANCE,
        isBuiltIn = true
    ),
    LightingPreset(
        id = "builtin_solar_flare",
        name = "Solar Flare",
        tagline = "Warm amber breathing glow for stealth night setups",
        primaryColor = RgbColor(255, 145, 0),
        secondaryColor = RgbColor(255, 48, 85),
        effect = LightingEffect.BREATHING,
        brightness = 72,
        speed = 38,
        fanCurve = FanCurveMode.SILENT,
        isBuiltIn = true
    ),
    LightingPreset(
        id = "builtin_emerald_matrix",
        name = "Emerald Matrix",
        tagline = "Cascading phosphor green meteor bursts",
        primaryColor = RgbColor(34, 229, 138),
        secondaryColor = RgbColor(0, 175, 210),
        effect = LightingEffect.METEOR,
        brightness = 85,
        speed = 65,
        fanCurve = FanCurveMode.BALANCED,
        isBuiltIn = true
    ),
    LightingPreset(
        id = "builtin_spectrum_prism",
        name = "Prism Wave",
        tagline = "Full 360-degree ARGB rainbow spectrum sweep",
        primaryColor = RgbColor(0, 240, 255),
        secondaryColor = RgbColor(255, 61, 127),
        effect = LightingEffect.RAINBOW_WAVE,
        brightness = 100,
        speed = 68,
        fanCurve = FanCurveMode.PERFORMANCE,
        isBuiltIn = true
    ),
    LightingPreset(
        id = "builtin_stealth_studio",
        name = "Midnight Studio",
        tagline = "Low-glare deep indigo static bias lighting",
        primaryColor = RgbColor(64, 96, 255),
        secondaryColor = RgbColor(30, 41, 90),
        effect = LightingEffect.STATIC,
        brightness = 45,
        speed = 25,
        fanCurve = FanCurveMode.SILENT,
        isBuiltIn = true
    )
)

class CoolMoonPreferencesRepository(private val context: Context) {

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val zoneListAdapter = moshi.adapter<List<HubZone>>(
        Types.newParameterizedType(List::class.java, HubZone::class.java)
    )
    private val presetListAdapter = moshi.adapter<List<LightingPreset>>(
        Types.newParameterizedType(List::class.java, LightingPreset::class.java)
    )
    private val wifiConfigAdapter = moshi.adapter(WifiControllerConfig::class.java)

    private object Keys {
        val POWER_ON = booleanPreferencesKey("power_on")
        val PRIMARY_RGB = intPreferencesKey("primary_rgb")
        val SECONDARY_RGB = intPreferencesKey("secondary_rgb")
        val EFFECT = stringPreferencesKey("effect")
        val BRIGHTNESS = intPreferencesKey("brightness")
        val SPEED = intPreferencesKey("speed")
        val FAN_CURVE = stringPreferencesKey("fan_curve")
        val DIRECTION_CW = booleanPreferencesKey("direction_cw")
        val MB_SYNC = booleanPreferencesKey("mb_sync")
        val ZONES_JSON = stringPreferencesKey("zones_json")
        val CUSTOM_PRESETS_JSON = stringPreferencesKey("custom_presets_json")
        val WIFI_CONFIG_JSON = stringPreferencesKey("wifi_config_json")
    }

    val snapshotFlow: Flow<PersistedRigSnapshot> = context.coolMoonDataStore.data.map { prefs ->
        val primaryInt = prefs[Keys.PRIMARY_RGB]
        val secondaryInt = prefs[Keys.SECONDARY_RGB]
        val effectName = prefs[Keys.EFFECT]
        val fanCurveName = prefs[Keys.FAN_CURVE]
        val zones = prefs[Keys.ZONES_JSON]?.let { json ->
            runCatching { zoneListAdapter.fromJson(json) }.getOrNull()
        } ?: defaultCoolMoonZones()
        val customPresets = prefs[Keys.CUSTOM_PRESETS_JSON]?.let { json ->
            runCatching { presetListAdapter.fromJson(json) }.getOrNull()
        }.orEmpty()
        val wifiConfig = prefs[Keys.WIFI_CONFIG_JSON]?.let { json ->
            runCatching { wifiConfigAdapter.fromJson(json) }.getOrNull()
        } ?: WifiControllerConfig(protocol = WifiProtocol.MAGIC_HOME_TCP)

        PersistedRigSnapshot(
            isPowerOn = prefs[Keys.POWER_ON] ?: true,
            primaryColor = primaryInt?.let(::unpackRgb) ?: RgbColor.Cyan,
            secondaryColor = secondaryInt?.let(::unpackRgb) ?: RgbColor.Ultraviolet,
            effect = effectName?.let { runCatching { LightingEffect.valueOf(it) }.getOrNull() }
                ?: LightingEffect.DUAL_RING_CHASE,
            brightness = prefs[Keys.BRIGHTNESS] ?: 88,
            speed = prefs[Keys.SPEED] ?: 62,
            fanCurve = fanCurveName?.let { runCatching { FanCurveMode.valueOf(it) }.getOrNull() }
                ?: FanCurveMode.BALANCED,
            directionCw = prefs[Keys.DIRECTION_CW] ?: true,
            mbSyncEnabled = prefs[Keys.MB_SYNC] ?: false,
            zones = zones,
            presets = builtInCoolMoonPresets() + customPresets,
            wifiConfig = wifiConfig
        )
    }

    suspend fun saveActiveState(
        isPowerOn: Boolean,
        primaryColor: RgbColor,
        secondaryColor: RgbColor,
        effect: LightingEffect,
        brightness: Int,
        speed: Int,
        fanCurve: FanCurveMode,
        directionCw: Boolean,
        mbSyncEnabled: Boolean
    ) {
        context.coolMoonDataStore.edit { prefs ->
            prefs[Keys.POWER_ON] = isPowerOn
            prefs[Keys.PRIMARY_RGB] = packRgb(primaryColor)
            prefs[Keys.SECONDARY_RGB] = packRgb(secondaryColor)
            prefs[Keys.EFFECT] = effect.name
            prefs[Keys.BRIGHTNESS] = brightness
            prefs[Keys.SPEED] = speed
            prefs[Keys.FAN_CURVE] = fanCurve.name
            prefs[Keys.DIRECTION_CW] = directionCw
            prefs[Keys.MB_SYNC] = mbSyncEnabled
        }
    }

    suspend fun saveZones(zones: List<HubZone>) {
        context.coolMoonDataStore.edit { prefs ->
            prefs[Keys.ZONES_JSON] = zoneListAdapter.toJson(zones)
        }
    }

    suspend fun saveCustomPresets(allPresets: List<LightingPreset>) {
        val customOnly = allPresets.filterNot { it.isBuiltIn }
        context.coolMoonDataStore.edit { prefs ->
            prefs[Keys.CUSTOM_PRESETS_JSON] = presetListAdapter.toJson(customOnly)
        }
    }

    suspend fun saveWifiConfig(config: WifiControllerConfig) {
        context.coolMoonDataStore.edit { prefs ->
            prefs[Keys.WIFI_CONFIG_JSON] = wifiConfigAdapter.toJson(config)
        }
    }

    private fun packRgb(c: RgbColor): Int =
        ((c.red and 0xFF) shl 16) or ((c.green and 0xFF) shl 8) or (c.blue and 0xFF)

    private fun unpackRgb(packed: Int): RgbColor =
        RgbColor(
            red = (packed ushr 16) and 0xFF,
            green = (packed ushr 8) and 0xFF,
            blue = packed and 0xFF
        )
}
