// Vercel serverless function: POST /api/scan
// Body: { image: "<base64, no data: prefix>", mimeType: "image/jpeg" }
// Sends the photo to Gemini for data plate OCR. The API key stays on the server
// (Vercel env var GEMINI_API_KEY) and is sent as a header, never in the URL.

const MODEL = process.env.GEMINI_MODEL || "gemini-3.5-flash";
const ALLOWED_TYPES = new Set(["image/jpeg", "image/png", "image/webp"]);
const MAX_BASE64_LENGTH = 4_000_000; // Vercel caps request bodies at ~4.5 MB

const PROMPT = `You are an expert aerospace quality inspector and avionics technician analyzing an image of an aircraft or avionics data plate / identification plate.
Perform precise Optical Character Recognition (OCR) and return ONLY a valid JSON object with the following fields:
- "isDataPlate": true if the image actually shows an avionics/instrument data plate or identification plate, otherwise false.
- "partNumber": The manufacturer part number (P/N, Part No., Model No.). Keep exact dashes, uppercase, and suffixes (e.g., '5934PA-1', '066-1070-00', '13-0004-1').
- "serialNumber": The unit serial number (S/N, Serial No., e.g., '34281', '4910').
- "mfgDate": The date of manufacture in YYYY-MM format or MM/YY as printed on the plate (e.g. '1985-06', '1998-11', '08/94', or empty string if not found).
- "modMarks": Modification status symbols, letters or stamps marked on mod dots/grid (e.g. 'M', '1-3', 'MOD A-D', or empty string if none marked).
- "condition": Inferred or stamped condition ('As Removed (AR)', 'Serviceable (SV)', 'Overhauled (OH)', or 'New (NE)'). Defaults to 'As Removed (AR)'.
- "has8130": true if an FAA Form 8130-3 or EASA Form 1 authorization tag or airworthiness release stamp is visible, otherwise false.
- "componentName": The instrument / avionics name (e.g., 'ALTIMETER', 'VHF NAV/COMM TRANSCEIVER', 'TRANSPONDER', 'DIRECTIONAL GYRO').
- "notes": Any manufacturer name, TSO certification (e.g. TSO-C10b), voltage, or condition remarks visible on the plate.
Use an empty string for any text field you cannot read. Never guess a part or serial number.

Strict output requirement: Return only the JSON object, with no markdown code blocks and no surrounding explanatory text.`;

const STRING_FIELDS = ["partNumber", "serialNumber", "mfgDate", "modMarks", "condition", "componentName", "notes"];

function send(res, status, body) {
  res.status(status).setHeader("Cache-Control", "no-store").json(body);
}

module.exports = async function handler(req, res) {
  if (req.method !== "POST") {
    res.setHeader("Allow", "POST");
    return send(res, 405, { error: "Use POST." });
  }

  const apiKey = process.env.GEMINI_API_KEY;
  if (!apiKey) {
    return send(res, 503, {
      error: "OCR is not configured: set the GEMINI_API_KEY environment variable in Vercel and redeploy. You can still use presets or manual entry.",
    });
  }

  let body = req.body;
  if (typeof body === "string") {
    try {
      body = JSON.parse(body);
    } catch {
      return send(res, 400, { error: "Body must be JSON." });
    }
  }
  const image = body && typeof body.image === "string" ? body.image : "";
  const mimeType = body && typeof body.mimeType === "string" ? body.mimeType : "image/jpeg";
  if (!image || !/^[A-Za-z0-9+/=]+$/.test(image)) {
    return send(res, 400, { error: "Missing or invalid base64 image." });
  }
  if (image.length > MAX_BASE64_LENGTH) {
    return send(res, 413, { error: "Image too large. Resize to 1280px before uploading." });
  }
  if (!ALLOWED_TYPES.has(mimeType)) {
    return send(res, 415, { error: "Only JPEG, PNG or WebP images are accepted." });
  }

  let geminiRes;
  try {
    geminiRes = await fetch(
      `https://generativelanguage.googleapis.com/v1beta/models/${encodeURIComponent(MODEL)}:generateContent`,
      {
        method: "POST",
        headers: { "Content-Type": "application/json", "x-goog-api-key": apiKey },
        body: JSON.stringify({
          contents: [{ parts: [{ text: PROMPT }, { inlineData: { mimeType, data: image } }] }],
          generationConfig: { responseMimeType: "application/json", temperature: 0.1 },
        }),
        signal: AbortSignal.timeout(55_000),
      }
    );
  } catch (err) {
    console.error("Gemini request failed:", err && err.name);
    return send(res, 504, { error: "Could not reach Gemini (timeout or network error). Try again." });
  }

  if (!geminiRes.ok) {
    let detail = "";
    try {
      detail = (await geminiRes.json())?.error?.message || "";
    } catch {}
    console.error("Gemini error", geminiRes.status, detail);
    return send(res, 502, { error: `Gemini returned ${geminiRes.status}${detail ? `: ${detail}` : ""}` });
  }

  const payload = await geminiRes.json();
  const text = payload?.candidates?.[0]?.content?.parts?.[0]?.text;
  if (!text) {
    return send(res, 502, { error: "Gemini returned an empty response (the image may have been blocked)." });
  }

  const raw = text.trim().replace(/^```(?:json)?\s*/i, "").replace(/\s*```$/, "").trim();
  let parsed;
  try {
    parsed = JSON.parse(raw);
  } catch {
    return send(res, 502, { error: "Gemini's reply was not valid JSON.", raw });
  }

  // Only pass through known fields with the right types.
  const data = { isDataPlate: parsed.isDataPlate !== false, has8130: parsed.has8130 === true };
  for (const field of STRING_FIELDS) {
    data[field] = typeof parsed[field] === "string" ? parsed[field].trim() : "";
  }
  return send(res, 200, { data, raw, model: MODEL });
};
