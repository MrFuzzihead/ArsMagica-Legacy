package am2.mixins.early;

import net.minecraft.client.renderer.EntityRenderer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import am2.guis.AMGuiHelper;

/**
 * Replacements for the three {@code BytecodeTransformers.alterEntityRenderer} insertion points.
 *
 * <ul>
 * <li>{@code orientCamera} callout: shrink/flip vertical camera shift.</li>
 * <li>{@code Project.gluPerspective} callout: roll the view while flipped. The LWJGL target is not remapped.</li>
 * <li>{@code updateCameraAndRender}: replace the {@code Display.isActive()} boolean used by the mouse block. The
 * old transformer hard-coded a local index and branched for OptiFine; Mixin's implicit boolean capture avoids
 * the index and OptiFine support is removed.</li>
 * </ul>
 */
@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin {

    @Inject(
        method = "setupCameraTransform(FI)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/EntityRenderer;orientCamera(F)V",
            shift = At.Shift.AFTER),
        require = 1)
    private void arsmagica2$shiftView(float partialTicks, int pass, CallbackInfo ci) {
        AMGuiHelper.shiftView(partialTicks);
    }

    @Inject(
        method = "setupCameraTransform(FI)V",
        at = @At(
            value = "INVOKE",
            target = "Lorg/lwjgl/util/glu/Project;gluPerspective(FFFF)V",
            shift = At.Shift.AFTER,
            remap = false),
        require = 1)
    private void arsmagica2$flipView(float partialTicks, int pass, CallbackInfo ci) {
        AMGuiHelper.flipView(partialTicks);
    }

    @ModifyVariable(
        method = "updateCameraAndRender(F)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/profiler/Profiler;startSection(Ljava/lang/String;)V",
            ordinal = 1,
            shift = At.Shift.AFTER),
        require = 1)
    private boolean arsmagica2$overrideMouseInput(boolean flag, float partialTicks) {
        return AMGuiHelper.overrideMouseInput((EntityRenderer) (Object) this, partialTicks, flag);
    }
}
