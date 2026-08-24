---
navigation:
  parent: machines.md
  title: Buffer
  icon: lazy:buffer
  position: 20
item_ids:
  - lazy:buffer
---

# Buffer

<Column alignItems="center" fullWidth={true}>
  <BlockImage id="lazy:buffer" scale="8" />
</Column>

The Buffer stores large amounts of items and fluids in one block.

## Storage

- 8 item slots, each holding one item type and up to 256 items.
- 4 fluid tanks, each holding one fluid type and up to 64,000 mB.
- The item slots support normal clicking, splitting, dragging, and shift-clicking.

- Right-click to open the screen.
- Sneak-right-click to show the total stored items and fluids in the action bar.
- The clear button destroys all stored contents after confirmation.

## Automation

Adjacent machines can insert or extract contents. Use [IO settings](io.md) for automatic output.

Stored items and fluids remain inside the dropped Buffer when it is broken or dismantled. IO settings reset when it is placed again.

<Recipe id="lazy:buffer" />
