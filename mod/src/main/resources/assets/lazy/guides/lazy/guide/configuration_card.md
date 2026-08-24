---
navigation:
  parent: index.md
  title: Configuration Card
  icon: lazy:configuration_card
  position: 85
item_ids:
  - lazy:configuration_card
---

# Configuration Card

<ItemImage id="lazy:configuration_card" scale="1.4" />

The Configuration Card stores one complete Lazy machine IO configuration. It can be reused for any compatible machine.

## Controls

- Right-click a machine to apply the card's configuration.
- Sneak-right-click a machine to copy its current IO configuration onto the card.
- Right-click the air while holding the card to open its IO panel directly.
- Place a machine while carrying a configured card to seed the new machine with that configuration. Blank cards are ignored.

## Stored settings

The card stores the IO mode, all six relative side modes, auto-eject, and the selected network target. A network target remains part of the card's link data, so it can be carried between machines without opening the source machine first.

If several carried cards contain different configurations, placement does not choose one automatically. Apply the intended card directly to the machine instead.
