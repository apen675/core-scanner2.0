package com.example.ui

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AirplanemodeActive
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.AdReferenceSheet
import com.example.ui.components.AvionicsTicketCard
import com.example.ui.components.CameraViewfinder
import com.example.ui.components.PartDetailDialog
import com.example.ui.components.PartEditDialog
import com.example.ui.components.PresetPickerDialog
import com.example.ui.components.SafetyDisclaimerCard
import com.example.ui.theme.AviationBlueCard
import com.example.ui.theme.AviationBlueCardDark
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
import com.example.ui.theme.JetBlackElevated
import com.example.ui.theme.JetBlackSurface
import com.example.ui.theme.TextLightSteel
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextWhite
import com.example.ui.theme.ToggleContainerBg
import com.example.ui.theme.ToggleSelectedBg
import com.example.ui.theme.ToggleSelectedBorder
import com.example.ui.theme.ToggleUnselected
import com.example.ui.viewmodel.AvionicsViewModel
import com.example.ui.viewmodel.InventoryFilter
import com.example.ui.viewmodel.UiEvent

enum class AviationTab(val label: String) {
    INVENTORY("Inventory"),
    SCANNER("Scanner")
}

@Composable
fun AvionicsScreen(
    viewModel: AvionicsViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    var currentTab by remember { mutableStateOf(AviationTab.INVENTORY) }

    val isCameraActive by viewModel.isCameraActive.collectAsStateWithLifecycle()
    val isConfirming by viewModel.isConfirming.collectAsStateWithLifecycle()
    val isSaving by viewModel.isSaving.collectAsStateWithLifecycle()
    val isOcrProcessing by viewModel.isOcrProcessing.collectAsStateWithLifecycle()
    val ocrStatusMessage by viewModel.ocrStatusMessage.collectAsStateWithLifecycle()
    val draftForm by viewModel.draftForm.collectAsStateWithLifecycle()
    val liveAdCheck by viewModel.liveAdCheck.collectAsStateWithLifecycle()
    val filteredInventory by viewModel.filteredInventory.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedFilter by viewModel.selectedFilter.collectAsStateWithLifecycle()
    val selectedPartForDetail by viewModel.selectedPartForDetail.collectAsStateWithLifecycle()
    val showAdReference by viewModel.showAdReference.collectAsStateWithLifecycle()
    val showPresetPicker by viewModel.showPresetPicker.collectAsStateWithLifecycle()
    val stats by viewModel.inventoryStats.collectAsStateWithLifecycle()

    var showSearchBar by remember { mutableStateOf(false) }
    var showClearHistoryDialog by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                is UiEvent.ShowToast -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                }
                is UiEvent.ShareCsv -> {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/csv"
                        putExtra(Intent.EXTRA_SUBJECT, "Avionics Inventory Export")
                        putExtra(Intent.EXTRA_TEXT, event.csvContent)
                    }
                    try {
                        context.startActivity(Intent.createChooser(shareIntent, "Share Avionics Inventory CSV"))
                    } catch (e: Exception) {
                        Toast.makeText(context, "Could not open share chooser", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing),
        containerColor = JetBlack,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            // Sticky Bottom Bar
            if (stats.first > 0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(JetBlackSurface.copy(alpha = 0.95f))
                        .border(width = 1.dp, color = DeepCharcoalBorder)
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Button(
                        onClick = { viewModel.exportCsv(context) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("submit_export_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DarkActionBg,
                            contentColor = TextWhite
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkActionBorder),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp), tint = BrushedPlatinum)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Export Room DB CSV (${stats.first} parts, ${stats.second} alerts)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // 1. Top Bar
            AviationTopBar(
                title = if (currentTab == AviationTab.INVENTORY) "Avionics History" else "Data Plate Scanner",
                itemCount = stats.first,
                onToggleSearch = { showSearchBar = !showSearchBar },
                onOpenReference = { viewModel.setShowAdReference(true) },
                onClearHistory = { showClearHistoryDialog = true }
            )


            // 2. Segmented Capsule Toggle
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                AviationSegmentedToggle(
                    selectedTab = currentTab,
                    onSelectTab = { currentTab = it }
                )
            }

            // Expandable Search Bar
            AnimatedVisibility(visible = showSearchBar) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    placeholder = { Text("Search P/N, Serial, or AD...", color = TextMuted, fontSize = 13.sp) },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = TextLightSteel, modifier = Modifier.size(18.dp))
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear", tint = TextLightSteel)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .height(48.dp)
                        .testTag("search_text_field"),
                    textStyle = TextStyle(fontFamily = InterFontFamily, fontSize = 13.sp, color = TextWhite),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DarkSteelBorder,
                        unfocusedBorderColor = DeepCharcoalBorder,
                        focusedContainerColor = DeepCharcoal,
                        unfocusedContainerColor = DeepCharcoal
                    )
                )
            }

            // Main Tab Content
            when (currentTab) {
                AviationTab.INVENTORY -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp)
                    ) {
                        // Route Style Header
                        item {
                            FlightRouteHeader(
                                origin = "Cessna 172N",
                                destination = "Part 145 Hangar",
                                totalParts = stats.first,
                                alerts = stats.second
                            )
                        }

                        // Blue Flight Analytics Announcement Card (Dark night-sky aesthetic)
                        item {
                            FlightAnalyticsBlueCard(
                                alertsCount = stats.second,
                                onOpenReference = { viewModel.setShowAdReference(true) }
                            )
                        }

                        // Filter Pills Row
                        item {
                            FilterPillRow(
                                selectedFilter = selectedFilter,
                                onSelectFilter = { viewModel.setFilter(it) }
                            )
                        }

                        // Scanned Inventory Cards
                        if (filteredInventory.isEmpty()) {
                            item {
                                EmptyStateTicket(
                                    onSwitchToScanner = { currentTab = AviationTab.SCANNER },
                                    onOpenPresets = { viewModel.setShowPresetPicker(true) }
                                )
                            }
                        } else {
                            items(
                                items = filteredInventory,
                                key = { it.id }
                            ) { part ->
                                AvionicsTicketCard(
                                    part = part,
                                    onClick = { viewModel.selectPartForDetail(part) },
                                    onDelete = { viewModel.deletePart(part.id) }
                                )
                            }
                        }

                        // Safety Disclaimer
                        item {
                            SafetyDisclaimerCard()
                        }
                    }
                }

                AviationTab.SCANNER -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp)
                    ) {
                        // Hero Header
                        item {
                            HeroScannerBanner()
                        }

                        // Camera Viewfinder & Reticle
                        item {
                            CameraViewfinder(
                                isCameraActive = isCameraActive,
                                onToggleCamera = { viewModel.setCameraActive(it) },
                                onCapturePhoto = { viewModel.processCapturedPhoto(it) },
                                onCaptureSampleAsset = { viewModel.processSampleAssetPhoto(it) },
                                onCapturePlateFallback = { viewModel.captureDataPlate() },
                                onOpenPresets = { viewModel.setShowPresetPicker(true) },
                                onOpenManualEntry = { viewModel.openManualEntry() },
                                isOcrProcessing = isOcrProcessing,
                                ocrStatusMessage = ocrStatusMessage
                            )
                        }

                        // Quick Presets
                        item {
                            Text(
                                text = "RECOMMENDED TEST PLATES",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = TextWhite,
                                letterSpacing = 0.5.sp
                            )
                        }

                        items(com.example.domain.AdComplianceChecker.SAMPLE_PRESETS) { sample ->
                            val isAlert = sample.label.contains("No Mod M") || sample.label.contains("Triggers")
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(DeepCharcoal)
                                    .border(1.dp, if (isAlert) AviationRedBadge.copy(alpha = 0.5f) else DeepCharcoalBorder, RoundedCornerShape(16.dp))
                                    .clickable { viewModel.applyPreset(sample) }
                                    .padding(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = sample.label,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isAlert) AviationRedText else TextWhite
                                        )
                                        Text(
                                            text = "P/N: ${sample.partNumber}  •  S/N: ${sample.serialNumber}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextLightSteel,
                                            fontSize = 11.sp
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = "Select",
                                        tint = BrushedPlatinum,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        item {
                            SafetyDisclaimerCard()
                        }
                    }
                }
            }
        }
    }

    // Confirmation & Edit Modal Dialog
    if (isConfirming) {
        PartEditDialog(
            draft = draftForm,
            liveAdResult = liveAdCheck,
            isSaving = isSaving,
            onUpdateField = { pn, sn, mfg, mod, cond, has8130, notes ->
                viewModel.updateDraft(pn, sn, mfg, mod, cond, has8130, notes)
            },
            onSave = {
                viewModel.saveDraftPart()
                currentTab = AviationTab.INVENTORY
            },
            onCancel = { viewModel.cancelCapture() }
        )
    }

    // Part Detail Inspection Modal
    selectedPartForDetail?.let { part ->
        PartDetailDialog(
            part = part,
            onDismiss = { viewModel.selectPartForDetail(null) },
            onDelete = { id -> viewModel.deletePart(id) }
        )
    }

    // Quick Samples Picker
    if (showPresetPicker) {
        PresetPickerDialog(
            onSelectPreset = { preset -> viewModel.applyPreset(preset) },
            onDismiss = { viewModel.setShowPresetPicker(false) }
        )
    }

    // Clear All History Confirmation Dialog
    if (showClearHistoryDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showClearHistoryDialog = false },
            title = {
                Text(
                    text = "Clear Room Database History?",
                    color = TextWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Text(
                    text = "This will permanently remove all stored scan history, timestamps, and extracted OCR text from the local Room database.",
                    color = TextLightSteel,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllHistory()
                        showClearHistoryDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AviationRedBadgeBg,
                        contentColor = AviationRedText
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("CLEAR ALL", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showClearHistoryDialog = false },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextLightSteel),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DeepCharcoalBorder)
                ) {
                    Text("CANCEL")
                }
            },
            containerColor = DeepCharcoal,
            shape = RoundedCornerShape(20.dp)
        )
    }

    // FAA AD Reference Sheet
    if (showAdReference) {
        AdReferenceSheet(
            onDismiss = { viewModel.setShowAdReference(false) }
        )
    }
}

// ── UI SUBCOMPONENTS ──

@Composable
private fun AviationTopBar(
    title: String,
    itemCount: Int = 0,
    onToggleSearch: () -> Unit,
    onOpenReference: () -> Unit,
    onClearHistory: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(DeepCharcoal)
                    .border(1.dp, DeepCharcoalBorder, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AirplanemodeActive,
                    contentDescription = "Flight",
                    tint = BrushedPlatinum,
                    modifier = Modifier.size(18.dp)
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = TextWhite,
                fontSize = 18.sp
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            IconButton(
                onClick = onToggleSearch,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(DeepCharcoal)
                    .border(1.dp, DeepCharcoalBorder, CircleShape)
            ) {
                Icon(imageVector = Icons.Default.Search, contentDescription = "Search", tint = TextWhite, modifier = Modifier.size(18.dp))
            }

            IconButton(
                onClick = onOpenReference,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(DeepCharcoal)
                    .border(1.dp, DeepCharcoalBorder, CircleShape)
            ) {
                Icon(imageVector = Icons.Default.MenuBook, contentDescription = "AD Guide", tint = BrushedPlatinum, modifier = Modifier.size(18.dp))
            }

            if (itemCount > 0) {
                IconButton(
                    onClick = onClearHistory,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(DeepCharcoal)
                        .border(1.dp, DeepCharcoalBorder, CircleShape)
                        .testTag("clear_history_button")
                ) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Clear History", tint = AviationRedText, modifier = Modifier.size(17.dp))
                }
            }
        }
    }
}


@Composable
private fun AviationSegmentedToggle(
    selectedTab: AviationTab,
    onSelectTab: (AviationTab) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(ToggleContainerBg)
            .border(1.dp, DeepCharcoalBorder, RoundedCornerShape(24.dp))
            .padding(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            AviationTab.values().forEach { tab ->
                val isSelected = selectedTab == tab
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isSelected) ToggleSelectedBg else Color.Transparent)
                        .border(
                            1.dp,
                            if (isSelected) ToggleSelectedBorder else Color.Transparent,
                            RoundedCornerShape(20.dp)
                        )
                        .clickable { onSelectTab(tab) }
                        .testTag("tab_${tab.name.lowercase()}"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = tab.label,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) TextWhite else ToggleUnselected,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun FlightRouteHeader(
    origin: String,
    destination: String,
    totalParts: Int,
    alerts: Int
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(DeepCharcoal)
            .border(1.dp, DeepCharcoalBorder, RoundedCornerShape(18.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = "AIRCRAFT REGISTRY", style = MaterialTheme.typography.labelSmall, color = TextLightSteel, fontSize = 9.sp)
                Text(text = origin, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextWhite)
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "ROOM DB: $totalParts SCANS", style = MaterialTheme.typography.labelSmall, color = BrushedPlatinum, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.width(20.dp).height(1.dp).background(DarkSteelBorder))
                    Icon(imageVector = Icons.Default.Flight, contentDescription = null, tint = BrushedPlatinum, modifier = Modifier.size(14.dp).padding(horizontal = 2.dp))
                    Box(modifier = Modifier.width(20.dp).height(1.dp).background(DarkSteelBorder))
                }
                Text(text = "PERSISTED LOCALLY", style = MaterialTheme.typography.labelSmall, color = TextMuted, fontSize = 8.sp)
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(text = "MAINTENANCE BASE", style = MaterialTheme.typography.labelSmall, color = TextLightSteel, fontSize = 9.sp)
                Text(text = destination, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextWhite)
            }
        }
    }
}

@Composable
private fun FlightAnalyticsBlueCard(
    alertsCount: Int,
    onOpenReference: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.horizontalGradient(
                    colors = listOf(AviationBlueCard, AviationBlueCardDark)
                )
            )
            .border(1.dp, DeepCharcoalBorder, RoundedCornerShape(18.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(DarkActionBg)
                            .border(1.dp, DarkActionBorder, RoundedCornerShape(10.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "• Active",
                            style = MaterialTheme.typography.labelSmall,
                            color = BrushedPlatinum,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                    }
                    Text(
                        text = if (alertsCount > 0) "$alertsCount AD Action Required" else "All Screened Compliant",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextLightSteel
                    )
                }

                Text(
                    text = "Flight Analytics Completed! ✈",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 15.sp
                )
            }

            Button(
                onClick = onOpenReference,
                colors = ButtonDefaults.buttonColors(
                    containerColor = DarkActionBg,
                    contentColor = TextWhite
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkActionBorder),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text(text = "View Report", fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun FilterPillRow(
    selectedFilter: InventoryFilter,
    onSelectFilter: (InventoryFilter) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(InventoryFilter.values()) { filter ->
            val isSelected = selectedFilter == filter
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isSelected) DarkTitaniumPill else DeepCharcoal)
                    .border(
                        1.dp,
                        if (isSelected) DarkSteelBorder else DeepCharcoalBorder,
                        RoundedCornerShape(16.dp)
                    )
                    .clickable { onSelectFilter(filter) }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (filter == InventoryFilter.ALL) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            tint = if (isSelected) BrushedPlatinum else TextLightSteel,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                    Text(
                        text = filter.label,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) TextWhite else TextLightSteel,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun HeroScannerBanner() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(DeepCharcoal, JetBlackSurface)
                )
            )
            .border(1.dp, DeepCharcoalBorder, RoundedCornerShape(20.dp))
            .padding(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Scan Avionics Plate",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite,
                    fontSize = 19.sp
                )
                Text(
                    text = "OCR extract Part #, Serial # & Mod marks for instant FAA AD screening",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextLightSteel,
                    fontSize = 12.sp
                )
            }

            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(DarkActionBg)
                    .border(1.dp, DarkActionBorder, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AirplanemodeActive,
                    contentDescription = null,
                    tint = BrushedPlatinum,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

@Composable
private fun EmptyStateTicket(
    onSwitchToScanner: () -> Unit,
    onOpenPresets: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(DeepCharcoal)
            .border(1.dp, DeepCharcoalBorder, RoundedCornerShape(22.dp))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(DarkActionBg)
                    .border(1.dp, DarkActionBorder, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = Icons.Default.Flight, contentDescription = null, tint = BrushedPlatinum, modifier = Modifier.size(24.dp))
            }

            Text(
                text = "NO SCANNED FLIGHT COMPONENTS",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextWhite
            )

            Text(
                text = "Scan physical data plates with camera or test with aviation presets.",
                style = MaterialTheme.typography.bodySmall,
                color = TextLightSteel,
                textAlign = TextAlign.Center
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onSwitchToScanner,
                    colors = ButtonDefaults.buttonColors(containerColor = DarkActionBg, contentColor = TextWhite),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DarkActionBorder),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("OPEN SCANNER", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }

                OutlinedButton(
                    onClick = onOpenPresets,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextWhite),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DeepCharcoalBorder)
                ) {
                    Text("LOAD PRESETS", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }
        }
    }
}
