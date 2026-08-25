---
navigation:
    parent: machines.md
    title: Replicator
    icon: lazy:replicator
    position: 40
item_ids:
    - lazy:replicator
---

# Replicator

<Column alignItems="center" fullWidth={true}>
  <BlockImage id="lazy:replicator" scale="8" />
</Column>

The Replicator repeatedly creates a marked item or fluid without consuming a source.

## Resource and interval

- There is one resource slot. Click it with a carried item, or drag either an item or fluid from JEI onto the same spot.
- Click the amount button, or right- or middle-click the resource slot, to enter an exact value or use `−` / `+` in resource-sized steps.
- Marking the same resource again preserves its amount. A different resource starts at its own default. Left-click the slot with an empty cursor to clear it.

The interval button cycles through four speeds:

| Setting   |  Interval |
| --------- | --------: |
| Fast      |  10 ticks |
| Normal    |  20 ticks |
| Slow      | 100 ticks |
| Very slow | 200 ticks |

## Automation

Use [IO settings](io.md) to send copies to adjacent item or fluid handlers, or to a supported network. Unaccepted output is not buffered; the Replicator simply offers a fresh copy on its next operation.

The marked resource and interval remain in the dropped machine when it is broken or dismantled. IO settings reset when it is placed again.

<Recipe id="lazy:replicator" />
