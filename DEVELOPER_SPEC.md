# Create: Dynamic Village — Developer Platform Spec

**Purpose of this document.** This is the shared source of truth for the mod's developer-facing
(data pack / add-on mod) API. It is written to be **verifiable**: every capability below lists its
exact data contract *and* concrete acceptance criteria. If an implementation doesn't satisfy the
checkboxes for a section, that section is not done.

**Scope.** Everything a third-party developer touches without forking the mod: trades, buildings,
professions, conditions, merge rules, diagnostics, and tooling. It does **not** cover the mod's
internal Create integration.

## Status legend

| Mark | Meaning |
|------|---------|
| ✅ **Shipped** | Present and verified in the v0.6 source (`mod-1.21.1-v6`). Listed so we can confirm v7 does not regress it. |
| 🔧 **Proposed** | Planned for v0.7. Not yet implemented. Acceptance criteria define "done". |
| 🧪 **Verify** | A check to run against a build, not a code inspection. |

**Working tree:** `mod-1.21.1-v7` is primary; `mod-1.20.1-v7` is kept in lockstep. All JSON
contracts below are **identical across both MC versions** — only the thin loader/registration Java
differs. v6 folders are frozen as the reference/fallback.

---

## Build status — v0.8 (chest loot)

| Feature | Status |
|---------|--------|
| Data-driven chest loot: `data/<ns>/dynamicvillage/loot/<structure>.json` → `{loot_table, override_existing, conditions}` | ✅ **Built** |
| `dynamicvillage:chest_loot` structure processor (stamps `LootTable`+`LootTableSeed` on empty chests at placement) | ✅ **Built** |
| Re-loot any building via same-path override; preserve hand-authored chests by default | ✅ **Built** |
| 4 modest Create loot tables (miner / train_mechanic / hydraulic / mechanical_engineer) + 10 assignments for the mod's chest buildings | ✅ **Built** |
| `savanna_miner` converted via `override_existing` (no NBT edit) | ✅ **Built** |
| Loot schema + `DATAPACK.md` section + example | ✅ **Built** |

**Verification (NeoForge 1.21.1 server boot):** ✅ `Loaded 10 chest loot assignment(s)`; ✅ all 5 pools injected buildings with the processor attached, no exceptions; ✅ the 4 loot tables passed Minecraft's loot codec (no parse errors); ✅ `Done`. Both trees compile clean. ✅ **Confirmed in-game:** generated village chests roll the loot. ✅ A boot with five deliberately-broken profession configs (unsupported tag field, duplicate id, clashing id, unregistered block, POI-clash) skipped each with one clear message and still reached `Done`.

---

## Build status — v0.7 (developer platform)

| Section | Feature | Status |
|---------|---------|--------|
| §1.2 | Count ranges (`{min,max}`) on any cost/result | ✅ **Built** |
| §1.2 | Item **tag** references (resolve to first tag member) | ✅ **Built** |
| §1.2 | Item **components/NBT** (enchanted books, potions, named items) on `result` | ✅ **Built** — **version-native** (this one field's JSON differs by version): data components on 1.21.x (`DataComponentPatch`), item NBT on 1.20.x (`CompoundTag`). Documented as the single cross-version JSON difference. |
| §4 | Conditions (`mod_loaded` / `item_exists` / `block_exists`, `negate`) on trade **and** building files | ✅ **Built** — as a **loader-neutral** `conditions` block the mod evaluates itself, *not* `neoforge:`/`forge:conditions` (their differing keys/APIs would break cross-version JSON parity). |
| §5 | `replace` flag (append-by-default, opt-in replace, deterministic order) | ✅ **Built** |
| §8 | JSON schemas (`schemas/*.schema.json`, incl. professions) + runnable `examples/example-addon/` pack | ✅ **Built** |
| §3 | Data-driven professions + POIs | ✅ **Built** (config-driven) — defined in `config/dynamicvillage/professions/*.json` (NOT a world data pack: professions/POIs register before data packs load — hard MC constraint). Registers POI + profession at the registry phase; job acquisition needs a data-pack `acquirable_job_site` tag entry (documented). **Registration verified at runtime**; in-game job *acquisition* not yet gameplay-tested. |

**Compilation:** both trees compile clean via `gradlew compileJava` → `BUILD SUCCESSFUL` — `mod-1.21.1-v7` (NeoForge) and `mod-1.20.1-v7` (Forge).

**Runtime verification (NeoForge 1.21.1 dedicated server boot):**
- ✅ All 68 built-in trades + 20 buildings load through the new codecs — proves **backward compatibility** (existing v0.6 JSON on the new richer codec).
- ✅ `Added 20/17/15/16 trade(s)` applied to the four professions at runtime with no exceptions (exercises tag/count-range resolution + offer building).
- ✅ A config-defined test profession registered its POI (16 states) and profession, server reached `Done` with no errors.
- ⏳ **Not yet gameplay-tested:** a live villager actually claiming a dynamic job site, and rendering of `components` results in-world. These need an interactive client session.

---

## 0. The developer-experience contract (design principles)

These are the promises the platform makes. Every feature is designed to keep them. They are the
developer-experience guarantees, stated as testable rules.

| # | Principle | What it means concretely | How we verify |
|---|-----------|--------------------------|---------------|
| P1 | **No Java required** | A developer can add professions, buildings, and trades with only JSON + NBT. | §1–§3 each have a pure-data path. |
| P2 | **Backward compatible** | Every new field is optional with a default; existing v0.6 packs load unchanged. | §7 compatibility checks. |
| P3 | **Fail soft, never crash** | One bad file is logged and skipped; it never breaks other files or the world. | §6 diagnostics + 🧪 malformed-file test. |
| P4 | **Composable / mergeable** | Any number of packs and mods contribute at once without overwriting each other. | §5 merge semantics. |
| P5 | **Cross-mod safe** | A file that references items/blocks from an absent mod is skipped, not fatal. | §1.4 item resolution + §4 conditions. |
| P6 | **Discoverable** | Clear docs (`DATAPACK.md`), a copy-paste example pack, and editor autocomplete (schema). | §8 tooling. |
| P7 | **Observable** | The log states exactly what loaded, what was skipped, and why, tagged `[DynamicVillage]`. | §6 logging contract. |
| P8 | **Namespaced & non-colliding** | Each dev writes under their own namespace; defaults live in the mod's namespace. | §5. |

---

## 1. Villager trades API

Data-driven villager trades. Add new trades, edit/remove the mod's, and target **any** profession
including vanilla ones.

**File location**
```
data/<namespace>/dynamicvillage/trades/<any_name>.json
```

**File shape**
```json
{
  "profession": "dynamicvillage:miner",
  "trades": [ { /* trade object */ } ]
}
```

### 1.1 Trade object — ✅ Shipped fields (v0.6)

| Field | Required | Default | Type | Notes |
|-------|----------|---------|------|-------|
| `level` | yes | — | int | Villager tier that unlocks it. Clamped to **1–5**. |
| `cost` | yes | — | item spec | What the villager takes. |
| `cost2` | no | none | item spec | Optional second input. |
| `result` | yes | — | item spec | What the villager gives. |
| `max_uses` | no | `12` | int | Uses before the trade locks until restock. |
| `xp` | no | `2` | int | Villager XP per trade. |
| `price_multiplier` | no | `0.05` | float | How much demand inflates the price. |

**✅ Shipped item spec (v0.6):** `{ "item": "<id>", "count": <int, default 1> }`.

**Acceptance criteria (regression — must still hold in v7)**
- [ ] A v0.6 trade file loads byte-for-byte unchanged and produces identical offers.
- [ ] `level` outside 1–5 is clamped, not rejected.
- [ ] Omitting `max_uses`/`xp`/`price_multiplier`/`count` applies the documented defaults.
- [ ] Trades are **added to the candidate pool** for that level; the villager still shows a random
      subset (vanilla behavior), i.e. we do not force every trade to appear.
- [ ] 🧪 A file targeting `minecraft:farmer` adds trades to vanilla farmers.

### 1.2 Richer item spec — 🔧 Proposed (v0.7)

Extends the item spec. All additions are optional; the v0.6 `{item,count}` form stays valid (P2).

| Field | Default | Type | Description |
|-------|---------|------|-------------|
| `item` | — | id | Exact item id. Mutually exclusive with `tag`. |
| `tag` | — | id | **Cost only.** Accept any item in this item tag (e.g. `c:ingots/zinc`). |
| `count` | `1` | int **or** `{min,max}` | Fixed count, or a random range rolled per offer. |
| `components` | none | object | Data components / NBT — enchantments, custom name, dye, potion, etc. Applied to `result` (and matched on `cost` where the game supports it). |

Example — sell a randomly-counted, enchanted result for a tag-matched cost:
```json
{
  "level": 3,
  "cost":   { "tag": "c:gems/diamond", "count": { "min": 2, "max": 4 } },
  "result": {
    "item": "minecraft:enchanted_book",
    "components": { "minecraft:stored_enchantments": { "minecraft:efficiency": 3 } }
  },
  "max_uses": 5
}
```

**Acceptance criteria — 🔧**
- [ ] `count` accepts both a bare int and a `{min,max}` object; range is rolled per generated offer.
- [ ] `tag` on a cost accepts any item in the tag; `tag` + `item` in the same spec is a validation
      error (logged, skipped — P3), not a silent pick.
- [ ] `components` on a `result` produces an item stack carrying those components (🧪 enchanted book
      trade appears and is functional).
- [ ] A spec with none of the new fields behaves exactly as v0.6 (P2 regression).
- [ ] An empty tag or a tag with no loaded items is skipped with a specific warning (P3/P7).

### 1.3 Merge & override — see §5.

### 1.4 Cross-mod item resolution — ✅ Shipped

Unknown item id ⇒ that single trade is skipped with a warning; the rest of the file still loads.

- [ ] 🧪 A file referencing a `create:` item on a world without Create loads its `minecraft:` trades
      and skips only the Create ones.

---

## 2. Village buildings API — ✅ Shipped (v0.6)

Inject custom structures into village generation.

**File location**
```
data/<namespace>/dynamicvillage/buildings/<any_name>.json
```

**Fields**

| Field | Required | Default | Description |
|-------|----------|---------|-------------|
| `pool` | yes | — | Target template pool, e.g. `minecraft:village/plains/houses`. **Any** pool works. |
| `structure` | yes | — | NBT template id (`data/<ns>/structure/<path>.nbt`). Needs a jigsaw block at the entrance. |
| `weight` | no | `1` | Share **relative to other custom buildings** in the same pool. `0` disables (removes a default). |
| `projection` | no | `rigid` | `rigid` or `terrain_matching`. |
| `processors` | no | `minecraft:empty` | Processor list id. |

**Spawn model:** end-user config `buildingSpawnChancePercent` (0–100, default 15) sets the overall
custom-vs-vanilla budget; per-building `weight` sets each building's share of that budget. The two
are independent.

**Acceptance criteria (regression)**
- [ ] Each building file adds one entry to its `pool`; files from different namespaces all apply.
- [ ] `weight: 0` removes a building (including one of the mod's own defaults) via an override.
- [ ] A missing/invalid `pool` is logged and skipped without affecting other buildings (P3).

### 2.1 🔧 Proposed additions
- [ ] `conditions` block (see §4) so a building can self-gate on a mod/tag.
- [ ] Documented recipe for targeting non-house pools (streets/decor/centers) — no code change,
      docs + example only.

---

## 3. Professions & job sites API — 🔧 Proposed (v0.7) — the platform unlock

**Today (v0.6):** professions and their POI job-site blocks are registered in Java
(`ModVillagers.java`). Data packs can *retarget trades to* a profession but cannot *create* one.
This is the last thing that forces a developer into Java.

**Goal:** declare a whole profession from data.

**Proposed file location**
```
data/<namespace>/dynamicvillage/professions/<any_name>.json
```

**Proposed fields**

| Field | Required | Default | Description |
|-------|----------|---------|-------------|
| `id` | yes | — | Profession id (your namespace). |
| `job_site_block` | one of these | — | A single block id whose states form the POI. |
| `job_site_blocks` | one of these | — | A list of block ids that together form the POI. |
| `work_sound` | no | generic villager work | Sound event id played while working. |
| `search_distance` | no | `1` | POI validity/search range (raised to 1 if lower). |
| `max_tickets` | no | `1` | Villagers that may claim one job site (raised to 1 if lower). |
| `conditions` | no | — | §4 conditions. |

> **`job_site_tag` is not supported** (and never worked): block tags are supplied by data packs and
> aren't bound when professions must be registered, so a tag could never resolve. A definition still
> using it is skipped with an explanatory error. Villager job-claiming is enabled by adding the
> profession id to the `minecraft:acquirable_job_site` tag in a normal data pack.

Example:
```json
{
  "id": "mypack:logistics_engineer",
  "job_site_block": "create:packager",
  "work_sound": "minecraft:entity.villager.work_toolsmith"
}
```

**Acceptance criteria — 🔧**
- [ ] A professions JSON registers a working `VillagerProfession` **and** its POI with no Java from
      the developer (P1).
- [ ] The declared job-site block(s) are added to `minecraft:acquirable_job_site` so a villager can
      take the job (🧪 unemployed villager near the block becomes the profession).
- [ ] At least one of `job_site_block` / `job_site_blocks` is provided; neither ⇒ logged, skipped (P3).
- [ ] A duplicate id, an id clashing with an existing profession, or a job-site block already owned by
      another POI ⇒ logged and skipped, never a startup crash (P3).
- [ ] The new profession is a valid `profession` target for a §1 trade file (the three systems
      compose: profession + building + trades from one pack).
- [ ] 🧪 A profession referencing a block from an absent mod is skipped, world still loads (P5).
- [ ] Registration timing is documented (POIs/professions are built-in registries; note whether it
      requires restart vs `/reload`).

---

## 4. Conditions — 🔧 Proposed (v0.7)

Support NeoForge's standard `"neoforge:conditions"` on trade, building, and profession files so a
developer can ship one file that self-gates instead of relying on implicit item-resolution skipping.

```json
{
  "neoforge:conditions": [
    { "type": "neoforge:mod_loaded", "modid": "immersiveengineering" }
  ],
  "profession": "dynamicvillage:mechanical_engineer",
  "trades": [ /* IE trades, loaded only if IE is present */ ]
}
```

**Acceptance criteria — 🔧**
- [ ] A file whose conditions are unmet is skipped silently-by-design (a single info line, not a
      warning — unmet conditions are normal, P7).
- [ ] A file with no conditions block always loads (P2).
- [ ] Conditions are honored on all three file types (trades, buildings, professions).

---

## 5. Merge, override & precedence semantics

**✅ Shipped (v0.6) behavior**
- Files in **different** namespaces/paths are **additive** — all contribute.
- A file at the **same path** as a default (e.g. `data/dynamicvillage/dynamicvillage/trades/miner.json`)
  **replaces** that default entirely (standard higher-priority-pack behavior).

**Acceptance criteria (regression)**
- [ ] Two packs adding trades to the same profession from their own namespaces both apply.
- [ ] Overriding the mod's `miner.json` at the same path replaces the Miner's default list; the log
      shows the reduced count (e.g. "Added 1" instead of "Added 20").

**🔧 Proposed — explicit, composable control**

The current "write into your own namespace and match the exact path to edit another pack's trades"
rule has a sharp edge: two packs overriding the same path silently clobber by load order. Proposed
opt-in field:

| Field | Default | Effect |
|-------|---------|--------|
| `replace` | `false` | `false` = append this file's trades to the profession's pool (composes with other packs). `true` = clear existing entries for this profession first, then add. |

**Acceptance criteria — 🔧**
- [ ] Default (`replace` absent/false) is additive across packs — no silent clobber.
- [ ] `replace: true` deterministically resets a profession's trade list before applying.
- [ ] Documented precedence when multiple files set `replace: true` (data pack order), logged (P7).

---

## 6. Diagnostics & logging contract

Every load path narrates itself under the `[DynamicVillage]` tag so a developer can debug from the
log alone (P7).

**✅ Shipped**
- Summary on load: `Loaded N villager trade(s) across M profession(s) (K invalid file(s) skipped)`.
- Per profession on villager setup: `Added N trade(s) to profession <id>`.
- Skipped item: `Skipping trade for <prof>: item <id> is not registered (mod not installed?)`.
- Invalid file: `Skipping invalid trade file <file>: <codec error>`.

**🔧 Proposed additions**
- Buildings and professions emit equivalent load-summary and skip lines (parity with trades).
- Condition-skips logged at info level with the unmet condition.
- Each skip message names the **file** and the **reason** (P7).

**Acceptance criteria**
- [ ] 🧪 Intentionally malformed file of each type ⇒ one warning naming the file, all other files
      still load, world starts (P3).
- [ ] Load summary counts match the number of valid files present.

---

## 7. Compatibility & versioning guarantees

- [ ] **P2:** Any valid v0.6 trade/building file loads unchanged in v0.7 (no field renames, no
      newly-required fields).
- [ ] All JSON contracts are **identical** between `mod-1.21.1-v7` and `mod-1.20.1-v7`; a pack works
      on both without edits.
- [ ] New optional fields degrade gracefully on the older loader if a dev backports a pack (unknown
      fields ignored, not fatal) — or, if not possible, this is documented per field.
- [ ] `DATAPACK.md` and this spec are updated in the **same change** as any contract change (docs
      never lag code).

---

## 8. Tooling & onboarding — 🔧 Proposed

Lower the time-to-first-working-pack.

| Item | Deliverable | Acceptance |
|------|-------------|------------|
| **JSON schema** | `assets`/`docs` schema files for trade, building, profession files. | [ ] Referencing them via `$schema` gives autocomplete + validation in VS Code. |
| **Example add-on pack** | `examples/example-addon/` in the repo: one new profession + one building + trades + the NBT, as a ready-to-drop data pack. | [ ] 🧪 Copied into a world's `datapacks/`, it loads with zero edits and the new villager works end-to-end. |
| **`DATAPACK.md` cross-links** | Link each section to the matching spec section here. | [ ] Every field in `DATAPACK.md` matches this spec exactly. |

---

## 9. Build order (recommended)

1. **§1.2 richer item spec** + **§4 conditions** — pure codec extensions, backward compatible, high
   value, low risk. Do first.
2. **§5 explicit `replace`** — small, removes the clobber sharp edge.
3. **§8 schema + example pack** — cheap, big onboarding win; do alongside 1–2.
4. **§3 data-driven professions** — the real platform unlock; scope deliberately (registration
   timing). Do last, on its own.

---

## 10. Master verification checklist

Copy of the must-pass items, for a single-glance review after each change.

**Regression (v0.6 behavior preserved)**
- [ ] Unmodified v0.6 pack loads and behaves identically (trades + buildings).
- [ ] Vanilla-profession trade targeting still works.
- [ ] Same-path override still replaces; cross-namespace still appends.
- [ ] Missing-mod item still skips softly.
- [ ] Malformed file still skipped without breaking others.

**New (per feature, when built)**
- [ ] Item spec: `{min,max}` counts, `tag` costs, `components` results.
- [ ] Conditions honored on all three file types.
- [ ] `replace` flag additive-by-default.
- [ ] Data-driven profession registers profession + POI + acquirable tag, composes with trades &
      buildings, no developer Java.
- [ ] Schema autocompletes; example pack works end-to-end on a fresh world.
- [ ] `DATAPACK.md` + this spec updated in the same change.

**Parity**
- [ ] Identical JSON behavior on `mod-1.21.1-v7` and `mod-1.20.1-v7`.
```
