package am2.mixins.early;

import net.minecraft.client.renderer.entity.RendererLivingEntity;
import net.minecraft.entity.EntityLivingBase;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import am2.utility.RenderUtilities;

/**
 * Replacement for {@code BytecodeTransformers.alterRendererLivingEntity}.
 *
 * <p>
 * The shrink scale must be applied after {@code renderLivingAt} positions the model, otherwise the model shrinks away
 * from its feet. This is a client-only mixin.
 * </p>
 */
@Mixin(RendererLivingEntity.class)
public abstract class RendererLivingEntityMixin {

    @Inject(
        method = "doRender(Lnet/minecraft/entity/EntityLivingBase;DDDFF)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/entity/RendererLivingEntity;renderLivingAt(Lnet/minecraft/entity/EntityLivingBase;DDD)V",
            shift = At.Shift.AFTER),
        require = 1)
    private void arsmagica2$setupShrinkRender(EntityLivingBase entity, double x, double y, double z, float yaw,
        float partialTicks, CallbackInfo ci) {
        RenderUtilities.setupShrinkRender(entity);
    }
}
