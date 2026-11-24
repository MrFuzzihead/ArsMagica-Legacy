package am2.mixins.early;

import net.minecraft.entity.Entity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Replaces the {@code ReflectionHelper.findMethod(... "fall" ...)} calls in {@code AMEventHandler} and
 * {@code WaterWalking}. {@code Entity.fall(float)} is protected, so unrelated AM2 code cannot call it directly.
 */
@Mixin(Entity.class)
public interface EntityInvoker {

    @Invoker("fall")
    void am2$invokeFall(float distance);
}
