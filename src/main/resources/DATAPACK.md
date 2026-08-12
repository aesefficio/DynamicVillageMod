# Adding village buildings with a data pack

Create: Dynamic Village reads its building list from data packs, so you can add your own
buildings to villages **without writing any code or editing the mod**. Other mods can do the
same — every data pack and mod is scanned and merged.

## 1. Add a building definition

Drop a JSON file in:

```
data/<your_namespace>/dynamicvillage/buildings/<any_name>.json
```

Minimal example:

```json
{
  "pool": "minecraft:village/plains/houses",
  "structure": "mypack:houses/cool_house"
}
```

Every file you add is one building. Use your own namespace — files never conflict or overwrite
each other, so any number of packs/mods can contribute buildings at once.

### Fields

| Field        | Required | Default            | Description |
|--------------|----------|--------------------|-------------|
| `pool`       | yes      | —                  | The village pool to add to, e.g. `minecraft:village/plains/houses`, `.../desert/houses`, `.../taiga/houses`, `.../snowy/houses`, `.../savanna/houses`. Any template pool works. |
| `structure`  | yes      | —                  | The NBT structure to place (any namespace). |
| `weight`     | no       | `1`                | Relative frequency vs the **other custom buildings** in the same pool. A `weight` of `3` shows up ~3x as often as a `weight` of `1`. Set it to `0` to disable a building (e.g. to remove one of the mod's defaults). |
| `projection` | no       | `rigid`            | `rigid` or `terrain_matching`. Village houses are almost always `rigid`. |
| `processors` | no       | `minecraft:empty`  | A processor list resource location, if you need block processing. |

## 2. Add the structure NBT

Put your structure where the game looks for templates:

```
data/<your_namespace>/structure/<path>.nbt
```

(Reference it from `structure` above — e.g. `data/mypack/structure/houses/cool_house.nbt`
is `"structure": "mypack:houses/cool_house"`.) The structure must contain a **jigsaw block**
at its entrance so it connects to the village road — see the main README for how to build one.

## 3. How the spawn rate works

The end-user's `buildingSpawnChancePercent` config controls how much of a village should be
**custom buildings vs vanilla houses overall**. Your per-building `weight` controls each
building's share of that custom budget. The two are independent: users tune the overall amount,
you tune the relative mix.

# Giving a building's chests loot

Buildings with chests spawn **empty** unless the chests are told which loot table to roll. You can
assign a loot table to any building — this mod's or your own — with a tiny file, and the mod stamps
that table onto the building's chests as the village generates (vanilla-style: the chest fills the
first time it's opened). This affects **newly generated** villages only.

## 1. Add a loot assignment

Drop a JSON file whose path mirrors the **structure** it applies to. For structure `ns:path` (i.e.
`data/ns/structure/path.nbt`), the assignment lives at:

```
data/<ns>/dynamicvillage/loot/<path>.json
```

```json
{ "loot_table": "mypack:chests/my_building" }
```

| Field              | Required | Default | Description |
|--------------------|----------|---------|-------------|
| `loot_table`       | yes      | —       | Any loot table id — your own (`data/<ns>/loot_table/…`), one of this mod's (`dynamicvillage:chests/village/{miner,train_mechanic,hydraulic,mechanical_engineer}`), or vanilla (`minecraft:chests/village/village_plains_house`). If no loaded pack provides it, a warning is logged at startup. |
| `override_existing`| no       | `false` | By default, chests that already carry a hand-authored `LootTable` in the NBT are left alone. Set `true` to replace even those. |
| `conditions`       | no       | —       | Load conditions (see below); the assignment is skipped if any is unmet. |

The loot table itself is a **normal Minecraft loot table** — author it the standard way; this file
just points a building's chests at it.

## 2. Re-loot another pack's (or this mod's) building

Because the assignment is keyed by structure path, you override any building's loot by dropping a
file at the **same path** — standard data pack priority decides the winner. For example, to change
this mod's plains miner house loot, add:

```
data/dynamicvillage/dynamicvillage/loot/plains/plains_miner.json
```
```json
{ "loot_table": "mypack:chests/richer_miner" }
```

No need to redefine the building — just its loot. On startup the log reports
`[DynamicVillage] Loaded N chest loot assignment(s)`.

---

# Adding a new villager profession

You can add a **brand-new profession** (with its own job-site block and trades) using only JSON.

**Important — this one uses `config/`, not a data pack.** Villager professions and their POI
(job-site) types live in registries that Minecraft freezes during mod loading, *before* any world
data pack is read. So new professions cannot come from a world data pack — the mod instead reads
them from the **config directory**, which is available that early. A modpack ships them in its
`config/` (or `defaultconfigs/`) folder. Everything else — the profession's trades and buildings —
still uses the normal data-pack systems.

## 1. Add a profession definition

Drop a JSON file in:

```
config/dynamicvillage/professions/<any_name>.json
```

```json
{
  "id": "mypack:logistics_engineer",
  "job_site_block": "create:packager",
  "work_sound": "minecraft:entity.villager.work_toolsmith"
}
```

### Fields

| Field            | Required | Default | Description |
|------------------|----------|---------|-------------|
| `id`             | yes      | —       | Profession id (your namespace). Also used as the id of its POI type. |
| `job_site_block` | one of   | —       | A single block whose states become the job-site POI. |
| `job_site_blocks`| one of   | —       | A list of block ids that together form the job-site POI. Use with or instead of `job_site_block`. |
| `work_sound`     | no       | villager work sound | Sound event id played while working. |
| `search_distance`| no       | 1       | POI validity/search range (values below 1 are raised to 1). |
| `max_tickets`    | no       | 1       | How many villagers may claim one job site (values below 1 are raised to 1). |
| `conditions`     | no       | —       | Load conditions (see below); the profession is skipped if any is unmet. |

> **Block *tags* are not supported for job sites.** Tags are supplied by data packs and aren't loaded
> until a world starts — long after professions have to be registered — so a tag could never be
> resolved here. List the blocks explicitly with `job_site_blocks`. A definition still using the old
> `job_site_tag` field is skipped with an explanatory error in the log.

Several blocks example:

```json
{
  "id": "mypack:logistics_engineer",
  "job_site_blocks": ["create:packager", "create:item_vault"]
}
```

Bad definitions never crash the game: a duplicate `id`, an id that clashes with an existing
profession, or blocks that aren't registered are each logged and skipped.

## 2. Make the job acquirable (data-pack tag)

For villagers to actually take the job, the POI must be in `minecraft:acquirable_job_site`. Add a
normal data-pack tag entry (this part *is* a data pack) listing your profession id:

```
data/minecraft/tags/point_of_interest_type/acquirable_job_site.json
```
```json
{ "replace": false, "values": ["mypack:logistics_engineer"] }
```

## 3. Add its trades

Use the normal trade system (below), targeting your new id:
`"profession": "mypack:logistics_engineer"`.

The log confirms registration on startup:
`[DynamicVillage] Registered profession mypack:logistics_engineer`.

---

# Adding or changing villager trades

Villager trades are also data driven. The mod ships standard trades for its professions, and you
can **add** new trades, **change** ours, or **remove** them — and you can even add trades to
**vanilla** professions (farmer, librarian, etc.).

## 1. Add a trade file

Drop a JSON file in:

```
data/<your_namespace>/dynamicvillage/trades/<any_name>.json
```

Each file targets one profession and lists its trades:

```json
{
  "profession": "dynamicvillage:mechanical_engineer",
  "trades": [
    {
      "level": 1,
      "cost":   { "item": "minecraft:emerald", "count": 2 },
      "result": { "item": "create:andesite_alloy", "count": 8 },
      "max_uses": 8,
      "xp": 8,
      "price_multiplier": 0.02
    }
  ]
}
```

`profession` can be any villager profession id, including vanilla ones like `minecraft:farmer`.

### Trade fields

| Field              | Required | Default | Description |
|--------------------|----------|---------|-------------|
| `level`            | yes      | —       | Villager tier 1–5 that unlocks the trade. |
| `cost`             | yes      | —       | Item spec (see below) the villager takes. |
| `cost2`            | no       | none    | Optional second cost item spec. |
| `result`           | yes      | —       | Item spec the villager gives. |
| `max_uses`         | no       | 12      | Times the trade can be used before it locks. |
| `xp`               | no       | 2       | Villager XP granted per trade. |
| `price_multiplier` | no       | 0.05    | How much demand raises the price. |

### Item spec (`cost`, `cost2`, `result`)

Each is an object with **exactly one** of `item`/`tag`, plus an optional `count`:

| Field   | Description |
|---------|-------------|
| `item`  | An exact item id, e.g. `minecraft:emerald`. |
| `tag`   | An item tag id, e.g. `c:ingots/zinc`. Resolves to the **first registered item** in that tag — handy for "whatever zinc ingot this pack provides". Use `item` **or** `tag`, not both. |
| `count` | A fixed number (`"count": 3`) **or** a random range (`"count": { "min": 2, "max": 5 }`) rolled per generated offer. Defaults to `1`. |
| `components` | **`result` only.** Extra data attached to the item — enchantments, custom name, dye, potion, etc. Costs are matched by item and count alone, so `components` on a `cost`/`cost2` does nothing and logs a warning. **Version-specific:** on 1.21.x this is a data-components object; on 1.20.x it is an item-NBT object. This is the one field whose contents differ between Minecraft versions. |

Enchanted-book result on **1.21.x** (data components):

```json
"result": {
  "item": "minecraft:enchanted_book",
  "components": { "minecraft:stored_enchantments": { "levels": { "minecraft:efficiency": 3 } } }
}
```

The same on **1.20.x** (item NBT):

```json
"result": {
  "item": "minecraft:enchanted_book",
  "components": { "StoredEnchantments": [ { "id": "minecraft:efficiency", "lvl": 3 } ] }
}
```

```json
{
  "level": 3,
  "cost":   { "tag": "c:ingots/zinc", "count": { "min": 2, "max": 4 } },
  "result": { "item": "create:precision_mechanism", "count": 1 },
  "max_uses": 5
}
```

The old `{ "item": "...", "count": 2 }` form still works exactly as before — the new fields are
optional additions.

## 2. Add vs change vs remove

- **Add new trades (recommended):** put a file in *your* namespace — its trades are **appended** to
  the profession's pool, composing cleanly with the mod's defaults and any other pack.
- **Replace a profession's trades:** add `"replace": true` to your file. This clears everything
  accumulated for that profession first, then adds your trades. Unlike the same-path override below,
  it works from any namespace. When multiple packs are present, files are processed in a
  deterministic order (sorted by file id) so `replace` behaves predictably.
- **Change/remove ours by override:** override the file at the *same path* — e.g. a higher-priority
  data pack with `data/dynamicvillage/dynamicvillage/trades/miner.json` fully replaces the Miner's
  default trade list.

## 3. Conditions (load only when...)

Any trade **or** building file may carry a `conditions` array. If **all** conditions pass, the file
loads; otherwise it is skipped (logged at info level — an unmet condition is normal). This lets one
pack ship cross-mod content safely.

```json
{
  "conditions": [
    { "type": "mod_loaded", "mod": "immersiveengineering" }
  ],
  "profession": "dynamicvillage:mechanical_engineer",
  "trades": [ /* only loaded when Immersive Engineering is present */ ]
}
```

| `type`         | Needs  | True when… |
|----------------|--------|------------|
| `mod_loaded`   | `mod`  | that mod id is loaded. |
| `item_exists`  | `id`   | that item id is registered. |
| `block_exists` | `id`   | that block id is registered. |

Add `"negate": true` to a single condition to invert it. This is a loader-neutral system evaluated
by the mod itself (not Forge/NeoForge `*:conditions`), so the **same JSON works on every Minecraft
version**.

## Notes

- Changes apply on **world load / server restart** (not live `/reload`).
- Invalid definitions are logged and skipped — one bad file won't break the others. Check the
  log for `[DynamicVillage]` lines to see what loaded.
- A trade that references an item/tag from a mod that isn't installed is skipped with a warning, so
  packs referencing Create items won't crash a world without Create.
- Requires the Create mod (this is a Create add-on).
- **Editor autocomplete:** point your editor at `schemas/dynamicvillage-trades.schema.json` and
  `schemas/dynamicvillage-building.schema.json` (via a `$schema` key or your editor's JSON-schema
  settings). A ready-to-run sample pack lives in `examples/example-addon/`.

---

# Full worked example: "every trade gives a stack of emerald blocks for one stick"

A complete, copy-paste walkthrough that replaces all of this mod's trades. Follow it exactly and
you'll have a working data pack.

### Step 1 — Make the pack folder

Open your world's data pack folder:

```
.../saves/<your world>/datapacks/
```

Make a new folder inside it called `EmeraldDeal`.

### Step 2 — Create `pack.mcmeta`

Inside `EmeraldDeal`, create a file named `pack.mcmeta` with this content:

```json
{
  "pack": {
    "pack_format": 15,
    "description": "Stick = stack of emerald blocks"
  }
}
```

(`15` is the data pack format for Minecraft 1.20.1.)

### Step 3 — Create the folders

Inside `EmeraldDeal`, make this exact folder chain (the doubled `dynamicvillage` is correct — it's
`namespace` then `folder`):

```
EmeraldDeal/
├── pack.mcmeta
└── data/
    └── dynamicvillage/
        └── dynamicvillage/
            └── trades/
                ├── mechanical_engineer.json
                ├── hydraulic_engineer.json
                ├── miner.json
                └── train_mechanic.json
```

Using the path `data/dynamicvillage/dynamicvillage/trades/` with the same file names as the mod is
what **overrides** (replaces) the mod's built-in trades.

### Step 4 — Fill in the four files

`mechanical_engineer.json`:

```json
{
  "profession": "dynamicvillage:mechanical_engineer",
  "trades": [
    { "level": 1, "cost": { "item": "minecraft:stick", "count": 1 }, "result": { "item": "minecraft:emerald_block", "count": 64 }, "max_uses": 999999 }
  ]
}
```

`hydraulic_engineer.json`:

```json
{
  "profession": "dynamicvillage:hydraulic_engineer",
  "trades": [
    { "level": 1, "cost": { "item": "minecraft:stick", "count": 1 }, "result": { "item": "minecraft:emerald_block", "count": 64 }, "max_uses": 999999 }
  ]
}
```

`miner.json`:

```json
{
  "profession": "dynamicvillage:miner",
  "trades": [
    { "level": 1, "cost": { "item": "minecraft:stick", "count": 1 }, "result": { "item": "minecraft:emerald_block", "count": 64 }, "max_uses": 999999 }
  ]
}
```

`train_mechanic.json`:

```json
{
  "profession": "dynamicvillage:train_mechanic",
  "trades": [
    { "level": 1, "cost": { "item": "minecraft:stick", "count": 1 }, "result": { "item": "minecraft:emerald_block", "count": 64 }, "max_uses": 999999 }
  ]
}
```

### Step 5 — Load it

Restart the world (a `/reload` alone won't change trades). Note that villagers that **already
exist** keep their old trades — this affects **newly employed** villagers, so test with a fresh one.

### Step 6 — Check it worked

In the log you should see, for each profession:

```
[DynamicVillage] Added 1 trade(s) to profession dynamicvillage:mechanical_engineer
```

"Added 1" (instead of the usual 20) means your override took. Trade a stick to a fresh villager and
you'll get a stack of emerald blocks.

### Want it at every level?

Add the same trade for each tier so it's always available:

```json
{
  "profession": "dynamicvillage:miner",
  "trades": [
    { "level": 1, "cost": { "item": "minecraft:stick", "count": 1 }, "result": { "item": "minecraft:emerald_block", "count": 64 }, "max_uses": 999999 },
    { "level": 2, "cost": { "item": "minecraft:stick", "count": 1 }, "result": { "item": "minecraft:emerald_block", "count": 64 }, "max_uses": 999999 },
    { "level": 3, "cost": { "item": "minecraft:stick", "count": 1 }, "result": { "item": "minecraft:emerald_block", "count": 64 }, "max_uses": 999999 },
    { "level": 4, "cost": { "item": "minecraft:stick", "count": 1 }, "result": { "item": "minecraft:emerald_block", "count": 64 }, "max_uses": 999999 },
    { "level": 5, "cost": { "item": "minecraft:stick", "count": 1 }, "result": { "item": "minecraft:emerald_block", "count": 64 }, "max_uses": 999999 }
  ]
}
```
