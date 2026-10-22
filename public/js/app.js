import { checkCompliance, getDrsSearchUrl, Status, SAMPLE_PRESETS, AD_REFERENCE } from "./ad-checker.js";

const STORAGE_KEY = "avionics-scanner:parts:v1";
const MAX_IMAGE_DIMENSION = 1280;

const SOURCE_LABELS = { ocr: "Gemini OCR", preset: "Demo preset", manual: "Manual entry" };
const FILTERS = [
  { id: "all", label: "All", test: () => true },
  { id: "action", label: "Action Required", test: (p) => p.adStatus === Status.ACTION_REQUIRED },
  { id: "compliant", label: "Compliant", test: (p) => p.adStatus === Status.COMPLIANT },
  { id: "8130", label: "8130-3 Present", test: (p) => p.has8130 },
];
const STATUS_CLASS = {
  [Status.ACTION_REQUIRED]: "s-action",
  [Status.COMPLIANT]: "s-ok",
  [Status.ADVISORY]: "s-advisory",
  [Status.UNKNOWN]: "s-unknown",
};

const $ = (sel) => document.querySelector(sel);
const esc = (value) =>
  String(value ?? "").replace(/[&<>"']/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" })[c]);

const state = {
  parts: loadParts(),
  query: "",
  filter: "all",
  draftSource: "manual",
  detailId: null,
};

// ---------- Storage (browser-local, like the app's Room database) ----------

function loadParts() {
  try {
    const parsed = JSON.parse(localStorage.getItem(STORAGE_KEY) || "[]");
    return Array.isArray(parsed) ? parsed : [];
  } catch {
    return [];
  }
}

function persist() {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(state.parts));
    return true;
  } catch {
    toast("Couldn't save: browser storage is unavailable or full.");
    return false;
  }
}

const newId = () =>
  globalThis.crypto?.randomUUID?.() ?? `${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 10)}`;

// ---------- Tabs ----------

function showTab(tab) {
  for (const btn of document.querySelectorAll("[data-tab]")) {
    const active = btn.dataset.tab === tab;
    btn.setAttribute("aria-selected", String(active));
    $(`#panel-${btn.dataset.tab}`).hidden = !active;
  }
  $("#page-title").textContent = tab === "inventory" ? "Avionics History" : "Data Plate Scanner";
}

document.querySelectorAll("[data-tab]").forEach((btn) => btn.addEventListener("click", () => showTab(btn.dataset.tab)));
document.querySelectorAll("[data-goto]").forEach((btn) => btn.addEventListener("click", () => showTab(btn.dataset.goto)));

// ---------- Inventory ----------

function filteredParts() {
  const q = state.query.trim().toLowerCase();
  const filter = FILTERS.find((f) => f.id === state.filter) ?? FILTERS[0];
  return state.parts
    .filter((p) => filter.test(p))
    .filter((p) =>
      !q ||
      [p.partNumber, p.serialNumber, p.componentName, p.rawExtractedText, p.adIdentifier, p.notes]
        .some((v) => String(v ?? "").toLowerCase().includes(q))
    )
    .sort((a, b) => b.timestamp - a.timestamp);
}

function formatWhen(ts) {
  const d = new Date(ts);
  const sameDay = d.toDateString() === new Date().toDateString();
  return sameDay
    ? d.toLocaleTimeString(undefined, { hour: "2-digit", minute: "2-digit" })
    : d.toLocaleDateString(undefined, { month: "short", day: "numeric", year: "numeric" });
}

function renderFilters() {
  $("#filters").innerHTML = FILTERS.map(
    (f) => `<button type="button" data-filter="${f.id}" aria-pressed="${f.id === state.filter}">${esc(f.label)}</button>`
  ).join("");
}

function renderInventory() {
  const parts = state.parts;
  $("#stat-total").textContent = parts.length;
  $("#stat-alerts").textContent = parts.filter((p) => p.adStatus === Status.ACTION_REQUIRED).length;
  $("#stat-8130").textContent = parts.filter((p) => p.has8130).length;

  const list = filteredParts();
  $("#empty-state").hidden = parts.length > 0;
  $("#no-results").hidden = parts.length === 0 || list.length > 0;
  $("#inventory-actions").hidden = parts.length === 0;

  $("#ticket-list").innerHTML = list
    .map((p) => {
      const cls = STATUS_CLASS[p.adStatus] ?? "s-unknown";
      return `<li>
        <button type="button" class="ticket ${cls}" data-id="${esc(p.id)}">
          <div class="ticket-main">
            <div class="ticket-top">
              <span class="ticket-name">${esc(p.componentName || "Unidentified component")}</span>
              <span class="badge ${cls}">${esc(p.adStatus)}</span>
            </div>
            <div class="ticket-pn">${esc(p.partNumber)}</div>
            <div class="ticket-meta">
              <span>S/N ${esc(p.serialNumber || "N/A")}</span>
              <span>${esc(formatWhen(p.timestamp))}</span>
              <span>${esc(SOURCE_LABELS[p.source] ?? "Unknown source")}</span>
            </div>
          </div>
          <div class="ticket-perf" aria-hidden="true"></div>
          <div class="ticket-foot">
            <span class="ticket-ad">${esc(p.adIdentifier)}</span>
            <span class="chips">
              ${p.modMarks ? `<span class="chip">${/^mod/i.test(p.modMarks) ? "" : "MOD "}${esc(p.modMarks)}</span>` : ""}
              ${p.has8130 ? `<span class="chip">8130-3</span>` : ""}
            </span>
          </div>
        </button>
      </li>`;
    })
    .join("");
}

$("#search").addEventListener("input", (e) => {
  state.query = e.target.value;
  renderInventory();
});
$("#filters").addEventListener("click", (e) => {
  const btn = e.target.closest("[data-filter]");
  if (!btn) return;
  state.filter = btn.dataset.filter;
  renderFilters();
  renderInventory();
});
$("#ticket-list").addEventListener("click", (e) => {
  const card = e.target.closest("[data-id]");
  if (card) openDetail(card.dataset.id);
});
$("#clear-all").addEventListener("click", () => {
  if (!confirm(`Delete all ${state.parts.length} saved scans from this browser? This can't be undone.`)) return;
  state.parts = [];
  persist();
  renderInventory();
  toast("History cleared");
});

// ---------- AD banner ----------

function adBannerHtml(result) {
  const cls = STATUS_CLASS[result.status] ?? "s-unknown";
  return {
    cls,
    html: `<div class="ad-head">
        <span class="ad-id">${esc(result.identifier)}</span>
        <span class="badge ${cls}">${esc(result.status)}</span>
      </div>
      <p class="ad-details">${esc(result.details)}</p>
      ${result.recommendation ? `<p class="ad-rec">${esc(result.recommendation)}</p>` : ""}`,
  };
}

function setAdBanner(el, result) {
  const { cls, html } = adBannerHtml(result);
  el.className = `ad-banner ${cls}`;
  el.innerHTML = html;
}

// ---------- Edit / confirm dialog ----------

const editDialog = $("#edit-dialog");
const editForm = $("#edit-form");

function openEditor(values, { source, raw = "", warning = "" }) {
  state.draftSource = source;
  editForm.reset();
  for (const [name, value] of Object.entries(values)) {
    const input = editForm.elements.namedItem(name);
    if (!input) continue;
    if (input.type === "checkbox") input.checked = Boolean(value);
    else if (input.tagName === "SELECT") input.value = [...input.options].some((o) => o.value === value) ? value : input.options[0].value;
    else input.value = value ?? "";
  }
  $("#edit-source").textContent = `Source: ${SOURCE_LABELS[source]}`;
  $("#edit-warning").textContent = warning;
  $("#edit-warning").hidden = !warning;
  $("#edit-raw").textContent = raw;
  $("#edit-raw-wrap").hidden = !raw;
  $("#edit-error").hidden = true;
  editForm.elements.partNumber.removeAttribute("aria-invalid");
  updateLiveAd();
  editDialog.showModal();
}

function readDraft() {
  const f = editForm.elements;
  return {
    partNumber: f.partNumber.value.trim().toUpperCase(),
    serialNumber: f.serialNumber.value.trim().toUpperCase(),
    modMarks: f.modMarks.value.trim(),
    componentName: f.componentName.value.trim(),
    mfgDate: f.mfgDate.value.trim(),
    condition: f.condition.value,
    has8130: f.has8130.checked,
    notes: f.notes.value.trim(),
  };
}

function updateLiveAd() {
  const d = readDraft();
  setAdBanner($("#live-ad"), checkCompliance(d.partNumber, d.serialNumber, d.modMarks));
}

editForm.addEventListener("input", updateLiveAd);
editForm.addEventListener("submit", (e) => {
  const draft = readDraft();
  if (!draft.partNumber) {
    e.preventDefault();
    editForm.elements.partNumber.setAttribute("aria-invalid", "true");
    $("#edit-error").textContent = "Part number is required.";
    $("#edit-error").hidden = false;
    editForm.elements.partNumber.focus();
    return;
  }
  const ad = checkCompliance(draft.partNumber, draft.serialNumber, draft.modMarks);
  const part = {
    id: newId(),
    ...draft,
    rawExtractedText: $("#edit-raw").textContent || JSON.stringify(draft, null, 2),
    adStatus: ad.status,
    adIdentifier: ad.identifier,
    adDetails: ad.details,
    adRecommendation: ad.recommendation,
    source: state.draftSource,
    timestamp: Date.now(),
  };
  state.parts.unshift(part);
  if (!persist()) {
    state.parts.shift();
    e.preventDefault();
    return;
  }
  renderInventory();
  showTab("inventory");
  resetViewfinder();
  toast(`Saved ${part.partNumber}`);
});

// ---------- Detail dialog ----------

const detailDialog = $("#detail-dialog");

function openDetail(id) {
  const p = state.parts.find((x) => x.id === id);
  if (!p) return;
  state.detailId = id;
  $("#detail-title").textContent = p.partNumber;
  $("#detail-sub").textContent = p.componentName || "Unidentified component";
  setAdBanner($("#detail-ad"), {
    status: p.adStatus,
    identifier: p.adIdentifier,
    details: p.adDetails,
    recommendation: p.adRecommendation,
  });
  const rows = [
    ["Serial number", p.serialNumber || "N/A"],
    ["Mod marks", p.modMarks || "None"],
    ["Mfg date", p.mfgDate || "Unknown"],
    ["Condition", p.condition],
    ["8130-3 tag", p.has8130 ? "Present" : "Not present"],
    ["Source", SOURCE_LABELS[p.source] ?? "Unknown"],
    ["Scanned", new Date(p.timestamp).toLocaleString(), true],
    ...(p.notes ? [["Notes", p.notes, true]] : []),
  ];
  $("#detail-grid").innerHTML = rows
    .map(([k, v, wide]) => `<div class="${wide ? "wide" : ""}"><dt>${esc(k)}</dt><dd>${esc(v)}</dd></div>`)
    .join("");
  $("#detail-raw").textContent = p.rawExtractedText || "";
  $("#detail-raw-wrap").hidden = !p.rawExtractedText;
  $("#detail-drs").href = getDrsSearchUrl(p.partNumber);
  detailDialog.showModal();
}

$("#detail-delete").addEventListener("click", () => {
  const p = state.parts.find((x) => x.id === state.detailId);
  if (!p || !confirm(`Delete ${p.partNumber} from history?`)) return;
  state.parts = state.parts.filter((x) => x.id !== p.id);
  persist();
  detailDialog.close();
  renderInventory();
  toast("Part removed");
});

// ---------- Presets, manual entry, reference ----------

$("#preset-list").innerHTML = SAMPLE_PRESETS.map(
  (p, i) => `<li><button type="button" data-preset="${i}">
      <span class="preset-label">${esc(p.label)}</span>
      <span class="preset-sub">${esc(p.subtitle)}</span>
      <span class="preset-pn">P/N ${esc(p.partNumber)} · S/N ${esc(p.serialNumber)}</span>
    </button></li>`
).join("");

$("#preset-list").addEventListener("click", (e) => {
  const btn = e.target.closest("[data-preset]");
  if (!btn) return;
  const p = SAMPLE_PRESETS[Number(btn.dataset.preset)];
  $("#preset-dialog").close();
  openEditor({ ...p, componentName: p.label.split(" (")[0] }, { source: "preset" });
});
$("#open-presets").addEventListener("click", () => $("#preset-dialog").showModal());
$("#manual-entry").addEventListener("click", () => openEditor({}, { source: "manual" }));

$("#reference-list").innerHTML = AD_REFERENCE.map(
  (r) => `<li><p class="ref-code">${esc(r.code)}</p><p class="ref-title">${esc(r.title)}</p><p class="ref-summary">${esc(r.summary)}</p></li>`
).join("");
$("#open-reference").addEventListener("click", () => $("#reference-dialog").showModal());

// Close buttons for all dialogs; backdrop clicks close everything except the
// edit form, where a stray tap would throw away the scan.
for (const dialog of document.querySelectorAll("dialog")) {
  dialog.addEventListener("click", (e) => {
    if (e.target === dialog && dialog !== editDialog) dialog.close();
    if (e.target.closest("[data-close]")) dialog.close();
  });
}

// ---------- Photo capture + OCR ----------

function setBusy(busy, text = "Reading plate with Gemini…") {
  $("#vf-busy").hidden = !busy;
  $("#vf-busy-text").textContent = text;
  for (const el of document.querySelectorAll("#camera-input, #upload-input, #open-presets, #manual-entry")) el.disabled = busy;
}

function showScanError(message) {
  $("#scan-error").textContent = message;
  $("#scan-error").hidden = !message;
}

function resetViewfinder() {
  const img = $("#preview");
  if (img.src.startsWith("blob:")) URL.revokeObjectURL(img.src);
  img.removeAttribute("src");
  img.hidden = true;
  $("#vf-hint").hidden = false;
}

function loadImage(file) {
  return new Promise((resolve, reject) => {
    const url = URL.createObjectURL(file);
    const img = new Image();
    img.onload = () => resolve({ img, url });
    img.onerror = () => {
      URL.revokeObjectURL(url);
      reject(new Error("This browser can't read that image format. Try a JPEG or PNG."));
    };
    img.src = url;
  });
}

/** Downscale to MAX_IMAGE_DIMENSION and re-encode as JPEG, returning raw base64. */
function toJpegBase64(img) {
  const scale = Math.min(1, MAX_IMAGE_DIMENSION / Math.max(img.naturalWidth, img.naturalHeight));
  const canvas = document.createElement("canvas");
  canvas.width = Math.max(1, Math.round(img.naturalWidth * scale));
  canvas.height = Math.max(1, Math.round(img.naturalHeight * scale));
  canvas.getContext("2d").drawImage(img, 0, 0, canvas.width, canvas.height);
  return canvas.toDataURL("image/jpeg", 0.85).split(",")[1];
}

async function handlePhoto(file) {
  if (!file) return;
  showScanError("");
  resetViewfinder();
  setBusy(true, "Preparing photo…");
  try {
    const { img, url } = await loadImage(file);
    const preview = $("#preview");
    preview.src = url;
    preview.hidden = false;
    $("#vf-hint").hidden = true;

    const image = toJpegBase64(img);
    setBusy(true, "Reading plate with Gemini…");
    const res = await fetch("/api/scan", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ image, mimeType: "image/jpeg" }),
    });
    let body = {};
    try {
      body = await res.json();
    } catch {}
    if (!res.ok) throw new Error(body.error || `Scan failed (HTTP ${res.status}).`);

    const d = body.data;
    const warnings = [];
    if (!d.isDataPlate) warnings.push("Gemini says this photo may not show a data plate. Check every field.");
    if (!d.partNumber) warnings.push("No part number was read. Enter it from the plate.");
    openEditor(d, { source: "ocr", raw: body.raw, warning: warnings.join(" ") });
  } catch (err) {
    showScanError(`${err.message} You can retake the photo, or use manual entry.`);
  } finally {
    setBusy(false);
  }
}

for (const id of ["#camera-input", "#upload-input"]) {
  $(id).addEventListener("change", (e) => {
    const file = e.target.files?.[0];
    e.target.value = ""; // allow re-selecting the same file
    handlePhoto(file);
  });
}

// ---------- CSV export ----------

function csvCell(value) {
  let s = String(value ?? "");
  // Stop spreadsheet apps from executing OCR text as a formula.
  if (/^[=+@\t\r]/.test(s) || /^-[^\d]/.test(s)) s = `'${s}`;
  return /[",\n\r]/.test(s) ? `"${s.replace(/"/g, '""')}"` : s;
}

$("#export-csv").addEventListener("click", () => {
  const headers = ["Record ID", "Part Number", "Serial Number", "Component Name", "Mfg Date", "Mod Marks", "Condition", "8130-3 Tag", "AD Status", "AD Identifier", "AD Details", "Source", "Scanned At (ISO)", "Raw Extracted Text", "Notes"];
  const rows = [...state.parts]
    .sort((a, b) => b.timestamp - a.timestamp)
    .map((p) =>
      [p.id, p.partNumber, p.serialNumber, p.componentName, p.mfgDate, p.modMarks, p.condition, p.has8130 ? "Yes" : "No", p.adStatus, p.adIdentifier, p.adDetails, SOURCE_LABELS[p.source] ?? "", new Date(p.timestamp).toISOString(), p.rawExtractedText, p.notes]
        .map(csvCell)
        .join(",")
    );
  const csv = [headers.join(","), ...rows].join("\r\n");
  const blob = new Blob(["﻿" + csv], { type: "text/csv;charset=utf-8" });
  const a = document.createElement("a");
  a.href = URL.createObjectURL(blob);
  a.download = `Avionics_Scans_${new Date().toISOString().slice(0, 10)}.csv`;
  document.body.append(a);
  a.click();
  a.remove();
  setTimeout(() => URL.revokeObjectURL(a.href), 1000);
  toast(`Exported ${rows.length} parts`);
});

// ---------- Toast ----------

let toastTimer;
function toast(message) {
  const el = $("#toast");
  el.textContent = message;
  el.hidden = false;
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => (el.hidden = true), 2600);
}

// ---------- Init ----------

renderFilters();
renderInventory();
