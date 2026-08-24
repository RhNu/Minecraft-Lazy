---
navigation:
  parent: tools.md
  title: Modular Configurator
  icon: lazy:modular_configurator
  position: 40
item_ids:
  - lazy:modular_configurator
---

# Modular Configurator

<Column alignItems="center" fullWidth={true}>
  <ItemImage id="lazy:modular_configurator" scale="1.4" />
</Column>

The Modular Configurator stores materials used while configuring supported machines from other mods. It has 18 slots, each holding up to 1024 matching items.

## Controls

- Right-click the air to open storage.
- Right-click an unsupported block to open storage instead.
- Sneak-right-click the air to clear saved machine configurations. Stored materials remain.

Blocks such as chests still receive their normal interaction before the held item, so opening the block can take priority.

## Mekanism

With Mekanism installed:

- Sneak-right-click a machine to copy its settings and installed upgrade counts.
- Right-click another machine of the exact same type to apply them.
- Related machines and different factory tiers do not count as the same type.

After applying the settings, the tool uses stored Mekanism upgrade items to fill any missing upgrades. Existing extra upgrades are never removed. Failed configuration or permission checks consume nothing.

The action bar reports whether configuration succeeded and whether enough upgrades were available.

<Recipe id="lazy:modular_configurator" />
