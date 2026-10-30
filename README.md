# Shadow Tweaks

Client-side control over entity shadows in Minecraft: size, opacity, render
distance, and which kinds of entity get one at all.

Fabric, Minecraft 1.21.11. Every setting applies instantly, no restart needed,
and every default matches vanilla exactly.

## What it does that the usual toggle doesn't

Most performance mods give you a single on/off switch for entity shadows.
Shadow Tweaks gives you the dial instead of the switch, and two things no
toggle can do:

**Shadows by entity category.** Keep shadows on players so you can read
positioning in a fight, and drop them for the floor full of items and
experience orbs. Five categories: players, hostile mobs, passive mobs, items
and XP orbs, everything else.

**Render distance.** Vanilla fades every shadow out at 16 blocks and that
limit is written into the code:

```java
float alpha = (float)((1.0 - state.squaredDistanceToCamera / 256.0)
                      * (double) this.getShadowOpacity(state));
```

256 is 16 squared. Past that, alpha is zero and the shadow is gone, however
clear your view is. Shadow Tweaks replaces the constant, so distant mobs keep
their shadow out to 64 blocks if you want them to.

## Features

- **Shadow size** — 0.00 to 3.00. At 0.00 shadows disappear, and the whole
  shadow calculation is skipped, so it costs less than vanilla.
- **Opacity** — fade shadows down without removing them.
- **Render distance** — 4 to 64 blocks. Vanilla is 16.
- **Per-category visibility** — five independent switches.
- **Client-side only** — no server component, no network packets, no gameplay
  changes. Safe on any server.

## Configuration

Open **Mods → Shadow Tweaks → ⚙** (requires Mod Menu).

| Tab | Group | Options |
|---|---|---|
| General | — | Mod on/off |
| Shadows | Appearance | Shadow size, opacity, render distance |
| | Show Shadows For | Players, hostile mobs, passive mobs, items and XP orbs, everything else |

Available in **English** and **German** — the screen follows your game
language.

## If nothing seems to happen

Minecraft has its own **Entity Shadows** switch in the video settings, and
Sodium Extra exposes the same one. Vanilla checks it before any shadow is
calculated, so with it off, Shadow Tweaks does nothing by design. The config
screen tells you when it detects this.

## Compatibility

Every hook uses MixinExtras' stackable injectors (`@ModifyExpressionValue`).
There is not a single exclusive injector (`@Redirect`, `@ModifyConstant`,
`@ModifyArgs`) in this mod, so other mods touching the same calls can coexist
instead of one of them failing to load.

All three hooks sit in one vanilla method, `EntityRenderer.updateShadow`, at
the point where the shadow radius and opacity have already been resolved
polymorphically. That means entities whose renderer overrides those methods —
slimes, striders, turtles, villagers, and anything a third-party mod brings
along — are covered too.

## Requirements

- Minecraft **1.21.11**
- **Fabric Loader 0.20.1** or newer
- [YetAnotherConfigLib](https://modrinth.com/mod/yacl)
- [Mod Menu](https://modrinth.com/mod/modmenu) *(optional — needed for the
  settings button)*

## License

Apache License 2.0 — free to use, modify and redistribute. The source is open,
so server admins can verify the mod does nothing beyond rendering.
