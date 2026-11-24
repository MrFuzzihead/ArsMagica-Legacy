package am2.power;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.World;
import net.minecraft.world.storage.ISaveHandler;
import net.minecraft.world.storage.SaveHandler;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.event.world.WorldEvent;

import am2.AMCore;
import am2.LogHelper;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;

/**
 * Persists power-node data per region file.
 *
 * <p>
 * The cache is keyed by {@link RegionCoordinates}, which includes the world save directory. Chunk unload now puts the
 * chunk compound into the region cache; the region file is written on world save/unload and whenever the config asks
 * for an immediate save. This fixes the old path where unloaded chunks were never written and where dimension and
 * world caches could collide.
 * </p>
 */
public class PowerNodeCache {

    public static String extension = ".amc";
    public static String folder = "AM2PowerData";
    private static final String pndID = "pnd_%d_%d";

    private final Map<String, File> saveDirs = new HashMap<String, File>();
    private final Map<RegionCoordinates, NBTTagCompound> dataCache = new HashMap<RegionCoordinates, NBTTagCompound>();

    public static final PowerNodeCache instance = new PowerNodeCache();

    static String getSaveId(World world) {
        ISaveHandler handler = world.getSaveHandler();
        if (handler instanceof SaveHandler) {
            return ((SaveHandler) handler).getWorldDirectory()
                .getAbsolutePath();
        }
        // Custom save handlers have no stable path; world identity is enough for one session.
        return world.getWorldInfo()
            .getWorldName() + ":"
            + System.identityHashCode(world);
    }

    private File getSaveFolder(World world, boolean createNew) {
        String key = getSaveId(world) + ":" + world.provider.dimensionId;
        File saveFolder = saveDirs.get(key);
        if (saveFolder != null) {
            return saveFolder;
        }

        ISaveHandler handler = world.getSaveHandler();
        if (!(handler instanceof SaveHandler)) {
            return null;
        }

        File dimensionFolder = new File(
            new File(((SaveHandler) handler).getWorldDirectory(), folder),
            String.format("DIM%d", world.provider.dimensionId));
        if (createNew) {
            dimensionFolder.mkdirs();
        } else if (!dimensionFolder.isDirectory()) {
            return null;
        }

        saveDirs.put(key, dimensionFolder);
        return dimensionFolder;
    }

    private File getFileForRegion(World world, RegionCoordinates rc, boolean createNew) {
        File saveFolder = getSaveFolder(world, createNew);
        if (saveFolder == null) {
            return null;
        }

        File file = new File(saveFolder, String.format("%d_%d%s", rc.x, rc.z, extension));
        if (!file.exists()) {
            if (!createNew) {
                return null;
            }
            try {
                if (!file.createNewFile() && !file.exists()) {
                    return null;
                }
            } catch (IOException e) {
                LogHelper.error("Unable to create power data file %s: %s", file.getAbsolutePath(), e.toString());
                return null;
            }
        }

        if ((!file.canWrite() && !file.setWritable(true)) || (!file.canRead() && !file.setReadable(true))) {
            LogHelper.error(
                "Unable to obtain a read/write handle for power data file %s. The affected power data will NOT be saved!",
                file.getAbsolutePath());
            return null;
        }
        return file;
    }

    private File getFileFromChunk(World world, ChunkCoordIntPair chunk, boolean createNew) {
        return getFileForRegion(world, new RegionCoordinates(world, chunk), createNew);
    }

    public NBTTagCompound getNBTForChunk(World world, ChunkCoordIntPair chunk) {
        RegionCoordinates rc = new RegionCoordinates(world, chunk);
        NBTTagCompound region = dataCache.get(rc);
        if (region == null) {
            region = readRegionFromFile(world, chunk);
            if (region == null) {
                return null;
            }
            dataCache.put(rc, region);
        }
        return region.getCompoundTag(getPNDIdentifier(chunk));
    }

    /** Package-private so the save/load lifecycle can be unit-tested without a real world. */
    void saveChunkData(World world, ChunkCoordIntPair chunk, NBTTagCompound compound, boolean flushImmediate) {
        RegionCoordinates rc = new RegionCoordinates(world, chunk);
        NBTTagCompound region = dataCache.get(rc);
        if (region == null) {
            region = readRegionFromFile(world, chunk);
            if (region == null) {
                region = new NBTTagCompound();
            }
            dataCache.put(rc, region);
        }

        region.setTag(getPNDIdentifier(chunk), compound);
        if (flushImmediate) {
            writeRegionFile(world, rc, region);
        }
    }

    private NBTTagCompound readRegionFromFile(World world, ChunkCoordIntPair chunk) {
        File file = getFileFromChunk(world, chunk, false);
        if (file == null) {
            return new NBTTagCompound();
        }
        try {
            NBTTagCompound compound = CompressedStreamTools.read(file);
            return compound == null ? new NBTTagCompound() : compound;
        } catch (Throwable e) {
            // A corrupt or empty file should not take the power network down; start a fresh region.
            return new NBTTagCompound();
        }
    }

    private void writeRegionFile(World world, RegionCoordinates rc, NBTTagCompound region) {
        File file = getFileForRegion(world, rc, true);
        if (file == null) {
            return;
        }
        try {
            CompressedStreamTools.write(region, file);
        } catch (IOException e) {
            LogHelper.error("Unable to write power data file %s: %s", file.getAbsolutePath(), e.toString());
        }
    }

    @SubscribeEvent
    public void onChunkUnload(ChunkEvent.Unload event) {
        if (event.world.isRemote || !PowerNodeRegistry.For(event.world)
            .hasDataForChunk(event.getChunk())) {
            return;
        }

        ChunkCoordIntPair pair = event.getChunk()
            .getChunkCoordIntPair();
        NBTTagCompound dataCompound = new NBTTagCompound();
        PowerNodeRegistry.For(event.world)
            .SaveChunkToNBT(pair, dataCompound);
        saveChunkData(event.world, pair, dataCompound, false);
        PowerNodeRegistry.For(event.world)
            .unloadChunk(event.getChunk());
    }

    @SubscribeEvent
    public void onWorldSave(WorldEvent.Save event) {
        World world = event.world;
        if (world.isRemote) {
            return;
        }

        // Data from unmapped/unloaded chunks is only in the cache. Flush it before the live data so live data wins.
        flushCachedRegions(world);

        boolean flushLive = AMCore.config != null && AMCore.config.savePowerDataOnWorldSave();
        for (Map.Entry<ChunkCoordIntPair, NBTTagCompound> entry : PowerNodeRegistry.For(world)
            .saveAll()
            .entrySet()) {
            saveChunkData(world, entry.getKey(), entry.getValue(), flushLive);
        }
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        saveWorldToFile(event.world);
    }

    private void flushCachedRegions(World world) {
        Iterator<Map.Entry<RegionCoordinates, NBTTagCompound>> it = dataCache.entrySet()
            .iterator();
        while (it.hasNext()) {
            Map.Entry<RegionCoordinates, NBTTagCompound> entry = it.next();
            RegionCoordinates rc = entry.getKey();
            if (!rc.belongsTo(world)) {
                continue;
            }
            writeRegionFile(world, rc, entry.getValue());
            it.remove();
        }
    }

    public void saveWorldToFile(World world) {
        if (world.isRemote) {
            return;
        }

        LogHelper.trace("Saving all cached power data for DIM %d to disk", world.provider.dimensionId);
        flushCachedRegions(world);

        for (Map.Entry<ChunkCoordIntPair, NBTTagCompound> entry : PowerNodeRegistry.For(world)
            .saveAll()
            .entrySet()) {
            saveChunkData(world, entry.getKey(), entry.getValue(), true);
        }

        PowerNodeRegistry.For(world)
            .unloadAll();

        String prefix = getSaveId(world) + ":";
        saveDirs.keySet()
            .removeIf(key -> key.startsWith(prefix));
    }

    private String getPNDIdentifier(ChunkCoordIntPair chunk) {
        return String.format(pndID, chunk.chunkXPos, chunk.chunkZPos);
    }
}
