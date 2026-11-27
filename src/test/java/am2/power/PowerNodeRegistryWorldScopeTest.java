package am2.power;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;

import java.lang.reflect.Field;

import net.minecraft.world.World;
import net.minecraft.world.WorldProvider;

import org.junit.After;
import org.junit.Test;

import am2.world.WorldStateManager;

public class PowerNodeRegistryWorldScopeTest {

    @After
    public void clear() {
        WorldStateManager.clearAll();
    }

    @Test
    public void registryIsCachedPerWorldIdentity() throws Exception {
        World first = world(0);
        World second = world(0);

        assertSame(PowerNodeRegistry.For(first), PowerNodeRegistry.For(first));
        assertFalse(PowerNodeRegistry.For(first) == PowerNodeRegistry.For(second));
    }

    @Test
    public void nullWorldUsesTheDummyRegistry() {
        assertSame(PowerNodeRegistry.For(null), PowerNodeRegistry.For(null));
    }

    private static World world(int dimension) throws Exception {
        World world = net.minecraftforge.common.DimensionManager.getWorld(0);
        if (world != null) return world;
        world = org.mockito.Mockito.mock(World.class);
        WorldProvider provider = org.mockito.Mockito.mock(WorldProvider.class);
        provider.dimensionId = dimension;
        Field field = World.class.getDeclaredField("provider");
        field.setAccessible(true);
        field.set(world, provider);
        return world;
    }
}
