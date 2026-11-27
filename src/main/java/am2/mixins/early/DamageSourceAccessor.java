package am2.mixins.early;

import net.minecraft.util.DamageSource;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Replaces the {@code ReflectionHelper.setPrivateValue(DamageSource.class, ...)} call in {@code DamageSources}.
 * There is no public setter for {@code isUnblockable}.
 */
@Mixin(DamageSource.class)
public interface DamageSourceAccessor {

    @Accessor("isUnblockable")
    void am2$setUnblockable(boolean unblockable);
}
