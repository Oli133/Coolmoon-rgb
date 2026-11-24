package com.example.data

import com.example.model.FanCurveMode
import com.example.model.LightingEffect
import com.example.model.RgbColor
import kotlinx.coroutines.delay

data class AiLightingPlan(
    val isSampleResponse: Boolean,
    val modelLabel: String,
    val planTitle: String,
    val thinkingSummary: String,
    val rationale: String,
    val primaryColor: RgbColor,
    val secondaryColor: RgbColor,
    val effect: LightingEffect,
    val brightness: Int,
    val speed: Int,
    val fanCurve: FanCurveMode,
    val zoneNotes: List<String>
)

interface LightingArchitectRepository {
    val isFirebaseConnected: Boolean
    suspend fun designLightingPlan(query: String, activeZoneCount: Int): Result<AiLightingPlan>
}

/**
 * Placeholder implementation used when Firebase AI Logic is not enabled.
 * Clearly labels every output as a sample response and never calls a direct API key or REST endpoint.
 */
class PlaceholderLightingArchitectRepository : LightingArchitectRepository {

    override val isFirebaseConnected: Boolean = false

    override suspend fun designLightingPlan(
        query: String,
        activeZoneCount: Int
    ): Result<AiLightingPlan> {
        delay(550)
        val q = query.lowercase()
        val plan = when {
            q.contains("warm") || q.contains("sunset") || q.contains("amber") || q.contains("cozy") -> {
                AiLightingPlan(
                    isSampleResponse = true,
                    modelLabel = "Sample Plan (Firebase AI Logic Disabled)",
                    planTitle = "Sunset Thermal Bias",
                    thinkingSummary = "Sample reasoning: Balanced warm kelvin tones (2700K-equivalent amber/coral) reduce evening eye strain on $activeZoneCount active zones while maintaining quiet 750 RPM airflow.",
                    rationale = "Uses slow breathing modulation between Warm Amber and Plasma Coral at 68% brightness so chassis glass stays atmospheric without monitor glare.",
                    primaryColor = RgbColor(255, 148, 24),
                    secondaryColor = RgbColor(255, 61, 105),
                    effect = LightingEffect.BREATHING,
                    brightness = 68,
                    speed = 34,
                    fanCurve = FanCurveMode.SILENT,
                    zoneNotes = listOf(
                        "Fans 1-3 (Intake): Warm Amber slow breathing at 35% PWM",
                        "Fans 4-6 (Radiator/Exhaust): Coral counter-fade for depth",
                        "LED Strips: 68% diffused bias glow behind GPU & desk edge"
                    )
                )
            }
            q.contains("game") || q.contains("fps") || q.contains("esport") || q.contains("thermal") -> {
                AiLightingPlan(
                    isSampleResponse = true,
                    modelLabel = "Sample Plan (Firebase AI Logic Disabled)",
                    planTitle = "Overclocked Crimson Vector",
                    thinkingSummary = "Sample reasoning: High-contrast kinetic comet arcs across $activeZoneCount ARGB ports paired with an 82% Performance fan curve for sustained boost clocks.",
                    rationale = "Sharp crimson-to-cyan kinetic trails create crisp motion contrast through tempered glass while prioritizing radiator static pressure.",
                    primaryColor = RgbColor(255, 45, 85),
                    secondaryColor = RgbColor(0, 232, 255),
                    effect = LightingEffect.COMET,
                    brightness = 96,
                    speed = 82,
                    fanCurve = FanCurveMode.PERFORMANCE,
                    zoneNotes = listOf(
                        "Front Intake Array: High-velocity Comet sweep (CW)",
                        "AIO Radiator Fans: 1750 RPM Performance curve",
                        "GPU Bracket Strip: Synchronized dual-tone accent"
                    )
                )
            }
            q.contains("focus") || q.contains("studio") || q.contains("minimal") || q.contains("work") -> {
                AiLightingPlan(
                    isSampleResponse = true,
                    modelLabel = "Sample Plan (Firebase AI Logic Disabled)",
                    planTitle = "Color-Neutral Obsidian Bias",
                    thinkingSummary = "Sample reasoning: Eliminates high-frequency flicker across $activeZoneCount zones using static neutral ice-cyan bias lighting to preserve monitor color perception.",
                    rationale = "Static low-saturation frost illumination minimizes distracting peripheral motion during coding, editing, or studio mixing.",
                    primaryColor = RgbColor(156, 218, 255),
                    secondaryColor = RgbColor(64, 118, 210),
                    effect = LightingEffect.STATIC,
                    brightness = 52,
                    speed = 20,
                    fanCurve = FanCurveMode.SILENT,
                    zoneNotes = listOf(
                        "All Fan Ports: Static 52% flicker-free frost glow",
                        "Acoustic Profile: Locked to Silent 750 RPM curve",
                        "LED Strips: Soft indirect rim light for display contrast"
                    )
                )
            }
            else -> {
                AiLightingPlan(
                    isSampleResponse = true,
                    modelLabel = "Sample Plan (Firebase AI Logic Disabled)",
                    planTitle = "Midnight Cyberpunk Dual-Ring",
                    thinkingSummary = "Sample reasoning: Mapped a counter-rotating Cyan + Ultraviolet phosphor split across $activeZoneCount CoolMoon ARGB channels with balanced acoustic headroom.",
                    rationale = "Pairs CoolMoon's 16-LED outer ring and 8-LED inner hub using Dual-Ring Chase so adjacent fans harmonize without washing out dark internals.",
                    primaryColor = RgbColor(0, 240, 255),
                    secondaryColor = RgbColor(168, 85, 247),
                    effect = LightingEffect.DUAL_RING_CHASE,
                    brightness = 88,
                    speed = 64,
                    fanCurve = FanCurveMode.BALANCED,
                    zoneNotes = listOf(
                        "Fans 1-4: Counter-rotating Cyan & Ultraviolet ring arcs",
                        "AIO Radiator: Balanced 1250 RPM (58% duty) cooling curve",
                        "Chassis & GPU Strips: Synchronized phosphor tail"
                    )
                )
            }
        }
        return Result.success(plan)
    }
}
