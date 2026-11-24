package am2.mixins.early;

import static am2.mixins.MixinTestSupport.inject;
import static am2.mixins.MixinTestSupport.mixinTarget;
import static org.junit.Assert.assertEquals;

import net.minecraft.client.model.ModelMagmaCube;
import net.minecraft.client.model.ModelSkeleton;
import net.minecraft.client.particle.EffectRenderer;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.Vec3;

import org.junit.Test;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Bytecode guards for the client render {@code @Fix} group: ethereal camera distance, cloaked particles and
 * hallucination models.
 */
public class ClientFixMixinTest {

    @Test
    public void vec3DistanceIsInterceptedForTheEtherealCamera() throws Exception {
        assertEquals(Vec3.class.getName(), mixinTarget(Vec3Mixin.class));
        assertInject(
            Vec3Mixin.class,
            "arsmagica2$etherealCameraDistance",
            "distanceTo",
            "HEAD",
            Vec3.class,
            CallbackInfoReturnable.class);
    }

    @Test
    public void cloakingHidesParticles() throws Exception {
        assertEquals(EffectRenderer.class.getName(), mixinTarget(EffectRendererMixin.class));
        assertInject(
            EffectRendererMixin.class,
            "arsmagica2$hideParticlesWhileCloaked",
            "renderParticles",
            "HEAD",
            Entity.class,
            float.class,
            CallbackInfo.class);
    }

    @Test
    public void magmaCubeSkipsHallucinationAnimations() throws Exception {
        assertEquals(ModelMagmaCube.class.getName(), mixinTarget(ModelMagmaCubeMixin.class));
        assertLivingAnimations(ModelMagmaCubeMixin.class);
    }

    @Test
    public void skeletonSkipsHallucinationAnimations() throws Exception {
        assertEquals(ModelSkeleton.class.getName(), mixinTarget(ModelSkeletonMixin.class));
        assertLivingAnimations(ModelSkeletonMixin.class);
    }

    @Test
    public void entityRendererFlagsTheEtherealCameraWindow() throws Exception {
        assertEquals(EntityRenderer.class.getName(), mixinTarget(EntityRendererMixin.class));

        assertInject(
            EntityRendererMixin.class,
            "arsmagica2$beginEtherealCamera",
            "orientCamera(F)V",
            "HEAD",
            float.class,
            CallbackInfo.class);
        assertInject(
            EntityRendererMixin.class,
            "arsmagica2$endEtherealCamera",
            "orientCamera(F)V",
            "RETURN",
            float.class,
            CallbackInfo.class);
    }

    private static void assertLivingAnimations(Class<?> mixinClass) throws Exception {
        assertInject(
            mixinClass,
            "arsmagica2$skipHallucinationAnimations",
            "setLivingAnimations",
            "HEAD",
            EntityLivingBase.class,
            float.class,
            float.class,
            float.class,
            CallbackInfo.class);
    }

    private static void assertInject(Class<?> mixinClass, String handlerName, String targetMethod, String atValue,
        Class<?>... parameterTypes) throws Exception {
        Inject inject = inject(mixinClass, handlerName, parameterTypes);
        assertEquals(1, inject.require());
        assertEquals(1, inject.method().length);
        assertEquals(targetMethod, inject.method()[0]);
        assertEquals(1, inject.at().length);
        assertEquals(atValue, inject.at()[0].value());
    }
}
