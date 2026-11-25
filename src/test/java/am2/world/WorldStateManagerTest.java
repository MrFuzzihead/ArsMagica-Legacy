package am2.world;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import java.lang.reflect.Field;

import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.World;
import net.minecraft.world.WorldProvider;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class WorldStateManagerTest {

    @Before
    @After
    public void clear() {
        WorldStateManager.clearAll();
    }

    @Test
    public void worldsWithTheSameDimensionAreIsolatedByIdentity() throws Exception {
        World first = world(0);
        World second = world(0);

        WorldStateManager.forceShielded(first)
            .add("1_2_3_0");
        assertFalse(
            WorldStateManager.forceShielded(second)
                .contains("1_2_3_0"));

        WorldStateManager.unload(first);
        assertTrue(
            WorldStateManager.forceShielded(second)
                .isEmpty());
    }

    @Test
    public void playerAndCreatureMapsAreWorldScoped() throws Exception {
        World first = world(0);
        World second = world(0);
        EntityPlayer player = mock(EntityPlayer.class);
        EntityCreature creature = mock(EntityCreature.class);

        WorldStateManager.hallucinationMap(first)
            .put(player, new java.util.ArrayList<EntityCreature>());
        WorldStateManager.dustMap(first)
            .put(player, new java.util.ArrayList<EntityItem>());
        WorldStateManager.tempCurseMap(first)
            .put(creature, 10);

        assertTrue(
            WorldStateManager.hallucinationMap(second)
                .isEmpty());
        assertTrue(
            WorldStateManager.dustMap(second)
                .isEmpty());
        assertTrue(
            WorldStateManager.tempCurseMap(second)
                .isEmpty());

        WorldStateManager.unload(first);
        assertTrue(
            WorldStateManager.hallucinationMap(first)
                .isEmpty());
    }

    @Test
    public void attachmentsAreScopedAndCachedByWorld() throws Exception {
        World first = world(0);
        World second = world(0);

        Object firstValue = WorldStateManager.getOrCreate(first, "key", Object::new);
        assertTrue(firstValue == WorldStateManager.getOrCreate(first, "key", Object::new));
        assertFalse(firstValue == WorldStateManager.getOrCreate(second, "key", Object::new));

        WorldStateManager.unload(first);
        assertFalse(firstValue == WorldStateManager.getOrCreate(first, "key", Object::new));
    }

    @Test
    public void retroGenQueuesAreWorldScoped() throws Exception {
        World first = world(0);
        World second = world(0);

        WorldStateManager.retroGenChunks(first)
            .add(new ChunkCoordIntPair(1, 2));

        assertTrue(
            WorldStateManager.retroGenChunks(second)
                .isEmpty());
        WorldStateManager.unload(first);
        assertTrue(
            WorldStateManager.retroGenChunks(first)
                .isEmpty());
    }

    @Test
    public void clearAllDropsEveryWorld() throws Exception {
        World world = world(0);
        WorldStateManager.forceShielded(world)
            .add("1_2_3_0");

        WorldStateManager.clearAll();

        assertTrue(WorldStateManager.isEmpty());
    }

    @Test
    public void nullWorldHasAnEmptyView() {
        assertTrue(
            WorldStateManager.forceShielded(null)
                .isEmpty());
    }

    private static World world(int dimension) throws Exception {
        World world = mock(World.class);
        WorldProvider provider = mock(WorldProvider.class);
        provider.dimensionId = dimension;
        Field field = World.class.getDeclaredField("provider");
        field.setAccessible(true);
        field.set(world, provider);
        return world;
    }
}
