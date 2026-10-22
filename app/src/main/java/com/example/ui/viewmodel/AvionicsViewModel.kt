package com.example.ui.viewmodel

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.AdCheckResult
import com.example.data.model.AdStatusLevel
import com.example.data.model.ScannedPart
import com.example.data.repository.PartRepository
import com.example.domain.AdComplianceChecker
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.example.data.api.GeminiDataPlateService

data class DraftPartForm(
    val partNumber: String = "",
    val serialNumber: String = "",
    val mfgDate: String = "",
    val modMarks: String = "",
    val condition: String = "As Removed (AR)",
    val has8130: Boolean = false,
    val componentName: String = "",
    val rawExtractedText: String = "",
    val notes: String = ""
)

enum class InventoryFilter(val label: String) {
    ALL("All"),
    ACTION_REQUIRED("Action Required"),
    COMPLIANT("Compliant"),
    TAGGED_8130("8130-3 Present")
}

sealed class UiEvent {
    data class ShowToast(val message: String) : UiEvent()
    data class ShareCsv(val csvContent: String, val filename: String) : UiEvent()
}

class AvionicsViewModel(
    application: Application,
    private val repository: PartRepository,
    private val geminiService: GeminiDataPlateService = GeminiDataPlateService()
) : AndroidViewModel(application) {

    init {
        viewModelScope.launch {
            repository.seedSampleHistoryIfEmpty()
        }
    }

    private val _isCameraActive = MutableStateFlow(false)
    val isCameraActive: StateFlow<Boolean> = _isCameraActive.asStateFlow()

    private val _isConfirming = MutableStateFlow(false)
    val isConfirming: StateFlow<Boolean> = _isConfirming.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _isOcrProcessing = MutableStateFlow(false)
    val isOcrProcessing: StateFlow<Boolean> = _isOcrProcessing.asStateFlow()

    private val _ocrStatusMessage = MutableStateFlow<String?>(null)
    val ocrStatusMessage: StateFlow<String?> = _ocrStatusMessage.asStateFlow()

    private val _draftForm = MutableStateFlow(DraftPartForm())
    val draftForm: StateFlow<DraftPartForm> = _draftForm.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedFilter = MutableStateFlow(InventoryFilter.ALL)
    val selectedFilter: StateFlow<InventoryFilter> = _selectedFilter.asStateFlow()

    private val _selectedPartForDetail = MutableStateFlow<ScannedPart?>(null)
    val selectedPartForDetail: StateFlow<ScannedPart?> = _selectedPartForDetail.asStateFlow()

    private val _showAdReference = MutableStateFlow(false)
    val showAdReference: StateFlow<Boolean> = _showAdReference.asStateFlow()

    private val _showPresetPicker = MutableStateFlow(false)
    val showPresetPicker: StateFlow<Boolean> = _showPresetPicker.asStateFlow()

    private val _eventFlow = MutableSharedFlow<UiEvent>()
    val eventFlow: SharedFlow<UiEvent> = _eventFlow.asSharedFlow()

    // Real-time compliance check for the current draft form
    val liveAdCheck: StateFlow<AdCheckResult> = _draftForm
        .combine(MutableStateFlow(Unit)) { draft, _ ->
            AdComplianceChecker.checkCompliance(
                partNumber = draft.partNumber,
                serialNumber = draft.serialNumber,
                modMarks = draft.modMarks
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AdComplianceChecker.checkCompliance("", "", "")
        )

    // Filtered inventory list from Room database
    val filteredInventory: StateFlow<List<ScannedPart>> = combine(
        repository.allParts,
        _searchQuery,
        _selectedFilter
    ) { parts, query, filter ->
        parts.filter { part ->
            val matchesQuery = if (query.isBlank()) {
                true
            } else {
                part.partNumber.contains(query, ignoreCase = true) ||
                        part.serialNumber.contains(query, ignoreCase = true) ||
                        part.componentName.contains(query, ignoreCase = true) ||
                        part.rawExtractedText.contains(query, ignoreCase = true) ||
                        part.adIdentifier.contains(query, ignoreCase = true) ||
                        part.notes.contains(query, ignoreCase = true)
            }

            val matchesFilter = when (filter) {
                InventoryFilter.ALL -> true
                InventoryFilter.ACTION_REQUIRED -> part.adStatus == AdStatusLevel.ACTION_REQUIRED.displayName
                InventoryFilter.COMPLIANT -> part.adStatus == AdStatusLevel.COMPLIANT.displayName
                InventoryFilter.TAGGED_8130 -> part.has8130
            }

            matchesQuery && matchesFilter
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Summary counts directly from Room database flow
    val inventoryStats = repository.allParts.combine(MutableStateFlow(Unit)) { parts, _ ->
        val total = parts.size
        val actionRequired = parts.count { it.adStatus == AdStatusLevel.ACTION_REQUIRED.displayName }
        val tagged8130 = parts.count { it.has8130 }
        Triple(total, actionRequired, tagged8130)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = Triple(0, 0, 0)
    )

    fun setCameraActive(active: Boolean) {
        _isCameraActive.value = active
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setFilter(filter: InventoryFilter) {
        _selectedFilter.value = filter
    }

    fun selectPartForDetail(part: ScannedPart?) {
        _selectedPartForDetail.value = part
    }

    fun setShowAdReference(show: Boolean) {
        _showAdReference.value = show
    }

    fun setShowPresetPicker(show: Boolean) {
        _showPresetPicker.value = show
    }

    /**
     * Sends a photo bitmap taken by the camera to Gemini 3.5 Flash for OCR extraction.
     */
    fun processCapturedPhoto(bitmap: Bitmap) {
        viewModelScope.launch {
            _isOcrProcessing.value = true
            _ocrStatusMessage.value = "Analyzing data plate with Gemini 3.5 Flash OCR..."
            val result = geminiService.extractDataPlateFromBitmap(bitmap)
            _isOcrProcessing.value = false
            _ocrStatusMessage.value = null

            result.onSuccess { data ->
                val pNumber = data.partNumber?.trim()?.uppercase() ?: ""
                val sNumber = data.serialNumber?.trim()?.uppercase() ?: ""
                val mfg = data.mfgDate?.trim() ?: ""
                val mods = data.modMarks?.trim() ?: ""
                val cond = data.condition?.trim() ?: "As Removed (AR)"
                val tag8130 = data.has8130 ?: false
                val compName = data.componentName?.trim() ?: ""
                val extraNotes = data.notes?.trim()
                val rawText = data.rawExtractedText ?: ""

                val combinedNotes = buildString {
                    if (compName.isNotBlank()) {
                        append("Item: $compName")
                    }
                    if (!extraNotes.isNullOrBlank()) {
                        if (isNotEmpty()) append(" | ")
                        append(extraNotes)
                    }
                }

                _draftForm.value = DraftPartForm(
                    partNumber = pNumber,
                    serialNumber = sNumber,
                    mfgDate = mfg,
                    modMarks = mods,
                    condition = cond,
                    has8130 = tag8130,
                    componentName = compName,
                    rawExtractedText = rawText,
                    notes = combinedNotes
                )
                _isConfirming.value = true
                _eventFlow.emit(UiEvent.ShowToast("Data plate identified via Gemini OCR: ${if (pNumber.isNotBlank()) pNumber else "Review fields"}"))
            }.onFailure { err ->
                _eventFlow.emit(UiEvent.ShowToast("Gemini OCR error: ${err.localizedMessage ?: "Unknown"}. Applying fallback."))
                captureDataPlate()
            }
        }
    }

    /**
     * Processes sample data plate photo stored in assets via Gemini OCR
     */
    fun processSampleAssetPhoto(assetName: String = "images_26f45d1f.jpg") {
        viewModelScope.launch {
            val app = getApplication<Application>()
            val bitmap = try {
                app.assets.open(assetName).use { inputStream ->
                    BitmapFactory.decodeStream(inputStream)
                }
            } catch (e: Exception) {
                null
            }

            if (bitmap != null) {
                processCapturedPhoto(bitmap)
            } else {
                captureDataPlate()
            }
        }
    }

    /**
     * Fallback / Quick test simulation from predefined preset data plates.
     */
    fun captureDataPlate() {
        val presets = AdComplianceChecker.SAMPLE_PRESETS
        val sample = presets[Random.nextInt(presets.size)]
        val randomSerial = Random.nextInt(10000, 99999).toString()
        val comp = sample.label.substringBefore(" (")
        val rawJson = """
            {
              "componentName": "$comp",
              "partNumber": "${sample.partNumber}",
              "serialNumber": "$randomSerial",
              "mfgDate": "${sample.mfgDate}",
              "modMarks": "${sample.modMarks}",
              "condition": "${sample.condition}",
              "has8130": ${sample.has8130},
              "notes": "${sample.notes}"
            }
        """.trimIndent()

        _draftForm.value = DraftPartForm(
            partNumber = sample.partNumber,
            serialNumber = randomSerial,
            mfgDate = sample.mfgDate,
            modMarks = sample.modMarks,
            condition = sample.condition,
            has8130 = sample.has8130,
            componentName = comp,
            rawExtractedText = rawJson,
            notes = sample.notes
        )
        _isConfirming.value = true
    }

    fun openManualEntry() {
        _draftForm.value = DraftPartForm()
        _isConfirming.value = true
    }

    fun applyPreset(preset: AdComplianceChecker.PresetPlate) {
        val comp = preset.label.substringBefore(" (")
        val rawJson = """
            {
              "componentName": "$comp",
              "partNumber": "${preset.partNumber}",
              "serialNumber": "${preset.serialNumber}",
              "mfgDate": "${preset.mfgDate}",
              "modMarks": "${preset.modMarks}",
              "condition": "${preset.condition}",
              "has8130": ${preset.has8130},
              "notes": "${preset.notes}"
            }
        """.trimIndent()

        _draftForm.value = DraftPartForm(
            partNumber = preset.partNumber,
            serialNumber = preset.serialNumber,
            mfgDate = preset.mfgDate,
            modMarks = preset.modMarks,
            condition = preset.condition,
            has8130 = preset.has8130,
            componentName = comp,
            rawExtractedText = rawJson,
            notes = preset.notes
        )
        _showPresetPicker.value = false
        _isConfirming.value = true
    }

    fun updateDraft(
        partNumber: String? = null,
        serialNumber: String? = null,
        mfgDate: String? = null,
        modMarks: String? = null,
        condition: String? = null,
        has8130: Boolean? = null,
        componentName: String? = null,
        rawExtractedText: String? = null,
        notes: String? = null
    ) {
        _draftForm.update { current ->
            current.copy(
                partNumber = partNumber ?: current.partNumber,
                serialNumber = serialNumber ?: current.serialNumber,
                mfgDate = mfgDate ?: current.mfgDate,
                modMarks = modMarks ?: current.modMarks,
                condition = condition ?: current.condition,
                has8130 = has8130 ?: current.has8130,
                componentName = componentName ?: current.componentName,
                rawExtractedText = rawExtractedText ?: current.rawExtractedText,
                notes = notes ?: current.notes
            )
        }
    }

    fun cancelCapture() {
        _isConfirming.value = false
        _draftForm.value = DraftPartForm()
    }

    /**
     * Stores the scanned data plate into the Room database, including the timestamp and extracted text data.
     */
    fun saveDraftPart() {
        val draft = _draftForm.value
        if (draft.partNumber.isBlank()) {
            viewModelScope.launch {
                _eventFlow.emit(UiEvent.ShowToast("Part Number cannot be empty"))
            }
            return
        }

        viewModelScope.launch {
            _isSaving.value = true
            val adResult = AdComplianceChecker.checkCompliance(
                partNumber = draft.partNumber,
                serialNumber = draft.serialNumber,
                modMarks = draft.modMarks
            )

            val timeFormatter = SimpleDateFormat("HH:mm", Locale.US)
            val currentTime = timeFormatter.format(Date())
            val currentTimestamp = System.currentTimeMillis()

            val rawData = if (draft.rawExtractedText.isNotBlank()) {
                draft.rawExtractedText
            } else {
                """
                {
                  "componentName": "${draft.componentName.trim()}",
                  "partNumber": "${draft.partNumber.trim().uppercase()}",
                  "serialNumber": "${draft.serialNumber.trim().uppercase()}",
                  "mfgDate": "${draft.mfgDate.trim()}",
                  "modMarks": "${draft.modMarks.trim()}",
                  "condition": "${draft.condition}",
                  "has8130": ${draft.has8130},
                  "notes": "${draft.notes.trim()}"
                }
                """.trimIndent()
            }

            val newPart = ScannedPart(
                partNumber = draft.partNumber.trim().uppercase(),
                serialNumber = draft.serialNumber.trim().uppercase(),
                mfgDate = draft.mfgDate.trim(),
                modMarks = draft.modMarks.trim(),
                condition = draft.condition,
                has8130 = draft.has8130,
                adStatus = adResult.status.displayName,
                adIdentifier = adResult.identifier,
                adDetails = adResult.details,
                scanTime = currentTime,
                timestamp = currentTimestamp,
                componentName = draft.componentName.trim(),
                rawExtractedText = rawData,
                notes = draft.notes.trim()
            )

            val insertedId = repository.insertPart(newPart)
            _isSaving.value = false
            _isConfirming.value = false
            _draftForm.value = DraftPartForm()
            _eventFlow.emit(UiEvent.ShowToast("Saved #${insertedId} (${newPart.partNumber}) to Room database"))
        }
    }

    fun deletePart(id: Long) {
        viewModelScope.launch {
            repository.deleteById(id)
            if (_selectedPartForDetail.value?.id == id) {
                _selectedPartForDetail.value = null
            }
            _eventFlow.emit(UiEvent.ShowToast("Part removed from Room database"))
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.deleteAll()
            _selectedPartForDetail.value = null
            _eventFlow.emit(UiEvent.ShowToast("All scan history cleared from Room database"))
        }
    }

    fun exportCsv(context: Context) {
        viewModelScope.launch {
            val parts = repository.allParts
            var currentList: List<ScannedPart> = emptyList()
            kotlinx.coroutines.withTimeoutOrNull(1000) {
                parts.collect {
                    currentList = it
                    throw kotlinx.coroutines.CancellationException()
                }
            }

            if (currentList.isEmpty()) {
                _eventFlow.emit(UiEvent.ShowToast("No parts to export in Room database"))
                return@launch
            }

            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val dateStr = dateFormat.format(Date())
            val filename = "Avionics_Scans_$dateStr.csv"

            val csvContent = buildCsv(currentList)

            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Avionics Inventory CSV", csvContent)
            clipboard.setPrimaryClip(clip)

            _eventFlow.emit(UiEvent.ShareCsv(csvContent, filename))
            _eventFlow.emit(UiEvent.ShowToast("CSV exported & copied to clipboard"))
        }
    }

    private fun buildCsv(parts: List<ScannedPart>): String {
        val headers = listOf(
            "Record ID",
            "Part Number",
            "Serial Number",
            "Component Name",
            "Mfg Date",
            "Mod Marks",
            "Condition",
            "8130-3 Tag",
            "AD Status",
            "AD Identifier",
            "AD Details",
            "Scan Time",
            "Timestamp (ms)",
            "Formatted Scan Date",
            "Raw Extracted Text",
            "Notes"
        )

        fun escape(value: String): String {
            val sanitized = value.replace("\"", "\"\"")
            return if (sanitized.contains(",") || sanitized.contains("\"") || sanitized.contains("\n")) {
                "\"$sanitized\""
            } else {
                sanitized
            }
        }

        val rows = parts.map { part ->
            listOf(
                part.id.toString(),
                escape(part.partNumber),
                escape(part.serialNumber),
                escape(part.componentName),
                escape(part.mfgDate),
                escape(part.modMarks),
                escape(part.condition),
                if (part.has8130) "Yes" else "No",
                escape(part.adStatus),
                escape(part.adIdentifier),
                escape(part.adDetails),
                escape(part.scanTime),
                part.timestamp.toString(),
                escape(part.formattedDate()),
                escape(part.rawExtractedText),
                escape(part.notes)
            ).joinToString(",")
        }

        return (listOf(headers.joinToString(",")) + rows).joinToString("\n")
    }

    companion object {
        fun provideFactory(application: Application): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val database = AppDatabase.getDatabase(application)
                    val repository = PartRepository(database.partDao())
                    return AvionicsViewModel(application, repository) as T
                }
            }
    }
}

