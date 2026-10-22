# Avionics Scanner

Aviation data plate scanner and Airworthiness Directive (AD) screening aid. Photograph an instrument's data plate, Gemini reads the part number, serial number and mod marks, and the app screens the part against known FAA ADs.

This repo contains two versions of the app:

| Version | Where | Runs on |
|---|---|---|
| Android app (Kotlin / Jetpack Compose) | `app/`, Gradle files | Android phones (build with Android Studio) |
| Web app (static HTML/JS + one serverless function) | `public/`, `api/scan.js`, `vercel.json` | Any browser; hosted on Vercel |

**Screening aid only.** Neither version replaces an official FAA AD compliance check. A certified A&P/IA must verify the records in the FAA DRS and the maintenance logs.

## Deploying the web app on Vercel

1. Push this repo to GitHub, including `public/`, `api/` and `vercel.json`.
2. In Vercel, import the repo (or open the existing project). Under **Settings → Build and Deployment**:
   - **Root Directory**: leave empty (the repo root).
   - **Framework Preset**: `Other`. `vercel.json` already sets this and serves the `public/` folder; no build command is needed.
3. Under **Settings → Environment Variables**, add:
   - `GEMINI_API_KEY`: a Gemini API key from Google AI Studio (Production and Preview).
   - `GEMINI_MODEL` (optional): defaults to `gemini-3.5-flash`.
4. **Redeploy** (Deployments → ⋯ → Redeploy). Environment variable changes only apply to new deployments.

Open the site. Without `GEMINI_API_KEY` the app still works with demo plates and manual entry, and photo scans show a "not configured" message.

### Things to know
- **The API key never reaches the browser.** The page posts the photo to `/api/scan`, which calls Gemini server-side with the key in a request header.
- **Anyone with the URL can use your Gemini quota.** If the site is public, set a spend cap on the key in Google AI Studio, or turn on Vercel **Deployment Protection** to require a login or password.
- **Scan history is stored in the browser** (localStorage), per device, the same way the Android app keeps it on the phone. Use **Export CSV** to keep a copy.
- Photos are downscaled to 1280 px JPEG in the browser before upload, so they stay well under Vercel's 4.5 MB request limit.

## Web app layout

| File | Purpose |
|---|---|
| `public/index.html`, `public/styles.css` | Page and styling (mirrors the Android app's theme) |
| `public/js/app.js` | UI: scanner, edit/confirm form with live AD check, history, filters, CSV export |
| `public/js/ad-checker.js` | AD screening rules, a port of `AdComplianceChecker.kt`. **Keep the two in sync.** |
| `api/scan.js` | Vercel function that sends the photo to Gemini and returns the parsed plate fields |
| `web-tests/ad-checker.test.mjs` | Rule tests: `node --test web-tests/ad-checker.test.mjs` |

## Android app

Open the repo in Android Studio. Put `GEMINI_API_KEY=...` in a `.env` file at the repo root (it's git-ignored), then run the `app` configuration. Run the unit tests from Android Studio. The repo has no Gradle wrapper script (`gradlew`), so Android Studio's bundled Gradle is the easiest route.
