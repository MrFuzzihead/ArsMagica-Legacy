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
import am2.world.WorldStateManager;
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

        WorldStateManager.clearAll();
    }

    @After
    public void tearDown() {
        WorldStateManager.clearAll();
        AMCore.proxy = previousProxy;
        AMCore.config = previousConfig;
    }

    @Test
    public void processesAtMostFiveChunksInFifoOrder() {
        for (int i = 0; i < 8; i++) {
            queue(world, new ChunkCoordIntPair(i, i + 10));
        }

        RetroactiveWorldgenerator.instance.continueRetrogen(world);

        verify(decorator, times(5))
            .generate(any(Random.class), anyInt(), anyInt(), eq(world), eq(chunkProvider), eq(chunkProvider));

        Deque<ChunkCoordIntPair> remaining = WorldStateManager.retroGenChunks(world);
        assertEquals(3, remaining.size());
        assertEquals(5, remaining.peekFirst().chunkXPos);
        assertEquals(15, remaining.peekFirst().chunkZPos);
        assertEquals(7, remaining.peekLast().chunkXPos);
        assertEquals(17, remaining.peekLast().chunkZPos);
    }

    @Test
    public void nextCallDrainsTheRestAndRemovesTheDimensionEntry() {
        for (int i = 0; i < 8; i++) {
            queue(world, new ChunkCoordIntPair(i, i));
        }

        RetroactiveWorldgenerator.instance.continueRetrogen(world);
        RetroactiveWorldgenerator.instance.continueRetrogen(world);

        verify(decorator, times(8))
            .generate(any(Random.class), anyInt(), anyInt(), eq(world), eq(chunkProvider), eq(chunkProvider));
        assertFalse(
            !WorldStateManager.retroGenChunks(world)
                .isEmpty());
    }

    @Test
    public void remoteWorldsAreIgnored() {
        world.isRemote = true;
        queue(world, new ChunkCoordIntPair(1, 2));

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
            WorldStateManager.retroGenChunks(world)
                .size());
    }

    @Test
    public void nullWorldIsIgnored() {
        queue(world, new ChunkCoordIntPair(1, 2));
        RetroactiveWorldgenerator.instance.continueRetrogen(null);
        assertEquals(
            1,
            WorldStateManager.retroGenChunks(world)
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

        WorldStateManager.retroGenChunks(world)
            .clear();
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

        queue(world, new ChunkCoordIntPair(1, 2));
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
    public void addQueuedRetrogenCreatesAndReusesTheDeque() throws Exception {
        CommonProxy realProxy = new CommonProxy();
        ChunkCoordIntPair first = new ChunkCoordIntPair(1, 2);
        ChunkCoordIntPair second = new ChunkCoordIntPair(3, 4);

        World queuedWorld = world(7);
        realProxy.addQueuedRetrogen(queuedWorld, first);
        realProxy.addQueuedRetrogen(queuedWorld, second);

        Deque<ChunkCoordIntPair> queued = WorldStateManager.retroGenChunks(queuedWorld);
        assertEquals(2, queued.size());
        assertSame(first, queued.pollFirst());
        assertSame(second, queued.pollFirst());
        assertTrue(queued instanceof ArrayDeque);
    }

    private static void queue(World world, ChunkCoordIntPair pair) {
        WorldStateManager.retroGenChunks(world)
            .add(pair);
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
