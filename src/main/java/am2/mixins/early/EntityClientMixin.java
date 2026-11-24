package am2.mixins.early;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import am2.buffs.BuffList;
import am2.utility.EtherealState;

/**
 * Client half of the ethereal {@code Entity} fixes.
 *
 * <p>
 * {@code Entity.isInvisibleToPlayer} is annotated {@code @SideOnly(Side.CLIENT)} and is stripped from the dedicated
 * server class by FML, so this handler cannot live in the common {@code EntityMixin}. A player under True Sight (or
 * ethereal) sees invisible entities.
 * </p>
 */
@Mixin(Entity.class)
public abstract class EntityClientMixin {

    @Inject(method = "isInvisibleToPlayer", at = @At("HEAD"), cancellable = true, require = 1)
    private void arsmagica2$trueSightSeesInvisible(EntityPlayer viewer, CallbackInfoReturnable<Boolean> cir) {
        if (viewer != null && (viewer.isPotionActive(BuffList.trueSight.id) || EtherealState.isEthereal(viewer))) {
            cir.setReturnValue(false);
        }
    }
}
