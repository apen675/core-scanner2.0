package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.PartDao
import com.example.data.model.ScannedPart
import com.example.data.repository.PartRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  private lateinit var database: AppDatabase
  private lateinit var partDao: PartDao
  private lateinit var repository: PartRepository

  @Before
  fun createDb() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
      .allowMainThreadQueries()
      .build()
    partDao = database.partDao()
    repository = PartRepository(partDao)
  }

  @After
  @Throws(IOException::class)
  fun closeDb() {
    database.close()
  }

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Avionics Scanner", appName)
  }

  @Test
  fun `room database stores scanned data plate with timestamp and extracted text`() = runBlocking {
    val testTimestamp = 1715000000000L
    val sampleRawOcr = """
      {
        "componentName": "ALTIMETER 35,000 FT",
        "partNumber": "5934PA-1",
        "serialNumber": "48102",
        "mfgDate": "1985-06",
        "modMarks": "M",
        "condition": "Serviceable (SV)",
        "has8130": true
      }
    """.trimIndent()

    val scannedPart = ScannedPart(
      partNumber = "5934PA-1",
      serialNumber = "48102",
      mfgDate = "1985-06",
      modMarks = "M",
      condition = "Serviceable (SV)",
      has8130 = true,
      adStatus = "Compliant",
      adIdentifier = "AD 86-05-02 (Verified)",
      adDetails = "Mod stamp 'M' verified",
      scanTime = "14:32",
      timestamp = testTimestamp,
      componentName = "ALTIMETER 35,000 FT",
      rawExtractedText = sampleRawOcr,
      notes = "Bench tested"
    )

    val insertedId = repository.insertPart(scannedPart)
    assertTrue("Inserted ID should be positive", insertedId > 0)

    val parts = repository.allParts.first()
    assertEquals(1, parts.size)

    val saved = parts.first()
    assertEquals("5934PA-1", saved.partNumber)
    assertEquals("48102", saved.serialNumber)
    assertEquals("ALTIMETER 35,000 FT", saved.componentName)
    assertEquals(testTimestamp, saved.timestamp)
    assertEquals(sampleRawOcr, saved.rawExtractedText)
    assertTrue(saved.has8130)
    assertEquals("Compliant", saved.adStatus)
  }

  @Test
  fun `room database history ordering and deletion`() = runBlocking {
    val part1 = ScannedPart(
      partNumber = "PART-A",
      timestamp = 1000L,
      scanTime = "10:00",
      rawExtractedText = "raw-text-A"
    )
    val part2 = ScannedPart(
      partNumber = "PART-B",
      timestamp = 2000L,
      scanTime = "11:00",
      rawExtractedText = "raw-text-B"
    )

    repository.insertPart(part1)
    repository.insertPart(part2)

    val list = repository.allParts.first()
    assertEquals(2, list.size)
    // ORDER BY timestamp DESC should put PART-B (timestamp 2000) first
    assertEquals("PART-B", list[0].partNumber)
    assertEquals("PART-A", list[1].partNumber)

    // Delete first part
    repository.deleteById(list[0].id)
    val remaining = repository.allParts.first()
    assertEquals(1, remaining.size)
    assertEquals("PART-A", remaining[0].partNumber)

    // Clear all
    repository.deleteAll()
    val empty = repository.allParts.first()
    assertEquals(0, empty.size)
  }

  @Test
  fun `seedSampleHistoryIfEmpty populates initial scans with timestamp and ocr`() = runBlocking {
    repository.seedSampleHistoryIfEmpty()

    val list = repository.allParts.first()
    assertTrue("Should seed at least 3 initial history items", list.size >= 3)
    list.forEach { part ->
      assertTrue("Timestamp should be non-zero", part.timestamp > 0)
      assertTrue("Raw extracted text should not be blank", part.rawExtractedText.isNotBlank())
      assertTrue("Part number should not be blank", part.partNumber.isNotBlank())
    }
  }
}

