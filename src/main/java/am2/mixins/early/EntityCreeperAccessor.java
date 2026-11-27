package am2.mixins.early;

import net.minecraft.entity.monster.EntityCreeper;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Replaces the index-based {@code ReflectionHelper.setPrivateValue(EntityCreeper.class, this, 0, 1)} call in
 * {@code EntityManaCreeper}. Index-based field access breaks silently when the vanilla field order changes.
 */
@Mixin(EntityCreeper.class)
public interface EntityCreeperAccessor {

    @Accessor("timeSinceIgnited")
    void am2$setTimeSinceIgnited(int timeSinceIgnited);
}
