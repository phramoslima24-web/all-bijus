# Changelog

## 2.1.0
- Reworked the Jinchuuriki status screen with a layered panel, clearer information hierarchy, chakra bar, and responsive dimensions.
- Improved the in-game HUD contrast and clamped chakra progress to the valid 0–100% range.
- Updated project and mod metadata to version 2.1.0.

## Validation
- All resource JSON files and all eight Blockbench `.bbmodel` files parse successfully.
- A Gradle build was not run in this environment because Gradle is not installed and external network access is unavailable. Use the included GitHub Actions workflow or run `gradle build` with Java 17 and Gradle 8.8.
