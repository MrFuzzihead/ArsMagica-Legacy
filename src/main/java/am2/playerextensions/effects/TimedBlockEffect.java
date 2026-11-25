package am2.playerextensions.effects;

import java.util.Objects;

/** Typed key for a timed effect attached to a block position. */
public final class TimedBlockEffect {

    public final int x;
    public final int y;
    public final int z;
    public final int dimension;
    public final int power;

    public TimedBlockEffect(int x, int y, int z, int dimension, int power) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.dimension = dimension;
        this.power = power;
    }

    public String representation() {
        return x + "_" + y + "_" + z + "_" + dimension;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof TimedBlockEffect)) return false;
        TimedBlockEffect that = (TimedBlockEffect) other;
        return x == that.x && y == that.y && z == that.z && dimension == that.dimension && power == that.power;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y, z, dimension, power);
    }
}
