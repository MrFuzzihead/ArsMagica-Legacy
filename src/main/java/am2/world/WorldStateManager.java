package am2.world;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;

/**
 * World-scoped transient state. Entries are keyed by the live {@link World} instance, not just its dimension id, so
 * client/server worlds that share a dimension cannot collide.
 */
public final class WorldStateManager {

    private static final Map<World, WorldState> states = new IdentityHashMap<World, WorldState>();

    private WorldStateManager() {}

    private static WorldState state(World world) {
        if (world == null) return null;
        WorldState state = states.get(world);
        if (state == null) {
            state = new WorldState();
            states.put(world, state);
        }
        return state;
    }

    public static Set<String> forceShielded(World world) {
        WorldState state = state(world);
        return state == null ? Collections.emptySet() : state.forceShielded;
    }

    public static Map<EntityPlayer, ArrayList<EntityCreature>> hallucinationMap(World world) {
        WorldState state = state(world);
        return state == null ? Collections.emptyMap() : state.hallucinationMap;
    }

    public static Map<EntityPlayer, ArrayList<EntityItem>> dustMap(World world) {
        WorldState state = state(world);
        return state == null ? Collections.emptyMap() : state.dustMap;
    }

    public static Map<EntityCreature, Integer> tempCurseMap(World world) {
        WorldState state = state(world);
        return state == null ? Collections.emptyMap() : state.tempCurseMap;
    }

    public static Map<UUID, Integer> acceleratedEntities(World world) {
        WorldState state = state(world);
        return state == null ? Collections.emptyMap() : state.acceleratedEntities;
    }

    public static Map<UUID, Integer> slowedEntities(World world) {
        WorldState state = state(world);
        return state == null ? Collections.emptyMap() : state.slowedEntities;
    }

    public static boolean hasAcceleratedEntities() {
        for (WorldState state : states.values()) {
            if (!state.acceleratedEntities.isEmpty()) return true;
        }
        return false;
    }

    public static void unload(World world) {
        if (world != null) states.remove(world);
    }

    public static void clearAll() {
        states.clear();
    }

    public static boolean isEmpty() {
        return states.isEmpty();
    }

    private static final class WorldState {

        final Set<String> forceShielded = new java.util.HashSet<String>();
        final Map<EntityPlayer, ArrayList<EntityCreature>> hallucinationMap = new HashMap<EntityPlayer, ArrayList<EntityCreature>>();
        final Map<EntityPlayer, ArrayList<EntityItem>> dustMap = new HashMap<EntityPlayer, ArrayList<EntityItem>>();
        final Map<EntityCreature, Integer> tempCurseMap = new HashMap<EntityCreature, Integer>();
        final Map<UUID, Integer> acceleratedEntities = new HashMap<UUID, Integer>();
        final Map<UUID, Integer> slowedEntities = new HashMap<UUID, Integer>();
    }
}
