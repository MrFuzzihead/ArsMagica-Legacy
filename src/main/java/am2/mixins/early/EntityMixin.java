package am2.mixins.early;

import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import am2.affinity.AffinityHelper;
import am2.utility.EtherealState;

/**
 * Replacements for the {@code Entity} {@code @Fix} group: no-clip collision, ethereal water/collision behaviour,
 * true-sight invisibility and the Archmage leggings' water-affinity exceptions.
 */
@Mixin(Entity.class)
public abstract class EntityMixin {

    @Inject(method = "isEntityInsideOpaqueBlock", at = @At("RETURN"), cancellable = true, require = 1)
    private void arsmagica2$noClipOverridesOpaqueBlock(CallbackInfoReturnable<Boolean> cir) {
        if (((Entity) (Object) this).noClip) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "handleWaterMovement", at = @At("HEAD"), cancellable = true, require = 1)
    private void arsmagica2$etherealWaterMovement(CallbackInfoReturnable<Boolean> cir) {
        Entity self = (Entity) (Object) this;
        if (self instanceof EntityPlayer && EtherealState.isEthereal((EntityPlayer) self)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "applyEntityCollision", at = @At("HEAD"), cancellable = true, require = 1)
    private void arsmagica2$etherealCollision(Entity other, CallbackInfo ci) {
        if (other instanceof EntityPlayer && EtherealState.isEthereal((EntityPlayer) other)) {
            ci.cancel();
        }
    }

    @Inject(method = "isInsideOfMaterial", at = @At("HEAD"), cancellable = true, require = 1)
    private void arsmagica2$waterAffinityMaterial(Material material, CallbackInfoReturnable<Boolean> cir) {
        Entity self = (Entity) (Object) this;
        if (self instanceof EntityPlayer && material != null
            && material.isLiquid()
            && AffinityHelper.isNotInWaterActually.contains(self)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "isInWater", at = @At("HEAD"), cancellable = true, require = 1)
    private void arsmagica2$waterAffinity(CallbackInfoReturnable<Boolean> cir) {
        Entity self = (Entity) (Object) this;
        if (self instanceof EntityPlayer && AffinityHelper.isNotInWaterActually.contains(self)) {
            cir.setReturnValue(false);
        }
    }
}
