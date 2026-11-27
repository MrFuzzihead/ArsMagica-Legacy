package am2.entities.renderers;

import net.minecraft.client.renderer.entity.RenderFish;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.EntityFishHook;
import net.minecraft.util.ResourceLocation;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Renderer for the arcane/infernal fishing hooks. Replaces the global {@code RenderFish.getEntityTexture} and
 * {@code Render.doRenderShadowAndFire} coremod patches with a per-entity renderer.
 */
@SideOnly(Side.CLIENT)
public class RenderFishingHook extends RenderFish {

    private final ResourceLocation texture;

    public RenderFishingHook(ResourceLocation texture) {
        this.texture = texture;
    }

    @Override
    protected ResourceLocation getEntityTexture(EntityFishHook hook) {
        return texture;
    }

    @Override
    public void doRenderShadowAndFire(Entity entity, double x, double y, double z, float yaw, float partialTicks) {
        // The custom hook textures do not need the vanilla hook shadow/fire overlay.
    }
}
