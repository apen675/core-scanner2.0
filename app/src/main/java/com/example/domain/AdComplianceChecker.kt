package com.example.domain

import com.example.data.model.AdCheckResult
import com.example.data.model.AdStatusLevel
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object AdComplianceChecker {

    /**
     * Inspects part number, serial number, and mod marks against known FAA Airworthiness Directives.
     */
    fun checkCompliance(
        partNumber: String,
        serialNumber: String,
        modMarks: String
    ): AdCheckResult {
        val cleanPn = partNumber.replace(Regex("[^a-zA-Z0-9]"), "").uppercase()
        val cleanMod = modMarks.uppercase().trim()
        val cleanSn = serialNumber.uppercase().trim()

        if (cleanPn.isEmpty()) {
            return AdCheckResult(
                status = AdStatusLevel.UNKNOWN,
                identifier = "No P/N Entered",
                details = "Enter a valid avionics part number to perform screening.",
                recommendation = "Enter Part Number"
            )
        }

        // 1. United Instruments / Cessna Altimeter AD 86-05-02
        // Applicable P/Ns: 5934PA-1, 5934PD-1, 5934P-1, C661071-0201, C661071-0102, C661071-0501
        if (cleanPn.startsWith("5934") ||
            cleanPn.startsWith("C661071") ||
            cleanPn.contains("5934PA")
        ) {
            val hasModM = cleanMod.contains("M") || cleanMod.contains("MODM") || cleanMod.contains("MOD M")
            return if (hasModM) {
                AdCheckResult(
                    status = AdStatusLevel.COMPLIANT,
                    identifier = "AD 86-05-02 (Verified)",
                    details = "Mod stamp 'M' detected on data plate. Barometric scale diaphragm modification verified. Check 24-month FAR 91.411 altimeter calibration.",
                    recommendation = "Verified compliant for mod stamp. Inspect seal."
                )
            } else {
                AdCheckResult(
                    status = AdStatusLevel.ACTION_REQUIRED,
                    identifier = "AD 86-05-02 (Missing 'M' Mod)",
                    details = "United Instruments Altimeter Barometric Scale / Diaphragm defect. Mandatory Action Required: Inspect for factory/overhaul mod plate punch or letter 'M'. Do not install in IFR aircraft without compliance certification.",
                    recommendation = "Do not install before shop overhaul / 'M' mod stamp verification."
                )
            }
        }

        // 2. BendixKing KT-76A / KT-78A Transponder AD 2004-08-16
        if (cleanPn.contains("0661062") ||
            cleanPn.contains("KT76A") ||
            cleanPn.contains("KT76") ||
            cleanPn.contains("KT78")
        ) {
            return AdCheckResult(
                status = AdStatusLevel.ACTION_REQUIRED,
                identifier = "AD 2004-08-16 / SB KT 76A-A1",
                details = "Cavity oscillator frequency drift check. Transponders with serial numbers in affected batches may transmit on inaccurate frequencies. Verify FAR 91.413 bench test certification within 24 calendar months.",
                recommendation = "Perform Mode A/C frequency & framing bench test before return to service."
            )
        }

        // 3. Narco AT-50 / AT-150 Transponder AD 94-14-14
        if (cleanPn.contains("AT150") || cleanPn.contains("AT50")) {
            val hasMod4 = cleanMod.contains("4") || cleanMod.contains("MOD4") || cleanMod.contains("M4")
            return if (hasMod4) {
                AdCheckResult(
                    status = AdStatusLevel.COMPLIANT,
                    identifier = "AD 94-14-14 (Mod 4 Complied)",
                    details = "High-voltage capacitor dielectric breakdown mod verified stamped on chassis.",
                    recommendation = "FAR 91.413 certification required prior to IFR flight."
                )
            } else {
                AdCheckResult(
                    status = AdStatusLevel.ACTION_REQUIRED,
                    identifier = "AD 94-14-14",
                    details = "Hazardous smoke and loss of altitude reporting due to internal capacitor failure. Modification to replace capacitors required before aircraft operation.",
                    recommendation = "Check bench records for capacitor modification."
                )
            }
        }

        // 4. Garmin GNS 430 / 530 / GNS 430W WAAS
        if (cleanPn.contains("01100280") ||
            cleanPn.contains("GNS430") ||
            cleanPn.contains("GNS530") ||
            cleanPn.contains("GNS430W")
        ) {
            return AdCheckResult(
                status = AdStatusLevel.ADVISORY,
                identifier = "FAA AC 20-165B / ADS-B Out Position Source",
                details = "Main software level v5.40 and GPS software v5.0 required for TSO-C145/146 compliant ADS-B Out pairing. Check internal memory battery if unit unpowered > 6 months.",
                recommendation = "Verify software version on power-up diagnostic screen."
            )
        }

        // 5. Garmin GTX 330 / GTX 330ES Transponder
        if (cleanPn.contains("01100455") || cleanPn.contains("GTX330")) {
            return AdCheckResult(
                status = AdStatusLevel.ADVISORY,
                identifier = "Garmin Service Advisory / ADS-B Extended Squitter",
                details = "GTX 330 requires ES software v7.04+ for FAA 14 CFR 91.227 rule compliance. Non-ES units provide Mode S only without 1090MHz broadcast.",
                recommendation = "Confirm ES software level in setup mode."
            )
        }

        // 6. Collins VIR-351 / VHF-251 Navigation & Comm
        if (cleanPn.contains("VIR351") || cleanPn.contains("VHF251")) {
            return AdCheckResult(
                status = AdStatusLevel.COMPLIANT,
                identifier = "Standard Avionics Screening",
                details = "No urgent active emergency ADs found. Standard bench alignment and audio sensitivity check recommended prior to installation.",
                recommendation = "Perform bench alignment."
            )
        }

        // 7. Honeywell / Bendix KRA-10A Radar Altimeter AD 2021-23-12
        if (cleanPn.contains("KRA10") || cleanPn.contains("KRA405")) {
            return AdCheckResult(
                status = AdStatusLevel.ACTION_REQUIRED,
                identifier = "AD 2021-23-12 (5G C-Band Interference)",
                details = "Requires flight manual limitation updates prohibiting certain radar-altimeter-dependent instrument approach procedures in 5G C-Band NOTAM areas unless filtering modification installed.",
                recommendation = "Verify RF front-end bandpass filter compliance."
            )
        }

        // 8. Piper / Edo-Aire Mitchell Autopilot Century II/III
        if (cleanPn.contains("1C388") || cleanPn.contains("CENTURY")) {
            return AdCheckResult(
                status = AdStatusLevel.ACTION_REQUIRED,
                identifier = "AD 79-20-05 (Pitch Servo Bridle Cable)",
                details = "Check bridle cable clamp slippage and roll servo sheer pin torque.",
                recommendation = "Perform 100-hour cable tension & clamp check."
            )
        }

        // Default: Unknown / Verify FAA DRS
        return AdCheckResult(
            status = AdStatusLevel.UNKNOWN,
            identifier = "FAA DRS Verification Required",
            details = "No automatic rule match for '$partNumber'. Perform search in FAA Dynamic Regulatory System (DRS) using Part Number and Serial Number before certification.",
            recommendation = "Search FAA DRS database."
        )
    }

    /**
     * Generates a direct search link to the FAA Dynamic Regulatory System (DRS).
     */
    fun getDrsSearchUrl(partNumber: String): String {
        val query = if (partNumber.isNotBlank()) partNumber.trim() else "Airworthiness Directive avionics"
        val encoded = URLEncoder.encode(query, StandardCharsets.UTF_8.toString())
        return "https://drs.faa.gov/browse/excelExternalWindow/DRS_RESULTS?query=$encoded"
    }

    /**
     * Quick presets representing real avionics data plates for instant screening test.
     */
    data class PresetPlate(
        val label: String,
        val subtitle: String,
        val partNumber: String,
        val serialNumber: String,
        val mfgDate: String,
        val modMarks: String,
        val condition: String,
        val has8130: Boolean,
        val notes: String
    )

    val SAMPLE_PRESETS = listOf(
        PresetPlate(
            label = "United Instruments 5934PA-1 (No Mod M)",
            subtitle = "Altimeter - Triggers AD 86-05-02 Alert",
            partNumber = "5934PA-1",
            serialNumber = "48291",
            mfgDate = "1983-04",
            modMarks = "",
            condition = "As Removed (AR)",
            has8130 = false,
            notes = "Removed from Cessna 172N left panel"
        ),
        PresetPlate(
            label = "United Instruments 5934PA-1 (With Mod M)",
            subtitle = "Altimeter - AD Compliant (Mod M verified)",
            partNumber = "5934PA-1",
            serialNumber = "91024",
            mfgDate = "1994-08",
            modMarks = "M",
            condition = "Serviceable (SV)",
            has8130 = true,
            notes = "Overhauled by Mid-Continent Instruments. Yellow tag attached."
        ),
        PresetPlate(
            label = "BendixKing KT-76A Transponder",
            subtitle = "Mode C - Triggers AD 2004-08-16 Check",
            partNumber = "066-1062-00",
            serialNumber = "65123",
            mfgDate = "1989-11",
            modMarks = "1, 2",
            condition = "As Removed (AR)",
            has8130 = false,
            notes = "Requires 24-mo FAR 91.413 recertification bench check"
        ),
        PresetPlate(
            label = "Garmin GNS 430W WAAS GPS/NAV/COMM",
            subtitle = "WAAS GPS - Advisory Check",
            partNumber = "011-00280-10",
            serialNumber = "97103412",
            mfgDate = "2008-05",
            modMarks = "MOD 1-5",
            condition = "Serviceable (SV)",
            has8130 = true,
            notes = "Main SW 5.40 / GPS SW 5.0. 14 CFR 91.227 ADS-B compliant"
        ),
        PresetPlate(
            label = "Narco AT-150 Transponder",
            subtitle = "Transponder - Triggers AD 94-14-14 Check",
            partNumber = "AT-150",
            serialNumber = "31940",
            mfgDate = "1978-02",
            modMarks = "",
            condition = "As Removed (AR)",
            has8130 = false,
            notes = "Capacitor modification status unknown"
        ),
        PresetPlate(
            label = "Collins VIR-351 VOR/LOC Nav",
            subtitle = "Nav Receiver - Standard Screening",
            partNumber = "VIR-351",
            serialNumber = "28410",
            mfgDate = "1981-09",
            modMarks = "MOD 1",
            condition = "Serviceable (SV)",
            has8130 = false,
            notes = "Bench tested OK for localizer tracking"
        )
    )
}
