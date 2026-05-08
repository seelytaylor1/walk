# Wandering Ledger — Art Production Backlog

This backlog contains only artist-facing production tasks.

Engineering responsibilities (rendering systems, animation playback, interaction logic, transitions, optimization, etc.) are intentionally excluded.

Artists are responsible for:

* illustration production
* animation frame creation
* texture creation
* iconography
* visual language consistency
* exported assets matching technical requirements

All assets should support the emotional direction defined in the UX specification:

* storybook
* cozy
* naturalistic
* warm
* literary
* atmospheric 

---

# GLOBAL ART DIRECTION RULES

## Visual Tone

Required influences:

* watercolor fantasy illustration
* illuminated manuscripts
* parchment journals
* woodcut printmaking
* cozy travel scenes
* hand-painted RPG backgrounds

Avoid:

* hyper-polished corporate UI
* mobile-game monetization aesthetics
* neon palettes
* sterile gradients
* anime gacha presentation
* grimdark realism

---

# MASTER TECHNICAL REQUIREMENTS

## Export Standards

### Static UI Assets

* Format: PNG
* Color Space: sRGB
* Resolution: 2x target Android density minimum
* Transparency required where appropriate

---

### Large Environment Illustrations

* Format: PNG layered export package
* Minimum Width: 4096px
* Aspect Ratio: 16:9 safe composition
* Separate foreground/midground/background layers

---

### Animated Sprite Sheets

* Format: PNG sprite sheets
* Power-of-two dimensions preferred
* Transparent background
* Include frame timing documentation

---

### Texture Assets

* Format: seamless PNG
* Minimum Size: 1024x1024

---

### Iconography

* SVG preferred
* PNG fallback at:

  * 64px
  * 128px
  * 256px

---

### Delivery Requirements

Each asset batch must include:

* source file
* exported production assets
* layer naming consistency
* palette notes
* intended animation notes (if relevant)

---

# EPIC A — VISUAL LANGUAGE FOUNDATION

---

## WL-ART-001 — Create Master Color Script

### Goal

Define the emotional palette of the game.

### Deliverables

Color scripts for:

* daylight travel
* dusk roads
* moonlit camp
* snowy routes
* rainy travel
* warm taverns
* bustling markets

### Technical Requirements

* PSD or Krita source
* Palette swatches exported separately
* Hex/RGB reference sheet included

### Acceptance Criteria

* Every palette feels warm and atmospheric
* No neon or modern-app coloration

---

## WL-ART-002 — Create Storybook Typography Package

### Goal

Define literary visual identity.

### Deliverables

Selections and examples for:

* chapter headings
* body copy
* handwritten annotations
* map labels
* market signage

### Technical Requirements

* Licensing cleared for commercial use
* Font files delivered
* Styling guide included

---

## WL-ART-003 — Create Decorative UI Motif Library

### Goal

Establish reusable ornamental language.

### Deliverables

* ink dividers
* decorative corners
* parchment edges
* wax seals
* chapter flourishes
* icon frames
* manuscript embellishments

### Technical Requirements

* SVG preferred
* Transparent PNG fallback

---

# EPIC B — JOURNEY SCREEN ART

The emotional centerpiece of the app. 

---

## WL-ART-004 — Paint Forest Route Environment Set

### Goal

Create cozy forest travel biome.

### Deliverables

Layered environment set:

* foreground foliage
* road plane
* distant trees
* sky layer
* atmosphere overlays

Variants:

* day
* dusk
* night
* rain

### Technical Requirements

* 4096px minimum width
* Layer-separated PSD
* Foreground/mid/background split

### Acceptance Criteria

* Supports parallax composition
* Scene readable behind UI overlays

---

## WL-ART-005 — Paint Mountain Route Environment Set

### Deliverables

* cliff paths
* distant peaks
* fog valleys
* stone roads
* alpine vegetation

Variants:

* dawn
* storm
* snowfall
* moonlight

### Acceptance Criteria

* Distinct silhouette language from forest biome

---

## WL-ART-006 — Paint Coastline Route Environment Set

### Deliverables

* cliff roads
* sea horizon
* waves
* lighthouses
* drifting gulls

Variants:

* golden sunset
* cloudy weather
* moonlit surf

---

## WL-ART-007 — Paint Grassland Route Environment Set

### Deliverables

* open road
* wheat fields
* wildflowers
* distant windmills
* rolling hills

Variants:

* spring bloom
* autumn harvest
* rainstorm

---

## WL-ART-008 — Paint Snow Route Environment Set

### Deliverables

* snow trails
* pine silhouettes
* lantern warmth
* frozen roads
* snowfall overlays

Variants:

* blizzard
* moonlit snow
* dawn frost

---

## WL-ART-009 — Create Camp Scene Illustration Set

### Goal

Create emotionally rewarding idle scenes.

### Deliverables

Regional camp variants:

* forest campsite
* mountain fire shelter
* roadside wagon camp
* coastal firepit
* snow shelter

Include:

* firelight glow
* cooking props
* tents/bedrolls
* ambient clutter

### Acceptance Criteria

* Camp feels inhabited and restful

---

## WL-ART-010 — Create Party Walking Sprite Sets

### Goal

Support animated travel scenes.

### Deliverables

Sprite sheets for:

* player walk cycle
* companion walk cycles
* idle loops
* camp idle loops

### Technical Requirements

* 8-direction optional
* Minimum 8-frame walk cycle
* Transparent backgrounds
* Frame timing sheet included

---

## WL-ART-011 — Create Companion Camp Interaction Poses

### Deliverables

Companion variants:

* warming hands
* reading
* sleeping
* cooking
* lookout stance
* conversation poses

### Acceptance Criteria

* Companions feel alive while idle

---

## WL-ART-012 — Create Step-Energy Meter Concepts

### Goal

Replace fitness UI metaphors.

### Deliverables

Finalized concept art for:

* lantern meter
* footprint reservoir
* provision satchel
* travel strength visualizations

### Technical Requirements

* Layer-separated
* Animation-ready decomposition

---

## WL-ART-013 — Create Illustrated Route Progress Assets

### Deliverables

* road ribbons
* waypoint icons
* inns
* encounter markers
* animated footstep frames
* caravan markers

---

# EPIC C — ATMOSPHERIC EFFECT ASSETS

---

## WL-ART-014 — Create Particle Texture Library

### Deliverables

* fog textures
* ember sprites
* leaf sprites
* rain streaks
* snow particles
* dust motes

### Technical Requirements

* Transparent PNG
* Tileable where applicable
* Multiple density variants

---

## WL-ART-015 — Paint Lighting Overlay Collection

### Deliverables

Overlay sets for:

* dawn warmth
* dusk amber
* moonlight blue
* candlelight glow
* storm darkening
* fog diffusion

### Technical Requirements

* Full-screen overlays
* Transparent blend-ready exports

---

# EPIC D — MAP ART

---

## WL-ART-016 — Paint World Map Master Illustration

### Goal

Create collectible parchment world map.

### Deliverables

* illustrated terrain
* roads
* mountains
* forests
* coastlines
* decorative border work

### Technical Requirements

* Minimum 6000px wide
* Layer-separated terrain groups

### Acceptance Criteria

* Feels hand-authored and exploratory

---

## WL-ART-017 — Create Animated Map Decoration Set

### Deliverables

* cloud layers
* wave loops
* caravan markers
* birds
* drifting banners

### Technical Requirements

* Loop-ready frame exports

---

## WL-ART-018 — Create Map Iconography Set

### Deliverables

Icons for:

* towns
* ruins
* roads
* danger
* rumors
* inns
* caravans

### Technical Requirements

* SVG preferred

---

# EPIC E — TOWN ART

---

## WL-ART-019 — Paint Town Establishing Illustrations

### Goal

Create memorable arrivals.

### Deliverables

One illustration per town showing:

* skyline
* architecture
* activity
* lighting mood
* environmental storytelling

### Technical Requirements

* 4096px minimum width
* Layer-separated exports

---

## WL-ART-020 — Create Town District Card Art

### Deliverables

Illustrations for:

* Market
* Tavern
* Guild
* Rumors
* Stables
* Departures

### Acceptance Criteria

* Immediately readable visually

---

## WL-ART-021 — Create Town Theme Packs

### Deliverables

Per-town:

* signage motifs
* banners
* local symbols
* decorative framing
* palette references

---

# EPIC F — MARKET ART

---

## WL-ART-022 — Paint Goods Illustration Library

### Goal

Create tactile trade goods.

### Deliverables

Illustrations for:

* grain
* salt
* herbs
* textiles
* spices
* tools
* contraband goods

### Technical Requirements

* Transparent background
* Consistent lighting angle
* Multiple rarity variants optional

---

## WL-ART-023 — Create Merchant UI Decoration Set

### Deliverables

* wooden signage
* scales
* crates
* shelves
* cloth backdrops
* merchant table textures

---

## WL-ART-024 — Create Trade Opportunity Visual FX

### Deliverables

* warm glows
* scarcity markers
* rumor seals
* excitement accents

### Technical Requirements

* Transparent overlay assets

---

# EPIC G — LEDGER ART

---

## WL-ART-025 — Create Ledger Page Template Library

### Goal

Create collectible journal feel.

### Deliverables

Page templates:

* clean parchment
* annotated page
* weathered page
* stitched inserts
* pinned notes
* folded scraps

### Technical Requirements

* Layer-separated
* Tile-safe parchment textures

---

## WL-ART-026 — Create Rumor Card Illustration Set

### Deliverables

Illustrated rumor motifs:

* smuggling
* famine
* festival
* war
* monster sightings
* caravan shortages

---

## WL-ART-027 — Create Journal Decoration Asset Pack

### Deliverables

* wax seals
* sketches
* ink blots
* stamps
* bookmarks
* marginalia

---

# EPIC H — COMPANION ART

---

## WL-ART-028 — Paint Companion Portrait Set

### Goal

Create emotional attachment anchors.

### Deliverables

Large painted portraits for each companion:

* neutral
* happy
* tired
* worried
* amused

### Technical Requirements

* 2048px minimum portrait height
* Layer-separated facial elements preferred

---

## WL-ART-029 — Create Companion Expression Sheet

### Deliverables

* facial variants
* reaction poses
* dialogue expressions

---

## WL-ART-030 — Create Companion Silhouette & Travel Variants

### Deliverables

* travel silhouettes
* camp silhouettes
* mounted variants optional
* weather gear variants

---

# EPIC I — NAVIGATION & UI ICONOGRAPHY

---

## WL-ART-031 — Create Hand-Inked Navigation Icon Set

### Deliverables

Icons for:

* Journey
* Map
* Town
* Ledger
* Company

States:

* inactive
* active
* highlighted

### Technical Requirements

* SVG preferred
* Pixel-clean at mobile sizes

---

## WL-ART-032 — Create UI Control Illustration Set

### Deliverables

* buttons
* toggles
* tabs
* sliders
* modal decorations
* notification frames

---

# EPIC J — AUDIO DIRECTION REFERENCES

---

## WL-ART-033 — Create Audio Moodboards & Timing References

### Goal

Help future audio implementation.

### Deliverables

Annotated references for:

* footsteps
* rain
* market chatter
* fire ambience
* page turns
* companion presence

### Acceptance Criteria

* Clearly communicates intended emotional soundscape
