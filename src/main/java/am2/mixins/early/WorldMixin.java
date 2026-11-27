package am2.mixins.early;

import net.minecraft.entity.Entity;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import am2.utility.EntityUtilities;

/**
 * Replacement for {@code BytecodeTransformers.alterWorld}.
 *
 * <p>
 * Vanilla builds the {@code PlaySoundAtEntityEvent}, posts it, then reads {@code event.name} before the world-access
 * loop. The pitch local (parameter 4) is rewritten after the event post so event listeners still see the original
 * pitch and only playback is modified, matching the old transformer's insertion point.
 * </p>
 */
@Mixin(World.class)
public abstract class WorldMixin {

    @ModifyVariable(
        method = "playSoundAtEntity",
        at = @At(
            value = "INVOKE",
            target = "Lcpw/mods/fml/common/eventhandler/EventBus;post(Lcpw/mods/fml/common/eventhandler/Event;)Z",
            shift = At.Shift.AFTER,
            remap = false),
        index = 4,
        require = 1)
    private float arsmagica2$modifySoundPitch(float pitch, Entity entity) {
        return EntityUtilities.modifySoundPitch(entity, pitch);
    }
}
