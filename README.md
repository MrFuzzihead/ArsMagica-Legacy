# Ars Magica 2.5

[![](https://img.shields.io/badge/Minecraft-1.7.10-62a34a)](https://minecraft.wiki/w/Java_Edition_1.7.10)
[![](https://img.shields.io/badge/Forge-10.13.4.1614-1e2b4f)](https://files.minecraftforge.net/net/minecraftforge/forge/index_1.7.10.html)
[![](https://img.shields.io/badge/Java-8-orange)](https://adoptium.net/temurin/releases/?version=8)
[![](https://img.shields.io/badge/License-MIT-blue)](LICENSE)

Ars Magica 2.5 - Carrying the Torch of True Magic: a continuation of the classic 1.7.10 magic mod, kept building and
running while preserving the original spellcrafting, bosses, worldgen and progression.

Originally created by Mithion; this fork continues the 1.7.10 line and is maintained by MrFuzzihead for the GTNH pack.

## What it adds

| Area | Highlights |
|---|---|
| Magic | spell crafting with components, shapes, modifiers and talents |
| Equipment | staffs, foci, mage armor, bound tools and artifacts |
| Blocks | caster blocks and runes, crafting altar, obelisks, essence network, workbenches |
| World | Witchwood forests, Moo Moo Farm, etherium, bosses and dungeons |
| Progression | altar, burnout, leveling, affinities, skill trees and the Arcane Compendium |
| Compatibility | NEI, Magic Bees, Thaumcraft, Forestry, Baubles and CoFH/Thermal Expansion |

The fork restores staffs, caster blocks and runes, adds new spell parts and icons, fills in missing recipes and lang
entries, and cleans up a long list of crashes and latent bugs. See the Arcane Compendium in-game for the full
documentation and the commit history for the detailed changelog.

## Requirements

| Dependency | Version |
|---|---|
| Minecraft | 1.7.10 |
| Minecraft Forge | 10.13.4.1614 |
| [AnimationAPI](https://www.curseforge.com/minecraft/mc-mods/animationapi) | required |
| Java | 8 |
| Baubles-Expanded, CodeChickenCore, Forestry, Magic Bees, Thaumcraft, CoFH Core, Thermal Expansion/Foundation | optional |

## Building

Everything is handled by the `com.gtnewhorizons.gtnhconvention` plugin:

```sh
./gradlew build              # compile, unit tests, format check, coverage gate
./gradlew test               # unit tests only
./gradlew jacocoTestReport   # HTML coverage -> build/reports/jacoco/test/html
./gradlew runClient          # dev client
./gradlew runServer          # dev server
```

Gradle runs on Java 25 (see `.java-version`). The test suite runs on a Java 8 toolchain, matching the mod's Java 8
bytecode target and the newest Mockito release that still runs there (4.x).

### Tests

| Command | What it runs |
|---|---|
| `./gradlew test` | 19 headless JUnit 4 tests (JUnit 4.13.2 + Mockito 4.11.0, Java 8 launcher) |
| `./gradlew check` | the above plus Spotless, Checkstyle, the coverage floor and the gameTest compile |
| `./gradlew gameTestServer` | boots a real dedicated server, runs 109 in-game assertions, then exits with the result. Also writes an in-game coverage report to `build/reports/jacoco/gameTest/html` |
| `./gradlew gameTestClient` | boots a real client, runs 23 client-side assertions at the main menu, then exits. Needs a display; use Xvfb on headless machines |
| `./gradlew jacocoGameTestReport` | reports coverage from the last in-game run using the classes the JaCoCo agent instrumented |

The in-game mod lives in `src/gameTest/java` and is never shipped. It covers what cannot be tested headless: the real
block/item registries, TileEntity creation and world placement, the crafting recipe and ore-dictionary lists, the API
singletons, the client proxy, key bindings, creative tabs and localisation.

`gameTestServer` writes `run/server/eula.txt` and a deterministic flat `server.properties` before launching, so it needs
no manual setup. It prints `ARSMAGICA_GAMETEST_OK` on success and exits non-zero otherwise; the client run prints
`ARSMAGICA_CLIENTTEST_OK`. `.github/workflows/game-test.yml` checks both markers.

## Configuration

`config/AM2/AM2.cfg` and `config/AM2/SkillConf.cfg`, generated on first launch. The notable options are the damage
multiplier, the Moo Moo Farm dimension and Witchwood Forest biome IDs, the graphics level, retroactive worldgen and
the alternative start that gates the Arcane Compendium behind Witchwood Trees.

## Contributing

Bug reports and pull requests are welcome. Run `./gradlew check` before opening a pull request, and run
`./gradlew gameTestServer` when touching registration, worldgen, recipes or the public API. The game-test workflow runs
both the dedicated-server and Xvfb client suites.

## License

The source code in this repository is licensed under the [MIT License](LICENSE). The original Ars Magica 2 assets
(textures, sounds, models and spell icons) remain under their original licenses and are not covered by the MIT
license; see [Credit](#credit) for attribution.

## Credit

Ars Magica 2 was created by Mithion. This fork is maintained by MrFuzzihead.

The exceptions to the original asset license are textures, the majority of which are the property of D3miurge,
licensed to this mod for its use. More information can be found at
http://www.minecraftforum.net/user/2252536-d3miurge/#am2.

Spell icons include Painterly Spell Packs 1 through 4, courtesy of J. W. Bjerk (eleazzaar) at opengameart.com. Some
icons are courtesy of Ails at http://www.alteredgamer.com/rpg-maker/100053-vx-custom-icons. Some textures were adapted
from the Sanity texture pack by Alvoria: https://www.curseforge.com/minecraft/texture-packs/alvorias-sanity. Parts of
models by Staralighto were also used.

Sounds are courtesy of opengameart.com and freesound.org, and their licenses remain those of their respective owners.
