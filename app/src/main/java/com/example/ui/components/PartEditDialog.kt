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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.AdCheckResult
import com.example.data.model.AdStatusLevel
import com.example.ui.theme.AviationGreenBadge
import com.example.ui.theme.AviationGreenBadgeBg
import com.example.ui.theme.AviationGreenText
import com.example.ui.theme.AviationRedBadge
import com.example.ui.theme.AviationRedBadgeBg
import com.example.ui.theme.AviationRedText
import com.example.ui.theme.BrushedPlatinum
import com.example.ui.theme.DarkActionBg
import com.example.ui.theme.DarkActionBorder
import com.example.ui.theme.DarkSteelBorder
import com.example.ui.theme.DarkTitanium
import com.example.ui.theme.DarkTitaniumPill
import com.example.ui.theme.DeepCharcoal
import com.example.ui.theme.DeepCharcoalBorder
import com.example.ui.theme.DeepCharcoalLight
import com.example.ui.theme.InterFontFamily
import com.example.ui.theme.JetBlack
import com.example.ui.theme.JetBlackSurface
import com.example.ui.theme.TextLightSteel
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextWhite
import com.example.ui.viewmodel.DraftPartForm

@Composable
fun PartEditDialog(
    draft: DraftPartForm,
    liveAdResult: AdCheckResult,
    isSaving: Boolean,
    onUpdateField: (
        partNumber: String?,
        serialNumber: String?,
        mfgDate: String?,
        modMarks: String?,
        condition: String?,
        has8130: Boolean?,
        notes: String?
    ) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    val conditionOptions = listOf(
        "As Removed (AR)",
        "Serviceable (SV)",
        "Overhauled (OH)",
        "New Surplus (NS)"
    )
    var conditionDropdownExpanded by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onCancel,
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
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "CONFIRM DATA PLATE",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextWhite
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(DarkTitaniumPill)
                                    .border(1.dp, DarkSteelBorder, RoundedCornerShape(6.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "VERIFY",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = BrushedPlatinum,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Text(
                            text = "Review optical extraction or edit fields before saving",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextLightSteel
                        )
                    }

                    IconButton(onClick = onCancel, modifier = Modifier.size(30.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = TextLightSteel)
                    }
                }

                // Live AD Compliance Feedback Banner
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            when (liveAdResult.status) {
                                AdStatusLevel.ACTION_REQUIRED -> AviationRedBadgeBg
                                AdStatusLevel.COMPLIANT -> AviationGreenBadgeBg
                                else -> DeepCharcoalLight
                            }
                        )
                        .border(
                            1.dp,
                            when (liveAdResult.status) {
                                AdStatusLevel.ACTION_REQUIRED -> AviationRedBadge.copy(alpha = 0.5f)
                                AdStatusLevel.COMPLIANT -> AviationGreenBadge.copy(alpha = 0.5f)
                                else -> DarkSteelBorder
                            },
                            RoundedCornerShape(12.dp)
                        )
                        .padding(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = when (liveAdResult.status) {
                                AdStatusLevel.ACTION_REQUIRED -> Icons.Default.Warning
                                AdStatusLevel.COMPLIANT -> Icons.Default.CheckCircle
                                AdStatusLevel.ADVISORY -> Icons.Default.Info
                                AdStatusLevel.UNKNOWN -> Icons.Default.HelpOutline
                            },
                            contentDescription = null,
                            tint = when (liveAdResult.status) {
                                AdStatusLevel.ACTION_REQUIRED -> AviationRedBadge
                                AdStatusLevel.COMPLIANT -> AviationGreenBadge
                                else -> BrushedPlatinum
                            },
                            modifier = Modifier.size(18.dp)
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "SCREENING: ${liveAdResult.status.displayName.uppercase()}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = when (liveAdResult.status) {
                                    AdStatusLevel.ACTION_REQUIRED -> AviationRedText
                                    AdStatusLevel.COMPLIANT -> AviationGreenText
                                    else -> BrushedPlatinum
                                }
                            )
                            Text(
                                text = liveAdResult.identifier,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = TextWhite
                            )
                            Text(
                                text = liveAdResult.details,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextLightSteel,
                                fontSize = 11.sp,
                                lineHeight = 15.sp
                            )
                        }
                    }
                }

                // Room DB Timestamp & OCR Indicator
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(JetBlackSurface)
                        .border(1.dp, DeepCharcoalBorder, RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ROOM DB PERSISTENCE",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextLightSteel,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                    Text(
                        text = "Timestamp & OCR text recorded",
                        style = MaterialTheme.typography.labelSmall,
                        color = BrushedPlatinum,
                        fontSize = 10.sp
                    )
                }

                // Extracted OCR Text Data Preview
                if (draft.rawExtractedText.isNotBlank()) {
                    var showOcrPayload by remember { mutableStateOf(false) }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(JetBlackSurface)
                            .border(1.dp, DeepCharcoalBorder, RoundedCornerShape(12.dp))
                            .padding(10.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showOcrPayload = !showOcrPayload },
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        tint = BrushedPlatinum,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "EXTRACTED OCR TEXT DATA",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = TextWhite,
                                        fontSize = 10.sp
                                    )
                                }
                                Text(
                                    text = if (showOcrPayload) "Hide ▲" else "View ▼",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = BrushedPlatinum,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            if (showOcrPayload) {
                                Text(
                                    text = draft.rawExtractedText,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        lineHeight = 15.sp
                                    ),
                                    color = BrushedPlatinum,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(DeepCharcoal, RoundedCornerShape(8.dp))
                                        .padding(8.dp)
                                )
                            }
                        }
                    }
                }

                // Part Number & Serial Number Fields
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = draft.partNumber,
                        onValueChange = { onUpdateField(it, null, null, null, null, null, null) },
                        label = { Text("Part Number (P/N)", fontSize = 12.sp) },
                        placeholder = { Text("e.g. 5934PA-1", color = TextMuted) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_part_number"),
                        textStyle = TextStyle(fontFamily = InterFontFamily, fontWeight = FontWeight.Bold, color = TextWhite),
                        colors = outlinedFieldColors(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = draft.serialNumber,
                        onValueChange = { onUpdateField(null, it, null, null, null, null, null) },
                        label = { Text("Serial Number (S/N)", fontSize = 12.sp) },
                        placeholder = { Text("e.g. 48102", color = TextMuted) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_serial_number"),
                        textStyle = TextStyle(fontFamily = InterFontFamily, color = TextWhite),
                        colors = outlinedFieldColors(),
                        singleLine = true
                    )
                }

                // Mfg Date & Mod Marks Fields
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = draft.mfgDate,
                        onValueChange = { onUpdateField(null, null, it, null, null, null, null) },
                        label = { Text("Mfg Date", fontSize = 12.sp) },
                        placeholder = { Text("YYYY-MM", color = TextMuted) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_mfg_date"),
                        textStyle = TextStyle(fontFamily = InterFontFamily, color = TextWhite),
                        colors = outlinedFieldColors(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = draft.modMarks,
                        onValueChange = { onUpdateField(null, null, null, it, null, null, null) },
                        label = { Text("Mod Marks", fontSize = 12.sp) },
                        placeholder = { Text("e.g. M, 1-4", color = TextMuted) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_mod_marks"),
                        textStyle = TextStyle(fontFamily = InterFontFamily, color = TextWhite),
                        colors = outlinedFieldColors(),
                        singleLine = true
                    )
                }

                // Condition Selector
                Column {
                    Text(
                        text = "RECORDED CONDITION",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextLightSteel,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(JetBlackSurface)
                                .border(1.dp, DeepCharcoalBorder, RoundedCornerShape(10.dp))
                                .clickable { conditionDropdownExpanded = true }
                                .padding(horizontal = 14.dp, vertical = 12.dp)
                        ) {
                            Text(
                                text = draft.condition,
                                color = TextWhite,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        DropdownMenu(
                            expanded = conditionDropdownExpanded,
                            onDismissRequest = { conditionDropdownExpanded = false },
                            modifier = Modifier.background(DeepCharcoal)
                        ) {
                            conditionOptions.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option, color = TextWhite) },
                                    onClick = {
                                        onUpdateField(null, null, null, null, option, null, null)
                                        conditionDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // FAA Form 8130-3 Tag Switch
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(JetBlackSurface)
                        .border(1.dp, DeepCharcoalBorder, RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "FAA Form 8130-3 / EASA Form 1",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = TextWhite
                        )
                        Text(
                            text = if (draft.has8130) "Airworthiness release tag attached" else "No physical tag attached",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (draft.has8130) AviationGreenText else TextMuted,
                            fontSize = 11.sp
                        )
                    }

                    Switch(
                        checked = draft.has8130,
                        onCheckedChange = { onUpdateField(null, null, null, null, null, it, null) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.Black,
                            checkedTrackColor = AviationGreenBadge,
                            uncheckedThumbColor = TextMuted,
                            uncheckedTrackColor = JetBlack
                        ),
                        modifier = Modifier.testTag("switch_8130")
                    )
                }

                // Notes Field
                OutlinedTextField(
                    value = draft.notes,
                    onValueChange = { onUpdateField(null, null, null, null, null, null, it) },
                    label = { Text("Logbook & Removal Notes", fontSize = 12.sp) },
                    placeholder = { Text("e.g. Removed from N172SP right panel", color = TextMuted) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_notes"),
                    textStyle = TextStyle(color = TextWhite),
                    colors = outlinedFieldColors(),
                    maxLines = 2
                )

                // Dialog Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onCancel,
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .testTag("dialog_cancel_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextLightSteel),
                        border = androidx.compose.foundation.BorderStroke(1.dp, DeepCharcoalBorder)
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("CANCEL", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = onSave,
                        enabled = !isSaving,
                        modifier = Modifier
                            .weight(1.3f)
                            .height(46.dp)
                            .testTag("dialog_save_part_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DarkActionBg,
                            contentColor = TextWhite
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkActionBorder),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = TextWhite,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("SAVE PART", fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun outlinedFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = DarkSteelBorder,
    unfocusedBorderColor = DeepCharcoalBorder,
    focusedLabelColor = BrushedPlatinum,
    unfocusedLabelColor = TextLightSteel,
    cursorColor = BrushedPlatinum,
    focusedContainerColor = JetBlackSurface,
    unfocusedContainerColor = JetBlackSurface
)
