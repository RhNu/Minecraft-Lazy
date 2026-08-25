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

The Replicator repeatedly creates a marked resource without consuming a source.

## Resource and interval

- There is one resource slot. Left-click it with a carried item, or drag an item or fluid from JEI onto the same spot. With Mekanism installed, chemicals can be dragged directly from JEI as well.
- Right-click the resource slot while carrying a container of supported resource to mark its first non-empty contained resource without consuming or modifying the container.
- Click the amount button, or middle-click the resource slot, to enter an exact value or use `−` / `+` in resource-sized steps.
- Marking the same resource again preserves its amount. A different resource starts at its own default. Left-click the slot with an empty cursor to clear it.

The interval button cycles through four speeds:

| Setting   |  Interval |
| --------- | --------: |
| Fast      |  10 ticks |
| Normal    |  20 ticks |
| Slow      | 100 ticks |
| Very slow | 200 ticks |

## Automation

Use [IO settings](io.md) to send copies to an adjacent handler for the selected resource, or to a supported network. Mekanism chemicals are sent through the adjacent chemical capability. Unaccepted output is not buffered; the Replicator simply offers a fresh copy on its next operation.

The marked resource and interval remain in the dropped machine when it is broken or dismantled. IO settings reset when it is placed again.

<Recipe id="lazy:replicator" />
