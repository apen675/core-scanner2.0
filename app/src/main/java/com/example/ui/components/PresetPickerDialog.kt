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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.domain.AdComplianceChecker
import com.example.ui.theme.AviationGreenBadge
import com.example.ui.theme.AviationGreenBadgeBg
import com.example.ui.theme.AviationGreenText
import com.example.ui.theme.AviationRedBadge
import com.example.ui.theme.AviationRedBadgeBg
import com.example.ui.theme.AviationRedText
import com.example.ui.theme.BrushedPlatinum
import com.example.ui.theme.DarkSteelBorder
import com.example.ui.theme.DeepCharcoal
import com.example.ui.theme.DeepCharcoalBorder
import com.example.ui.theme.DeepCharcoalLight
import com.example.ui.theme.JetBlackSurface
import com.example.ui.theme.TextLightSteel
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextWhite

@Composable
fun PresetPickerDialog(
    onSelectPreset: (AdComplianceChecker.PresetPlate) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .clip(RoundedCornerShape(24.dp))
                .background(DeepCharcoal)
                .border(1.dp, DeepCharcoalBorder, RoundedCornerShape(24.dp))
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "AVIONICS TEST PRESETS",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextWhite
                        )
                        Text(
                            text = "1-Tap real avionics data plates to screen AD rules",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextLightSteel
                        )
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(30.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = TextLightSteel)
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(390.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(AdComplianceChecker.SAMPLE_PRESETS) { sample ->
                        val isAlertSample = sample.label.contains("No Mod M") || sample.label.contains("Triggers")

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(JetBlackSurface)
                                .border(
                                    1.dp,
                                    if (isAlertSample) AviationRedBadge.copy(alpha = 0.5f) else DeepCharcoalBorder,
                                    RoundedCornerShape(16.dp)
                                )
                                .clickable { onSelectPreset(sample) }
                                .padding(14.dp)
                                .testTag("preset_${sample.partNumber.replace("-", "_")}")
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isAlertSample) Icons.Default.Warning else Icons.Default.Flight,
                                            contentDescription = null,
                                            tint = if (isAlertSample) AviationRedBadge else BrushedPlatinum,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = sample.partNumber,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = TextWhite,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                if (sample.has8130) AviationGreenBadgeBg else DeepCharcoalLight
                                            )
                                            .border(
                                                1.dp,
                                                if (sample.has8130) AviationGreenBadge.copy(alpha = 0.5f) else DarkSteelBorder,
                                                RoundedCornerShape(8.dp)
                                            )
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = if (sample.has8130) "8130-3" else "NO TAG",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (sample.has8130) AviationGreenText else TextMuted,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Text(
                                    text = sample.label,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isAlertSample) AviationRedText else TextWhite
                                )

                                Text(
                                    text = sample.subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextLightSteel,
                                    fontSize = 12.sp
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(
                                        text = "S/N: ${sample.serialNumber}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextMuted
                                    )
                                    if (sample.modMarks.isNotBlank()) {
                                        Text(
                                            text = "MOD: [${sample.modMarks}]",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = BrushedPlatinum
                                        )
                                    }
                                    Text(
                                        text = sample.condition,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextMuted
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
