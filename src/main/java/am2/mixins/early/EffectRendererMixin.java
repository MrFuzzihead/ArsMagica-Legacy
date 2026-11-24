package am2.mixins.early;

import net.minecraft.client.particle.EffectRenderer;
import net.minecraft.entity.Entity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import am2.network.AMPacketProcessorClient;

/**
 * Replacement for the {@code EffectRenderer.renderParticles} {@code @Fix}: a cloaked player does not render
 * particles. Client-only.
 */
@Mixin(EffectRenderer.class)
public abstract class EffectRendererMixin {

    @Inject(method = "renderParticles", at = @At("HEAD"), cancellable = true, require = 1)
    private void arsmagica2$hideParticlesWhileCloaked(Entity entity, float partialTicks, CallbackInfo ci) {
        if (AMPacketProcessorClient.cloaking > 0) {
            ci.cancel();
        }
    }
}
