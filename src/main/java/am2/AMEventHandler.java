package am2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import net.minecraft.block.Block;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent;

import am2.buffs.BuffEffectScrambleSynapses;
import am2.buffs.BuffList;
import am2.entities.EntityHallucination;
import am2.entities.EntitySpecificHallucinations;
import am2.items.ItemOre;
import am2.items.ItemsCommonProxy;
import am2.playerextensions.ExtendedProperties;
import am2.playerextensions.effects.SpatialVortexEffect;
import am2.playerextensions.effects.TimedBlockEffect;
import am2.playerextensions.effects.TimedEffectCodec;
import am2.playerextensions.effects.TimedEntityEffect;
import cofh.api.energy.IEnergyHandler;

public class AMEventHandler {

    static boolean enabled_accelerate = true;
    static boolean enabled_slow = true;
    static boolean enabled_timeFortified = true;
    static boolean enabled_shield = true;
    static boolean enable_spatialVortex = true;

    private static final Random rand = new Random();
    static final Map<ExtendedProperties, Integer> tempFlipped = new HashMap<ExtendedProperties, Integer>();

    public static void setTempFlipped(ExtendedProperties properties, int ticks) {
        tempFlipped.put(properties, ticks);
    }

    public static Map<UUID, Integer> acceleratedEntitiesUUIDs = new HashMap<>();
    public static Map<UUID, Integer> slowedEntitiesUUIDs = new HashMap<>();

    /** Null-safe tile lookup for the spatial-vortex bookkeeping; an unloaded dimension yields a null world. */
    static TileEntity getTileEntitySafe(World world, int x, int y, int z) {
        return world == null ? null : world.getTileEntity(x, y, z);
    }

    /** CoFH is compileOnly; short-circuit before resolving IEnergyHandler when the mod is absent. */
    static boolean isEnergyHandler(Object te) {
        return AMCore.cofh && te instanceof IEnergyHandler;
    }

    /** Releases a Fortify Time chunk ticket only when the world and tile entity still exist. */
    static void releaseFortifiedChunk(World world, int x, int y, int z) {
        if (world == null) {
            return;
        }
        TileEntity te = world.getTileEntity(x, y, z);
        if (te == null) {
            return;
        }
        AMChunkLoader.INSTANCE.releaseStaticChunkLoad(te.getClass(), x, y, z, world);
    }

    /**
     * Apply extra entity ticks once per server tick.
     *
     * <p>
     * This used to run from {@link LivingUpdateEvent}, which meant that every living entity caused a complete
     * scan of every loaded entity. Apart from doing the same work L times per tick, that made the cost quadratic in
     * the number of loaded living entities. UUIDs are used as map keys so the normal update path never has to
     * allocate the string representation of an entity UUID.
     * </p>
     */
    private static UUID parseUUID(String value) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    static int[] getMinIndex(int[] array) {
        int min = array[0];
        int indexForMin = 0;
        for (int i = 0; i < array.length; i++) {
            int score = array[i];
            if (min > score) {
                min = score;
                indexForMin = i;
            }
        }
        return new int[] { indexForMin, min };
    }

    static int[] getMaxIndex(int[] array) {
        int max = array[0];
        int indexForMax = 0;
        for (int i = 0; i < array.length; i++) {
            int score = array[i];
            if (max < score) {
                max = score;
                indexForMax = i;
            }
        }
        return new int[] { indexForMax, max };
    }

    /**
     * Timed block/entity bookkeeping mutates server worlds and reads server-side dimensions. Clients only render the
     * synced result, so skip the parsing and world lookups entirely there.
     */
    public static boolean isAccelerateEnabled() {
        return enabled_accelerate;
    }

    static boolean shouldTickAcceleratedBlocks(World world) {
        return world != null && !world.isRemote && enabled_accelerate;
    }

    static boolean shouldTickTimedEffects(World world) {
        return world != null && !world.isRemote
            && (enabled_accelerate || enabled_slow || enable_spatialVortex || enabled_timeFortified || enabled_shield);
    }

    static final int MAX_ACCELERATED_TILE_KEYS = 256;

    private static final class BoundedKeyCache<V> extends LinkedHashMap<String, V> {

        private static final long serialVersionUID = 1L;

        private BoundedKeyCache() {
            super(64, 0.75F, true);
        }

        @Override
        protected boolean removeEldestEntry(Map.Entry<String, V> eldest) {
            return size() > MAX_ACCELERATED_TILE_KEYS;
        }
    }

    private static final BoundedKeyCache<TimedBlockEffect> acceleratedTileKeys = new BoundedKeyCache<TimedBlockEffect>();
    private static final BoundedKeyCache<TimedEntityEffect> acceleratedEntityKeys = new BoundedKeyCache<TimedEntityEffect>();
    private static final BoundedKeyCache<SpatialVortexEffect> spatialVortexKeys = new BoundedKeyCache<SpatialVortexEffect>();
    private static final BoundedKeyCache<TimedBlockEffect> timedBlockKeys = new BoundedKeyCache<TimedBlockEffect>();

    static TimedBlockEffect acceleratedTileKey(String raw) {
        TimedBlockEffect cached = acceleratedTileKeys.get(raw);
        if (cached != null) return cached;

        TimedBlockEffect parsed = TimedEffectCodec.parseAcceleratedTile(raw);
        if (parsed != null) {
            acceleratedTileKeys.put(raw, parsed);
        }
        return parsed;
    }

    static TimedEntityEffect acceleratedEntityKey(String raw) {
        TimedEntityEffect cached = acceleratedEntityKeys.get(raw);
        if (cached != null) return cached;

        TimedEntityEffect parsed = TimedEffectCodec.parseEntity(raw);
        if (parsed != null) {
            acceleratedEntityKeys.put(raw, parsed);
        }
        return parsed;
    }

    static SpatialVortexEffect spatialVortexKey(String raw) {
        SpatialVortexEffect cached = spatialVortexKeys.get(raw);
        if (cached != null) return cached;

        SpatialVortexEffect parsed = TimedEffectCodec.parseSpatialVortex(raw);
        if (parsed != null) {
            spatialVortexKeys.put(raw, parsed);
        }
        return parsed;
    }

    static TimedBlockEffect timedBlockKey(String raw) {
        TimedBlockEffect cached = timedBlockKeys.get(raw);
        if (cached != null) return cached;

        TimedBlockEffect parsed = TimedEffectCodec.parseTimedBlock(raw);
        if (parsed != null) {
            timedBlockKeys.put(raw, parsed);
        }
        return parsed;
    }

    static int acceleratedTileKeyCacheSize() {
        return acceleratedTileKeys.size();
    }

    static void clearAcceleratedTileKeyCache() {
        acceleratedTileKeys.clear();
    }

    static void clearParsedEffectKeyCaches() {
        acceleratedTileKeys.clear();
        acceleratedEntityKeys.clear();
        spatialVortexKeys.clear();
        timedBlockKeys.clear();
    }

    /** Runs one accelerated update for a block position, reusing the tile/block lookups for all requested ticks. */
    static void tickAcceleratedBlock(World world, int x, int y, int z) {
        TileEntity tile = world.getTileEntity(x, y, z);
        if (tile != null && tile.canUpdate()) {
            tile.updateEntity();
        }

        Block block = world.getBlock(x, y, z);
        if (block != null && block.getTickRandomly() && world.rand.nextInt(100) == 0) {
            block.updateTick(world, x, y, z, world.rand);
        }
    }

    public static Map<EntityPlayer, ArrayList<EntityCreature>> hallucinationMap = new HashMap<EntityPlayer, ArrayList<EntityCreature>>();
    public static Map<EntityPlayer, ArrayList<EntityItem>> dustMap = new HashMap<EntityPlayer, ArrayList<EntityItem>>();
    public static Map<EntityCreature, Integer> tempCurseMap = new HashMap<EntityCreature, Integer>();

    /** Drops world-bound transient state when a dimension unloads. */
    public static void clearWorldState(World world) {
        hallucinationMap.entrySet()
            .removeIf(entry -> entry.getKey() == null || entry.getKey().worldObj == world);
        dustMap.entrySet()
            .removeIf(entry -> entry.getKey() == null || entry.getKey().worldObj == world);
        tempCurseMap.entrySet()
            .removeIf(entry -> entry.getKey() == null || entry.getKey().worldObj == world);
        am2.world.WorldStateManager.unload(world);
    }

    /** Clears all transient state on server stop or client disconnect. */
    public static void clearAllTransientState() {
        hallucinationMap.clear();
        dustMap.clear();
        tempCurseMap.clear();
        am2.world.WorldStateManager.clearAll();
        acceleratedEntitiesUUIDs.clear();
        slowedEntitiesUUIDs.clear();
        tempFlipped.clear();
    }

    void cleanupLowerHallucination(EntityPlayer player) {
        player.removePotionEffect(BuffList.scrambleSynapses.id);
        player.extinguish();
        player.removePotionEffect(Potion.confusion.id);
        if (hallucinationMap.get(player) != null) {
            for (EntityCreature creature : hallucinationMap.get(player)) {
                if (creature != null) creature.setDead();
            }
            hallucinationMap.get(player)
                .clear();
        }
        if (dustMap.get(player) != null) {
            for (EntityItem item : dustMap.get(player)) {
                if (item != null) item.setDead();
            }
            dustMap.get(player)
                .clear();
        }
    }

    void cleanupHigherHallucination(EntityPlayer player) {
        cleanupLowerHallucination(player);
        player.curePotionEffects(new ItemStack(Items.milk_bucket));
    }

    void lowerHallucinationTick(EntityPlayer player) {
        if (player.worldObj.rand.nextInt(103) == 0) { // scramble
            player.addPotionEffect(new BuffEffectScrambleSynapses(50, 0));
        }
        if (player.worldObj.rand.nextInt(75) == 0) { // spontaneous combustion
            player.setFire(7);
        }
        if (player.worldObj.rand.nextInt(50) == 0) { // nausea
            player.addPotionEffect(new PotionEffect(Potion.confusion.id, 100, 0));
        }

        int x = MathHelper.floor_double(player.posX); // creatures
        int y = MathHelper.floor_double(player.posY);
        int z = MathHelper.floor_double(player.posZ);
        if (player.worldObj.rand.nextInt(550) == 0) {
            Class<? extends EntityHallucination> halclass = null;
            switch (player.worldObj.rand.nextInt(5)) {
                case 1:
                    halclass = EntitySpecificHallucinations.EntityHallucinationZombie.class;
                    break;
                case 2:
                    halclass = EntitySpecificHallucinations.EntityHallucinationSpider.class;
                    break;
                case 3:
                    halclass = EntitySpecificHallucinations.EntityHallucinationWitherSkeleton.class;
                    break;
                case 4:
                    halclass = EntitySpecificHallucinations.EntityHallucinationMagmacube.class;
                    break;
                default:
                    halclass = EntitySpecificHallucinations.EntityHallucinationCreeper.class;
                    break;
            }

            EntityCreature summoned = summonCreature(player.worldObj, halclass, x, y, z, player, 4, 9);
            hallucinationMap.computeIfAbsent(player, k -> new ArrayList<>());
            hallucinationMap.get(player)
                .add(summoned);
        }
        if (player.worldObj.rand.nextInt(100) == 0) { // dust
            EntityItem item = new EntityItem(player.worldObj);
            item.setPosition(
                (x - 15) + player.worldObj.rand.nextInt(16),
                y + player.worldObj.rand.nextInt(6),
                (z - 15) + player.worldObj.rand.nextInt(16));
            item.setEntityItemStack(new ItemStack(ItemsCommonProxy.itemOre, 1, ItemOre.META_COGNITIVEDUST));
            player.worldObj.spawnEntityInWorld(item);
            dustMap.computeIfAbsent(player, k -> new ArrayList<EntityItem>());
            dustMap.get(player)
                .add(item);
        }
    }

    void higherHallucinationTick(EntityPlayer player) {
        lowerHallucinationTick(player); // all the effects of the lower... and more!
        if (player.worldObj.rand.nextInt(75) == 0) { // random effects
            player.addPotionEffect(new PotionEffect(player.worldObj.rand.nextInt(20) + 1, 100, 0));
        }
        int x = MathHelper.floor_double(player.posX);
        int y = MathHelper.floor_double(player.posY);
        int z = MathHelper.floor_double(player.posZ);
        if (player.worldObj.rand.nextInt(80) == 0) { // random noises
            String soundString = null;
            switch (player.worldObj.rand.nextInt(8)) {
                case 1:
                    soundString = "mob.skeleton.say";
                    break;
                case 2:
                    soundString = "game.tnt.primed";
                    break;
                case 3:
                    soundString = "random.bow";
                    break;
                case 4:
                    soundString = "random.chestopen";
                    break;
                case 5:
                    soundString = "random.chestclosed";
                    break;
                case 6:
                    soundString = "random.door_open";
                    break;
                case 7:
                    soundString = "random.fuse";
                    break;
                default:
                    soundString = "mob.zombie.say";
                    break;
            }
            player.worldObj.playSoundAtEntity(player, soundString, 1F, 1F);
        }
        if (player.worldObj.rand.nextInt(150) == 0) { // random floating explosion things
            int explosionX = (x - 30) + player.worldObj.rand.nextInt(31);
            int explosionY = y + player.worldObj.rand.nextInt(25);
            int explosionZ = (z - 30) + player.worldObj.rand.nextInt(31);
            player.worldObj.spawnParticle("largeexplode", explosionX, explosionY, explosionZ, 1.0D, 0.0D, 0.0D);
            player.worldObj.spawnParticle("hugeexplosion", explosionX, explosionY, explosionZ, 0.0D, 0.0D, 0.0D);
            player.worldObj.playSoundEffect(
                explosionX,
                explosionY,
                explosionZ,
                "random.explode",
                4.0F,
                (1.0F + (rand.nextFloat() - rand.nextFloat()) * 0.2F) * 0.7F);
            player.attackEntityFrom(DamageSource.magic, 1 + player.worldObj.rand.nextInt(10));
        }
        if (player.worldObj.rand.nextInt(50) == 0) { // random teleporations and snaps
            int cx = 0, cy = 0, cz = 0;
            if (player.worldObj.rand.nextBoolean()) { // teleport
                cx = rand.nextInt(50);
                cy = rand.nextInt(7);
                cz = rand.nextInt(50);
                if (!player.worldObj.isAirBlock((int) player.posX + cx, (int) player.posY + cy, (int) player.posZ + cz)
                    || !player.worldObj
                        .isAirBlock((int) player.posX + cx, (int) player.posY + cy - 1, (int) player.posZ + cz)) {
                    cx = 0;
                    cy = 0;
                    cz = 0;
                }
            }
            player.setPositionAndRotation(
                player.posX + cx,
                player.posY + cy,
                player.posZ + cz,
                player.rotationYaw * (rand.nextFloat() + rand.nextFloat()),
                player.rotationPitch * (rand.nextFloat() + rand.nextFloat()));
        }
        if (player.worldObj.rand.nextInt(750) == 0) { // even more hallucinations + ones specific to end
            Class<? extends EntityHallucination> halclass = null;
            switch (player.worldObj.rand.nextInt(8)) {
                case 1:
                    halclass = EntitySpecificHallucinations.EntityHallucinationZombie.class;
                    break;
                case 2:
                    halclass = EntitySpecificHallucinations.EntityHallucinationSpider.class;
                    break;
                case 3:
                    halclass = EntitySpecificHallucinations.EntityHallucinationWitherSkeleton.class;
                    break;
                case 4:
                    halclass = EntitySpecificHallucinations.EntityHallucinationEnderman.class;
                    break;
                case 5:
                case 6:
                case 7:
                    halclass = EntitySpecificHallucinations.EntityHallucinationEndermite.class;
                    break;
                default:
                    halclass = EntitySpecificHallucinations.EntityHallucinationCreeper.class;
                    break;
            }

            EntityCreature summoned = summonCreature(player.worldObj, halclass, x, y, z, player, 5, 13);
            hallucinationMap.computeIfAbsent(player, k -> new ArrayList<>());
            hallucinationMap.get(player)
                .add(summoned);
        }
    }

    public static EntityCreature summonCreature(World world, Class<? extends EntityHallucination> creatureClass, int x,
        int y, int z, EntityLivingBase target, int minRange, int maxRange) {
        if (!world.isRemote) {
            int activeRadius = maxRange - minRange;
            int ax = world.rand.nextInt(activeRadius * 2 + 1);
            if (ax > activeRadius) {
                ax += minRange * 2;
            }
            int nx = x - maxRange + ax;
            int az = world.rand.nextInt(activeRadius * 2 + 1);
            if (az > activeRadius) {
                az += minRange * 2;
            }
            int nz = z - maxRange + az;
            int ny;
            for (ny = y; ny < y + 8; ++ny) {
                if (world.isAirBlock(nx, ny, nz)) break;
            }
            while (world.isAirBlock(nx, ny, nz) && ny > 0) {
                --ny;
            }
            int hy;
            for (hy = 0; hy < 6; ++hy) {
                if (!world.isAirBlock(nx, ny + hy + 1, nz)) break;
            }
            if (hy >= 2) {
                EntityHallucination creature = EntitySpecificHallucinations.create(creatureClass, world);
                if (creature != null) {
                    if (target instanceof EntityPlayer) {
                        EntityPlayer player = (EntityPlayer) target;
                        creature.setTarget(player.getCommandSenderName());
                    }

                    creature.setLocationAndAngles(
                        0.5D + (double) nx,
                        0.05D + (double) ny + 1.0D,
                        0.5D + (double) nz,
                        0.0F,
                        0.0F);
                    world.spawnEntityInWorld(creature);

                    return creature;
                }
            }
        }
        return null;
    }

}
