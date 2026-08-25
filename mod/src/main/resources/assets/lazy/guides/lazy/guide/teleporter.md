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

The Teleporter opens a server-authoritative menu for creating and visiting encapsulated spaces in Lazy's void dimension. Your outside return point and last selected space belong to your player, not to an individual item.

## Use

- Right-click while holding the Teleporter to open its menu.
- Create a 15×15×15 encapsulated space, select it from the newest-first list, rename it, or travel to it.
- The short ID is always visible; hover it for the complete UUID. Deleting a space requires confirmation and destroys its walls and every block and entity inside.
- **Travel to origin** visits the central void platform. **Return outside** uses your last successful outside departure point, then falls back to your respawn point.
- A successful teleport has a five-second cooldown by default. Failed travel does not overwrite your return point.
- With Curios installed, equip the Teleporter and bind **Activate Teleporter** to open the same menu.

You can only list and manage spaces you created. Server operators can manage every space. The server may change cooldown, safe-position search range, the per-player space limit, and dimension restrictions.

<Recipe id="lazy:teleporter" />
