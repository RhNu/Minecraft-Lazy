---
navigation:
  parent: machines.md
  title: Simulation Chamber
  icon: lazy:simulation_chamber
  position: 70
item_ids:
  - lazy:simulation_chamber
---

# Simulation Chamber

<Column alignItems="center" fullWidth={true}>
  <BlockImage id="lazy:simulation_chamber" scale="8" />
</Column>

The Simulation Chamber produces farming and mob drops without building a farm or keeping the mob in the world.

## Inputs

- **Target:** a supported item, spawn egg, or bound <ItemLink id="lazy:data_model" />.
- **Processing Core:** up to 64 cores. Better cores increase speed and output.
- **Context:** up to three optional condition items or behavior tools. They are not consumed or damaged.

## Data Model

Use a blank <ItemLink id="lazy:data_model" /> on a supported living entity, then place the bound model in the target slot. See [Processing Cores](processing_cores.md) for core values.

## Context slots

- The chamber uses one target/seed slot plus three unordered context slots. Each recipe condition needs a different slot, while extra behavior tools are allowed.
- The first weapon is used as the simulated killing tool.
- A furnace, blast furnace, or smoker applies its matching smelting, blasting, or smoking recipe to output batches. Multiple cooking tools try in slot order.
- A lava bucket or magma block removes drops that would normally burn in lava. With Apotheosis installed, affix equipment is salvaged first; gems are never salvaged.
- A grindstone removes every enchanted drop after lava processing.
- Changing tools during an active job affects only the next job.

Simple mature crops and conventional saplings are discovered automatically. Complex fruit leaves, vines, colonies, and environment-dependent plants are supplied by their Integration or by datapack/KubeJS recipes. The chamber evaluates their mature block loot tables without placing or breaking blocks and without firing third-party interaction events.

If two recipes have the same priority and the same number of matching tool conditions, the chamber stops instead of choosing one by ID. Use `/lazy simulation inspect held` or `/lazy simulation inspect chamber <pos>` to see candidates, missing tools, injections, and conflicts.

Explicit recipes, automatic discoveries, entity profiles, and injections share one variant registry. Datapacks and KubeJS may assign a `group` (or `.simulationGroup(...)`) for stable inspection and display grouping; groups do not change selection priority.

JEI shows supported simulations and their possible results. If output storage is full, the chamber pauses until space is available. Use [IO settings](io.md) to move item and fluid results.

<Recipe id="lazy:simulation_chamber" />
