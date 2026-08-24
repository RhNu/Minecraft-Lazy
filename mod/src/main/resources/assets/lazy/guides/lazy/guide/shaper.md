---
navigation:
  parent: machines.md
  title: Shaper
  icon: lazy:shaper
  position: 60
item_ids:
  - lazy:shaper
---

# Shaper

<Column alignItems="center" fullWidth={true}>
  <BlockImage id="lazy:shaper" scale="8" />
</Column>

The Shaper converts a material between common forms without losing material or using energy.

## Use

- Click the ghost sample slot with the form you want to produce.
- Add matching nuggets, ingots, gems, dusts, raw materials, plates, rods, wires, gears, or storage blocks.
- Click the sample slot with an empty cursor to clear it.

Conversions use whole, lossless amounts. For example, nine nuggets become one ingot, one ingot becomes nine nuggets, and one plate becomes two rods. Leftovers stay in the input storage until a complete conversion is possible.

## Storage and output

- Input and output each hold up to eight item types.
- [IO settings](io.md) control side input, automatic output, and network output.
- JEI lists the available conversions for each material.

Input and output remain in the dropped machine. The selected sample and IO settings reset when it is placed again.

<Recipe id="lazy:shaper" />
