// Web port of app/src/main/java/com/example/domain/AdComplianceChecker.kt.
// Keep the two in sync: same rule order, same mod-mark parsing.

export const Status = Object.freeze({
  ACTION_REQUIRED: "Action Required",
  COMPLIANT: "Compliant",
  ADVISORY: "Advisory",
  UNKNOWN: "Unknown",
});

/**
 * Splits a data plate mod-mark string into individual mod tokens, e.g.
 * "MOD M" -> {M}, "1, 2" -> {1, 2}, "MOD 1-5" -> {1, 2, 3, 4, 5}.
 * Numeric ranges are expanded; letter ranges like "A-D" stay a single token so
 * they can never satisfy a single-letter mod check. Anything unrecognized simply
 * doesn't match, so unclear marks fall through to ACTION_REQUIRED.
 */
export function parseModMarks(modMarks) {
  const normalized = String(modMarks ?? "")
    .toUpperCase()
    .replace(/\s*-\s*/g, "-")
    .replace(/\bMODS?\.?\s*/g, " ");
  const tokens = new Set();
  for (const token of normalized.split(/[\s,;/&+]+/)) {
    if (!token) continue;
    const range = token.match(/^(\d+)-(\d+)$/);
    if (range) {
      const start = parseInt(range[1], 10);
      const end = parseInt(range[2], 10);
      if (start <= end && end - start <= 50) {
        for (let i = start; i <= end; i++) tokens.add(String(i));
        continue;
      }
    }
    // "04" and "4" are the same mod number
    tokens.add(/^\d+$/.test(token) ? token.replace(/^0+/, "") || "0" : token);
  }
  return tokens;
}

/** Screens a part number + mod marks against the known FAA Airworthiness Directives. */
export function checkCompliance(partNumber, serialNumber, modMarks) {
  const cleanPn = String(partNumber ?? "").replace(/[^a-zA-Z0-9]/g, "").toUpperCase();
  const mods = parseModMarks(modMarks);

  if (!cleanPn) {
    return {
      status: Status.UNKNOWN,
      identifier: "No P/N Entered",
      details: "Enter a valid avionics part number to perform screening.",
      recommendation: "Enter Part Number",
    };
  }

  // 1. United Instruments / Cessna Altimeter AD 86-05-02
  if (cleanPn.startsWith("5934") || cleanPn.startsWith("C661071") || cleanPn.includes("5934PA")) {
    return mods.has("M")
      ? {
          status: Status.COMPLIANT,
          identifier: "AD 86-05-02 (Verified)",
          details: "Mod stamp 'M' detected on data plate. Barometric scale diaphragm modification verified. Check 24-month FAR 91.411 altimeter calibration.",
          recommendation: "Verified compliant for mod stamp. Inspect seal.",
        }
      : {
          status: Status.ACTION_REQUIRED,
          identifier: "AD 86-05-02 (Missing 'M' Mod)",
          details: "United Instruments Altimeter Barometric Scale / Diaphragm defect. Mandatory Action Required: Inspect for factory/overhaul mod plate punch or letter 'M'. Do not install in IFR aircraft without compliance certification.",
          recommendation: "Do not install before shop overhaul / 'M' mod stamp verification.",
        };
  }

  // 2. BendixKing KT-76A / KT-78A Transponder AD 2004-08-16
  if (["0661062", "KT76A", "KT76", "KT78"].some((p) => cleanPn.includes(p))) {
    return {
      status: Status.ACTION_REQUIRED,
      identifier: "AD 2004-08-16 / SB KT 76A-A1",
      details: "Cavity oscillator frequency drift check. Transponders with serial numbers in affected batches may transmit on inaccurate frequencies. Verify FAR 91.413 bench test certification within 24 calendar months.",
      recommendation: "Perform Mode A/C frequency & framing bench test before return to service.",
    };
  }

  // 3. Narco AT-50 / AT-150 Transponder AD 94-14-14
  if (cleanPn.includes("AT150") || cleanPn.includes("AT50")) {
    return mods.has("4")
      ? {
          status: Status.COMPLIANT,
          identifier: "AD 94-14-14 (Mod 4 Complied)",
          details: "High-voltage capacitor dielectric breakdown mod verified stamped on chassis.",
          recommendation: "FAR 91.413 certification required prior to IFR flight.",
        }
      : {
          status: Status.ACTION_REQUIRED,
          identifier: "AD 94-14-14",
          details: "Hazardous smoke and loss of altitude reporting due to internal capacitor failure. Modification to replace capacitors required before aircraft operation.",
          recommendation: "Check bench records for capacitor modification.",
        };
  }

  // 4. Garmin GNS 430 / 530 / GNS 430W WAAS
  if (["01100280", "GNS430", "GNS530"].some((p) => cleanPn.includes(p))) {
    return {
      status: Status.ADVISORY,
      identifier: "FAA AC 20-165B / ADS-B Out Position Source",
      details: "Main software level v5.40 and GPS software v5.0 required for TSO-C145/146 compliant ADS-B Out pairing. Check internal memory battery if unit unpowered > 6 months.",
      recommendation: "Verify software version on power-up diagnostic screen.",
    };
  }

  // 5. Garmin GTX 330 / GTX 330ES Transponder
  if (cleanPn.includes("01100455") || cleanPn.includes("GTX330")) {
    return {
      status: Status.ADVISORY,
      identifier: "Garmin Service Advisory / ADS-B Extended Squitter",
      details: "GTX 330 requires ES software v7.04+ for FAA 14 CFR 91.227 rule compliance. Non-ES units provide Mode S only without 1090MHz broadcast.",
      recommendation: "Confirm ES software level in setup mode.",
    };
  }

  // 6. Collins VIR-351 / VHF-251 Navigation & Comm
  if (cleanPn.includes("VIR351") || cleanPn.includes("VHF251")) {
    return {
      status: Status.COMPLIANT,
      identifier: "Standard Avionics Screening",
      details: "No urgent active emergency ADs found. Standard bench alignment and audio sensitivity check recommended prior to installation.",
      recommendation: "Perform bench alignment.",
    };
  }

  // 7. Honeywell / Bendix KRA-10A Radar Altimeter AD 2021-23-12
  if (cleanPn.includes("KRA10") || cleanPn.includes("KRA405")) {
    return {
      status: Status.ACTION_REQUIRED,
      identifier: "AD 2021-23-12 (5G C-Band Interference)",
      details: "Requires flight manual limitation updates prohibiting certain radar-altimeter-dependent instrument approach procedures in 5G C-Band NOTAM areas unless filtering modification installed.",
      recommendation: "Verify RF front-end bandpass filter compliance.",
    };
  }

  // 8. Piper / Edo-Aire Mitchell Autopilot Century II/III
  if (cleanPn.includes("1C388") || cleanPn.includes("CENTURY")) {
    return {
      status: Status.ACTION_REQUIRED,
      identifier: "AD 79-20-05 (Pitch Servo Bridle Cable)",
      details: "Check bridle cable clamp slippage and roll servo sheer pin torque.",
      recommendation: "Perform 100-hour cable tension & clamp check.",
    };
  }

  return {
    status: Status.UNKNOWN,
    identifier: "FAA DRS Verification Required",
    details: `No automatic rule match for '${partNumber}'. Perform search in FAA Dynamic Regulatory System (DRS) using Part Number and Serial Number before certification.`,
    recommendation: "Search FAA DRS database.",
  };
}

/** Direct search link into the FAA Dynamic Regulatory System (DRS). */
export function getDrsSearchUrl(partNumber) {
  const query = String(partNumber ?? "").trim() || "Airworthiness Directive avionics";
  return `https://drs.faa.gov/browse/excelExternalWindow/DRS_RESULTS?query=${encodeURIComponent(query)}`;
}

/** Demo data plates for instant screening tests (same as SAMPLE_PRESETS in the Android app). */
export const SAMPLE_PRESETS = [
  { label: "United Instruments 5934PA-1 (No Mod M)", subtitle: "Altimeter - Triggers AD 86-05-02 Alert", partNumber: "5934PA-1", serialNumber: "48291", mfgDate: "1983-04", modMarks: "", condition: "As Removed (AR)", has8130: false, notes: "Removed from Cessna 172N left panel" },
  { label: "United Instruments 5934PA-1 (With Mod M)", subtitle: "Altimeter - AD Compliant (Mod M verified)", partNumber: "5934PA-1", serialNumber: "91024", mfgDate: "1994-08", modMarks: "M", condition: "Serviceable (SV)", has8130: true, notes: "Overhauled by Mid-Continent Instruments. Yellow tag attached." },
  { label: "BendixKing KT-76A Transponder", subtitle: "Mode C - Triggers AD 2004-08-16 Check", partNumber: "066-1062-00", serialNumber: "65123", mfgDate: "1989-11", modMarks: "1, 2", condition: "As Removed (AR)", has8130: false, notes: "Requires 24-mo FAR 91.413 recertification bench check" },
  { label: "Garmin GNS 430W WAAS GPS/NAV/COMM", subtitle: "WAAS GPS - Advisory Check", partNumber: "011-00280-10", serialNumber: "97103412", mfgDate: "2008-05", modMarks: "MOD 1-5", condition: "Serviceable (SV)", has8130: true, notes: "Main SW 5.40 / GPS SW 5.0. 14 CFR 91.227 ADS-B compliant" },
  { label: "Narco AT-150 Transponder", subtitle: "Transponder - Triggers AD 94-14-14 Check", partNumber: "AT-150", serialNumber: "31940", mfgDate: "1978-02", modMarks: "", condition: "As Removed (AR)", has8130: false, notes: "Capacitor modification status unknown" },
  { label: "Collins VIR-351 VOR/LOC Nav", subtitle: "Nav Receiver - Standard Screening", partNumber: "VIR-351", serialNumber: "28410", mfgDate: "1981-09", modMarks: "MOD 1", condition: "Serviceable (SV)", has8130: false, notes: "Bench tested OK for localizer tracking" },
];

export const AD_REFERENCE = [
  { title: "United Instruments Altimeter AD 86-05-02", code: "AD 86-05-02 R1", summary: "Applies to 5934PA-1, 5934PD-1, and Cessna C661071 series altimeters. Check for stamped 'M' or punched mod plate indicating diaphragm and barometric gear train modification. Without 'M' stamp, instrument cannot be certified for IFR flight." },
  { title: "BendixKing Transponders AD 2004-08-16", code: "KT 76A / KT 78A", summary: "Addresses cavity oscillator drift causing intermittent reply failures to ATC secondary surveillance radars. Requires bench test verification per FAR 91.413 within preceding 24 calendar months." },
  { title: "Authorized Release Certificate: FAA Form 8130-3", code: "14 CFR Part 43 / Order 8130.21", summary: "Airworthiness Approval Tag issued by certified Part 145 repair stations or manufacturer. Block 11 specifies status (Serviceable, Overhauled, Rebuilt). Dual-release certifications cover both FAA and EASA requirements." },
  { title: "24-Month Pitot-Static & Transponder Tests", code: "14 CFR 91.411 & 91.413", summary: "Altimeter, static pressure system, and automatic altitude reporting encoder must be tested within 24 calendar months for IFR flight. Transponder must be tested per FAR Part 43 Appendix F." },
];
