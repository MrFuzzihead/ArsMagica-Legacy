package am2.power;

import java.util.Objects;

import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.World;

/**
 * Identity of one 32x32-chunk region file: the save directory, dimension and region coordinates.
 *
 * <p>
 * The old key only carried the dimension and region, so two worlds in the same session could read or write each
 * other's power files. Including the save directory keeps saves isolated.
 * </p>
 */
public class RegionCoordinates {

    public final String saveId;
    public final int x;
    public final int z;
    public final int dimension;

    RegionCoordinates(World world, ChunkCoordIntPair pair) {
        this.saveId = PowerNodeCache.getSaveId(world);
        this.x = (int) Math.floor(pair.chunkXPos / 32.0D);
        this.z = (int) Math.floor(pair.chunkZPos / 32.0D);
        this.dimension = world.provider.dimensionId;
    }

    boolean belongsTo(World world) {
        return dimension == world.provider.dimensionId && saveId.equals(PowerNodeCache.getSaveId(world));
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof RegionCoordinates)) return false;
        RegionCoordinates other = (RegionCoordinates) obj;
        return x == other.x && z == other.z && dimension == other.dimension && saveId.equals(other.saveId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(saveId, x, z, dimension);
    }
}
