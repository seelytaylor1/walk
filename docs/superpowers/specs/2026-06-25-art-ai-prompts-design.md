# Design: AI Generation Prompts for Art Assets

**Date**: 2026-06-25
**Status**: Approved

## Problem

All 33 art tasks in `specs/001-wandering-ledger/art-requirements.md` are outstanding with no assets produced. The artist will use ChatGPT (DALL-E 3) or Gemini to generate assets and needs a prompt for each one.

## Decision

Add a `### AI Prompt` section to each art task block in `art-requirements.md`. Prompts are self-contained — style language is baked into every prompt so any single prompt can be copy-pasted directly into a chat UI without needing a separate style reference.

## Prompt Structure

Every prompt follows this anatomy:

1. **Style prefix** (fixed across all prompts): watercolor fantasy, illuminated manuscript, hand-painted RPG background, warm/cozy/atmospheric/literary
2. **Scene description** (asset-specific): what is depicted, composition, lighting, key elements
3. **Avoid clause** (fixed): no neon, no corporate UI, no anime gacha, no photorealism

## Variants

Assets with multiple variants (e.g., day/dusk/night/rain for environment sets) get one labeled sub-prompt per variant under the same `### AI Prompt` section.

## Scope

~33 tasks × ~2.5 variants average ≈ 80–90 individual prompts.

## What Stays Out of Prompts

Technical export specs (resolution, file format, layer naming) are not included in prompts — those live in the existing Technical Requirements sections and are post-generation concerns.

## Visual Tone Reference

Required influences baked into every prompt:
- Watercolor fantasy illustration
- Illuminated manuscripts
- Parchment journals
- Woodcut printmaking
- Cozy travel scenes
- Hand-painted RPG backgrounds

Avoided in every prompt:
- Hyper-polished corporate UI
- Neon palettes
- Anime gacha presentation
- Grimdark realism
- Photorealism
