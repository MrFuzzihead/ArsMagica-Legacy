package am2.playerextensions.effects;

import java.util.Objects;

/** Typed key for a spatial-vortex anchor. */
public final class SpatialVortexEffect {

    public final int x;
    public final int y;
    public final int z;
    public final int dimension;

    public SpatialVortexEffect(int x, int y, int z, int dimension) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.dimension = dimension;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof SpatialVortexEffect)) return false;
        SpatialVortexEffect that = (SpatialVortexEffect) other;
        return x == that.x && y == that.y && z == that.z && dimension == that.dimension;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y, z, dimension);
    }
}
