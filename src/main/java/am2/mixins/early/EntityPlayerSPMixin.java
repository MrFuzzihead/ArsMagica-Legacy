package am2.mixins.early;

import net.minecraft.client.entity.EntityPlayerSP;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import am2.guis.AMGuiHelper;

/**
 * Replacement for {@code BytecodeTransformers.alterEntityPlayerSP}.
 *
 * <p>
 * {@code overrideKeyboardInput} only flips the movement input when the player is under the scramble-synapses effect, so
 * running it after vanilla's {@code updatePlayerMoveState()} is sufficient. Client-only.
 * </p>
 */
@Mixin(EntityPlayerSP.class)
public abstract class EntityPlayerSPMixin {

    @Inject(
        method = "onLivingUpdate",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/util/MovementInput;updatePlayerMoveState()V",
            shift = At.Shift.AFTER),
        require = 1)
    private void arsmagica2$overrideKeyboardInput(CallbackInfo ci) {
        AMGuiHelper.overrideKeyboardInput();
    }
}
