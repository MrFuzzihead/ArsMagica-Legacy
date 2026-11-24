package am2.mixins.early;

import net.minecraft.util.Vec3;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import am2.utility.ClientEtherealState;

/**
 * Replacement for the {@code Vec3.distanceTo} {@code @Fix}.
 *
 * <p>
 * While the local camera is orienting for an ethereal player, distance checks must not clamp the third-person camera
 * against blocks. Returning {@link Double#MAX_VALUE} makes vanilla's camera collision code skip the obstruction.
 * Client-only: {@link ClientEtherealState} is a client class.
 * </p>
 */
@Mixin(Vec3.class)
public abstract class Vec3Mixin {

    @Inject(method = "distanceTo", at = @At("HEAD"), cancellable = true, require = 1)
    private void arsmagica2$etherealCameraDistance(Vec3 other, CallbackInfoReturnable<Double> cir) {
        if (ClientEtherealState.isEtherealCamera()) {
            cir.setReturnValue(Double.MAX_VALUE);
        }
    }
}
