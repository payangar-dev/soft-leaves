# Soft Leaves

A tiny quality-of-life mod for Minecraft 26.2 (NeoForge + Fabric): leaves no longer block movement.

- **Pass through foliage** as if it were air, with a slight drag.
- **Speed-scaled resistance**: falling into a canopy slows you down far more than strolling through a bush. Vertical momentum you lose in the leaves also reduces your accumulated fall distance, so trees soften your landing.
- **Rustling**: moving through leaves plays the block's own leaf sounds.

Everything runs on vanilla mechanics through a single common mixin; there is no config, no content, and no dependency beyond the loader itself.

## Building

```
./gradlew build
```

Jars are produced in `fabric/build/libs/` and `neoforge/build/libs/`.

## License

MIT
