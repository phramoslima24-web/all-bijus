# Changelog

## 2.2.0
- Added an articulated 3D Jinchuuriki mantle layer for player renderers.
- Added progressive multi-tail visuals, animated tail tips, Chomei wings, Isobu shell, and creature-specific horns.
- Synced host transformation data to tracking clients so nearby players see the same form.
- Added transformation/release particle bursts.
- Added a dedicated white tint texture for translucent 3D rendering.

## 2.1.0
- Reworked the Jinchuuriki status screen with a layered panel, clearer information hierarchy, chakra bar, and responsive dimensions.
- Improved the in-game HUD contrast and clamped chakra progress to the valid 0–100% range.
- Updated project and mod metadata to version 2.1.0.

## Validation
- JSON resources and Blockbench model files are validated by the project generator.
- A Gradle build has not yet been run in this environment because Gradle is not installed. Build with Java 17 and Gradle 8.8, then test the player layer in Minecraft Forge 1.20.1.
