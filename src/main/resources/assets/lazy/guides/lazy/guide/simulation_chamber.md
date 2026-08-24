---
navigation:
  parent: index.md
  title: Simulation Chamber
  icon: lazy:simulation_chamber
  position: 50
item_ids:
  - lazy:simulation_chamber
---

# Simulation Chamber

<BlockImage id="lazy:simulation_chamber" scale="8" />

The Simulation Chamber runs data-driven item and entity simulations without spawning a permanent farm or mob. Results are stored in internal item and fluid outputs until IO can move them away.

## Inputs

- **Target** accepts an item with an explicit or automatic simulation, a spawn egg, a bound <ItemLink id="lazy:data_model" />, or another supported entity carrier.
- **Processing Core** accepts up to 64 cores. Each core contributes the tier's speed and output multiplier.
- **Tools** provide optional behaviour. Three tool slots are read as the batch advances; tools are not consumed or damaged.

## Data Model

Use a blank <ItemLink id="lazy:data_model" /> on a supported living entity to bind that entity to the model. Sneak-use a bound model to clear it. Players and targets rejected by the simulation rules cannot be bound.

Core values and stacking behavior are documented on the [Processing Cores](processing_cores.md) page.

## Tool slots

- A weapon is used as the simulated entity's killing tool. The first weapon slot wins.
- A lava bucket filters outputs tagged as incinerated.
- Roll work always obeys the server `rollBudgetPerTick`; IO mode only chooses where stored output is transported.

Recipes, automatic material rules, entity loot, output probabilities, and tool tags are data-driven. When output storage is full or output cannot be confirmed, the chamber pauses and shows a pending-output warning. Configure its item/fluid routing in the common [IO settings](io.md).

<Recipe id="lazy:simulation_chamber" />
