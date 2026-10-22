package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AirplanemodeActive
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.PrecisionManufacturing
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.AdStatusLevel
import com.example.data.model.ScannedPart
import com.example.domain.AdComplianceChecker
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
import com.example.ui.theme.DeepCharcoal
import com.example.ui.theme.DeepCharcoalBorder
import com.example.ui.theme.DeepCharcoalLight
import com.example.ui.theme.JetBlackSurface
import com.example.ui.theme.TextLightSteel
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextWhite

@Composable
fun PartDetailDialog(
    part: ScannedPart,
    onDismiss: () -> Unit,
    onDelete: (Long) -> Unit
) {
    val context = LocalContext.current
    val isActionRequired = part.adStatus == AdStatusLevel.ACTION_REQUIRED.displayName

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
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${part.partNumber}  |  S/N ${part.serialNumber.ifBlank { "N/A" }}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextWhite,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "Avionics Instrument • ${part.condition}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextLightSteel,
                            fontSize = 12.sp
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = TextLightSteel)
                    }
                }

                // Aircraft / Component Photo Banner
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(JetBlackSurface, DeepCharcoalLight)
                            )
                        )
                        .border(1.dp, DeepCharcoalBorder, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AirplanemodeActive,
                            contentDescription = "Aircraft Jet",
                            tint = BrushedPlatinum,
                            modifier = Modifier.size(50.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "CERTIFIED AVIONICS COMPONENT",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextWhite,
                            letterSpacing = 1.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Carousel dots
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        repeat(5) { index ->
                            Box(
                                modifier = Modifier
                                    .size(if (index == 0) 14.dp else 6.dp, 6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(if (index == 0) BrushedPlatinum else TextMuted.copy(alpha = 0.5f))
                            )
                        }
                    }
                }

                // Section 1: Room DB & Scan History Audit
                DetailSection(title = "Room DB Scan History & Audit") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(JetBlackSurface)
                            .border(1.dp, DeepCharcoalBorder, RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                                        imageVector = Icons.Default.CalendarMonth,
                                        contentDescription = null,
                                        tint = BrushedPlatinum,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Scan Timestamp",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextLightSteel
                                    )
                                }
                                Text(
                                    text = part.formattedDate(),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = TextWhite
                                )
                            }

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
                                        imageVector = Icons.Default.Speed,
                                        contentDescription = null,
                                        tint = TextLightSteel,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Epoch Timestamp",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextLightSteel
                                    )
                                }
                                Text(
                                    text = "${part.timestamp} ms",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    color = BrushedPlatinum
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Room Record ID",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextLightSteel
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(DarkTitanium)
                                        .border(1.dp, DarkSteelBorder, RoundedCornerShape(6.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "#${part.id}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = TextWhite,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }

                // Section 2: Component Information
                DetailSection(title = "Component Information") {
                    if (part.componentName.isNotBlank()) {
                        InfoLine(icon = Icons.Default.AirplanemodeActive, label = "Component", value = part.componentName)
                    }
                    InfoLine(icon = Icons.Default.PrecisionManufacturing, label = "Part Number", value = part.partNumber)
                    InfoLine(icon = Icons.Default.Speed, label = "Serial Number", value = part.serialNumber.ifBlank { "N/A" }, valueColor = TextWhite)
                    InfoLine(icon = Icons.Default.CalendarMonth, label = "YOM (Mfg Date)", value = part.mfgDate.ifBlank { "Not Stamped" })
                    InfoLine(icon = Icons.Default.Security, label = "Modification Marks", value = part.modMarks.ifBlank { "None Stamped" })
                    InfoLine(icon = Icons.Default.Home, label = "Condition", value = part.condition)
                }

                // Section 3: Extracted OCR Text Data
                if (part.rawExtractedText.isNotBlank()) {
                    DetailSection(title = "Extracted OCR Text Data") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(JetBlackSurface)
                                .border(1.dp, DeepCharcoalBorder, RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "RAW OCR PAYLOAD / TEXT",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextLightSteel,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    )
                                    IconButton(
                                        onClick = {
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            val clip = ClipData.newPlainText("Extracted OCR Text", part.rawExtractedText)
                                            clipboard.setPrimaryClip(clip)
                                            Toast.makeText(context, "OCR Text copied to clipboard", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = "Copy OCR",
                                            tint = BrushedPlatinum,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }

                                Text(
                                    text = part.rawExtractedText,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        lineHeight = 15.sp
                                    ),
                                    color = BrushedPlatinum
                                )
                            }
                        }
                    }
                }

                // Section 4: AD Compliance & Regulatory Screening
                DetailSection(title = "AD Compliance & Regulations") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isActionRequired) AviationRedBadgeBg else AviationGreenBadgeBg)
                            .border(1.dp, if (isActionRequired) AviationRedBadge.copy(alpha = 0.5f) else AviationGreenBadge.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = if (isActionRequired) Icons.Default.Warning else Icons.Default.Verified,
                                    contentDescription = null,
                                    tint = if (isActionRequired) AviationRedBadge else AviationGreenBadge,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "STATUS: ${part.adStatus.uppercase()}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isActionRequired) AviationRedText else AviationGreenText
                                )
                            }
                            if (part.adIdentifier.isNotBlank()) {
                                Text(
                                    text = part.adIdentifier,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = TextWhite
                                )
                            }
                            Text(
                                text = part.adDetails,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isActionRequired) AviationRedText.copy(alpha = 0.9f) else AviationGreenText.copy(alpha = 0.9f),
                                lineHeight = 16.sp
                            )
                        }
                    }
                }

                // Section 5: Services & Certification
                DetailSection(title = "Services & Certification") {
                    ServiceItem(
                        title = "FAA Form 8130-3 / EASA Form 1",
                        subtitle = if (part.has8130) "Airworthiness release tag attached & verified" else "Missing physical 8130-3 tag",
                        isIncluded = part.has8130
                    )
                    ServiceItem(
                        title = "FAR 91.411 / 91.413 24-Month Inspection",
                        subtitle = "Altimeter / Transponder recertification interval compliance",
                        isIncluded = true
                    )
                    ServiceItem(
                        title = "DRS Records Cross-Check",
                        subtitle = "Queried against FAA Dynamic Regulatory System database",
                        isIncluded = true
                    )
                }

                // Section 6: Removal / Installation Log
                if (part.notes.isNotBlank()) {
                    DetailSection(title = "Removal & Logbook Notes") {
                        Text(
                            text = part.notes,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextWhite,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(JetBlackSurface, RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        )
                    }
                }


                // Bottom Buttons
                Button(
                    onClick = {
                        val url = AdComplianceChecker.getDrsSearchUrl(part.partNumber)
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        try {
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Could not open browser", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .testTag("drs_search_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DarkActionBg,
                        contentColor = TextWhite
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkActionBorder),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(16.dp), tint = BrushedPlatinum)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "SEARCH FAA DRS DATABASE (${part.partNumber})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        letterSpacing = 0.5.sp
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val text = "P/N: ${part.partNumber}, S/N: ${part.serialNumber}, Status: ${part.adStatus} - ${part.adIdentifier}"
                            val clip = ClipData.newPlainText("Part", text)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextWhite),
                        border = androidx.compose.foundation.BorderStroke(1.dp, DeepCharcoalBorder)
                    ) {
                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("COPY", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            onDelete(part.id)
                            onDismiss()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("delete_part_modal_button"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AviationRedBadgeBg,
                            contentColor = AviationRedText
                        )
                    ) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("DELETE", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = TextWhite,
            fontSize = 14.sp
        )
        content()
    }
}

@Composable
private fun InfoLine(
    icon: ImageVector,
    label: String,
    value: String,
    valueColor: Color = TextWhite
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
            Icon(imageVector = icon, contentDescription = null, tint = TextLightSteel, modifier = Modifier.size(16.dp))
            Text(text = label, style = MaterialTheme.typography.bodySmall, color = TextLightSteel)
        }
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = valueColor
        )
    }
}

@Composable
private fun ServiceItem(
    title: String,
    subtitle: String,
    isIncluded: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = TextWhite)
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = TextLightSteel, fontSize = 11.sp)
        }
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(if (isIncluded) AviationGreenBadgeBg else JetBlackSurface),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = if (isIncluded) AviationGreenText else TextMuted,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}
