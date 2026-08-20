---
navigation:
  parent: index.md
  title: Repairer
  icon: lazy:repairer
  position: 40
item_ids:
  - lazy:repairer
---

# Repairer

<BlockImage id="lazy:repairer" scale="1.15" />

The Repairer restores durability to one damaged, damageable item at a time. It does not require an ingredient or consume the item.

Place the item in the input slot and press the anvil button. Each press repairs a random percentage of the item's maximum durability between the configured minimum and maximum values. The defaults are 5%–15%; both values are server configuration settings and are clamped to 1%–100%.

The input belongs to the player rather than the machine. It is returned as a separate drop when the Repairer is broken or dismantled. Compatible integrations can run a post-repair hook; Silent Gear uses this to recalculate its gear data.

<Recipe id="lazy:repairer" />
