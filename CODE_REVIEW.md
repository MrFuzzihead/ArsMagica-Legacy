# Ars Magica 2.5 (Minecraft 1.7.10 / Forge) — Code Review

Review of the current working tree at commit `01775e6` (branch `feature-Refactors`).

- **Source size:** 1,053 Java files, ~156,400 lines in `src/main/java`.
- **Tests:** 4 JUnit files (19 tests) and 3 game-test files (~370 lines, 109 server assertions + 23 client assertions).
- **Measured unit-test coverage:** 177 of 69,939 lines = **0.25%** (39 of 30,318 branches = 0.13%). The `check` gate is 0.2% line coverage, so it is effectively a no-op.
- **Review method:** full read of the core lifecycle, event handlers, power system, spell construction/casting, networking, persistence, worldgen, API, build files, and produced logs/artifacts. Findings marked **Verified** are directly visible in the code; **Likely** findings depend on a runtime condition that is not exercised by the current tests.

The codebase is large and has clearly had recent stability work (the CME fixes, the game-test harness, the network-thread guards in `ServerTickHandler`, and the game-test marker checks are all good). The issues below are what is still left, ordered by impact.

---

## 1. Executive summary

| # | Severity | Finding | Primary evidence |
|---|---|---|---|
| 1 | **Critical** | The ASM coremod is not registered in the produced jar; all 34 active `@Fix` patches and every `BytecodeTransformers` transformation are inert. Tickrate control, ethereal-player filtering, shrink/flip rendering, redstone-inlay power, and >127 potion-ID persistence silently do not work. | `gradle.properties:122` (`coreModClass =` empty), jar `META-INF/MANIFEST.MF` contains only `FMLAT`, no `FMLCorePlugin` in `run/*/logs` |
| 2 | **Critical** | The server trusts client packets for skill points, known spell parts and spell creation. A modified client can grant itself any skills/spells, create arbitrary spells without reagents or validation, and edit other players/blocks anywhere. | `SkillData.java:319-380`, `AMPacketProcessorServer.java:310-331`, `TileEntityInscriptionTable.java:410-415`, `576-608` |
| 3 | **Critical** | Packet readers allocate attacker-controlled sizes with no bounds: `new byte[len]` / `new int[len]`. A malicious client can OOM or crash the server. | `AMDataReader.java:128-141`, `163-174` |
| 4 | **Critical** | The power-node persistence cache loses data for unloaded chunks, and its file cache is not dimension/world-scoped. Power can vanish after a chunk unload/save, and DIM0/DIM-1 (or two saves) can read/write each other's power files. | `PowerNodeCache.java:28,54-72,87-131,229-253` |
| 5 | **Critical** | `PowerNodeRegistry` uses an inconsistent `TreeMap` comparator. Depending on insertion order, `get(chunk)` can fail to find an existing chunk, making nodes stop working after neighbouring chunks load. | `PowerNodeRegistry.java:474-485` |
| 6 | **Critical** | `ExtendedProperties.saveNBTData` reuses the `persistentobj` index counter for the compendium entries; on load it always starts at 0. If a player has any extra properties, compendium unlock data is lost/corrupted on relog. | `ExtendedProperties.java:1210-1225` vs `1287-1293` |
| 7 | **High** | The inscription table `MAKE_SPELL` path creates and gives a spell with no `SpellValidator` check, no reagent check, no distance/owner check, and a blind `(EntityPlayer)` cast. | `TileEntityInscriptionTable.java:410-415,576-608` |
| 8 | **High** | `AMChunkLoader` leaks tickets and can keep chunks loaded forever. It never reuses an existing ticket, never removes released tickets from its map, and `ticketsLoaded` does not repopulate the map. `FortifyTime`'s expiry path also dereferences a potentially missing tile entity. | `AMChunkLoader.java:20-89,92-123`, `AMEventHandler.java:1250-1272` |
| 9 | **High** | Several per-tick block/TE updates are network floods: the mana battery sets metadata and calls `markBlockForUpdate` every tick; the enervator sends full TE NBT to all nearby clients every tick. | `TileEntityManaBattery.java:49-62`, `TileEntityEntropicEnervator.java:87-124` |
| 10 | **High** | Configuration values are used as `Random.nextInt(bound)` with no validation; setting `0`, a negative value, or equal ore min/max crashes chunk generation. The meteor spawner has the same problem. | `AM2WorldDecorator.java:170,185,221`, `MeteorSpawnHelper.java:52-55` |
| 11 | **High** | `AMEnchantmentHelper` removes the wrong enchantment in its zero-level path (`silkTouch` instead of `fortune`/`looting`), so enchantments get stuck and silk touch is silently removed. | `AMEnchantmentHelper.java:41-59` |
| 12 | **High** | Lingering spells are ticked once per world tick (and again from the client handler in single-player), so their speed depends on how many dimensions are loaded and they can double-tick locally. | `ServerTickHandler.java:88-103`, `ClientTickHandler.java:397-414` |
| 13 | **High** | The public API's `spellPartManager` is never assigned (the init line is commented out); any addon calling `ArsMagicaApi.registerSkillTreeEntry`/`getSpellPartManager` gets an NPE. | `AMCore.java:350`, `ArsMagicaApi.java:86-89,203-208` |
| 14 | **Medium** | Many static collections are not world-scoped, not cleared on unload, and are accessed from both the client and server threads in single-player. This leaks worlds/entities and creates data races. | `AMEventHandler.java:635-638,1554-1556`, `ItemFrameWatcher.java:44`, `FlickerOperatorMoonstoneAttractor.java:20` |
| 15 | **Medium** | `ItemSpellBook.GetActiveSlot` trusts NBT; a crafted/corrupt `spellbookactiveslot` outside `0..7` crashes on right-click/scroll. | `ItemSpellBook.java:224-230,305-311` |

---

## 1.1 Implementation status

This section tracks the review items that have been closed since the report was written. Each entry names the commit
and the automated tests that now cover the fix.

| Finding | Status | Commit | Automated coverage |
|---|---|---|---|
| 2.1 Coremod not registered | **Fixed** — GTNH Mixin loader is registered and guarded | `53b3d3f` … `d6cd9ea` | `verifyMixinManifest`, `MixinsTest`, server/client game tests |
| 2.2 Client-authoritative packets | **Fixed** — skill sync direction, inscription-table ownership/creative/recipe checks, coordinate reach checks | `8d6d3fc` | `SkillDataTest`, `PacketAuthorityTest`, `TileEntityInscriptionTableAuthorityTest` |
| 2.3 Unbounded packet allocations | **Fixed** — negative/oversized lengths rejected, 1 MiB NBT cap, `readFully` | `ceb14c9` | `AMDataReaderTest` |
| 2.4 Power-node persistence | **Fixed** — world+dimension+region keys, flush on save/unload, non-destructive `readFromNBT` | `1353eff` | `PowerNodeCacheTest`, `PowerNodeEntryNbtTest` |
| 2.5 `PowerNodeRegistry` comparator | **Fixed** — lexicographic comparator | `ceb14c9` | `PowerNodeRegistryComparatorTest` |
| 2.6 Compendium NBT indices | **Fixed** — per-map index helpers | `ceb14c9` | `ExtendedPropertiesNbtTest` |
| 2.7 Inscription-table multi-removal | **Fixed** — exact count, clamped bounds | `ceb14c9` | `TileEntityInscriptionTableTest` |
| 3.2 Fortify-Time expiry NPE | **Fixed** — world/tile null guards | `d72a7d2` | `AMEventHandlerSafetyTest` |
| 3.3 Spatial-vortex null/CoFH guards | **Fixed** — null-world tile lookups and `AMCore.cofh` short-circuit | `d72a7d2` | `AMEventHandlerSafetyTest` |
| 3.5 Worldgen config crashes | **Fixed** — bounds-safe chance/ore/meteor rolls | `d72a7d2` | `WorldgenMathTest` |
| 3.6 Wrong enchantment removal | **Fixed** — matching fortune/looting removal | `d72a7d2` | `AMEnchantmentHelperTest` |
| 3.8 Unset spell-part manager | **Fixed** — SkillManager assigned, clear guard | `d72a7d2` | game-test API assertion |
| 3.9 IMC blacklist parsing | **Fixed** — literal pipe parser | `d72a7d2` | `AMCoreImcParsingTest` |
| 3.1 Chunk-loader tickets | **Fixed** — reuse/release/restore map lifecycle and real key equality | `c64bf57` | `AMChunkLoaderTest`, game-test ticket lifecycle |
| 3.4 TE network floods | **Fixed** — change-gated battery metadata and throttled enervator NBT | `c64bf57` | `TileEntityManaBatteryUpdateTest`, `TileEntityEntropicEnervatorTest` |
| 3.7 Lingering spells | **Fixed** — one server tick per matching world, clear on unload | `c64bf57` | `SpellHelperLingeringTest` |
| 4.1 Item NBT bounds/casts | **Fixed** — clamped spell-book slot, guarded cast, reagent triples, stable NBT key | `6641377` | `ItemSpellBookTest`, `SpellUtilsReagentTest` |
| 4.2 Flicker out-marker removal | **Fixed** — removes the vector from every priority list | `6641377` | `TileEntityFlickerHabitatTest` |
| 4.5 Multiblock replacement precedence | **Fixed** — block/meta condition parenthesised | `6641377` | `MultiblockStructureDefinitionTest` |
| 4.7 Collection contracts | **Fixed** — RegionCoordinates, CompoundKey and ManaStack equals/hashCode aligned | `6641377` | `CompoundKeyTest`, `ManaStackTest` |
| 4.3 Moonstone attractor scoping | **Fixed** — attractors keyed by dimension | `a24ebad` | `FlickerOperatorMoonstoneAttractorTest` |
| 4.4 Item-frame keys | **Fixed** — dimension in the key, cleared on unload/disconnect | `a24ebad` | `ItemFrameWatcherKeyTest` |
| 4.8 Optional dependencies | **Fixed** — CoFH short-circuit plus unused Baubles/Thermal deps removed | `a24ebad` | `RedstoneFluxDrainTest` |
| 4.11 Malformed player NBT | **Fixed** — contingency default, null-safe save, skill-tree/affinity bounds | `a24ebad` | `ExtendedPropertiesNbtTest`, `SkillDataTest` |
| 4.6 Static state/leaks | **Fixed** — world-scoped cleanup, Set shields, real extra-property removal | `4c806ab` | `AMEventHandlerStateTest`, `ExtendedPropertiesNbtTest` |
| 4.9 Potion ID validation | **Fixed** — free-slot resolution, null-safe clash check, single config save | `4c806ab` | `AMConfigPotionIdTest` |
| 4.10 Dimension/biome registration | **Fixed** — reserved/occupied IDs validated with fallbacks | `4c806ab` | `AMConfigIdTest` |
| 5.1 Skill-tree hot path | **Fixed** — identity index and cached unmodifiable tree views (`getTree` copy kept for API compatibility) | `f65e0ae`, `994404e` | `SkillTreeManagerLookupTest` |
| 5.2 Encoded effect parsing | **Fixed** — no-match fast path and bounded parsed-key caches for every timed-effect family | `be436de`, `771fac4`, `4a05007` | `AMEventHandlerAcceleratedBlockTest`, `AMEventHandlerAcceleratedTileKeyTest`, `AMEventHandlerParsedEffectKeyTest` |
| 5.5 `PowerTypes` allocation/lookup | **Fixed** — shared cached array plus concurrent ID map | `f65e0ae` | `PowerTypesTest` |
| 5.6 Retroactive worldgen | **Fixed** — FIFO deque, server-only, END phase only | `f65e0ae` | `RetroactiveWorldgeneratorTest` |
| 5.4 Altar multiblock rebuild | **Fixed** — shared static primary/fake definitions built once | `6e968e0` | game-test altar definition checks |
| 5.7 Arcane reconstructor sync | **Fixed** — marks the block only on active-state transitions | `6e968e0` | `TileEntityArcaneReconstructorUpdateTest` |
| 5.7 Worldgen tree reuse | **Fixed** — cached generators with per-tree height reset | `1c402cb` | `WitchwoodTreeHugeReuseTest` |
| 5.7 Power-node NBT loading | **Fixed** — in-place top-level read and O(1) nested-path removal | `ce8fd9e` | `PowerNodeEntryNbtTest` |
| 5.7 Client timed-effect bookkeeping | **Fixed** — `onEntityLiving` skips server-only timed effects on the client | `a75cbf4` | `AMEventHandlerPerfGuardTest` |
| 5.7 Timed-effect early-out | **Fixed** — timed-key index skips per-entity timed work when no effects exist | `159e053` | `ExtendedPropertiesTimedEffectsTest` |
| 6.7 Helper stack mutation | **Fixed** — fuel comparators and enervator outputs are copied | `0d5a4d7` | `ObeliskFuelHelperTest`, `EnervatorRecipeHelperTest` |
| 6.7 API property getters | **Fixed** — null-safe entities/identifiers and lifecycle docs | `d148bbc` | `ArsMagicaApiPropertiesTest` |
| 6.7 Legacy second mana API | **Fixed** — live legacy types deprecated, commented-out dead types removed | `9ff50da` | `LegacyManaApiTest` |
| 6.6 Duplicated sync countdown | **Fixed** — shared `SyncWindow` for all three synced stores | `c268948` | `SyncWindowTest`, `SyncWindowIntegrationTest` |
| 6.1 Bucket handler extraction | **Partially fixed** — bucket fill moved to `am2.handlers.BucketHandler` | `5d9dc43` | `BucketHandlerTest`, game-test bucket checks |
| 6.1 Potion-brewing extraction | **Partially fixed** — brewing replacement moved to `PotionBrewingHandler` | `a583c03` | `PotionBrewingHandlerTest` |
| 6.1 Passive-effect handlers | **Partially fixed** — temporal-anchor join cleanup and fury break-speed extracted | `e8b50a4` | `SmallEffectHandlersTest` |
| 6.1 Cost/overlay handlers | **Partially fixed** — spellbook mana discount and client overlays extracted | `ff0fcdf` | `ManaCostHandlerTest`, `BlockOverlayHandlerTest` |
| 6.1 Item-watch handlers | **Partially fixed** — item-frame interaction and item-toss watching extracted | `a1b1147` | `ItemWatchHandlerTest` |
| 6.1 Entity construction | **Partially fixed** — property/attribute registration moved to `EntityConstructionHandler` | `b0db6bd` | `EntityConstructionHandlerTest` |
| 6.1 Drop/pickup handlers | **Partially fixed** — summon drops and compendium pickup extracted | `a955317` | `SummonDropHandlerTest`, `CompendiumPickupHandlerTest` |
| 6.1 Soulspike attacks | **Partially fixed** — soulspike on-hit logic extracted | `898455b` | `SoulspikeAttackHandlerTest` |
| 6.1 Connection tickrate | **Partially fixed** — client connect/disconnect/login tickrate handling extracted | `e6266d5` | FML-runtime game tests only |
| 6.1 Force-shield block protection | **Partially fixed** — protected block-break handling extracted | `fd3513b` | FML-runtime game tests only |
| 6.1 Accelerated entity tick | **Partially fixed** — server-tick extra entity updates extracted | `1fce9b3` | `AcceleratedEntityTickHandlerTest` |
| 6.1 Soulbound death handling | **Partially fixed** — death/clone inventory preservation extracted | `10d9b05` | `SoulboundItemsHandlerTest` |
| 6.1 Player clone data | **Partially fixed** — extended/skill/rift/affinity clone transfer extracted | `77e43df` | `PlayerCloneDataHandlerTest` |
| 6.1 Jump movement buffs | **Partially fixed** — agility/leap/entangled/ender-boot jump handling extracted | `ee51011` | `JumpHandlerTest` |
| 6.1 Living attack defense | **Partially fixed** — fire armor retaliation and mana shield extracted | `c6bd8f0` | `CombatDefenseHandlerTest` |
| 6.1 Handler section extraction | **Partially fixed** — `onEntityLiving` fully decomposed into named sections; other event families remain | `c8e815f`, `3cb040e`, `6bb2836`, `01e293e`, `6817ebc`, `a9c056a`, `5e101ac`, `b1c6314`, `3f47f30`, `3689056`, `9b76b22`, `aaeaf3b`, `388e527`, `6227d51` | `AMEventHandlerPerfGuardTest` |
| 6.4 Packet payload validation | **Partially fixed** — 1 MiB cap before payload allocation on both processors | `092f9ea` | `PacketValidationTest` |
| 6.4 Strict packet reads | **Fixed for both processors** — failure guards across server and client packet handlers | `74fc17f`, `e10b97c`, `eadef7a` | `AMDataReaderTest`, `AMPacketProcessorServerStrictReadTest`, `AMPacketProcessorClientStrictReadTest` |
| 6.4 Packet direction tags | **Fixed** — per-ID direction registry enforced before dispatch | `493cb80` | `PacketValidationTest` |
| 6.4 Self-scoped packet authority | **Partially fixed** — sender-bound desync/beta requests and spellbook slot bounds | `0e32562` | `PacketAuthorityTest` |
| 6.4 Container authority rechecks | **Fixed** — workbench reach rechecked; customization bound to sender | `9dd5a72` | `PacketAuthorityTest` (reach/self helpers) |
| 6.4 Validated packet envelope | **Partially fixed** — direction/size/id/payload centralized in `PacketEnvelope` | `f8a478b` | `PacketEnvelopeTest` |
| 6.4 Typed handler registries | **Fixed** — packet switches replaced by typed per-side handler tables | `65de89d` | `PacketRegistryTest` |
| 6.3 Typed timed-effect keys | **Partially fixed** — typed block/entity/vortex keys and codec extracted from `AMEventHandler` | `9e22120` | `TimedEffectCodecTest` |
| 6.3 Accelerated-tile storage | **Partially fixed** — typed duration map is the hot-path source; string key retained for sync/NBT | `f550670` | `ExtendedPropertiesAcceleratedTileTest` |
| 6.3 Entity effect storage | **Partially fixed** — accelerated/slowed entities use typed duration maps with legacy string mirroring | `50e3ceb` | `ExtendedPropertiesEntityEffectTest` |
| 6.3 Slowed-tile storage | **Partially fixed** — slowed tiles use a typed duration map with legacy string mirroring | `2b51cac` | `ExtendedPropertiesSlowedTileTest` |
| 6.3 Fortified/shielded storage | **Partially fixed** — both block families use typed duration maps with legacy string mirroring | `c009e3a` | `ExtendedPropertiesFortifiedShieldTest` |
| 6.3 Spatial-vortex storage | **Partially fixed** — typed spatial map drives the vortex loop; legacy strings mirrored | `3f5b88e` | `ExtendedPropertiesSpatialVortexTest` |
| 6.3 Typed effect storage | **Fixed** — typed maps are the source of truth; NBT/sync derive and migrate legacy strings | `cfa992c` | `ExtendedProperties*EffectTest`, `TimedEffectCodecTest` |

The ASM/AT/reflection migration described in §12 is also fully implemented: no custom ASM transformers, no
`ReflectionHelper`/private-access reflection, and no access transformers remain.

## 2. Critical findings

### 2.1 The coremod is not registered (all ASM fixes are dead code)

> **Status: Fixed.** The GTNH Mixin loader/configs are now registered, the manifest is guarded by
> `verifyMixinManifest`, and the old `mysteriumlib` framework has been deleted.


**Verified.** The build sets `coreModClass` to an empty value:

```properties
# gradle.properties:121-122
coreModClass =
```

The produced artifact's manifest contains no `FMLCorePlugin` entry (only `FMLAT`):

```text
Manifest-Version: 1.0
FMLAT: arsmagica2_at.cfg
```

The dedicated-server launch log likewise only instantiates Forge's own coremods, NEI's and CodeChickenCore's; there is no `MysteriumPatchesFixLoaderMagicka` / `BytecodeTransformers` line. The loader class is not named by the manifest or by any lifecycle hook; the only references to it are from the ASM classes themselves, so it is never loaded.

**Impact:**

- All 34 active `@Fix` methods in `MysteriumPatchesFixesMagicka` are inert. That includes:
  - `selectEntitiesWithinAABB` / `getClosestPlayer` filtering of ethereal players.
  - `isBlockIndirectlyGettingPowered` handling for redstone inlays.
  - `processClickWindow` protection for ethereal players.
  - `canBePushed`, `canBeCollidedWith`, `isOnLadder`, `handleWaterMovement`, `applyEntityCollision`, `isEntityInsideOpaqueBlock` adjustments for ethereal/shrunk players.
  - `hasWorldObj` tile-entity slowdown used by `DiluteTime` (so `slowedTiles` is populated but never read).
  - The server/client tickrate patch: `MysteriumPatchesFixesMagicka.changeServerTickrate` only writes a static field that the vanilla `MinecraftServer.run` never reads without the transformer.
- All `BytecodeTransformers` transformations are inert:
  - `EntityRenderer` flip/shift camera callouts.
  - `RendererLivingEntity` shrink rendering callout.
  - `World.playSoundAtEntity` pitch modification.
  - `EntityPlayerSP`, `NetHandlerPlayServer`, `NetHandlerPlayClient`, and `PotionEffect` / `S1D` / `S1E` potion-ID widening.
  - The `MinecraftServer.run` `50L` → `servertickrate` replacement.

The runtime potion array extension in `BuffList` still lets default IDs (24–56) work, but potion IDs above 127 will still be truncated when `PotionEffect` writes its ID as a byte to NBT and to the network, because the widening transformer never runs.

**Recommendation:** Decide whether this fork intends to keep the coremod.

- If yes: set `coreModClass = net.tclproject.mysteriumlib.asm.fixes.MysteriumPatchesFixLoaderMagicka` in `gradle.properties`, rebuild the manifest, and add an integration test that asserts the marker/transformation actually ran. Treat the transformer as security-sensitive code and review its `ClassWriter.COMPUTE_MAXS` usage (no frame computation) before enabling it on Java 7/8 class files.
- If no: delete `MysteriumPatchesFixesMagicka`, `BytecodeTransformers`, `MysteriumPatchesFixLoaderMagicka`, `CustomLoadingPlugin`, `FirstClassTransformer`, `CustomClassTransformer`, `ASMFix`, the `methods.bin` blobs, and the dead `slowedTiles`/tickrate code. Re-test the features that depended on them.

Either way, this must be resolved before release; the current state is a silent half-implemented feature set.

### 2.2 Client-authoritative server packets (cheats and crashes)

> **Status: Fixed** in `8d6d3fc`; covered by `SkillDataTest`, `PacketAuthorityTest` and
> `TileEntityInscriptionTableAuthorityTest`. Generic per-player packet-rate limiting and the cosmetic
> `SYNCMAPTOSERVER` model-map relay remain follow-up hardening items.


**Verified.** `AMPacketProcessorServer` dispatches many client packets directly into server-side state with no direction/authority check.

#### Skill data

`handleSyncSpellKnowledge` calls `SkillData.For(player).handlePacketData(data)`. That method accepts both `SPELL_LEARNED` and `CLIENT_SYNC`. The `CLIENT_SYNC` branch (`SkillData.java:331-373`) directly clears/repopulates known shapes/components/modifiers/talents and **sets all spell points**:

```java
case CLIENT_SYNC:
    int flags = rdr.getInt();
    if (rdr.getBoolean()) this.primaryTree = SkillTrees.values()[rdr.getInt()];
    ...
    if ((flags & NUM_SPELL_POINTS) == NUM_SPELL_POINTS) {
        setSpellPoints(rdr.getInt(), rdr.getInt(), rdr.getInt());
    }
```

A client can therefore give itself every spell part and arbitrary blue/green/red/silver points. `learn(id,type)` is server-validated, but the `CLIENT_SYNC` path bypasses it. The same `handlePacketData` is used for the legitimate server→client sync, so the protocol has no direction flag.

#### Inscription table / free spells

`handleInscriptionTableUpdate` (`AMPacketProcessorServer.java:310-316`) accepts coordinates and `HandleUpdatePacket` bytes from any player. It does not check the player is using the table, is within reach, or owns it. `HandleUpdatePacket` then replaces the table's recipe, shape groups, name and read-only flag (`TileEntityInscriptionTable.java:369-425`).

`MAKE_SPELL` casts the client-supplied entity ID directly:

```java
// TileEntityInscriptionTable.java:410-415
case MAKE_SPELL:
    int entityID = rdr.getInt();
    EntityPlayer player = (EntityPlayer) worldObj.getEntityByID(entityID);
    if (player != null) {
        createSpellForPlayer(player);
    }
```

and `createSpellForPlayer` (`576-608`) builds a spell stack and adds it to the target's inventory with **no** `currentRecipeIsValid()` call, no reagent consumption, no validation, and no distance check. (The GUI only shows the create button to creative players, but the packet path has no such check.) The blind cast also throws `ClassCastException` if the ID belongs to a non-player.

`RESET_NAME` (`417-422`) casts `player.openContainer` to `ContainerInscriptionTable` without an `instanceof` check.

#### Tile entity NBT injection

`handleDecoBlockUpdate` (`AMPacketProcessorServer.java:319-327`) reads the coordinates from the client, gets any `TileEntityParticleEmitter`, and calls `te.readFromNBT(rdr.getNBTTagCompound())` with no distance or permission check. A client can overwrite the emitter's saved state anywhere in the world. `handleImbueArmor`, `handleCasterUpdate`, `handleMBW...` and several others follow the same pattern: coordinates come from the client, the TE is located, and the operation is performed without a reach check.

**Recommendation:** Introduce a packet direction/authority policy.

- Split client→server and server→client handlers. Never accept `CLIENT_SYNC` on the server.
- Validate every coordinate packet against `player.getDistanceSq(x,y,z) <= 64` (and the player's open container where relevant).
- For the inscription table, only accept updates when `player.openContainer instanceof ContainerInscriptionTable` belongs to that table, and call `currentRecipeIsValid()` before creating the spell.
- Consume the actual recipe/reagents on the server; never let the client decide the result.
- Guard entity IDs with `instanceof EntityPlayer` (or `EntityPlayerMP`) and verify the sender is the target.
- Add a small per-player packet rate limiter / length cap to prevent log floods from malformed packets.

### 2.3 Unbounded packet allocations (remote OOM/DoS)

> **Status: Fixed** in `ceb14c9`; covered by `AMDataReaderTest`.


**Verified.** `AMDataReader` is a thin wrapper around `DataInputStream`, and two methods trust the length field:

```java
// AMDataReader.java:128-141
public NBTTagCompound getNBTTagCompound() {
    int len = dataStream.readInt();
    byte[] bytes = new byte[len];
    dataStream.read(bytes);                  // return value ignored
    ...
}

// AMDataReader.java:163-174
public int[] getIntArray() {
    int[] arr = new int[dataStream.readInt()];
    ...
}
```

A crafted client packet with `len = Integer.MAX_VALUE` causes an `OutOfMemoryError` (or a negative-size exception) on the server. The `IOException` catch does not cover `NegativeArraySizeException` or OOM. `read(bytes)` is not checked with `readFully`, so short reads silently leave zeros in the buffer and `getRemainingBytes` can return `null`.

**Recommendation:** Enforce a maximum packet/NBT size (for example 1 MiB, which is already far above any legitimate NBT this mod sends), reject negative/oversized lengths before allocating, use `readFully`, and return a typed parse error instead of zero-filled data. Validate `getIntArray` lengths against the remaining payload.

### 2.4 Power-node persistence is broken in several ways

> **Status: Fixed** in `1353eff`; covered by `PowerNodeCacheTest` and `PowerNodeEntryNbtTest`.


**Verified.** This is a cluster of bugs that can cause stored power to disappear or move between dimensions/saves.

#### Data is not written on chunk unload

`PowerNodeCache.onChunkUnload` saves the chunk's registry data with `flushImmediate = false`:

```java
// PowerNodeCache.java:173-191
SaveNBTToFile(world, chunk, dataCompound, false);
```

But `SaveNBTToFile` only writes when `flushImmediate` is true; when it is false it just updates an in-memory compound and never puts that compound into `dataCache`:

```java
// PowerNodeCache.java:87-131
NBTTagCompound dataCompound = dataCache.get(rc);
if (dataCompound == null) { ...read the file... }
dataCompound.setTag(getPNDIdentifier(chunk), compound);
if (flushImmediate) { ...write... }
// no dataCache.put(rc, dataCompound)
```

Then `saveWorldToFile` removes every cache entry for the dimension without writing it:

```java
// PowerNodeCache.java:229-245
// cached data to file
Iterator<RegionCoordinates> it = dataCache.keySet().iterator();
while (it.hasNext()) {
    RegionCoordinates rc = it.next();
    if (rc.dimension == world.provider.dimensionId) {
        it.remove();
    }
}
```

The helper written to flush cached regions, `cacheToFile` (`213-227`), is dead code and is never called. Net effect: power data for chunks that unload before the next world save can be silently lost.

#### The file cache is keyed only by region name

`saveFilesCached` is a `HashMap<String, File>` keyed by `rX_rZ` (`PowerNodeCache.java:28,52-72`). It is checked before the per-dimension folder is used:

```java
String fileName = String.format("%d_%d%s", rX, rZ, extension);
File file = saveFilesCached.get(fileName);
if (file != null) return file;
```

The cache key does not include the dimension or the save/world. Consequences:

- Overworld and Nether region `0_0.amc` share one cached `File`; the first dimension wins for the rest of the session.
- In single-player, switching worlds leaves `saveFilesCached` populated with `File` objects pointing into the previous save's `AM2PowerData` folder. Power data can be read from or written into the old save.

`saveWorldToFile` removes `saveDirs` but never `saveFilesCached`.

#### Also related

- `RegionCoordinates.equals` performs an unchecked cast (`(RegionCoordinates) obj`) and uses bitwise `&` instead of `&&`; `hashCode` is `x + z + dimension`, which has heavy collisions (`(1,2)` vs `(2,1)`).
- `PowerNodeCache.getNBTForChunk` removes the cache entry whenever the region compound lacks the `"AM2PowerData"` key (`79-83`), causing a needless disk read on every empty region.
- `PowerNodeEntry.readFromNBT` destroys the NBT passed to it with `removeTag(0)` (`PowerNodeEntry.java:243-262`); callers that pass a live tag (for example `LoadChunkFromNBT`) mutate the saved compound. `parseFromNBT` and `setDataCompoundForNode` copy first; the load path should too.
- `PowerNodeCache.onWorldUnload` calls `saveWorldToFile`, which is also called from `AMCore.serverStopping`; ensure this is idempotent (it currently removes the cache on the first call, so the second call has nothing to save).

**Recommendation:** Rework the cache key around `(World save identity, dimension, region)` and make the lifecycle explicit: cache on read, flush on chunk unload or keep in a bounded dirty-region map, and write all dirty regions on `WorldEvent.Save` and `WorldEvent.Unload`. Remove `cacheToFile` or wire it in. Add a test that saves, unloads a chunk, saves the world, reloads, and verifies the power amount.

### 2.5 `PowerNodeRegistry` comparator violates the `Comparator` contract

> **Status: Fixed** in `ceb14c9`; covered by `PowerNodeRegistryComparatorTest`.


**Verified.** The registry is a `TreeMap<ChunkCoordIntPair, ...>` with a custom comparator:

```java
// PowerNodeRegistry.java:474-485
public int compare(ChunkCoordIntPair a, ChunkCoordIntPair b) {
    if (a.chunkXPos == b.chunkXPos && a.chunkZPos == b.chunkZPos) return 0;
    if (a.chunkXPos > b.chunkXPos) return 1;
    else if (a.chunkZPos > b.chunkZPos) return 1;
    return -1;
}
```

For `a=(1,5)` and `b=(2,0)`: `compare(a,b)` returns `1` (because `a.z > b.z`), and `compare(b,a)` returns `1` (because `b.x > a.x`). Both directions return `1`, so the ordering is not antisymmetric. A `TreeMap` built with this comparator can place keys in a subtree that later lookups never visit; `containsKey`/`get` can return `null` for a key that is definitely present.

This is a high-impact gameplay bug: power networks can stop working seemingly at random after chunks load in different orders.

**Fix:** Use a correct lexicographic comparator, or better, use a `HashMap<ChunkCoordIntPair, ...>` and let `ChunkCoordIntPair` provide `equals`/`hashCode`:

```java
int cmp = Integer.compare(a.chunkXPos, b.chunkXPos);
return cmp != 0 ? cmp : Integer.compare(a.chunkZPos, b.chunkZPos);
```

Add a unit test that inserts a set of chunk pairs in several orders and asserts every one is retrievable.

### 2.6 Compendium entries are saved at the wrong NBT indices

> **Status: Fixed** in `ceb14c9`; covered by `ExtendedPropertiesNbtTest`.


**Verified.** `ExtendedProperties.saveNBTData` uses one counter for both extra properties and compendium entries:

```java
// ExtendedProperties.java:1210-1225
int c = 0;
for (String o : extra_properties.keySet()) {
    compound.setString("persistentobj" + c, ...);
    compound.setString("persistentobjname" + c, ...);
    c++;
}
compound.setInteger("persistentobjsize", extra_properties.size());

for (String o : compendium_entries.keySet()) {
    compound.setString("compentry" + c, ...);
    compound.setString("compentryname" + c, ...);
    c++;
}
compound.setInteger("compendiumsize", compendium_entries.size());
```

But loading always starts at index 0:

```java
// ExtendedProperties.java:1291-1293
for (int j = 0; j < compound.getInteger("compendiumsize"); j++) {
    compendium_entries.put(compound.getString("compentryname" + j), compound.getString("compentry" + j));
}
```

If `extra_properties` is non-empty (it often is: `ethereal`, `karma`, time-fortified/accelerated/shielded/settlement keys), the compendium entries are written as `compentryN..` but read as `compentry0..N-1`, so unlock/lock/new state is lost or replaced by empty strings. This affects the player's Arcane Compendium progression, and it is copied through `PlayerEvent.Clone` too.

**Fix:** reset `c = 0` before the compendium loop (or use separate counters/keys). Add an NBT round-trip test with both maps non-empty.

### 2.7 Inscription-table multi-removal is off by one

> **Status: Fixed** in `ceb14c9`; covered by `TileEntityInscriptionTableTest`.


**Verified.** Both removal helpers loop with `<=` while removing:

```java
// TileEntityInscriptionTable.java:501-508
for (int i = 0; i <= length; ++i) group.remove(startIndex);

// TileEntityInscriptionTable.java:526-533
for (int i = 0; i <= length; ++i) this.getCurrentRecipe().remove(startIndex);
```

If `length` matches the number of elements to remove, this removes one extra element; if the requested range ends at the list end it throws `IndexOutOfBoundsException`. The GUI passes a length from a selection, so this is reachable by normal use.

**Fix:** use `i < length` or `subList(...).clear()` with bounds validation. Guard `startIndex` and `groupIndex` as well.

---

## 3. High-severity findings

### 3.1 Chunk-loader tickets leak and never release

> **Status: Fixed** in `c64bf57`; covered by `AMChunkLoaderTest` and the game-test ticket reuse/release check.


`AMChunkLoader` has several lifecycle bugs (`AMChunkLoader.java`):

- `requestStaticChunkLoad` always calls `ForgeChunkManager.requestTicket`, even if a ticket for that identifier already exists. `cacheTicket` only warns and returns, but the new ticket has already been created and is then used to force the chunk. The result is an untracked, permanently forced chunk.
- `releaseStaticChunkLoad` only unforces the chunk; it never calls `ForgeChunkManager.releaseTicket(ticket)` and never removes the entry from `tickets` (`80-90`).
- `ticketsLoaded` forces valid restored chunks but does not put the tickets into `tickets` (`92-123`). After a server restart, `releaseStaticChunkLoad` cannot find them, so the restored chunks stay loaded.
- `TicketIdentifier.compareTo` returns `-1` for every non-equal pair (`140-143`). It is only “working” because `getTicket` does a linear scan and compares the result to 0, but the class violates `Comparable` and is O(n) per lookup.
- `ticketsLoaded` uses the reversed assignability check:

```java
// AMChunkLoader.java:110-112
TileEntity te = world.getTileEntity(coords[0], coords[1], coords[2]);
if (te != null && te.getClass().isAssignableFrom(clazz)) {
```

This should be `clazz.isAssignableFrom(te.getClass())`; the current form rejects subclasses and accepts superclasses by accident.

**Fix:** key tickets by a value object with proper `equals`/`hashCode` in a `HashMap`, reuse an existing ticket on request, release and remove on unload/expiry, and repopulate the map in `ticketsLoaded` after validating the TE class. Consider a single ticket per world with multiple forced chunks instead of one ticket per block.

### 3.2 Time-fortified expiry can NPE and leave chunk loads behind

> **Status: Fixed** in `d72a7d2`; covered by `AMEventHandlerSafetyTest`.


`AMEventHandler.java:1250-1272` releases a static chunk load after a Fortify Time effect expires, but it dereferences the tile entity without a null check:

```java
AMChunkLoader.INSTANCE.releaseStaticChunkLoad(
    DimensionManager.getWorld(dim)
        .getTileEntity(x, y, z)
        .getClass(),
    x, y, z,
    DimensionManager.getWorld(dim));
```

If the block was broken, the chunk unloaded, or the TE replaced before the timer expires, `getTileEntity` returns `null` and the player's `LivingUpdateEvent` throws. Because the method is called every tick for every player, this can become a repeating crash/log flood. It also calls `DimensionManager.getWorld(dim)` three times and passes a class that is no longer used by the ticket identifier.

**Fix:** resolve the world once, return if null, resolve the TE, return if null, and pass a stable class/identifier (or make the release key independent of the TE class). Wrap the whole expiry loop in a defensive `try/catch` that removes the stale extra variable.

### 3.3 Spatial-vortex handling NPEs when a dimension is unloaded, and RF access is unguarded

> **Status: Fixed** in `d72a7d2`; covered by `AMEventHandlerSafetyTest`.


`AMEventHandler.java:957-1000` (and the repeated blocks at `1055-1230`) does:

```java
World thisdim = DimensionManager.getWorld(Integer.parseInt(entryvalues[4]));
...
TileEntity te = thisdim.getTileEntity(x + xadd, y, z + zadd);
```

`DimensionManager.getWorld` returns `null` for an unloaded dimension. A player can cast a spatial vortex in the Nether, return to the Overworld, let the Nether unload, and the caster's next `LivingUpdateEvent` will NPE every tick.

The same code directly uses `cofh.api.energy.IEnergyHandler` (`AMEventHandler.java:116,981-985,...`) without checking `AMCore.cofh` or `Loader.isModLoaded("CoFHCore")`. `enableSpatialVortex` defaults to `true`, so a server without CoFH that has a vortex and a TE in range will hit `NoClassDefFoundError` at the `instanceof IEnergyHandler` instruction. This is exactly the optional-dependency crash that `compileOnly` dependencies need to prevent.

**Fix:** skip/clean the extra variable when `thisdim == null`; guard the whole CoFH section with `AMCore.cofh` (or put it behind an optional interface implemented by a class that is only loaded when CoFH is present); catch `NoClassDefFoundError` around optional API calls if you must reference them directly.

### 3.4 Mana battery and enervator re-send block updates every tick

> **Status: Fixed** in `c64bf57`; covered by `TileEntityManaBatteryUpdateTest` and
> `TileEntityEntropicEnervatorTest`.


`TileEntityManaBattery.updateEntity` (`TileEntityManaBattery.java:49-62`) executes this every tick on the server:

```java
worldObj.setBlockMetadataWithNotify(xCoord, yCoord, zCoord, highest.ID(), 2);
worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
```

Both branches do the same, even when the metadata is unchanged. This sends block metadata + a block update packet for every battery every tick. A base with many batteries will saturate the network and cause client render churn.

`TileEntityEntropicEnervator.updateEntity` (`TileEntityEntropicEnervator.java:107-124`) calls `sendEnervatorUpdateToClient()` every tick while active. That method (`87-102`) serializes the entire tile entity to NBT and sends it to all clients within 100 blocks. This is 20 full NBT packets per second per active enervator.

**Fix:** track the last sent metadata/power type and only update on change. For the enervator, send a small progress packet every N ticks (or use `getDescriptionPacket`/`markBlockForUpdate` at a sane interval), and use `sendToAllAround` with a smaller radius.

### 3.5 Worldgen configuration can crash the game

> **Status: Fixed** in `d72a7d2`; covered by `WorldgenMathTest`.


`AM2WorldDecorator.generateOverworld` uses config values directly as RNG bounds:

```java
// AM2WorldDecorator.java:170
if (random.nextInt(witchChance) == 0) { ... }

// AM2WorldDecorator.java:185
if (biome != BiomeGenBase.ocean && typeValid && random.nextInt(wakeChance) < 7) { ... }

// AM2WorldDecorator.java:221
int y = random.nextInt(maxY - minY) + minY;
```

`getPoolFrequency` is guarded (`poolChance > 0`), but `witchChance`, `wakeChance`, and the ore height deltas are not. Users reasonably use 0 to disable a feature, and the pool option documentation explicitly suggests it.

- `witchwoodFrequency = 0` → `nextInt(0)` → `IllegalArgumentException` during chunk generation.
- `wakebloomFrequency = 0` → same.
- `vinteumMaxHeight == vinteumMinHeight` (or any ore pair equal) → `nextInt(0)`.

`MeteorSpawnHelper.tick` has the same pattern:

```java
// MeteorSpawnHelper.java:52-55
rand.nextInt(
    (AMCore.config.getMoonstoneFrequency() / 19)
        + (1000 * ws.provider.getMoonPhase(ws.provider.getWorldTime())))
    == 0
```

At moon phase 0, if `moonstoneFrequency < 19` (including the natural “0 to disable” setting), the bound is 0 and the server throws every tick `ticksSinceLastMeteor == 0`.

**Fix:** validate/clamp all RNG bounds in `AMConfig.init` and again at use sites. Use `Math.max(1, bound)` or guard with `> 0`; treat `0` as “feature disabled”. Add unit tests for 0, negative, and equal-min/max values.

### 3.6 `AMEnchantmentHelper` removes the wrong enchantment

> **Status: Fixed** in `d72a7d2`; covered by `AMEnchantmentHelperTest`.


```java
// AMEnchantmentHelper.java:41-50
public static ItemStack fortuneStack(ItemStack stack, int level) {
    ...
    if (level > 0) map.put(Enchantment.fortune.effectId, level);
    else map.remove(Enchantment.silkTouch.effectId);   // should be fortune
}

public static ItemStack lootingStack(ItemStack stack, int level) {
    ...
    if (level > 0) map.put(Enchantment.looting.effectId, level);
    else map.remove(Enchantment.silkTouch.effectId);   // should be looting
}
```

Consequences:

- Removing a Fortune spell modifier leaves Fortune on the item.
- Removing a Looting modifier leaves Looting.
- Both calls silently strip Silk Touch when called with `level <= 0`.
- `SpellUtils.changeEnchantmentsForShapeGroup` (`SpellUtils.java:597-610`) computes one `looting` value from `FORTUNE_LEVEL` and passes it to both `fortuneStack` and `lootingStack`; there is no `LOOTING_LEVEL` enum, so a Fortune modifier grants both. That may be intentional, but it should be explicit.

**Fix:** remove the matching enchantment in each helper and add unit tests for `set(level)`, `remove(0)`, and “does not touch the other enchantment”.

### 3.7 Lingering spells tick per world and on both sides

> **Status: Fixed** in `c64bf57`; covered by `SpellHelperLingeringTest`.


`SpellHelper.lingeringSpellList` is a single static list shared by the server and client (in an integrated server they are the same JVM). It is ticked in two places:

- `ServerTickHandler.onWorldTick` (`88-103`) runs for every world, including the client world and every loaded dimension.
- `ClientTickHandler.onClientTick` (`397-414`) ticks the same list from the client thread.

A lingering/zone spell therefore advances once per tick per loaded world, plus once more on the client in single-player. In a world with overworld + Nether + End loaded, a 600-tick spell finishes in roughly 200 ticks. In single-player, it can tick twice per frame. The list is also not filtered by the spell's own `world`, so a spell cast in the Nether is advanced by Overworld ticks too.

**Fix:** tick the list from one place only (the server tick), and inside that handler only call `doUpdate()` for spells whose `world` matches the current world (or, better, move the list into a per-world manager). The client should only render particles; it should not advance the authoritative timers.

### 3.8 `ArsMagicaApi.spellPartManager` is never initialized

> **Status: Fixed** in `d72a7d2`; the game test asserts the manager is non-null after init.


```java
// AMCore.java:350 (inside initAPI)
// ArsMagicaApi.instance.setSpellPartManager(new SpellManager());
```

`ArsMagicaApi.registerSkillTreeEntry` and `getSpellPartManager` dereference it without a null check:

```java
// ArsMagicaApi.java:86-89, 203-208
public final ISpellPartManager getSpellPartManager() { return spellPartManager; }
public final void registerSkillTreeEntry(...) {
    spellPartManager.registerSkillTreeEntry(entry, name);
    skillTreeManager.RegisterPart(...);
}
```

Any addon that uses the documented API to register a spell part will get a `NullPointerException` during pre-init/init. The manager is also not created anywhere else (`SkillManager.instance` exists and implements `ISpellPartManager`).

**Fix:** assign `SkillManager.instance` in `initAPI` (before the API is exposed to addons), add a null guard and a clear log message if it is missing, and add an API smoke test that calls `getSpellPartManager()`.

### 3.9 IMC blacklist parsing is broken

> **Status: Fixed** in `d72a7d2`; covered by `AMCoreImcParsingTest`.


`AMCore.onIMCReceived` parses the `dsb` and `bsb` messages with:

```java
// AMCore.java:294-295 and 311-312
String[] split = msg.getStringValue().split("|");
```

`String.split` takes a regular expression, and `|` is the alternation operator. `"EntityDryad|22".split("|")` returns every character (`["E","n","t",...]`), not the two fields. `split.length != 2` is therefore always true, and every `dsb`/`bsb` IMC is rejected as malformed. The dimension/biome spawn blacklist API is non-functional.

Use `split("\\|")` or `Pattern.quote("|")`. Add a unit test around the parser (extract it from the event handler so it can be tested without an IMC event).

---

## 4. Medium-severity findings

### 4.1 Item NBT bounds / casts

> **Status: Fixed** in `6641377`; covered by `ItemSpellBookTest` and `SpellUtilsReagentTest`.


- **`ItemSpellBook.GetActiveSlot`** (`ItemSpellBook.java:305-311`) returns the raw `spellbookactiveslot` NBT value. `GetActiveItemStack`/`GetActiveScroll` immediately index a 40-element array with it (`216-230`). A book with `spellbookactiveslot: -1` or `100` (NBT-edited, copied from an old version, or produced by another mod) throws `ArrayIndexOutOfBoundsException` on right-click/scroll. Clamp to `0..7` in `GetActiveSlot`, and validate it when loading the book.
- **`ItemSpellBook.GetActiveScroll`** casts the active item to `ItemSpellBase` (`221`). `ContainerSpellBook` normally prevents non-spells, but crafted NBT can place any item in the book inventory. Use `instanceof` and treat a non-spell as empty.
- **`SpellUtils.parseSpellRequirements`** (`SpellUtils.java:524-528`) iterates the reagent array with `i += 3` and reads `i+1`, `i+2` with no length check. A spell NBT array with length not divisible by 3 crashes when the tooltip/cast requirements are read. Validate `reagentList.length % 3 == 0`.
- **`RiftStorage`/`TileEntityCrystalMarker`/`TileEntityEntropicEnervator`** use `String.format("ArrayIndex", i)` as an NBT key (`RiftStorage.java:40,56`, `TileEntityCrystalMarker.java:172`, `TileEntityEntropicEnervator.java:148`). It works because the key is constant and the index is stored in the value, but it is confusing and easy to break. Use a plain constant (`"ArrayIndex"`) or store the index in a normal field.

### 4.2 Flicker out-marker removal is a no-op

> **Status: Fixed** in `6641377`; covered by `TileEntityFlickerHabitatTest`.


`TileEntityFlickerHabitat` stores outputs in `HashMap<Integer, ArrayList<AMVector3>> outList`, but removal does:

```java
// TileEntityFlickerHabitat.java:236-242
public void removeOutMarkerLocation(int x, int y, int z) {
    outList.remove(new AMVector3(x, y, z));
}
```

`HashMap.remove(Object)` removes an **Integer priority key**, not an `AMVector3` inside the list, so nothing is removed. Breaking an output crystal marker leaves a stale location in the habitat; item transport keeps trying to use it (and may log or scan a missing TE) until the habitat is reloaded.

**Fix:** iterate the priority lists and remove the vector from each, or store an `inList` and a flat `outList` keyed by position with priority as the value.

### 4.3 `FlickerOperatorMoonstoneAttractor.attractors` is global and dimension-blind

> **Status: Fixed** in `a24ebad`; covered by `FlickerOperatorMoonstoneAttractorTest`.


```java
// FlickerOperatorMoonstoneAttractor.java:20-26
private static final ArrayList<AMVector3> attractors = new ArrayList<AMVector3>();

public static AMVector3 getMeteorAttractor(AMVector3 target) {
    for (AMVector3 attractor : attractors) {
        if (attractor.distanceSqTo(target) <= 16384) return attractor.copy();
    }
    return null;
}
```

The coordinates have no dimension. An attractor in the Nether can redirect an Overworld meteor (and vice versa) if the X/Z happen to be within 128 blocks. The list is also only cleaned when the operator ticks without power or is removed; a chunk unload/world switch leaves stale entries. The linear scan is fine for a handful of attractors, but use a world-keyed map to make the semantics correct.

### 4.4 Item-frame watcher keys collide across dimensions

> **Status: Fixed** in `a24ebad`; covered by `ItemFrameWatcherKeyTest`.


`EntityItemFrameComparator.equals` and `hashCode` (`ItemFrameWatcher.java:263-279`) use only `frame.getEntityId()` and `worldObj.isRemote`. Entity IDs are per-world, so a frame in the Nether and a frame in the Overworld with the same ID compare equal on the server. One frame's watched-time counter can overwrite the other's. Include `worldObj` identity/dimension in the key (or use the frame's UUID).

The watcher is also not cleared on `WorldEvent.Unload`/disconnect. It removes invalid frames on its next tick, but a client that disconnects from a world leaves the `watchedFrames` map holding entities/worlds until the next world. Add a `clear()` call on unload and keep the existing `lock`.

### 4.5 `MultiblockStructureDefinition.replaceAllBlocksOfType` has a precedence bug

> **Status: Fixed** in `6641377`; covered by `MultiblockStructureDefinitionTest`.


```java
// MultiblockStructureDefinition.java:388-398
if (bd.block == originalBlock && bd.meta == originalMeta || originalMeta == WILDCARD_VALUE) {
    bd.block = newBlock;
    ...
}
```

`&&` binds tighter than `||`. When `originalMeta == WILDCARD_VALUE` (which the two-argument overload always passes), **every block in every group is replaced**, not just blocks of the requested type. This is an API method that compatibility addons may call; the result is a completely rewritten structure definition.

**Fix:** `if (bd.block == originalBlock && (originalMeta == WILDCARD_VALUE || bd.meta == originalMeta))`. Also use `WILDCARD_VALUE` checks on `newMeta` consistently.

### 4.6 Static state, leaks and thread-safety in event handlers

> **Status: Fixed** in `4c806ab`; covered by `AMEventHandlerStateTest` and `ExtendedPropertiesNbtTest`.


`AMEventHandler` has several mutable static collections:

```java
// AMEventHandler.java:635-638
public static List<String> forceShielded = new ArrayList<>();
public static Map<String, Integer> slowedTiles = new HashMap<>();
public static Map<UUID, Integer> acceleratedEntitiesUUIDs = new HashMap<>();
public static Map<UUID, Integer> slowedEntitiesUUIDs = new HashMap<>();

// AMEventHandler.java:1554-1556
public static Map<EntityPlayer, ArrayList<EntityCreature>> hallucinationMap = new HashMap<>();
public static Map<EntityPlayer, ArrayList<EntityItem>> dustMap = new HashMap<>();
public static Map<EntityCreature, Integer> tempCurseMap = new HashMap<>();
```

They are not world-scoped, not cleared on `WorldEvent.Unload`/server stop, and several are mutated in `LivingUpdateEvent`, which fires for both logical sides in an integrated server. The result is a mix of stale entries from previous worlds, strong references that prevent GC, and unsynchronised `HashMap` access from the client and server threads. The `ItemFrameWatcher` comments already describe this exact class of bug; the same treatment is needed here.

Also:

- `forceShielded` is a `List<String>` scanned on every block break. Use a `Set` or, better, a `Map<dimension, Set<BlockPos>>`.
- `hallucinationMap`/`dustMap` are cleared per entry in `cleanupLowerHallucination` but never removed from the map; a logout with the potion still active leaves the player and entities referenced.
- `tempFlipped` (`AMEventHandler.java:136`) is keyed by `ExtendedProperties`; the EEP object is recreated on clone, so old keys remain until the timer expires.
- `getExtraVariablesContains` returns a fresh `HashMap` every call (`ExtendedProperties.java:1387-1393`). `AMEventHandler` calls it 1–6 times per entity per tick, even for empty maps. The code then calls `.clear()` on some of these copies (`AMEventHandler.java:874-877`), believing it clears the real data; it does not. If the intent is “disable the effect when the skill is off”, remove the keys from `extra_properties` instead.

### 4.7 `RegionCoordinates`/`CompoundKey`/`ManaStack` collection contracts

> **Status: Fixed** in `6641377`; covered by `CompoundKeyTest` and `ManaStackTest`, with `RegionCoordinates`
> fixed alongside the power cache. `AMVector3` keys in the registry are freshly constructed per lookup.


- `RegionCoordinates.equals` (`RegionCoordinates.java:19-23`) has an unchecked cast and bitwise `&`; `hashCode` is `x + z + dimension`. Add `instanceof`, use `&&`, and use `Objects.hash`/a proper mix. (This map is not hot enough for the collisions to be critical, but it is a correctness smell.)
- `CompoundKey.equals` (`CompoundKey.java:20-30`) treats meta `-1` as a wildcard, but `hashCode` uses the actual meta. Two “equal” keys can have different hashes; any `HashMap` use will fail to find a wildcard key. It is only used in an `ArrayList` today, but the contract is broken.
- `ManaStack.equals` (`ManaStack.java:175-190`) delegates to `isManaEqual` (which ignores `amount`), while `hashCode` includes `amount`. Same contract problem. Also, `ManaStack`'s constructor calls `ManaRegistry.makeDelegate(mana)`; `ManaRegistry.registerMana(String, Mana)` never creates a delegate, so a stack for a string-registered mana throws `NullPointerException` in `getMana()`.
- `AMVector3` is mutable and used as a `HashMap` key in the power registry. Any in-place `add`/`sub` after insertion breaks lookup. Either use immutable vectors for keys or copy before storing. Its `hashCode` is `(int)(x + y + z)`, which collapses many coordinates into the same bucket.

### 4.8 Optional-dependency handling is inconsistent

> **Status: Fixed** in `a24ebad`; covered by `RedstoneFluxDrainTest`. Unused Baubles/Thermal dependencies
> were removed.


- **CoFH:** `AMEventHandler` imports and uses `cofh.api.energy.IEnergyHandler` directly, and `RedstoneFluxDrain` does the same. Neither checks `AMCore.cofh` / `Loader.isModLoaded("CoFHCore")`. The `@Mod` dependency is only `after:CoFHCore` (not required), so this is a real crash path on servers without CoFH (see 3.3).
- **Thaumcraft:** `TileEntityLectern.getValidItems` is guarded with `Loader.isModLoaded("Thaumcraft")` but still references `thaumcraft.api.ItemApi` in the bytecode; `ItemMageHood` uses `@Optional.Interface(striprefs = true)`. This pattern works in practice but deserves a smoke test with Thaumcraft absent.
- **Baubles:** `dependencies.gradle` declares `Baubles-Expanded`, but there are no `baubles` references in the source. Remove the dependency or implement the integration.
- **Thermal Foundation / Thermal Expansion:** declared `compileOnly`, no references. Remove.
- **NEI:** `NEIAM2Config` implements `IConfigureNEI` but is not referenced anywhere. If NEI's classpath discovery does not pick it up, the recipe/usage handlers are dead. Add a game test with NEI present or wire it explicitly.

### 4.9 Configurable potion IDs are not validated

> **Status: Fixed** in `4c806ab`; covered by `AMConfigPotionIdTest`.


`AMConfig.getConfigurablePotionID` (`1410-1436`):

- `getNextFreePotionID` returns `-1` when the array is full. If the config key exists, the `-1` fallback branch is skipped and `prop.getInt(-1)` can return `-1`; `createAMPotion` then indexes `Potion.potionTypes[-1]`.
- A configured ID outside `0..254` (or negative) is passed straight to the `ArsMagicaPotion` constructor and the `Potion.potionTypes[index]` check.
- If the chosen index is already occupied, the code logs an error but still creates a potion there, potentially overwriting another mod's potion. The `postInit` clash check then calls `.getName()` on a possibly null array entry (`BuffList.java:164-172`).
- The method calls `save()` once per potion, causing ~33 config writes at startup.

**Fix:** validate range, skip occupied slots instead of overwriting, choose a config default that is actually free, handle the null case in the clash check, and batch the config save.

### 4.10 Dimension/biome registration is unvalidated

> **Status: Fixed** in `4c806ab`; covered by `AMConfigIdTest`.


```java
// AMCore.java:162-163
DimensionManager.registerProviderType(config.getMMFDimensionID(), SCLWorldProvider.class, false);
DimensionManager.registerDimension(config.getMMFDimensionID(), config.getMMFDimensionID());
```

`getMMFDimensionID()` defaults to `-31`, but there is no check that it is not `0`, `-1`, `1`, already registered, or already used as a provider type by another mod. A config collision can replace/break the overworld/Nether/End or throw during init. `getWitchwoodForestID()`/`getMMFBiomeID()` are likewise passed straight to `BiomeGenBase`.

**Fix:** validate the IDs at config load, log a clear error and fall back to the default, and check `DimensionManager.isDimensionRegistered`/provider-type registration before registering.

### 4.11 Possible NPEs on malformed player NBT

> **Status: Fixed** in `a24ebad`; covered by `ExtendedPropertiesNbtTest` and `SkillDataTest`.


- `ExtendedProperties.loadNBTData` (`1278-1285`) catches `NullPointerException` while loading contingency stacks but can still assign `null`; `getContingencyEffect(n).getItem()` is then called on every tick in `AMEventHandler`. A save with a missing/invalid contingency tag can crash the player's tick loop. Guard the getters or restore the snowball default.
- `ExtendedProperties.saveNBTData` (`1198-1201`) writes `contingencyStacks[i].writeToNBT(...)` with no null check; the same corrupt state crashes on logout/save.
- `SkillData.loadNBTData` (`531-534`) indexes `SkillTrees.values()[ordinal]` without checking the bounds.
- `AffinityData.saveNBTData` (`170-177`) indexes `Affinity.values()[affinity]` without checking the ordinal range.
- `SkillData` uses `syncTickDelay = 2400` (`SkillData.java:46-52`), while `ExtendedProperties` uses 50 (2.5 s) and `AffinityData` uses 100 (5 s). `SkillData.setSpellPoints`/`incrementSpellPoints` only set the update flag; the level-up path in `ExtendedProperties.addMagicXP` increments skill points without calling `SkillData.forceSync()`. Unless a command or `learn()` forces a sync, the client's skill-point counter can lag up to two minutes after a level-up. The commands happen to call `forceSync()` (`GiveSkillPoints.java:70-78`), so this mainly affects natural level-ups. Call `forceSync()` from `SkillData` setters or lower the interval.

---

## 5. Performance findings

These are measurable algorithmic/allocation costs, not micro-optimisations.

### 5.1 Spell hot path copies skill trees on every modifier calculation

`SkillTreeManager.getSkillTreeEntry` (`SkillTreeManager.java:105-136`) calls `getTree` for each of the four trees, and `getTree` allocates a new `ArrayList` and copies the whole tree (`85-103`). It then linearly scans each copy with identity comparison.

`SpellUtils` calls this from every `getModifiedDouble_*`/`getModifiedInt_*` overload for the `AugmentedCasting` check (`SpellUtils.java:152-158`, `184-190`, and six more overloads), and `SpellUtils.getComponentsForStage`/`getModifiersForStage` call `isSkillDisabled`, which calls `getSkillTreeEntry` again for every part of every stage (`762-800`, `SkillTreeManager.java:1287-1291`).

A single spell cast can allocate hundreds of `ArrayList`s and scan thousands of entries. This is almost certainly one of the largest CPU costs in combat.

**Fix:** build a `HashMap<ISkillTreeEntry, SkillTreeEntry>` once in `SkillTreeManager.init`, use it for `getSkillTreeEntry`/`isSkillDisabled`, and cache the `AugmentedCasting` entry in `SpellUtils` (or in a per-player cached skill snapshot). Return an unmodifiable view from `getTree` instead of a copy if callers only read.

> **Status: Fixed.** `SkillTreeManager` now maintains an identity-keyed index as parts are registered and exposes cached
> unmodifiable tree views through `getTreeView`, so `getSkillTreeEntry`/`isSkillDisabled` are O(1) with no per-call list
> copies. The public `getTree` keeps its original defensive-copy contract for addon compatibility. Covered by
> `SkillTreeManagerLookupTest` (`f65e0ae`, `994404e`).

### 5.2 `getExtraVariablesContains` allocates a map per call and parses strings every tick

`ExtendedProperties.getExtraVariablesContains` (`1387-1393`) creates a new `HashMap` and scans/allocates keys. `AMEventHandler.onEntityLiving` calls it up to six times per entity per tick, then `split("_")` and `Integer.parseInt` each key and value (`825-1310`). For a server with many entities/players this is a large, constant allocation and parsing load.

**Fix:** store effects in typed data structures (for example `Map<BlockPos, Effect>` keyed by dimension) instead of encoded strings; at minimum return `Collections.emptyMap()` when there are no matches and use a cached parsed representation. Move the “accelerated blocks/entities” bookkeeping out of `LivingUpdateEvent` into a server-side manager.

> **Status: Fixed.** `getExtraVariablesContains` returns the shared empty map when there is no match, handles null
> prefixes, and uses one `entrySet` walk. All timed-effect key families now use bounded parsed-key caches, and each
> effect loop parses its duration once. The only remaining allocation is the snapshot map for effects that actually
> match; that copy is required because the loops mutate the underlying extra properties. Covered by
> `ExtendedPropertiesExtraVariablesTest` (`6e968e0`), `AMEventHandlerAcceleratedBlockTest` (`be436de`),
> `AMEventHandlerAcceleratedTileKeyTest` (`771fac4`) and `AMEventHandlerParsedEffectKeyTest` (`4a05007`).

### 5.3 Mana battery / enervator network spam

See 3.4. Every tick, per block. This is likely the single largest source of packet traffic in a developed base.

### 5.4 `TileEntityCraftingAltar` rebuilds its multiblock and does ore-dictionary lookups per TE

`TileEntityCraftingAltar` (`120-139`) calls `setupMultiblock()` in its constructor. `setupMultiblock` (`141-230`) creates a new `MultiblockStructureDefinition`, iterates `OreDictionary.getOres` for plank/sandstone/cobble/stairs/magical wood/quartz, and adds well over a hundred allowed blocks. This runs for every altar tile entity on load, on both sides. The `getFakeMultiblock()` method (`1297-1348`) rebuilds another ~60-block definition on every `getDefinition()` call.

**Fix:** build the static definition once (pre-init/init), cache the `BlockDec` lists, and share immutable copies. `getFakeMultiblock` should be a cached field.

> **Status: Fixed.** The primary and fake altar definitions are built once in `SharedStructure` and shared by every
> altar tile entity; `getDefinition()` returns the cached fake definition. Covered by the altar checks in the server
> game test (`6e968e0`).

### 5.5 `PowerTypes.all()` allocates a new array on every call

`PowerTypes.all()` returns `allPowerTypes.toArray(new PowerTypes[...])`, and `getByID` (`src/main/java/am2/api/power/PowerTypes.java:82-91`) calls `all()` for every lookup. `PowerTypes.all()` is used in hot paths (power ticks, NBT, request loops). Return a cached array (or an unmodifiable list) and use a `HashMap<Integer, PowerTypes>` for `getByID`. This also fixes the O(n) lookup that grows when addons register new types.

> **Status: Fixed.** `PowerTypes` builds its array and ID map once, rebuilds both on registration, and `getByID` is a
> constant-time map lookup. Covered by `PowerTypesTest` (`f65e0ae`).

### 5.6 Retroactive worldgen is O(n) and runs twice

`RetroactiveWorldgenerator.continueRetrogen` (`18-42`) processes up to 5 chunks per call and uses `chunks.remove(0)` on an `ArrayList`, which shifts the whole remaining list every time. It is called from `ServerTickHandler.onWorldTick` for every world and from `ClientTickHandler.onWorldTick` when the integrated server is running (`ServerTickHandler.java:76`, `ClientTickHandler.java:430-434`). In a single-player world it can process the queue twice per tick; in a dedicated server it runs on both `WorldTickEvent` phases because there is no phase check. Use an `ArrayDeque`/index cursor, process only on `Phase.END`, only on the server, and throttle with a time budget rather than a fixed count.

> **Status: Fixed.** The queue is an `ArrayDeque` drained FIFO with O(1) removal, `continueRetrogen` rejects remote
> worlds, `ServerTickHandler` calls it only at `Phase.END`, and `ClientTickHandler` no longer drives retro-gen. Covered
> by `RetroactiveWorldgeneratorTest` (`f65e0ae`).

### 5.7 Other allocation hot spots

- `AMEventHandler.onEntityLiving` runs for every living entity every tick, even on the client, and calls `ExtendedProperties.For(ent)`, `manaBurnoutTick`, `getMaxMana`, `SkillData.For`, `AffinityData.For`, `ArmorHelper` scans, etc. Consider splitting the player-only and mob-only paths and early-returning for entities with no AM2 data.
- `PowerNodeEntry.saveToNBT`/`readFromNBT` rebuild lists/maps on every chunk save; cache or reuse buffers where possible.
- `AM2WorldDecorator` creates a new `WitchwoodTreeHuge`/`WitchwoodTreeEvenMoreHuge` per generated tree (`AM2WorldDecorator.java:168-175`), and each constructor allocates arrays and a `Random`. Reuse a single generator instance (they reseed from the chunk random).
- `TileEntityManaBattery.updateEntity` also calls `worldObj.notifyBlockChange` every 600 ticks; that's fine, but combined with the per-tick update spam it is noise.
- `TileEntityArcaneReconstructor.queueRepairableItem` calls `markBlockForUpdate` every time it finds no valid item, even when the state did not change (`TileEntityArcaneReconstructor.java:260-282`).

> **Status: Fixed for the allocation hot spots.** The arcane reconstructor only marks the block on active-state changes,
> worldgen reuses its tree generators, power-node path NBT loading is linear/non-destructive, `onEntityLiving` skips
> server-only timed work on the client, and entities without timed effects skip the per-entity timed bookkeeping
> entirely. The remaining structural split of `onEntityLiving` is tracked under §6.1 (architecture), not as a per-tick
> allocation bug. Covered by `TileEntityArcaneReconstructorUpdateTest` (`6e968e0`), `WitchwoodTreeHugeReuseTest`
> (`1c402cb`), `PowerNodeEntryNbtTest` (`ce8fd9e`), `AMEventHandlerPerfGuardTest` (`a75cbf4`) and
> `ExtendedPropertiesTimedEffectsTest` (`159e053`).

---

## 6. Architecture and refactor recommendations

### 6.1 Split `AMEventHandler`

`AMEventHandler.java` is 2,161 lines and handles potions, souls, death/clone, combat, buffs, adjacency/spatial mechanics, block protection, world interactions, and dozens of unrelated events. Split it into focused classes (`PlayerLifecycleHandler`, `CombatEventHandler`, `TimeEffectsHandler`, `SpatialVortexHandler`, `ShieldHandler`, `BucketHandler`, etc.). This will make the static-state leaks and side guards reviewable in isolation.

> **Status: Partially fixed.** `onEntityLiving` is fully decomposed into named sections, and bucket filling, potion
> brewing, temporal-anchor cleanup, fury break-speed, mana cost, client overlays, item watching, entity construction,
> summon drops, compendium pickup, soulspike attacks, connection tickrate, force-shield block protection, accelerated
> entity ticking, soulbound death handling, player-clone copying, jump movement buffs and living-attack defense now live
> in focused handler classes. `AMEventHandler` still contains 7 event handlers and 1,668 lines, so the class split is not
> complete.

### 6.2 Introduce a world-scoped manager for AM2 state

The power registry, `CustomWorldData`, `ItemFrameWatcher`, `RetroactiveWorldgenerator`, `FlickerOperatorMoonstoneAttractor`, `AMEventHandler` statics, and `PowerNodeCache` all use static maps keyed by dimension (or not keyed at all). A single `WorldState`/`WorldStateManager` registered on `WorldEvent.Load`/`Unload` and cleared on server stop would eliminate the cross-world leaks and the `DIM0`/`DIM-1` collisions. It also gives you a natural place to clear `hallucinationMap`, `tempFlicker`, `forceShielded`, `slowedTiles`, `attractors`, and the frame watcher.

### 6.3 Replace stringly-typed extra properties with typed data

`extra_properties` is a `Map<String,String>` with keys such as `"accelerated_fast_tile_x_y_z_dim"` and values parsed as ints every tick. This is both a performance and a robustness problem (malformed strings throw `NumberFormatException`). Define small data classes (`TimedBlockEffect`, `TimedEntityEffect`, `SpatialVortex`) and store them in maps keyed by dimension/position/UUID. Serialization becomes straightforward and the hot path stops parsing.

> **Status: Fixed.** Every timed-effect family is stored in typed maps, and those maps are now the source of truth.
> NBT and packet serialization derive the legacy encoded strings from the maps; loading migrates old encoded strings into
> typed storage. The codec is retained only for compatibility. Covered by `TimedEffectCodecTest` (`9e22120`), the
> `ExtendedProperties*EffectTest` suite, and the serialization check added in `cfa992c`.

### 6.4 Add a proper packet layer

The network code has a single `AMDataReader`/`AMDataWriter` pair, no length validation, no direction checks, and manual ID dispatch. A small packet framework with:

- a `Side`/direction tag,
- a maximum payload length,
- typed `read`/`write` helpers that return errors instead of zero,
- a per-handler “validate coordinates/ownership/open container” contract,
- and a debug rate limiter

would remove a whole class of bugs. At minimum, add a `PacketValidation` utility and use it in the handlers listed in 2.2.

> **Status: Fixed.** `PacketEnvelope` centralizes ID/direction/size validation and payload extraction; both processors
> dispatch through typed `PacketRegistry` tables. Client and server handlers check `AMDataReader.hasFailed()` before
> mutating state, self-scoped requests are sender-bound, and container-based workbench/customization requests revalidate
> authority. Covered by `PacketValidationTest` (`092f9ea`, `493cb80`), `AMDataReaderTest` (`74fc17f`),
> `AMPacketProcessorServerStrictReadTest` (`e10b97c`), `AMPacketProcessorClientStrictReadTest` (`eadef7a`),
> `PacketAuthorityTest` (`0e32562`, `9dd5a72`), `PacketEnvelopeTest` (`f8a478b`) and `PacketRegistryTest` (`65de89d`).

### 6.5 Decide the ASM strategy

The `mysteriumlib` ASM framework is 2,000+ lines of custom bytecode manipulation, plus a 1,700-line `BytecodeTransformers`, plus a 1,000-line `ASMFix` builder. It currently does nothing in the shipped artifact (2.1). Maintaining a private ASM framework for 1.7.10 is risky: it is sensitive to obfuscation mappings, other mods' transformers, and Java 7/8 stack-map frames (it uses `COMPUTE_MAXS`, not `COMPUTE_FRAMES`). If the features are still wanted, prefer a small, focused Mixin set (the build already has the GTNH Mixin tooling available) or move the affected behavior into normal Forge events. If not, delete it; it is the largest unmaintained surface in the repo.

### 6.6 Reduce duplicated sync code

`ExtendedProperties`, `AffinityData`, and `SkillData` each implement their own `ticksToSync`/`forcingSync`/`hasUpdate`/`getUpdateData`/`handlePacketData` pattern with slightly different semantics. Extract a base `SyncedExtendedProperties` helper. This will also make the `syncTickDelay` behavior consistent and testable.

> **Status: Fixed.** The duplicated countdown, delayed-sync and forced-sync state now lives in `SyncWindow`; each store
> keeps its own payload and “has data” rule. Covered by `SyncWindowTest` and `SyncWindowIntegrationTest` (`c268948`).

### 6.7 Clean up the API surface

- `ArsMagicaApi.spellPartManager` (2.0.8 / 3.8).
- `ManaStack`/`ManaRegistry`/`ManaContainer`/`ManaContainerRegistry`/`TileManaHandler`/`TileEntityManaBatteryNew`/`TileEntityAMPowerNew` appear to be an abandoned second power API. `ManaContainerRegistry` is almost entirely commented out. Either finish and document it or remove it to avoid confusing addon authors.
- `ObeliskFuelHelper.registerFuelType` mutates the caller's `ItemStack` (`stack.stackSize = 0`, `ObeliskFuelHelper.java:18-21`). Copy first.
- `EnervatorRecipeHelper.getRecipe` returns the stored output stack itself and mutates `stackSize` (`EnervatorRecipeHelper.java:29-53`). Return a copy.
- `ArsMagicaApi.getSpellPartManager` and the identifier getters should have clear null/initialisation documentation.

> **Status: Fixed.** `ObeliskFuelHelper.registerFuelType` and `EnervatorRecipeHelper.getRecipe` copy their
> inputs/outputs, the extended-property getters are null-safe and documented, the legacy second mana API is deprecated,
> and the fully commented-out dead types have been removed. Covered by `ObeliskFuelHelperTest`,
> `EnervatorRecipeHelperTest` (`0d5a4d7`), `ArsMagicaApiPropertiesTest` (`d148bbc`) and `LegacyManaApiTest` (`9ff50da`).

---

## 7. Test gaps

### Current state

- 19 JUnit tests cover `AMVector2`, `AMVector3`, `MathUtilities`, and `ManaContainerInfo`.
- The server game test has 109 assertions: mostly registration smoke tests (blocks, items, TEs, ore dictionary, one recipe, API singleton non-null), plus a couple of world placements.
- The client game test has 23 assertions at the main menu (proxy, key bindings, creative tabs, localisation).
- Coverage is 177/69,939 lines (0.25%), with a 0.2% floor. The game-test coverage report is generated but not enforced.

### The most valuable missing tests

1. **Coremod registration test** — assert that `MysteriumPatchesFixLoaderMagicka` was loaded and that at least one transformation marker is present. This would have caught 2.1 immediately. If the coremod is intentionally removed, assert its absence and remove the code.
2. **Network/reader tests** — `AMDataReader`/`AMDataWriter` round-trips for every type, malformed length/negative length/short read, oversized NBT, non-multiple-of-3 reagent arrays.
3. **Packet authority tests** — unit-test `SkillData.handlePacketData` with a `CLIENT_SYNC` payload and assert the server rejects/ignores it; test `TileEntityInscriptionTable.HandleUpdatePacket` with a non-player entity ID and with an invalid recipe; test `AMPacketProcessorServer.handleDecoBlockUpdate` with a far-away TE.
4. **NBT round-trip tests** — `ExtendedProperties` with non-empty `extra_properties` + compendium entries + all five contingencies; `SkillData` with corrupt `SpellPoints`/`PrimarySkillTree`; `AffinityData` with an out-of-range ordinal; `RiftStorage` with duplicate/invalid slot indices.
5. **Power system tests** — comparator ordering with multiple insertion orders; `PowerNodeEntry` path save/load; `PowerNodeCache` save → chunk unload → world save → reload; two dimensions with the same region coordinates; two different worlds with the same dimension ID.
6. **Spell tests** — `constructSpellStack` with duplicate modifiers, multiple shape groups, a Summon in a later stage; `parseSpellRequirements` with malformed arrays; `changeEnchantmentsForShapeGroup` with fortune/silk/looting combinations; `SpellValidator` stage rules; `createSpellStack` with an empty recipe.
7. **Item tests** — `ItemSpellBook` active slot with `-1`, `8`, `39`, `100`; a non-spell item in the active slot; `ItemSpellBook.SetNextSlot` return value; `ItemManaStone`/`ItemSoulspike` negative NBT values.
8. **Config/worldgen tests** — `AMConfig` with zero/negative `witchwoodFrequency`, `wakebloomFrequency`, equal ore min/max, invalid dimension/biome/potion IDs; `AM2WorldDecorator` on a `WorldType.FLAT` and on the MMF dimension.
9. **Game tests** — place and break a fully formed crafting altar and assert the multiblock/TE state; cast a spell with a spell book and assert mana/burnout sync; place a mana battery and assert metadata changes only on change; unload/reload a chunk with a power network; run a dedicated-server test without CoFH to prove the optional-dependency guards.
10. **Thread-safety/leak tests** — a single-player integration test that creates a world, ticks 200 ticks, unloads the world, and asserts the static maps are empty (or at least bounded). This would catch the `ItemFrameWatcher`/`AMEventHandler` leaks.

### Coverage gate

A 0.2% floor cannot protect anything. Either set a meaningful per-package floor for the pure utility/API packages, or switch to a changed-line coverage gate in CI (`diff-cover --fail-under=...`) once the baseline is stable. The README already mentions `diff-cover`; make it blocking for new code.

---

## 8. Build / CI / dependency notes

- **`coreModClass` empty** — see 2.1. This is the single most important build-level issue.
- **Optional dependency declarations** — Baubles, Thermal Foundation, and Thermal Expansion are declared but unused. CoFH is used without runtime guards. Remove unused deps and add `@Optional`/reflection guards for the ones that remain.
- **`mcmod.info`** — the author list is `TCLProject`, `Mithion`, `Zerodaimaru`; the README says this fork is maintained by MrFuzzihead. Update the metadata/credits to match the actual maintainer and the MIT source license. The asset-license note in the README is good; keep it in the repo.
- **`NEIAM2Config`** — verify that NEI's `IConfigureNEI` discovery actually finds it. If not, register it explicitly or add a game test with NEI present.
- **`run` logs** — the dev client log contains a `FileNotFoundException` for a versioned jar name with a space (`feature-Refactors.3 a40447bc42`). The actual artifacts use `+` (`feature-Refactors.3+a40447bc42`). This is a stale run configuration, not a source bug, but it makes local testing noisy; clean the `run`/IDE configuration.
- **Antlr4 jar read error** — the dev launch complains that `antlr4-4.13.2.jar` is corrupt/unreadable. It is unrelated to the mod source, but it should be cleaned from the dev classpath so real errors are visible.
- **README test counts** — the README says 19 unit tests / 109 server assertions / 23 client assertions, which matches the current run. Keep those numbers in sync if tests are added.
- **CI** — the game-test workflow greps all logs for the markers, which is good. Add a check that the coremod marker is present (or explicitly absent) and that the test run fails on any `ERROR` from the mod's own logger.

---

## 9. Dead code / cleanup inventory

These classes/features are unreferenced or incomplete. Some are intentionally retained for API compatibility; mark them as deprecated or remove them.

**Unreferenced classes (class name appears only in its own file):**

- `am2/blocks/renderers/EverstoneRenderHandler`
- `am2/bosses/ai/WaterGuardianTargetSelector`
- `am2/commands/SetCompendiumState` (also has a `var2[1]` out-of-bounds bug; see below)
- `am2/entities/ai/TargetTypes`
- `am2/illeffect/IllEffectDrainMana`, `IllEffectExplode`, `IllEffectSpark`, `IllEffectSparkStorm`, `IllEffectSpawnDarkling`
- `am2/interop/NEIAM2Config` (unless NEI auto-discovers it)
- `am2/spell/components/Permanence` (both effect methods are TODO; not registered in `SkillManager`)
- `am2/utility/BlockUtilities`
- `net/tclproject/mysteriumlib/math/LocalityHelper`

**Incomplete / abandoned features:**

- `TileEntityAMPowerNew` is entirely commented out; `TileEntityManaBatteryNew` extends `TileManaHandler` and is unused.
- `ManaContainerRegistry` is almost entirely commented-out code; `IManaContainerItem` is commented out.
- The `illeffect` subsystem has no registrations: `IllEffectsManager.instance` is never populated and the only caller in `TileEntityBlackAurem` is commented out (`TileEntityBlackAurem.java:255-260`). The `entriesSortedByValues` method creates a `sortedEntries` set and then ignores it (`IllEffectsManager.java:106-120`), so the “sorted” map is not sorted.
- `SetCompendiumState.processCommand` checks `var2.length != 1` and then reads `var2[1]` (`SetCompendiumState.java:32-40`); even if registered, it would throw `ArrayIndexOutOfBoundsException`. It is not registered in `AMCore.serverStarting`.
- `SpellValidator.modifierCanBeAdded` always returns `false` (`SpellValidator.java:139-141`); the real check lives in `ContainerInscriptionTable`. Remove the stub or delegate.
- `ExtendedProperties.isEntryLocked`/`isEntryNew` (`1411-1419`) are unused and dereference a possibly absent map entry.
- `ExtendedProperties.detectPossibleDesync` always returns `false` (`1166-1168`); the client→server desync packet path is dead.
- `AMEventHandler.tempFlipped`/`getMinIndex` etc. are used, but the `IPowerNodeComparer` inner class in `PowerNodeRegistry` (`487-509`) is unused.

**Commented-out code and stale TODOs (3,064 comment lines, 21 TODO/FIXME):**

- `AMEventHandler` contains a large commented-out `onEntityDeathHighPriority` block, a commented-out `MDE`/`MFR` integration, and commented-out sound loop code.
- `BlockManaBattery` has a commented-out damage/damage-item scheme.
- `TileEntityBlackAurem` has the commented-out ill-effect trigger.
- `TileEntityCraftingAltar` has large commented-out “devastating explosion” commentary and dead fields (`allShapeGroupsAdded` is `final false`).
- `SpellUtils` has commented-out old stream code and a dead `ISkillTreeEntry entry = ...` local (`SpellUtils.java:695`).
- `AM2WorldDecorator` constructs a `WorldGenEssenceLakes` local and never uses it (`AM2WorldDecorator.java:111`); the actual lake generation creates another instance at `203`.
- Many `String.format("ArrayIndex", i)` calls should be plain constants.

**Error-handling cleanup:**

- 94 `catch (Throwable ...)` blocks and 84 `printStackTrace()` calls. Packet handlers catch `Throwable` and print full stack traces; a malicious client can use this for log flooding. Renderers swallow `Throwable` entirely (for example `EssenceGeneratorRenderer.java:198`, `FlickerHabitatRenderer.java:46,93`), which hides real rendering failures.
- 59 raw `List`/`Map`/`Iterator` declarations; generally harmless on Java 8, but they make the packet code harder to audit.
- Several comment blocks are shouty/profane (for example the repeated “I AM SO BLOODY TIRED…” in `ExtendedProperties.java:73-82`). Clean these up for a public MIT repo.

---

## 10. Smaller correctness / UX issues

- **`AMKeyBindings`**: `SpellBookNextSpellKey` and `AffinityActivationKey` both default to `X` (`AMKeyBindings.java:26-45`). Because the event handler uses an `else if` chain, pressing X with a spell book never reaches the affinity ability, and pressing X without a spell book still consumes the branch. Bind them to different keys (and check for conflicts with other mods).
- **`ShrinkHandler`** subscribes to `LivingEvent` rather than `LivingEvent.LivingUpdateEvent` (`ShrinkHandler.java:16-18`), so the shrink logic also runs on jump/fall events and can apply multiple times per tick. Change the parameter type.
- **`ShrinkHandler`** returns early when the `Shrink` skill is disabled (`21-23`) without restoring the player's size, so disabling the skill can leave a player permanently small until relog. Run the restore path before the early return.
- **`AMEventHandler.onBucketFill`** removes the liquid block in `attemptFill` without a `world.isRemote` guard (`AMEventHandler.java:1793-1813`). `FillBucketEvent` fires on both sides; the client predicts the removal. Make the block change server-authoritative and use the normal bucket flow.
- **`ItemSoulspike`** ethereal form sets `capabilities.disableDamage = true`, `allowEdit = false`, and `noClip` on the server; if the player disconnects/crashes while the extra variable is present, the cleanup path depends on `LivingUpdateEvent`. Add a login-time sanitizer that clears `ethereal`, restores capabilities, and removes the boots tag if the duration is invalid.
- **`EntitySpellProjectile.getShootingEntity`** (`221-225`): the `-999` sentinel calls `getClosestPlayer(..., -1)` and returns the nearest player regardless of distance. A projectile with a missing owner can target an unrelated player. Store the caster UUID in NBT/datawatcher or use a `DummyEntityPlayer` consistently.
- **`TileEntityFlickerHabitat` constructor** checks `worldObj != null` before randomising `rotateOffset`/`floatOffset`, but `worldObj` is always null in a `TileEntity` constructor. The random offsets are never set. Move the randomisation to `setWorldObj` or `updateEntity`.
- **`TileEntityManaBattery.readFromNBT`** uses `PowerTypes.getByID(...)`, which returns `NONE` for unknown IDs, but `outputPowerType` can still be `null` if a future `getByID` changes; add a null fallback and clamp.
- **`TileEntityManaBattery.breakBlock`** only saves the current `outputPowerType` amount (`BlockManaBattery.java:126-145`). If the battery holds multiple power types (possible while empty, as described above), the others are lost. Save all types or forbid mixed storage.
- **`AStar.Path.compareTo`** casts `f - p.f` to `int` (`AStar.java:49-53`), so paths with costs less than 1 apart compare equal. Use `Double.compare`.
- **`IllEffectsManager.entriesSortedByValues`** creates a sorted set and then ignores it; the returned `TreeMap` is unsorted. Either implement the sort or remove the method.
- **`ObeliskFuelHelper.registerFuelType`** mutates the caller's stack to size 0 (`ObeliskFuelHelper.java:18-21`). Copy first.
- **`EnervatorRecipeHelper.getRecipe`** returns the shared output stack and mutates its `stackSize` (`EnervatorRecipeHelper.java:29-53`). Return a copy.
- **`TileEntityCrystalMarker.cyclePriority`** sends a description packet to every nearby player individually (`TileEntityCrystalMarker.java:54-65`); `markBlockForUpdate`/`sendToAllAround` is simpler.
- **`AMClientEventHandler.onJoinWorldClient`** builds a new `tablist` list from the player info list and removes names from that copy, then checks `tablist.isEmpty()`. The intent is to detect “only me online”; it works, but the removal from a copy is confusing. Add a comment or use a boolean count.

---

## 11. Suggested roadmap

### Immediate (before next release)

1. Resolve the coremod registration (2.1). This is a product decision, not just a bug.
2. Lock down client→server packets (2.2, 3.8) and bound the packet reader (2.3).
3. Fix the power persistence cache/comparator (2.4, 2.5) and the compendium NBT index (2.6). These are player-data-loss issues.
4. Fix the config RNG crashes (3.5) and the CoFH/dimension NPEs (3.3).
5. Add regression tests for all of the above.

### Next

6. Fix the chunk-loader ticket lifecycle and the time-fortified release NPE (3.1, 3.2).
7. Remove the per-tick block/TE update spam (3.4) and fix the lingering-spell tick duplication (3.7).
8. Fix the enchantment helper and the spell book NBT bounds (3.6, 4.1).
9. Clean the static-state leaks and add world-unload cleanup (4.6); make `getExtraVariablesContains` copy-vs-view semantics explicit.
10. Correct the Flicker/ItemFrame/Multiblock issues (4.2-4.5).

### Later

11. Split `AMEventHandler` and introduce a `WorldState` manager (6.1, 6.2).
12. Replace stringly-typed extra properties with typed effects (6.3).
13. Build a proper packet validation layer (6.4).
14. Cache skill-tree lookups and parsed spells (5.1), and reduce the allocation hot spots (5.2-5.7).
15. Remove abandoned APIs/dead classes and update `mcmod.info`/README (7, 8, 9).
16. Raise the coverage gate and add the game tests listed in 7.

---

## 12. Access transformers, ASM and reflection — Mixin migration plan

This section audits every access transformer, bytecode transformer and reflective access in the mod, compares the approach with `MrFuzzihead/FuzziTweaks`, and lays out a concrete migration to GTNH Mixins.

### 12.1 Bottom line

**Yes, the ASM and most of the reflective access can be moved to Mixins, and the broad access transformers can be removed or narrowed.** The important qualification is that not every reflection site should become a Mixin:

- **ASM transforms and `@Fix` patches → Mixins.** They are exactly the kind of bytecode-level behaviour Mixins exist for.
- **Private-field access from the mod's own code → `@Accessor` / `@Invoker` Mixin interfaces.** This replaces `ReflectionHelper`, `setAccessible`, and the `Potion` private-field access.
- **Broad access transformers (`*`) → narrow ATs or accessor interfaces.** A `@Mixin` accessor is the cleaner replacement when the access is from unrelated code; for the fishing-hook/arrow subclasses, a narrowly scoped AT is usually simpler and less risky than rewriting 20 inherited field accesses into accessor calls.
- **Reflection used only to load optional mod classes or to instantiate entity classes → do not mixin.** For optional mods, use `Loader.isModLoaded` plus an isolated compat class (as described in 12.2.5); for entity classes, use `EntityList`/a small factory. These are classloading/registry concerns, not access-control concerns.
- **The custom ASM framework (`net.tclproject.mysteriumlib`) → delete.** It is a private transformer framework that duplicates what GTNH Mixins already provides; `Mixin`/MixinExtras replace it with less code and a supported lifecycle.

The current build does not register the ASM coremod at all (see section 2.1), so this migration is also an opportunity to delete a large amount of dead/inert code rather than porting every historical patch blindly.

### 12.2 Current inventory

#### 12.2.1 Access transformers

`src/main/resources/META-INF/arsmagica2_at.cfg` contains four entries:

```text
public net.minecraft.entity.projectile.EntityFishHook *
public net.minecraft.entity.projectile.EntityArrow *
public net.minecraft.potion.Potion field_76418_K
public net.minecraft.client.renderer.EntityRenderer func_78467_g(F)V
```

The SRG Minecraft jar (`build/rfg/srg_merged_minecraft.jar`) shows that these are not cosmetic:

- `EntityFishHook` has private fields such as `field_146037_g`, `field_146048_h`, `field_146050_i`, `field_146046_j`, `field_146051_au`, `field_146055_aB`, etc. `EntityFishHookArcane` and `EntityFishHookInfernal` read/write about 20 of them directly.
- `EntityArrow` has private fields `field_145791_d`, `field_145792_e`, `field_145789_f`, `field_145790_g`, `field_70254_i` (`inGround`), `field_70250_c` (`shootingEntity`), `field_70253_h` (`inData`), `field_70252_j` (`ticksInGround`), `field_70257_an` (`ticksInAir`), `field_70255_ao` (`damage`), and `field_70256_ap` (`knockbackStrength`). `EntityBoundArrow` uses them directly.
- `Potion`'s `field_76418_K` (`isBadEffect`) is private final; `Bless` and `Curse` read it. The public `isBadEffect()` method is `@SideOnly(Side.CLIENT)`, so it is not usable from the common/server code path.
- `EntityRenderer.func_78467_g` (`orientCamera`) is private. No normal compiled code in this repo calls it directly; it is only a target name in the ASM transformer and the old `@Fix` method. This AT entry appears unused and can probably be removed.

The broad `*` entries are the most questionable part of the current build. They expose every member of two vanilla classes to the mod's code because a handful of subclasses use private fields. A better end state is:

1. Replace `EntityFishHook *` with **exact private fields** used by the two subclasses.
2. Replace `EntityArrow *` with **exact private fields** used by `EntityBoundArrow`.
3. Replace `Potion field_76418_K` with either a one-line narrow AT or a `PotionAccessor` Mixin (the latter is preferable if the goal is to remove ATs).
4. Remove the `EntityRenderer func_78467_g` line unless a compile check proves it is needed.

Example narrow AT (replace the two `*` lines; keep only fields that are actually private and used):

```text
# EntityFishHook fields used by EntityFishHookArcane / EntityFishHookInfernal
public net.minecraft.entity.projectile.EntityFishHook field_146037_g
public net.minecraft.entity.projectile.EntityFishHook field_146038_az
public net.minecraft.entity.projectile.EntityFishHook field_146040_ay
public net.minecraft.entity.projectile.EntityFishHook field_146045_ax
public net.minecraft.entity.projectile.EntityFishHook field_146046_j
public net.minecraft.entity.projectile.EntityFishHook field_146047_aw
public net.minecraft.entity.projectile.EntityFishHook field_146048_h
public net.minecraft.entity.projectile.EntityFishHook field_146049_av
public net.minecraft.entity.projectile.EntityFishHook field_146050_i
public net.minecraft.entity.projectile.EntityFishHook field_146051_au
public net.minecraft.entity.projectile.EntityFishHook field_146054_aA
public net.minecraft.entity.projectile.EntityFishHook field_146055_aB
public net.minecraft.entity.projectile.EntityFishHook field_146056_aC
public net.minecraft.entity.projectile.EntityFishHook field_146057_aD
public net.minecraft.entity.projectile.EntityFishHook field_146058_aE
public net.minecraft.entity.projectile.EntityFishHook field_146059_aF
public net.minecraft.entity.projectile.EntityFishHook field_146060_aG

# EntityArrow private fields used by EntityBoundArrow
public net.minecraft.entity.projectile.EntityArrow field_145789_f
public net.minecraft.entity.projectile.EntityArrow field_145790_g
public net.minecraft.entity.projectile.EntityArrow field_145791_d
public net.minecraft.entity.projectile.EntityArrow field_145792_e
public net.minecraft.entity.projectile.EntityArrow field_70250_c
public net.minecraft.entity.projectile.EntityArrow field_70252_j
public net.minecraft.entity.projectile.EntityArrow field_70253_h
public net.minecraft.entity.projectile.EntityArrow field_70254_i
public net.minecraft.entity.projectile.EntityArrow field_70255_ao
public net.minecraft.entity.projectile.EntityArrow field_70256_ap
public net.minecraft.entity.projectile.EntityArrow field_70257_an
```

Verify this list against the SRG jar (`javap -classpath build/rfg/srg_merged_minecraft.jar -p ...`) before applying it. If the project wants **zero access transformers**, the alternative is an accessor interface:

```java
@Mixin(EntityFishHook.class)
public interface EntityFishHookAccessor {

    // Use the field name exactly as Mixin sees it in the development mapping
    // (field_146037_g in this workspace); the annotation processor writes the
    // production obfuscation mapping into the refmap.
    @Accessor("field_146037_g")
    int am2$getBlockX();

    @Accessor("field_146037_g")
    void am2$setBlockX(int x);

    @Accessor("field_146051_au")
    boolean am2$isInGround();

    // ...one pair per private field used by the subclasses
}
```

Then rewrite `this.field_146037_g` to `((EntityFishHookAccessor) this).am2$getBlockX()`. That is a mechanical change across two large subclasses and is **not** a good first migration step. The narrow AT is the smaller, safer change; the accessor route is the way to get rid of the AT entirely later.

#### 12.2.2 Bytecode transformers

`am2/preloader/BytecodeTransformers.java` has ten active transformer methods (some with multiple insertion points, so the table below has more rows). These map cleanly to Mixins:

| Transformer | Target and behaviour | Mixin replacement |
|---|---|---|
| `alterServerTickrate` | Replaces every `LDC 50L` in `MinecraftServer.run()` with `MysteriumPatchesFixesMagicka.servertickrate` | `MinecraftServerMixin` with `@ModifyConstant` on the `50L` constants, or `@Redirect` on `Thread.sleep(J)V` plus the tick-loop constants. Prefer a normal `TickrateController` helper instead of the ASM class. |
| `alterRendererLivingEntity` | After `RendererLivingEntity.doRender()` calls `renderLivingAt(entity, x, y, z)`, calls `RenderUtilities.setupShrinkRender(entity)` | `RendererLivingEntityMixin` with `@Inject(method = "doRender", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/RendererLivingEntity;renderLivingAt(Lnet/minecraft/entity/EntityLivingBase;DDD)V", shift = At.Shift.AFTER))`. Use `@Local(argsOnly = true) EntityLivingBase` or the method argument. |
| `alterEntityRenderer` (camera shift/flip) | After `EntityRenderer.setupCameraTransform()` calls `orientCamera(F)` and `GLU.gluPerspective(FFFF)`, calls `AMGuiHelper.shiftView(partialTicks)` / `flipView(partialTicks)` | `EntityRendererMixin` with `@Inject` at each `INVOKE`, `shift = AFTER`. `gluPerspective` is a LWJGL call and is not obfuscated; the target must include its owner/descriptor. |
| `alterEntityRenderer` (mouse) | After `Profiler.startSection("mouse")` in `updateCameraAndRender()`, replaces the local boolean used by the following `if` with `AMGuiHelper.overrideMouseInput(renderer, partialTicks, flag)` | `EntityRendererMixin` with `@Inject` after `startSection("mouse")` and a `@Local`/MixinExtras local reference, or a `@ModifyVariable` at the boolean store. Do **not** use a hard-coded local index. The old OptiFine local-shift branch is removed with OptiFine support; if Angelica/NotFine compatibility is later added, verify its local layout separately. |
| `alterEntityPlayerSP` | After `movementInput.updatePlayerMoveState()` in `EntityPlayerSP.onLivingUpdate()`, calls `AMGuiHelper.overrideKeyboardInput()` | `EntityPlayerSPMixin` with `@Inject` at the `INVOKE` of `updatePlayerMoveState`, `shift = AFTER`. |
| `alterWorld` | In `World.playSoundAtEntity()`, modifies the pitch argument before `IWorldAccess.playSound(...)` | `WorldMixin` with `@ModifyArg` on `IWorldAccess.playSound(String,DDDDFF)` index 5, using `@Local(argsOnly = true) Entity` to get the entity; or `@ModifyVariable` at the pitch local. This is cleaner than rewriting locals. |
| `alterNetHandlerPlayServer` | In `NetHandlerPlayServer.processPlayer()`, after `d4 = stance - yPosition` is stored, overwrites `d4` with `1.5` | `NetHandlerPlayServerMixin` with `@ModifyVariable` at the `STORE` for `d4` (or `@Inject` after the store and a mutable `@Local`). Decide whether this behaviour is still wanted; it currently disables the illegal-stance check for every player, not just ethereal players. |
| `alterPotionEffect` | Changes `PotionEffect` NBT write/read of the potion ID from `setByte`/`getByte` to `setInteger`/`getInteger` | If potion IDs are capped at 127 (see below), delete this transformer. If not, `PotionEffectMixin` with `@Redirect` on `NBTTagCompound.setByte(String,B)` and `getByte(String)` in the two methods, including a compatibility read for old byte tags. |
| `alterS1DPacketEntityEffect` | Widens the potion ID on the network packet from byte to int, including packet field accesses | Best avoided: Mixin cannot change a field descriptor in place. Either cap potion IDs at 127 and delete the transformer, or use an `@Overwrite`/new int field and packet mixins. The former is strongly preferred. |
| `alterNetHandlerPlayClient` | Corresponding byte→int handling for the client packet path | Same as `S1D`: cap IDs and delete, or use packet-level Mixins with a new int field. |
| `alterS1EPacketRemoveEntityEffect` | Byte→int potion ID in the remove-effect packet | Same as `S1D`: cap IDs and delete, or packet Mixin. |

The potion-ID widening transforms exist solely to support potion IDs above 127. The mod's default buff IDs are 24–56. The clean solution is to **enforce potion IDs in `1..127`** (config validation), delete all four potion-ID packet/NBT transformers, and keep the array large enough for the mod's own buffs. This removes the trickiest ASM→Mixin port and avoids changing packet field types or breaking old saves.

#### 12.2.3 `@Fix` patches

`MysteriumPatchesFixesMagicka` has 34 active `@Fix` methods. The first parameter is the target class, and the method name/parameters identify the target method (see `FixParser`/`ASMFix`). The active methods group as follows:

| Target | Patches | Mixin strategy |
|---|---|---|
| `World` | `selectEntitiesWithinAABB`, `getClosestPlayer`, `getEntitiesWithinAABBExcludingEntity`, `isBlockIndirectlyGettingPowered` | `WorldMixin`: `@ModifyReturnValue` for the three entity-query methods; `@Inject(method = "isBlockIndirectlyGettingPowered", at = @At("HEAD"), cancellable = true)` to return the redstone-inlay result. Move `providingRedstone` to a normal `RedstoneInlayState` helper. |
| `Entity` | `isEntityInsideOpaqueBlock`, `handleWaterMovement`, `applyEntityCollision`, `isInvisibleToPlayer`, `isInsideOfMaterial`, `isInWater` | `EntityMixin`: `@ModifyReturnValue` for the boolean queries, or `@Inject(at = @At("HEAD"), cancellable = true)` for the short-circuit cases. |
| `EntityLivingBase` | `canBePushed`, `canBeCollidedWith`, `isOnLadder` | `EntityLivingBaseMixin`: `@ModifyReturnValue`/`@Inject`. `isOnLadder` should use `ForgeHooks.isLivingOnLadder(...)` instead of re-implementing the vanilla check. |
| `NetHandlerPlayServer` | `processClickWindow` | `NetHandlerPlayServerMixin`: `@Inject` at `HEAD`, cancellable; send the container snapshot and return early for ethereal players. |
| `EntityRenderer` | `orientCamera`, `orientCameraEnd` | `EntityRendererMixin`: `@Inject` at HEAD/TAIL to set/clear the client flag. The flag can live in a normal `ClientEtherealState` rather than the old ASM class. |
| `WorldRenderer` | `updateRenderer`, `updateRendererEnd` | `WorldRendererMixin`: `@Inject` HEAD/TAIL if the flag is still needed. `updatingRenderWorld` is otherwise unused, so consider deleting both patches. |
| `Vec3` | `distanceTo` | `Vec3Mixin`: `@ModifyReturnValue` returning `Double.MAX_VALUE` for the ethereal local camera. |
| `RenderManager` | `getEntityRenderObject` | `RenderManagerMixin`: `@Inject(at = @At("RETURN"), cancellable = true)` or `@ModifyReturnValue` to return the special player renderer for maid models. |
| `EventBus` | `register` | This exists only to block FoamFix's Ears transformer. Prefer deleting it and documenting the incompatibility, or use an `EventBusMixin` with `remap = false`. |
| `Block` | `getRenderType` | The fix currently always returns false; it does nothing. Delete it. |
| `Item` | `setDamage` | Do not mixin `Item.setDamage`. Override `setDamage` in `ItemSoulspike` (or add the check in its `onUpdate`/use path). This removes a global patch. |
| `EntityItem` | `onUpdate` | Do not mixin `EntityItem.onUpdate`. Implement `ItemOre.onEntityItemUpdate(EntityItem)` for cognitive dust (and etherium) and remove the global patch. `EntityItem` already calls `Item.onEntityItemUpdate` (`EntityItem.java:96`). |
| `TileEntity` | `hasWorldObj` | Do not port this global slowdown patch. It changes a method used by renderers/chests and is a compatibility hazard. Remove it and implement per-TE throttling in the AM2 TEs or a world-tick manager. `slowedTiles` then becomes dead. |
| `BlockDynamicLiquid` | `updateTick` | `BlockDynamicLiquidMixin`: `@Inject` at HEAD, cancellable, for the lava/essence reaction. |
| `BlockLiquid` | `velocityToAddToEntity` | `BlockLiquidMixin`: `@Inject` at HEAD, cancellable, for the ethereal water exception. |
| `EffectRenderer` | `renderParticles` | `EffectRendererMixin`: `@Inject` at HEAD, cancellable, when cloaking is active. |
| `ModelMagmaCube` / `ModelSkeleton` | `setLivingAnimations` | Client `ModelMagmaCubeMixin`/`ModelSkeletonMixin`: `@Inject` at HEAD, cancellable, for hallucinations. Or use a custom renderer/model and delete the global patch. |
| `RenderFish` | `getEntityTexture` | Prefer a custom renderer for `EntityFishHookArcane`/`EntityFishHookInfernal` and delete the global `RenderFish` patch. If keeping it, `RenderFishMixin`: `@Inject` at RETURN, cancellable. |
| `Render` | `doRenderShadowAndFire` | Custom fishing-hook renderer (override the method) is cleaner than a global `RenderMixin`. |
| `CraftingManager` | `findMatchingRecipe` | Do not port this as a global mixin. Implement the staff/spell/armor combinations as normal `IRecipe` instances registered in `CraftingManager` (or a `CraftingManagerMixin` as a temporary fallback). This is one of the largest and most fragile `@Fix` patches. |
| `RealmsBridge` | `switchToRealms` | The patch disables a Realms crash. Delete it unless the crash is still reproducible; if needed, a client-only `RealmsBridgeMixin` with `@Inject` at HEAD, cancellable. |

The `@Fix` methods also own unrelated static state: `providingRedstone`, `countdownToChangeBack`, `playerModelMap`, `staffSlotTo`/`spellSlotFrom`/`armorSlotTo`, `tileticks`, `timerField`, etc. Those are normal mutable state and should move to normal classes (`RedstoneInlayState`, `ClientPlayerModelMap`, `CraftingRecipeBuilder`, `TickrateController`) so the mixins are thin and testable.

Before deleting `MysteriumPatchesFixesMagicka`, also move its ordinary helper API. `ArmorEventHandler` calls `MysteriumPatchesFixesMagicka.isPlayerEthereal(...)` in more than a dozen places, `ClientTickHandler`/`AMClientEventHandler` use `playerModelMap`, and `AMEventHandler`/`ServerTickHandler` use `changeServerTickrate`/`changeTickrate`. Put `isPlayerEthereal` and the player-model map in normal utility classes first; the remaining class can then be deleted without a mechanical import rewrite across the event handlers.

#### 12.2.4 Reflection inventory

These are the reflective accesses that are **not** part of the ASM framework (the ASM framework's own reflection is deleted with it):

| Site | Current access | Replacement |
|---|---|---|
| `AMEventHandler.java:1339-1342` | `ReflectionHelper.findMethod(Entity.class, ..., "func_70069_a", "fall", float.class)` + `setAccessible`/`invoke` | `EntityInvoker` Mixin: `@Invoker("fall") void am2$invokeFall(float)`. Same for `WaterWalking.java:110-113`. |
| `WaterWalking.java:110-113` | same `Entity.fall` reflection | Same `EntityInvoker`. |
| `ArmorEventHandler.java:105-122` | `ReflectionHelper.get/setPrivateValue(EntityPlayer.class, player, "speedInAir", "field_71102_ce")` | `EntityPlayerAccessor` with `@Accessor("speedInAir") float getSpeedInAir()` and `void setSpeedInAir(float)`. |
| `Dispelling.java:53-56` | `ReflectionHelper.getPrivateValue(Potion.class, ..., 35)` for the bad-effect flag | `PotionAccessor` with `@Accessor("isBadEffect") boolean am2$isBadEffect()`, or keep one narrow AT line. |
| `BlocksCommonProxy.java:934-945` | `ReflectionHelper.getPrivateValue(GameData.class, null, "mainData")` + `findMethod("registerBlock"/"registerItem")` | Use the public `GameRegistry.registerBlock(block, itemClass, name, ctorArgs...)` overload. `ItemMultiTexture` and `ItemSlab` constructors can be supplied through the existing `itemCtorArgs` parameter. No Mixin needed. |
| `BuffList.java:332-338` | `setFinalStatic` reflection to replace `Potion.potionTypes` | `PotionMixin` with `@Shadow @Final @Mutable private static Potion[] potionTypes;` and `@Inject(method = "<clinit>", at = @At("TAIL"))` to allocate a larger array (e.g. 128). Then `BuffList` no longer needs `setPotionArrayLength`/reflection. |
| `BuffList.java:355` | scans `Potion.class.getDeclaredFields()` to find the `Potion[]` field | Removed by `PotionMixin`. |
| `BuffList.java:391-393` | `getDeclaredConstructors()[0]` + `newInstance` for `BuffEffect` | Use a `Map<Integer, Factory<BuffEffect>>` or `getConstructor(int.class, int.class).newInstance(...)`. No Mixin. |
| `BuffList.java:409-412` | `ReflectionHelper.findField(BuffList.class, name)` to read own static potion fields | Build a `Map<String, ArsMagicaPotion>` in `Instantiate()`. No reflection/Mixin. |
| `DamageSources.java:136` | `ReflectionHelper.setPrivateValue(DamageSource.class, original, true, "field_76374_o", "isUnblockable")` | `DamageSourceAccessor` with `@Accessor("isUnblockable") void setUnblockable(boolean)`. |
| `EntityManaCreeper.java:35` | `ReflectionHelper.setPrivateValue(EntityCreeper.class, this, 0, 1)` (field index 1) | `EntityCreeperAccessor` with `@Accessor("timeSinceIgnited") void setTimeSinceIgnited(int)`. |
| `AMGuiHelper.java:739-766` | five `ReflectionHelper` get/set calls on private `EntityRenderer` `smoothCam*` fields | `EntityRendererAccessor` with getters/setters for `smoothCamYaw`, `smoothCamPitch`, `smoothCamPartialTicks`, `smoothCamFilterX`, `smoothCamFilterY`. |
| `GuiArcaneCompendium.java:365-368` | `ReflectionHelper.getPrivateValue(ShapedOreRecipe.class, ..., "width"/"height")` | `ShapedOreRecipeAccessor` (`remap = false` if targeting the Forge class) with `@Accessor("width") int getWidth()` and `getHeight()`. `getInput()` is already public. |
| `ArcaneCompendium.java:145-146` | `ReflectionHelper.getPrivateValue(Minecraft.class, ..., "launchedVersion")` | Use `cpw.mods.fml.common.Loader.MC_VERSION` (public constant) or a `MinecraftAccessor.launchedVersion()` if the exact launched version is needed. |
| `RecipeUtilities.java:43,48` | `ReflectionHelper.getPrivateValue(ShapedOreRecipe.class, ..., 3)` and `ShapelessOreRecipe.class, ..., 1` | Use the public `ShapedOreRecipe.getInput()` and `ShapelessOreRecipe.getInput()`. `ShapedRecipes.recipeItems`/`ShapelessRecipes.recipeItems` are already public. |
| `CompendiumEntryStructure.java:41-42` | `ReflectionHelper.getPrivateValue(TileEntity.class, null, 1)` for the static `nameToClassMap` | `TileEntityAccessor` with `@Accessor("nameToClassMap") static Map<String, Class<? extends TileEntity>>` (or an AM2 registration map of controller class → TE class). |
| `CompendiumEntryRitual.java:48-49` | `RitualShapeHelper.class.getField(...).get(...)` | Add a public getter/map to `RitualShapeHelper`; no reflection or Mixin. |
| `EntityUtilities.java:55-70` | `EntityLiving.class.getDeclaredMethod("func_70650_aV"/"isAIEnabled")` + `setAccessible`/`invoke` | `EntityLivingInvoker` with `@Invoker("isAIEnabled") boolean am2$isAIEnabled()`. |
| `MysteriumPatchesFixesMagicka.java:540-560` | `Minecraft.timer` field lookup + assignment for tickrate changes | `MinecraftAccessor` with `@Accessor("timer") void setTimer(Timer)`; or remove the client tickrate feature if it is not retained. |
| `CustomLoadingPlugin.java:96-97`, `MinecraftMetaReader.java:23-28,125`, `MetaReader.java:29-30,304`, `MysteriumPatchesFixLoaderMagicka.java:31-34` | `CoreModManager.deobfuscatedEnvironment`, `LaunchClassLoader.runTransformers`, `ClassLoader.findLoadedClass`, OptiFine `Class.forName` probes | Delete with the ASM framework. OptiFine support is stripped; if Angelica/NotFine needs compatibility, add a gated late/client Mixin instead. Mixins do not need these loader internals. |
| `AMChunkLoader.java:100` | `Class.forName(clazzName)` from ticket NBT | Keep `Class.forName` for a server-authored ticket, but validate the result is a known `TileEntity` class (or store a whitelist/registry instead of a class name). Not a Mixin. |
| `AMCore.java:203-205` | `Class.forName` for optional Forestry/MagicBees classes | Replace with `Loader.isModLoaded` and an isolated `AMBeeCompat`; not a Mixin and not a reason to add a required dependency. |
| `AMConfig.java:844` | `Class.forName` for the mob/appropriation blacklist | Keep with validation (must be an `Entity`/`EntityLivingBase` subclass); not a Mixin. |
| `SpawnBlacklists.java:28,42` | `Class.forName` from IMC strings | Keep with validation; this is an API/security boundary, not a Mixin. |
| `Appropriation.java:340` | `Class.forName(...).getConstructor(World.class).newInstance(...)` for stored entity class | Use `EntityList.createEntityByName`/a whitelist/factory. Avoid arbitrary class instantiation from NBT. |
| `AMSpawnEgg.java:316`, `EntityAISummonAllies.java:70`, `CompendiumEntryBoss.java:34`, `CompendiumEntryMob.java:34` | reflective `(World)` constructors for known entity classes | Use `EntityList.createEntityByName`/`EntityList.createEntityByID`, or a small factory interface. No Mixin. |
| `EntityAIApplyPotionOnCollide.java:124-141` | reflective `PotionEffect(int,int,int)` constructor to copy a `PotionEffect` subtype | Add a `copy()`/factory method to the buff base, or use `new PotionEffect(...)` where subclass identity is not required. No Mixin. |

#### 12.2.5 Optional-mod checks vs adding a dependency

Only a few reflection sites are actually checking whether another mod's classes exist:

| Site | What is checked | Is a dependency the right fix? |
|---|---|---|
| `AMCore.java:203-205` | `forestry.api.recipes.RecipeManagers`, `magicbees.bees.BeeProductHelper`, `magicbees.bees.BeeSpecies` before `AMBeeCompat.init()` | **Use `Loader.isModLoaded` instead of `Class.forName`.** `compileOnly` already gives compile-time access, and Forestry/MagicBees are optional per the README. Do not add `implementation`/`api`; that would either require the mods or still leave the classes absent in a normal install. Replace the three probes with `Loader.isModLoaded("Forestry") && Loader.isModLoaded("MagicBees")`, keep `AMBeeCompat` isolated, and catch `NoClassDefFoundError`/`LinkageError` around `init()` for API-version mismatches. |
| `MysteriumPatchesFixLoaderMagicka.java:31-34` | `optifine.OptiFineForgeTweaker` / `optifine.OptiFineTweaker` | **No — OptiFine support is to be stripped.** Do not add an OptiFine dependency. Remove the OptiFine probe and the OptiFine-specific branches together with `MysteriumPatchesFixLoaderMagicka`/`BytecodeTransformers`. If Angelica/NotFine later needs equivalent compatibility, add a narrowly gated late/client Mixin (`TargetMods` + `Mixins`); otherwise leave OptiFine compatibility out entirely. |
| `SpawnBlacklists.java:28,42` | arbitrary entity class names supplied through the mod's IMC API | **No.** The mod cannot depend on every mod that might send an IMC blacklist; keep `Class.forName` with validation/caching. |
| `AMConfig.java:844` | arbitrary entity class names from the user's config | **No.** The class is user data, not a known dependency. Keep validation (must be an `Entity`/`EntityLivingBase` subclass) and catch failures. |
| `AMChunkLoader.java:100` | a `TileEntity` class name saved in a chunk-loader ticket | **No.** This is persisted data; use a registry/whitelist of allowed TE classes rather than adding dependencies. |
| `Appropriation.java:340` | an entity class name stored in spell/item NBT | **No.** Use `EntityList.createEntityByName`/a whitelist instead of arbitrary `Class.forName` + constructor access. |

The remaining reflection in the inventory is **not** optional-mod detection: it is private-field/method access or an internal Forge/loader API. Adding a dependency does not make a private field public, so those sites need Mixin accessors/invokers or public API replacements, not a Gradle dependency.

For the Forestry/MagicBees case specifically, the recommended end state is:

```java
if (Loader.isModLoaded("Forestry") && Loader.isModLoaded("MagicBees")) {
    try {
        AMBeeCompat.init(); // isolated class containing the direct API calls
    } catch (NoClassDefFoundError | LinkageError e) {
        LogHelper.info("A compatible Forestry/MagicBees API was not found, compat not loading.");
    }
}
```

`compileOnly` remains in `dependencies.gradle`; no `implementation`/`api` is required. If the project instead decides to make Forestry/MagicBees mandatory, then:

- change the dependency to `api`/`implementation`,
- add `required-after:Forestry;required-after:MagicBees` to `@Mod.dependencies`,
- update `mcmod.info`/README/CurseForge relations,
- and only then replace the `Loader.isModLoaded` branch with direct calls.

That is a product decision (drop optional compatibility), not a Mixin/reflection cleanup. The same logic applies to any other optional integration.

**OptiFine:** remove the support rather than adding a dependency. That means deleting `MysteriumPatchesFixLoaderMagicka`'s `Class.forName("optifine...")` probes, the `foundOptiFine`/`isOptiFinePresent` and `isOptifineFilePresent` branches, and the OptiFine-specific local-index handling in `BytecodeTransformers`/`AMGuiHelper`. If Angelica/NotFine needs the same camera/mouse compatibility after the Mixin migration, implement it as a separately gated late/client Mixin and add `TargetMods` entries for Angelica/NotFine; do not restore the old OptiFine coremod probe.

### 12.3 The FuzziTweaks Mixin strategy (reference)

`MrFuzzihead/FuzziTweaks` is a useful template because it uses GTNH's Mixin stack on the same 1.7.10/Forge/GTNH build convention:

1. **Enable Mixins in `gradle.properties` and register an early loader.**
   - `usesMixins = true`
   - `mixinsPackage = mixins`
   - `coreModClass = mixins.EarlyMixinsLoader`
   - `usesMixinDebug = true` in development.
2. **Two loaders:**
   - `EarlyMixinsLoader` implements `IFMLLoadingPlugin, IEarlyMixinLoader`; `getMixinConfig()` returns `mixins.fuzzitweaks.early.json`; `getMixins(loadedCoreMods)` returns `IMixins.getEarlyMixins(Mixins.class, loadedCoreMods)`.
   - `LateMixinsLoader` implements `ILateMixinLoader` and is annotated `@LateMixin`; it returns `IMixins.getLateMixins(Mixins.class, loadedMods)`.
3. **One `Mixins` enum implements `IMixins`.** Each entry is a `MixinBuilder` with:
   - `.setPhase(Phase.EARLY/LATE)`
   - `.addCommonMixins("...")` / `.addClientMixins("...")`
   - `.addRequiredMod(TargetMods.X)` / `.addExcludedMod(...)`
   - `.setApplyIf(() -> Config.option)`
4. **`TargetMods` enum** wraps optional mod/coremod IDs with `TargetModBuilder` (`setCoreModClass`, `setModId`), so late mixins only load when the target is present.
5. **Phase-specific JSON files** (`mixins.fuzzitweaks.early.json`, `.late.json`) set `package`, `refmap`, `minVersion = 0.8.5-GTNH`, `compatibilityLevel = JAVA_8`.
6. **Mixin style:** `@Mixin`, `@Shadow`, `@Inject`, `@Redirect`, `@WrapOperation` and `@ModifyExpressionValue` from MixinExtras, `@Overwrite` only where an iterator/whole-method replacement is unavoidable, `require = 1` on every injection, and `remap = false` for Forge classes such as `ForgeChunkManager`/`EventBus`.
7. **No explicit `gtnhmixins` dependency** is declared in FuzziTweaks' `dependencies.gradle`; the GTNH convention plugin supplies the Mixin/UniMixins toolchain when `usesMixins = true` (the generated metadata also makes UniMixins a required dependency). The AM2 build uses the same convention plugin, so enabling `usesMixins` is enough.
8. **Early config:** `getMixins()` calls `Config.synchronizeConfiguration(...)` before FML pre-init because `setApplyIf`/mixins are selected before the normal `FMLPreInitializationEvent`. This is essential: `AMCore.config` is created in pre-init, so if AM2 wants config-gated mixins, it needs an equivalent early config bootstrap (or must apply the mixins unconditionally and gate the behaviour at runtime).
9. **Tests:** FuzziTweaks cannot apply Mixins headless, so it has bytecode guard tests that read the compiled mixin class and assert the `@Inject`/`@Redirect` target and `require` contract (for example `CoalGeneratorMixinTest`). It then covers the actual transformed behaviour in the in-game test mod. Mixin classes are excluded from the JaCoCo coverage report because they are not runnable headless.

That is the strategy to copy, with AM2-specific package names and feature grouping.

### 12.4 Proposed Ars Magica layout

```text
src/main/java/am2/mixins/
  EarlyMixinsLoader.java
  LateMixinsLoader.java
  Mixins.java
  TargetMods.java
  MixinConfig.java                 // early AM2.cfg reader, if config-gated mixins are needed
  early/
    WorldMixin.java
    EntityMixin.java
    EntityLivingBaseMixin.java
    EntityPlayerMixin.java
    EntityPlayerSPMixin.java
    EntityItemMixin.java            // only if a normal Item hook is not enough
    EntityRendererMixin.java
    RendererLivingEntityMixin.java
    RenderManagerMixin.java
    RenderFishMixin.java            // or a custom renderer instead
    EffectRendererMixin.java
    MinecraftServerMixin.java
    MinecraftMixin.java
    NetHandlerPlayServerMixin.java
    NetHandlerPlayClientMixin.java  // only if >127 potion IDs are kept
    PotionMixin.java
    S1DPacketEntityEffectMixin.java // only if >127 potion IDs are kept
    S1EPacketRemoveEntityEffectMixin.java
    BlockMixin.java
    BlockDynamicLiquidMixin.java
    BlockLiquidMixin.java
    CraftingManagerMixin.java       // temporary fallback for custom recipes
    TileEntityMixin.java            // only if hasWorldObj is retained
    RealmsBridgeMixin.java          // only if the Realms crash fix is retained
  late/
    cofh/...                        // only if an optional CoFH mixin is genuinely needed
    thaumcraft/...
    angelica/...                    // only if Angelica/NotFine compatibility is confirmed
```

`gradle.properties`:

```properties
usesMixins = true
mixinsPackage = mixins
coreModClass = am2.mixins.EarlyMixinsLoader
usesMixinDebug = true
# keep accessTransformersFile only if the narrow AT is retained
accessTransformersFile = arsmagica2_at.cfg
```

The two phase JSON files should mirror FuzziTweaks:

```json
{
  "required": true,
  "minVersion": "0.8.5-GTNH",
  "package": "am2.mixins.early",
  "refmap": "mixins.arsmagica2.refmap.json",
  "target": "@env(DEFAULT)",
  "compatibilityLevel": "JAVA_8"
}
```

```json
{
  "required": true,
  "minVersion": "0.8.5-GTNH",
  "package": "am2.mixins.late",
  "refmap": "mixins.arsmagica2.refmap.json",
  "target": "@env(DEFAULT)",
  "compatibilityLevel": "JAVA_8"
}
```

`Mixins` enum groups should mirror feature ownership, not one giant entry:

```java
public enum Mixins implements IMixins {
    ETHEREAL(new MixinBuilder().setPhase(Phase.EARLY)
        .addCommonMixins("WorldMixin", "EntityMixin", "EntityLivingBaseMixin")
        .addClientMixins("RenderManagerMixin", "RenderFishMixin")),
    SHRINK_FLIP(new MixinBuilder().setPhase(Phase.EARLY)
        .addClientMixins("EntityRendererMixin", "RendererLivingEntityMixin", "EntityPlayerSPMixin")),
    TICKRATE(new MixinBuilder().setPhase(Phase.EARLY)
        .addCommonMixins("MinecraftServerMixin")
        .addClientMixins("MinecraftMixin")
        .setApplyIf(() -> MixinConfig.enableTickrate)),
    POTION_IDS(new MixinBuilder().setPhase(Phase.EARLY)
        .addCommonMixins("PotionMixin")),
    POTION_PACKETS(new MixinBuilder().setPhase(Phase.EARLY)
        .addCommonMixins("S1DPacketEntityEffectMixin", "NetHandlerPlayClientMixin", "S1EPacketRemoveEntityEffectMixin")
        .setApplyIf(() -> false /* only if IDs above 127 are supported */));
    // ...
}
```

The exact names are illustrative; the point is that each group can be enabled/disabled and client/common mixins are separated.

### 12.5 Migration order

1. **Audit and narrow the AT file first.** Replace `EntityFishHook *`/`EntityArrow *` with the exact field lists, remove the likely-unused `EntityRenderer` entry, and decide whether `Potion field_76418_K` stays as an AT or becomes a `PotionAccessor`. Compile with the narrowed file before touching Mixins. This reduces the compile-time surface immediately.
2. **Add the Mixin infrastructure without porting behaviour.** Add `usesMixins`, the two loaders, `Mixins`/`TargetMods`, early/late JSON, and one smoke mixin (for example `PotionMixin` growing `potionTypes` to 128). Add a game-test assertion that `Potion.potionTypes.length >= 128` and a bytecode guard test for the mixin. This proves the loader/manifest/refmap path before changing gameplay.
3. **Move the easy non-Mixin cases first:**
   - Replace `AMCore`'s Forestry/MagicBees `Class.forName` probes with `Loader.isModLoaded("Forestry") && Loader.isModLoaded("MagicBees")`, keeping `AMBeeCompat` isolated.
   - Strip the OptiFine support: delete the OptiFine `Class.forName` probes, `foundOptiFine`/`isOptiFinePresent`/`isOptiFineFilePresent` branches, and the OptiFine local-index workaround. Only add a gated Angelica/NotFine Mixin later if that compatibility is actually required.
   - `ItemSoulspike.setDamage` override instead of the global `Item.setDamage` patch.
   - `ItemOre.onEntityItemUpdate` instead of `EntityItem.onUpdate`.
   - Custom `IRecipe`/`CraftingManager` registration instead of the `findMatchingRecipe` patch.
   - Custom fishing-hook renderer instead of `RenderFish`/`Render.doRenderShadowAndFire`.
   - `GameRegistry.registerBlock(block, itemClass, name, args...)` instead of `GameData` reflection.
   - Public `ShapedOreRecipe.getInput()`/`ShapelessOreRecipe.getInput()` instead of `RecipeUtilities` reflection.
   - `Loader.MC_VERSION` instead of the `launchedVersion` reflection.
   - Public getters/factories for the `Class.forName`/constructor-reflection sites.
4. **Port the reflection accessors:** `Entity.fall` invoker, `EntityPlayer.speedInAir` accessor, `EntityLiving.isAIEnabled` invoker, `DamageSource.isUnblockable` accessor, `EntityCreeper.timeSinceIgnited` accessor, `EntityRenderer.smoothCam*` accessors, `Minecraft.timer` accessor, `ShapedOreRecipe.width/height` accessor, `TileEntity.nameToClassMap` accessor.
5. **Port the core transforms:** `MinecraftServer` tickrate, `RendererLivingEntity` shrink, `EntityRenderer` camera/mouse, `EntityPlayerSP` input, `World` sound pitch, `NetHandlerPlayServer` stance. Test each in-game (client and server) before moving on.
6. **Port the `@Fix` groups:** entity filtering/ethereal, water/ethereal, client rendering, block/liquid, item/entity-item, `NetHandlerPlayServer`, `CraftingManager` fallback, etc. Prefer `@ModifyReturnValue`/`@WrapOperation` over `@Overwrite`.
7. **Delete the old framework and dead code:** `BytecodeTransformers`, `MysteriumPatchesFixLoaderMagicka`, `MysteriumPatchesFixesMagicka` (after moving the surviving helpers), `CustomLoadingPlugin`, `FirstClassTransformer`, `CustomClassTransformer`, `ASMFix` and the other `mysteriumlib` ASM classes, `methods.bin`, the `EntityRenderer` AT line if unused, and the old `@Fix`-only static state.
8. **Update tests and CI:** add bytecode guard tests for every mixin, assert the Mixin config/manifest is loaded, and add in-game assertions for at least one transformed path per feature group.

### 12.6 Pitfalls specific to this codebase

- **The current coremod is silently absent.** Do not assume "it works in dev" means the transformer is active. The produced manifest has no `FMLCorePlugin`; see section 2.1. After adding Mixins, assert the `FMLCorePlugin`/`Mixin` loader is present in CI.
- **Do not run the old ASM and the new Mixins at the same time.** If both are active, injections apply twice (especially `@Inject` at HEAD/TAIL) and behaviour becomes nondeterministic. Remove/disable the old coremod before porting, or keep it unregistered while the Mixins are developed.
- **`AMCore.config` is too late for `MixinBuilder.setApplyIf`.** FuzziTweaks solves this by loading its config in `getMixins()` (`EarlyMixinsLoader`) before FML pre-init. If AM2 wants config-gated mixins, add an equivalent `MixinConfig` reader for `config/AM2/AM2.cfg`; otherwise apply the mixins unconditionally and gate their handler bodies on `AMCore.config` at runtime.
- **Use MCP names in Mixin annotations, not the SRG names used in much of this source.** The refmap maps MCP member names to obfuscated names. `@Accessor("speedInAir")`/`@Invoker("fall")`/`@At(target = "Lnet/minecraft/...;renderLivingAt(...)V")` are the right form. `remap = false` is only for Forge/non-MCP classes (`EventBus`, `GameData`, `ForgeChunkManager`, `ShapedOreRecipe`).
- **OptiFine support is being stripped, so do not keep its local-index workaround.** `alterEntityRenderer` currently branches between local 2 and 3 for the mouse boolean, and `alterNetHandlerPlayServer` hard-codes local 13; the OptiFine-specific branch should be deleted with the rest of the OptiFine compatibility. A Mixin `@ModifyVariable`/`@Local`/`@WrapOperation` should avoid brittle local indexes. If Angelica/NotFine is adopted later, treat its local layout as a separate compatibility target and test it explicitly. FuzziTweaks' `@WrapOperation` style is a good model.
- **`@Redirect` of constructors is rejected by GTNH's Mixin fork.** FuzziTweaks documents this in `HatsCdn`/`ThreadHatsReaderMixin`. Use `@WrapOperation`/`@ModifyArg`/a wrapper helper instead of redirecting `new`.
- **Mixin cannot change a field descriptor in place.** This is why the potion-ID packet widening transforms are a poor first port. Cap potion IDs at 127 and delete them; or use a new int field plus `@Overwrite` of the packet methods.
- **Client-only classes must not be in the common mixin list.** Put `EntityRenderer`, `RendererLivingEntity`, `RenderFish`, `ModelMagmaCube`, `ModelSkeleton`, `Minecraft`, `EntityPlayerSP`, `RealmsBridge` in `addClientMixins`; keep `World`, `Entity`, `Potion`, `MinecraftServer`, `NetHandlerPlayServer`, `CraftingManager`, `Block*` common.
- **Optional mod classes belong in late mixins.** Use `TargetMods` + `addRequiredMod`; never reference CoFH/Thaumcraft classes in an early common mixin. CoFH's `IEnergyHandler` is better handled with a separate optional integration class than a mixin.
- **Use `require = 1`.** A silent missing target is exactly how the current coremod became dead. A bytecode guard test should assert the target method/descriptor and annotation, as FuzziTweaks does.
- **Static final fields:** `Potion.potionTypes` and similar need `@Shadow @Final @Mutable`; `Minecraft.timer` is private but not final and only needs an accessor. Test in an obfuscated production jar, not only in dev.
- **`@Overwrite` is a last resort.** `findMatchingRecipe`, `processClickWindow`, `hasWorldObj`, `updateCameraAndRender`, and the packet read/write methods can all be expressed with `@Inject`/`@Modify*` first; an `@Overwrite` makes the mod incompatible with other mods touching the same method.

### 12.7 Test strategy for the migration

1. **Bytecode guard tests (headless).** Follow FuzziTweaks' `CoalGeneratorMixinTest`: read the compiled mixin class with ASM, find the handler method, assert the `@Inject`/`@Redirect`/`@ModifyVariable` annotation, target method, `require`, and the expected helper call. This catches the "mixin compiled but target moved" class of failure.
2. **Smoke test the Mixin loader.** Assert `Potion.potionTypes.length >= 128` after startup, and assert a tiny marker set by a smoke mixin. This would have caught the current missing coremod.
3. **In-game server tests.** Exercise each grouped feature: ethereal player filtering, redstone inlay, water-walk/ethereal water, shrink/flip, custom staff recipes, fishing-rod rendering, and `NetHandlerPlayServer` click handling. The existing `gameTestServer`/`gameTestClient` harness is suitable.
4. **Client tests under Xvfb.** Cover `EntityRenderer`, `RendererLivingEntity`, `RenderFish`, and `Minecraft.timer` paths; these are client-only and cannot be tested headless.
5. **Obfuscated production test.** Run the built jar (not only the dev workspace) with a production-like mod set; refmap/mapping failures often only appear there.
6. **Coverage.** Exclude mixin classes from the headless JaCoCo report (as FuzziTweaks does) and track their contract via the bytecode guard tests; otherwise the coverage number is dominated by untestable injected code.

### 12.8 Recommendation summary

- **Do** move the ASM transforms and `@Fix` patches to GTNH Mixins using the FuzziTweaks early/late loader + `Mixins` enum + `MixinBuilder` pattern.
- **Do** replace the targeted reflection with `@Accessor`/`@Invoker` interfaces; these are small and safe.
- **Do** remove the `GameData`, `RecipeUtilities`, `launchedVersion`, `Potion` array, `speedInAir`, `isUnblockable`, `timeSinceIgnited`, `smoothCam*`, `timer`, `TileEntity` map, and `Entity.fall` reflection.
- **Do not** turn the `EntityFishHook`/`EntityArrow` broad ATs into dozens of accessor calls unless removing ATs is a hard project goal; narrow the ATs first.
- **Do not** port `hasWorldObj`, `switchToRealms`, `getRenderType`, or the Ears `register` hack as-is; remove them unless a current bug requires them.
- **Do not** port `findMatchingRecipe`, `setDamage`, or `EntityItem.onUpdate` as global mixins if a normal `IRecipe`, `ItemSoulspike` override, or `ItemOre.onEntityItemUpdate` can do the job. Fewer mixins means fewer compatibility risks.
- **Cap potion IDs at 127** and delete the potion NBT/packet widening transforms; this is the single biggest simplification in the migration.
- **Add the Mixin loader check to CI.** The current coremod is inert and the test harness never noticed; the new mechanism must be observable.
- **Use `Loader.isModLoaded` for the optional Forestry/MagicBees integration; do not make them required.** Strip OptiFine compatibility now, with an optional gated Angelica/NotFine Mixin only if that need is confirmed later.


---

## Appendix A — Measurements

```text
Java files under src/main/java:      1053
Lines under src/main/java:          156397
Java files under src/test/java:     4
Java files under src/gameTest/java: 3
JUnit tests:                        19
Game-test assertions:               109 server + 23 client
JaCoCo line coverage:               177 / 69939 (0.25%)
JaCoCo branch coverage:             39 / 30318 (0.13%)
Coverage floor in addon.gradle.kts: 0.002 (0.2%)
catch (Throwable ...):              94
printStackTrace():                  84
TODO/FIXME:                         21
Commented-out lines (`//`):         3064
Raw List/Map declarations:          59
Static collections:                 73
@SideOnly annotations:              327
```

## Appendix B — Useful reproduction commands

```bash
# Unit tests + coverage
./gradlew test jacocoTestReport
# Full check (Spotless, Checkstyle, coverage floor, game-test compile)
./gradlew check
# Real dedicated server game tests
./gradlew gameTestServer
# Client game tests (needs a display / Xvfb)
./gradlew gameTestClient

# Inspect the emitted manifest (this is how the missing coremod was confirmed)
unzip -p build/libs/*.jar META-INF/MANIFEST.MF

# Check the server log for the coremod/ASM markers
grep -R "Core: Altering definition\|MysteriumPatches" run/server/logs run/client/logs
```

---

*This review intentionally does not change any source files; it is a report only.*
