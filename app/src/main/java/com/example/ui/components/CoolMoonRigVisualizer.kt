package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.FanCurveMode
import com.example.model.HubZone
import com.example.model.LightingEffect
import com.example.model.RgbColor
import com.example.model.ZoneKind
import com.example.ui.theme.ObsidianCanvas
import com.example.ui.theme.ObsidianOutline
import com.example.ui.theme.ObsidianSurfaceElevated
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun CoolMoonRigVisualizer(
    isPowerOn: Boolean,
    primaryColor: RgbColor,
    secondaryColor: RgbColor,
    effect: LightingEffect,
    brightness: Int,
    speed: Int,
    fanCurve: FanCurveMode,
    directionCw: Boolean,
    zones: List<HubZone>,
    selectedZoneId: Int?,
    onSelectZone: (Int?) -> Unit,
    modifier: Modifier = Modifier
) {
    var effectPhase by remember { mutableFloatStateOf(0f) }
    var bladeAngle by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(isPowerOn, speed, fanCurve, directionCw) {
        var lastNanos = 0L
        while (true) {
            withFrameNanos { frameNanos ->
                if (lastNanos != 0L) {
                    val dt = ((frameNanos - lastNanos) / 1_000_000_000f).coerceAtMost(0.05f)
                    val rpmDegPerSec = when (fanCurve) {
                        FanCurveMode.SILENT -> 150f
                        FanCurveMode.BALANCED -> 270f
                        FanCurveMode.PERFORMANCE -> 420f
                        FanCurveMode.FULL_BLAST -> 600f
                    }
                    bladeAngle = (bladeAngle + rpmDegPerSec * dt) % 360f
                    if (isPowerOn) {
                        val speedFactor = 0.25f + (speed / 100f) * 2.2f
                        val dir = if (directionCw) 1f else -1f
                        effectPhase = (effectPhase + dt * speedFactor * dir + 10f) % 10f
                    }
                }
                lastNanos = frameNanos
            }
        }
    }

    val fanZones = remember(zones) {
        zones.filter { it.kind == ZoneKind.FAN_ARGB }.take(4)
    }
    val stripZones = remember(zones) {
        zones.filter { it.kind == ZoneKind.LED_STRIP }.take(2)
    }

    val pColor = primaryColor.toComposeColor()
    val sColor = secondaryColor.toComposeColor()
    val dimFactor = if (isPowerOn) (0.2f + 0.8f * (brightness / 100f)) else 0.04f

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = "Live CoolMoon ARGB chassis rig visualizer" },
        shape = MaterialTheme.shapes.large,
        color = ObsidianSurfaceElevated,
        tonalElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, ObsidianOutline, MaterialTheme.shapes.large)
                .padding(14.dp)
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
                    Box(
                        modifier = Modifier
                            .size(9.dp)
                            .clip(CircleShape)
                            .background(if (isPowerOn) pColor else Color(0xFF475569))
                    )
                    Text(
                        text = if (isPowerOn) "${effect.displayName} • ${brightness}%" else "RIG STANDBY",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = "${fanCurve.targetRpm} RPM",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(214.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                if (isPowerOn) pColor.copy(alpha = 0.16f * dimFactor) else ObsidianCanvas,
                                ObsidianCanvas
                            )
                        )
                    )
                    .border(1.dp, ObsidianOutline.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val fanAreaHeight = size.height * 0.78f
                    val fanRadius = (minOf(size.width / 4.8f, fanAreaHeight / 1.95f)).coerceAtLeast(24f)
                    val positions = listOf(
                        Offset(size.width * 0.22f, fanAreaHeight * 0.36f),
                        Offset(size.width * 0.50f, fanAreaHeight * 0.36f),
                        Offset(size.width * 0.78f, fanAreaHeight * 0.36f),
                        Offset(size.width * 0.50f, fanAreaHeight * 0.78f)
                    )

                    // Draw chassis airflow lines faintly
                    drawLine(
                        color = Color.White.copy(alpha = 0.04f),
                        start = Offset(24f, fanAreaHeight * 0.57f),
                        end = Offset(size.width - 24f, fanAreaHeight * 0.57f),
                        strokeWidth = 1.5f
                    )

                    fanZones.forEachIndexed { idx, zone ->
                        val center = positions.getOrElse(idx) { Offset(size.width * 0.5f, fanAreaHeight * 0.5f) }
                        val zonePrimary = if (!zone.syncWithMaster && zone.overrideColor != null) {
                            zone.overrideColor.toComposeColor()
                        } else {
                            pColor
                        }
                        val isZoneActive = isPowerOn && zone.enabled
                        val isSelected = selectedZoneId == zone.id

                        drawCoolMoonFan(
                            center = center,
                            radius = fanRadius,
                            bladeAngle = bladeAngle + idx * 17f,
                            phase = effectPhase,
                            fanOffset = idx * 0.18f,
                            isActive = isZoneActive,
                            isSelected = isSelected,
                            effect = effect,
                            primary = zonePrimary,
                            secondary = sColor,
                            dimFactor = dimFactor
                        )
                    }

                    // Bottom ARGB LED Strip Bar
                    val stripZone = stripZones.firstOrNull()
                    val stripActive = isPowerOn && (stripZone?.enabled != false)
                    val stripPrimary = if (stripZone != null && !stripZone.syncWithMaster && stripZone.overrideColor != null) {
                        stripZone.overrideColor.toComposeColor()
                    } else {
                        pColor
                    }
                    val stripY = size.height * 0.89f
                    val stripStartX = size.width * 0.10f
                    val stripWidth = size.width * 0.80f
                    val segments = 24
                    val segGap = 4f
                    val segWidth = (stripWidth - segGap * (segments - 1)) / segments

                    drawRoundRect(
                        color = Color(0xFF141A29),
                        topLeft = Offset(stripStartX - 8f, stripY - 9f),
                        size = Size(stripWidth + 16f, 18f),
                        cornerRadius = CornerRadius(9f, 9f)
                    )

                    for (i in 0 until segments) {
                        val t = i / segments.toFloat()
                        val ledColor = if (!stripActive) {
                            Color(0xFF232B3E)
                        } else {
                            sampleArgbLedColor(
                                t = t,
                                phase = effectPhase,
                                effect = effect,
                                primary = stripPrimary,
                                secondary = sColor,
                                dimFactor = dimFactor
                            )
                        }
                        val sx = stripStartX + i * (segWidth + segGap)
                        if (stripActive) {
                            drawCircle(
                                color = ledColor.copy(alpha = 0.25f * dimFactor),
                                radius = segWidth * 1.15f,
                                center = Offset(sx + segWidth / 2f, stripY)
                            )
                        }
                        drawRoundRect(
                            color = ledColor,
                            topLeft = Offset(sx, stripY - 3.5f),
                            size = Size(segWidth, 7f),
                            cornerRadius = CornerRadius(3f, 3f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Quick Zone Focus Selector Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ZoneFocusPill(
                    label = "All Sync",
                    selected = selectedZoneId == null,
                    accent = pColor,
                    onClick = { onSelectZone(null) },
                    modifier = Modifier.testTag("zone_focus_all")
                )
                fanZones.take(3).forEach { zone ->
                    ZoneFocusPill(
                        label = "Fan ${zone.id}",
                        selected = selectedZoneId == zone.id,
                        accent = (zone.overrideColor ?: primaryColor).toComposeColor(),
                        onClick = { onSelectZone(if (selectedZoneId == zone.id) null else zone.id) },
                        modifier = Modifier.testTag("zone_focus_${zone.id}")
                    )
                }
                stripZones.firstOrNull()?.let { strip ->
                    ZoneFocusPill(
                        label = "Strip",
                        selected = selectedZoneId == strip.id,
                        accent = (strip.overrideColor ?: primaryColor).toComposeColor(),
                        onClick = { onSelectZone(if (selectedZoneId == strip.id) null else strip.id) },
                        modifier = Modifier.testTag("zone_focus_strip")
                    )
                }
            }
        }
    }
}

@Composable
private fun ZoneFocusPill(
    label: String,
    selected: Boolean,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg = if (selected) accent.copy(alpha = 0.18f) else Color(0xFF141926)
    val borderColor = if (selected) accent else ObsidianOutline
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .border(1.dp, borderColor, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun DrawScope.drawCoolMoonFan(
    center: Offset,
    radius: Float,
    bladeAngle: Float,
    phase: Float,
    fanOffset: Float,
    isActive: Boolean,
    isSelected: Boolean,
    effect: LightingEffect,
    primary: Color,
    secondary: Color,
    dimFactor: Float
) {
    // Outer chassis frame ring
    if (isActive) {
        val glowColor = sampleArgbLedColor(
            t = fanOffset,
            phase = phase,
            effect = effect,
            primary = primary,
            secondary = secondary,
            dimFactor = dimFactor
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(glowColor.copy(alpha = 0.30f * dimFactor), Color.Transparent),
                center = center,
                radius = radius * 1.48f
            ),
            radius = radius * 1.48f,
            center = center
        )
    }

    drawCircle(
        color = Color(0xFF151B2B),
        radius = radius * 1.06f,
        center = center
    )

    if (isSelected) {
        drawCircle(
            color = Color.White.copy(alpha = 0.75f),
            radius = radius * 1.15f,
            center = center,
            style = Stroke(width = 2.5f)
        )
    }

    // Fan blades rotating inside
    rotate(degrees = bladeAngle, pivot = center) {
        val bladeCount = 9
        for (b in 0 until bladeCount) {
            val angleRad = (b * 2.0 * PI / bladeCount).toFloat()
            val innerR = radius * 0.28f
            val outerR = radius * 0.84f
            val sweep = 0.38f
            val path = Path().apply {
                moveTo(
                    center.x + cos(angleRad) * innerR,
                    center.y + sin(angleRad) * innerR
                )
                quadraticTo(
                    center.x + cos(angleRad + sweep * 0.55f) * (outerR * 0.68f),
                    center.y + sin(angleRad + sweep * 0.55f) * (outerR * 0.68f),
                    center.x + cos(angleRad + sweep) * outerR,
                    center.y + sin(angleRad + sweep) * outerR
                )
                lineTo(
                    center.x + cos(angleRad + sweep * 0.45f) * outerR,
                    center.y + sin(angleRad + sweep * 0.45f) * outerR
                )
                close()
            }
            drawPath(path = path, color = Color(0xFF252E44))
        }
    }

    // 16 Outer Addressable Ring LEDs
    val outerLedCount = 16
    val outerRingRadius = radius * 0.94f
    for (i in 0 until outerLedCount) {
        val angle = (i * 2.0 * PI / outerLedCount).toFloat()
        val ledPos = Offset(
            x = center.x + cos(angle) * outerRingRadius,
            y = center.y + sin(angle) * outerRingRadius
        )
        val ledColor = if (!isActive) {
            Color(0xFF262F45)
        } else {
            sampleArgbLedColor(
                t = (i / outerLedCount.toFloat() + fanOffset) % 1f,
                phase = phase,
                effect = effect,
                primary = primary,
                secondary = secondary,
                dimFactor = dimFactor
            )
        }
        if (isActive) {
            drawCircle(
                color = ledColor.copy(alpha = 0.35f * dimFactor),
                radius = radius * 0.14f,
                center = ledPos
            )
        }
        drawCircle(
            color = ledColor,
            radius = radius * 0.072f,
            center = ledPos
        )
    }

    // Inner Hub Cap + 8 Hub LEDs
    drawCircle(
        color = Color(0xFF0E121D),
        radius = radius * 0.31f,
        center = center
    )
    val innerLedCount = 8
    val innerRingRadius = radius * 0.24f
    for (i in 0 until innerLedCount) {
        val angle = (i * 2.0 * PI / innerLedCount).toFloat()
        val ledPos = Offset(
            x = center.x + cos(angle) * innerRingRadius,
            y = center.y + sin(angle) * innerRingRadius
        )
        val ledColor = if (!isActive) {
            Color(0xFF262F45)
        } else {
            sampleArgbLedColor(
                t = (1f - (i / innerLedCount.toFloat()) + fanOffset) % 1f,
                phase = phase * 1.15f,
                effect = effect,
                primary = primary,
                secondary = secondary,
                dimFactor = dimFactor
            )
        }
        drawCircle(
            color = ledColor,
            radius = radius * 0.048f,
            center = ledPos
        )
    }

    // Metallic CoolMoon hub badge center
    drawCircle(
        color = Color(0xFF1F283D),
        radius = radius * 0.12f,
        center = center
    )
}

private fun sampleArgbLedColor(
    t: Float,
    phase: Float,
    effect: LightingEffect,
    primary: Color,
    secondary: Color,
    dimFactor: Float
): Color {
    val normT = ((t % 1f) + 1f) % 1f
    val raw = when (effect) {
        LightingEffect.STATIC, LightingEffect.MOTHERBOARD_SYNC -> primary
        LightingEffect.BREATHING -> {
            val wave = (sin(phase * 2.4f) + 1f) / 2f
            lerpColor(primary, secondary, wave)
        }
        LightingEffect.RAINBOW_WAVE -> {
            val hue = ((normT + phase * 0.45f) % 1f) * 360f
            Color.hsv(hue = (hue + 360f) % 360f, saturation = 0.92f, value = 1f)
        }
        LightingEffect.DUAL_RING_CHASE -> {
            val head = (phase * 0.55f) % 1f
            val dist = circularDistance(normT, head)
            val tail = (1f - (dist * 3.2f).coerceIn(0f, 1f))
            lerpColor(secondary.copy(alpha = 0.35f), primary, tail)
        }
        LightingEffect.COMET -> {
            val head = (phase * 0.7f) % 1f
            val diff = ((head - normT) + 1f) % 1f
            val intensity = (1f - (diff * 2.5f).coerceIn(0f, 1f))
            lerpColor(secondary.copy(alpha = 0.22f), primary, intensity)
        }
        LightingEffect.MARQUEE -> {
            val bucket = (((normT * 8f) + phase * 3.5f).toInt()) % 2
            if (bucket == 0) primary else secondary
        }
        LightingEffect.METEOR -> {
            val wave = (sin((normT * 2f * PI.toFloat()) + phase * 4.5f) + 1f) / 2f
            val sparkle = if (wave > 0.78f) 1f else wave * 0.4f
            lerpColor(secondary.copy(alpha = 0.25f), primary, sparkle)
        }
        LightingEffect.CANDLE_FLICKER -> {
            val flicker = 0.55f + 0.45f * sin(phase * 7f + normT * 9f)
            primary.copy(alpha = flicker.coerceIn(0.25f, 1f))
        }
    }
    return Color(
        red = (raw.red * dimFactor).coerceIn(0f, 1f),
        green = (raw.green * dimFactor).coerceIn(0f, 1f),
        blue = (raw.blue * dimFactor).coerceIn(0f, 1f),
        alpha = raw.alpha
    )
}

private fun circularDistance(a: Float, b: Float): Float {
    val d = kotlin.math.abs(a - b)
    return minOf(d, 1f - d)
}

private fun lerpColor(a: Color, b: Color, t: Float): Color {
    val clamped = t.coerceIn(0f, 1f)
    return Color(
        red = a.red + (b.red - a.red) * clamped,
        green = a.green + (b.green - a.green) * clamped,
        blue = a.blue + (b.blue - a.blue) * clamped,
        alpha = a.alpha + (b.alpha - a.alpha) * clamped
    )
}
