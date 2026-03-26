
<div align="center">

# ✦ AstroView

**Lightweight astronomical image viewer for Android**

Open and visualize FITS, XISF, and TIFF files from your phone.

[![API](https://img.shields.io/badge/API-26%2B-brightgreen?style=flat-square)](https://android-arsenal.com/api?level=26)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue?style=flat-square)](LICENSE)
[![Platform](https://img.shields.io/badge/Platform-Android-3DDC84?style=flat-square&logo=android&logoColor=white)](https://www.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9-7F52FF?style=flat-square&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Material3-4285F4?style=flat-square&logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)

</div>

---

## About

AstroView is a free, open-source Android app designed for astrophotographers who need a quick way to preview astronomical image files on their phone or tablet. No bloat, no ads, no internet — just open your files and inspect them.

Built entirely with **Jetpack Compose** and **zero external image processing libraries**. All FITS/XISF/TIFF decoding and the auto-stretch algorithm are implemented natively in Kotlin.

---

## Features

| Feature | Details |
|---|---|
| 📂 **File Support** | FITS (.fits, .fit, .fts), XISF (.xisf), TIFF (.tiff, .tif) |
| 🎨 **Bit Depths** | 8-bit, 16-bit, 32-bit integer, 32-bit float, 64-bit float |
| 🌈 **Color Modes** | Monochrome and RGB |
| ✨ **Auto Stretch (STF)** | Midtone Transfer Function targeting median 0.25 with MAD-based shadow clipping |
| 🔄 **Transforms** | Rotate ±90°, flip horizontal, flip vertical |
| 🔍 **Zoom & Pan** | Pinch-to-zoom (0.5x–20x) with smooth panning |
| 📊 **Info Bar** | Filename, dimensions, bit depth, color mode, format, STF indicator |
| 📱 **Modern UI** | Material 3, dark space theme, adaptive navigation bar support |
| 🪶 **Lightweight** | ~4 MB APK, no internet permissions, no external dependencies |

---

## Auto Stretch Algorithm

AstroView implements a **Midtone Transfer Function (MTF)** auto-stretch inspired by PixInsight's Screen Transfer Function (STF):

1. **Median** is computed per channel
2. **MAD** (Median Absolute Deviation) is calculated and normalized (×1.4826)
3. **Shadow clipping** is set at `median - 2.8 × MAD`
4. The **midtone balance** parameter is computed to map the normalized median to a **target of 0.25**
5. The MTF curve is applied to every pixel:

```
MTF(x, m) = (m - 1) × x / ((2m - 1) × x - m)
```

This ensures that any linear astronomical image — regardless of its original histogram distribution — becomes clearly visible with a single toggle.

---

## Installation

### From APK (Recommended)

1. Go to the [Releases](https://github.com/Ft2801/AstroView/releases) page
2. Download the latest `AstroView-vX.X.X.apk`
3. Transfer to your Android device and install
4. You may need to enable **"Install from unknown sources"** in your device settings

### Build from Source

**Prerequisites:**
- Android Studio Hedgehog (2023.1.1) or newer
- Android SDK 34
- JDK 17

**Steps:**

```bash
# Clone the repository
git clone https://github.com/Ft2801/AstroView.git
cd AstroView

# Open in Android Studio
# File → Open → select the AstroView folder

# Or build from command line
./gradlew assembleDebug
```

The debug APK will be at:
```
app/build/outputs/apk/debug/app-debug.apk
```

For a release build:
```bash
./gradlew assembleRelease
```

> **Note:** Release builds require a signing key. See [Android documentation](https://developer.android.com/studio/publish/app-signing) for details.

---

## Usage

1. **Open** the app and tap the folder icon or the "Open File" button
2. **Select** a FITS, XISF, or TIFF file from your device
3. The image loads and displays in its **original linear** state
4. Tap **STF** to toggle the auto-stretch — the image becomes visible instantly
5. Use **Rot L / Rot R** to rotate and **Flip H / Flip V** to mirror
6. **Pinch** to zoom in/out, **drag** to pan when zoomed
7. Tap **Reset** to restore original orientation
8. The **info bar** at the top shows file details and current STF state

---

## Supported Formats

### FITS (Flexible Image Transport System)

| BITPIX | Type | Supported |
|---|---|---|
| 8 | Unsigned 8-bit integer | ✅ |
| 16 | Signed 16-bit integer | ✅ |
| 32 | Signed 32-bit integer | ✅ |
| -32 | 32-bit IEEE float | ✅ |
| -64 | 64-bit IEEE double | ✅ |

- NAXIS2 (2D mono) and NAXIS3 (3D color) images
- BZERO/BSCALE applied automatically
- Big-endian per FITS standard

### XISF (Extensible Image Serialization Format)

| Sample Format | Supported |
|---|---|
| UInt8 | ✅ |
| UInt16 | ✅ |
| UInt32 | ✅ |
| Float32 | ✅ |
| Float64 | ✅ |

- Uncompressed attachments
- XML header parsing
- Planar and pixel-interleaved layouts

### TIFF (Tagged Image File Format)

| Configuration | Supported |
|---|---|
| 8-bit unsigned | ✅ |
| 16-bit unsigned | ✅ |
| 32-bit unsigned | ✅ |
| 32-bit float | ✅ |
| Uncompressed | ✅ |
| Compressed (LZW, ZIP, etc.) | ❌ |

---

## Architecture

```
com.astroview/
├── model/           Data classes (AstroImage, ViewerState)
├── decoder/         Format-specific decoders (FITS, XISF, TIFF)
├── engine/          Image processing (AutoStretch, Transforms, Bitmap conversion)
├── viewmodel/       MVVM state management
└── ui/
    ├── theme/       Material 3 dark space theme
    ├── components/  Reusable UI (ImageViewer, BottomToolbar)
    └── screens/     Main viewer screen
```

**Design decisions:**
- **No external libraries** for image decoding — everything is native Kotlin
- **MVVM** with `StateFlow` for reactive UI updates
- **Pre-computed bitmaps** — both original and stretched versions are cached in memory for instant toggle
- **Background processing** — decoding and stretch computation run on `Dispatchers.IO` / `Dispatchers.Default`
- **Adaptive system bars** — `systemBarsPadding()` handles gesture nav, button nav, and notches

---

## Tech Stack

| Component | Technology |
|---|---|
| Language | Kotlin 1.9 |
| UI Framework | Jetpack Compose with Material 3 |
| Architecture | MVVM + StateFlow |
| Min SDK | API 26 (Android 8.0 Oreo) |
| Target SDK | API 34 (Android 14) |
| Build System | Gradle 8.2 with Kotlin DSL |
| Image Decoding | Custom native Kotlin decoders |
| External Dependencies | None beyond AndroidX |

---

## Requirements

- Android 8.0 (API 26) or higher
- ~4 MB storage
- RAM depends on image size (a 4096×4096 RGB float image uses ~200 MB)

---

## Roadmap

- [ ] Compressed TIFF support (LZW, ZIP)
- [ ] XISF compressed blocks
- [ ] Histogram display
- [ ] Per-channel stretch controls
- [ ] Debayer for raw CFA images
- [ ] Dark/Flat reference overlay

---

## License

This project is licensed under the **MIT License** — see the [LICENSE](LICENSE) file for details.
