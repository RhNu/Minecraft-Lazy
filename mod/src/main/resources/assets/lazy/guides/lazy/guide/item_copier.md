---
navigation:
  parent: index.md
  title: Item Copier
  icon: lazy:item_copier
  position: 30
item_ids:
  - lazy:item_copier
---

# Item Copier

<BlockImage id="lazy:item_copier" scale="8" />

The Item Copier repeatedly creates the selected item and pushes it into adjacent inventories or a compatible network. It does not consume a source stack.

## Template and interval

Open the screen and click the ghost template slot while carrying an item to mark it, or drag the target output into it from JEI. Click it with an empty cursor to clear the template. The template is stored as one item, while each push attempts a full stack of that item's normal maximum size.

The interval button cycles through these server-side schedules:

| Setting | Interval |
| --- | ---: |
| Fast | 10 ticks |
| Normal | 20 ticks |
| Slow | 100 ticks |
| Very slow | 200 ticks |

## Automation

Use the common [IO settings](io.md) to choose adjacent face output or a network target. The template and interval are preserved when the machine is broken or dismantled.

<Recipe id="lazy:item_copier" />
