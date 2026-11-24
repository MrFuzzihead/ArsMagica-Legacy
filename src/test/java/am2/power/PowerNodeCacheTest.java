package am2.power;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.lang.reflect.Field;

import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.World;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.storage.SaveHandler;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Covers the power-region save lifecycle: unloaded chunk data is cached, flushed on world save, read back from disk,
 * and isolated by dimension and world save directory.
 */
public class PowerNodeCacheTest {

    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void unloadedChunkDataIsWrittenOnWorldSave() throws Exception {
        World world = world(0, temporaryFolder.getRoot());
        PowerNodeCache cache = new PowerNodeCache();
        ChunkCoordIntPair chunk = new ChunkCoordIntPair(0, 0);

        cache.saveChunkData(world, chunk, chunkData("value"), false);
        File file = regionFile(temporaryFolder.getRoot(), 0, 0);
        assertFalse("cached-only save must not touch disk", file.exists());

        cache.saveWorldToFile(world);

        assertTrue(file.isFile());
        NBTTagCompound region = CompressedStreamTools.read(file);
        assertNotNull(region);
        assertEquals(
            "value",
            region.getCompoundTag("pnd_0_0")
                .getString("test"));
    }

    @Test
    public void aFreshCacheReadsThePersistedChunkBack() throws Exception {
        World world = world(0, temporaryFolder.getRoot());
        ChunkCoordIntPair chunk = new ChunkCoordIntPair(0, 0);
        new PowerNodeCache().saveChunkData(world, chunk, chunkData("persisted"), true);

        PowerNodeCache fresh = new PowerNodeCache();
        NBTTagCompound loaded = fresh.getNBTForChunk(world, chunk);
        assertNotNull(loaded);
        assertEquals("persisted", loaded.getString("test"));
    }

    @Test
    public void dimensionsUseSeparateRegionFiles() throws Exception {
        File saveDirectory = temporaryFolder.getRoot();
        World overworld = world(0, saveDirectory);
        World nether = world(-1, saveDirectory);
        ChunkCoordIntPair chunk = new ChunkCoordIntPair(0, 0);
        PowerNodeCache cache = new PowerNodeCache();

        cache.saveChunkData(overworld, chunk, chunkData("overworld"), true);
        cache.saveChunkData(nether, chunk, chunkData("nether"), true);

        assertTrue(regionFile(saveDirectory, 0, 0).isFile());
        assertTrue(new File(new File(saveDirectory, "AM2PowerData"), "DIM-1/0_0.amc").isFile());
        assertEquals(
            "overworld",
            CompressedStreamTools.read(regionFile(saveDirectory, 0, 0))
                .getCompoundTag("pnd_0_0")
                .getString("test"));
    }

    @Test
    public void differentWorldSavesUseSeparateRegionFiles() throws Exception {
        File worldA = temporaryFolder.newFolder("worldA");
        File worldB = temporaryFolder.newFolder("worldB");
        ChunkCoordIntPair chunk = new ChunkCoordIntPair(0, 0);
        PowerNodeCache cache = new PowerNodeCache();

        cache.saveChunkData(world(0, worldA), chunk, chunkData("a"), true);
        cache.saveChunkData(world(0, worldB), chunk, chunkData("b"), true);

        assertEquals("a", readChunk(regionFile(worldA, 0, 0)));
        assertEquals("b", readChunk(regionFile(worldB, 0, 0)));
    }

    private static String readChunk(File file) throws Exception {
        return CompressedStreamTools.read(file)
            .getCompoundTag("pnd_0_0")
            .getString("test");
    }

    private static NBTTagCompound chunkData(String value) {
        NBTTagCompound compound = new NBTTagCompound();
        compound.setString("test", value);
        return compound;
    }

    private static File regionFile(File saveDirectory, int dimension, int region) {
        return new File(
            new File(new File(saveDirectory, "AM2PowerData"), "DIM" + dimension),
            region + "_" + region + ".amc");
    }

    private static World world(int dimension, File saveDirectory) throws Exception {
        World world = mock(World.class);
        world.isRemote = false;

        WorldProvider provider = mock(WorldProvider.class);
        provider.dimensionId = dimension;
        Field providerField = World.class.getDeclaredField("provider");
        providerField.setAccessible(true);
        providerField.set(world, provider);

        SaveHandler saveHandler = mock(SaveHandler.class);
        when(saveHandler.getWorldDirectory()).thenReturn(saveDirectory);
        when(world.getSaveHandler()).thenReturn(saveHandler);
        return world;
    }
}
