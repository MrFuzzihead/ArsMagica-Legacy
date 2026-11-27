package am2.mixins.early;

import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import am2.entities.renderers.ClientPlayerModelMap;

/**
 * Replacement for the {@code RenderManager.getEntityRenderObject} {@code @Fix}. Players whose synchronized model name
 * starts with {@code maid} use the special Alex-style player renderer. Client-only.
 */
@Mixin(RenderManager.class)
public abstract class RenderManagerMixin {

    @Inject(method = "getEntityRenderObject", at = @At("RETURN"), cancellable = true, require = 1)
    private void arsmagica2$specialPlayerRenderer(Entity entity, CallbackInfoReturnable<Render> cir) {
        if (entity instanceof EntityPlayer && ClientPlayerModelMap.isSpecialModel((EntityPlayer) entity)) {
            cir.setReturnValue(ClientPlayerModelMap.getSpecialRenderer());
        }
    }
}
