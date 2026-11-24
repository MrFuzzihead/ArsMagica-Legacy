package am2.mixins.early;

import net.minecraft.client.renderer.EntityRenderer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Replaces the five {@code ReflectionHelper} smooth-camera accesses in {@code AMGuiHelper.overrideMouseInput}. The
 * fields are private and the public API cannot expose them. Client-only.
 */
@Mixin(EntityRenderer.class)
public interface EntityRendererAccessor {

    @Accessor("smoothCamYaw")
    float am2$getSmoothCamYaw();

    @Accessor("smoothCamYaw")
    void am2$setSmoothCamYaw(float value);

    @Accessor("smoothCamPitch")
    float am2$getSmoothCamPitch();

    @Accessor("smoothCamPitch")
    void am2$setSmoothCamPitch(float value);

    @Accessor("smoothCamPartialTicks")
    float am2$getSmoothCamPartialTicks();

    @Accessor("smoothCamPartialTicks")
    void am2$setSmoothCamPartialTicks(float value);

    @Accessor("smoothCamFilterX")
    float am2$getSmoothCamFilterX();

    @Accessor("smoothCamFilterY")
    float am2$getSmoothCamFilterY();
}
