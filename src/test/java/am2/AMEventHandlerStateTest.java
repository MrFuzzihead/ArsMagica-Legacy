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

        AMEventHandler.hallucinationMap.put(playerA, new ArrayList<EntityCreature>());
        AMEventHandler.hallucinationMap.put(playerB, new ArrayList<EntityCreature>());
        AMEventHandler.dustMap.put(playerA, new ArrayList<net.minecraft.entity.item.EntityItem>());
        AMEventHandler.dustMap.put(playerB, new ArrayList<net.minecraft.entity.item.EntityItem>());
        AMEventHandler.tempCurseMap.put(creatureA, 10);
        AMEventHandler.tempCurseMap.put(creatureB, 10);
        AMEventHandler.forceShielded.add("1_2_3_0");
        AMEventHandler.forceShielded.add("1_2_3_-1");

        AMEventHandler.clearWorldState(worldA);

        assertFalse(AMEventHandler.hallucinationMap.containsKey(playerA));
        assertTrue(AMEventHandler.hallucinationMap.containsKey(playerB));
        assertFalse(AMEventHandler.dustMap.containsKey(playerA));
        assertTrue(AMEventHandler.dustMap.containsKey(playerB));
        assertFalse(AMEventHandler.tempCurseMap.containsKey(creatureA));
        assertTrue(AMEventHandler.tempCurseMap.containsKey(creatureB));
        assertFalse(AMEventHandler.forceShielded.contains("1_2_3_0"));
        assertTrue(AMEventHandler.forceShielded.contains("1_2_3_-1"));
    }

    @Test
    public void clearAllTransientStateEmptiesEverything() throws Exception {
        EntityPlayer player = playerIn(world(0));
        AMEventHandler.hallucinationMap.put(player, new ArrayList<EntityCreature>());
        AMEventHandler.dustMap.put(player, new ArrayList<net.minecraft.entity.item.EntityItem>());
        AMEventHandler.tempCurseMap.put(creatureIn(world(0)), 10);
        AMEventHandler.forceShielded.add("1_2_3_0");
        AMEventHandler.acceleratedEntitiesUUIDs.put(java.util.UUID.randomUUID(), 1);
        AMEventHandler.slowedEntitiesUUIDs.put(java.util.UUID.randomUUID(), 1);

        AMEventHandler.clearAllTransientState();

        assertTrue(AMEventHandler.hallucinationMap.isEmpty());
        assertTrue(AMEventHandler.dustMap.isEmpty());
        assertTrue(AMEventHandler.tempCurseMap.isEmpty());
        assertTrue(AMEventHandler.forceShielded.isEmpty());
        assertTrue(AMEventHandler.acceleratedEntitiesUUIDs.isEmpty());
        assertTrue(AMEventHandler.slowedEntitiesUUIDs.isEmpty());
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
