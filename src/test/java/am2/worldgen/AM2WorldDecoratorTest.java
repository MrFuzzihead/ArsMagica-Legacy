package am2.worldgen;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.Random;

import net.minecraft.world.World;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.WorldType;
import net.minecraft.world.chunk.IChunkProvider;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import am2.AMCore;
import am2.configuration.AMConfig;

public class AM2WorldDecoratorTest {

    private AMConfig previousConfig;

    @Before
    public void installConfig() {
        previousConfig = AMCore.config;
        AMCore.config = mock(AMConfig.class);
        when(AMCore.config.getWorldgenBlacklist()).thenReturn(new int[0]);
        when(AMCore.config.getWorldgenWhitelistEnabled()).thenReturn(false);
    }

    @After
    public void restoreConfig() {
        AMCore.config = previousConfig;
    }

    @Test
    public void flatWorldsSkipAllGeneration() {
        AM2WorldDecorator decorator = spy(new AM2WorldDecorator());
        World world = world(0, WorldType.FLAT);
        IChunkProvider chunkProvider = mock(IChunkProvider.class);

        decorator.generate(mock(Random.class), 0, 0, world, chunkProvider, chunkProvider);

        verify(decorator, never()).generateNether(
            any(Random.class),
            anyInt(),
            anyInt(),
            any(World.class),
            any(IChunkProvider.class),
            any(IChunkProvider.class));
        verify(decorator, never()).generateOverworld(
            any(Random.class),
            anyInt(),
            anyInt(),
            any(World.class),
            any(IChunkProvider.class),
            any(IChunkProvider.class));
    }

    @Test
    public void theMmfDimensionUsesTheOverworldGenerator() {
        AM2WorldDecorator decorator = spy(new AM2WorldDecorator());
        World world = world(50, WorldType.DEFAULT);
        IChunkProvider chunkProvider = mock(IChunkProvider.class);
        doNothing().when(decorator)
            .generateOverworld(any(Random.class), anyInt(), anyInt(), any(World.class), isNull(), isNull());

        decorator.generate(mock(Random.class), 0, 0, world, null, null);

        verify(decorator)
            .generateOverworld(any(Random.class), anyInt(), anyInt(), any(World.class), isNull(), isNull());
    }

    @Test
    public void theNetherUsesTheNetherGenerator() {
        AM2WorldDecorator decorator = spy(new AM2WorldDecorator());
        World world = world(-1, WorldType.DEFAULT);
        doNothing().when(decorator)
            .generateNether(any(Random.class), anyInt(), anyInt(), any(World.class), isNull(), isNull());

        decorator.generate(mock(Random.class), 0, 0, world, null, null);

        verify(decorator).generateNether(any(Random.class), anyInt(), anyInt(), any(World.class), isNull(), isNull());
    }

    @Test
    public void theEndGeneratesNothing() {
        AM2WorldDecorator decorator = spy(new AM2WorldDecorator());
        World world = world(1, WorldType.DEFAULT);
        IChunkProvider chunkProvider = mock(IChunkProvider.class);

        decorator.generate(mock(Random.class), 0, 0, world, chunkProvider, chunkProvider);

        verify(decorator, never()).generateNether(
            any(Random.class),
            anyInt(),
            anyInt(),
            any(World.class),
            any(IChunkProvider.class),
            any(IChunkProvider.class));
        verify(decorator, never()).generateOverworld(
            any(Random.class),
            anyInt(),
            anyInt(),
            any(World.class),
            any(IChunkProvider.class),
            any(IChunkProvider.class));
    }

    private static World world(int dimension, WorldType type) {
        World world = mock(World.class);
        WorldProvider provider = mock(WorldProvider.class);
        provider.dimensionId = dimension;
        provider.terrainType = type;
        try {
            Field field = World.class.getDeclaredField("provider");
            field.setAccessible(true);
            field.set(world, provider);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
        return world;
    }
}
