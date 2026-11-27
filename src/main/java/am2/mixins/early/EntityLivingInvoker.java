package am2.mixins.early;

import net.minecraft.entity.EntityLiving;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Replaces the reflective {@code EntityLiving.isAIEnabled()} lookup in {@code EntityUtilities}. The method is
 * protected, so it is not callable from {@code EntityUtilities}.
 */
@Mixin(EntityLiving.class)
public interface EntityLivingInvoker {

    @Invoker("isAIEnabled")
    boolean am2$isAIEnabled();
}
