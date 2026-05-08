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

---

# 19. Audio & Haptics Integration Slice

## Type

HITL

## Blocked By

2

## User Stories Covered

Supports all emotional UX goals.

## Deliverables

* audio event hooks
* haptic manager
* interaction presets
* environmental hooks

## Visible Outcome

Interface gains tactile and sensory feedback.

## Success Criteria

* haptics subtle and warm
* audio hooks reusable without rewrites

---

# 20. Accessibility & Motion Safety Slice

## Type

AFK

## Blocked By

18

## User Stories Covered

Supports all user stories.

## Deliverables

* reduced motion settings
* contrast scaling
* font scaling
* overlay readability safeguards

## Visible Outcome

Atmospheric UI remains usable.

## Success Criteria

* readability preserved under all atmospheric states
* motion-sensitive users protected

---

# 21. Performance Hardening Slice

## Type

AFK

## Blocked By

All rendering-heavy slices

## User Stories Covered

Supports all stories.

## Deliverables

* render profiling
* texture memory optimization
* recomposition optimization
* lazy asset loading
* frame diagnostics

## Visible Outcome

App remains stable under production asset load.

## Success Criteria

* stable 60fps on target devices
* no major memory spikes
* no visible hitching during transitions

---
