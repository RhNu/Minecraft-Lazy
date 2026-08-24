---
navigation:
  parent: index.md
  title: Teleporter
  icon: lazy:teleporter
  position: 105
item_ids:
  - lazy:teleporter
---

# Teleporter

<ItemImage id="lazy:teleporter" scale="1.4" />

The Teleporter moves you between the normal world and Lazy's private void dimension. Each Teleporter remembers its own return point and void destination.

## Use

- Hold use for one second to teleport. A successful use has a five-second cooldown by default.
- Entering the void records your current position as the return point. Using it in the void returns you to that saved position.
- The tooltip shows saved return and target coordinates. Failed teleports do not replace them.
- If Curios is installed, the Teleporter can be equipped in its dedicated slot and activated with the unbound Teleporter key. Key activation skips charging but still obeys cooldowns and dimension restrictions.

The server can change charge time, cooldown, safe-position search radius, and void safety-platform creation. Some dimensions may forbid activation.

<Recipe id="lazy:teleporter" />
