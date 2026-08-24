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
- **Tools:** up to three optional tools. They are not consumed or damaged.

## Data Model

Use a blank <ItemLink id="lazy:data_model" /> on a supported living entity, then place the bound model in the target slot. See [Processing Cores](processing_cores.md) for core values.

## Tool slots

- The first weapon is used as the simulated killing tool.
- A lava bucket removes drops that would normally burn in lava.

JEI shows supported simulations and their possible results. If output storage is full, the chamber pauses until space is available. Use [IO settings](io.md) to move item and fluid results.

<Recipe id="lazy:simulation_chamber" />
