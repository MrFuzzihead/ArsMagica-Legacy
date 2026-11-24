package am2.worldgen;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Random;

import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.World;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.chunk.IChunkProvider;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import am2.AMCore;
import am2.configuration.AMConfig;
import am2.proxy.CommonProxy;
import am2.proxy.tick.ServerTickHandler;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.Side;

/**
 * Pins the retro-gen queue semantics: FIFO, bounded work per call, O(1) removal, server-only processing and a single
 * WorldTickEvent phase.
 */
public class RetroactiveWorldgeneratorTest {

    private CommonProxy previousProxy;
    private AMConfig previousConfig;
    private CommonProxy proxy;
    private AM2WorldDecorator decorator;
    private World world;
    private IChunkProvider chunkProvider;

    @Before
    public void setUp() throws Exception {
        previousProxy = AMCore.proxy;
        previousConfig = AMCore.config;

        decorator = mock(AM2WorldDecorator.class);
        proxy = mock(CommonProxy.class);
        proxy.worldGen = decorator;
        when(proxy.drainDeferredPotionEffects()).thenReturn(new java.util.HashMap<>());
        when(proxy.drainDeferredDimensionTransfers()).thenReturn(new java.util.HashMap<>());
        AMCore.proxy = proxy;

        world = world(0);
        chunkProvider = mock(IChunkProvider.class);
        when(world.getChunkProvider()).thenReturn(chunkProvider);

        RetroactiveWorldgenerator.deferredChunkGeneration.clear();
    }

    @After
    public void tearDown() {
        RetroactiveWorldgenerator.deferredChunkGeneration.clear();
        AMCore.proxy = previousProxy;
        AMCore.config = previousConfig;
    }

    @Test
    public void processesAtMostFiveChunksInFifoOrder() {
        for (int i = 0; i < 8; i++) {
            queue(0, new ChunkCoordIntPair(i, i + 10));
        }

        RetroactiveWorldgenerator.instance.continueRetrogen(world);

        verify(decorator, times(5))
            .generate(any(Random.class), anyInt(), anyInt(), eq(world), eq(chunkProvider), eq(chunkProvider));

        Deque<ChunkCoordIntPair> remaining = RetroactiveWorldgenerator.deferredChunkGeneration.get(0);
        assertEquals(3, remaining.size());
        assertEquals(5, remaining.peekFirst().chunkXPos);
        assertEquals(15, remaining.peekFirst().chunkZPos);
        assertEquals(7, remaining.peekLast().chunkXPos);
        assertEquals(17, remaining.peekLast().chunkZPos);
    }

    @Test
    public void nextCallDrainsTheRestAndRemovesTheDimensionEntry() {
        for (int i = 0; i < 8; i++) {
            queue(0, new ChunkCoordIntPair(i, i));
        }

        RetroactiveWorldgenerator.instance.continueRetrogen(world);
        RetroactiveWorldgenerator.instance.continueRetrogen(world);

        verify(decorator, times(8))
            .generate(any(Random.class), anyInt(), anyInt(), eq(world), eq(chunkProvider), eq(chunkProvider));
        assertFalse(RetroactiveWorldgenerator.deferredChunkGeneration.containsKey(0));
    }

    @Test
    public void remoteWorldsAreIgnored() {
        world.isRemote = true;
        queue(0, new ChunkCoordIntPair(1, 2));

        RetroactiveWorldgenerator.instance.continueRetrogen(world);

        verify(decorator, never()).generate(
            any(Random.class),
            anyInt(),
            anyInt(),
            any(World.class),
            any(IChunkProvider.class),
            any(IChunkProvider.class));
        assertEquals(
            1,
            RetroactiveWorldgenerator.deferredChunkGeneration.get(0)
                .size());
    }

    @Test
    public void nullWorldIsIgnored() {
        queue(0, new ChunkCoordIntPair(1, 2));
        RetroactiveWorldgenerator.instance.continueRetrogen(null);
        assertEquals(
            1,
            RetroactiveWorldgenerator.deferredChunkGeneration.get(0)
                .size());
    }

    @Test
    public void emptyOrMissingQueueIsANoOp() {
        RetroactiveWorldgenerator.instance.continueRetrogen(world);
        verify(decorator, never()).generate(
            any(Random.class),
            anyInt(),
            anyInt(),
            any(World.class),
            any(IChunkProvider.class),
            any(IChunkProvider.class));

        RetroactiveWorldgenerator.deferredChunkGeneration.put(0, new ArrayDeque<ChunkCoordIntPair>());
        RetroactiveWorldgenerator.instance.continueRetrogen(world);
        verify(decorator, never()).generate(
            any(Random.class),
            anyInt(),
            anyInt(),
            any(World.class),
            any(IChunkProvider.class),
            any(IChunkProvider.class));
    }

    @Test
    public void serverTickOnlyRunsRetrogenAtEndPhase() {
        AMCore.config = mock(AMConfig.class);
        when(AMCore.config.retroactiveWorldgen()).thenReturn(true);
        ServerTickHandler handler = new ServerTickHandler();

        queue(0, new ChunkCoordIntPair(1, 2));
        handler.onWorldTick(new TickEvent.WorldTickEvent(Side.SERVER, TickEvent.Phase.START, world));
        verify(decorator, never()).generate(
            any(Random.class),
            anyInt(),
            anyInt(),
            any(World.class),
            any(IChunkProvider.class),
            any(IChunkProvider.class));

        handler.onWorldTick(new TickEvent.WorldTickEvent(Side.SERVER, TickEvent.Phase.END, world));
        verify(decorator, times(1))
            .generate(any(Random.class), anyInt(), anyInt(), eq(world), eq(chunkProvider), eq(chunkProvider));
    }

    @Test
    public void addQueuedRetrogenCreatesAndReusesTheDeque() {
        CommonProxy realProxy = new CommonProxy();
        ChunkCoordIntPair first = new ChunkCoordIntPair(1, 2);
        ChunkCoordIntPair second = new ChunkCoordIntPair(3, 4);

        realProxy.addQueuedRetrogen(7, first);
        realProxy.addQueuedRetrogen(7, second);

        Deque<ChunkCoordIntPair> queued = RetroactiveWorldgenerator.deferredChunkGeneration.get(7);
        assertEquals(2, queued.size());
        assertSame(first, queued.pollFirst());
        assertSame(second, queued.pollFirst());
        assertTrue(queued instanceof ArrayDeque);
    }

    private static void queue(int dimension, ChunkCoordIntPair pair) {
        Deque<ChunkCoordIntPair> chunks = RetroactiveWorldgenerator.deferredChunkGeneration.get(dimension);
        if (chunks == null) {
            chunks = new ArrayDeque<ChunkCoordIntPair>();
            RetroactiveWorldgenerator.deferredChunkGeneration.put(dimension, chunks);
        }
        chunks.add(pair);
    }

    private static World world(int dimension) throws Exception {
        World world = mock(World.class);
        WorldProvider provider = mock(WorldProvider.class);
        provider.dimensionId = dimension;
        Field field = World.class.getDeclaredField("provider");
        field.setAccessible(true);
        field.set(world, provider);
        world.rand = new Random(0);
        return world;
    }
}
