# Wandering Ledger — Domain Glossary

## Core Concepts

**PlayerState** — The single persistent record of the player's character: gold, current town, banked steps, inventory slots, lifetime steps, and completed trade count.

**BankedSteps** — Steps recorded by the device that have been credited to the player and are available to spend on road travel.

**RoadSegment** — A directed connection between two Towns with a step cost, a narrative distance label, and an event pool.

**EventPool** — The set of Encounter types possible on a given RoadSegment. One Encounter is drawn per journey.

**Encounter** — A road event that resolves deterministically during travel based on a seed and the active party composition. Outcomes may include gold change, bond change, or narrative text.

**Town** — A named location with a biome, a market, a Reputation score, and a story state.

**Reputation** — A per-Town score (0–100) that increases by completing Orders issued by that Town. Higher Reputation unlocks additional Goods in that Town's market. Permanently earned — does not decay.

**Good** — A tradeable item with a base value and a contraband flag. Each Good is abundant in one Town and scarce in another.

**TownPrice** — The current buy and sell prices for a Good at a specific Town, driven by supply level (Scarce / Normal / Abundant).

**Rumor** — A market tip generated on arrival at a Town. Rumors expire after a fixed number of visits and are approximately 50% false.

**Companion** — A recruitable character with a role, a base stat, a bond level, and an active state. At most 3 companions may be active at once.

**CompanionRole** — The mechanical archetype of a Companion. Determines which encounters and game systems the companion influences.

| Role | Core Bonus |
|---|---|
| Scout | Step cost reduction |
| Fighter | Combat power in hostile encounters |
| Rogue | Trade advantage in merchant encounters |

**Bond** — A companion relationship stat (0–5) earned through town interactions and road encounters. Higher bond amplifies the companion's role-specific numeric bonus.

**CampState** — The passive rest state that activates when the player has not traveled for 5 minutes and has at least 100 banked steps.

**Order** — A town-issued task that asks the player to buy or deliver specific Goods. Completing an Order increases the player's Reputation with the issuing Town. Two types exist in v1: Delivery and Route. Orders persist until completed or expired; a town refills its board to a cap of 3 on each player arrival.

**Delivery Order** — An Order asking the player to bring a quantity of a Scarce Good to the issuing Town. Rewards +5 Reputation on completion.

**Route Order** — An Order asking the player to take a quantity of an Abundant Good from the issuing Town to a specified destination Town. Rewards +8 Reputation on completion.

**Ledger** — The persistent event log of travel arrivals, encounters, and notable trade events.
