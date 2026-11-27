package am2.mixins.early;

import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import am2.utility.RedstoneInlayState;

/**
 * Replacement for the {@code isBlockIndirectlyGettingPowered} {@code @Fix} method.
 *
 * <p>
 * The Redstone Signal spell component writes an entry into {@link RedstoneInlayState}; this injection answers vanilla's
 * power query and consumes one tick of the inlay. The state is owned by normal code so the spell and the mixin cannot
 * drift apart.
 * </p>
 */
@Mixin(World.class)
public abstract class WorldRedstoneMixin {

    @Inject(method = "isBlockIndirectlyGettingPowered", at = @At("HEAD"), cancellable = true, require = 1)
    private void arsmagica2$redstoneInlay(int x, int y, int z, CallbackInfoReturnable<Boolean> cir) {
        if (RedstoneInlayState.isProvidingRedstone((World) (Object) this, x, y, z)) {
            cir.setReturnValue(true);
        }
    }
}
