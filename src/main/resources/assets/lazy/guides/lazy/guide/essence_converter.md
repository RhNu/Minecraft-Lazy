---
navigation:
  parent: index.md
  title: Essence Converter
  icon: lazy:essence_converter
  position: 60
item_ids:
  - lazy:essence_converter
---

# Essence Converter

<BlockImage id="lazy:essence_converter" scale="1.15" />

The Essence Converter is available when Mystical Agriculture is installed. It combines essence into a selected target tier and exposes the completed target essence as output.

## Target and conversion

Select a target tier before inserting essence. A target cannot be changed while the converter contains complete output or a fractional remainder. The converter accepts the six Mystical Agriculture tiers that are present; Insanium becomes available when Mystical Agradditions is installed.

Input is converted by inferium value, so lower and higher tiers can be mixed. The fractional remainder is retained instead of being discarded. The default capacity is 1,000,000,000,000 complete target essences and can be changed in the server configuration.

The input slot accepts essence, the output slot extracts the selected tier, and the clear action destroys all stored essence. Route output to adjacent inventories or a compatible network with the common [IO settings](io.md).

If Mystical Agradditions is removed while an Insanium target is saved, the stored value is downgraded to Supremium rather than lost.
