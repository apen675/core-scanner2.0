// Run with: node --test web-tests/ad-checker.test.mjs
import { test } from "node:test";
import assert from "node:assert/strict";
import { checkCompliance, parseModMarks, getDrsSearchUrl, Status, SAMPLE_PRESETS } from "../public/js/ad-checker.js";

test("altimeter AD 86-05-02: Mod M variants are compliant", () => {
  for (const marks of ["M", "m", "MOD M", "MODM", "Mod. M", "1, M", "M/2"]) {
    assert.equal(checkCompliance("5934PA-1", "48102", marks).status, Status.COMPLIANT, marks);
  }
});

test("altimeter AD 86-05-02: other marks containing the letter M need action", () => {
  for (const marks of ["", "MOD 1", "MOD A-D", "MODS 1-3", "MM", "AM"]) {
    const r = checkCompliance("5934PA-1", "48102", marks);
    assert.equal(r.status, Status.ACTION_REQUIRED, marks);
    assert.match(r.identifier, /86-05-02/);
  }
});

test("Narco AT-150 AD 94-14-14: Mod 4 variants are compliant", () => {
  for (const marks of ["4", "MOD 4", "MOD4", "1, 2, 4", "MOD 1-5", "04"]) {
    assert.equal(checkCompliance("AT-150", "31940", marks).status, Status.COMPLIANT, marks);
  }
});

test("Narco AT-150 AD 94-14-14: numbers merely containing 4 need action", () => {
  for (const marks of ["", "14", "24", "MOD 1-3", "40", "1-3, 40"]) {
    assert.equal(checkCompliance("AT-150", "31940", marks).status, Status.ACTION_REQUIRED, marks);
  }
});

test("other rules", () => {
  assert.equal(checkCompliance("066-1062-00", "", "").status, Status.ACTION_REQUIRED);
  assert.equal(checkCompliance("011-00280-10", "", "").status, Status.ADVISORY);
  assert.equal(checkCompliance("GTX 330", "", "").status, Status.ADVISORY);
  assert.equal(checkCompliance("VIR-351", "", "").status, Status.COMPLIANT);
  assert.equal(checkCompliance("KRA-10A", "", "").status, Status.ACTION_REQUIRED);
  assert.equal(checkCompliance("1C388", "", "").status, Status.ACTION_REQUIRED);
  assert.equal(checkCompliance("XYZ-1", "", "").status, Status.UNKNOWN);
  assert.equal(checkCompliance("  ", "", "").identifier, "No P/N Entered");
});

test("parseModMarks", () => {
  assert.deepEqual(parseModMarks("MOD M"), new Set(["M"]));
  assert.deepEqual(parseModMarks("1, 2"), new Set(["1", "2"]));
  assert.deepEqual(parseModMarks("MOD 1 - 5"), new Set(["1", "2", "3", "4", "5"]));
  assert.deepEqual(parseModMarks("MOD A-D"), new Set(["A-D"]));
  assert.deepEqual(parseModMarks("  "), new Set());
  assert.deepEqual(parseModMarks(null), new Set());
});

test("presets produce the outcome their subtitle promises", () => {
  const expected = [Status.ACTION_REQUIRED, Status.COMPLIANT, Status.ACTION_REQUIRED, Status.ADVISORY, Status.ACTION_REQUIRED, Status.COMPLIANT];
  SAMPLE_PRESETS.forEach((p, i) => {
    assert.equal(checkCompliance(p.partNumber, p.serialNumber, p.modMarks).status, expected[i], p.label);
  });
});

test("DRS search URL is encoded", () => {
  assert.equal(getDrsSearchUrl("5934PA-1 &x"), "https://drs.faa.gov/browse/excelExternalWindow/DRS_RESULTS?query=5934PA-1%20%26x");
});
