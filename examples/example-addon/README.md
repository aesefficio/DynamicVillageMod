# Example add-on data pack

A complete, working Create: Dynamic Village add-on pack you can copy and adapt. It demonstrates
**every** data-driven feature, including adding a brand-new profession.

## What it does

| Part | Location | Shows |
|------|----------|-------|
| `data/.../trades/engineer_extras.json` | data pack | **Adds** two trades to the mod's Mechanical Engineer. Uses a **tag cost** (`c:ingots/zinc`), a **count range** (`{min,max}`, rolled per offer), and a **condition** (loads only when `create` is present). |
| `data/.../trades/vanilla_farmer.json` | data pack | Adds a trade to a **vanilla** profession (`minecraft:farmer`). |
| `data/.../buildings/plains_extra.json` | data pack | Injects an extra building into `minecraft:village/plains/houses` with a `weight` and a condition. |
| `config/.../professions/logistics_engineer.json` | **config** | Defines a **new profession** whose job site is `create:packager`. |
| `data/minecraft/tags/.../acquirable_job_site.json` | data pack | Makes the new profession's job site claimable by villagers. |
| `data/.../trades/logistics_engineer.json` | data pack | Trades for the new profession — including a **count-range** result. |
| `data/dynamicvillage/dynamicvillage/loot/plains/plains_train.json` | data pack | **Re-loots one of this mod's own buildings** — points the plains train house's chests at a vanilla loot table by dropping a same-path override (no need to redefine the building). |

Because every data file lives under the `exampleaddon` namespace, nothing overwrites the mod's own
files — it all **merges** in. To *replace* a profession's trades instead of appending, add
`"replace": true` to a trades file.

## How to use it

Two parts, because professions must be registered before data packs load:

1. **Data pack:** copy the `data/` folder and `pack.mcmeta` into a world's `datapacks/example-addon/`
   directory (`.../saves/<world>/datapacks/example-addon/`).
2. **Profession config:** copy `config/dynamicvillage/` into your instance's `config/` folder
   (`.../config/dynamicvillage/professions/...`). A modpack ships this in `config/` or
   `defaultconfigs/`.
3. Restart the world/server (trades and professions apply on load, not on `/reload`).
4. Watch the log for `[DynamicVillage]` lines confirming what loaded:
   - `Registered profession exampleaddon:logistics_engineer`
   - `Added N trade(s) to profession dynamicvillage:mechanical_engineer`
   - `Added 1 trade(s) to profession minecraft:farmer`
   - `Loaded N village building definition(s) ...`
5. A newly-employed Mechanical Engineer, Farmer, or Logistics Engineer (a villager who claims a
   `create:packager`) offers the new trades; new plains villages can roll the extra building.

> The profession example needs Create installed (its job site is `create:packager`). Its
> `block_exists` / `mod_loaded` conditions make it skip cleanly if Create is absent.

## Notes

- **`pack_format`**: `48` targets Minecraft 1.21.x. For 1.20.1 change it to `15`. The data files
  themselves are identical across versions.
- **`$schema`** lines give you autocomplete/validation in editors like VS Code. They are ignored by
  the game. The relative path assumes this pack sits beside the repo's `schemas/` folder; adjust or
  remove as needed.
- **Structures**: this example reuses one of the mod's own structures so it works out of the box. In
  a real pack you would ship your own NBT at `data/<namespace>/structure/<path>.nbt` with a jigsaw
  block at its entrance and point `structure` at it.
- Trades that reference items/tags from a mod that isn't installed are skipped with a warning, so a
  pack referencing Create items won't crash a world without Create.
