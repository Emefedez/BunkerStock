<p align="center">
  <a href="https://emefedez.github.io/BunkerStock/"><img src="docs/images/header.svg" alt="BunkerStock" height="72"></a>
</p>

<p align="center">
  <a href="https://github.com/Emefedez/BunkerStock/stargazers"><img src="https://img.shields.io/github/stars/Emefedez/BunkerStock?style=flat-square&logo=github" alt="GitHub Stars"></a>
  <a href="https://github.com/Emefedez/BunkerStock/releases"><img src="https://img.shields.io/github/v/release/Emefedez/BunkerStock?include_prereleases&style=flat-square&color=22c55e" alt="Latest Release"></a>
  <a href="https://github.com/Emefedez/BunkerStock/releases/latest"><img src="https://img.shields.io/badge/Android-Native%20APK-3ddc84?style=flat-square&logo=android&logoColor=white" alt="Android Native APK"></a>
  <a href="https://emefedez.github.io/BunkerStock/"><img src="https://img.shields.io/badge/Web%20%2F%20iOS%20%2F%20iPadOS-Wasm%20PWA-245dc4?style=flat-square&logo=safari&logoColor=white" alt="Web and iOS/iPadOS PWA"></a>
  <a href="https://github.com/Emefedez/BunkerStock/actions/workflows/build-apk.yml"><img src="https://img.shields.io/github/actions/workflow/status/Emefedez/BunkerStock/build-apk.yml?style=flat-square&logo=githubactions&logoColor=white&label=Android%20CI" alt="Android CI"></a>
  <a href="https://github.com/Emefedez/BunkerStock/actions/workflows/deploy-web.yml"><img src="https://img.shields.io/github/actions/workflow/status/Emefedez/BunkerStock/deploy-web.yml?style=flat-square&logo=githubactions&logoColor=white&label=Web%20CI" alt="Web CI"></a>
</p>

<p align="center">
  <code>EAN-13 / EAN-8 / UPC</code> · <code>QR / GS1-128</code> · <code>Nutri-Score</code> · <code>Yuka Score</code> · <code>AES-256-GCM Family Sync</code> · <code>A4 QR PDF</code> · <code>JSON / CSV</code> · <code>Offline-First</code>
</p>

# BunkerStock

**BunkerStock** is a native-first **Android** app (Kotlin + Jetpack Compose + Material 3) with a supporting **Compose Multiplatform Web / iOS Safari PWA (`wasmJs`)** build for offline-first household and emergency inventory management, expiration tracking, nutritional health scoring, and zero-account end-to-end encrypted family sync, styled with a machined tactical concrete & Geiger-phosphor aesthetic.

> **Try the Web / iOS Safari PWA:** **[emefedez.github.io/BunkerStock](https://emefedez.github.io/BunkerStock/)** (also accessible at **[emefedez.github.io/bunkerstock](https://emefedez.github.io/bunkerstock)**).
>
> **Android-First Architecture:** Android remains the primary target and keeps 100% native Google Jetpack Compose, CameraX + bundled offline ML Kit, DataStore, WorkManager, SAF, PrintHelper, and zero extra runtime/APK overhead (`-PandroidOnly=true` skips all web tooling). The supporting Web/iOS PWA (`-PwebOnly=true`) shares the exact same Compose UI and syncs end-to-end with Android devices.

### Context

I had a bunch of code from when I was learning about managing more complex API calls and fetching the results with proper separation, since my dad asked for help with managing our food at home, I tried to help the best I could!!

---

## Screenshots

| First-Time Language Setup | Warehouse Dashboard | Multi-Select Checkbox Filter |
| :---: | :---: | :---: |
| ![First-Time Setup](docs/screenshots/01_first_setup.png) | ![Warehouse Dashboard](docs/screenshots/02_warehouse_dashboard.png) | ![Checkbox Filter](docs/screenshots/03_filter_dropdown.png) |

| Product Detail & BioShock Yuka Gauge | Multi-Batch Interactive Calendar | Batch QR · A4 PDF Export |
| :---: | :---: | :---: |
| ![Product Detail](docs/screenshots/04_product_detail.png) | ![Interactive Calendar](docs/screenshots/05_product_detail_calendar.png) | ![Batch QR A4 PDF](docs/screenshots/06_batch_qr_pdf.png) |

| Settings & Autonomy | E2E Encrypted Family Sync |
| :---: | :---: |
| ![Settings](docs/screenshots/07_settings.png) | ![Family Sync](docs/screenshots/08_family_sync.png) |

---

## Key Features

### Tactical Dashboard & Rotation Alerts
- **Auto-Scrolling Hazard Marquee**: Single-row compact rotation alert strip (`Expired` + `Critical` items) with one-tap *"Review Affected"* filter.
- **5 Actionable KPI Cards**: **References**, **Expired**, **Critical (customizable days)**, **Water Reserve** (auto-calculated liters + autonomy days based on people & daily liters), and **Food Units**.
- **Warehouse Freshness Bar**: Live segmented health indicator (`% Valid`) reacting dynamically to custom expiry thresholds.
- **Compact Single-Row Search & Multi-Filter**: Unified `40 dp` toolbar with instant search, a **checkbox dropdown filter** (multi-select categories + expiry statuses), and sorting.
- **Compact Inventory Rows**: Product photo thumbnail (auto-resolved from Open Food Facts / web or custom local image), quick `- / +` stock steppers, batch indicator, and stacked **Nutri-Score + Yuka health score** column on the right.

### Full-Screen Product Detail & Multi-Batch Expiry Calendar
- Tap any product card to smoothly **zoom upward from the product icon** into a dedicated full-screen page featuring:
  - **Zoomable HD Hero Photo** (with loading spinner over the generic icon while fetching, tap to open a full-screen pinch-to-zoom viewer, and an *"Added by <user>"* badge on family-synced items).
  - **Full-Color Compact Expiry Tile**: Tinted directly with status color showing concise countdown (*"Hace 6 días" / "In 14 days"*) and date underneath, matched in height to the Category & Placement tile beside it.
  - **Nutri-Score (A–E)** beside an **animated BioShock-inspired semicircular Yuka health gauge** with a custom vector carrot icon that smoothly fills from `0` to the score.
  - **Concrete Nutrition Facts (*Per 100 g*)**: 8-metric grid (Calories with kJ→kcal fallback, Protein, Carbs, Sugars, Fat, Saturates, Fibre, Salt) plus an expandable animated `0–100 g` bar chart (auto-enriched via Open Food Facts when missing).
  - **Quick Inline Controls**: Category icon dropdown (including **Other `?`**), quantity stepper, and *"No expiration date"* toggle.
  - **Multiple Numbered Batches (`1. 03/12/2026`, `2. 22/02/2027`)**: Track separate purchases of the same product with different expiration dates and quantities; pick which batch(es) appear on the calendar.
  - **Interactive Expiry Calendar**: Visual approach trajectory shading the days between **Today** and your batch expiration date(s) — tap any day to update the active batch's date directly.
  - **Integrated QR Label, Placement & Notes**: Print or share PNG labels and edit bunker location/notes inline.

### Zero-Scroll Optical Scanner, YsiFoods Product Search & Shopping Haul Organizer (`🛒`)
- **Native Android Scanner (CameraX + Bundled Offline ML Kit)** & **Web/iOS Safari Scanner (`getUserMedia` + native `BarcodeDetector` with `zxing-wasm` fallback + iPhone Pro `2x` macro zoom toggle)**: Supports `QR`, `EAN-13`, `EAN-8`, `UPC-A`, `UPC-E`, `Code-128`, `DataMatrix`, and `GS1-128` `(17)YYMMDD` / `(10)LOT` extraction with 2-frame consensus debouncing.
- **YsiFoods-Style Live Product Name Search**: Type any product name directly into the scanner bottom bar to search both your local stock and Open Food Facts live with Nutri-Score, Yuka score, and kcal/100g previews.
- **Shopping Mode + Bulk Haul Organizer**: Scan or search an entire grocery haul back-to-back (serialized mutations guarantee calories & macros are saved on every item), then tap **Organize haul** to multi-select groups of items and apply **Location** (*Fridge / Freezer / Pantry / Bunker / custom*), **Expiry presets or exact calendar dates**, and **Categories** in one tap.
- **Fixed-Position Viewfinder Controls**: Scan QR from gallery image, flashlight toggle, and front/back camera switcher.
- **Non-Blocking Detection Popup**: Camera stays live while showing Open Food Facts lookup, automatic shelf-life estimation, and nutritional scoring.

### Batch QR Labels to Multi-Page A4 PDF
- Pick any subset (or all) of your inventory and export a print-ready **A4 PDF** (`24 scannable ~30 mm QR labels per sheet` with dashed cut guides, auto-spilling onto additional pages when exceeded).
- Auto-generates persistent `BNK-XXXXXX` codes for unlabeled items and supports both **Save PDF** (SAF on Android / A4 Print Sheet on Web/iOS) and **Share / Print PDF**.

### Zero-Account Family Group Sync (E2E Encrypted + Diff Polling)
- Create a family group in one tap and invite family members via **QR code or shareable `bunkerstock://join?...` / `#join?...` link** — **no accounts, logins, or API keys required**.
- **Cross-Platform Wire Compatibility**: Android (`javax.crypto` + `GZIP`) and Web/iOS (`crypto.subtle` + `CompressionStream("gzip")`) share the exact `v2:` AES-256-GCM wire format so Android and iPhone devices sync seamlessly.
- **Diff-Aware Auto-Sync**: Always checks for group diffs when opening or resuming the app, plus a configurable & toggleable foreground diff-polling interval (default every `15 s`) that skips redundant uploads/writes when nothing changed.
- Includes an optional self-hostable Cloudflare Worker in [`sync-worker/worker.js`](sync-worker/worker.js).

### Native Expiry Notifications, Custom Thresholds & Localization
- **Direct Numeric Threshold Inputs + Reset (`0 / 30 / 90`)** in Settings for *Expired*, *Critical*, and *Caution*, dynamically updating KPIs, health bar, filters, and alerts.
- **Native Android Background Notifications** via WorkManager when confirmed items enter the dangerous expiry range.
- **First-Time Language Selector**: Clean **Español / English** choice on first launch (switchable anytime in Settings), plus **Dark / Light / System** themes.
- **Data Portability & Auto-Updates**: JSON backup export/import, Excel-ready UTF-8 CSV export, and read-only GitHub Releases APK update checker on Android.

---

## Download & Launch

- **Android (Primary Native App):** Grab the latest signed release APK from **[GitHub Releases](https://github.com/Emefedez/BunkerStock/releases/latest)**. All releases are signed with a persistent keystore so new versions install seamlessly in-place.
- **Web / iOS / iPadOS (Supporting PWA):** Open **[emefedez.github.io/BunkerStock](https://emefedez.github.io/BunkerStock/)** in Safari (iOS 18.2+ for WasmGC) and tap **Share → Add to Home Screen**.

## Build & Test

```bash
# Native Android build & unit tests (zero web/wasm overhead)
gradle -PandroidOnly=true :shared:testDebugUnitTest :app:testDebugUnitTest :app:assembleDebug

# Web / iOS PWA build & shared tests (zero Android SDK needed)
gradle -PwebOnly=true :shared:jvmTest :webApp:wasmJsBrowserDistribution
```

GitHub Actions runs Android tests/APK builds via [`build-apk.yml`](.github/workflows/build-apk.yml) and independently compiles & deploys the Web/iOS PWA to GitHub Pages via [`deploy-web.yml`](.github/workflows/deploy-web.yml).

---

## Credits

Created by **Emefedez**.  
Built with Android / Jetpack Compose, Compose Multiplatform (WasmJs), Material 3, CameraX, Google ML Kit, ZXing, Open Food Facts, GitHub Actions, `android-design-skills`, and the Google DeepMind team.
