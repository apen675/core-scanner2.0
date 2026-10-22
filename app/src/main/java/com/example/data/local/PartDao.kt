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

