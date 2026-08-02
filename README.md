# Soft Leaves

A tiny quality-of-life mod for Minecraft 1.21.11 (NeoForge + Fabric): leaves no longer block movement.

- **Pass through foliage** as if it were air, with a slight drag.
- **Speed-scaled resistance**: falling into a canopy slows you down far more than strolling through a bush. Vertical momentum you lose in the leaves also reduces your accumulated fall distance, so trees soften your landing.
- **Rustling**: moving through leaves plays the block's own leaf sounds.

Everything runs on vanilla mechanics through a single common mixin; there is no content and no dependency beyond the loader itself.

## Configuration

`config/soft_leaves.json` is written on first launch and read once at startup:

- `resistance` (default `1.0`, range `0.0` to `3.0`): multiplies how hard foliage brakes whatever moves through it. `0` keeps leaves passable but stops them slowing anything down, `3` turns a canopy into molasses. Speed still scales the braking, this only shifts how strong the whole effect is.

In multiplayer, give the server and its clients the same value: a mismatch makes their movement predictions disagree.

## Building

```
./gradlew build
```

Jars are produced in `fabric/build/libs/` and `neoforge/build/libs/`.

## License

MIT
