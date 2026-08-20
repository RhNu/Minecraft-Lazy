---
navigation:
  parent: index.md
  title: Buffer
  icon: lazy:buffer
  position: 10
item_ids:
  - lazy:buffer
---

# Buffer

<BlockImage id="lazy:buffer" scale="1.15" />

The Buffer is a large mixed item-and-fluid storage machine. It is useful as a local cache or as a shared hand-off point between machines.

## Storage

- 8 item slots hold one item type per slot, up to 256 items in each slot.
- 4 fluid tanks hold one fluid type per tank, up to 64,000 mB in each tank.
- Both inventories are exposed to adjacent capabilities and can be viewed in the machine screen.

Right-click opens the screen. Sneak-right-click reports the total stored items and fluid amount without opening it. The clear button destroys all stored contents after confirmation.

## Automation

The Buffer can accept and emit items and fluids through the common [IO settings](io.md). Stored contents remain in the dropped machine item when the Buffer is broken or dismantled.

<Recipe id="lazy:buffer" />
