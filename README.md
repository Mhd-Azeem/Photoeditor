# Photo Editor — iOS Photos-Inspired Android App

A native Android photo editing app built with Kotlin + Jetpack Compose, replicating Apple's iOS Photos app editing interface and functionality.

## Features

### Editing Tools
- **19 adjustments** across 4 groups (Light, Color, B&W, Detail):
  - **Light**: Exposure, Brilliance, Highlights, Shadows, Contrast, Brightness, Black Point
  - **Color**: Saturation, Vibrance, Warmth, Tint
  - **B&W**: Intensity, Neutrals, Tone, Grain
  - **Detail**: Sharpness, Definition, Noise Reduction
  - **Other**: Vignette
- **16 preset filters**: Vivid, Dramatic, Mono, Noir, Fade, Chrome, Instant, and more
- **Crop & Rotate**: Drag handles, aspect ratio chips (Free, Square, 4:3, 16:9), rotation slider

### Interface (matches iOS Photos)
- Pure black background, white/gray icons, yellow iOS-style accent (`#FFD60A`)
- Full-screen image preview with pinch-to-zoom and pan
- Bottom tab bar: Adjust | Filters | Crop
- Horizontally scrollable adjustment icon row
- Horizontal drag slider with live numeric value display
- Per-adjustment reset with yellow indicator dot when modified
- Auto Enhance (magic wand) for one-tap intelligent adjustments
- Filter carousel with live thumbnails and intensity slider

### Technical
- **MVVM** with `ViewModel` + `StateFlow`
- **Non-destructive** editing — original bitmap untouched, edit stack applied at export
- **Real-time preview** via debounced coroutine pipeline (scales preview for performance)
- **ColorMatrix pipeline** for all color/tone adjustments (GPU-accelerated via hardware canvas)
- **Per-pixel processing** for sharpness (unsharp mask), noise reduction (box blur), grain
- **Vignette** via RadialGradient overlay on Canvas
- **Save to Gallery** via MediaStore API (Android 10+ compatible)
- **Share** via FileProvider + ACTION_SEND intent
- **Exif orientation correction** using `ExifInterface`
- Coil for image loading/caching
- Navigation Compose for screen routing

## Tech Stack

| Component | Library |
|-----------|---------|
| UI | Jetpack Compose + Material3 |
| Architecture | MVVM + StateFlow |
| Image loading | Coil 2 |
| Navigation | Navigation Compose |
| Filters | Android ColorMatrix + Canvas pipeline |
| Media | MediaStore API + Photo Picker |
| Build | Gradle Kotlin DSL |
| Language | Kotlin 2.0 |

## Project Structure

```
app/src/main/java/com/photoeditor/
├── MainActivity.kt
├── PhotoEditorApplication.kt
├── data/
│   ├── model/           # AdjustmentType, EditState, FilterType, CropState
│   └── repository/      # ImageRepository (load/save bitmaps)
├── filters/
│   ├── ColorMatrixBuilder.kt   # All ColorMatrix math
│   ├── FilterPresets.kt        # 16 preset filter matrices
│   └── ImageProcessor.kt       # Full processing pipeline
├── viewmodel/
│   ├── EditViewModel.kt        # All edit state management
│   └── EditViewModelFactory.kt
└── ui/
    ├── theme/           # iOS-style dark colors, typography
    ├── navigation/      # NavGraph
    ├── screens/         # HomeScreen, EditScreen
    └── components/
        ├── TopBar.kt           # Cancel / Done / Share / Revert
        ├── ImagePreview.kt     # Pinch-zoom + crop overlay host
        ├── BottomTabBar.kt     # Adjust | Filters | Crop tabs
        ├── adjust/
        │   ├── AdjustPanel.kt  # Adjustment icon row + value display
        │   └── VerticalSlider.kt # Custom drag slider
        ├── filters/
        │   └── FiltersPanel.kt  # Filter carousel + intensity slider
        └── crop/
            ├── CropPanel.kt    # Rotation controls + aspect ratios
            └── CropOverlay.kt  # Canvas crop rect with drag handles
```

## Setup

### Requirements
- Android Studio Hedgehog (2023.1.1) or newer
- JDK 17
- Android SDK API 34
- Minimum device: API 26 (Android 8.0)

### Build
```bash
# Clone the repo
git clone https://github.com/mhd-azeem/photoeditor.git
cd photoeditor

# Generate the Gradle wrapper JAR (required first time)
gradle wrapper

# Build debug APK
./gradlew assembleDebug

# Install on connected device
./gradlew installDebug
```

> **Note**: The `gradlew` shell script is included but the `gradle-wrapper.jar` binary is not tracked in git. Run `gradle wrapper` once after cloning to generate it, or open the project in Android Studio which will handle this automatically.

### Permissions
The app requests:
- `READ_MEDIA_IMAGES` (Android 13+) or `READ_EXTERNAL_STORAGE` (Android ≤12) — to pick photos
- `WRITE_EXTERNAL_STORAGE` (Android ≤9) — to save edited photos

On Android 10+, photos are saved to `Pictures/PhotoEditor/` using the MediaStore API with no storage permission needed.

## Usage

1. Launch the app and tap **"Choose Photo"**
2. Pick any photo from your gallery using the system Photo Picker
3. Edit using the three tabs:
   - **Adjust** — tap an adjustment icon, then drag the slider left/right
   - **Filters** — tap a filter thumbnail; drag the Intensity slider
   - **Crop** — drag corner/edge handles; tap aspect ratio chips; drag rotation slider
4. Tap the **wand icon** for one-tap Auto Enhance
5. Tap **Done** to save the edited image to your gallery
6. Tap **Share** to share directly from the editor
7. Use **More → Revert to Original** to undo all changes
