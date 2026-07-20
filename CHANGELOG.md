# Changelog

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
