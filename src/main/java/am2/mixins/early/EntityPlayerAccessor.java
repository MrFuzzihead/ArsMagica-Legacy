package am2.mixins.early;

import net.minecraft.entity.player.EntityPlayer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Replaces the {@code ReflectionHelper} get/set of {@code EntityPlayer.speedInAir} in {@code ArmorEventHandler}.
 * The field is protected, and the handler is not a subclass of {@code EntityPlayer}.
 */
@Mixin(EntityPlayer.class)
public interface EntityPlayerAccessor {

    @Accessor("speedInAir")
    float am2$getSpeedInAir();

    @Accessor("speedInAir")
    void am2$setSpeedInAir(float speedInAir);
}
