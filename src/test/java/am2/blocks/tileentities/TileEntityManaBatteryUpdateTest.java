package am2.blocks.tileentities;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.storage.WorldInfo;

import org.junit.Test;

/**
 * Pins the mana battery's change-gated metadata update. The old code sent a block metadata update and full tile NBT
 * packet for every battery every tick, even when the power type had not changed.
 */
public class TileEntityManaBatteryUpdateTest {

    @Test
    public void unchangedMetadataDoesNotSendAnUpdate() {
        World world = world();
        when(world.getBlockMetadata(0, 0, 0)).thenReturn(2);

        TileEntityManaBattery battery = new TileEntityManaBattery();
        setWorldObjWithoutRegistering(battery, world);
        battery.updatePowerMetadata(2);

        verify(world, never()).setBlockMetadataWithNotify(anyInt(), anyInt(), anyInt(), anyInt(), anyInt());
        verify(world, never()).markBlockForUpdate(anyInt(), anyInt(), anyInt());
    }

    @Test
    public void changedMetadataSendsOneUpdate() {
        World world = world();
        when(world.getBlockMetadata(0, 0, 0)).thenReturn(1);

        TileEntityManaBattery battery = new TileEntityManaBattery();
        setWorldObjWithoutRegistering(battery, world);
        battery.updatePowerMetadata(2);

        verify(world).setBlockMetadataWithNotify(0, 0, 0, 2, 2);
        verify(world).markBlockForUpdate(0, 0, 0);
    }

    /** Set the protected field directly so the test does not register a node in the global power registry. */
    private static void setWorldObjWithoutRegistering(TileEntity tileEntity, World world) {
        try {
            Field field = TileEntity.class.getDeclaredField("worldObj");
            field.setAccessible(true);
            field.set(tileEntity, world);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    private static World world() {
        World world = mock(World.class);
        WorldProvider provider = mock(WorldProvider.class);
        provider.dimensionId = 0;
        try {
            Field field = World.class.getDeclaredField("provider");
            field.setAccessible(true);
            field.set(world, provider);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
        WorldInfo info = mock(WorldInfo.class);
        when(info.getWorldName()).thenReturn("mana-battery-test");
        when(world.getWorldInfo()).thenReturn(info);
        return world;
    }
}
