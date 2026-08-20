---
navigation:
  parent: index.md
  title: IO and automation
  icon: lazy:configuration_card
  position: 80
---

# IO and automation

<ItemImage id="lazy:configuration_card" scale="1.4" />

The IO panel is shared by machines that move items, fluids, or FE. It keeps the machine's local inventory separate from the way outputs leave the machine.

## Modes

- **Passive** keeps automatic output disabled. The machine still exposes the capabilities it supports to adjacent blocks.
- **Faces** lets each side be disabled, input, output, or both. Enable auto-eject to push stored outputs into sides marked as output.
- **Network** sends supported outputs to the selected network provider. A provider may support items, fluids, FE, or a combination of them.

Use the side buttons with the machine's front as the reference point. A machine can be rotated with a wrench; the face configuration follows its new facing.

The portable <ItemLink id="lazy:energy_battery" /> is documented on the [Energy Battery](energy_battery.md) page.

## Configuration Card

<ItemLink id="lazy:configuration_card" /> stores the complete IO configuration. Right-click a machine to apply a card, or sneak-right-click to copy the machine's configuration back to it. The card can also be opened in hand to edit the same panel.

When a configured card is used to place a machine, the machine starts with that configuration. Network targets remain linked through the card rather than being copied into the machine as a runtime network object.

## Breaking machines

Breaking a machine preserves its stored contents in the dropped machine item. Items that the machine only holds for the player, such as the Repairer's input, are dropped separately. The same rules apply when a machine is dismantled with a wrench.
