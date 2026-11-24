package am2.mixins.early;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.command.IEntitySelector;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import am2.utility.EtherealState;

/**
 * Replacement for the three entity-query {@code @Fix} methods in {@code MysteriumPatchesFixesMagicka}.
 *
 * <p>
 * Ethereal players (soulspike boots) must not be visible to entity targeting, spell impacts, or
 * {@code getClosestPlayer}. Filtering after the vanilla query keeps the original selectors and AABB logic intact.
 * </p>
 */
@Mixin(World.class)
public abstract class WorldEntityQueryMixin {

    @Inject(method = "selectEntitiesWithinAABB", at = @At("RETURN"), cancellable = true, require = 1)
    private void arsmagica2$filterEtherealEntities(Class<?> entityClass, AxisAlignedBB aabb, IEntitySelector selector,
        CallbackInfoReturnable<List> cir) {
        cir.setReturnValue(filterEthereal(cir.getReturnValue()));
    }

    @Inject(
        method = "getEntitiesWithinAABBExcludingEntity(Lnet/minecraft/entity/Entity;Lnet/minecraft/util/AxisAlignedBB;Lnet/minecraft/command/IEntitySelector;)Ljava/util/List;",
        at = @At("RETURN"),
        cancellable = true,
        require = 1)
    private void arsmagica2$filterEtherealEntitiesExcluding(CallbackInfoReturnable<List> cir) {
        cir.setReturnValue(filterEthereal(cir.getReturnValue()));
    }

    @Inject(method = "getClosestPlayer", at = @At("RETURN"), cancellable = true, require = 1)
    private void arsmagica2$filterEtherealClosestPlayer(CallbackInfoReturnable<EntityPlayer> cir) {
        if (EtherealState.isEthereal(cir.getReturnValue())) {
            cir.setReturnValue(null);
        }
    }

    private static List filterEthereal(List original) {
        if (original == null || original.isEmpty()) {
            return original;
        }

        List filtered = new ArrayList(original.size());
        for (Object entry : original) {
            if (!(entry instanceof EntityPlayer) || !EtherealState.isEthereal((EntityPlayer) entry)) {
                filtered.add(entry);
            }
        }
        return filtered;
    }
}
