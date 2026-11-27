package am2.mixins.early;

import static am2.mixins.MixinTestSupport.mixinTarget;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Method;

import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.renderer.entity.RendererLivingEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;

import org.junit.Test;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Bytecode guards for the core transforms ported from {@code BytecodeTransformers}.
 *
 * <p>
 * These pin the target method, the injection point and the {@code require} contract. A mixin that compiles but silently
 * misses its target is exactly the failure mode that made the old coremod inert.
 * </p>
 */
public class CoreTransformMixinTest {

    @Test
    public void serverTickrateReplacesTheFiftyMillisecondConstant() throws Exception {
        assertEquals(MinecraftServer.class.getName(), mixinTarget(MinecraftServerMixin.class));

        Method handler = MinecraftServerMixin.class.getDeclaredMethod("arsmagica2$serverTickLength", long.class);
        ModifyConstant modify = handler.getAnnotation(ModifyConstant.class);
        assertNotNull("@ModifyConstant is missing", modify);
        assertEquals(1, modify.method().length);
        assertEquals("run", modify.method()[0]);
        assertEquals(1, modify.require());

        Constant[] constants = modify.constant();
        assertEquals(1, constants.length);
        assertEquals(50L, constants[0].longValue());
    }

    @Test
    public void soundPitchModifiesThePitchParameterAfterTheEventPost() throws Exception {
        assertEquals(World.class.getName(), mixinTarget(WorldMixin.class));

        Method handler = WorldMixin.class.getDeclaredMethod("arsmagica2$modifySoundPitch", float.class, Entity.class);
        ModifyVariable modify = handler.getAnnotation(ModifyVariable.class);
        assertNotNull("@ModifyVariable is missing", modify);
        assertEquals(1, modify.method().length);
        assertEquals("playSoundAtEntity", modify.method()[0]);
        assertEquals(4, modify.index());
        assertEquals(1, modify.require());

        At at = modify.at();
        assertEquals("INVOKE", at.value());
        assertTrue(
            at.target(),
            at.target()
                .contains("EventBus;post"));
        assertEquals(At.Shift.AFTER, at.shift());
        assertFalse("the Forge EventBus target must not be remapped", at.remap());
    }

    @Test
    public void entityRendererShiftRunsAfterOrientCamera() throws Exception {
        assertEquals(EntityRenderer.class.getName(), mixinTarget(EntityRendererMixin.class));

        Inject inject = inject(EntityRendererMixin.class, "arsmagica2$shiftView", float.class, int.class);
        assertEquals(1, inject.require());
        assertTrue(
            inject.at()[0].target(),
            inject.at()[0].target()
                .contains("EntityRenderer;orientCamera(F)V"));
        assertEquals(At.Shift.AFTER, inject.at()[0].shift());
    }

    @Test
    public void entityRendererFlipRunsAfterGluPerspective() throws Exception {
        Inject inject = inject(EntityRendererMixin.class, "arsmagica2$flipView", float.class, int.class);
        assertEquals(1, inject.require());
        assertEquals("Lorg/lwjgl/util/glu/Project;gluPerspective(FFFF)V", inject.at()[0].target());
        assertFalse("LWJGL is not remapped", inject.at()[0].remap());
    }

    @Test
    public void entityRendererMouseOverrideReplacesTheActiveFlag() throws Exception {
        Method handler = EntityRendererMixin.class
            .getDeclaredMethod("arsmagica2$overrideMouseInput", boolean.class, float.class);
        ModifyVariable modify = handler.getAnnotation(ModifyVariable.class);
        assertNotNull("@ModifyVariable is missing", modify);
        assertEquals("updateCameraAndRender(F)V", modify.method()[0]);
        assertEquals(1, modify.require());

        At at = modify.at();
        assertEquals("INVOKE", at.value());
        assertTrue(
            at.target(),
            at.target()
                .contains("Profiler;startSection"));
        assertEquals(1, at.ordinal());
        assertEquals(At.Shift.AFTER, at.shift());
    }

    @Test
    public void entityPlayerSpOverridesKeyboardInputAfterVanillaUpdate() throws Exception {
        assertEquals(EntityPlayerSP.class.getName(), mixinTarget(EntityPlayerSPMixin.class));

        Inject inject = inject(EntityPlayerSPMixin.class, "arsmagica2$overrideKeyboardInput");
        assertEquals(1, inject.require());
        assertTrue(
            inject.at()[0].target(),
            inject.at()[0].target()
                .contains("MovementInput;updatePlayerMoveState()V"));
        assertEquals(At.Shift.AFTER, inject.at()[0].shift());
    }

    @Test
    public void rendererLivingEntityAppliesShrinkAfterRenderLivingAt() throws Exception {
        assertEquals(RendererLivingEntity.class.getName(), mixinTarget(RendererLivingEntityMixin.class));

        Inject inject = inject(
            RendererLivingEntityMixin.class,
            "arsmagica2$setupShrinkRender",
            EntityLivingBase.class,
            double.class,
            double.class,
            double.class,
            float.class,
            float.class);
        assertEquals(1, inject.require());
        assertTrue(
            inject.at()[0].target(),
            inject.at()[0].target()
                .contains("RendererLivingEntity;renderLivingAt"));
        assertEquals(At.Shift.AFTER, inject.at()[0].shift());
    }

    @Test
    public void entityRendererAccessorExposesTheSmoothCameraFields() throws Exception {
        assertEquals(EntityRenderer.class.getName(), mixinTarget(EntityRendererAccessor.class));

        assertAccessor("am2$getSmoothCamYaw", "smoothCamYaw");
        assertAccessor("am2$setSmoothCamYaw", "smoothCamYaw", float.class);
        assertAccessor("am2$getSmoothCamPitch", "smoothCamPitch");
        assertAccessor("am2$setSmoothCamPitch", "smoothCamPitch", float.class);
        assertAccessor("am2$getSmoothCamPartialTicks", "smoothCamPartialTicks");
        assertAccessor("am2$setSmoothCamPartialTicks", "smoothCamPartialTicks", float.class);
        assertAccessor("am2$getSmoothCamFilterX", "smoothCamFilterX");
        assertAccessor("am2$getSmoothCamFilterY", "smoothCamFilterY");
    }

    private static Inject inject(Class<?> mixinClass, String handlerName, Class<?>... parameterTypes) throws Exception {
        // @Inject handlers append the CallbackInfo parameter after the target method arguments.
        Class<?>[] signature = new Class<?>[parameterTypes.length + 1];
        System.arraycopy(parameterTypes, 0, signature, 0, parameterTypes.length);
        signature[parameterTypes.length] = CallbackInfo.class;

        Method handler = mixinClass.getDeclaredMethod(handlerName, signature);
        Inject inject = handler.getAnnotation(Inject.class);
        assertNotNull("@Inject is missing on " + handlerName, inject);
        assertEquals(1, inject.at().length);
        return inject;
    }

    private static void assertAccessor(String methodName, String expectedTarget, Class<?>... parameterTypes)
        throws Exception {
        Method method = EntityRendererAccessor.class.getDeclaredMethod(methodName, parameterTypes);
        Accessor accessor = method.getAnnotation(Accessor.class);
        assertNotNull("@Accessor is missing on " + methodName, accessor);
        assertEquals(expectedTarget, accessor.value());
    }
}
