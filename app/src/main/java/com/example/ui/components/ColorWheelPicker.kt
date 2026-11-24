package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.model.RgbColor
import com.example.ui.theme.ObsidianOutline
import com.example.ui.theme.ObsidianSurfaceElevated
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ColorWheelControlDeck(
    primaryColor: RgbColor,
    secondaryColor: RgbColor,
    editingSecondary: Boolean,
    supportsDualColor: Boolean,
    activeZoneLabel: String?,
    onSelectEditingSecondary: (Boolean) -> Unit,
    onColorSelected: (RgbColor) -> Unit,
    modifier: Modifier = Modifier
) {
    val activeColor = if (editingSecondary && supportsDualColor) secondaryColor else primaryColor

    val quickSwatches = remember {
        listOf(
            "Cyan" to RgbColor.Cyan,
            "Violet" to RgbColor.Ultraviolet,
            "Coral" to RgbColor.PlasmaCoral,
            "Emerald" to RgbColor.Emerald,
            "Amber" to RgbColor.Amber,
            "Cobalt" to RgbColor.Cobalt,
            "Crimson" to RgbColor.Crimson,
            "Frost" to RgbColor.FrostWhite
        )
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = ObsidianSurfaceElevated
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, ObsidianOutline, MaterialTheme.shapes.large)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (activeZoneLabel != null) "Zone Override • $activeZoneLabel" else "Spectrum Wheel",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "RGB(${activeColor.red}, ${activeColor.green}, ${activeColor.blue}) • ${activeColor.toHex()}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                if (supportsDualColor && activeZoneLabel == null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ColorSlotChip(
                            label = "A",
                            color = primaryColor,
                            selected = !editingSecondary,
                            onClick = { onSelectEditingSecondary(false) },
                            modifier = Modifier.testTag("color_slot_primary")
                        )
                        ColorSlotChip(
                            label = "B",
                            color = secondaryColor,
                            selected = editingSecondary,
                            onClick = { onSelectEditingSecondary(true) },
                            modifier = Modifier.testTag("color_slot_secondary")
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                HsvColorWheel(
                    selectedColor = activeColor,
                    onColorChanged = onColorSelected,
                    modifier = Modifier
                        .size(164.dp)
                        .testTag("hsv_color_wheel")
                )

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Phosphor Swatches",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        quickSwatches.forEach { (name, swatch) ->
                            val isChosen = activeColor == swatch
                            Box(
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(swatch.toComposeColor())
                                    .border(
                                        width = if (isChosen) 2.5.dp else 1.dp,
                                        color = if (isChosen) Color.White else Color.White.copy(alpha = 0.25f),
                                        shape = CircleShape
                                    )
                                    .clickable { onColorSelected(swatch) }
                                    .semantics { contentDescription = "Select $name color" }
                                    .testTag("swatch_${name.lowercase()}")
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ColorSlotChip(
    label: String,
    color: RgbColor,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) Color(0xFF252E47) else Color(0xFF131826))
            .border(
                width = 1.dp,
                color = if (selected) color.toComposeColor() else ObsidianOutline,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(14.dp)
                .clip(CircleShape)
                .background(color.toComposeColor())
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun HsvColorWheel(
    selectedColor: RgbColor,
    onColorChanged: (RgbColor) -> Unit,
    modifier: Modifier = Modifier
) {
    var wheelSize by remember { mutableStateOf(IntSize.Zero) }

    val hsv = remember(selectedColor) {
        val out = FloatArray(3)
        android.graphics.Color.RGBToHSV(selectedColor.red, selectedColor.green, selectedColor.blue, out)
        out
    }

    val hueSweepColors = remember {
        listOf(
            Color.Red,
            Color.Yellow,
            Color.Green,
            Color.Cyan,
            Color.Blue,
            Color.Magenta,
            Color.Red
        )
    }

    fun pickAtOffset(pos: Offset) {
        val w = wheelSize.width.toFloat()
        val h = wheelSize.height.toFloat()
        if (w <= 0f || h <= 0f) return
        val cx = w / 2f
        val cy = h / 2f
        val radius = minOf(cx, cy) - 8f
        if (radius <= 0f) return

        val dx = pos.x - cx
        val dy = pos.y - cy
        val dist = hypot(dx, dy).coerceAtMost(radius)
        val sat = (dist / radius).coerceIn(0f, 1f)
        var angleDeg = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
        if (angleDeg < 0f) angleDeg += 360f
        val picked = Color.hsv(hue = angleDeg, saturation = sat, value = 1f)
        onColorChanged(RgbColor.fromComposeColor(picked))
    }

    Box(
        modifier = modifier
            .onSizeChanged { wheelSize = it }
            .pointerInput(Unit) {
                detectTapGestures { offset -> pickAtOffset(offset) }
            }
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    change.consume()
                    pickAtOffset(change.position)
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val radius = (minOf(size.width, size.height) / 2f) - 8f
            val center = Offset(size.width / 2f, size.height / 2f)

            drawCircle(
                brush = Brush.sweepGradient(colors = hueSweepColors, center = center),
                radius = radius,
                center = center
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White, Color.Transparent),
                    center = center,
                    radius = radius
                ),
                radius = radius,
                center = center
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.16f),
                radius = radius,
                center = center,
                style = Stroke(width = 2f)
            )

            // Reticle for current Hue + Saturation
            val angleRad = (hsv[0] * PI / 180.0).toFloat()
            val satDist = hsv[1].coerceIn(0f, 1f) * radius
            val marker = Offset(
                x = center.x + cos(angleRad) * satDist,
                y = center.y + sin(angleRad) * satDist
            )
            drawCircle(
                color = Color.Black.copy(alpha = 0.55f),
                radius = 12f,
                center = marker
            )
            drawCircle(
                color = selectedColor.toComposeColor(),
                radius = 8.5f,
                center = marker
            )
            drawCircle(
                color = Color.White,
                radius = 10.5f,
                center = marker,
                style = Stroke(width = 2.5f)
            )
        }
    }
}
