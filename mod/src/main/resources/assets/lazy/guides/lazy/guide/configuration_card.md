---
navigation:
  parent: tools.md
  title: Configuration Card
  icon: lazy:configuration_card
  position: 10
item_ids:
  - lazy:configuration_card
---

# Configuration Card

<Column alignItems="center" fullWidth={true}>
  <ItemImage id="lazy:configuration_card" scale="1.4" />
</Column>

The Configuration Card copies and reuses a Lazy machine's [IO settings](io.md).

## Controls

- Right-click a machine to apply the card.
- Sneak-right-click a machine to copy its settings.
- Right-click the air to edit the card directly.
- Place a machine while carrying one configured card to apply those settings immediately.

## Stored settings

The card stores the mode, all six side settings, auto-eject, and the selected network target.

Blank cards are ignored during placement. If carried cards contain different settings, the new machine stays at its defaults; apply the intended card directly instead.

With Curios installed, a card in the Configuration Card slot also participates in automatic placement and network selection.

<Recipe id="lazy:configuration_card" />
