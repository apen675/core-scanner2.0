package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Entity(tableName = "scanned_parts")
data class ScannedPart(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val partNumber: String,
    val serialNumber: String = "",
    val mfgDate: String = "",
    val modMarks: String = "",
    val condition: String = "As Removed (AR)",
    val has8130: Boolean = false,
    val adStatus: String = "Unknown",
    val adIdentifier: String = "",
    val adDetails: String = "",
    val scanTime: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val componentName: String = "",
    val rawExtractedText: String = "",
    val notes: String = ""
) {
    fun formattedDate(): String {
        val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm:ss", Locale.US)
        return sdf.format(Date(timestamp))
    }
}

enum class AdStatusLevel(val displayName: String) {
    ACTION_REQUIRED("Action Required"),
    COMPLIANT("Compliant"),
    ADVISORY("Advisory"),
    UNKNOWN("Unknown")
}

data class AdCheckResult(
    val status: AdStatusLevel,
    val identifier: String,
    val details: String,
    val recommendation: String = ""
)

