---
navigation:
  parent: index.md
  title: Modular Configurator
  icon: lazy:modular_configurator
item_ids:
  - lazy:modular_configurator
---

# Modular Configurator

The Modular Configurator is a general-purpose integration tool. The item and its 18 storage slots always belong to Lazy; installed integrations decide which materials those slots accept and what happens when the tool is used on another mod's block. Each slot holds up to 1024 matching items.

## Controls

- Right-click the air to open its storage.
- When using it on a block has no integration effect, the storage opens instead.
- Sneak-right-click the air to clear every saved integration configuration. Stored materials are not removed.

Blocks such as chests still receive their normal interaction before the held item, so opening the block can take priority.

## Mekanism

With Mekanism installed, the tool reproduces its Configuration Card workflow:

- Sneak-right-click a Mekanism machine to copy its complete configuration and the number of each installed upgrade.
- Right-click a machine of the exact same registered type to apply the configuration.
- Configuration is never pasted across different machine types, including related factory tiers.

After a configuration is applied successfully, the tool compares the target's installed upgrades with the copied counts. It consumes Mekanism upgrade items from its storage only to fill the difference. Extra upgrades already present on the target are never removed. If configuration, security, or machine-type validation fails, no upgrade items are consumed.

The action bar reports whether configuration succeeded and whether upgrades were fully installed, partially installed, already satisfied, or missing from storage.
