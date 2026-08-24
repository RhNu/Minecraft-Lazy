---
navigation:
  title: Shaper
  icon: lazy:shaper
  parent: index.md
item_ids:
  - lazy:shaper
---

# Shaper

<BlockImage id="lazy:shaper" scale="1.15" />

The Shaper converts common forms of the same material without consuming material or energy. Put the desired form in the phantom sample slot, then load nuggets, ingots, gems, dusts, raw materials, plates, rods, wires, gears, or storage blocks into any of its eight input lanes.

<Recipe id="lazy:shaper" />

Each lane holds up to 1,024 items. Conversions complete immediately in the smallest lossless whole-item trade: nine nuggets become one ingot, one ingot becomes nine nuggets, and one plate becomes two rods. Items that cannot complete a whole trade remain visible in the input lane.

The eight output lanes are shared by every input. Matching products merge before an empty lane is used. If the pool cannot fit a whole trade, the front display reports a blocked state until space becomes available.

The sample is a setting, not storage. Clicking it with an item selects a form without consuming the item; clicking it with an empty cursor clears it. The sample resets when the machine is dismantled, while input and output contents stay with the dropped machine item.

Use [IO and automation](io.md) for face input, automatic output, and network output. JEI lists lossless conversions around one base form for each material. Material packs can extend the synced `lazy:material_form` datapack registry, and pack authors can use the Shaper blacklist tags to disable unsafe inputs or outputs.
