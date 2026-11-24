package am2.mixins.early;

import net.minecraft.client.model.ModelMagmaCube;
import net.minecraft.entity.EntityLivingBase;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import am2.entities.EntityHallucination;

/**
 * Replacement for the {@code ModelMagmaCube.setLivingAnimations} {@code @Fix}: hallucination entities use the
 * vanilla model without the magma-cube animation. Client-only.
 */
@Mixin(ModelMagmaCube.class)
public abstract class ModelMagmaCubeMixin {

    @Inject(method = "setLivingAnimations", at = @At("HEAD"), cancellable = true, require = 1)
    private void arsmagica2$skipHallucinationAnimations(EntityLivingBase entity, float limbSwing, float limbSwingAmount,
        float partialTicks, CallbackInfo ci) {
        if (entity instanceof EntityHallucination) {
            ci.cancel();
        }
    }
}
