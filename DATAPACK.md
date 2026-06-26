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
| `cost`             | yes      | —       | `{ "item", "count" }` the villager takes. |
| `cost2`            | no       | none    | Optional second cost item. |
| `result`           | yes      | —       | `{ "item", "count" }` the villager gives. |
| `max_uses`         | no       | 12      | Times the trade can be used before it locks. |
| `xp`               | no       | 2       | Villager XP granted per trade. |
| `price_multiplier` | no       | 0.05    | How much demand raises the price. |

## 2. Add vs change vs remove

- **Add new trades:** put a file in *your* namespace — its trades are appended.
- **Change/remove ours:** override the file at the *same path* — e.g. a higher-priority data pack
  with `data/dynamicvillage/dynamicvillage/trades/miner.json` fully replaces the Miner's default
  trade list, so you can edit, reorder, or delete trades.

## Notes

- Changes apply on **world load / server restart** (not live `/reload`).
- Invalid definitions are logged and skipped — one bad file won't break the others. Check the
  log for `[DynamicVillage]` lines to see what loaded.
- A trade that references an item from a mod that isn't installed is skipped with a warning, so
  packs referencing Create items won't crash a world without Create.
- Requires the Create mod (this is a Create add-on).

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
    "pack_format": 48,
    "description": "Stick = stack of emerald blocks"
  }
}
```

(`48` is the data pack format for Minecraft 1.21.1.)

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
