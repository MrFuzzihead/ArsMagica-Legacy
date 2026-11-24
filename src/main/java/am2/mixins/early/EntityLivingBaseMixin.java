package am2.mixins.early;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import am2.utility.EtherealState;

/**
 * Replacements for the {@code EntityLivingBase} {@code @Fix} group: an ethereal player cannot be pushed, collided with
 * or stand on a ladder. Vanilla's own {@code isOnLadder} already defers to {@code ForgeHooks.isLivingOnLadder}, so the
 * old re-implementation is not needed.
 */
@Mixin(EntityLivingBase.class)
public abstract class EntityLivingBaseMixin {

    @Inject(method = "canBePushed", at = @At("HEAD"), cancellable = true, require = 1)
    private void arsmagica2$etherealCanBePushed(CallbackInfoReturnable<Boolean> cir) {
        if (isEtherealSelf()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "canBeCollidedWith", at = @At("HEAD"), cancellable = true, require = 1)
    private void arsmagica2$etherealCanBeCollidedWith(CallbackInfoReturnable<Boolean> cir) {
        if (isEtherealSelf()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "isOnLadder", at = @At("HEAD"), cancellable = true, require = 1)
    private void arsmagica2$etherealOnLadder(CallbackInfoReturnable<Boolean> cir) {
        if (isEtherealSelf()) {
            cir.setReturnValue(false);
        }
    }

    private boolean isEtherealSelf() {
        EntityLivingBase self = (EntityLivingBase) (Object) this;
        return self instanceof EntityPlayer && EtherealState.isEthereal((EntityPlayer) self);
    }
}
