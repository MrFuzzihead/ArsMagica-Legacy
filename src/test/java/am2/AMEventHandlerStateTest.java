package am2;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import java.lang.reflect.Field;
import java.util.ArrayList;

import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import net.minecraft.world.WorldProvider;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import am2.world.WorldStateManager;

/**
 * Pins the transient-state cleanup: world-bound player/entity references are released on unload, and all state is
 * cleared on server stop or disconnect.
 */
public class AMEventHandlerStateTest {

    @Before
    @After
    public void clearState() {
        AMEventHandler.clearAllTransientState();
    }

    @Test
    public void worldUnloadRemovesOnlyThatWorldsEntries() throws Exception {
        World worldA = world(0);
        World worldB = world(-1);
        EntityPlayer playerA = playerIn(worldA);
        EntityPlayer playerB = playerIn(worldB);
        EntityCreature creatureA = creatureIn(worldA);
        EntityCreature creatureB = creatureIn(worldB);

        WorldStateManager.hallucinationMap(worldA)
            .put(playerA, new ArrayList<EntityCreature>());
        WorldStateManager.hallucinationMap(worldB)
            .put(playerB, new ArrayList<EntityCreature>());
        WorldStateManager.dustMap(worldA)
            .put(playerA, new ArrayList<net.minecraft.entity.item.EntityItem>());
        WorldStateManager.dustMap(worldB)
            .put(playerB, new ArrayList<net.minecraft.entity.item.EntityItem>());
        WorldStateManager.tempCurseMap(worldA)
            .put(creatureA, 10);
        WorldStateManager.tempCurseMap(worldB)
            .put(creatureB, 10);
        WorldStateManager.forceShielded(worldA)
            .add("1_2_3_0");
        WorldStateManager.forceShielded(worldB)
            .add("1_2_3_-1");

        AMEventHandler.clearWorldState(worldA);

        assertFalse(
            WorldStateManager.hallucinationMap(worldA)
                .containsKey(playerA));
        assertTrue(
            WorldStateManager.hallucinationMap(worldB)
                .containsKey(playerB));
        assertFalse(
            WorldStateManager.dustMap(worldA)
                .containsKey(playerA));
        assertTrue(
            WorldStateManager.dustMap(worldB)
                .containsKey(playerB));
        assertFalse(
            WorldStateManager.tempCurseMap(worldA)
                .containsKey(creatureA));
        assertTrue(
            WorldStateManager.tempCurseMap(worldB)
                .containsKey(creatureB));
        assertFalse(
            WorldStateManager.forceShielded(worldA)
                .contains("1_2_3_0"));
        assertTrue(
            WorldStateManager.forceShielded(worldB)
                .contains("1_2_3_-1"));
    }

    @Test
    public void clearAllTransientStateEmptiesEverything() throws Exception {
        World world = world(0);
        EntityPlayer player = playerIn(world);
        WorldStateManager.hallucinationMap(world)
            .put(player, new ArrayList<EntityCreature>());
        WorldStateManager.dustMap(world)
            .put(player, new ArrayList<net.minecraft.entity.item.EntityItem>());
        WorldStateManager.tempCurseMap(world)
            .put(creatureIn(world), 10);
        WorldStateManager.forceShielded(world)
            .add("1_2_3_0");
        WorldStateManager.acceleratedEntities(world)
            .put(java.util.UUID.randomUUID(), 1);
        WorldStateManager.slowedEntities(world)
            .put(java.util.UUID.randomUUID(), 1);

        AMEventHandler.clearAllTransientState();

        assertTrue(WorldStateManager.isEmpty());
    }

    @Test
    public void twoHundredWorldLifecyclesDoNotAccumulateState() throws Exception {
        for (int i = 0; i < 200; i++) {
            World world = world(i % 4);
            WorldStateManager.forceShielded(world)
                .add("world_" + i);
            WorldStateManager.acceleratedEntities(world)
                .put(java.util.UUID.randomUUID(), i);
            WorldStateManager.retroGenChunks(world)
                .add(new net.minecraft.world.ChunkCoordIntPair(i, i));

            AMEventHandler.clearWorldState(world);
        }

        assertTrue(WorldStateManager.isEmpty());
    }

    private static EntityPlayer playerIn(World world) {
        EntityPlayer player = mock(EntityPlayer.class);
        player.worldObj = world;
        return player;
    }

    private static EntityCreature creatureIn(World world) {
        EntityCreature creature = mock(EntityCreature.class);
        creature.worldObj = world;
        return creature;
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
