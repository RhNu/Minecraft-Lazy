---
navigation:
  parent: index.md
  title: Commands
  icon: lazy:teleporter
  position: 40
---

# Commands

## Rise to the surface

<CommandLink command="/lazy rise">Run /lazy rise</CommandLink>

`/lazy rise` is unavailable in the Void dimension.

Searches upward in your current column and moves you to the first safe position with a clear view of the sky.

## Damage protection

Protection commands require operator permission level 2.

- <CommandLink command="/lazy protection damage_cap">Show the current settings</CommandLink>
- <CommandLink command="/lazy protection damage_cap on">Enable protection</CommandLink>
- <CommandLink command="/lazy protection damage_cap off">Disable protection</CommandLink>
- `/lazy protection damage_cap set <value>` — set the maximum damage from one hit
- <CommandLink command="/lazy protection damage_cap reset">Reset saved settings</CommandLink>

### Damage cap values

- A positive value limits the damage taken from each hit.
- A value of `0` prevents normal damage while protection is enabled.
- Damage that bypasses invulnerability is not affected.
- Resetting removes the saved setting and leaves protection disabled with a value of `0`.
