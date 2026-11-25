package am2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemSnowball;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent;

import am2.api.power.IPowerNode;
import am2.api.power.PowerTypes;
import am2.api.spell.enums.Affinity;
import am2.api.spell.enums.BuffPowerLevel;
import am2.armor.ArmorHelper;
import am2.armor.infusions.GenericImbuement;
import am2.buffs.BuffList;
import am2.buffs.BuffStatModifiers;
import am2.damage.DamageSources;
import am2.items.ItemSoulspike;
import am2.mixins.early.EntityInvoker;
import am2.playerextensions.AffinityData;
import am2.playerextensions.ExtendedProperties;
import am2.playerextensions.SkillData;
import am2.playerextensions.effects.SpatialVortexEffect;
import am2.playerextensions.effects.TimedBlockEffect;
import am2.playerextensions.effects.TimedEntityEffect;
import am2.power.PowerNodeRegistry;
import am2.spell.SkillManager;
import am2.spell.SkillTreeManager;
import am2.utility.EntityUtilities;
import am2.utility.MathUtilities;
import am2.world.WorldStateManager;
import cofh.api.energy.IEnergyHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;

public class LivingUpdateHandler extends AMEventHandler {

    private static int tick = 0;

    void tickTimedEffects(EntityLivingBase ent, ExtendedProperties extendedProperties, World world) {
        if (extendedProperties.hasTimedEffects() && shouldTickAcceleratedBlocks(world)) {
            // accelerated blocks and entities are done outside the 5-tick performance optimisation to make them smooth
            for (Map.Entry<TimedBlockEffect, Integer> entry : new ArrayList<Map.Entry<TimedBlockEffect, Integer>>(
                extendedProperties.getAcceleratedTiles()
                    .entrySet())) {
                TimedBlockEffect key = entry.getKey();
                int remaining = entry.getValue();
                if (remaining < 3) {
                    extendedProperties.removeAcceleratedTile(key);
                } else {
                    extendedProperties.setAcceleratedTileDuration(key, remaining - 1);
                    World wrld = DimensionManager.getWorld(key.dimension);
                    if (wrld != null) {
                        for (int i = 0; i < key.power; i++) {
                            tickAcceleratedBlock(wrld, key.x, key.y, key.z);
                        }
                    }
                }
            }
            // Accelerated entities are processed once per server tick in onServerTick(), not from every
            // LivingUpdateEvent. Keeping the entity-list scan out of this hot path avoids the old L^2 behavior.
        }

        if (extendedProperties.hasTimedEffects() && shouldTickTimedEffects(world)) {
            tick++;
            if (tick > 100000) tick = 0;
            if ((tick % ((ent.worldObj.playerEntities.size() + 1) * 5)) == 0) { // only does behavior every 5 player
                                                                                // ticks to ease computing load
                // e.g. if there's 4 players, they tick like 1,2,3,4,1,2,3,4,1,2,3,4, etc (In theory, hopefully), and we
                // pick 25th (1st), 50th (2nd), 75th(3rd), etc. ticks.
                // there are some free ticks in-between, e.g. with 1 player it ticks every 10 ticks instead of 5. That
                // eases load further (but causes double the duration - not a big deal for these spells).

                if (enabled_accelerate) {
                    if (SkillTreeManager.instance.isSkillDisabled(SkillManager.instance.getSkill("ConcentrateTime"))) {
                        for (TimedBlockEffect effect : new ArrayList<TimedBlockEffect>(
                            extendedProperties.getAcceleratedTiles()
                                .keySet())) {
                            extendedProperties.removeAcceleratedTile(effect);
                        }
                        for (TimedEntityEffect effect : new ArrayList<TimedEntityEffect>(
                            extendedProperties.getAcceleratedEntities()
                                .keySet())) {
                            extendedProperties.removeAcceleratedEntity(effect);
                        }
                    }
                    Map<UUID, Integer> acceleratedEntitiesUUIDs = WorldStateManager.acceleratedEntities(world);
                    for (Map.Entry<TimedEntityEffect, Integer> entry : new ArrayList<Map.Entry<TimedEntityEffect, Integer>>(
                        extendedProperties.getAcceleratedEntities()
                            .entrySet())) {
                        TimedEntityEffect key = entry.getKey();
                        int remaining = entry.getValue();
                        if (remaining < 3) {
                            extendedProperties.removeAcceleratedEntity(key);
                            acceleratedEntitiesUUIDs.remove(key.entityUUID);
                        } else {
                            extendedProperties.setAcceleratedEntityDuration(key, remaining - 5);
                            if (acceleratedEntitiesUUIDs.get(key.entityUUID) == null) {
                                acceleratedEntitiesUUIDs.put(key.entityUUID, Integer.valueOf(key.power));
                            }
                        }
                    }
                }
                if (enabled_slow) {
                    if (SkillTreeManager.instance.isSkillDisabled(SkillManager.instance.getSkill("DiluteTime"))) {
                        extendedProperties.removeExtraVariablesContaining("accelerated_slow_entity_");
                        extendedProperties.removeExtraVariablesContaining("accelerated_slow_tile_");
                    }
                    Map<UUID, Integer> slowedEntitiesUUIDs = WorldStateManager.slowedEntities(world);
                    for (Map.Entry<TimedEntityEffect, Integer> entry : new ArrayList<Map.Entry<TimedEntityEffect, Integer>>(
                        extendedProperties.getSlowedEntities()
                            .entrySet())) {
                        TimedEntityEffect key = entry.getKey();
                        int remaining = entry.getValue();
                        if (remaining < 3) {
                            extendedProperties.removeSlowedEntity(key);
                            slowedEntitiesUUIDs.remove(key.entityUUID);
                        } else {
                            extendedProperties.setSlowedEntityDuration(key, remaining - 5);
                            if (slowedEntitiesUUIDs.get(key.entityUUID) == null) {
                                slowedEntitiesUUIDs.put(key.entityUUID, Integer.valueOf(key.power));
                            }
                        }
                    }

                    for (Map.Entry<TimedBlockEffect, Integer> entry : new ArrayList<Map.Entry<TimedBlockEffect, Integer>>(
                        extendedProperties.getSlowedTiles()
                            .entrySet())) {
                        TimedBlockEffect key = entry.getKey();
                        int remaining = entry.getValue();
                        if (remaining < 3) {
                            extendedProperties.removeSlowedTile(key);
                        } else {
                            extendedProperties.setSlowedTileDuration(key, remaining - 5);
                        }
                    }
                }

                if (enable_spatialVortex) {
                    Map<SpatialVortexEffect, String> spatialVortices = extendedProperties.getSpatialVortices();
                    if (!spatialVortices.isEmpty()) {
                        int[] totalenergy = new int[spatialVortices.size()];
                        int[] totalenergyExternallyLimited = new int[spatialVortices.size()];
                        int[] totaletheriumdark = new int[spatialVortices.size()];
                        int[] totaletheriumlight = new int[spatialVortices.size()];

                        int vIndex = 0;
                        for (Map.Entry<SpatialVortexEffect, String> entry : spatialVortices.entrySet()) {
                            SpatialVortexEffect key = entry.getKey();
                            if (key == null) continue;
                            int x = key.x;
                            int y = key.y;
                            int z = key.z;
                            World thisdim = DimensionManager.getWorld(key.dimension);
                            totalenergy[vIndex] = 0;
                            totaletheriumdark[vIndex] = 0;
                            totaletheriumlight[vIndex] = 0;
                            for (int xadd = -1; xadd <= 1; xadd += 2) {
                                for (int zadd = -1; zadd <= 1; zadd += 2) {
                                    TileEntity te = getTileEntitySafe(thisdim, x + xadd, y, z + zadd);
                                    if (te != null) {
                                        if (isEnergyHandler(te)) {
                                            totalenergy[vIndex] += ((IEnergyHandler) te)
                                                .getEnergyStored(ForgeDirection.UNKNOWN);
                                            totalenergyExternallyLimited[vIndex] += ((IEnergyHandler) te)
                                                .extractEnergy(ForgeDirection.UNKNOWN, 50000, true); // sim only
                                        }
                                        if (te instanceof IPowerNode) {
                                            totaletheriumdark[vIndex] += (int) PowerNodeRegistry.For(thisdim)
                                                .getPower((IPowerNode) te, PowerTypes.DARK);
                                            totaletheriumlight[vIndex] += (int) PowerNodeRegistry.For(thisdim)
                                                .getPower((IPowerNode) te, PowerTypes.LIGHT);
                                        }
                                    }
                                }
                            }
                            vIndex++;
                        }
                        if (AMCore.config.getDebugVortex()) {
                            System.out.println(Arrays.toString(totalenergy));
                            System.out.println(Arrays.toString(totalenergyExternallyLimited));
                            System.out.println(Arrays.toString(totaletheriumdark));
                            System.out.println(Arrays.toString(totaletheriumlight));
                        }
                        int[] maxE = getMaxIndex(totalenergy);
                        int[] maxD = getMaxIndex(totaletheriumdark);
                        int[] maxL = getMaxIndex(totaletheriumlight);
                        int[] minE = getMinIndex(totalenergy);
                        int[] minD = getMinIndex(totaletheriumdark);
                        int[] minL = getMinIndex(totaletheriumlight);

                        if (AMCore.config.getDebugVortex()) {
                            System.out.println("E" + Arrays.toString(minE));
                            System.out.println(Arrays.toString(maxE));
                            System.out.println("D" + Arrays.toString(minD));
                            System.out.println(Arrays.toString(maxD));
                            System.out.println("L" + Arrays.toString(minL));
                            System.out.println(Arrays.toString(maxL));
                        }

                        int halfDiffE = (maxE[1] - minE[1]) / 2;
                        int halfDiffD = (maxD[1] - minD[1]) / 2;
                        int halfDiffL = (maxL[1] - minL[1]) / 2;

                        if (AMCore.config.getDebugVortex()) {
                            System.out.println(halfDiffD + "," + halfDiffE + "," + halfDiffL + " half diff");
                        }

                        // total energy available to transfer, limited by: 50,000 per 5 ticks, half the diff between max
                        // and min, and external factors (such as device's throughput rate)
                        int toTransferE = Math.min(totalenergyExternallyLimited[maxE[0]], halfDiffE); // 50000 is the
                                                                                                      // max
                                                                                                      // externallyLimited
                                                                                                      // can be anyways
                        int toTransferD = Math.min(50000, halfDiffD);
                        int toTransferL = Math.min(50000, halfDiffL);

                        if (AMCore.config.getDebugVortex()) {
                            System.out.println(toTransferD + "," + toTransferE + "," + toTransferL + " to transfer");
                        }

                        int maximumEnergyMinimumVortexCanAccept = 0;
                        if (toTransferE > 0) {
                            // maximum energy we can *accept* right now (RF only)
                            int tIndex = 0;
                            for (Map.Entry<SpatialVortexEffect, String> entry : spatialVortices.entrySet()) {
                                if (AMCore.config.getDebugVortex()) {
                                    System.out.println(minE[0]);
                                }
                                if (tIndex == minE[0]) {
                                    SpatialVortexEffect key = entry.getKey();
                                    if (key == null) continue;
                                    int x = key.x;
                                    int y = key.y;
                                    int z = key.z;
                                    World thisdim = DimensionManager.getWorld(key.dimension);
                                    for (int xadd = -1; xadd <= 1; xadd += 2) {
                                        for (int zadd = -1; zadd <= 1; zadd += 2) {
                                            TileEntity te = getTileEntitySafe(thisdim, x + xadd, y, z + zadd);
                                            if (te != null) {
                                                if (isEnergyHandler(te)) {
                                                    if (AMCore.config.getDebugVortex()) {
                                                        System.out.println("got here!");
                                                    }
                                                    maximumEnergyMinimumVortexCanAccept += ((IEnergyHandler) te)
                                                        .receiveEnergy(ForgeDirection.UNKNOWN, 50000, true); // sim only
                                                }
                                            }
                                        }
                                    }
                                    break;
                                }
                                tIndex++;
                            }
                        }

                        if (AMCore.config.getDebugVortex()) {
                            System.out.println(maximumEnergyMinimumVortexCanAccept + " max can accept");
                        }

                        int toTransferEActual = Math.min(toTransferE, maximumEnergyMinimumVortexCanAccept);
                        boolean ELeft = toTransferEActual > 0;
                        boolean LLeft = toTransferL > 0;
                        boolean DLeft = toTransferD > 0;

                        int localIndex = 0;
                        // energy subtraction. BEWARE: Terrible code to follow!! Prepare bleach for eyes.
                        if (ELeft) {
                            int toSubtractE = toTransferEActual;
                            for (Map.Entry<SpatialVortexEffect, String> entry : spatialVortices.entrySet()) {
                                if (localIndex == maxE[0]) {
                                    SpatialVortexEffect key = entry.getKey();
                                    if (key == null) continue;
                                    int x = key.x;
                                    int y = key.y;
                                    int z = key.z;
                                    World thisdim = DimensionManager.getWorld(key.dimension);
                                    for (int xadd = -1; xadd <= 1; xadd += 2) {
                                        for (int zadd = -1; zadd <= 1; zadd += 2) {
                                            TileEntity te = getTileEntitySafe(thisdim, x + xadd, y, z + zadd);
                                            if (te != null) {
                                                if (isEnergyHandler(te)) {
                                                    if (toSubtractE > 0) toSubtractE -= ((IEnergyHandler) te)
                                                        .extractEnergy(ForgeDirection.UNKNOWN, toSubtractE, false); // real
                                                }
                                            }
                                        }
                                    }
                                }
                                localIndex++;
                            }
                            localIndex = 0;
                        }
                        if (DLeft) {
                            int toSubtractD = toTransferD;
                            for (Map.Entry<SpatialVortexEffect, String> entry : spatialVortices.entrySet()) {
                                if (localIndex == maxD[0]) {
                                    SpatialVortexEffect key = entry.getKey();
                                    if (key == null) continue;
                                    int x = key.x;
                                    int y = key.y;
                                    int z = key.z;
                                    World thisdim = DimensionManager.getWorld(key.dimension);
                                    for (int xadd = -1; xadd <= 1; xadd += 2) {
                                        for (int zadd = -1; zadd <= 1; zadd += 2) {
                                            TileEntity te = getTileEntitySafe(thisdim, x + xadd, y, z + zadd);
                                            if (te != null) {
                                                if (te instanceof IPowerNode) {
                                                    if (toSubtractD > 0) toSubtractD -= (int) PowerNodeRegistry
                                                        .For(thisdim)
                                                        .consumePower((IPowerNode) te, PowerTypes.DARK, toSubtractD); // real
                                                }
                                            }
                                        }
                                    }
                                }
                                localIndex++;
                            }
                            localIndex = 0;
                        }
                        if (LLeft) {
                            int toSubtractL = toTransferL;
                            for (Map.Entry<SpatialVortexEffect, String> entry : spatialVortices.entrySet()) {
                                if (localIndex == maxL[0]) {
                                    SpatialVortexEffect key = entry.getKey();
                                    if (key == null) continue;
                                    int x = key.x;
                                    int y = key.y;
                                    int z = key.z;
                                    World thisdim = DimensionManager.getWorld(key.dimension);
                                    for (int xadd = -1; xadd <= 1; xadd += 2) {
                                        for (int zadd = -1; zadd <= 1; zadd += 2) {
                                            TileEntity te = getTileEntitySafe(thisdim, x + xadd, y, z + zadd);
                                            if (te != null) {
                                                if (te instanceof IPowerNode) {
                                                    if (toSubtractL > 0) toSubtractL -= (int) PowerNodeRegistry
                                                        .For(thisdim)
                                                        .consumePower((IPowerNode) te, PowerTypes.LIGHT, toSubtractL); // real
                                                }
                                            }
                                        }
                                    }
                                }
                                localIndex++;
                            }
                        }

                        if (ELeft || DLeft || LLeft) {
                            // actually do the energy transfer. Finally. Note: I *know* this whole algo I came up with
                            // is badly optimized, and I welcome any PRs to optimize it.
                            int fIndex = 0;
                            for (Map.Entry<SpatialVortexEffect, String> entry : spatialVortices.entrySet()) {
                                if (fIndex == minE[0] && ELeft) {
                                    SpatialVortexEffect key = entry.getKey();
                                    if (key == null) continue;
                                    int x = key.x;
                                    int y = key.y;
                                    int z = key.z;
                                    World thisdim = DimensionManager.getWorld(key.dimension);
                                    for (int xadd = -1; xadd <= 1; xadd += 2) {
                                        for (int zadd = -1; zadd <= 1; zadd += 2) {
                                            TileEntity te = getTileEntitySafe(thisdim, x + xadd, y, z + zadd);
                                            if (te != null) {
                                                if (isEnergyHandler(te)) {
                                                    if (toTransferEActual > 0)
                                                        toTransferEActual -= ((IEnergyHandler) te).receiveEnergy(
                                                            ForgeDirection.UNKNOWN,
                                                            toTransferEActual,
                                                            false); // real
                                                    else ELeft = false;
                                                }
                                            }
                                        }
                                    }
                                }
                                if (fIndex == minD[0] && DLeft) {
                                    if (AMCore.config.getDebugVortex()) {
                                        System.out.println(fIndex + " fIndex when minD");
                                    }
                                    SpatialVortexEffect key = entry.getKey();
                                    if (key == null) continue;
                                    int x = key.x;
                                    int y = key.y;
                                    int z = key.z;
                                    World thisdim = DimensionManager.getWorld(key.dimension);
                                    for (int xadd = -1; xadd <= 1; xadd += 2) {
                                        for (int zadd = -1; zadd <= 1; zadd += 2) {
                                            TileEntity te = getTileEntitySafe(thisdim, x + xadd, y, z + zadd);
                                            if (te != null) {
                                                if (te instanceof IPowerNode) {
                                                    if (toTransferD > 0)
                                                        toTransferD -= (int) PowerNodeRegistry.For(thisdim)
                                                            .insertPower((IPowerNode) te, PowerTypes.DARK, toTransferD);
                                                    else DLeft = false;
                                                }
                                            }
                                        }
                                    }
                                }
                                if (fIndex == minL[0] && LLeft) {
                                    if (AMCore.config.getDebugVortex()) {
                                        System.out.println(fIndex + " fIndex when minL");
                                    }
                                    SpatialVortexEffect key = entry.getKey();
                                    if (key == null) continue;
                                    int x = key.x;
                                    int y = key.y;
                                    int z = key.z;
                                    World thisdim = DimensionManager.getWorld(key.dimension);
                                    for (int xadd = -1; xadd <= 1; xadd += 2) {
                                        for (int zadd = -1; zadd <= 1; zadd += 2) {
                                            TileEntity te = getTileEntitySafe(thisdim, x + xadd, y, z + zadd);
                                            if (te != null) {
                                                if (te instanceof IPowerNode) {
                                                    if (toTransferL > 0) toTransferL -= (int) PowerNodeRegistry
                                                        .For(thisdim)
                                                        .insertPower((IPowerNode) te, PowerTypes.LIGHT, toTransferL);
                                                    else LLeft = false;
                                                }
                                            }
                                        }
                                    }
                                }
                                fIndex++;
                            }
                        }
                    }
                }

                if (enabled_timeFortified) {
                    for (Map.Entry<TimedBlockEffect, Integer> entry : new ArrayList<Map.Entry<TimedBlockEffect, Integer>>(
                        extendedProperties.getFortifiedTiles()
                            .entrySet())) {
                        TimedBlockEffect key = entry.getKey();
                        int remaining = entry.getValue();
                        if (remaining < 3) {
                            extendedProperties.removeFortifiedTile(key);
                            if (!world.isRemote) {
                                releaseFortifiedChunk(DimensionManager.getWorld(key.dimension), key.x, key.y, key.z);
                            }
                        } else {
                            extendedProperties.setFortifiedTileDuration(key, remaining - 5);
                        }
                    }
                }
                if (enabled_shield) {
                    for (Map.Entry<TimedBlockEffect, Integer> entry : new ArrayList<Map.Entry<TimedBlockEffect, Integer>>(
                        extendedProperties.getShieldedTiles()
                            .entrySet())) {
                        TimedBlockEffect key = entry.getKey();
                        int remaining = entry.getValue();
                        if (remaining < 3) {
                            extendedProperties.removeShieldedTile(key);
                            if (!world.isRemote) {
                                java.util.Set<String> shielded = am2.world.WorldStateManager
                                    .forceShielded(DimensionManager.getWorld(key.dimension));
                                shielded.remove(key.representation());
                            }
                        } else {
                            String represent = key.representation();
                            java.util.Set<String> shielded = am2.world.WorldStateManager
                                .forceShielded(DimensionManager.getWorld(key.dimension));
                            if (!(shielded.contains(represent))) shielded.add(represent);
                            extendedProperties.setShieldedTileDuration(key, remaining - 5);
                        }
                    }
                }
            }
        }

    }

    void tickFlippedState(EntityLivingBase ent, ExtendedProperties extendedProperties, World world) {
        // unflip flipped players
        if (tempFlipped.containsKey(extendedProperties)) {
            tempFlipped.put(extendedProperties, tempFlipped.get(extendedProperties) - 1);
            if (tempFlipped.get(extendedProperties) <= 0) {
                tempFlipped.remove(extendedProperties);
                extendedProperties.toggleFlipped();
            }
        }

        extendedProperties.flipTick();

        if (extendedProperties.getIsFlipped()) {
            if ((ent).motionY < 2) (ent).motionY += 0.15f;

            double posY = ent.posY + ent.height;
            if (!world.isRemote) posY += ent.getEyeHeight();
            if (world.rayTraceBlocks(
                Vec3.createVectorHelper(ent.posX, posY, ent.posZ),
                Vec3.createVectorHelper(ent.posX, posY + 1, ent.posZ),
                true) != null) {
                if (!ent.onGround) {
                    if (ent.fallDistance > 0) {
                        ((EntityInvoker) ent).am2$invokeFall(ent.fallDistance);
                        ent.fallDistance = 0;
                    }
                }
                ent.onGround = true;
            } else {
                if (ent instanceof EntityPlayer) {
                    if (ent.motionY > 0) {
                        if (world.isRemote) ent.fallDistance += (float) (ent.posY - ent.prevPosY);
                        else ent.fallDistance += (float) ((((EntityPlayer) ent).field_71095_bQ
                            - ((EntityPlayer) ent).field_71096_bN) * 2);
                    }
                }
                ent.onGround = false;
            }
        }

    }

    void tickMovementBuffs(EntityLivingBase ent, ExtendedProperties extendedProperties) {
        // leap buff
        if (ent.isPotionActive(BuffList.leap)) {
            int amplifier = ent.getActivePotionEffect(BuffList.leap)
                .getAmplifier();

            switch (amplifier) {
                case BuffPowerLevel.Low:
                    extendedProperties.setFallProtection(8);
                    break;
                case BuffPowerLevel.Medium:
                    extendedProperties.setFallProtection(20);
                    break;
                case BuffPowerLevel.High:
                    extendedProperties.setFallProtection(45);
                    break;
                default:
                    break;
            }
        }

        if (ent.isPotionActive(BuffList.gravityWell)) {
            if (ent.motionY < 0 && ent.motionY > -3f) {
                ent.motionY *= 1.59999999999999998D;
            }
        }

        // slowfall/shrink buff
        // (isSneaking calls DataWatcher which are slow, so we test it late)
        if (ent.isPotionActive(BuffList.slowfall) || ent.isPotionActive(BuffList.shrink)
            || (ent instanceof EntityPlayer && AffinityData.For(ent)
                .getAffinityDepth(Affinity.NATURE) == 1.0f && !ent.isSneaking())) {
            if (!ent.onGround && ent.motionY < 0.0D) {
                ent.motionY *= 0.79999999999999998D;
            }
        }

        // swift swim
        if (ent.isPotionActive(BuffList.swiftSwim)) {
            if (ent.isInWater()) {
                if (!(ent instanceof EntityPlayer) || !((EntityPlayer) ent).capabilities.isFlying) {
                    ent.motionX *= (1.133f + 0.03 * ent.getActivePotionEffect(BuffList.swiftSwim)
                        .getAmplifier());
                    ent.motionZ *= (1.133f + 0.03 * ent.getActivePotionEffect(BuffList.swiftSwim)
                        .getAmplifier());

                    if (ent.motionY > 0) {
                        ent.motionY *= 1.134;
                    }
                }
            }
        }

        // watery grave
        if (ent.isPotionActive(BuffList.wateryGrave)) {
            if (ent.isInWater()) {
                double pullVel = -0.5f;
                pullVel *= (ent.getActivePotionEffect(BuffList.wateryGrave)
                    .getAmplifier() + 1);
                if (ent.motionY > pullVel) ent.motionY -= 0.1;
            }
        }

    }

    void tickEtherealForm(EntityPlayer player, ExtendedProperties extendedProperties) {
        int durationLeft = Integer.parseInt(extendedProperties.getExtraVariable("ethereal"));
        if (durationLeft < 3) {
            // deactivate form
            ItemSoulspike.removeTagFromBoots(player.inventory.armorInventory[0]);
            player.capabilities.disableDamage = false;
            player.capabilities.allowEdit = true;
            player.capabilities.isFlying = false;
            player.noClip = false;
            player.setInvisible(false);
            extendedProperties.removeFromExtraVariables("ethereal");
        } else {
            // tick ethereal form
            // decrease duration
            extendedProperties.addToExtraVariables("ethereal", String.valueOf(durationLeft - 1));
        }
    }

    void tickPlayerFlight(EntityPlayer player, ExtendedProperties extendedProperties) {
        if (player.isPotionActive(BuffList.flight.id) || player.isPotionActive(BuffList.levitation.id)
            || player.capabilities.isCreativeMode) {
            extendedProperties.hadFlight = true;
            if (player.isPotionActive(BuffList.levitation)) {
                if (player.capabilities.isFlying) {
                    float factor = 0.4f;
                    player.motionX *= factor;
                    player.motionZ *= factor;
                    player.motionY *= 0.0001f;
                }
            }
        } else if (extendedProperties.hadFlight) {
            player.capabilities.allowFlying = false;
            player.capabilities.isFlying = false;
            extendedProperties.hadFlight = false;
        }
    }

    void tickContingenciesAndAura(EntityLivingBase ent, ExtendedProperties extendedProperties, World world) {
        if (!ent.onGround && ent.fallDistance >= 4f
            && !(extendedProperties.getContingencyEffect(2)
                .getItem() instanceof ItemSnowball)) {
            int distanceToGround = MathUtilities.getDistanceToGround(ent, world);
            if (distanceToGround < -8 * ent.motionY) {
                extendedProperties.procContingency(2, null);
            }
        }
        if (!(extendedProperties.getContingencyEffect(3)
            .getItem() instanceof ItemSnowball) && ent.isBurning()) {
            extendedProperties.procContingency(3, null);
        }

        if (!ent.worldObj.isRemote && ent.ticksExisted % 200 == 0) {
            extendedProperties.setSyncAuras();
        }

    }

    void tickManaLinksAndLocalMovement(EntityLivingBase ent, ExtendedProperties extendedProperties, World world) {
        // mana link pfx
        if (ent.worldObj.isRemote) extendedProperties.spawnManaLinkParticles();

        if (ent.ticksExisted % 20 == 0) extendedProperties.cleanupManaLinks();

        if (world.isRemote) {
            AMCore.proxy.sendLocalMovementData(ent);
        }
    }

    void tickHallucinations(EntityPlayer player) {
        if (player.worldObj.isRemote) return;
        // hallucination effects
        if (player.isPotionActive(BuffList.psychedelic)) {
            if (player.worldObj.provider.dimensionId == -1) { // any amplifier can cause lower level hallucinations
                if (player.getActivePotionEffect(BuffList.psychedelic)
                    .getDuration() > 10) lowerHallucinationTick(player);
                else cleanupLowerHallucination(player);
            } else if (player.worldObj.provider.dimensionId == 1 && player.getActivePotionEffect(BuffList.psychedelic)
                .getAmplifier() == 1) { // only amplified for higher
                    if (player.getActivePotionEffect(BuffList.psychedelic)
                        .getDuration() > 10) higherHallucinationTick(player);
                    else cleanupHigherHallucination(player);
                }
        }
    }

    boolean shouldCancelSlowedEntity(EntityLivingBase ent, World world) {
        Map<UUID, Integer> slowedEntitiesUUIDs = WorldStateManager.slowedEntities(world);
        if (slowedEntitiesUUIDs.isEmpty()) return false;

        Integer updateInterval = slowedEntitiesUUIDs.get(ent.getUniqueID());
        return updateInterval != null
            && !SkillTreeManager.instance.isSkillDisabled(SkillManager.instance.getSkill("DiluteTime"))
            && updateInterval > 0
            && ent.ticksExisted % updateInterval != 0;
    }

    void tickPlayerArmor(EntityPlayer player, ExtendedProperties extendedProperties, World world) {
        if (player.worldObj.isRemote) {
            int divisor = extendedProperties.getAuraDelay() > 0 ? extendedProperties.getAuraDelay() : 1;
            if (player.ticksExisted % divisor == 0) AMCore.proxy.particleManager.spawnAuraParticles(player);
            AMCore.proxy.setViewSettings();
        }

        ArmorHelper.HandleArmorInfusion(player);
        ArmorHelper.HandleArmorEffects(player, world);

        if (ArmorHelper.isInfusionPreset(player.getCurrentArmor(1), GenericImbuement.stepAssist)) {
            player.stepHeight = 1.0111f;
        } else if (player.stepHeight == 1.0111f) {
            player.stepHeight = 0.5f;
        }

        IAttributeInstance attr = player.getEntityAttribute(SharedMonsterAttributes.movementSpeed);
        if (ArmorHelper.isInfusionPreset(player.getCurrentArmor(0), GenericImbuement.runSpeed)) {
            if (attr.getModifier(GenericImbuement.imbuedHasteID) == null) {
                attr.applyModifier(GenericImbuement.imbuedHaste);
            }
        } else {
            if (attr.getModifier(GenericImbuement.imbuedHasteID) != null) {
                attr.removeModifier(GenericImbuement.imbuedHaste);
            }
        }
    }

    void tickSummon(EntityLivingBase ent) {
        if (!ent.worldObj.isRemote && EntityUtilities.isSummon(ent) && !EntityUtilities.isTileSpawnedAndValid(ent)) {
            int owner = EntityUtilities.getOwner(ent);
            Entity ownerEnt = ent.worldObj.getEntityByID(owner);
            if (!EntityUtilities.decrementSummonDuration(ent)) {
                ent.attackEntityFrom(DamageSources.unsummon, 5000);
            }
            if (owner == -1 || ownerEnt == null || ownerEnt.isDead || ownerEnt.getDistanceSqToEntity(ent) > 900) {
                if (ent instanceof EntityLiving && !((EntityLiving) ent).getCustomNameTag()
                    .isEmpty()) {
                    EntityUtilities.setOwner(ent, null);
                    EntityUtilities.setSummonDuration(ent, -1);
                    EntityUtilities.revertAI((EntityCreature) ent);
                } else {
                    ent.attackEntityFrom(DamageSources.unsummon, 5000);
                }
            }
        }

    }

    void tickPropertySync(EntityLivingBase ent, ExtendedProperties extendedProperties) {
        // data sync
        extendedProperties.handleExtendedPropertySync();

        if (ent instanceof EntityPlayer) {
            AffinityData.For(ent)
                .handleExtendedPropertySync();
            SkillData.For((EntityPlayer) ent)
                .handleExtendedPropertySync();
        }
    }

    ExtendedProperties prepareEntityProperties(EntityLivingBase ent) {
        BuffStatModifiers.instance.applyStatModifiersBasedOnBuffs(ent);

        ExtendedProperties extendedProperties = ExtendedProperties.For(ent);
        extendedProperties.handleSpecialSyncData();
        extendedProperties.manaBurnoutTick();
        return extendedProperties;
    }

    void tickAgility(EntityLivingBase ent) {
        if (ent.isPotionActive(BuffList.agility.id)) {
            ent.stepHeight = 1.01f;
        } else if (ent.stepHeight == 1.01f) {
            ent.stepHeight = 0.5f;
        }
    }

    @SubscribeEvent
    public void onEntityLiving(LivingUpdateEvent event) {

        EntityLivingBase ent = event.entityLiving;
        World world = ent.worldObj;

        if (shouldCancelSlowedEntity(ent, world)) {
            event.setCanceled(true);
            return;
        }

        ExtendedProperties extendedProperties = prepareEntityProperties(ent);

        if (ent instanceof EntityPlayer && extendedProperties.hasExtraVariable("ethereal")) {
            tickEtherealForm((EntityPlayer) ent, extendedProperties);
        }

        tickTimedEffects(ent, extendedProperties, world);

        // ================================================================================

        tickFlippedState(ent, extendedProperties, world);

        if (ent instanceof EntityPlayer) {
            tickPlayerArmor((EntityPlayer) ent, extendedProperties, world);
        }

        tickContingenciesAndAura(ent, extendedProperties, world);

        // buff particles
        // if (ent.worldObj.isRemote)
        // AMCore.instance.proxy.particleManager.spawnBuffParticles(ent);

        tickPropertySync(ent, extendedProperties);

        if (ent instanceof EntityPlayer) {
            tickPlayerFlight((EntityPlayer) ent, extendedProperties);
        }

        tickAgility(ent);

        tickSummon(ent);

        tickMovementBuffs(ent, extendedProperties);

        if (ent instanceof EntityPlayer) {
            tickHallucinations((EntityPlayer) ent);
        }

        tickManaLinksAndLocalMovement(ent, extendedProperties, world);
    }

}
