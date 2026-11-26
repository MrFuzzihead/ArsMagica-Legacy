package am2.blocks.tileentities;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Random;

import net.minecraft.world.World;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.storage.ISaveHandler;
import net.minecraft.world.storage.WorldInfo;

import org.junit.Test;

public class TileEntityFlickerHabitatOffsetTest {

    @Test
    public void clientWorldInitialisesTheRandomOffsets() {
        TileEntityFlickerHabitat habitat = new TileEntityFlickerHabitat();
        World world = world(true);

        habitat.setWorldObj(world);

        assertNotEquals(0.0F, habitat.getRotateOffset(), 0.0001F);
        assertNotEquals(0.0F, habitat.getFloatOffset(), 0.0001F);
    }

    private static World world(boolean remote) {
        World world = mock(World.class);
        WorldProvider provider = mock(WorldProvider.class);
        provider.dimensionId = 0;
        try {
            java.lang.reflect.Field field = World.class.getDeclaredField("provider");
            field.setAccessible(true);
            field.set(world, provider);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
        WorldInfo info = mock(WorldInfo.class);
        when(info.getWorldName()).thenReturn("flicker-offset-test");
        when(world.getWorldInfo()).thenReturn(info);
        when(world.getSaveHandler()).thenReturn(mock(ISaveHandler.class));
        world.isRemote = remote;
        world.rand = new Random(1234L);
        return world;
    }

    @Test
    public void serverWorldLeavesTheOffsetsAtZero() {
        TileEntityFlickerHabitat habitat = new TileEntityFlickerHabitat();
        World world = world(false);

        habitat.setWorldObj(world);

        assertEquals(0.0F, habitat.getRotateOffset(), 0.0001F);
        assertEquals(0.0F, habitat.getFloatOffset(), 0.0001F);
    }
}
