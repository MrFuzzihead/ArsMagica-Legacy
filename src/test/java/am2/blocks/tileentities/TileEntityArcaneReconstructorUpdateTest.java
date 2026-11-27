package am2.blocks.tileentities;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import org.junit.Before;
import org.junit.Test;

/**
 * The reconstructor used to call {@code markBlockForUpdate} every repair-rate tick, even when it had no item and was
 * already inactive. Only actual active-state transitions should sync.
 */
public class TileEntityArcaneReconstructorUpdateTest {

    private TileEntityArcaneReconstructor reconstructor;
    private World world;
    private Method queueRepairableItem;

    @Before
    public void setUp() throws Exception {
        reconstructor = new TileEntityArcaneReconstructor();
        world = mock(World.class);
        Field worldField = net.minecraft.tileentity.TileEntity.class.getDeclaredField("worldObj");
        worldField.setAccessible(true);
        worldField.set(reconstructor, world);
        queueRepairableItem = TileEntityArcaneReconstructor.class.getDeclaredMethod("queueRepairableItem");
        queueRepairableItem.setAccessible(true);
    }

    @Test
    public void idleInactiveReconstructorDoesNotSyncEveryTick() throws Exception {
        assertTrue(invokeQueue());

        verify(world, never()).markBlockForUpdate(anyInt(), anyInt(), anyInt());
    }

    @Test
    public void queueingAndClearingAnItemSyncsOnlyOnTransitions() throws Exception {
        Item item = mock(Item.class);
        when(item.isRepairable()).thenReturn(true);
        ItemStack stack = mock(ItemStack.class);
        when(stack.getItem()).thenReturn(item);
        when(stack.copy()).thenReturn(stack);
        reconstructor.setInventorySlotContents(4, stack);

        assertTrue(invokeQueue());
        assertNotNull(reconstructor.getStackInSlot(3));
        verify(world, times(1)).markBlockForUpdate(anyInt(), anyInt(), anyInt());

        // The active slot is occupied, so this is not a state change.
        assertFalse(invokeQueue());
        verify(world, times(1)).markBlockForUpdate(anyInt(), anyInt(), anyInt());

        // Removing the active item while active is a transition back to idle.
        reconstructor.setInventorySlotContents(3, null);
        assertTrue(invokeQueue());
        verify(world, times(2)).markBlockForUpdate(anyInt(), anyInt(), anyInt());

        // Already idle and empty: no further sync.
        assertTrue(invokeQueue());
        verify(world, times(2)).markBlockForUpdate(anyInt(), anyInt(), anyInt());
    }

    private boolean invokeQueue() throws Exception {
        return (Boolean) queueRepairableItem.invoke(reconstructor);
    }
}
