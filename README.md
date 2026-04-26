# AstroView

Lightweight astronomical image viewer for Android.

Open and inspect FITS and XISF files directly from your Android device.

[![API](https://img.shields.io/badge/API-26%2B-brightgreen?style=flat-square)](https://android-arsenal.com/api?level=26)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue?style=flat-square)](LICENSE)
[![Platform](https://img.shields.io/badge/Platform-Android-3DDC84?style=flat-square&logo=android&logoColor=white)](https://www.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-1.9-7F52FF?style=flat-square&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Material3-4285F4?style=flat-square&logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![GitHub release](https://img.shields.io/github/v/release/Ft2801/AstroView?style=flat-square&color=orange)](https://github.com/Ft2801/AstroView/releases)

## About

AstroView is a free, open-source Android application designed for astrophotographers who need a
quick and reliable way to preview astronomical image files on a mobile device. No bloat, no ads,
no internet connection required. Open your files and inspect them.

Built entirely with Jetpack Compose and zero external image processing libraries. All FITS and XISF
decoding and the auto-stretch algorithm are implemented natively in Kotlin.

## File Support Constraints

AstroView supports FITS and XISF files only. The following hard limits apply to every file opened:

- Maximum file size: 80 MB
- Maximum image resolution: 15 megapixels (width x height)

Files exceeding either limit will be rejected with an explicit error message. These constraints
exist to protect device memory and ensure a responsive user experience on a wide range of Android
hardware.

## Features

| Feature           | Details                                                                      |
|-------------------|------------------------------------------------------------------------------|
| File Support      | FITS (.fits, .fit, .fts), XISF (.xisf)                                      |
| Bit Depths        | 8-bit, 16-bit, 32-bit integer, 32-bit float, 64-bit float                   |
| Color Modes       | Monochrome and RGB                                                           |
| Auto Stretch      | Midtone Transfer Function targeting median 0.25 with MAD-based shadow clip   |
| Transforms        | Rotate +/-90 degrees, flip horizontal, flip vertical                         |
| Zoom and Pan      | Pinch-to-zoom (0.5x to 20x) with smooth panning                             |
| Info Bar          | Filename, dimensions, bit depth, color mode, format, STF indicator           |
| Modern UI         | Material 3, dark space theme, adaptive navigation bar support                |
| Lightweight       | Approximately 4 MB APK, no internet permissions, no external dependencies    |

## Auto Stretch Algorithm

AstroView implements a Midtone Transfer Function (MTF) auto-stretch inspired by PixInsight's
Screen Transfer Function (STF):

1. Median is computed per channel.
2. MAD (Median Absolute Deviation) is calculated and normalized by a factor of 1.4826.
3. Shadow clipping is set at `median - 2.8 x MAD`.
4. The midtone balance parameter is computed to map the normalized median to a target of 0.25.
5. The MTF curve is applied to every pixel:

```
MTF(x, m) = (m - 1) * x / ((2m - 1) * x - m)
```

This ensures that any linear astronomical image, regardless of its original histogram distribution,
becomes clearly visible with a single toggle.

## Supported Formats

### FITS (Flexible Image Transport System)

| BITPIX | Type                    |
|--------|-------------------------|
| 8      | Unsigned 8-bit integer  |
| 16     | Signed 16-bit integer   |
| 32     | Signed 32-bit integer   |
| -32    | 32-bit IEEE float       |
| -64    | 64-bit IEEE double      |

- NAXIS2 (2D mono) and NAXIS3 (3D color) images supported.
- BZERO and BSCALE applied automatically.
- Big-endian byte order per FITS standard.

### XISF (Extensible Image Serialization Format)

| Sample Format |
|---------------|
| UInt8         |
| UInt16        |
| UInt32        |
| Float32       |
| Float64       |

- Uncompressed attachments only.
- XML header parsing.
- Planar and pixel-interleaved layouts.

## Architecture

```
com.astroview/
|-- model/        Data classes (AstroImage, ViewerState)
|-- decoder/      Format-specific decoders (FitsDecoder, XisfDecoder, ImageDecoder)
|-- engine/       Image processing (AutoStretch, TransformEngine, ImageProcessor)
|-- viewmodel/    MVVM state management (ViewerViewModel)
`-- ui/
    |-- theme/        Material 3 dark space theme
    |-- components/   Reusable composables (ImageViewer, BottomToolbar)
    `-- screens/      Main viewer screen (ViewerScreen)
```

Design decisions:

- No external libraries for image decoding. Everything is native Kotlin.
- MVVM architecture with StateFlow for reactive UI updates.
- Both original and auto-stretched bitmaps are pre-computed and cached in memory for instant toggle.
- Decoding and stretch computation run on Dispatchers.IO and Dispatchers.Default respectively.
- systemBarsPadding() handles gesture navigation, button navigation, and notches.

## Tech Stack

| Component           | Technology                         |
|---------------------|------------------------------------|
| Language            | Kotlin 1.9                         |
| UI Framework        | Jetpack Compose with Material 3    |
| Architecture        | MVVM with StateFlow                |
| Min SDK             | API 26 (Android 8.0 Oreo)         |
| Target SDK          | API 34 (Android 14)               |
| Build System        | Gradle 8.2 with Kotlin DSL        |
| Image Decoding      | Custom native Kotlin decoders      |
| External Libraries  | None beyond AndroidX               |

## Requirements

- Android 8.0 (API 26) or higher.
- Approximately 4 MB storage for the application.
- RAM usage depends on image size. A 4096x4096 RGB float image requires approximately 200 MB.

## Installation

### From APK

1. Go to the [Releases](https://github.com/Ft2801/AstroView/releases) page.
2. Download the latest `AstroView-vX.X.X.apk`.
3. Transfer to your Android device and install.
4. Enable "Install from unknown sources" in your device settings if required.

### Build from Source

Prerequisites:

- Android Studio Hedgehog (2023.1.1) or newer
- Android SDK 34
- JDK 17

```bash
git clone https://github.com/Ft2801/AstroView.git
cd AstroView
./gradlew assembleDebug
```

The debug APK will be located at:

```
app/build/outputs/apk/debug/app-debug.apk
```

For a release build:

```bash
./gradlew assembleRelease
```

Release builds require a signing key. See the
[Android documentation](https://developer.android.com/studio/publish/app-signing) for details.

## Usage

1. Open the app and tap the Open button.
2. Select a FITS or XISF file from your device. Files must not exceed 80 MB or 15 megapixels.
3. The image loads and displays in its original linear state.
4. Tap STF to toggle the auto-stretch. The image becomes visually interpretable instantly.
5. Use Rot L and Rot R to rotate, and Flip H and Flip V to mirror the image.
6. Pinch to zoom in or out. Drag to pan when zoomed.
7. Tap Reset to restore the original orientation.
8. The info bar at the top shows file details and the current STF state.

## Roadmap

- XISF compressed blocks
- Histogram display
- Per-channel stretch controls
- Debayer support for raw CFA images

## License

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.
