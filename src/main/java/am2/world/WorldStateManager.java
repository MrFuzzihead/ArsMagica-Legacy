package am2.world;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

import net.minecraft.world.World;

/**
 * World-scoped transient state. Entries are keyed by the live {@link World} instance, not just its dimension id, so
 * client/server worlds that share a dimension cannot collide.
 */
public final class WorldStateManager {

    private static final Map<World, WorldState> states = new IdentityHashMap<World, WorldState>();

    private WorldStateManager() {}

    public static Set<String> forceShielded(World world) {
        if (world == null) return Collections.emptySet();
        WorldState state = states.get(world);
        if (state == null) {
            state = new WorldState();
            states.put(world, state);
        }
        return state.forceShielded;
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
    }
}
