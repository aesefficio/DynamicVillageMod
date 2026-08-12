# Changelog

## v0.8 — Chest Loot

Village-building chests used to spawn **empty** — the structures saved plain chests with no loot
table. v0.8 fixes that and turns chest loot into a data-driven system anyone can use.

### For players
- The mod's village buildings now spawn **randomized, Create-themed loot** in their chests
  (profession-appropriate: miners get ores/drilling materials, train mechanics get track/casing
  parts, plumbers get fluid/copper gear). Loot is modest — cheap materials and a small vanilla
  touch, nothing that skips Create's tech tree. Affects **newly generated** villages.

### For data pack & add-on developers
- **Assign a loot table to any building's chests** with a one-line file at
  `data/<ns>/dynamicvillage/loot/<structure_path>.json` — for your own new buildings *or* to
  **re-loot this mod's** buildings (drop a same-path override; standard data pack priority wins, no
  need to redefine the building).
- The loot table itself is a normal Minecraft loot table — reference any id (yours, this mod's, or
  vanilla). An `override_existing` flag can replace even hand-authored chest loot.
- Under the hood: a `dynamicvillage:chest_loot` structure processor stamps the loot table onto
  chests at village generation (vanilla-style — chests fill on first open). Hand-authored loot
  chests are preserved by default.
- New JSON schema (`schemas/dynamicvillage-loot.schema.json`), a `DATAPACK.md` section, and an
  example in `example-addon` that re-loots one of the mod's buildings.

### Notes
- Loot appears only in **newly generated** villages, and only where a building already has chests.
- Fully backward compatible: buildings without a loot assignment behave exactly as before.

### Also in v0.8 — review fixes

A full pass over the codebase turned up several issues, all fixed here:

- **Job sites for data-defined professions now use blocks, not tags.** `job_site_tag` could never
  work — block tags come from data packs and aren't loaded when professions must be registered, so
  any profession using it silently failed to appear. Replaced with `job_site_blocks` (a list); the
  old field is now rejected with an explanatory error instead of failing quietly.
- **A malformed profession config can no longer crash startup.** Duplicate ids, ids clashing with an
  existing profession, and unregistered job-site blocks are logged and skipped.
- **Loot assignments support `conditions`**, matching trades and buildings.
- **A loot table that no pack provides now warns at startup** instead of silently giving empty chests.
- **Chests saved without block-entity data are now filled** rather than skipped.
- `components` on a trade **cost** now warns that it is ignored (it only ever applied to `result`).
- Trade counts in the startup log no longer over-report when a pack uses `replace`.
- Out-of-range trade levels and unusable POI values are reported instead of being silently clamped.
- Conditions missing a required field now consistently fail closed (a `negate`d one could previously
  pass by accident).
- At `buildingSpawnChancePercent = 100`, the pool weight multiplier dropped from 1000× to 50× —
  the same near-total custom share for a fraction of the memory.
- Removed dead code, unused casts against Create internals, and an orphaned creative-tab
  translation key; corrected the repository URLs in the mod metadata.

## v0.7 — Developer Platform Update

This release turns Create: Dynamic Village from "a mod with a couple of data-driven bits" into a
**platform** that modpack, mod, and data pack developers can build on without touching the mod's
code. Everything a v0.6 pack did still works unchanged; v0.7 adds new, optional power on top.

Builds: `dynamicvillage-0.7-1.21.1.jar` (NeoForge 1.21.1) and `dynamicvillage-0.7-1.20.1.jar`
(Forge 1.20.1). Both were boot-tested on a dedicated server — all trades, buildings, and a
data-defined profession load with no errors.

---

### For data pack & add-on developers

#### Richer trade items — tags, count ranges, and item data
Trade `cost`/`result` entries gained three optional powers:
- **Count ranges** — `"count": { "min": 2, "max": 5 }` rolls a random amount per offer, instead of
  a fixed number.
- **Item tags** — `"tag": "c:ingots/zinc"` resolves to the first item in a tag, so a trade can say
  "whatever zinc ingot this pack provides" instead of hard-coding one mod's item.
- **Components / NBT** on results — attach enchantments, custom names, potions, etc.

*Why it helps:* packs can now express real, interesting trades (enchanted books, randomized
bundles, cross-mod materials) that were impossible before. The tag support in particular makes packs
**portable across mod sets** rather than pinned to one mod's exact item ids.

#### Load conditions
Any trade or building file can carry a `conditions` array (`mod_loaded`, `item_exists`,
`block_exists`, each optionally negated). If the conditions aren't met, the file is skipped.

*Why it helps:* one pack can safely ship content for many mods — e.g. "these trades only load if
Immersive Engineering is installed." It's a **loader-neutral** system the mod evaluates itself
(deliberately *not* NeoForge/Forge `*:conditions`, whose differing keys would break cross-version
parity), so the **exact same JSON works on every Minecraft version**.

#### Merge control — `replace`
Trade files now **append by default** (so any number of packs stack cleanly), with an opt-in
`"replace": true` to fully redefine a profession's trades. Files load in a deterministic, sorted
order.

*Why it helps:* removes the old sharp edge where two packs editing the same profession would
silently clobber each other. Collaboration between packs "just works" now.

#### Data-defined professions (new!)
You can add a **brand-new villager profession** with its own job-site block and trades using only
JSON — no Java. Define it in `config/dynamicvillage/professions/<name>.json` (config, not a data
pack, because professions must register before data packs load — a hard Minecraft constraint), add
its job site to the `acquirable_job_site` tag via a normal data pack, and give it trades through the
usual trade system.

*Why it helps:* this is the big one — the last thing that used to require editing the mod. A modpack
can now ship a complete themed villager (profession + POI + building + trades) as pure data.

#### Tooling — schemas and a worked example
- **JSON schemas** for trade, building, and profession files (`schemas/*.schema.json`) — point your
  editor at them for autocomplete and validation.
- **A runnable example add-on** (`examples/example-addon/`) demonstrating every feature end to end,
  including a new profession.

*Why it helps:* dramatically lowers time-to-first-working-pack. Authors get red squiggles for typos
instead of silent skips, and a copy-paste starting point.

#### Documentation
`DATAPACK.md` was expanded to cover all of the above, and a new `DEVELOPER_SPEC.md` documents the
full contract with acceptance criteria — a shared reference for verifying pack behavior.

---

### Gameplay — built-in trade rebalance

The mod's own villager trades were rebalanced to fix economy exploits and inconsistencies:
- **Fixed an emerald exploit:** the Miner's coal trade was an infinite emerald printer
  (`1 coal → 2 emeralds`); it's now a normal coal sink (`15 coal → 1 emerald`), matching vanilla.
- **Repriced under-costed goods:** brass nuggets, fluid tanks, and large water wheels were far too
  cheap and trivialized Create progression — all brought in line.
- **Removed contradictions:** the crushing-wheel and mechanical-crafter trades had inconsistent
  prices between professions; these were reconciled.

*Why it helps the mod:* villager trading no longer lets players skip Create's tech progression for
free, so the mod feels balanced alongside Create instead of undercutting it.

---

### Compatibility & internals
- **Fully backward compatible:** every valid v0.6 trade/building file loads unchanged.
- **Cross-version parity:** all JSON contracts are identical between the 1.20.1 and 1.21.1 builds,
  with one documented exception — the `components` field, whose contents differ because 1.20.x uses
  item NBT and 1.21.x uses data components.
- **Fail-soft loading:** an invalid file is logged and skipped; it never breaks other files or
  crashes the world. Trades referencing an absent mod's items are skipped with a warning.
- **Observability:** all loading is logged under `[DynamicVillage]`, so pack authors can see exactly
  what loaded, what was skipped, and why.

---

### Known limitations
- New professions are defined in `config/`, not a world data pack — an unavoidable consequence of
  when Minecraft freezes its registries. Their trades and buildings are still normal data packs.
- Server-side loading is verified; live **in-game** confirmation of a villager claiming a new job
  site, and of `components` results rendering in-world, still needs a play session.
