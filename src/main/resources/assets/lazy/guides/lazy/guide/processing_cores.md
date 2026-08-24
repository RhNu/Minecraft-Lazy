---
navigation:
  parent: index.md
  title: Processing Cores
  icon: lazy:processing_core_t1
  position: 110
item_ids:
  - lazy:processing_core_t1
  - lazy:processing_core_t2
  - lazy:processing_core_t3
  - lazy:processing_core_t4
---

# Processing Cores

<ItemImage id="lazy:processing_core_t1" scale="1.4" />

Processing Cores are shared machine components. In the Simulation Chamber, they control its speed and output multiplier. The chamber accepts up to 64 cores, and cores are not consumed while it runs.

| Core | Speed | Output |
| --- | ---: | ---: |
| <ItemLink id="lazy:processing_core_t1" /> | ×1 | ×1 |
| <ItemLink id="lazy:processing_core_t2" /> | ×2 | ×4 |
| <ItemLink id="lazy:processing_core_t3" /> | ×6 | ×12 |
| <ItemLink id="lazy:processing_core_t4" /> | ×18 | ×36 |

These values are server configuration settings. Each core contributes its own speed and output multiplier, so adding cores scales both the simulation rate and virtual output rolls.
