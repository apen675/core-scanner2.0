package com.example.data.api

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class GenerateContentRequest(
    @Json(name = "contents") val contents: List<Content>,
    @Json(name = "generationConfig") val generationConfig: GenerationConfig? = null
)

@JsonClass(generateAdapter = true)
data class Content(
    @Json(name = "parts") val parts: List<Part>
)

@JsonClass(generateAdapter = true)
data class Part(
    @Json(name = "text") val text: String? = null,
    @Json(name = "inlineData") val inlineData: InlineData? = null
)

@JsonClass(generateAdapter = true)
data class InlineData(
    @Json(name = "mimeType") val mimeType: String,
    @Json(name = "data") val data: String
)

@JsonClass(generateAdapter = true)
data class GenerationConfig(
    @Json(name = "responseMimeType") val responseMimeType: String? = null,
    @Json(name = "temperature") val temperature: Float? = null
)

@JsonClass(generateAdapter = true)
data class GenerateContentResponse(
    @Json(name = "candidates") val candidates: List<Candidate>? = null
)

@JsonClass(generateAdapter = true)
data class Candidate(
    @Json(name = "content") val content: Content? = null
)

@JsonClass(generateAdapter = true)
data class ExtractedPlateData(
    @Json(name = "partNumber") val partNumber: String? = null,
    @Json(name = "serialNumber") val serialNumber: String? = null,
    @Json(name = "mfgDate") val mfgDate: String? = null,
    @Json(name = "modMarks") val modMarks: String? = null,
    @Json(name = "condition") val condition: String? = null,
    @Json(name = "has8130") val has8130: Boolean? = null,
    @Json(name = "componentName") val componentName: String? = null,
    @Json(name = "notes") val notes: String? = null,
    @Json(name = "rawExtractedText") val rawExtractedText: String? = null
)

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

class GeminiDataPlateService {

    private val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        })
        .build()

    private val apiService: GeminiApiService = Retrofit.Builder()
        .baseUrl("https://generativelanguage.googleapis.com/")
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
        .create(GeminiApiService::class.java)

    /**
     * Converts bitmap to base64 JPEG with bounded dimension to optimize network latency
     */
    private fun bitmapToBase64(bitmap: Bitmap, maxDimension: Int = 1280, quality: Int = 85): String {
        val width = bitmap.width
        val height = bitmap.height
        val scaled = if (width > maxDimension || height > maxDimension) {
            val ratio = width.toFloat() / height.toFloat()
            val targetWidth: Int
            val targetHeight: Int
            if (width >= height) {
                targetWidth = maxDimension
                targetHeight = (maxDimension / ratio).toInt().coerceAtLeast(1)
            } else {
                targetHeight = maxDimension
                targetWidth = (maxDimension * ratio).toInt().coerceAtLeast(1)
            }
            Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
        } else {
            bitmap
        }

        val outputStream = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    }

    suspend fun extractDataPlateFromBitmap(bitmap: Bitmap): Result<ExtractedPlateData> = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY

        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            Log.w("GeminiService", "GEMINI_API_KEY is not configured or placeholder. Simulating OCR extraction.")
            val sampleJson = """
                {
                  "componentName": "ALTIMETER 35,000 FT",
                  "partNumber": "5934PA-1",
                  "serialNumber": "48102",
                  "mfgDate": "1985-06",
                  "modMarks": "M",
                  "condition": "As Removed (AR)",
                  "has8130": false,
                  "notes": "TSO-C10b UNITED INSTRUMENTS INC. WICHITA KANSAS"
                }
            """.trimIndent()
            return@withContext Result.success(
                ExtractedPlateData(
                    partNumber = "5934PA-1",
                    serialNumber = "48102",
                    mfgDate = "1985-06",
                    modMarks = "M",
                    condition = "As Removed (AR)",
                    has8130 = false,
                    componentName = "ALTIMETER 35,000 FT",
                    notes = "Demo OCR: Configure GEMINI_API_KEY in AI Studio Secrets panel for live cloud model extraction.",
                    rawExtractedText = sampleJson
                )
            )
        }

        try {
            val base64Image = bitmapToBase64(bitmap)

            val prompt = """
                You are an expert aerospace quality inspector and avionics technician analyzing an image of an aircraft or avionics data plate / identification plate.
                Perform precise Optical Character Recognition (OCR) and return ONLY a valid JSON object with the following fields:
                - "partNumber": The manufacturer part number (P/N, Part No., Model No.). Keep exact dashes, uppercase, and suffixes (e.g., '5934PA-1', '066-1070-00', '13-0004-1').
                - "serialNumber": The unit serial number (S/N, Serial No., e.g., '34281', '4910').
                - "mfgDate": The date of manufacture in YYYY-MM format or MM/YY as printed on the plate (e.g. '1985-06', '1998-11', '08/94', or empty string if not found).
                - "modMarks": Modification status symbols, letters or stamps marked on mod dots/grid (e.g. 'M', '1-3', 'MOD A-D', or empty string if none marked).
                - "condition": Inferred or stamped condition ('As Removed (AR)', 'Serviceable (SV)', 'Overhauled (OH)', or 'New (NE)'). Defaults to 'As Removed (AR)'.
                - "has8130": true if an FAA Form 8130-3 or EASA Form 1 authorization tag or airworthiness release stamp is visible, otherwise false.
                - "componentName": The instrument / avionics name (e.g., 'ALTIMETER', 'VHF NAV/COMM TRANSCEIVER', 'TRANSPONDER', 'DIRECTIONAL GYRO').
                - "notes": Any manufacturer name, TSO certification (e.g. TSO-C10b), voltage, or condition remarks visible on the plate.

                Strict output requirement: Return only the JSON object, with no markdown code blocks and no surrounding explanatory text.
            """.trimIndent()

            val request = GenerateContentRequest(
                contents = listOf(
                    Content(
                        parts = listOf(
                            Part(text = prompt),
                            Part(inlineData = InlineData(mimeType = "image/jpeg", data = base64Image))
                        )
                    )
                ),
                generationConfig = GenerationConfig(
                    responseMimeType = "application/json",
                    temperature = 0.1f
                )
            )

            val response = apiService.generateContent(apiKey, request)
            val responseText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: throw IllegalStateException("Empty response from Gemini API")

            val cleanJson = responseText.trim()
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()

            val adapter = moshi.adapter(ExtractedPlateData::class.java)
            val parsed = adapter.fromJson(cleanJson)
                ?: throw IllegalStateException("Failed to parse OCR JSON response: $cleanJson")

            val withRawText = parsed.copy(
                rawExtractedText = cleanJson
            )

            Result.success(withRawText)
        } catch (e: Exception) {
            Log.e("GeminiService", "OCR extraction error", e)
            Result.failure(e)
        }
    }
}
