package com.example.data.repository

import com.example.data.local.PartDao
import com.example.data.model.ScannedPart
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class PartRepository(private val partDao: PartDao) {

    val allParts: Flow<List<ScannedPart>> = partDao.getAllParts()

    val partCount: Flow<Int> = partDao.getPartCount()

    fun searchParts(query: String): Flow<List<ScannedPart>> = partDao.searchParts(query)

    suspend fun getPartById(id: Long): ScannedPart? = partDao.getPartById(id)

    fun getPartByIdFlow(id: Long): Flow<ScannedPart?> = partDao.getPartByIdFlow(id)

    suspend fun insertPart(part: ScannedPart): Long = partDao.insertPart(part)

    suspend fun insertParts(parts: List<ScannedPart>): List<Long> = partDao.insertParts(parts)

    suspend fun updatePart(part: ScannedPart) = partDao.updatePart(part)

    suspend fun deletePart(part: ScannedPart) = partDao.deletePart(part)

    suspend fun deleteById(id: Long) = partDao.deleteById(id)

    suspend fun deleteAll() = partDao.deleteAll()

    suspend fun seedSampleHistoryIfEmpty() {
        val currentList = allParts.first()
        if (currentList.isNotEmpty()) return

        val now = System.currentTimeMillis()
        val oneHourAgo = now - 3600_000L * 2
        val yesterday = now - 86400_000L * 1

        val initialHistory = listOf(
            ScannedPart(
                partNumber = "5934PA-1",
                serialNumber = "48102",
                mfgDate = "1985-06",
                modMarks = "M",
                condition = "Serviceable (SV)",
                has8130 = true,
                adStatus = "Compliant",
                adIdentifier = "AD 86-05-02 (Verified)",
                adDetails = "Mod stamp 'M' detected on data plate. Barometric scale diaphragm modification verified. Check 24-month FAR 91.411 altimeter calibration.",
                scanTime = "14:28",
                timestamp = now,
                componentName = "ALTIMETER 35,000 FT",
                rawExtractedText = """
                    {
                      "componentName": "ALTIMETER 35,000 FT",
                      "partNumber": "5934PA-1",
                      "serialNumber": "48102",
                      "mfgDate": "1985-06",
                      "modMarks": "M",
                      "condition": "Serviceable (SV)",
                      "has8130": true,
                      "notes": "TSO-C10b UNITED INSTRUMENTS INC. WICHITA KANSAS"
                    }
                """.trimIndent(),
                notes = "Item: ALTIMETER 35,000 FT | TSO-C10b United Instruments"
            ),
            ScannedPart(
                partNumber = "066-1062-00",
                serialNumber = "65123",
                mfgDate = "1989-11",
                modMarks = "1, 2",
                condition = "As Removed (AR)",
                has8130 = false,
                adStatus = "Action Required",
                adIdentifier = "AD 2004-08-16 / SB KT 76A-A1",
                adDetails = "Cavity oscillator frequency drift check. Transponders with serial numbers in affected batches may transmit on inaccurate frequencies. Verify FAR 91.413 bench test certification within 24 calendar months.",
                scanTime = "11:15",
                timestamp = oneHourAgo,
                componentName = "KT-76A TRANSPONDER",
                rawExtractedText = """
                    {
                      "componentName": "KT-76A TRANSPONDER",
                      "partNumber": "066-1062-00",
                      "serialNumber": "65123",
                      "mfgDate": "1989-11",
                      "modMarks": "1, 2",
                      "condition": "As Removed (AR)",
                      "has8130": false,
                      "notes": "BENDIX/KING ALLIEDSIGNAL AVIONICS OLATHE KS"
                    }
                """.trimIndent(),
                notes = "Requires 24-mo FAR 91.413 recertification bench check"
            ),
            ScannedPart(
                partNumber = "011-00280-10",
                serialNumber = "97103412",
                mfgDate = "2008-05",
                modMarks = "MOD 1-5",
                condition = "Serviceable (SV)",
                has8130 = true,
                adStatus = "Advisory",
                adIdentifier = "FAA AC 20-165B / ADS-B Out Position Source",
                adDetails = "Main software level v5.40 and GPS software v5.0 required for TSO-C145/146 compliant ADS-B Out pairing. Check internal memory battery if unit unpowered > 6 months.",
                scanTime = "Yesterday",
                timestamp = yesterday,
                componentName = "GNS 430W GPS/NAV/COMM",
                rawExtractedText = """
                    {
                      "componentName": "GNS 430W GPS/NAV/COMM",
                      "partNumber": "011-00280-10",
                      "serialNumber": "97103412",
                      "mfgDate": "2008-05",
                      "modMarks": "MOD 1-5",
                      "condition": "Serviceable (SV)",
                      "has8130": true,
                      "notes": "GARMIN AT INC. SALEM OR TSO-C146a"
                    }
                """.trimIndent(),
                notes = "Main SW 5.40 / GPS SW 5.0. 14 CFR 91.227 ADS-B compliant"
            )
        )
        insertParts(initialHistory)
    }
}

