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

The Shaper converts common forms of the same material without consuming material or energy. Put the desired form in the phantom sample slot, then load nuggets, ingots, gems, dusts, raw materials, plates, rods, wires, gears, or storage blocks into its shared input store.

<Recipe id="lazy:shaper" />

The input store holds up to eight exact item identities with Long quantities. Conversions complete immediately in the smallest lossless whole-item trade: nine nuggets become one ingot, one ingot becomes nine nuggets, and one plate becomes two rods. Items that cannot complete a whole trade remain visible in the input store.

The output store holds up to eight exact product identities. Matching products merge before an empty entry is used; one full identity does not block conversions that merge into another existing product.

The sample is a setting, not storage. Clicking it with an item selects a form without consuming the item; clicking it with an empty cursor clears it. The sample resets when the machine is dismantled, while input and output contents stay with the dropped machine item.

Use [IO and automation](io.md) for face input, automatic output, and network output. JEI lists lossless conversions around one base form for each material. Material packs can extend the synced `lazy:material_form` datapack registry, and pack authors can use the Shaper blacklist tags to disable unsafe inputs or outputs.
