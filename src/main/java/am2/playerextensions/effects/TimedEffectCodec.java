package am2.playerextensions.effects;

import java.util.UUID;

/** Parsers for the legacy encoded timed-effect keys. */
public final class TimedEffectCodec {

    private TimedEffectCodec() {}

    public static TimedBlockEffect parseAcceleratedTile(String raw) {
        if (raw == null) return null;
        String[] parts = raw.split("_");
        if (parts.length < 8) return null;
        try {
            return new TimedBlockEffect(
                Integer.parseInt(parts[3]),
                Integer.parseInt(parts[4]),
                Integer.parseInt(parts[5]),
                Integer.parseInt(parts[6]),
                Integer.parseInt(parts[7]));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Parses {@code timefortified_tile_x_y_z_dim} and {@code shielded_tile_x_y_z_dim}. */
    public static TimedBlockEffect parseTimedBlock(String raw) {
        if (raw == null) return null;
        String[] parts = raw.split("_");
        if (parts.length < 6) return null;
        try {
            return new TimedBlockEffect(
                Integer.parseInt(parts[2]),
                Integer.parseInt(parts[3]),
                Integer.parseInt(parts[4]),
                Integer.parseInt(parts[5]),
                0);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static TimedEntityEffect parseEntity(String raw) {
        if (raw == null) return null;
        String[] parts = raw.split("_");
        if (parts.length < 5) return null;
        UUID uuid = parseUUID(parts[4]);
        if (uuid == null) return null;
        try {
            return new TimedEntityEffect(uuid, Integer.parseInt(parts[3]));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static SpatialVortexEffect parseSpatialVortex(String raw) {
        if (raw == null) return null;
        String[] parts = raw.split("_");
        if (parts.length < 5) return null;
        try {
            return new SpatialVortexEffect(
                Integer.parseInt(parts[1]),
                Integer.parseInt(parts[2]),
                Integer.parseInt(parts[3]),
                Integer.parseInt(parts[4]));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static UUID parseUUID(String value) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
