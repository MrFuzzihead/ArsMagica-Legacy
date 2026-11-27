package am2.mixins.early;

import net.minecraft.block.BlockLiquid;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import am2.affinity.AffinityHelper;

/**
 * Replacement for the {@code BlockLiquid.velocityToAddToEntity} {@code @Fix}. A player wearing the Archmage leggings
 * (recorded in {@link AffinityHelper#isNotInWaterActually}) is not pulled by liquid currents.
 */
@Mixin(BlockLiquid.class)
public abstract class BlockLiquidMixin {

    @Inject(method = "velocityToAddToEntity", at = @At("HEAD"), cancellable = true, require = 1)
    private void arsmagica2$waterAffinityVelocity(World world, int x, int y, int z, Entity entity, Vec3 velocity,
        CallbackInfo ci) {
        if (entity instanceof EntityPlayer && AffinityHelper.isNotInWaterActually.contains(entity)) {
            ci.cancel();
        }
    }
}
