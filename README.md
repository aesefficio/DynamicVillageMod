# Create: Dynamic Village

Bring life and activity to your Create worlds.

Create: Dynamic Village is an add-on for [Create](https://modrinth.com/mod/create) that adds
Create-themed villagers and the buildings they live in. Villages generate with mechanical
engineers, hydraulic engineers, miners and train mechanics — each with their own workshop,
job site, trades, and loot.

**Requires Create 6.0+.**

## What it adds

- **Four villager professions**, each with a Create block as its job site:
  | Profession | Job site |
  |---|---|
  | Mechanical Engineer | Schematic Table |
  | Hydraulic Engineer | Item Drain |
  | Miner | Mechanical Drill |
  | Train Mechanic | Track Station |
- **20 new village buildings** — a workshop per profession, styled for plains, desert, savanna,
  snowy and taiga villages.
- **Create-themed trades** for every profession, and **randomized chest loot** in the workshops.
- **Denser villages** — an optional boost to vanilla decoration density.

## Configuration

In-game via **Mods → Create: Dynamic Village → Config**, or `config/dynamicvillage-common.toml`:

| Option | Default | Description |
|---|---|---|
| `buildingSpawnChancePercent` | `35` | How much of a village is made of this mod's buildings (0 = vanilla only, 100 = almost entirely custom). |
| `decorationDensityMultiplier` | `2` | Fills more of a village's empty decoration spots. `1` = vanilla. |

## For data pack & mod developers

Almost everything here is data-driven — you can add or change content **without touching the mod's
code**. See **[DATAPACK.md](DATAPACK.md)** for the full guide:

- **Trades** — add to any profession (including vanilla ones), or replace ours. Supports item tags,
  random count ranges, and components/NBT.
- **Buildings** — inject your own structures into any village pool, or disable ours.
- **Chest loot** — assign a loot table to any building's chests, including re-looting ours.
- **New professions** — define a whole profession (job site, sound, trades) in JSON.
- **Load conditions** — gate any of the above on another mod being present, so one pack can safely
  ship cross-mod content.

Also included: JSON schemas in [`schemas/`](schemas/) for editor autocomplete, a ready-to-run
sample pack in [`examples/example-addon/`](examples/example-addon/), and
[DEVELOPER_SPEC.md](DEVELOPER_SPEC.md) documenting the full contract.

## Building from source

```
gradlew build
```

The jar lands in `build/libs/`. Requires JDK 17 for 1.20.1 (JDK 21 for the 1.21.1 branch).

## Links

- Issues: https://github.com/aesefficio/DynamicVillageMod/issues
- Changelog: [CHANGELOG.md](CHANGELOG.md)

Licensed under GPL-3.0. Credits: Ben001109, Fatasiangamer.
