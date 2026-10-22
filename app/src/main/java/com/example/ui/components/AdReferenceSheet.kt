package com.example.ui.components

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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
fun AdReferenceSheet(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

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
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.MenuBook, contentDescription = null, tint = BrushedPlatinum)
                        Column {
                            Text(
                                text = "AVIONICS COMPLIANCE GUIDE",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextWhite
                            )
                            Text(
                                text = "FAA AD screening rules & documentation standards",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextLightSteel
                            )
                        }
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(30.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = TextLightSteel)
                    }
                }

                // Reference Section 1: Altimeter AD 86-05-02
                ReferenceCard(
                    title = "United Instruments Altimeter AD 86-05-02",
                    code = "AD 86-05-02 R1",
                    summary = "Applies to 5934PA-1, 5934PD-1, and Cessna C661071 series altimeters. Check for stamped 'M' or punched mod plate indicating diaphragm and barometric gear train modification. Without 'M' stamp, instrument cannot be certified for IFR flight."
                )

                // Reference Section 2: Transponder AD 2004-08-16
                ReferenceCard(
                    title = "BendixKing Transponders AD 2004-08-16",
                    code = "KT 76A / KT 78A",
                    summary = "Addresses cavity oscillator drift causing intermittent reply failures to ATC secondary surveillance radars. Requires bench test verification per FAR 91.413 within preceding 24 calendar months."
                )

                // Reference Section 3: FAA Form 8130-3 vs EASA Form 1
                ReferenceCard(
                    title = "Authorized Release Certificate: FAA Form 8130-3",
                    code = "14 CFR Part 43 / Order 8130.21",
                    summary = "Airworthiness Approval Tag issued by certified Part 145 repair stations or manufacturer. Block 11 specifies status (Serviceable, Overhauled, Rebuilt). Dual-release certifications cover both FAA and EASA requirements."
                )

                // Reference Section 4: 14 CFR 91.411 & 91.413 Inspections
                ReferenceCard(
                    title = "24-Month Pitot-Static & Transponder Tests",
                    code = "14 CFR 91.411 & 91.413",
                    summary = "Altimeter, static pressure system, and automatic altitude reporting encoder must be tested within 24 calendar months for IFR flight. Transponder must be tested per FAR Part 43 Appendix F."
                )

                // FAA Dynamic Regulatory System (DRS) Portal Link
                Button(
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://drs.faa.gov/"))
                        context.startActivity(intent)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp),
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
                        text = "OPEN FAA DYNAMIC REGULATORY SYSTEM (DRS)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun ReferenceCard(
    title: String,
    code: String,
    summary: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(JetBlackSurface)
            .border(1.dp, DeepCharcoalBorder, RoundedCornerShape(14.dp))
            .padding(12.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(DeepCharcoalLight)
                        .border(1.dp, DarkSteelBorder, RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = code,
                        style = MaterialTheme.typography.labelSmall,
                        color = BrushedPlatinum,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Text(
                text = summary,
                style = MaterialTheme.typography.bodySmall,
                color = TextLightSteel,
                lineHeight = 16.sp
            )
        }
    }
}
