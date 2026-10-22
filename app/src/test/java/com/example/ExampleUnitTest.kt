package com.example

import com.example.data.model.AdStatusLevel
import com.example.domain.AdComplianceChecker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

  @Test
  fun testAltimeterAd860502_missingModM_triggersActionRequired() {
    val result = AdComplianceChecker.checkCompliance(
      partNumber = "5934PA-1",
      serialNumber = "48102",
      modMarks = ""
    )
    assertEquals(AdStatusLevel.ACTION_REQUIRED, result.status)
    assertTrue(result.identifier.contains("86-05-02"))
  }

  @Test
  fun testAltimeterAd860502_withModM_isCompliant() {
    val result = AdComplianceChecker.checkCompliance(
      partNumber = "5934PA-1",
      serialNumber = "48102",
      modMarks = "M"
    )
    assertEquals(AdStatusLevel.COMPLIANT, result.status)
  }

  @Test
  fun testAltimeterAd860502_modMVariants_areCompliant() {
    for (marks in listOf("M", "m", "MOD M", "MODM", "Mod. M", "1, M", "M/2")) {
      val result = AdComplianceChecker.checkCompliance("5934PA-1", "48102", marks)
      assertEquals("modMarks='$marks'", AdStatusLevel.COMPLIANT, result.status)
    }
  }

  @Test
  fun testAltimeterAd860502_otherMarksContainingLetterM_triggerActionRequired() {
    // Previously any mark containing the letter M (e.g. the word "MOD") passed as Mod M.
    for (marks in listOf("MOD 1", "MOD A-D", "MODS 1-3", "MM", "AM")) {
      val result = AdComplianceChecker.checkCompliance("5934PA-1", "48102", marks)
      assertEquals("modMarks='$marks'", AdStatusLevel.ACTION_REQUIRED, result.status)
    }
  }

  @Test
  fun testNarcoAt150Ad941414_mod4Variants_areCompliant() {
    for (marks in listOf("4", "MOD 4", "MOD4", "1, 2, 4", "MOD 1-5", "04")) {
      val result = AdComplianceChecker.checkCompliance("AT-150", "31940", marks)
      assertEquals("modMarks='$marks'", AdStatusLevel.COMPLIANT, result.status)
    }
  }

  @Test
  fun testNarcoAt150Ad941414_numbersContainingDigit4_triggerActionRequired() {
    // Previously "14", "24" or "1-3, 40" passed as Mod 4.
    for (marks in listOf("", "14", "24", "MOD 1-3", "40", "1-3, 40")) {
      val result = AdComplianceChecker.checkCompliance("AT-150", "31940", marks)
      assertEquals("modMarks='$marks'", AdStatusLevel.ACTION_REQUIRED, result.status)
    }
  }

  @Test
  fun testParseModMarks() {
    assertEquals(setOf("M"), AdComplianceChecker.parseModMarks("MOD M"))
    assertEquals(setOf("1", "2"), AdComplianceChecker.parseModMarks("1, 2"))
    assertEquals(setOf("1", "2", "3", "4", "5"), AdComplianceChecker.parseModMarks("MOD 1 - 5"))
    assertEquals(setOf("A-D"), AdComplianceChecker.parseModMarks("MOD A-D"))
    assertEquals(emptySet<String>(), AdComplianceChecker.parseModMarks("  "))
  }

  @Test
  fun testTransponderKt76A_triggersActionRequired() {
    val result = AdComplianceChecker.checkCompliance(
      partNumber = "066-1062-00",
      serialNumber = "99120",
      modMarks = ""
    )
    assertEquals(AdStatusLevel.ACTION_REQUIRED, result.status)
    assertTrue(result.identifier.contains("2004-08-16"))
  }

  @Test
  fun testGarminGns430W_isAdvisory() {
    val result = AdComplianceChecker.checkCompliance(
      partNumber = "011-00280-10",
      serialNumber = "55123",
      modMarks = ""
    )
    assertEquals(AdStatusLevel.ADVISORY, result.status)
  }

  @Test
  fun testGeminiOcrJsonParsing() {
    val json = """
      {
        "partNumber": "5934PA-1",
        "serialNumber": "48102",
        "mfgDate": "1985-06",
        "modMarks": "M",
        "condition": "As Removed (AR)",
        "has8130": false,
        "componentName": "ALTIMETER",
        "notes": "TSO-C10b UNITED INSTRUMENTS"
      }
    """.trimIndent()
    val moshi = com.squareup.moshi.Moshi.Builder()
      .add(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory())
      .build()
    val adapter = moshi.adapter(com.example.data.api.ExtractedPlateData::class.java)
    val parsed = adapter.fromJson(json)
    org.junit.Assert.assertNotNull(parsed)
    assertEquals("5934PA-1", parsed?.partNumber)
    assertEquals("48102", parsed?.serialNumber)
    assertEquals("M", parsed?.modMarks)
    assertEquals(false, parsed?.has8130)
  }
}
