---
navigation:
  parent: tools.md
  title: Teleporter
  icon: lazy:teleporter
  position: 30
item_ids:
  - lazy:teleporter
---

# Teleporter

<Column alignItems="center" fullWidth={true}>
  <ItemImage id="lazy:teleporter" scale="1.4" />
</Column>

The Teleporter moves you between the normal world and Lazy's private void dimension. Each Teleporter remembers its own return point and void destination.

## Use

- Hold use for one second to teleport. A successful use has a five-second cooldown by default.
- Entering the void records your current position as the return point. Using it in the void returns you to that saved position.
- The tooltip shows saved return and target coordinates. Failed teleports do not replace them.
- With Curios installed, equip it in the Teleporter slot. Bind **Activate Teleporter** in Controls to use it without charging; cooldowns and dimension restrictions still apply.

The server may change charge time, cooldown, safe-position search range, and safety-platform creation. Some dimensions may forbid teleporting.

<Recipe id="lazy:teleporter" />
