# Function map

**What it is:** "Avionics Scanner". You photograph an aircraft avionics data plate, Gemini OCRs the plate into structured fields, and a hard-coded rule table screens the part number against a few FAA Airworthiness Directives (ADs). Results are saved to a local history that you can search, filter and export as CSV.

The repo has two implementations of the same app:

| | Android app | Web app |
|---|---|---|
| Code | `app/src/main/java/com/example/` (Kotlin, Jetpack Compose, Room, CameraX, Retrofit) | `public/` (static HTML/CSS/JS, no build step) + `api/scan.js` (Vercel function) |
| OCR | Calls Gemini directly from the phone (key compiled into the APK) | Browser → `/api/scan` → Gemini (key stays on the server) |
| AD rules | `domain/AdComplianceChecker.kt` | `public/js/ad-checker.js` (a port; **keep the two in sync**) |
| History | Room DB `avionics_scanner_db` on the phone | `localStorage` in the browser |
| Tests | `app/src/test/…` (JUnit + Robolectric) | `web-tests/ad-checker.test.mjs` (`node --test`) |

The two `.zip` files at the repo root are snapshots of the Android source. Apart from line endings they're identical to `app/`.

---

# Part 1: Web app

## Data flow

```
index.html ─► js/app.js (UI + state) ──imports──► js/ad-checker.js (checkCompliance, presets, reference)
                 │ photo → canvas resize → base64 JPEG
                 ▼
          POST /api/scan ─► api/scan.js ─(x-goog-api-key header)─► Gemini generateContent
                 │ {data, raw}
                 ▼
          edit/confirm dialog (live AD check) ─► save ─► localStorage "avionics-scanner:parts:v1"
```

## `api/scan.js` (Vercel serverless function, CommonJS)
| Item | What it does |
|---|---|
| `handler(req, res)` (default export) | POST only (405 otherwise). Returns 503 with a setup message if `GEMINI_API_KEY` is unset. Validates the body `{image, mimeType}`: base64 only (400), ≤ 4,000,000 chars (413), JPEG/PNG/WebP only (415). Calls Gemini `v1beta/models/{GEMINI_MODEL}:generateContent` with the key in the `x-goog-api-key` header, JSON response mode, temperature 0.1, 55 s timeout (504 on timeout). Strips ``` fences, parses the JSON (502 on bad JSON or an upstream error) and returns `{data, raw, model}`. |
| `send(res, status, body)` | Sends JSON with `Cache-Control: no-store` |
| `PROMPT` | The Android OCR prompt plus an `isDataPlate` flag and "never guess a part or serial number" |
| `STRING_FIELDS` | Whitelist of returned fields. Strings are trimmed; `has8130` and `isDataPlate` are coerced to booleans. |

Environment: `GEMINI_API_KEY` (required for OCR), `GEMINI_MODEL` (default `gemini-3.5-flash`).
`vercel.json` sets `framework: null`, `outputDirectory: "public"`, and `maxDuration: 60` for this function.

## `public/js/ad-checker.js` (ES module, shared rules)
| Export | What it does |
|---|---|
| `Status` | `ACTION_REQUIRED` / `COMPLIANT` / `ADVISORY` / `UNKNOWN` display strings |
| `parseModMarks(modMarks): Set` | Tokenizes mod marks: drops the word "MOD"/"MODS", splits on whitespace and `, ; / & +`, expands numeric ranges ("1-5" → 1..5, max span 50), strips leading zeros, and keeps letter ranges ("A-D") as one token |
| `checkCompliance(partNumber, serialNumber, modMarks)` | Same first-match rule chain as the Kotlin version (table in Part 2). `serialNumber` is unused. |
| `getDrsSearchUrl(partNumber)` | URL-encoded FAA DRS search link |
| `SAMPLE_PRESETS` | The 6 demo plates |
| `AD_REFERENCE` | The 4 reference cards shown in the AD reference dialog |

## `public/js/app.js` (UI controller, ES module)
| Function / section | What it does |
|---|---|
| `state` | `{parts, query, filter, draftSource, detailId}` |
| `loadParts()` / `persist()` | Read and write `localStorage`, wrapped in try/catch. A failed save shows a toast and rolls back. |
| `newId()` | `crypto.randomUUID()`, with a fallback |
| `showTab(tab)` | Inventory / Scanner tab switching and page title |
| `filteredParts()` | Filter (All / Action Required / Compliant / 8130-3) + case-insensitive search over P/N, S/N, component, raw text, AD id and notes, newest first |
| `formatWhen(ts)` | Time if today, else short date |
| `renderFilters()` / `renderInventory()` | Stats card, ticket list (status stripe, source label, mod and 8130 chips), empty and no-results states, action bar |
| `adBannerHtml(result)` / `setAdBanner(el, result)` | Colour-coded AD result banner |
| `openEditor(values, {source, raw, warning})` | Fills and opens the confirm dialog with a source chip ("Gemini OCR" / "Demo preset" / "Manual entry"), an optional warning and the raw OCR output |
| `readDraft()` | Reads and normalizes the form (P/N and S/N uppercased) |
| `updateLiveAd()` | Re-runs `checkCompliance` on every input |
| edit form `submit` | Requires a P/N. Stores the AD result, recommendation, `source` and timestamp, then switches to Inventory. Enter key submits; backdrop clicks don't close this dialog. |
| `openDetail(id)` | Detail dialog: AD banner, field grid, raw text, DRS link |
| `#detail-delete`, `#clear-all` | Delete one / delete all (with `confirm()`) |
| presets / manual entry / reference | Build the lists and open the dialogs |
| `setBusy`, `showScanError`, `resetViewfinder` | Scanner UI state |
| `loadImage(file)` | Decodes the photo via an object URL; unreadable formats give a clear error |
| `toJpegBase64(img)` | Canvas downscale to a 1280 px longest edge, JPEG q=0.85, raw base64 |
| `handlePhoto(file)` | Preview → resize → `POST /api/scan` → editor. Warns if Gemini says the image isn't a data plate or no P/N was read. Errors are shown inline; no fake fallback data. |
| `csvCell(value)` | RFC-4180 quoting + spreadsheet formula-injection guard (prefixes `'` to `= + @ \t \r` and `-<non-digit>`) |
| `#export-csv` handler | 15-column CSV (adds Source and ISO timestamp), UTF-8 BOM, downloads `Avionics_Scans_YYYY-MM-DD.csv` |
| `toast(message)` | 2.6 s status toast |

All text from OCR or storage goes through `esc()` before it is inserted as HTML.

## `public/index.html`, `public/styles.css`
Markup for the two tab panels and four `<dialog>`s (edit, detail, presets, reference). On phones the dialogs open as bottom sheets. The palette is copied from `ui/theme/Color.kt`. Camera capture uses `<input type="file" accept="image/*" capture="environment">`.

---

# Part 2: Android app

## Architecture / data flow

```
MainActivity ─► AvionicsScreen (Compose UI, 2 tabs) ◄──StateFlows── AvionicsViewModel
                     │ user actions                                   │       │
                     ▼                                                ▼       ▼
     CameraViewfinder ─(Bitmap)─► processCapturedPhoto ─► GeminiDataPlateService ─► Gemini REST API
                                        │ (on failure: captureDataPlate = random preset)
                                        ▼
                                 DraftPartForm ─► liveAdCheck ─► AdComplianceChecker.checkCompliance
                                        │ saveDraftPart
                                        ▼
                                 PartRepository ─► PartDao ─► Room DB "avionics_scanner_db" (table scanned_parts)
```

Paths below are relative to `app/src/main/java/com/example/`.

## Entry point
**`MainActivity.kt`**
| Function | What it does |
|---|---|
| `onCreate` | Enables edge-to-edge and sets the content to `MyApplicationTheme { AvionicsScreen(viewModel) }`. The ViewModel comes from `AvionicsViewModel.provideFactory`. |

## Domain logic
**`domain/AdComplianceChecker.kt`** (singleton `object`)
| Function | What it does |
|---|---|
| `checkCompliance(partNumber, serialNumber, modMarks): AdCheckResult` | Normalizes the P/N (alphanumerics only, uppercase) and runs it through a first-match rule chain. **serialNumber is never used.** |
| `parseModMarks(modMarks): Set<String>` | Tokenizes mod marks so the checks match whole marks, not substrings (same logic as the JS port) |
| `getDrsSearchUrl(partNumber)` | URL-encoded FAA DRS search link. Used by `PartDetailDialog`. |
| `SAMPLE_PRESETS` / `data class PresetPlate` | Six hard-coded demo plates |

Rule chain in `checkCompliance` (identical in `ad-checker.js`):
| Match on P/N | Result |
|---|---|
| empty | UNKNOWN "No P/N Entered" |
| `5934*`, `C661071*` (United Instruments altimeter) | COMPLIANT if the mod tokens include `M`, else ACTION_REQUIRED (AD 86-05-02) |
| `0661062`, `KT76`, `KT78` (KT-76A/78A transponder) | ACTION_REQUIRED (AD 2004-08-16) |
| `AT150`, `AT50` (Narco transponder) | COMPLIANT if the mod tokens include `4`, else ACTION_REQUIRED (AD 94-14-14) |
| `01100280`, `GNS430`, `GNS530` | ADVISORY (ADS-B software level) |
| `01100455`, `GTX330` | ADVISORY (ES software) |
| `VIR351`, `VHF251` | COMPLIANT (standard screening) |
| `KRA10`, `KRA405` | ACTION_REQUIRED (AD 2021-23-12, 5G) |
| `1C388`, `CENTURY` | ACTION_REQUIRED (AD 79-20-05) |
| anything else | UNKNOWN: "search FAA DRS" |

## Data layer
**`data/model/ScannedPart.kt`**
| Item | What it does |
|---|---|
| `@Entity ScannedPart` | Room row: id, P/N, S/N, mfgDate, modMarks, condition, has8130, adStatus/adIdentifier/adDetails, scanTime, timestamp, componentName, rawExtractedText, notes |
| `ScannedPart.formattedDate()` | Formats `timestamp` as `MMM dd, yyyy HH:mm:ss` |
| `enum AdStatusLevel` | ACTION_REQUIRED / COMPLIANT / ADVISORY / UNKNOWN. The display name is what gets stored. |
| `data class AdCheckResult` | status, identifier, details, recommendation |

**`data/local/PartDao.kt`** (Room DAO)
| Function | SQL / behaviour |
|---|---|
| `getAllParts(): Flow` | `SELECT * … ORDER BY timestamp DESC` |
| `getPartById(id)` / `getPartByIdFlow(id)` | Single row (suspend / Flow) |
| `searchParts(query): Flow` | LIKE across 4 columns. *Unused.* |
| `getPartCount(): Flow<Int>` | `COUNT(*)`. *Unused.* |
| `insertPart` / `insertParts` | Insert with REPLACE on conflict |
| `updatePart`, `deletePart` | @Update / @Delete. *Unused.* |
| `deleteById(id)`, `deleteAll()` | Explicit DELETEs |

**`data/local/AppDatabase.kt`**
| Function | What it does |
|---|---|
| `partDao()` | DAO accessor |
| `getDatabase(context)` | Singleton for `avionics_scanner_db` (v2) with `fallbackToDestructiveMigration` |

**`data/repository/PartRepository.kt`**
| Function | What it does |
|---|---|
| `allParts`, `partCount` | DAO Flows |
| `searchParts`, `getPartById`, `getPartByIdFlow`, `insertPart(s)`, `updatePart`, `deletePart`, `deleteById`, `deleteAll` | One-line DAO delegations |
| `seedSampleHistoryIfEmpty()` | Inserts 3 demo scans when the table is empty |

**`data/api/GeminiDataPlateService.kt`**
| Item | What it does |
|---|---|
| DTOs `GenerateContentRequest`, `Content`, `Part`, `InlineData`, `GenerationConfig`, `GenerateContentResponse`, `Candidate` | Moshi models for Gemini `generateContent` |
| `ExtractedPlateData` | Parsed OCR result |
| `API_KEY_HEADER` | `"x-goog-api-key"` |
| `interface GeminiApiService.generateContent(apiKey, request)` | Retrofit `POST v1beta/models/gemini-3.5-flash:generateContent`, key sent as a header |
| `okHttpClient` | HTTP logging at BASIC in debug builds, NONE in release; the key header is redacted |
| `bitmapToBase64(bitmap, 1280, 85)` *(private)* | Downscale and JPEG-encode to Base64 |
| `extractDataPlateFromBitmap(bitmap): Result<ExtractedPlateData>` | Returns a **hard-coded fake altimeter** when the key is missing. Otherwise sends the prompt and image, strips fences and parses with Moshi. |

## Presentation: ViewModel
**`ui/viewmodel/AvionicsViewModel.kt`**

Types: `DraftPartForm`, `InventoryFilter` (ALL / ACTION_REQUIRED / COMPLIANT / TAGGED_8130), `UiEvent` (`ShowToast`, `ShareCsv`).
StateFlows: `isCameraActive`, `isConfirming`, `isSaving`, `isOcrProcessing`, `ocrStatusMessage`, `draftForm`, `searchQuery`, `selectedFilter`, `selectedPartForDetail`, `showAdReference`, `showPresetPicker`, `liveAdCheck`, `filteredInventory`, `inventoryStats`, `eventFlow`.

| Function | What it does |
|---|---|
| `init` | Seeds demo history |
| `setCameraActive`, `setSearchQuery`, `setFilter`, `selectPartForDetail`, `setShowAdReference`, `setShowPresetPicker` | State setters |
| `processCapturedPhoto(bitmap)` | Gemini OCR → draft → confirm dialog. **On failure, loads a random preset** via `captureDataPlate()`. |
| `processSampleAssetPhoto(assetName)` | Runs a bundled asset through OCR |
| `captureDataPlate()` | Random preset with a random serial |
| `openManualEntry()`, `applyPreset(preset)`, `updateDraft(...)`, `cancelCapture()` | Draft management |
| `saveDraftPart()` | AD check → insert `ScannedPart` |
| `deletePart(id)`, `clearAllHistory()` | Deletes |
| `exportCsv(context)` / `buildCsv(parts)` | 16-column CSV → clipboard + share intent |
| `companion provideFactory(application)` | DB → Repository → ViewModel |

## Presentation: Compose UI
**`ui/AvionicsScreen.kt`**: `AvionicsScreen(viewModel)` (root: tabs, event handling, dialogs), `enum AviationTab`, plus private `AviationTopBar`, `AviationSegmentedToggle`, `FlightRouteHeader`, `FlightAnalyticsBlueCard`, `FilterPillRow`, `HeroScannerBanner`, `EmptyStateTicket`.

**`ui/components/`**
| Composable | What it does |
|---|---|
| `CameraViewfinder(...)` | Camera permission, CameraX preview/capture and the laser animation. The local `triggerCameraCapture()` falls back to a bundled asset on error. |
| `PartEditDialog(...)` | Confirm/edit form with a live AD banner. `componentName` isn't editable. |
| `PartDetailDialog(part, onDismiss, onDelete)` | Detail view with a DRS link (private `DetailSection`, `InfoLine`, `ServiceItem`) |
| `AvionicsTicketCard(part, onClick, onDelete)` | Boarding-pass-style list item |
| `PresetPickerDialog`, `AdReferenceSheet` (private `ReferenceCard`), `SafetyDisclaimerCard` | Demo plates, reference sheet, disclaimer |

**`ui/theme/`**: `Color.kt`, `Theme.kt` (`MyApplicationTheme`, dark only), `Type.kt`.

## Tests
| File | Covers |
|---|---|
| `app/src/test/…/ExampleUnitTest.kt` | AD rules incl. mod-mark edge cases, `parseModMarks`, Moshi OCR parsing |
| `app/src/test/…/ExampleRobolectricTest.kt` | In-memory Room: insert/read, ordering and deletion, seeding |
| `app/src/androidTest/…/ExampleInstrumentedTest.kt` | Package-name smoke test |
| `web-tests/ad-checker.test.mjs` | JS rules: mod-mark edge cases, every rule branch, presets, DRS URL |

---

# Known issues

| # | Issue | Status |
|---|---|---|
| 1 | Mod-mark checks used substring matching, so "MOD 1" passed as Mod M and "14" passed as Mod 4 (false "Compliant") | **Fixed** in both apps (`parseModMarks`) |
| 2 | Android: API key sent as the `?key=` query param and logged by `HttpLoggingInterceptor` | **Fixed** (header, redacted, debug-only logging). The key is **still compiled into the APK**; fixing that needs a backend proxy like `api/scan.js`. |
| 3 | Android: no key → fake altimeter OCR result; OCR failure → a random preset loaded as if scanned | Open in Android. The web app shows errors instead and labels every record's source. |
| 4 | Android: the bundled "sample data plate" asset (`images_26f45d1f.jpg`, also in `public/assets/aistudio/`) is actually a screenshot of a jet-charter app, not a data plate | Open. The web app doesn't use it. |
| 5 | `serialNumber` is accepted but never used by `checkCompliance`, although the KT-76A AD mentions affected serial batches | Open (both apps) |
| 6 | Android: raw JSON in `captureDataPlate`, `applyPreset` and `saveDraftPart` is built by string interpolation without escaping | Open. The web app uses `JSON.stringify`. |
| 7 | Android: `exportCsv` reads the Flow by throwing `CancellationException`; `allParts.first()` is simpler | Open |
| 8 | Android: dead DAO/repository methods (`searchParts`, `getPartCount`, `updatePart`, `deletePart`) | Open |
| 9 | Android: `fallbackToDestructiveMigration` wipes history on schema changes | Open |
| 10 | Web: `/api/scan` is public, so anyone with the URL can spend the Gemini quota | Mitigate with a key spend cap or Vercel Deployment Protection (see README) |
| 11 | Web: history lives in one browser's `localStorage` (no sync across devices) | By design; use CSV export |
