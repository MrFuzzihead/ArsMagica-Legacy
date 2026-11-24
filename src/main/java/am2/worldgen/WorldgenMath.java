package am2.worldgen;

import java.util.Random;

/**
 * Bounds-safe wrappers around {@link Random#nextInt(int)} for configuration-driven world generation.
 *
 * <p>
 * A config value of 0 (the natural way to disable a feature) or an inverted ore height range used to throw
 * {@code IllegalArgumentException} during chunk generation. These helpers treat non-positive bounds as "feature
 * disabled" and never call {@code nextInt(0)}.
 * </p>
 */
public final class WorldgenMath {

    private WorldgenMath() {}

    /** True only when a positive bound was configured and the roll came up zero. */
    public static boolean chance(Random random, int bound) {
        return bound > 0 && random.nextInt(bound) == 0;
    }

    /** True only when a positive bound was configured and the roll is below the threshold. */
    public static boolean chanceBelow(Random random, int bound, int threshold) {
        return bound > 0 && threshold > 0 && random.nextInt(bound) < threshold;
    }

    /** Safe ore height: an empty/inverted range collapses to {@code minY} instead of {@code nextInt(0)}. */
    public static int oreHeight(Random random, int minY, int maxY) {
        int range = maxY - minY;
        if (range <= 0) {
            return minY;
        }
        return minY + random.nextInt(range);
    }

    /** Meteor roll used by {@code MeteorSpawnHelper}; a zero bound means "do not roll". */
    public static boolean meteorRoll(Random random, int moonstoneFrequency, int moonPhase) {
        int bound = (moonstoneFrequency / 19) + (1000 * moonPhase);
        return bound > 0 && random.nextInt(bound) == 0;
    }
}
