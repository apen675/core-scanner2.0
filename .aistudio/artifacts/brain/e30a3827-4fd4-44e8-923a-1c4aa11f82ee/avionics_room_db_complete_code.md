# Avionics Data Plate Scanner — Room Database Implementation & Code Archive

This document contains the complete Kotlin source code files for the local Room database persistence layer created for the Avionics Data Plate Scanner app, as well as instructions on how to download the project.

---

## 📦 How to Download Your Code

You have two convenient ways to download:

### Option 1: AI Studio Full Project Export (Recommended)
1. In the upper-right corner of the **Google AI Studio** window, click on the **Settings / Menu** icon (gear or three dots).
2. Click **"Export as ZIP"** (or **"Download ZIP"**).
3. This downloads the complete, ready-to-build Android Studio project including all Gradle scripts, manifests, resources, and Kotlin files.

### Option 2: Pre-Packaged Source ZIP File
A dedicated zip archive containing all updated Kotlin source packages has been generated in the root of your workspace:
- **Workspace Path:** `/avionics_room_db_source_code.zip`
- **Artifacts Path:** `/.aistudio/artifacts/brain/e30a3827-4fd4-44e8-923a-1c4aa11f82ee/avionics_room_db_source_code.zip`

---

## 1. Data Model (`app/src/main/java/com/example/data/model/ScannedPart.kt`)

```kotlin
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
```

---

## 2. Room DAO (`app/src/main/java/com/example/data/local/PartDao.kt`)

```kotlin
package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.ScannedPart
import kotlinx.coroutines.flow.Flow

@Dao
interface PartDao {

    @Query("SELECT * FROM scanned_parts ORDER BY timestamp DESC")
    fun getAllParts(): Flow<List<ScannedPart>>

    @Query("SELECT * FROM scanned_parts WHERE id = :id")
    suspend fun getPartById(id: Long): ScannedPart?

    @Query("SELECT * FROM scanned_parts WHERE id = :id")
    fun getPartByIdFlow(id: Long): Flow<ScannedPart?>

    @Query("SELECT * FROM scanned_parts WHERE partNumber LIKE '%' || :query || '%' OR serialNumber LIKE '%' || :query || '%' OR componentName LIKE '%' || :query || '%' OR rawExtractedText LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    fun searchParts(query: String): Flow<List<ScannedPart>>

    @Query("SELECT COUNT(*) FROM scanned_parts")
    fun getPartCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPart(part: ScannedPart): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertParts(parts: List<ScannedPart>): List<Long>

    @Update
    suspend fun updatePart(part: ScannedPart)

    @Delete
    suspend fun deletePart(part: ScannedPart)

    @Query("DELETE FROM scanned_parts WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM scanned_parts")
    suspend fun deleteAll()
}
```

---

## 3. Room Database (`app/src/main/java/com/example/data/local/AppDatabase.kt`)

```kotlin
package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.ScannedPart

@Database(entities = [ScannedPart::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun partDao(): PartDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "avionics_scanner_db"
                ).fallbackToDestructiveMigration(dropAllTables = true).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
```

---

## 4. Repository Layer (`app/src/main/java/com/example/data/repository/PartRepository.kt`)

```kotlin
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
```
