---
navigation:
  parent: machines.md
  title: Item Copier
  icon: lazy:item_copier
  position: 40
item_ids:
  - lazy:item_copier
---

# Item Copier

<Column alignItems="center" fullWidth={true}>
  <BlockImage id="lazy:item_copier" scale="8" />
</Column>

The Item Copier repeatedly creates the selected item without consuming a source stack.

## Template and interval

- Click the ghost template slot with an item to select it.
- Drag an item from JEI onto the slot to select it from a recipe view.
- Click the slot with an empty cursor to clear it.
- Each operation attempts to output one normal maximum stack.

The interval button cycles through four speeds:

| Setting | Interval |
| --- | ---: |
| Fast | 10 ticks |
| Normal | 20 ticks |
| Slow | 100 ticks |
| Very slow | 200 ticks |

## Automation

Use [IO settings](io.md) to send copies to adjacent inventories or a supported network. If the destination cannot accept the full result, the copier waits and tries again later.

The template and interval remain in the dropped machine when it is broken or dismantled. IO settings reset when it is placed again.

<Recipe id="lazy:item_copier" />
