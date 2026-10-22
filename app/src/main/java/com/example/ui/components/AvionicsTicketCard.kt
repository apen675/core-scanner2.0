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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AirplanemodeActive
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.PrecisionManufacturing
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AdStatusLevel
import com.example.data.model.ScannedPart
import com.example.ui.theme.AviationGreenBadge
import com.example.ui.theme.AviationGreenBadgeBg
import com.example.ui.theme.AviationGreenText
import com.example.ui.theme.AviationRedBadge
import com.example.ui.theme.AviationRedBadgeBg
import com.example.ui.theme.AviationRedText
import com.example.ui.theme.BrushedPlatinum
import com.example.ui.theme.DarkSteelBorder
import com.example.ui.theme.DarkTitanium
import com.example.ui.theme.DarkTitaniumPill
import com.example.ui.theme.DeepCharcoal
import com.example.ui.theme.DeepCharcoalBorder
import com.example.ui.theme.DeepCharcoalLight
import com.example.ui.theme.MetallicSilver
import com.example.ui.theme.TextLightSteel
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextWhite

@Composable
fun AvionicsTicketCard(
    part: ScannedPart,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isActionRequired = part.adStatus == AdStatusLevel.ACTION_REQUIRED.displayName
    val isCompliant = part.adStatus == AdStatusLevel.COMPLIANT.displayName

    val (brandName, iconVector) = when {
        part.partNumber.contains("5934") || part.partNumber.contains("C661071") ->
            Pair("United Instruments", Icons.Default.Speed)
        part.partNumber.contains("066") || part.partNumber.contains("KT") ->
            Pair("BendixKing", Icons.Default.PrecisionManufacturing)
        part.partNumber.contains("011") || part.partNumber.contains("GNS") || part.partNumber.contains("GTX") ->
            Pair("Garmin Aviation", Icons.Default.TravelExplore)
        part.partNumber.contains("AT") ->
            Pair("Narco Avionics", Icons.Default.AirplanemodeActive)
        else ->
            Pair("Avionics Unit", Icons.Default.Flight)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(DeepCharcoal)
            .border(
                1.dp,
                if (isActionRequired) AviationRedBadge.copy(alpha = 0.6f) else DeepCharcoalBorder,
                RoundedCornerShape(20.dp)
            )
            .clickable(onClick = onClick)
            .testTag("ticket_card_${part.partNumber}")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Top Row: Brand Icon & Name + Status Capsule Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(DarkTitaniumPill)
                            .border(1.dp, DarkSteelBorder, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = iconVector,
                            contentDescription = brandName,
                            tint = BrushedPlatinum,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Column {
                        Text(
                            text = if (part.componentName.isNotBlank()) part.componentName else brandName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextWhite,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "S/N: ${part.serialNumber.ifBlank { "N/A" }}",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextLightSteel,
                                fontSize = 11.sp
                            )
                            Text(
                                text = "•",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted,
                                fontSize = 10.sp
                            )
                            Text(
                                text = "🕒 ${part.scanTime.ifBlank { "Scanned" }}",
                                style = MaterialTheme.typography.bodySmall,
                                color = BrushedPlatinum,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // Capsule Status Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            when {
                                isActionRequired -> AviationRedBadgeBg
                                isCompliant -> AviationGreenBadgeBg
                                else -> DarkTitaniumPill
                            }
                        )
                        .border(
                            1.dp,
                            when {
                                isActionRequired -> AviationRedBadge.copy(alpha = 0.5f)
                                isCompliant -> AviationGreenBadge.copy(alpha = 0.5f)
                                else -> DarkSteelBorder
                            },
                            RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = when {
                            isActionRequired -> "Action Required"
                            isCompliant -> "Compliant"
                            else -> part.adStatus
                        },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            isActionRequired -> AviationRedText
                            isCompliant -> AviationGreenText
                            else -> BrushedPlatinum
                        },
                        fontSize = 10.sp
                    )
                }
            }


            // Middle Flight Path Row: Part No. ──── ✈ ──── Condition
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Part Number in Inter Bold
                Column(modifier = Modifier.weight(1.3f)) {
                    Text(
                        text = part.partNumber,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite,
                        fontSize = 17.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (part.mfgDate.isNotBlank()) "MFG ${part.mfgDate}" else "AVIONICS PART",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }

                // Center: Flight line with dark steel & platinum airplane icon
                Row(
                    modifier = Modifier
                        .weight(1.2f)
                        .padding(horizontal = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(1.dp)
                            .background(DarkSteelBorder)
                    )
                    Icon(
                        imageVector = Icons.Default.Flight,
                        contentDescription = "Flight Route",
                        tint = BrushedPlatinum,
                        modifier = Modifier
                            .size(16.dp)
                            .rotate(90f)
                            .padding(horizontal = 2.dp)
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(1.dp)
                            .background(DarkSteelBorder)
                    )
                }

                // Right: Condition & 8130 tag
                Column(
                    modifier = Modifier.weight(1.3f),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = part.condition.substringBefore(" (").uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite,
                        fontSize = 14.sp,
                        maxLines = 1
                    )
                    Text(
                        text = if (part.has8130) "FAA 8130-3 TAG" else "NO 8130 TAG",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (part.has8130) AviationGreenText else TextMuted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Divider separating ticket details
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(DeepCharcoalBorder)
            )

            // Bottom Sub-Row: AD ID and "View Details →"
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(if (isActionRequired) AviationRedBadge else AviationGreenBadge)
                    )
                    Text(
                        text = part.adIdentifier.ifBlank { "FAA AD Screening Verified" },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isActionRequired) AviationRedText else TextLightSteel,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "View Details",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextWhite,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Details",
                        tint = BrushedPlatinum,
                        modifier = Modifier.size(12.dp)
                    )

                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = TextMuted,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}
