---
navigation:
  parent: automation.md
  title: IO and automation
  icon: lazy:configuration_card
  position: 10
---

# IO and automation

<Column alignItems="center">
  <ItemImage id="lazy:configuration_card" scale="1.4" />
</Column>

The IO panel controls how a machine accepts and sends items, fluids, and FE.

## Output modes

- **Passive:** The machine does not send anything by itself. Pipes and nearby machines can still access supported contents.
- **Faces:** Set each side to disabled, input, output, or both. Auto-eject sends stored outputs through sides marked as output.
- **Network:** Send supported outputs to a selected network.

Side settings use the machine's front as their reference. They follow the machine when it is rotated with a wrench.

## Select a network

### Applied Energistics 2

1. Put a <ItemLink id="lazy:configuration_card" /> in the linking slot of an AE2 Wireless Access Point.
2. Carry the linked card in your hand, inventory, or Curios slot.
3. Open the machine's IO panel and select **AE2 ME Network**.

A card in your hand takes priority. If your inventory contains cards linked to different networks, hold the card you want before selecting the network.

AE2 accepts items and fluids. With Applied Flux installed, it can also accept FE.

### Beyond Dimensions

1. Set the network you want as your primary Beyond Dimensions network.
2. Open the machine's IO panel and select **Beyond Dimensions**.

## Interrupted transfers

- If the selected network is gone, the machine disconnects and returns to passive mode.
- If a transfer cannot be confirmed, output pauses to avoid sending the same contents twice.
- Check the destination before selecting **Resume**. Select **Disconnect** to return to passive mode instead.

## Configuration cards

The <ItemLink id="lazy:configuration_card" /> can copy, edit, and apply the complete IO setup. See [Configuration Card](configuration_card.md) for all controls and placement rules.

## Breaking a machine

- Stored outputs remain inside the dropped machine item.
- Player-owned input, such as an item placed in the Repairer, drops separately.
- IO mode, side settings, auto-eject, and network selection reset when the machine is placed again.

The same rules apply when dismantling a machine with a wrench.
