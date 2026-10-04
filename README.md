# Glass (vMusic) — Android FLAC Music Player

Minimal, local-files Android music player with an iOS look and feel, playing high-quality FLAC (up to 24-bit / 192 kHz) with low CPU and battery usage.

## Key Features
- **100% Material-Free Compose:** Built purely on Jetpack Compose Foundation, UI, and Graphics with zero Material/Material3 dependencies.
- **Fluid Liquid-Glass Motion:**
  - Multi-tier `Modifier.glass()` with AGSL meniscus lens refraction and chromatic edge dispersion (API 33+).
  - Dynamic interactive touch sheen following pointer coordinates and drag velocity.
  - iOS-calibrated mathematical rubber-band overscroll resistance.
  - Directional liquid blob stretch indicator for tab transitions.
  - Tactile `PressableScale` (0.96 scale-down on press with spring overshoot) replacing Android ripples.
- **Integrated Self-Updating Architecture:**
  - Queries GitHub Releases API for `SamarVScode/vMusic`.
  - Background chunked APK streaming to app cache with real-time byte progress reporting.
  - In-place installer launch via `FileProvider`.
  - Custom Glass-styled update dialog with animated blood-red progress indicator.
- **Audio Excellence:** Media3 ExoPlayer with direct DSP audio offload and gapless playback.
