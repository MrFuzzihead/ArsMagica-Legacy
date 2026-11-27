package am2;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;

import net.minecraft.entity.item.EntityItemFrame;
import net.minecraft.world.World;
import net.minecraft.world.WorldProvider;

import org.junit.Test;

/**
 * Pins the item-frame key: entity IDs are per-world, so frames in different dimensions must not compare equal.
 */
public class ItemFrameWatcherKeyTest {

    @Test
    public void sameFrameInTheSameDimensionIsEqual() throws Exception {
        EntityItemFrame frame = frame(7, 0);
        ItemFrameWatcher.EntityItemFrameComparator a = new ItemFrameWatcher.EntityItemFrameComparator(frame);
        ItemFrameWatcher.EntityItemFrameComparator b = new ItemFrameWatcher.EntityItemFrameComparator(frame);

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    public void sameEntityIdInDifferentDimensionsIsNotEqual() throws Exception {
        ItemFrameWatcher.EntityItemFrameComparator a = new ItemFrameWatcher.EntityItemFrameComparator(frame(7, 0));
        ItemFrameWatcher.EntityItemFrameComparator b = new ItemFrameWatcher.EntityItemFrameComparator(frame(7, -1));

        assertNotEquals(a, b);
        assertNotEquals(a.hashCode(), b.hashCode());
    }

    @Test
    public void differentEntityIdsAreNotEqual() throws Exception {
        ItemFrameWatcher.EntityItemFrameComparator a = new ItemFrameWatcher.EntityItemFrameComparator(frame(7, 0));
        ItemFrameWatcher.EntityItemFrameComparator b = new ItemFrameWatcher.EntityItemFrameComparator(frame(8, 0));

        assertNotEquals(a, b);
    }

    private static EntityItemFrame frame(int entityId, int dimension) throws Exception {
        EntityItemFrame frame = mock(EntityItemFrame.class);
        when(frame.getEntityId()).thenReturn(entityId);

        World world = mock(World.class);
        world.isRemote = false;
        WorldProvider provider = mock(WorldProvider.class);
        provider.dimensionId = dimension;
        Field field = World.class.getDeclaredField("provider");
        field.setAccessible(true);
        field.set(world, provider);
        frame.worldObj = world;
        return frame;
    }
}
