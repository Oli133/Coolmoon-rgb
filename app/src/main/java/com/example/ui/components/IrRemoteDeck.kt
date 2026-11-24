package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.IrRemoteKey
import com.example.ui.theme.EmeraldOnline
import com.example.ui.theme.ObsidianCanvas
import com.example.ui.theme.ObsidianOutline
import com.example.ui.theme.ObsidianSurfaceElevated

@Composable
fun IrRemoteDeck(
    hasIrHardware: Boolean,
    onKeyClick: (IrRemoteKey) -> Unit,
    modifier: Modifier = Modifier
) {
    val rows = listOf(
        listOf(IrRemoteKey.POWER_ON, IrRemoteKey.MB_SYNC, IrRemoteKey.POWER_OFF),
        listOf(IrRemoteKey.BRIGHT_UP, IrRemoteKey.AUTO_CYCLE, IrRemoteKey.BRIGHT_DOWN),
        listOf(IrRemoteKey.SPEED_UP, IrRemoteKey.RAINBOW, IrRemoteKey.SPEED_DOWN),
        listOf(IrRemoteKey.MODE_NEXT, IrRemoteKey.FAN_SPEED_UP, IrRemoteKey.MODE_PREV),
        listOf(IrRemoteKey.COLOR_RED, IrRemoteKey.COLOR_GREEN, IrRemoteKey.COLOR_BLUE),
        listOf(IrRemoteKey.COLOR_CYAN, IrRemoteKey.COLOR_PURPLE, IrRemoteKey.COLOR_YELLOW),
        listOf(IrRemoteKey.COLOR_PINK, IrRemoteKey.COLOR_WHITE, IrRemoteKey.FAN_SPEED_DOWN)
    )

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
                        text = "CoolMoon 21-Key IR Remote",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (hasIrHardware) {
                            "38kHz NEC IR Emitter Ready"
                        } else {
                            "No IR blaster detected — mirrors to Virtual Rig & Packet Log"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (hasIrHardware) EmeraldOnline else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(ObsidianCanvas)
                    .border(1.dp, ObsidianOutline.copy(alpha = 0.7f), RoundedCornerShape(18.dp))
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rows.forEach { rowKeys ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowKeys.forEach { key ->
                            IrKeyButton(
                                key = key,
                                onClick = { onKeyClick(key) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("ir_key_${key.name.lowercase()}")
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun IrKeyButton(
    key: IrRemoteKey,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor = when (key) {
        IrRemoteKey.POWER_ON -> Color(0xFF12362A)
        IrRemoteKey.POWER_OFF -> Color(0xFF3E1824)
        else -> Color(0xFF182032)
    }
    val accent = key.accentHex?.let { Color(it) }

    Box(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .height(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .border(
                width = 1.dp,
                color = accent?.copy(alpha = 0.55f) ?: ObsidianOutline,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .semantics { contentDescription = "IR remote ${key.label}" },
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (accent != null) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(accent)
                )
            }
            Text(
                text = key.shortLabel,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
