Ars Magica 2.5
==========

Ars Magica 2.5 - Carrying the Torch of True Magic

## New Features (TL;DR)

- Re-added Staffs
- Re-added Caster Blocks and Runes
- Added New Components, Shape, Modifiers and Talents
- Added an enormous amount of new spell icons
- Added compat with Not Enough Items and Magic Bees
- Added recipes for multiple previously uncraftable items
- Added multiple new items, blocks, contraptions and rituals with utility
- Added and changed multiple core mod mechanics
- Re-added and implemented multiple lost and unfinished features
- Refined Altar, Burnout, Leveling and Bosses
- Improved/optimized startup time
- Fixed missing lang entries
- Fixed a plethora of bugs and crashes
- See the Arcane Compendium in-game: for a detailed and thorough documentation
- See the commits and release notes: for a better and more specific summary

### Testing

Unit tests live in `src/test/java` and run on a Java 8 toolchain. The non-shipped in-game test mod lives in
`src/gameTest/java` and is only added to the classpath by the game-test tasks.

```sh
./gradlew check                 # unit tests, JaCoCo report/floor, Spotless and Checkstyle
./gradlew gameTestServer        # boots a real dedicated server and exits with the result
./gradlew gameTestClient        # boots the client and exits with the result (needs a display)
./gradlew jacocoTestReport      # HTML/XML coverage for the unit tests
./gradlew jacocoGameTestReport  # coverage from the in-game run
```

### License
This mod is open sourced under the Creative Commons 3.0 Attribution Non-Commercial License
https://creativecommons.org/licenses/by-nc/3.0/legalcode

Summary
https://creativecommons.org/licenses/by-nc/3.0/

You are free to:
* Share — copy and redistribute the material in any medium or format
* Adapt — remix, transform, and build upon the material
* The licensor cannot revoke these freedoms as long as you follow the license terms.

Under the following conditions:
* Attribution — You must give appropriate credit, provide a link to the license, and indicate if changes were made. You may do so in any reasonable manner, but not in any way that suggests the licensor endorses you or your use.
* NonCommercial — You may not use the material for commercial purposes.
* No additional restrictions — You may not apply legal terms or technological measures that legally restrict others from doing anything the license permits.

The exceptions to this license are textures, majority of which are property of D3miurge, licensed to this mod for it's use.
More information cam be found here: http://www.minecraftforum.net/user/2252536-d3miurge/#am2

Spell icons used include Painterly Spell Packs 1 through 4, courtesy of J. W. Bjerk (eleazzaar) at opengameart.com.

Some icons used courtesy of Ails http://www.alteredgamer.com/rpg-maker/100053-vx-custom-icons

Some textures used have been adapted from the Sanity texturepack by Alvoria

https://www.curseforge.com/minecraft/texture-packs/alvorias-sanity

Parts of models by Staralighto have also been used.

Sounds are courtesy of opengameart.com and freesound.org, and licenses are that of their respective owners.
