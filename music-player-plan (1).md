# Glass — Android FLAC Music Player: Build Plan (v2)

> v2 changes: **no Material3**. Fully custom design system, 100% iOS look, pure black / white with a **blood-red accent**, and **animated liquid-glass** surfaces.

## 1. Goal

A minimal, local-files Android music player that looks and feels like an iOS app.
Plays high-quality FLAC (up to 24-bit / 192 kHz) with very low CPU and battery use.

**Screens (nothing else):**
1. Song list (home): search + refresh button
2. Player (mini player + full-screen)
3. Settings

**Non-goals:** streaming, playlists, equalizer, lyrics, cloud, accounts, ads, analytics.

---

## 2. Assumptions (tell me to change any)

| Topic | Assumption |
|---|---|
| Min Android | API 26 (Android 8). Glass quality scales by API level (see §6) |
| Source | Local files via MediaStore |
| UI | Kotlin + Jetpack Compose **foundation/ui only** (no Material) |
| Formats | FLAC first-class; MP3, AAC, OGG, WAV, M4A work from the same engine |
| Themes | Dark (pure black) and Light (pure white), follows system, override in Settings |
| Library | Up to ~20,000 tracks |
| Distribution | Sideload APK first |

---

## 3. Tech Stack (Material-free)

| Layer | Choice | Notes |
|---|---|---|
| UI toolkit | `compose.ui`, `compose.foundation`, `compose.animation`, `compose.runtime` | **Do not add `material3` or `material`**; no ripple, no M3 theme, no M3 text/colors |
| Design system | Custom `GlassTheme` + own components | See §5 and §6 |
| Backdrop blur | **Haze** (`dev.chrisbanes.haze`) | Blur of content behind bars / pills; works API 31+ |
| Refraction / specular | **AGSL `RuntimeShader`** (API 33+) | Lens distortion + moving highlight on small glass elements |
| Playback | **Media3 ExoPlayer** + `MediaSessionService` | Native FLAC, gapless, offload, notification |
| Library DB | Room + FTS4 | Instant search |
| Settings | DataStore | |
| Images | Coil | Downsampled art, small cache |
| Icons | Own vector set (Phosphor, MIT licence, thin weight) | **SF Symbols can't be used** (Apple-only licence) |
| Font | **Inter** (open source), tight tracking, semibold titles | **SF Pro can't be bundled** (Apple-only licence); Inter is the closest match |
| Navigation | Navigation Compose with custom iOS-style transitions | Not a Material dependency |
| Haptics | `HapticFeedback` (light impacts on taps, toggles, scrub ticks) | |
| Build | Gradle KTS, R8, Baseline Profile | |

---

## 4. Architecture

```
UI (custom Compose design system)
 ├─ SongListScreen ── GlassSearchField, RefreshButton
 ├─ PlayerScreen ──── MiniPlayer ⇄ FullPlayer (morphing)
 └─ SettingsScreen ── iOS grouped lists
        │
ViewModels (StateFlow)
        │
 ├─ LibraryRepository ── Room (+FTS) ◄── MediaStoreScanner
 ├─ PlaybackController ── MediaController ◄──► PlaybackService (ExoPlayer + MediaSession)
 └─ SettingsRepository ── DataStore
```

- Single activity, 3 destinations.
- Playback lives in a foreground `MediaSessionService`; UI can die without stopping music.

---

## 5. Design System: "Glass" (100% iOS look)

### 5.1 Colour tokens

| Token | Dark (default) | Light |
|---|---|---|
| `background` | `#000000` | `#FFFFFF` |
| `labelPrimary` | `#FFFFFF` | `#000000` |
| `labelSecondary` | white 60% | black 60% |
| `labelTertiary` | white 30% | black 30% |
| `separator` | white 15% | black 12% |
| `fillSubtle` (rows pressed, inactive tracks) | white 12% | black 8% |
| `glassTint` | white 8–12% | white 55–70% |
| `glassBorder` | white 22% → 4% diagonal gradient | white 80% → black 6% |
| **`accent` (blood red)** | `#C0111F` | `#C0111F` |
| `accentPressed` | `#8E0B16` | `#8E0B16` |
| `accentText` (small red text on black) | `#E5303E` | `#C0111F` |
| `accentGlow` | `#FF2A3A` at 30% | `#FF2A3A` at 18% |

- Red is used **only** for: play/pause fill, seek progress, active toggle, selected state, refresh spinner, focus caret. Everything else is black / white / gray, as in stock iOS.
- Contrast check: `#C0111F` on black is ~3.3:1 (fine for icons, tracks, big shapes) and ~6.3:1 on white. Small red text on black uses `accentText`.
- Pure `#000000` background = true OLED black (also saves battery).

### 5.2 Custom components (replace everything Material would give)

| Component | iOS reference |
|---|---|
| `GlassSurface` | The base modifier: blur + tint + saturation boost + specular border |
| `LargeTitleHeader` | Collapsing large title → inline title with blur bar |
| `GlassNavPill` / floating bottom bar | iOS 26 floating tab bar |
| `GlassSearchField` | Pill search with magnifier, clear button, "Cancel" slide-in |
| `SongRow` | Plain row (no glass), 0.5 dp separators, art 48 dp, press = fillSubtle |
| `MiniPlayer` | Floating glass capsule above bottom edge |
| `FullPlayer` | Big art, glass transport cluster, glass seek slider |
| `GlassSlider` | Thin track → thickens on drag, red progress, haptic ticks |
| `GlassSwitch` | iOS 51×31 switch, red when on, spring thumb |
| `GroupedList` / `GroupedRow` | iOS Settings inset-grouped cards |
| `GlassSheet` | Bottom sheet with drag handle (for pickers) |
| `RefreshButton` | Round glass button, red arc spinner while scanning |
| `PressableScale` | Replaces ripple: scale 0.96 + highlight on press, spring back |

### 5.3 Typography (Inter)
Large title 34 bold · Title 22 semibold · Headline 17 semibold · Body 17 regular · Subhead 15 · Caption 12 · tabular numbers for time labels.

### 5.4 Shape & spacing
Corner radii 12 (rows) / 22–28 (cards, sheets) / full (pills, buttons) · 16 dp screen margins · 8 dp grid · continuous-corner feel (use high radii, optional squircle path).

---

## 6. Animated Liquid-Glass Engine

### 6.1 `Modifier.glass()` layers (bottom → top)
1. **Backdrop blur** of whatever is behind (Haze), radius 20–30 dp, rendered at reduced resolution.
2. **Saturation boost** (ColorMatrix ~1.6×) so colours behind pop like iOS.
3. **Tint** (`glassTint`).
4. **Refraction** (AGSL lens shader): bends the backdrop near the element's edges; **small elements only** (buttons, mini player, tab pill).
5. **Specular border**: gradient stroke, brighter top-left, plus a thin inner shadow at the bottom edge.
6. **Moving highlight**: a soft sheen that shifts with touch position and drag velocity. Optional gyroscope shift (off by default; opt-in under Reduce motion exceptions).

### 6.2 Animation catalogue (all spring-based, no linear tweens)

| Interaction | Animation |
|---|---|
| Press any glass control | Scale 1 → 0.96, sheen brightens, haptic tick; releases with springy overshoot |
| Mini player → full player | Container morph via `SharedTransitionLayout`: art, title, controls fly to their positions; glass capsule expands to full screen; swipe-down is interactive and follows the finger |
| Tab / segment switch | Glass pill slides under the selected item and stretches ("liquid" blob) while moving |
| Play ⇄ Pause | Icon morph (animated vector) + album art scales 0.88 ⇄ 1.0 (iOS Music style) |
| Large title | Collapses with scroll; nav bar blur fades in |
| Search focus | Field widens, "Cancel" slides in, list dims slightly |
| Refresh | Button press → red arc spins; on finish, count label springs in ("1,248 songs") |
| Slider drag | Track thickens 4 → 10 dp, thumb grows, time labels slide apart |
| Toggle | Thumb stretches while moving, track tints red |
| Screen push / pop | iOS slide with 30% parallax of the screen underneath; edge-swipe back is interactive (predictive back) |
| Overscroll | Rubber-band spring effect (custom `OverscrollEffect`) |
| Player background | Blurred, darkened album art with a slow colour drift (very low update rate, paused when screen off or app backgrounded) |

### 6.3 Quality tiers (auto-selected, user override in Settings)

| Tier | Condition | What you get |
|---|---|---|
| **A: Full** | API 33+, no battery saver | Blur + saturation + refraction shader + sheen |
| **B: Blur** | API 31–32 | Blur + saturation + specular border + sheen (no shader) |
| **C: Flat glass** | API 26–30, battery saver, or "Reduced" | Translucent tint + gradient border + faux sheen; no blur |

A **frame-time governor** (JankStats / `Choreographer`) drops one tier automatically if it sees sustained jank, and notifies the user once.

---

## 7. CPU / Battery Strategy

### Audio path (the part that must be efficient)
- **Audio offload** on by default (`AudioOffloadPreferences`): decoding runs on the DSP so the CPU sleeps; auto-falls back if the device or format doesn't support it.
- Native FLAC extractor; **no app-side resampling, no software DSP/effects**.
- Optional "Hi-res output" (float PCM) toggle; default 16/24-bit PCM.
- Small `LoadControl` buffers (local files don't need big ones).
- Gapless through ExoPlayer playlist items.
- Optional (Android 14+): USB DAC bit-perfect path via mixer attributes.
- Screen off ⇒ zero UI work: all animations and progress updates stop.

### UI / GPU budget
- **Glass only where it matters**: nav bar, search field, mini player, transport cluster, settings header. **Never on song rows.**
- Song list sits *behind* the translucent top/bottom bars (that gives the iOS "content blurs underneath" look); blur is downsampled to ~¼ resolution.
- Refraction shader runs on elements < ~200×200 dp only, and only while they animate (static ⇒ cached).
- Progress bar updates ≈ 10 Hz, only on the visible player.
- Player background blur is computed **once per track**, then cached as a bitmap (colour drift is a cheap gradient overlay, not a re-blur).
- Respect system **"Remove animations"** / Reduce Motion ⇒ springs shortened, morphs become cross-fades, sheen static.
- Baseline Profile + R8 for fast startup.

### Library path
- Scan once into Room; launch reads DB only.
- Refresh = incremental diff using `DATE_MODIFIED` / generation, not a full rescan.
- Bit depth / sample rate read lazily, not during scan.

---

## 8. Feature Spec

### 8.1 Song list (home)
- Large title "Songs"; rows: art, title, artist, duration, small `FLAC · 24/96` badge.
- Search field (live FTS, 150 ms debounce) and refresh button in the header.
- Tap = play and queue current filtered list. Optional alphabet fast-scroller.

### 8.2 Player
- Mini: glass capsule (art, title, play/pause, next). Tap or swipe up = full.
- Full: large art, title/artist, glass seek slider with times, prev / play-pause / next, shuffle, repeat, format line (`FLAC · 24-bit · 96 kHz`).
- Notification + lock screen via MediaSession.

### 8.3 Settings (iOS grouped list)
- **Appearance:** Theme (System / Light / Dark) · Glass quality (Auto / Full / Reduced) · Reduce motion · Haptics
- **Playback:** Audio offload · Hi-res output · Gapless
- **Library:** Folders to scan · Minimum track length (30 s)
- **About:** version

(Accent is fixed to blood red; no colour picker.)

### 8.4 Permissions
`READ_MEDIA_AUDIO` (13+) / `READ_EXTERNAL_STORAGE` (≤12), `FOREGROUND_SERVICE_MEDIA_PLAYBACK`, `POST_NOTIFICATIONS`.

---

## 9. Data Model

```kotlin
@Entity(tableName = "tracks")
data class Track(
  @PrimaryKey val mediaStoreId: Long,
  val uri: String,
  val title: String,
  val artist: String,
  val album: String,
  val durationMs: Long,
  val mimeType: String,
  val bitDepth: Int?,
  val sampleRateHz: Int?,
  val dateModified: Long,
  val albumArtUri: String?
)
// + FTS4 virtual table over title, artist, album
```

---

## 10. Project Structure

```
app/src/main/java/com/glass/player/
├── MainActivity.kt
├── design/                 ← the Material replacement
│   ├── GlassTheme.kt        (tokens, light/dark, CompositionLocals)
│   ├── Typography.kt
│   ├── GlassModifier.kt     (blur, tint, border, sheen, tiers)
│   ├── shaders/             (refraction.agsl)
│   ├── GlassQualityGovernor.kt
│   ├── motion/              (springs, PressableScale, rubber-band overscroll)
│   └── components/          (GlassSlider, GlassSwitch, GlassNavPill, GroupedList, GlassSheet…)
├── ui/
│   ├── songs/
│   ├── player/              (MiniPlayer, FullPlayer, morph transition)
│   └── settings/
├── data/ (db, scanner, settings)
├── playback/ (PlaybackService, PlaybackController)
└── util/
```

---

## 11. Milestones

| # | Milestone | Output | Est. |
|---|---|---|---|
| 1 | Project setup | Compose-only project (verify no Material dependency), tokens, Inter, icons | 1 day |
| 2 | Design system core | `GlassTheme`, `Modifier.glass()` tiers A/B/C, PressableScale | 2 days |
| 3 | Components | Slider, switch, nav pill, search field, grouped list, sheet | 2–3 days |
| 4 | Library | MediaStore scanner, Room, song list, permissions | 1–2 days |
| 5 | Playback | Media3 service, queue, gapless, offload, notification | 2 days |
| 6 | Player UI + morph | Mini ⇄ full transition, art/play-pause animations | 2–3 days |
| 7 | Search + Refresh | FTS search, incremental refresh, animations | 1 day |
| 8 | Settings | Grouped lists wired to DataStore | 1 day |
| 9 | Shader + polish | AGSL refraction, sheen, overscroll, haptics | 2 days |
| 10 | Performance + QA | Governor, profiling, Baseline Profile, device matrix, APK | 2 days |

Total: roughly **16–20 working days** solo (the custom glass system is the main added cost vs v1).

---

## 12. Testing & Targets

**Test files:** 16/44.1, 24/96, 24/192 FLAC · >200 MB FLAC · gapless album · embedded art · broken tags.

| Metric | Target |
|---|---|
| CPU, screen off, offload on | < 2–3% |
| CPU, offload off | < 8% (mid-range) |
| Cold start to song list | < 1 s |
| Scroll with glass bars | 60 fps (120 on high-refresh) without blur-caused drops |
| Glass animations | No dropped frames on tier A on a mid-range device; governor downgrades otherwise |
| Memory (full player open) | < 150 MB |
| Screen-off FLAC battery | ≥ 20 h on a 5000 mAh phone (offload on) |
| 10k-track scan / incremental refresh | < 10 s / < 2 s |

Tools: Android Studio Profiler, Perfetto, GPU rendering profile, `dumpsys audio`, `dumpsys batterystats`, Macrobenchmark, JankStats.

---

## 13. Risks & Mitigations

| Risk | Mitigation |
|---|---|
| Animated glass is GPU-heavy on weak phones | Three tiers, small-area shaders, cached blurs, auto governor, manual override |
| AGSL needs API 33+ | Tier B/C fallbacks look close without refraction |
| Blur only on API 31+ | Tier C flat-glass fallback tuned to still look iOS-like |
| Offload unsupported for some rates | Auto fallback to PCM path |
| No SF Pro / SF Symbols (licence) | Inter + Phosphor icons; visually near-identical |
| Without Material, no free accessibility/semantics | Add `semantics {}` roles, 48 dp touch targets, content descriptions manually |
| Blood red on black has low contrast for small text | Use `accentText` for small text; red only on large shapes |
| OEM kills background service | Proper foreground service + battery-optimisation hint in Settings |

---

## 14. Out of Scope for v1
Playlists, equalizer, lyrics, widgets, Android Auto, ReplayGain, CUE, DSD.

---

## 15. Open Questions (defaults used if unanswered)

1. Min API: **26** with tiers (default), or **31+** only so blur is guaranteed everywhere?
2. Is the gyroscope-driven sheen wanted, or keep it touch-only (default, cheaper)?
3. Queue/position persistence across restarts? Default **yes**.
4. App name/package? Default **"Glass"**, `com.glass.player`.
5. Blood-red shade: default `#C0111F`; want darker (`#8A0303`) or brighter?
