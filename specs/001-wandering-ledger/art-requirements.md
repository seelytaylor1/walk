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

### AI Prompt

**Daylight travel:**
Watercolor fantasy illustration in the style of an illuminated manuscript and hand-painted RPG background. Warm, cozy, atmospheric, and literary. A sunlit country road at midday — golden amber light on dusty ochre earth, sage green hedgerows lining both sides, soft cerulean sky with cream clouds, dappled shadows warm as parchment. Wide landscape composition. Visible brushwork, natural pigment texture. No neon colors, no corporate UI, no anime gacha aesthetic, no photorealism.

**Dusk roads:**
Watercolor fantasy illustration in the style of an illuminated manuscript and hand-painted RPG background. Warm, cozy, atmospheric, and literary. A road at dusk — deep amber horizon bleeding into a violet-rose sky, burnt sienna tree silhouettes lining the path, lantern gold highlights catching the last light, cool purple shadows pooling on the road. Wide landscape composition. Visible brushwork, natural pigment texture. No neon colors, no corporate UI, no anime gacha aesthetic, no photorealism.

**Moonlit camp:**
Watercolor fantasy illustration in the style of an illuminated manuscript and hand-painted RPG background. Warm, cozy, atmospheric, and literary. A moonlit camp at night — deep indigo sky overhead, silver-blue moonlight washing over a small tent, warm firelight amber glowing at the fire's heart, cool violet shadows at the edges of the scene, ivory canvas catching the glow. Wide landscape composition. Visible brushwork, natural pigment texture. No neon colors, no corporate UI, no anime gacha aesthetic, no photorealism.

**Snowy routes:**
Watercolor fantasy illustration in the style of an illuminated manuscript and hand-painted RPG background. Warm, cozy, atmospheric, and literary. A snow-covered road in winter — pale blue-white snow underfoot, steel grey clouds massing overhead, dark pine green silhouettes on either side, a warm lantern amber glow from a distant shelter set against deep cold blue shadows. Wide landscape composition. Visible brushwork, natural pigment texture. No neon colors, no corporate UI, no anime gacha aesthetic, no photorealism.

**Rainy travel:**
Watercolor fantasy illustration in the style of an illuminated manuscript and hand-painted RPG background. Warm, cozy, atmospheric, and literary. A rainy road — slate grey sky, deep olive foliage dripping with rain, wet mud brown road reflecting the overcast light, muted teal puddles, soft diffused white light through the clouds. Wide landscape composition. Visible brushwork, natural pigment texture. No neon colors, no corporate UI, no anime gacha aesthetic, no photorealism.

**Warm taverns:**
Watercolor fantasy illustration in the style of an illuminated manuscript and hand-painted RPG background. Warm, cozy, atmospheric, and literary. A tavern interior at night — deep mahogany wood beams and bar, firelight amber and orange from the hearth, warm cream candlelight on the tables, rich burgundy curtains framing the windows, soot-darkened ceiling above. Wide or portrait composition. Visible brushwork, natural pigment texture. No neon colors, no corporate UI, no anime gacha aesthetic, no photorealism.

**Bustling markets:**
Watercolor fantasy illustration in the style of an illuminated manuscript and hand-painted RPG background. Warm, cozy, atmospheric, and literary. A bustling outdoor market at noon — terracotta awnings overhead, saffron-colored spices in open stalls, rich indigo cloth bolts hanging from hooks, sun-bleached stone underfoot, warm midday light filling the square. Wide landscape composition. Visible brushwork, natural pigment texture. No neon colors, no corporate UI, no anime gacha aesthetic, no photorealism.

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

### AI Prompt

**Chapter headings:**
Watercolor fantasy illustration in the style of an illuminated manuscript and hand-painted RPG background. Warm, cozy, atmospheric, and literary. A decorative chapter heading lettered in a medieval calligraphic style, with ornate drop capital, ink flourishes, and red-and-gold rubrication on aged parchment. The text reads "Chapter One: The Road Ahead." Visible ink texture, hand-lettered feel. No neon colors, no corporate UI, no anime gacha aesthetic, no photorealism.

**Handwritten annotations:**
Watercolor fantasy illustration in the style of an illuminated manuscript and hand-painted RPG background. Warm, cozy, atmospheric, and literary. A page of aged parchment with handwritten journal annotations in sepia ink — small cursive notes in the margins, underlines, arrows pointing to sketches, small pressed flowers tucked in. Intimate and personal. Visible ink texture, natural paper grain. No neon colors, no corporate UI, no anime gacha aesthetic, no photorealism.

**Map labels:**
Watercolor fantasy illustration in the style of an illuminated manuscript and hand-painted RPG background. Warm, cozy, atmospheric, and literary. Sample map label lettering in a hand-inked cartographic style — small caps for region names, italic script for rivers and roads, compass rose label, all on aged parchment. Ink texture visible, slightly uneven letterforms. No neon colors, no corporate UI, no anime gacha aesthetic, no photorealism.

**Market signage:**
Watercolor fantasy illustration in the style of an illuminated manuscript and hand-painted RPG background. Warm, cozy, atmospheric, and literary. A collection of hand-painted wooden market signs — "Spices & Herbs," "Dry Goods," "Fine Cloth" — each in a different folk-art lettering style with simple painted borders and decorative motifs. Warm wood tones, slightly worn paint. No neon colors, no corporate UI, no anime gacha aesthetic, no photorealism.

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

### AI Prompt

**Ink dividers:**
Watercolor fantasy illustration in the style of an illuminated manuscript. Warm, cozy, atmospheric, and literary. A collection of hand-inked horizontal dividers — feather quill lines, vine-and-leaf borders, simple knotwork bands, dotted pen rules — arranged on white/transparent background. Black ink, varied line weights, slight irregularity. No neon colors, no corporate UI, no anime gacha aesthetic, no photorealism.

**Decorative corners:**
Watercolor fantasy illustration in the style of an illuminated manuscript. Warm, cozy, atmospheric, and literary. Four ornate corner ornaments in ink — interlaced knotwork, floral vine, geometric manuscript style, and simple pen-flourish. Black ink on transparent background, suitable for framing a page or card. No neon colors, no corporate UI, no anime gacha aesthetic, no photorealism.

**Parchment edges:**
Watercolor and mixed media in the style of an aged manuscript. Warm, cozy, and literary. A set of torn and deckled parchment edge textures — ragged horizontal tears, frayed vertical sides, burnt edges — on transparent background. Realistic aged paper texture, sepia and cream tones. No neon colors, no corporate UI, no photorealism.

**Wax seals:**
Watercolor fantasy illustration in the style of an illuminated manuscript. Warm, cozy, atmospheric. A collection of five wax seals in deep crimson, forest green, midnight blue, bronze, and black wax — each stamped with a different emblem: a compass rose, a crossed quill and sword, a castle tower, a merchant scale, a crescent moon. Richly textured wax surface, impressed design. No neon colors, no corporate UI, no anime gacha aesthetic, no photorealism.

**Chapter flourishes:**
Watercolor fantasy illustration in the style of an illuminated manuscript. Warm, cozy, and literary. A set of hand-inked calligraphic flourishes and ornamental swashes — scrolling end-marks, pen-flourish tail ornaments, leaf-curl dividers — on transparent background. Black ink, natural line variation. No neon colors, no corporate UI, no anime gacha aesthetic, no photorealism.

**Icon frames:**
Watercolor fantasy illustration in the style of an illuminated manuscript. Warm, cozy, atmospheric, and literary. A collection of ornate circular and rectangular frames for icons — rope borders, vine borders, knotwork borders, simple ruled borders with corner flourishes — on transparent background. Ink and light watercolor, parchment warmth. No neon colors, no corporate UI, no anime gacha aesthetic, no photorealism.

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

### AI Prompt

**Day:**
Watercolor fantasy illustration in the style of a hand-painted RPG background and illuminated manuscript. Warm, cozy, atmospheric, and literary. A sunlit forest road at midday — dappled golden light through ancient oak canopy, wildflowers along the verge, a cobblestone path curving gently into soft distance, foreground ferns and roots, misty blue-green background trees. Wide 16:9 landscape composition. Visible brushwork, natural pigment texture. No neon colors, no corporate UI, no anime gacha aesthetic, no photorealism.

**Dusk:**
Watercolor fantasy illustration in the style of a hand-painted RPG background and illuminated manuscript. Warm, cozy, atmospheric, and literary. A forest road at dusk — amber and rose light filtering sideways through silhouetted trees, long shadows across the path, fireflies beginning to appear, warm haze in the distance, deep violet undergrowth. Wide 16:9 landscape composition. Visible brushwork, natural pigment texture. No neon colors, no corporate UI, no anime gacha aesthetic, no photorealism.

**Night:**
Watercolor fantasy illustration in the style of a hand-painted RPG background and illuminated manuscript. Warm, cozy, atmospheric, and literary. A moonlit forest road at night — silver moonlight cutting through a dark canopy, pale road stones glowing softly, deep indigo tree silhouettes, glowing mushrooms and distant fireflies, a single lantern hung from a branch. Wide 16:9 landscape composition. Visible brushwork, natural pigment texture. No neon colors, no corporate UI, no anime gacha aesthetic, no photorealism.

**Rain:**
Watercolor fantasy illustration in the style of a hand-painted RPG background and illuminated manuscript. Warm, cozy, atmospheric, and literary. A forest road in the rain — soft grey light through heavy foliage, rain streaks blurring the background, puddles reflecting the canopy, deep greens and slate greys, steam rising from the wet path. Wide 16:9 landscape composition. Visible brushwork, natural pigment texture. No neon colors, no corporate UI, no anime gacha aesthetic, no photorealism.

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

### AI Prompt

**Dawn:**
Watercolor fantasy illustration in the style of a hand-painted RPG background and illuminated manuscript. Warm, cozy, atmospheric, and literary. A mountain cliff path at dawn — pale rose and gold light cresting distant snowy peaks, fog pooled in the valley far below, stone steps carved into the mountainside, hardy alpine flowers at the path's edge, a distant traveler silhouette. Wide 16:9 landscape composition. Visible brushwork, natural pigment texture. No neon colors, no corporate UI, no anime gacha aesthetic, no photorealism.

**Storm:**
Watercolor fantasy illustration in the style of a hand-painted RPG background and illuminated manuscript. Dramatic, atmospheric, and literary. A mountain road under a storm — dark roiling clouds, dramatic light breaks through grey, rain-slicked stone path, a distant inn light warm against the threatening sky, wind-bent trees clinging to the cliff face. Wide 16:9 landscape composition. Visible brushwork, natural pigment texture. No neon colors, no corporate UI, no anime gacha aesthetic, no photorealism.

**Snowfall:**
Watercolor fantasy illustration in the style of a hand-painted RPG background and illuminated manuscript. Cozy, atmospheric, and literary. A mountain road in gentle snowfall — soft white flakes blurring the background peaks, pine silhouettes dusted in white, stone road barely visible under fresh snow, warm amber light from a distant shelter, muted blue-white palette. Wide 16:9 landscape composition. Visible brushwork, natural pigment texture. No neon colors, no corporate UI, no anime gacha aesthetic, no photorealism.

**Moonlight:**
Watercolor fantasy illustration in the style of a hand-painted RPG background and illuminated manuscript. Mysterious, cozy, atmospheric, and literary. A mountain path under a full moon — silver light on grey stone, ink-black silhouette peaks against a deep indigo sky, stars scattered thickly, fog clinging to the valley below, a single torch at a distant waypost. Wide 16:9 landscape composition. Visible brushwork, natural pigment texture. No neon colors, no corporate UI, no anime gacha aesthetic, no photorealism.

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

### AI Prompt

**Golden sunset:**
Watercolor fantasy illustration in the style of a hand-painted RPG background and illuminated manuscript. Warm, cozy, atmospheric, and literary. A coastal cliff road at golden sunset — blazing amber and rose sky reflected in the sea, a lighthouse silhouetted against the glow, gulls drifting on warm air, waves catching the light far below the cliff edge, wildflowers at the path side. Wide 16:9 landscape composition. Visible brushwork, natural pigment texture. No neon colors, no corporate UI, no anime gacha aesthetic, no photorealism.

**Cloudy weather:**
Watercolor fantasy illustration in the style of a hand-painted RPG background and illuminated manuscript. Cozy, atmospheric, and literary. A coastal cliff road under a soft overcast sky — silver-grey sea, pale diffused light, whitecaps on the water, a lighthouse with its beam cutting through light fog, green sea-grass bending in the wind. Wide 16:9 landscape composition. Visible brushwork, natural pigment texture. No neon colors, no corporate UI, no anime gacha aesthetic, no photorealism.

**Moonlit surf:**
Watercolor fantasy illustration in the style of a hand-painted RPG background and illuminated manuscript. Mysterious, atmospheric, and literary. A coastal cliff road under moonlight — a broad silver moon path across a dark sea, waves catching white light, the lighthouse lamp glowing amber, dark cliffs and pale road, a single ship's light far at sea. Wide 16:9 landscape composition. Visible brushwork, natural pigment texture. No neon colors, no corporate UI, no anime gacha aesthetic, no photorealism.

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

### AI Prompt

**Spring bloom:**
Watercolor fantasy illustration in the style of a hand-painted RPG background and illuminated manuscript. Warm, cozy, atmospheric, and literary. An open country road through spring grasslands — wildflowers crowding the verge in pink and yellow, bright green rolling hills, white clouds drifting overhead, distant windmill sails turning, a gentle lane curving toward the horizon. Wide 16:9 landscape composition. Visible brushwork, natural pigment texture. No neon colors, no corporate UI, no anime gacha aesthetic, no photorealism.

**Autumn harvest:**
Watercolor fantasy illustration in the style of a hand-painted RPG background and illuminated manuscript. Warm, cozy, atmospheric, and literary. A country road through autumn harvest fields — golden wheat on both sides, rich amber and sienna hills in the distance, a horse-drawn cart on the road, a windmill catching the warm afternoon light, fallen leaves on the path. Wide 16:9 landscape composition. Visible brushwork, natural pigment texture. No neon colors, no corporate UI, no anime gacha aesthetic, no photorealism.

**Rainstorm:**
Watercolor fantasy illustration in the style of a hand-painted RPG background and illuminated manuscript. Atmospheric and literary. A grassland road under a rainstorm — dark bruised clouds sweeping in from the horizon, rain blurring the distant hills, bending grass, a lone tree with a traveler sheltering beneath it, dramatic light breaking through at the horizon's edge. Wide 16:9 landscape composition. Visible brushwork, natural pigment texture. No neon colors, no corporate UI, no anime gacha aesthetic, no photorealism.

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

### AI Prompt

**Blizzard:**
Watercolor fantasy illustration in the style of a hand-painted RPG background and illuminated manuscript. Atmospheric and literary. A snow road in a blizzard — near-whiteout conditions, dark pine silhouettes barely visible, a traveler hunched against the wind, a distant lantern warm amber through the driving snow, the road itself nearly lost. Wide 16:9 landscape composition. Visible brushwork, natural pigment texture. No neon colors, no corporate UI, no anime gacha aesthetic, no photorealism.

**Moonlit snow:**
Watercolor fantasy illustration in the style of a hand-painted RPG background and illuminated manuscript. Cozy, atmospheric, and literary. A snow road under a clear winter moon — deep blue-white snow, black pine silhouettes stark against a star-filled indigo sky, moonlight making the road glow silver, a lantern on a post, breath-mist in the cold air. Wide 16:9 landscape composition. Visible brushwork, natural pigment texture. No neon colors, no corporate UI, no anime gacha aesthetic, no photorealism.

**Dawn frost:**
Watercolor fantasy illustration in the style of a hand-painted RPG background and illuminated manuscript. Warm, cozy, atmospheric, and literary. A snow road at dawn — pale pink and gold light on pristine frost, every pine needle rimmed in ice crystals, the road just beginning to be traced in lilac shadow, a distant village with smoke rising from chimneys, cold air and quiet beauty. Wide 16:9 landscape composition. Visible brushwork, natural pigment texture. No neon colors, no corporate UI, no anime gacha aesthetic, no photorealism.

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

### AI Prompt

**Forest campsite:**
Watercolor fantasy illustration in the style of a hand-painted RPG background and illuminated manuscript. Warm, cozy, atmospheric, and literary. A cozy forest campsite at night — a crackling fire surrounded by bedrolls and packs, a pot simmering on the fire, lanterns hung from low branches, firelight dancing on nearby tree trunks, a sense of safety and rest. Wide 16:9 composition. Visible brushwork, warm amber palette. No neon colors, no corporate UI, no anime gacha aesthetic, no photorealism.

**Mountain fire shelter:**
Watercolor fantasy illustration in the style of a hand-painted RPG background and illuminated manuscript. Warm, cozy, atmospheric, and literary. A rough stone shelter on a mountain pass — a fire burning inside a low stone windbreak, bedrolls spread on pine boughs, a stewpot, cloaks drying on sticks, the dark peaks outside, warmth against the cold. Wide 16:9 composition. Visible brushwork, warm amber and cool grey contrast. No neon colors, no corporate UI, no anime gacha aesthetic, no photorealism.

**Roadside wagon camp:**
Watercolor fantasy illustration in the style of a hand-painted RPG background and illuminated manuscript. Warm, cozy, atmospheric, and literary. A roadside camp beside a covered merchant wagon — a fire with a spit roast, barrels and crates stacked nearby, lanterns on wagon hooks, a dog curled near the fire, the dark road visible in the background. Wide 16:9 composition. Visible brushwork, warm firelight palette. No neon colors, no corporate UI, no anime gacha aesthetic, no photorealism.

**Coastal firepit:**
Watercolor fantasy illustration in the style of a hand-painted RPG background and illuminated manuscript. Warm, cozy, atmospheric, and literary. A camp on a coastal cliff — a stone firepit with driftwood fire, bedrolls on flat rock, fish drying on a makeshift rack, the moon over the sea in the background, the sound of distant waves implied. Wide 16:9 composition. Visible brushwork, warm fire against cool sea tones. No neon colors, no corporate UI, no anime gacha aesthetic, no photorealism.

**Snow shelter:**
Watercolor fantasy illustration in the style of a hand-painted RPG background and illuminated manuscript. Warm, cozy, atmospheric, and literary. A camp in winter — a small tent half-buried in snow, firelight glowing amber from inside through the canvas, snow banked around the walls for insulation, a pair of boots left outside, snowfall continuing gently. Wide 16:9 composition. Visible brushwork, warm amber glow against deep blue-white cold. No neon colors, no corporate UI, no anime gacha aesthetic, no photorealism.

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

### AI Prompt

*Generate one frame per generation, then vary the pose for the next frame.*

**Player walk cycle — mid-stride:**
Watercolor fantasy illustration in the style of an illuminated manuscript and hand-painted RPG character art. Warm, cozy, atmospheric, and literary. A cloaked traveler mid-stride in profile, left-facing — weight forward on the front foot, back leg extended, arms in a natural swing, cloak flowing. Simple, readable silhouette, parchment-warm palette, expressive hand-painted style. Transparent background. No neon colors, no corporate UI, no anime gacha aesthetic, no photorealism.

**Player walk cycle — contact:**
Watercolor fantasy illustration in the style of an illuminated manuscript and hand-painted RPG character art. Warm, cozy, atmospheric, and literary. A cloaked traveler at foot-contact in a walk cycle, left-facing profile — heel just touching down, body upright, opposite arm forward. Simple, readable silhouette, parchment-warm palette. Transparent background. No neon colors, no corporate UI, no anime gacha aesthetic, no photorealism.

**Companion walk cycle — mid-stride:**
Watercolor fantasy illustration in the style of an illuminated manuscript and hand-painted RPG character art. Warm, cozy, atmospheric, and literary. A companion character mid-stride in profile, left-facing — distinct silhouette from a cloaked traveler, perhaps taller or differently geared, natural walking pose. Transparent background, warm hand-painted style. No neon colors, no corporate UI, no anime gacha aesthetic, no photorealism.

**Idle — standing rest:**
Watercolor fantasy illustration in the style of an illuminated manuscript and hand-painted RPG character art. Warm, cozy, atmospheric, and literary. A cloaked traveler standing at rest in profile — relaxed posture, weight on one foot, hands at sides or resting on a walking staff, head slightly bowed. Transparent background, parchment-warm palette, readable silhouette. No neon colors, no corporate UI, no anime gacha aesthetic, no photorealism.

**Camp idle — seated by fire:**
Watercolor fantasy illustration in the style of an illuminated manuscript and hand-painted RPG character art. Warm, cozy, atmospheric, and literary. A traveler seated beside a campfire — legs crossed, hands resting in lap, firelight painting their face amber, expression restful and at ease. Transparent background, warm firelight palette. No neon colors, no corporate UI, no anime gacha aesthetic, no photorealism.

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

### AI Prompt

**Warming hands:**
Watercolor fantasy illustration in the style of an illuminated manuscript and hand-painted RPG character art. Warm, cozy, atmospheric, and literary. A companion character crouched by a campfire, hands extended toward the warmth, face lit amber by the firelight, expression content and at rest. Transparent background, painterly style. No neon colors, no corporate UI, no anime gacha aesthetic, no photorealism.

**Reading:**
Watercolor fantasy illustration in the style of an illuminated manuscript and hand-painted RPG character art. Warm, cozy, atmospheric, and literary. A companion character seated cross-legged, holding a small book close to a lantern, brow slightly furrowed in concentration, pack leaned against a tree beside them. Transparent background, painterly style. No neon colors, no corporate UI, no anime gacha aesthetic, no photorealism.

**Sleeping:**
Watercolor fantasy illustration in the style of an illuminated manuscript and hand-painted RPG character art. Warm, cozy, atmospheric, and literary. A companion character asleep on a bedroll, cloak pulled over as a blanket, face peaceful, one hand tucked under their head, dim firelight in the scene. Transparent background, painterly style. No neon colors, no corporate UI, no anime gacha aesthetic, no photorealism.

**Cooking:**
Watercolor fantasy illustration in the style of an illuminated manuscript and hand-painted RPG character art. Warm, cozy, atmospheric, and literary. A companion character tending a small cooking pot over a camp fire, stirring carefully, steam rising, expression focused and domestic. Transparent background, painterly style. No neon colors, no corporate UI, no anime gacha aesthetic, no photorealism.

**Lookout stance:**
Watercolor fantasy illustration in the style of an illuminated manuscript and hand-painted RPG character art. Warm, cozy, atmospheric, and literary. A companion character standing at the edge of camp, arms crossed or hand on sword hilt, scanning the dark with calm alertness, back to the firelight. Transparent background, painterly style. No neon colors, no corporate UI, no anime gacha aesthetic, no photorealism.

**Conversation poses:**
Watercolor fantasy illustration in the style of an illuminated manuscript and hand-painted RPG character art. Warm, cozy, atmospheric, and literary. A set of companion character poses for dialogue — gesturing while talking, listening with arms folded, laughing, looking thoughtful. Each pose on transparent background, consistent character design, painterly style. No neon colors, no corporate UI, no anime gacha aesthetic, no photorealism.

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

### AI Prompt

**Lantern meter:**
Watercolor fantasy illustration in the style of an illuminated manuscript and hand-painted RPG UI. Warm, cozy, atmospheric, and literary. A decorative oil lantern used as an energy meter — the flame inside glows brightly at full and dims to a small ember when nearly empty, the glass reservoir shows the fuel level visually. Aged brass fittings, hand-painted detail, transparent background. No neon colors, no corporate UI, no anime gacha aesthetic, no photorealism.

**Footprint reservoir:**
Watercolor fantasy illustration in the style of an illuminated manuscript and hand-painted RPG UI. Warm, cozy, atmospheric, and literary. A decorative step-energy meter shaped like a trail of footprints leading into a circular reservoir — prints fill in as steps accumulate, empty prints shown as ghost outlines. Parchment warmth, ink illustration style, transparent background. No neon colors, no corporate UI, no anime gacha aesthetic, no photorealism.

**Provision satchel:**
Watercolor fantasy illustration in the style of an illuminated manuscript and hand-painted RPG UI. Warm, cozy, atmospheric, and literary. A worn leather travel satchel used as an inventory/energy meter — the flap shows fullness through visible bulging and straining buckles at full, lying flat and empty when depleted. Hand-stitched detail, aged leather texture, transparent background. No neon colors, no corporate UI, no anime gacha aesthetic, no photorealism.

**Travel strength visualization:**
Watercolor fantasy illustration in the style of an illuminated manuscript and hand-painted RPG UI. Warm, cozy, atmospheric, and literary. A concept showing travel readiness as a glowing road ribbon — brightly lit and vibrant at full energy, dimming and narrowing toward the edges when depleted. Parchment warmth, painterly, transparent background. No neon colors, no corporate UI, no anime gacha aesthetic, no photorealism.

---

## WL-ART-013 — Create Illustrated Route Progress Assets

### Deliverables

* road ribbons
* waypoint icons
* inns
* encounter markers
* animated footstep frames
* caravan markers

### AI Prompt

**Road ribbons:**
Watercolor fantasy illustration in the style of an illuminated manuscript and hand-painted RPG map. Warm, cozy, atmospheric, and literary. A set of hand-drawn road ribbon elements — straight sections, curves, crossroads, cobblestone texture details — for composing a route on a parchment map. Ink outline with watercolor fill, aged parchment warmth, transparent background. No neon colors, no corporate UI, no anime gacha aesthetic, no photorealism.

**Waypoint icons:**
Watercolor fantasy illustration in the style of an illuminated manuscript and hand-painted RPG map. Warm, cozy, atmospheric, and literary. A set of waypoint marker icons for a travel map — a milestone stone, a crossroads signpost, a distance marker, a resting stone — all hand-inked on transparent background. Consistent scale and ink weight. No neon colors, no corporate UI, no anime gacha aesthetic, no photorealism.

**Inn markers:**
Watercolor fantasy illustration in the style of an illuminated manuscript and hand-painted RPG map. Warm, cozy, atmospheric, and literary. A set of inn marker icons for a travel map — a cozy inn sign hanging from a post, a small house with firelight window, a bed symbol in medieval manuscript style — hand-inked on transparent background. No neon colors, no corporate UI, no anime gacha aesthetic, no photorealism.

**Encounter markers:**
Watercolor fantasy illustration in the style of an illuminated manuscript and hand-painted RPG map. Warm, cozy, atmospheric, and literary. A set of encounter marker icons for a travel map — a crossed-swords symbol, a question mark in an ornate frame, a beast footprint, a storm cloud — hand-inked on transparent background. Slightly foreboding but still cozy in style. No neon colors, no corporate UI, no anime gacha aesthetic, no photorealism.

**Animated footstep frames:**
Watercolor fantasy illustration in the style of an illuminated manuscript. Warm, cozy, atmospheric, and literary. A single boot print in ink and watercolor — dark and fresh at the heel and toe, fading slightly at the edges, on a transparent background. Warm earthen tones. Generate one print per run; vary ink density and completeness across generations to build up a step-by-step appearance sequence. No neon colors, no corporate UI, no anime gacha aesthetic, no photorealism.

**Caravan markers:**
Watercolor fantasy illustration in the style of an illuminated manuscript and hand-painted RPG map. Warm, cozy, atmospheric, and literary. A set of caravan marker icons for a travel map — a covered wagon side view, a merchant cart, a pack mule, a caravan group silhouette — hand-inked on transparent background. Readable at small map scale. No neon colors, no corporate UI, no anime gacha aesthetic, no photorealism.

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

### AI Prompt

**Fog textures:**
Watercolor fantasy illustration in the style of a hand-painted RPG background. Atmospheric and literary. A set of soft fog texture overlays — wispy, diffuse, tileable — in cool grey-white tones on a transparent background. Painterly, natural, no hard edges. Multiple density variants (light mist, moderate fog, heavy fog). No neon colors, no corporate UI, no photorealism.

**Ember sprites:**
Watercolor fantasy illustration in the style of an illuminated manuscript. Warm, cozy, and atmospheric. A sprite sheet of individual ember and spark particles — small glowing orange and amber dots with motion blur trails, on transparent background. Hot center, soft glow edges, varied sizes. No neon colors, no corporate UI, no photorealism.

**Leaf sprites:**
Watercolor fantasy illustration in the style of an illuminated manuscript. Warm, cozy, atmospheric, and literary. A set of individual falling leaf sprites — oak, maple, and generic broadleaf shapes — in autumn amber, rust, and gold tones, on transparent background. Hand-painted, natural shapes, varied rotation states. No neon colors, no corporate UI, no photorealism.

**Rain streaks:**
Watercolor and ink illustration in the style of a hand-painted RPG background. Atmospheric. A set of rain streak overlays — thin diagonal streaks of varying density, light drizzle to heavy downpour — on transparent background. Soft, natural, no harsh digital lines. Multiple density variants. No neon colors, no corporate UI, no photorealism.

**Snow particles:**
Watercolor fantasy illustration in the style of an illuminated manuscript. Cozy, atmospheric. A set of individual snowflake and snow particle sprites — soft white flakes of varying sizes with gentle blur, on transparent background. Natural, organic shapes, some with faint watercolor detail. No neon colors, no corporate UI, no photorealism.

**Dust motes:**
Watercolor fantasy illustration in the style of an illuminated manuscript. Warm, cozy, atmospheric. A set of dust mote particle sprites — soft golden motes drifting in warm light, tiny, translucent, on transparent background. The kind seen in a shaft of sunlight through a tavern window. Painterly, gentle. No neon colors, no corporate UI, no photorealism.

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

### AI Prompt

**Dawn warmth:**
Watercolor fantasy illustration in the style of a hand-painted RPG background. Warm, atmospheric. A full-screen soft lighting overlay for dawn — pale rose and gold radiance from the lower center, fading to lighter tones at the edges, transparent background. Painterly, blend-ready, no hard edges. No neon colors, no corporate UI, no photorealism.

**Dusk amber:**
Watercolor fantasy illustration in the style of a hand-painted RPG background. Warm, atmospheric. A full-screen soft lighting overlay for dusk — deep amber and orange warmth from the lower left, fading to cool violet at the upper right, transparent background. Painterly, blend-ready, no hard edges. No neon colors, no corporate UI, no photorealism.

**Moonlight blue:**
Watercolor fantasy illustration in the style of a hand-painted RPG background. Mysterious, atmospheric. A full-screen soft lighting overlay for moonlight — cool silver-blue radiance from upper center, fading to deeper indigo at the lower edges, transparent background. Painterly, blend-ready, no hard edges. No neon colors, no corporate UI, no photorealism.

**Candlelight glow:**
Watercolor fantasy illustration in the style of a hand-painted RPG background. Warm, cozy, atmospheric. A full-screen soft vignette overlay for candlelight interiors — warm amber center glow, deep brown-black vignette toward the corners, transparent background. Painterly, blend-ready, no hard edges. No neon colors, no corporate UI, no photorealism.

**Storm darkening:**
Watercolor fantasy illustration in the style of a hand-painted RPG background. Dramatic, atmospheric. A full-screen soft lighting overlay for stormy weather — muted grey-green cast over the full screen, darker at the upper edges, faint greenish tinge of approaching storm, transparent background. Painterly, blend-ready, no hard edges. No neon colors, no corporate UI, no photorealism.

**Fog diffusion:**
Watercolor fantasy illustration in the style of a hand-painted RPG background. Atmospheric and literary. A full-screen fog diffusion overlay — soft white-grey haze with painterly irregular density, heaviest at the lower third, transparent background. Tileable horizontally. Painterly, blend-ready, no hard edges. No neon colors, no corporate UI, no photorealism.

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

### AI Prompt

Watercolor fantasy illustration in the style of an illuminated manuscript and hand-drawn cartography. Warm, cozy, atmospheric, and literary. A full parchment world map — hand-illustrated terrain with watercolor mountains, ink-drawn forests, painted coastlines with decorative wave patterns, roads shown as double-line ink paths, town symbols, and an ornate decorative border with compass rose and cartouche. Aged parchment warmth, visible brushwork and ink irregularity, a sense of a world waiting to be explored. Wide landscape format. No neon colors, no corporate UI, no anime gacha aesthetic, no photorealism.

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

### AI Prompt

*Generate one still per generation; vary position slightly across runs to build a looping sequence.*

**Cloud layers:**
Watercolor fantasy illustration in the style of an illuminated manuscript map. Warm, cozy, atmospheric. A single soft watercolor cloud — cream and grey-white tones, natural painterly shape, transparent background. Generate multiple clouds at slightly different positions and shapes to assemble a drifting loop. No neon colors, no corporate UI, no photorealism.

**Wave loops:**
Watercolor fantasy illustration in the style of an illuminated manuscript map. Warm, atmospheric. A single decorative map ocean wave in a still position — blue-grey watercolor, medieval cartographic style, transparent background. Generate at slightly different crest heights to assemble a looping wave animation. No neon colors, no corporate UI, no photorealism.

**Birds:**
Watercolor fantasy illustration in the style of an illuminated manuscript map. Warm, cozy, atmospheric. A single small bird in flight — wings at mid-stroke, silhouette style in dark ink with faint watercolor, transparent background. Tiny enough to decorate a map sea or sky. Generate with wings up, level, and down for a three-frame loop. No neon colors, no corporate UI, no photorealism.

**Drifting banners:**
Watercolor fantasy illustration in the style of an illuminated manuscript map. Warm, cozy, atmospheric. A single small heraldic pennant hanging still — rich jewel-tone fabric color (burgundy, forest green, or midnight blue), ink-drawn detail, transparent background. Generate at slightly different drape angles to assemble a gentle wind-ripple animation. No neon colors, no corporate UI, no photorealism.

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

### AI Prompt

**Towns:**
Watercolor fantasy illustration in the style of an illuminated manuscript map. Warm, cozy, atmospheric. A set of hand-inked town icons for a fantasy map — small walled town, large city with towers, hamlet with church steeple — each on transparent background, consistent ink weight, readable at small scale. No neon colors, no corporate UI, no anime gacha aesthetic, no photorealism.

**Ruins:**
Watercolor fantasy illustration in the style of an illuminated manuscript map. Mysterious, atmospheric. A set of hand-inked ruin icons for a fantasy map — crumbled tower, broken walls, overgrown columns, sunken foundation — each on transparent background, consistent ink weight, slightly foreboding. No neon colors, no corporate UI, no anime gacha aesthetic, no photorealism.

**Danger markers:**
Watercolor fantasy illustration in the style of an illuminated manuscript map. Atmospheric. A set of hand-inked danger marker icons for a fantasy map — a skull-and-crossbones in manuscript style, a beast claw mark, a storm warning symbol, a sword-and-shield warning — each on transparent background. Foreboding but consistent with the cozy-literary tone. No neon colors, no corporate UI, no anime gacha aesthetic, no photorealism.

**Rumors:**
Watercolor fantasy illustration in the style of an illuminated manuscript map. Warm, cozy, atmospheric. A set of hand-inked rumor icons for a fantasy map — a speech bubble with a quill, an eye peeking from a hood, a folded note with a wax seal — each on transparent background. Mysterious but warm. No neon colors, no corporate UI, no anime gacha aesthetic, no photorealism.

**Inns:**
Watercolor fantasy illustration in the style of an illuminated manuscript map. Warm, cozy, atmospheric. A set of hand-inked inn icons for a fantasy map — a hanging tavern sign, a bed symbol, a hearth symbol, a mug icon — each on transparent background, readable at small map scale. No neon colors, no corporate UI, no anime gacha aesthetic, no photorealism.

**Caravans:**
Watercolor fantasy illustration in the style of an illuminated manuscript map. Warm, cozy, atmospheric. A set of hand-inked caravan icons for a fantasy map — a covered wagon, a pack mule, a merchant cart, a caravan trail marker — each on transparent background, consistent ink weight. No neon colors, no corporate UI, no anime gacha aesthetic, no photorealism.

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

### AI Prompt

**Market town (example establishing shot):**
Watercolor fantasy illustration in the style of a hand-painted RPG background and illuminated manuscript. Warm, cozy, atmospheric, and literary. A traveler's first view arriving at a bustling market town — terracotta rooftops, a market square visible through the gate, smoke from bakery chimneys, colorful awnings, people moving in the streets, late afternoon golden light. Wide 16:9 composition. Visible brushwork, warm palette. No neon colors, no corporate UI, no anime gacha aesthetic, no photorealism.

**Mountain trading post (example establishing shot):**
Watercolor fantasy illustration in the style of a hand-painted RPG background and illuminated manuscript. Warm, cozy, atmospheric, and literary. A traveler's first view arriving at a mountain trading post — stone buildings with steep roofs, a watchtower, smoke from forge fires, furs and goods hung outside, dramatic cliff backdrop, cool blue mountain light with warm lamplight below. Wide 16:9 composition. Visible brushwork, cool-warm contrast palette. No neon colors, no corporate UI, no anime gacha aesthetic, no photorealism.

**Coastal port town (example establishing shot):**
Watercolor fantasy illustration in the style of a hand-painted RPG background and illuminated manuscript. Warm, cozy, atmospheric, and literary. A traveler's first view arriving at a coastal port — whitewashed buildings on a hill, boats in the harbor, a lighthouse, fishing nets hung to dry, gulls, warm Mediterranean light. Wide 16:9 composition. Visible brushwork, bright coastal palette. No neon colors, no corporate UI, no anime gacha aesthetic, no photorealism.

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

### AI Prompt

**Market:**
Watercolor fantasy illustration in the style of an illuminated manuscript and hand-painted RPG background. Warm, cozy, atmospheric, and literary. A card illustration of an outdoor market district — colorful stalls, merchants calling out, baskets of goods, hanging scales, a busy crowd. Warm noon light, rich colors. Portrait card format, readable at small size. No neon colors, no corporate UI, no anime gacha aesthetic, no photorealism.

**Tavern:**
Watercolor fantasy illustration in the style of an illuminated manuscript and hand-painted RPG background. Warm, cozy, atmospheric, and literary. A card illustration of a tavern interior — low beamed ceiling, fireplace, round tables, mugs raised, a bard in the corner, warm amber light. Portrait card format, readable at small size. No neon colors, no corporate UI, no anime gacha aesthetic, no photorealism.

**Guild:**
Watercolor fantasy illustration in the style of an illuminated manuscript and hand-painted RPG background. Warm, cozy, atmospheric, and literary. A card illustration of a guild hall — stone interior with a large table, maps pinned to the walls, a notice board, a senior figure in robes behind a desk. Serious but warm. Portrait card format. No neon colors, no corporate UI, no anime gacha aesthetic, no photorealism.

**Rumors:**
Watercolor fantasy illustration in the style of an illuminated manuscript and hand-painted RPG background. Warm, cozy, atmospheric, and literary. A card illustration representing the rumors district — a hooded figure in a dark corner of a tavern, notes passed under a table, a corkboard of pinned messages, candlelight. Mysterious but literary. Portrait card format. No neon colors, no corporate UI, no anime gacha aesthetic, no photorealism.

**Stables:**
Watercolor fantasy illustration in the style of an illuminated manuscript and hand-painted RPG background. Warm, cozy, atmospheric, and literary. A card illustration of the town stables — horses in stalls, hay dust in slanted light, a groom at work, saddles hung on hooks, the smell of the place implied. Portrait card format. No neon colors, no corporate UI, no anime gacha aesthetic, no photorealism.

**Departures:**
Watercolor fantasy illustration in the style of an illuminated manuscript and hand-painted RPG background. Warm, cozy, atmospheric, and literary. A card illustration of the departures gate — the town gate open to a road stretching into distance, a traveler adjusting their pack, morning light, a sense of the journey beginning. Portrait card format. No neon colors, no corporate UI, no anime gacha aesthetic, no photorealism.

---

## WL-ART-021 — Create Town Theme Packs

### Deliverables

Per-town:

* signage motifs
* banners
* local symbols
* decorative framing
* palette references

### AI Prompt

*Generate each element separately — one banner, one seal, one border per run.*

**Market town banner:**
Watercolor fantasy illustration in the style of an illuminated manuscript. Warm, cozy, atmospheric, and literary. A hand-painted market town hanging banner — terracotta and gold cloth, a scales motif at the center, folk-art lettering, slightly worn edges and fraying at the bottom. Transparent background. No neon colors, no corporate UI, no anime gacha aesthetic, no photorealism.

**Market town heraldic seal:**
Watercolor fantasy illustration in the style of an illuminated manuscript. Warm, cozy, atmospheric, and literary. A hand-inked heraldic seal for a market town — a pair of merchant's scales on a simple shield shape, terracotta and gold tones, ink outline, slightly imperfect as if pressed in wax. Transparent background. No neon colors, no corporate UI, no anime gacha aesthetic, no photorealism.

**Market town decorative border:**
Watercolor fantasy illustration in the style of an illuminated manuscript. Warm, cozy, atmospheric, and literary. A decorative page border in a market town style — terracotta and gold vine-and-coin motifs, hand-inked, suitable for framing a card or UI panel. Transparent background. No neon colors, no corporate UI, no anime gacha aesthetic, no photorealism.

**Mountain trading post banner:**
Watercolor fantasy illustration in the style of an illuminated manuscript. Warm, cozy, atmospheric, and literary. A hand-painted mountain trading post banner — steel blue and iron grey cloth, a crossed-axes-over-peak motif, rugged folk-art lettering. Transparent background. No neon colors, no corporate UI, no anime gacha aesthetic, no photorealism.

**Mountain trading post heraldic seal:**
Watercolor fantasy illustration in the style of an illuminated manuscript. Warm, cozy, atmospheric, and literary. A hand-inked heraldic seal for a mountain trading post — a mountain peak with crossed axes on a simple shield, steel blue and iron grey tones, ink outline. Transparent background. No neon colors, no corporate UI, no anime gacha aesthetic, no photorealism.

**Mountain trading post decorative border:**
Watercolor fantasy illustration in the style of an illuminated manuscript. Warm, cozy, atmospheric, and literary. A decorative page border in a mountain trading post style — steel blue and grey geometric knotwork and forge-hammer motifs, hand-inked, suitable for framing a card or UI panel. Transparent background. No neon colors, no corporate UI, no anime gacha aesthetic, no photorealism.

**Coastal port banner:**
Watercolor fantasy illustration in the style of an illuminated manuscript. Warm, cozy, atmospheric, and literary. A hand-painted coastal port banner — sea blue and white cloth, an anchor-on-wave motif, nautical folk-art lettering, salt-worn edges. Transparent background. No neon colors, no corporate UI, no anime gacha aesthetic, no photorealism.

**Coastal port heraldic seal:**
Watercolor fantasy illustration in the style of an illuminated manuscript. Warm, cozy, atmospheric, and literary. A hand-inked heraldic seal for a coastal port town — an anchor rising from a stylized wave on a simple shield, sea blue and white tones, ink outline. Transparent background. No neon colors, no corporate UI, no anime gacha aesthetic, no photorealism.

**Coastal port decorative border:**
Watercolor fantasy illustration in the style of an illuminated manuscript. Warm, cozy, atmospheric, and literary. A decorative page border in a coastal port style — sea blue and white rope-and-wave motifs, hand-inked, suitable for framing a card or UI panel. Transparent background. No neon colors, no corporate UI, no anime gacha aesthetic, no photorealism.

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

### AI Prompt

**Grain:**
Watercolor fantasy illustration in the style of an illuminated manuscript. Warm, cozy, atmospheric, and literary. A detailed painting of a sack of grain — burlap sack tied at the top, grain spilling slightly from a seam, warm golden-straw color, top-left lighting angle. Transparent background, painterly, tactile. No neon colors, no corporate UI, no anime gacha aesthetic, no photorealism.

**Salt:**
Watercolor fantasy illustration in the style of an illuminated manuscript. Warm, cozy, atmospheric, and literary. A detailed painting of a salt trade good — a sealed clay jar or wrapped cloth parcel, white crystal salt visible, top-left lighting angle. Transparent background, painterly, tactile. No neon colors, no corporate UI, no anime gacha aesthetic, no photorealism.

**Herbs:**
Watercolor fantasy illustration in the style of an illuminated manuscript. Warm, cozy, atmospheric, and literary. A detailed painting of a bundle of dried herbs — several varieties tied with twine, green and sage tones, top-left lighting angle. Transparent background, painterly, botanical illustration influence, tactile. No neon colors, no corporate UI, no anime gacha aesthetic, no photorealism.

**Textiles:**
Watercolor fantasy illustration in the style of an illuminated manuscript. Warm, cozy, atmospheric, and literary. A detailed painting of a bolt of cloth trade good — richly colored fabric rolled into a bolt, deep burgundy or indigo, top-left lighting angle. Transparent background, painterly, tactile. No neon colors, no corporate UI, no anime gacha aesthetic, no photorealism.

**Spices:**
Watercolor fantasy illustration in the style of an illuminated manuscript. Warm, cozy, atmospheric, and literary. A detailed painting of a spice trade good — a small ornate jar or wooden box of exotic spice, saffron or deep red pepper color visible, top-left lighting angle. Transparent background, painterly, tactile, slightly precious. No neon colors, no corporate UI, no anime gacha aesthetic, no photorealism.

**Tools:**
Watercolor fantasy illustration in the style of an illuminated manuscript. Warm, cozy, atmospheric, and literary. A detailed painting of a set of trade tools — a hammer, chisel, and file bundled together, iron and wood tones, top-left lighting angle. Transparent background, painterly, tactile. No neon colors, no corporate UI, no anime gacha aesthetic, no photorealism.

**Contraband goods:**
Watercolor fantasy illustration in the style of an illuminated manuscript. Mysterious, atmospheric, and literary. A detailed painting of contraband trade goods — a dark wrapped parcel with a wax seal, rope-bound, slightly ominous, top-left lighting angle. Transparent background, painterly, deliberately ambiguous contents. No neon colors, no corporate UI, no anime gacha aesthetic, no photorealism.

---

## WL-ART-023 — Create Merchant UI Decoration Set

### Deliverables

* wooden signage
* scales
* crates
* shelves
* cloth backdrops
* merchant table textures

### AI Prompt

**Wooden signage:**
Watercolor fantasy illustration in the style of an illuminated manuscript. Warm, cozy, atmospheric, and literary. A set of hand-painted wooden merchant signs — hanging sign with iron bracket, flat counter placard, price tag tied with twine — warm wood tones, hand-lettered style, transparent background. No neon colors, no corporate UI, no anime gacha aesthetic, no photorealism.

**Scales:**
Watercolor fantasy illustration in the style of an illuminated manuscript. Warm, cozy, atmospheric, and literary. A detailed painting of a merchant's balance scale — brass pans, iron arm, ornate counterweight, resting on a wooden surface, top-left lighting. Transparent background, painterly, tactile. No neon colors, no corporate UI, no anime gacha aesthetic, no photorealism.

**Crates:**
Watercolor fantasy illustration in the style of an illuminated manuscript. Warm, cozy, atmospheric, and literary. A set of merchant crates and barrels — stacked wooden crates with rope handles, an open barrel with goods, a tied sack — warm wood and burlap tones, transparent background, consistent lighting. No neon colors, no corporate UI, no anime gacha aesthetic, no photorealism.

**Shelves:**
Watercolor fantasy illustration in the style of an illuminated manuscript. Warm, cozy, atmospheric, and literary. A merchant shelf unit illustration — rough wooden shelves stacked with jars, parcels, and cloth, warm interior light, slightly cluttered and inviting. Transparent background, painterly. No neon colors, no corporate UI, no anime gacha aesthetic, no photorealism.

**Cloth backdrops:**
Watercolor fantasy illustration in the style of an illuminated manuscript. Warm, cozy, atmospheric, and literary. A set of merchant stall cloth backdrop textures — striped canvas awning, draped velvet display cloth, burlap partition — in earthy market colors (saffron, ochre, burgundy), tileable, transparent background edges. No neon colors, no corporate UI, no anime gacha aesthetic, no photorealism.

**Merchant table textures:**
Watercolor fantasy illustration in the style of an illuminated manuscript. Warm, cozy, atmospheric, and literary. A merchant counter table surface texture — worn wood planks with a few stains and scratches, a small inlaid scale mark, warm mid-light. Tileable, transparent background, painterly. No neon colors, no corporate UI, no anime gacha aesthetic, no photorealism.

---

## WL-ART-024 — Create Trade Opportunity Visual FX

### Deliverables

* warm glows
* scarcity markers
* rumor seals
* excitement accents

### Technical Requirements

* Transparent overlay assets

### AI Prompt

**Warm glow:**
Watercolor fantasy illustration in the style of an illuminated manuscript. Warm, cozy, atmospheric. A soft radial glow overlay in warm amber-gold, centered, fading to transparent at the edges — for highlighting a profitable trade opportunity. Painterly, no hard edges, transparent background. No neon colors, no corporate UI, no photorealism.

**Scarcity marker:**
Watercolor fantasy illustration in the style of an illuminated manuscript. Warm, atmospheric, and literary. A hand-illustrated scarcity marker icon — an hourglass nearly run out, or a nearly empty shelf drawn in ink, with a red wax seal stamped "scarce" in a medieval script. Transparent background, ink and watercolor. No neon colors, no corporate UI, no anime gacha aesthetic, no photorealism.

**Rumor seal:**
Watercolor fantasy illustration in the style of an illuminated manuscript. Mysterious, atmospheric, and literary. A hand-illustrated rumor seal — a wax seal cracked open to reveal a scroll hint, or a whispered note folded with a feather seal, ink and deep red wax. Transparent background, painterly detail. No neon colors, no corporate UI, no anime gacha aesthetic, no photorealism.

**Excitement accents:**
Watercolor fantasy illustration in the style of an illuminated manuscript. Warm, cozy, atmospheric. A set of hand-drawn excitement accent marks — a starburst in gold ink, a decorative exclamation flourish, a radiating lines burst in manuscript style — on transparent background. Ink and gold leaf feel. No neon colors, no corporate UI, no anime gacha aesthetic, no photorealism.

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

### AI Prompt

**Clean parchment:**
Watercolor fantasy illustration in the style of an illuminated manuscript. Warm, cozy, atmospheric, and literary. A clean blank parchment page — smooth aged paper in warm cream and ochre, faint natural fiber texture, slight unevenness at the edges, no writing. Transparent background edges, full-page fill. Painterly, natural. No neon colors, no corporate UI, no photorealism.

**Annotated page:**
Watercolor fantasy illustration in the style of an illuminated manuscript. Warm, cozy, atmospheric, and literary. A parchment journal page filled with handwritten notes — ruled lines in faded ink, margin annotations, a small sketch, a highlighted passage, a footnote. Lived-in and personal. Transparent background edges. No neon colors, no corporate UI, no anime gacha aesthetic, no photorealism.

**Weathered page:**
Watercolor fantasy illustration in the style of an illuminated manuscript. Warm, cozy, atmospheric, and literary. A heavily weathered parchment page — water stains, foxing spots, a small tear repaired with a patch, faded ink at the edges, creased from folding. Still readable but clearly well-traveled. Transparent background edges. No neon colors, no corporate UI, no photorealism.

**Stitched inserts:**
Watercolor fantasy illustration in the style of an illuminated manuscript. Warm, cozy, atmospheric, and literary. A parchment page with hand-stitched paper inserts — a folded note sewn into the margin with visible thread, a small extra page tucked and stitched, the stitching imperfect and charming. Transparent background edges. No neon colors, no corporate UI, no photorealism.

**Pinned notes:**
Watercolor fantasy illustration in the style of an illuminated manuscript. Warm, cozy, atmospheric, and literary. A parchment journal page with notes pinned or wax-sealed to it — a small folded message held by a melted wax blob, a torn scrap pinned with a small nail, casting slight shadows. Transparent background edges. No neon colors, no corporate UI, no anime gacha aesthetic, no photorealism.

**Folded scraps:**
Watercolor fantasy illustration in the style of an illuminated manuscript. Warm, cozy, atmospheric, and literary. A set of folded paper and parchment scraps — a folded letter, a torn half-page, a rolled scroll end, a note folded into a triangle — each on transparent background. Aged paper tones, visible fold creases, hand-torn edges. No neon colors, no corporate UI, no photorealism.

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

### AI Prompt

**Smuggling:**
Watercolor fantasy illustration in the style of an illuminated manuscript. Mysterious, atmospheric, and literary. A rumor card illustration for smuggling — hooded figures passing a dark wrapped bundle at night, a docked boat in shadows, lantern light catching a wax seal. Shadowy but literary, not grimdark. Portrait card format. No neon colors, no corporate UI, no anime gacha aesthetic, no photorealism.

**Famine:**
Watercolor fantasy illustration in the style of an illuminated manuscript. Somber, atmospheric, and literary. A rumor card illustration for famine — an empty market stall with bare shelves, a worried merchant, thin crowds, withered grain, subdued warm tones. Literary, not graphic. Portrait card format. No neon colors, no corporate UI, no anime gacha aesthetic, no photorealism.

**Festival:**
Watercolor fantasy illustration in the style of an illuminated manuscript. Warm, cozy, atmospheric, and literary. A rumor card illustration for a festival — bunting strung between buildings, a dancing crowd, musicians on a stage, lanterns lit in daytime, joyful chaos. Vibrant but painterly. Portrait card format. No neon colors, no corporate UI, no anime gacha aesthetic, no photorealism.

**War:**
Watercolor fantasy illustration in the style of an illuminated manuscript. Somber, atmospheric, and literary. A rumor card illustration for distant war — a courier arriving with a sealed letter, soldiers visible in a distant background, worried faces in a market square, banners at half-mast. Literary, not gory. Portrait card format. No neon colors, no corporate UI, no anime gacha aesthetic, no photorealism.

**Monster sightings:**
Watercolor fantasy illustration in the style of an illuminated manuscript. Mysterious, cozy, atmospheric, and literary. A rumor card illustration for monster sightings — frightened travelers pointing at claw marks on a road sign, oversized tracks in mud, a witness with wide eyes at a tavern table. Spooky but cozy, illustrated-children's-book scary. Portrait card format. No neon colors, no corporate UI, no anime gacha aesthetic, no photorealism.

**Caravan shortages:**
Watercolor fantasy illustration in the style of an illuminated manuscript. Warm, atmospheric, and literary. A rumor card illustration for caravan shortages — a near-empty wagon with a worried merchant checking a ledger, fewer goods than expected on shelves, a road stretching into distance with no caravans in sight. Portrait card format. No neon colors, no corporate UI, no anime gacha aesthetic, no photorealism.

---

## WL-ART-027 — Create Journal Decoration Asset Pack

### Deliverables

* wax seals
* sketches
* ink blots
* stamps
* bookmarks
* marginalia

### AI Prompt

**Wax seals:**
Watercolor fantasy illustration in the style of an illuminated manuscript. Warm, cozy, atmospheric, and literary. A set of decorative wax seals for journal pages — different colors and emblems (compass, quill, tower, leaf, coin), each slightly imperfect as if hand-pressed. Transparent background, painterly wax texture. No neon colors, no corporate UI, no anime gacha aesthetic, no photorealism.

**Sketches:**
Watercolor fantasy illustration in the style of an illuminated manuscript. Warm, cozy, atmospheric, and literary. A set of small ink sketches for journal decoration — a quick map doodle, a plant study, a face profile, a landmark tower, a horse — each as if dashed in a travel journal margin. Transparent background, loose ink line. No neon colors, no corporate UI, no photorealism.

**Ink blots:**
Ink illustration in the style of an illuminated manuscript. A set of irregular ink blot marks — small dots, feathered quill splatters, a dragged smear, a corner drip — as would naturally occur in a handwritten journal. Black ink on transparent background, organic shapes. No neon colors, no corporate UI, no photorealism.

**Stamps:**
Watercolor fantasy illustration in the style of an illuminated manuscript. Warm, cozy, atmospheric, and literary. A set of hand-stamped mark decorations for journal pages — a town seal stamp in red ink, a merchant house mark, a customs clearance mark, a "PAID" stamp in a medieval script — on transparent background, slightly uneven as if rubber-stamped by hand. No neon colors, no corporate UI, no anime gacha aesthetic, no photorealism.

**Bookmarks:**
Watercolor fantasy illustration in the style of an illuminated manuscript. Warm, cozy, atmospheric, and literary. A set of decorative bookmark ribbons and paper tabs — a ribbon bookmark in burgundy silk, a leather tab with a stamped compass, a paper strip with handwritten label, a dried pressed flower used as a bookmark. Transparent background. No neon colors, no corporate UI, no anime gacha aesthetic, no photorealism.

**Marginalia:**
Watercolor fantasy illustration in the style of an illuminated manuscript. Warm, cozy, atmospheric, and literary. A set of hand-drawn marginalia decorations — a tiny knight on horseback, a vine spiraling up the margin, a small owl perched on a letter, a pointing finger "manicule," decorative line-end marks. Black ink on transparent background, whimsical and literary. No neon colors, no corporate UI, no photorealism.

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

### AI Prompt

**Companion portrait — neutral:**
Watercolor fantasy illustration in the style of an illuminated manuscript and hand-painted RPG character portrait. Warm, cozy, atmospheric, and literary. A large painted portrait of a human companion — a road-worn but kind traveler in practical clothes and a worn cloak, direct gaze, neutral expression, soft natural background suggesting the road. Painterly, warm palette, no hard digital lines. Portrait format, bust or three-quarter length. No neon colors, no corporate UI, no anime gacha aesthetic, no photorealism.

**Companion portrait — happy:**
Watercolor fantasy illustration in the style of an illuminated manuscript and hand-painted RPG character portrait. Warm, cozy, atmospheric, and literary. The same companion character in a genuinely happy expression — a warm smile reaching the eyes, relaxed posture, a hint of laughter. Painterly, warm palette. Portrait format. No neon colors, no corporate UI, no anime gacha aesthetic, no photorealism.

**Companion portrait — tired:**
Watercolor fantasy illustration in the style of an illuminated manuscript and hand-painted RPG character portrait. Warm, cozy, atmospheric, and literary. The same companion character showing road-weariness — heavy eyes, slight slouch, a yawn half-suppressed, dusty road grime on the cloak. Painterly, muted warm palette. Portrait format. No neon colors, no corporate UI, no anime gacha aesthetic, no photorealism.

**Companion portrait — worried:**
Watercolor fantasy illustration in the style of an illuminated manuscript and hand-painted RPG character portrait. Warm, cozy, atmospheric, and literary. The same companion character with a worried expression — brow furrowed, eyes searching, hands gripped on the cloak, the concern of someone who cares. Painterly, warm palette, slightly cooler tones. Portrait format. No neon colors, no corporate UI, no anime gacha aesthetic, no photorealism.

**Companion portrait — amused:**
Watercolor fantasy illustration in the style of an illuminated manuscript and hand-painted RPG character portrait. Warm, cozy, atmospheric, and literary. The same companion character with an amused expression — a wry half-smile, raised eyebrow, the look of someone who has seen it all and finds this funny. Painterly, warm palette. Portrait format. No neon colors, no corporate UI, no anime gacha aesthetic, no photorealism.

---

## WL-ART-029 — Create Companion Expression Sheet

### Deliverables

* facial variants
* reaction poses
* dialogue expressions

### AI Prompt

*Generate one expression or pose per run. Establish the neutral portrait first, then reference it in follow-up prompts to keep the character consistent.*

**Facial variant — neutral:**
Watercolor fantasy illustration in the style of an illuminated manuscript and hand-painted RPG character portrait. Warm, cozy, atmospheric, and literary. A companion character face close-up — neutral, calm expression, direct gaze, road-worn but kind. Transparent background, painterly, warm palette. No neon colors, no corporate UI, no anime gacha aesthetic, no photorealism.

**Facial variant — happy:**
Watercolor fantasy illustration in the style of an illuminated manuscript and hand-painted RPG character portrait. Warm, cozy, atmospheric, and literary. The same companion character face close-up — a warm, genuine smile reaching the eyes, relaxed brow. Transparent background, painterly, warm palette. No neon colors, no corporate UI, no anime gacha aesthetic, no photorealism.

**Facial variant — sad:**
Watercolor fantasy illustration in the style of an illuminated manuscript and hand-painted RPG character portrait. Warm, cozy, atmospheric, and literary. The same companion character face close-up — downcast eyes, a slight trembling of the lip, quiet sorrow. Transparent background, painterly, muted warm palette. No neon colors, no corporate UI, no anime gacha aesthetic, no photorealism.

**Facial variant — surprised:**
Watercolor fantasy illustration in the style of an illuminated manuscript and hand-painted RPG character portrait. Warm, cozy, atmospheric, and literary. The same companion character face close-up — wide eyes, raised brows, mouth slightly open in genuine surprise. Transparent background, painterly, warm palette. No neon colors, no corporate UI, no anime gacha aesthetic, no photorealism.

**Facial variant — angry:**
Watercolor fantasy illustration in the style of an illuminated manuscript and hand-painted RPG character portrait. Warm, cozy, atmospheric, and literary. The same companion character face close-up — brow furrowed deeply, jaw set, eyes hard but not cruel. Transparent background, painterly, warm palette with slightly cooler tones. No neon colors, no corporate UI, no anime gacha aesthetic, no photorealism.

**Facial variant — thoughtful:**
Watercolor fantasy illustration in the style of an illuminated manuscript and hand-painted RPG character portrait. Warm, cozy, atmospheric, and literary. The same companion character face close-up — gaze slightly averted, brow softly furrowed, lips pressed together in quiet consideration. Transparent background, painterly, warm palette. No neon colors, no corporate UI, no anime gacha aesthetic, no photorealism.

**Reaction pose — startled:**
Watercolor fantasy illustration in the style of an illuminated manuscript and hand-painted RPG character art. Warm, cozy, atmospheric, and literary. The companion character half-body — a startled step back, one hand raised in surprise, weight shifted to the rear foot. Transparent background, painterly. No neon colors, no corporate UI, no anime gacha aesthetic, no photorealism.

**Reaction pose — disagreement:**
Watercolor fantasy illustration in the style of an illuminated manuscript and hand-painted RPG character art. Warm, cozy, atmospheric, and literary. The companion character half-body — arms crossed firmly, chin slightly raised, expression unconvinced. Transparent background, painterly. No neon colors, no corporate UI, no anime gacha aesthetic, no photorealism.

**Reaction pose — delighted:**
Watercolor fantasy illustration in the style of an illuminated manuscript and hand-painted RPG character art. Warm, cozy, atmospheric, and literary. The companion character half-body — hands clasped together, leaning forward, face bright with delight. Transparent background, painterly, warm palette. No neon colors, no corporate UI, no anime gacha aesthetic, no photorealism.

**Reaction pose — pointing urgently:**
Watercolor fantasy illustration in the style of an illuminated manuscript and hand-painted RPG character art. Warm, cozy, atmospheric, and literary. The companion character half-body — arm extended, finger pointing with urgency, expression intent and alert. Transparent background, painterly. No neon colors, no corporate UI, no anime gacha aesthetic, no photorealism.

**Dialogue — speaking:**
Watercolor fantasy illustration in the style of an illuminated manuscript and hand-painted RPG character portrait. Warm, cozy, atmospheric, and literary. The companion character bust portrait — mid-sentence, one hand raised slightly, expression animated and engaged. Transparent background, painterly, warm palette. No neon colors, no corporate UI, no anime gacha aesthetic, no photorealism.

**Dialogue — listening:**
Watercolor fantasy illustration in the style of an illuminated manuscript and hand-painted RPG character portrait. Warm, cozy, atmospheric, and literary. The companion character bust portrait — attentive, head tilted slightly, expression open and receptive. Transparent background, painterly, warm palette. No neon colors, no corporate UI, no anime gacha aesthetic, no photorealism.

**Dialogue — confiding:**
Watercolor fantasy illustration in the style of an illuminated manuscript and hand-painted RPG character portrait. Warm, cozy, atmospheric, and literary. The companion character bust portrait — leaning slightly forward, voice low, expression earnest and private. Transparent background, painterly, warm palette. No neon colors, no corporate UI, no anime gacha aesthetic, no photorealism.

---

## WL-ART-030 — Create Companion Silhouette & Travel Variants

### Deliverables

* travel silhouettes
* camp silhouettes
* mounted variants optional
* weather gear variants

### AI Prompt

**Travel silhouettes:**
Watercolor fantasy illustration in the style of an illuminated manuscript. Warm, cozy, atmospheric, and literary. A set of companion travel silhouettes — side-profile walking poses in a clean ink silhouette style, different stride lengths, cloak billowing, pack on back. Black silhouette on transparent background, expressive and readable at small size. No neon colors, no corporate UI, no anime gacha aesthetic, no photorealism.

**Camp silhouettes:**
Watercolor fantasy illustration in the style of an illuminated manuscript. Warm, cozy, atmospheric, and literary. A set of companion camp silhouettes — seated by a fire, lying down, standing watch, crouching to cook. Black silhouette on transparent background, readable and expressive. No neon colors, no corporate UI, no anime gacha aesthetic, no photorealism.

**Weather gear variants:**
Watercolor fantasy illustration in the style of an illuminated manuscript and hand-painted RPG character art. Warm, cozy, atmospheric, and literary. A companion character shown in weather gear variants — heavy travel cloak pulled tight in rain, fur-lined hood in snow, light summer road gear. Consistent character, transparent background, painterly. No neon colors, no corporate UI, no anime gacha aesthetic, no photorealism.

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

### AI Prompt

**Navigation icons — inactive state:**
Ink illustration in the style of an illuminated manuscript. Warm, cozy, atmospheric. A set of five hand-inked navigation icons — a road winding into distance (Journey), a folded parchment map (Map), a town gate or tower (Town), an open ledger book (Ledger), a group of silhouetted companions (Company). All in a muted grey-brown ink on transparent background, clean at small mobile sizes. Consistent line weight, readable silhouettes. No neon colors, no corporate UI, no anime gacha aesthetic, no photorealism.

**Navigation icons — active state:**
Ink illustration in the style of an illuminated manuscript. Warm, cozy, atmospheric. The same five navigation icons — Journey, Map, Town, Ledger, Company — in an active/selected state. Darker ink with a warm amber or gold accent, slightly bolder weight, the same hand-inked style. Transparent background, clean at mobile sizes. No neon colors, no corporate UI, no anime gacha aesthetic, no photorealism.

**Navigation icons — highlighted state:**
Ink and watercolor illustration in the style of an illuminated manuscript. Warm, cozy, atmospheric. The same five navigation icons — Journey, Map, Town, Ledger, Company — in a highlighted/pressed state. A soft parchment-warm glow or watercolor wash behind each icon, ink detail on top. Transparent background, clean at mobile sizes. No neon colors, no corporate UI, no anime gacha aesthetic, no photorealism.

---

## WL-ART-032 — Create UI Control Illustration Set

### Deliverables

* buttons
* toggles
* tabs
* sliders
* modal decorations
* notification frames

### AI Prompt

**Buttons:**
Watercolor fantasy illustration in the style of an illuminated manuscript. Warm, cozy, atmospheric, and literary. A set of hand-illustrated UI button styles — a primary button as a carved wooden plaque with ink lettering, a secondary button as a parchment tab, a destructive action button with a red wax stamp. Transparent background, painterly, readable. No neon colors, no corporate UI, no anime gacha aesthetic, no photorealism.

**Toggles:**
Watercolor fantasy illustration in the style of an illuminated manuscript. Warm, cozy, atmospheric, and literary. A set of hand-illustrated toggle controls — a small illustrated lever switch in wood and brass, an on/off shown as a lit/unlit lantern, a check mark in manuscript style. Transparent background, painterly. No neon colors, no corporate UI, no anime gacha aesthetic, no photorealism.

**Tabs:**
Watercolor fantasy illustration in the style of an illuminated manuscript. Warm, cozy, atmospheric, and literary. A set of hand-illustrated tab controls — parchment page tabs as if cut from the top of a ledger, active tab raised, inactive tabs flat, inked labels. Transparent background, painterly. No neon colors, no corporate UI, no anime gacha aesthetic, no photorealism.

**Sliders:**
Watercolor fantasy illustration in the style of an illuminated manuscript. Warm, cozy, atmospheric, and literary. A hand-illustrated slider control — a wooden ruled track with a carved bead slider, shown at various positions, inked scale marks. Transparent background, painterly. No neon colors, no corporate UI, no anime gacha aesthetic, no photorealism.

**Modal decorations:**
Watercolor fantasy illustration in the style of an illuminated manuscript. Warm, cozy, atmospheric, and literary. A set of decorative modal dialog frame elements — ornate corner pieces, a header band with manuscript flourishes, a footer ribbon with wax seal accents. Transparent background, ink and watercolor, frame-ready. No neon colors, no corporate UI, no anime gacha aesthetic, no photorealism.

**Notification frames:**
Watercolor fantasy illustration in the style of an illuminated manuscript. Warm, cozy, atmospheric, and literary. A set of notification frame designs — a scrolled parchment banner for announcements, a wax-sealed note frame for alerts, a simple inked border for info toasts. Transparent background, painterly. No neon colors, no corporate UI, no anime gacha aesthetic, no photorealism.

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

### AI Prompt

**Footsteps:**
Watercolor fantasy illustration in the style of an illuminated manuscript and hand-painted RPG background. Warm, cozy, atmospheric, and literary. A close-up of a cobblestone road with fresh boot prints in soft mud at the edge — the texture of worn stone, a dusting of dry grass at the verge, the quiet of a road between villages. Evokes the steady sound of unhurried footsteps. No neon colors, no corporate UI, no anime gacha aesthetic, no photorealism.

**Rain:**
Watercolor fantasy illustration in the style of an illuminated manuscript and hand-painted RPG background. Atmospheric and literary. A rain-soaked scene — heavy drops exploding across a stone surface, rivulets threading between cobbles, a puddle's surface alive with concentric rings, grey-blue diffusion through the downpour. Immersive and layered, evoking the full sound of rainfall. No neon colors, no corporate UI, no photorealism.

**Market chatter:**
Watercolor fantasy illustration in the style of an illuminated manuscript and hand-painted RPG background. Warm, cozy, atmospheric, and literary. A bustling market scene viewed from within the crowd — a merchant mid-call, two buyers in close conversation, a cart wheel turning nearby, a dog weaving between legs, a distant bell above a shop door. The warmth and noise of a living market. No neon colors, no corporate UI, no anime gacha aesthetic, no photorealism.

**Fire ambience:**
Watercolor fantasy illustration in the style of an illuminated manuscript and hand-painted RPG background. Warm, cozy, atmospheric, and literary. A campfire at close range — glowing embers shifting orange and white at the core, a log just beginning to settle, sparks lifting lazily into the dark above, the fire's heat felt in the warm palette. Intimate and crackling. No neon colors, no corporate UI, no photorealism.

**Page turns:**
Watercolor fantasy illustration in the style of an illuminated manuscript. Warm, cozy, atmospheric, and literary. A hand mid-turn on a heavy parchment page — fingers at the corner lifting it, the page curving in arc, light catching the paper grain and showing its thickness and weight. The satisfying mass of an old journal. No neon colors, no corporate UI, no photorealism.

**Companion presence:**
Watercolor fantasy illustration in the style of an illuminated manuscript and hand-painted RPG background. Warm, cozy, atmospheric, and literary. A companion seated quietly by a campfire — head bowed slightly over a book, one hand resting near the warmth, firelight soft on their face, the scene unhurried and intimate. Their presence felt without words. No neon colors, no corporate UI, no anime gacha aesthetic, no photorealism.
