package am2.playerextensions.effects;

import java.util.Objects;
import java.util.UUID;

/** Typed key for a timed effect attached to an entity. */
public final class TimedEntityEffect {

    public final UUID entityUUID;
    public final int power;

    public TimedEntityEffect(UUID entityUUID, int power) {
        this.entityUUID = entityUUID;
        this.power = power;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof TimedEntityEffect)) return false;
        TimedEntityEffect that = (TimedEntityEffect) other;
        return power == that.power && Objects.equals(entityUUID, that.entityUUID);
    }

    @Override
    public int hashCode() {
        return Objects.hash(entityUUID, power);
    }
}
