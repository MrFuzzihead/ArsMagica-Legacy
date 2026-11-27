package am2.utility;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.init.Blocks;
import net.minecraft.world.World;

/**
 * Tracks the temporary redstone power emitted by the Redstone Signal spell component.
 *
 * <p>
 * The data used to live on {@code MysteriumPatchesFixesMagicka} next to the ASM {@code @Fix} for
 * {@code World.isBlockIndirectlyGettingPowered}. Keeping the state in normal code lets the new
 * {@code WorldRedstoneMixin} and the spell component share it, and makes the decrement logic unit-testable.
 * </p>
 */
public final class RedstoneInlayState {

    /** {dimension, x, y, z, remainingTicks}. */
    private static final List<int[]> PROVIDING_REDSTONE = new ArrayList<int[]>();

    private RedstoneInlayState() {}

    public static void add(int dimension, int x, int y, int z, int activeTime) {
        PROVIDING_REDSTONE.add(new int[] { dimension, x, y, z, activeTime });
    }

    /**
     * Mirrors the old {@code isBlockIndirectlyGettingPowered} patch: consume one tick of a matching inlay and notify
     * the block. Returns true when the inlay supplied power this call.
     */
    public static boolean isProvidingRedstone(World world, int x, int y, int z) {
        return isProvidingRedstone(world.provider.dimensionId, world, x, y, z);
    }

    /** Dimension-explicit overload so the counter logic is testable without constructing a real world provider. */
    static boolean isProvidingRedstone(int dimension, World world, int x, int y, int z) {
        int index = -1;
        for (int i = 0; i < PROVIDING_REDSTONE.size(); i++) {
            int[] provider = PROVIDING_REDSTONE.get(i);
            if (provider[0] == dimension && provider[1] == x && provider[2] == y && provider[3] == z) {
                index = i;
                break;
            }
        }

        if (index < 0) {
            return false;
        }

        int remaining = PROVIDING_REDSTONE.get(index)[4] - 1;
        if (remaining <= 0) {
            PROVIDING_REDSTONE.remove(index);
        } else {
            PROVIDING_REDSTONE.set(index, new int[] { dimension, x, y, z, remaining });
        }

        world.getBlock(x, y, z)
            .onNeighborBlockChange(world, x, y, z, Blocks.stonebrick);
        return true;
    }

    /** The mutable backing list, kept for the still-compiled (but unregistered) old coremod class. */
    public static List<int[]> backingList() {
        return PROVIDING_REDSTONE;
    }

    public static void clear() {
        PROVIDING_REDSTONE.clear();
    }

    public static int size() {
        return PROVIDING_REDSTONE.size();
    }
}
