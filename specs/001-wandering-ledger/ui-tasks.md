# Wandering Ledger — Proposed Vertical Slice Breakdown

This restructuring shifts the project away from large subsystem-oriented implementation and toward playable, reviewable vertical slices.

Each slice:

* delivers visible user value
* exercises multiple systems end-to-end
* supports rapid visual iteration
* reduces long-lived integration risk
* keeps PRs reviewable

---

# Slice Types

## HITL

Human-in-the-loop.

Requires:

* visual review
* UX tuning
* animation feel evaluation
* design iteration
* art integration review

These slices should usually end with:

* video capture
* device QA
* UX signoff

---

## AFK

Primarily infrastructure or deterministic engineering.

Can usually proceed with:

* minimal UX review
* automated validation
* architecture review only

---

# Proposed Vertical Slice Order

---

# 1. Core Theme Foundation

## Type

AFK

## Blocked By

None

## User Stories Covered

Supports all user stories.

## Deliverables

* tokenized theme system
* typography token model
* atmospheric color system
* biome-aware theme injection
* Compose theme locals
* removal of hardcoded colors

## Visible Outcome

App can switch visual identity dynamically.

## Success Criteria

* no feature module uses raw colors
* runtime theme swapping works
* typography scales safely

## Status

**COMPLETE** ✓

---

# 2. Storybook Navigation Shell

## Type

HITL

## Blocked By

1

## User Stories Covered

Global navigation usability across all stories.

## Deliverables

* custom bottom navigation
* atmospheric top bars
* transition framework
* screen container architecture
* safe-area handling

## Visible Outcome

Entire app begins feeling like a cohesive storybook product.

## Success Criteria

* navigation feels soft and tactile
* transitions feel physical rather than app-like
* thumb ergonomics validated

## Status

**COMPLETE** ✓

---

# 3. Journey Screen Skeleton

## Type

HITL

## Blocked By

1, 2

## User Stories Covered

User Story: Traveling
User Story: Banking Steps

## Deliverables

* journey scaffold
* illustration viewport
* overlay slot system
* responsive layout rules
* placeholder route presentation
* placeholder step meter

## Visible Outcome

Core emotional screen exists structurally.

## Success Criteria

* illustration dominates screen
* overlays remain readable
* layout stable across device sizes

## Status

**COMPLETE** ✓

---

# 4. Party Rendering Slice

## Type

HITL

## Blocked By

3

## User Stories Covered

User Story: Traveling
User Story: Companion Attachment

## Deliverables

* sprite rendering
* animation scheduler
* party formations
* directional movement
* idle states

## Visible Outcome

Characters visibly travel through the world.

## Success Criteria

* multiple companions render correctly
* movement appears believable
* animations survive recomposition

## Status

**COMPLETE** ✓

---

# 5. Environment Atmosphere Slice

## Type

HITL

## Blocked By

3

## User Stories Covered

User Story: Traveling
User Story: Idle/Camp State
User Story: World Discovery

## Deliverables

* layered biome renderer
* parallax system
* weather overlays
* time-of-day tinting
* environmental transitions

## Visible Outcome

World feels alive and atmospheric.

## Success Criteria

* no visible loading hitching
* atmospheric transitions interpolate smoothly
* environment identity clearly changes by biome

## Status

**COMPLETE** ✓

---

# 6. Step Fantasy Slice

## Type

HITL

## Blocked By

3

## User Stories Covered

User Story: Banking Steps

## Deliverables

* metaphor-driven step meter
* animated accumulation
* route affordability visuals
* backend step-bank integration

## Visible Outcome

Walking no longer resembles fitness tracking.

## Success Criteria

* users emotionally interpret steps as travel energy
* affordability understandable at glance
* meter animations feel organic

## Status

**COMPLETE** ✓
---

# 7. Route Progress Slice

## Type

HITL

## Blocked By

3, 5

## User Stories Covered

User Story: Traveling
User Story: Route Planning

## Deliverables

* spline route renderer
* travel marker animation
* encounter markers
* waypoint rendering

## Visible Outcome

Travel progress visually maps to geography.

## Success Criteria

* progress readable without bars
* route identity feels handcrafted

## Status

**COMPLETE** ✓

---

# 8. Camp State Slice

## Type

HITL

## Blocked By

4, 5

## User Stories Covered

User Story: Idle/Camp State
User Story: Checking In

## Deliverables

* camp-state detection
* camp renderer
* idle companion placement
* environmental loops

## Visible Outcome

Idle app state becomes emotionally rewarding.

## Success Criteria

* users enjoy opening app while idle
* camp scenes feel cozy and alive

## Status

**COMPLETE** ✓

---

# 9. World Map Exploration Slice

## Type

HITL

## Blocked By

1, 2, 5

## User Stories Covered

User Story: Route Planning
User Story: World Discovery

## Deliverables

* parchment map renderer
* terrain layers
* discovery fogging
* animated map details
* route highlighting

## Visible Outcome

World exploration becomes aspirational.

## Success Criteria

* map never resembles GPS UI
* exploration feels inviting

## Status

**COMPLETE** ✓

---

# 10. Town Arrival Slice

## Type

HITL

## Blocked By

2, 5

## User Stories Covered

User Story: Arrival

## Deliverables

* arrival presentation state machine
* establishing artwork renderer
* title overlays
* atmospheric arrival transitions

## Visible Outcome

Travel has emotional payoff.

## Success Criteria

* arrivals feel rewarding
* transitions feel cinematic but skippable

## Status

**COMPLETE** ✓

---

# 11. Town Navigation Slice

## Type

HITL

## Blocked By

10

## User Stories Covered

User Story: Browsing Services

## Deliverables

* district card renderer
* responsive district layout
* hover/focus states
* district availability binding

## Visible Outcome

Town interaction becomes spatial and tactile.

## Success Criteria

* town navigation readable one-handed
* districts visually distinct

## Status

**COMPLETE** ✓

---

# 12. Market Presentation Slice

## Type

HITL

## Blocked By

11

## User Stories Covered

User Story: Trading Goods

## Deliverables

* goods card renderer
* scarcity visual treatments
* rarity emphasis states
* tactile transaction layouts

## Visible Outcome

Trading feels physical instead of spreadsheet-driven.

## Success Criteria

* goods readable at glance
* scarcity emotionally legible

## Status

**COMPLETE** ✓

---

# 13. Trade Opportunity Slice

## Type

HITL

## Blocked By

12

## User Stories Covered

User Story: Understanding Trade Opportunities

## Deliverables

* opportunity highlighting
* rumor-linked indicators
* profitability emphasis states
* visual recommendation logic

## Visible Outcome

Players quickly understand profitable trades.

## Success Criteria

* profitable routes discoverable visually
* emphasis feels atmospheric rather than gamified

## Status

**COMPLETE** ✓

---

# 14. Ledger Journal Slice

## Type

HITL

## Blocked By

2

## User Stories Covered

User Story: Tracking Rumors
User Story: Reflecting On Journey

## Deliverables

* ledger page renderer
* layered scraps/pins
* page transitions
* rumor card composition

## Visible Outcome

Ledger feels collectible and intimate.

## Success Criteria

* journal presentation feels physical
* rumor browsing emotionally rewarding

## Status

**COMPLETE**

---

# 15. Chronicle Timeline Slice

## Type

HITL

## Blocked By

14

## User Stories Covered

User Story: Reflecting On Journey

## Deliverables

* timeline renderer
* route history
* encounter summaries
* companion notes

## Visible Outcome

Player journey develops historical weight.

## Success Criteria

* long histories remain performant
* timeline remains emotionally readable

## Status

**COMPLETE** ✓

---

# 16. Companion Attachment Slice

## Type

HITL

## Blocked By

4, 11

## User Stories Covered

User Story: Companion Attachment

## Deliverables

* portrait-driven layout
* mood overlays
* relationship visualization
* responsive companion text layout

## Visible Outcome

Companions become emotional anchors.

## Success Criteria

* portraits dominate screen
* companions feel memorable

## Status

**COMPLETE** ✓

---

# 17. Companion Commentary Slice

## Type

HITL

## Blocked By

16

## User Stories Covered

User Story: Checking In

## Deliverables

* gameplay commentary hooks
* contextual dialogue renderer
* cooldown handling
* commentary selection logic

## Visible Outcome

Party reacts dynamically to the world.

## Success Criteria

* commentary feels varied
* reactions reinforce travel fantasy

## Status

**COMPLETE** ✓

---

# 18. Atmospheric FX Infrastructure

## Type

AFK

## Blocked By

5

## User Stories Covered

Supports all atmospheric stories.

## Deliverables

* particle emitter system
* lifecycle manager
* render batching
* biome particle presets
* reduced-motion scaling

## Visible Outcome

Ambient environmental motion becomes scalable.

## Success Criteria

* particles performant on mid-range devices
* reduced-motion support works globally

## Status

**COMPLETE** ✓

## Slice 19 — Audio & Haptics Integration

**Objective:** Warm, tactile feedback that reinforces interactions. Ambient audio establishes atmosphere. Never intrusive.

**Modules:** `core/audio`, `core/haptics`

### Files to create

```
core/audio/
  AudioManager.kt
  AudioEventBus.kt
  AudioPreferences.kt

core/haptics/
  HapticManager.kt
```

### AudioEvent model

```kotlin
sealed class AudioEvent {
    object StepBankTick         : AudioEvent()   // soft tick every 500 steps added
    object TravelBegin          : AudioEvent()   // footstep swell
    object TownArrival          : AudioEvent()   // warm bell chord
    object MarketBuy            : AudioEvent()   // coin clink
    object MarketSell           : AudioEvent()   // richer coin clink
    object LedgerOpen           : AudioEvent()   // page rustle
    object RumorReceived        : AudioEvent()   // parchment scratch
    object EncounterStart       : AudioEvent()   // tension sting
    object EncounterSuccess     : AudioEvent()   // resolve swell
    object EncounterFailure     : AudioEvent()   // low thud
    object BondIncrease         : AudioEvent()   // gentle warm tone
}
```

### AudioManager

Use `SoundPool` for all short one-shot sounds. Use a single `MediaPlayer` for the ambient biome loop.

```kotlin
class AudioManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prefs: AudioPreferences
) {
    private val pool = SoundPool.Builder().setMaxStreams(4).build()
    private val soundIds = mutableMapOf<AudioEvent, Int>()
    private var ambientPlayer: MediaPlayer? = null

    init {
        // Load assets synchronously on a background thread via coroutine in the caller
        soundIds[TravelBegin]     = pool.load(context, R.raw.travel_begin, 1)
        soundIds[TownArrival]     = pool.load(context, R.raw.town_arrival, 1)
        // ... all events
    }

    fun play(event: AudioEvent) {
        if (!prefs.sfxEnabled) return
        soundIds[event]?.let { id -> pool.play(id, prefs.sfxVolume, prefs.sfxVolume, 0, 0, 1f) }
    }

    fun startAmbient(biome: Biome) {
        val resId = biome.ambientResId ?: return
        ambientPlayer?.release()
        ambientPlayer = MediaPlayer.create(context, resId).apply {
            isLooping = true
            setVolume(0f, 0f)
            start()
        }
        fadeAmbientIn()
    }

    fun stopAmbient() = fadeAmbientOut { ambientPlayer?.release(); ambientPlayer = null }

    private fun fadeAmbientIn() { /* coroutine stepping volume 0→prefs.ambientVolume over 800ms */ }
    private fun fadeAmbientOut(onComplete: () -> Unit) { /* coroutine stepping volume down, then onComplete */ }
}
```

### HapticManager

```kotlin
class HapticManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prefs: AudioPreferences
) {
    private val vibrator = context.getSystemService(Vibrator::class.java)

    fun perform(effect: HapticEffect) {
        if (!prefs.hapticsEnabled || vibrator == null) return
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return  // predefined effects require API 29
        val ve = when (effect) {
            HapticEffect.SOFT_TAP -> VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
            HapticEffect.CONFIRM  -> VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
            HapticEffect.REWARD   -> VibrationEffect.createWaveform(longArrayOf(0,40,60,80), intArrayOf(0,120,0,200), -1)
            HapticEffect.ERROR    -> VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK)
        }
        vibrator.vibrate(ve)
    }
}
```

### Action mapping

| User action | Audio event | Haptic |
|-------------|-------------|--------|
| Tap "Travel" | `TravelBegin` | `SOFT_TAP` |
| Town arrival | `TownArrival` | `REWARD` |
| Buy good | `MarketBuy` | `CONFIRM` |
| Sell good | `MarketSell` | `CONFIRM` |
| Open Ledger | `LedgerOpen` | `SOFT_TAP` |
| Rumor received | `RumorReceived` | `SOFT_TAP` |
| Bond increases | `BondIncrease` | `REWARD` |
| Insufficient steps | — | `ERROR` |

Call `audioManager.play(...)` and `hapticManager.perform(...)` from the ViewModel after the state mutation succeeds, not optimistically. This prevents audio/haptic feedback on operations that fail.

### Settings additions

Add to the existing settings screen:

- "Sound effects" toggle → `AudioPreferences.sfxEnabled`
- "Ambient sounds" toggle → `AudioPreferences.ambientEnabled`
- "Haptic feedback" toggle → `AudioPreferences.hapticsEnabled`
- Volume sliders for SFX and ambient (range 0–100, step 1)

All changes apply immediately (no restart required). Disable the volume slider for SFX when SFX is toggled off.

### Acceptance criteria

- All audio events play within 50ms of the triggering UI action on D04
- Ambient crossfade produces no audible pop or gap when the biome changes
- Haptics fire on API 29+ devices; silently no-op on API 26–28
- Disabling SFX in settings immediately silences all one-shot sounds (verified by toggling mid-session)
- Ambient loop restarts correctly after a phone call interruption (`AudioFocusChangeListener` handles `AUDIOFOCUS_LOSS_TRANSIENT`)

**COMPLETE** ✓

---

## Slice 20 — Accessibility & Motion Safety

**Objective:** The atmospheric UI must be fully usable for users with motion sensitivity, visual needs, or motor differences.

**Scope:** All feature modules. Shared infrastructure in `core/designsystem`.

### Files to create / modify

```
core/designsystem/
  ReduceMotionProvider.kt       ← new
  AccessibilityPreferences.kt  ← new

(modify all feature screens to add content descriptions and motion guards)
```

### Reduced motion

```kotlin
val LocalReduceMotion = compositionLocalOf { false }

@Composable
fun ReduceMotionProvider(content: @Composable () -> Unit) {
    val systemReduce = LocalContext.current
        .resources.configuration.isNightModeActive  // swap with actual animation scale check below
    // Read system "Remove animations" setting:
    val animatorDuration = Settings.Global.getFloat(
        LocalContext.current.contentResolver,
        Settings.Global.ANIMATOR_DURATION_SCALE, 1f
    )
    val systemReduceMotion = animatorDuration == 0f
    val userOverride by accessibilityPrefs.reduceMotion.collectAsState(initial = false)

    CompositionLocalProvider(
        LocalReduceMotion provides (systemReduceMotion || userOverride),
        content = content
    )
}
```

Place `ReduceMotionProvider` at the root of `AppScaffold`, wrapping all screens.

**Where to guard:**

| Component | Reduced motion behavior |
|-----------|------------------------|
| Parallax background layers | Disable transform animation; render layers at their midpoint static offset |
| Character walk animation | Freeze at the neutral standing frame |
| `ParticleCanvas` | Render nothing (see Slice 18) |
| Screen transitions (NavHost) | Replace slide transitions with `fadeIn + fadeOut` only |
| Bond meter spring | Replace spring with `snap()` spec |
| `AnimatedVisibility` | Keep `fadeIn/fadeOut`; remove `slideIn/slideOut` |

### Content descriptions

Every meaningful non-text element requires a `contentDescription`. Use the following templates:

```kotlin
// Portrait canvas
Modifier.semantics {
    contentDescription = "${companion.name}, ${companion.role}. Bond level ${companion.bond} of ${companion.maxBond}."
}

// Step meter
Modifier.semantics {
    contentDescription = "${bankedSteps.formatSteps()} steps banked. Today: ${todaySteps.formatSteps()} steps walked."
}

// Route card
Modifier.semantics {
    contentDescription = "Travel to ${road.toTown}: costs ${road.cost.formatSteps()} steps. " +
        if (affordable) "Affordable." else "Need ${shortfall.formatSteps()} more steps."
}

// Map town node
Modifier.semantics {
    contentDescription = "${town.name}, ${town.region}. Tap to view routes."
    role = Role.Button
}

// Decorative backgrounds
Modifier.semantics { invisibleToUser() }
```

Run Layout Inspector → "Semantics" view to audit before signoff.

### Touch targets

All tappable elements must meet 48dp × 48dp minimum. Apply:

```kotlin
Modifier.minimumInteractiveComponentSize()
```

This is available in `androidx.compose.material3` and is API 26 compatible. Add it to every custom interactive composable in `core/designsystem`.

### Contrast modes

Add `ContrastMode` enum: `STANDARD`, `HIGH`, `NIGHT`.

- `HIGH`: text colors raised to full `#000000` / `#FFFFFF`; remove any `alpha < 0.9f` from text; border weights increased to 1.5dp
- `NIGHT`: blue channel reduced by 30% across all theme colors; suitable for use before sleep

Expose a `contrastMode` selector in Settings (segmented control, three options). Store in `AccessibilityPreferences` (DataStore). `AppTheme` reads the current mode and selects the matching `ColorScheme` variant.

### Font scaling

All text uses `sp` units. Test at 200% system font scale (`Settings → Display → Font Size → Largest`) and verify:

- Market screen: good names do not truncate below 80% of their string
- Route cards: town name and step cost remain on one line each or wrap gracefully
- Companion screen: quote text wraps without overflowing its container

Fix any overflow by replacing fixed-height containers with `wrapContentHeight()` and adding `maxLines` + `TextOverflow.Ellipsis` as a last resort only where vertical space is genuinely constrained.

### Acceptance criteria

- All interactive elements pass 48dp minimum tap target (verify with Layout Inspector → "Highlight semantics bounds")
- System "Remove animations" → all parallax, walk, and particle animations stop; screen transitions fade only
- TalkBack reads step meter, route cards, portrait canvas, and map nodes without any "unlabeled" announcements
- High contrast mode: all text passes WCAG AA (4.5:1 contrast ratio) — verify with the Accessibility Scanner app
- At 200% font scale: no text truncates to the point of being unreadable in any screen

**COMPLETE** ✓

---

## Slice 21 — Performance Hardening

**Objective:** Stable 60fps on D04 (Pixel 4a) as the baseline device. No memory growth during play sessions.

**Scope:** All rendering-heavy modules. Work is profiling-driven — instrument first, then fix.

### Profiling setup

Enable Composition tracing in debug builds by adding to `app/build.gradle`:

```groovy
debugImplementation("androidx.compose.runtime:runtime-tracing:1.0.0-beta01")
```

Capture traces: Android Studio → Profiler → CPU → System Trace → start, interact for 30 seconds, stop. Look for frames exceeding 16ms on the main thread.

### Recomposition audit (do in this order)

1. Open Layout Inspector → "Recomposition counts" while the Journey screen is idle (no walking). Any composable recomposing more than once every 5 seconds while idle is a bug — trace the state read that's causing it.

2. `StepMeter` must not recompose on every step event. Gate recomposition with `derivedStateOf`:

```kotlin
val meterSegment by remember {
    derivedStateOf { (bankedSteps / 500).coerceAtMost(MAX_SEGMENTS) }
}
```
`StepMeter` reads `meterSegment` — it only recomposes when the segment count changes, not on every step.

3. The particle canvas is the highest-churn composable. Wrap it in a dedicated `Box` with `key("particles")` so its recomposition scope is isolated from the rest of the scene.

4. Use `mutableStateListOf<Particle>()` (not `mutableStateOf(List<Particle>)`) for the particle backing list. Structural changes (add/remove) trigger recomposition; content changes via index assignment do not.

### Asset memory

- All scene background art must be pre-rasterized to WebP at `@2x` resolution (780 × 780px for the scene viewport). Do not decode SVG at runtime. Store in `res/drawable-xxhdpi/`.
- Walk cycle sprite sheets: one `ImageBitmap` per companion, not one per frame. Implement `SpriteSheetPainter(sheet, frameWidth, frameHeight, currentFrame)` that calls `drawImage` with a `srcOffset` computed from `currentFrame`.
- Use Coil `AsyncImage` for any art loaded at runtime (companion portraits from assets). Configure with `diskCachePolicy = CachePolicy.ENABLED` and `memoryCachePolicy = CachePolicy.ENABLED`.
- Chronicle list items must not hold references to `Bitmap` objects. Store icon tokens (enum) in `ChronicleEntry`, resolve to drawables in the composable.

### Frame diagnostics (debug only)

Add a `FrameRateMonitor` composable in `AppScaffold`, gated on `BuildConfig.DEBUG`:

```kotlin
@Composable
fun FrameRateMonitor() {
    LaunchedEffect(Unit) {
        var lastNs = System.nanoTime()
        while (true) {
            withFrameNanos { nowNs ->
                val frameMs = (nowNs - lastNs) / 1_000_000
                if (frameMs > 16) Log.w("FrameDrop", "Frame took ${frameMs}ms")
                lastNs = nowNs
            }
        }
    }
}
```

### Target benchmarks on D04 (Pixel 4a)

| Scenario | Target |
|----------|--------|
| Journey screen idle | 60fps, main thread CPU < 4% |
| Walking animation active | 60fps, CPU < 7% |
| 24 particles active | 60fps, CPU < 9% |
| Chronicle list scroll (200 entries) | 60fps, no dropped frames |
| Market screen first composition | < 180ms to first pixel |
| Cold start → first interactive frame | < 800ms |

Measure CPU with `adb shell top -d 1 -p $(adb shell pidof com.wanderingledger)`. Measure frame times with System Trace.

### Acceptance criteria

- `StepFidelityBenchmarkTest` passes with no regressions after rendering changes
- No `FrameDrop` warnings during a 2-minute play session on D04 in release build (`./gradlew assembleRelease`)
- Heap size does not grow monotonically during a 5-minute session — verify with Memory Profiler "Record Java/Kotlin allocations", looking for retained `Bitmap` or `Path` leaks
- Cold start time < 800ms on D04 (use `adb shell am start -W` to measure)
- Particle system: zero allocations per frame after 2-second warm-up (Memory Profiler confirms)