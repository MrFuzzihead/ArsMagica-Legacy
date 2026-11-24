package am2.mixins.early;

import static am2.mixins.MixinTestSupport.mixinTarget;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.lang.reflect.Method;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.monster.EntityCreeper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.potion.Potion;
import net.minecraft.util.DamageSource;
import net.minecraft.util.Timer;
import net.minecraftforge.oredict.ShapedOreRecipe;

import org.junit.Test;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Bytecode guard for the accessor/invoker mixins that replace {@code ReflectionHelper} calls.
 *
 * <p>
 * The target names here are MCP names. If a future change renames the accessor target, the refmap still builds but the
 * runtime lookup fails; this test pins the name and the target class so that change is caught headlessly.
 * </p>
 */
public class AccessorMixinTest {

    @Test
    public void entityInvokerTargetsFall() throws Exception {
        assertEquals(Entity.class.getName(), mixinTarget(EntityInvoker.class));

        Method method = EntityInvoker.class.getDeclaredMethod("am2$invokeFall", float.class);
        Invoker invoker = method.getAnnotation(Invoker.class);
        assertNotNull("@Invoker is missing", invoker);
        assertEquals("fall", invoker.value());
    }

    @Test
    public void entityPlayerAccessorTargetsSpeedInAir() throws Exception {
        assertEquals(EntityPlayer.class.getName(), mixinTarget(EntityPlayerAccessor.class));

        assertAccessor(EntityPlayerAccessor.class.getDeclaredMethod("am2$getSpeedInAir"), "speedInAir");
        assertAccessor(EntityPlayerAccessor.class.getDeclaredMethod("am2$setSpeedInAir", float.class), "speedInAir");
    }

    @Test
    public void entityLivingInvokerTargetsIsAIEnabled() throws Exception {
        assertEquals(EntityLiving.class.getName(), mixinTarget(EntityLivingInvoker.class));

        Method method = EntityLivingInvoker.class.getDeclaredMethod("am2$isAIEnabled");
        Invoker invoker = method.getAnnotation(Invoker.class);
        assertNotNull("@Invoker is missing", invoker);
        assertEquals("isAIEnabled", invoker.value());
    }

    @Test
    public void entityCreeperAccessorTargetsTimeSinceIgnited() throws Exception {
        assertEquals(EntityCreeper.class.getName(), mixinTarget(EntityCreeperAccessor.class));

        assertAccessor(
            EntityCreeperAccessor.class.getDeclaredMethod("am2$setTimeSinceIgnited", int.class),
            "timeSinceIgnited");
    }

    @Test
    public void potionAccessorTargetsIsBadEffect() throws Exception {
        assertEquals(Potion.class.getName(), mixinTarget(PotionAccessor.class));

        assertAccessor(PotionAccessor.class.getDeclaredMethod("am2$isBadEffect"), "isBadEffect");
    }

    @Test
    public void damageSourceAccessorTargetsIsUnblockable() throws Exception {
        assertEquals(DamageSource.class.getName(), mixinTarget(DamageSourceAccessor.class));

        assertAccessor(
            DamageSourceAccessor.class.getDeclaredMethod("am2$setUnblockable", boolean.class),
            "isUnblockable");
    }

    @Test
    public void shapedOreRecipeAccessorTargetsWidthAndHeight() throws Exception {
        assertEquals(ShapedOreRecipe.class.getName(), mixinTarget(ShapedOreRecipeAccessor.class));
        assertAccessor(ShapedOreRecipeAccessor.class.getDeclaredMethod("am2$getWidth"), "width");
        assertAccessor(ShapedOreRecipeAccessor.class.getDeclaredMethod("am2$getHeight"), "height");
    }

    @Test
    public void minecraftAccessorTargetsTimer() throws Exception {
        assertEquals(net.minecraft.client.Minecraft.class.getName(), mixinTarget(MinecraftAccessor.class));
        assertAccessor(MinecraftAccessor.class.getDeclaredMethod("am2$setTimer", Timer.class), "timer");
    }

    private static void assertAccessor(Method method, String expectedTarget) {
        Accessor accessor = method.getAnnotation(Accessor.class);
        assertNotNull("@Accessor is missing on " + method.getName(), accessor);
        assertEquals(expectedTarget, accessor.value());
    }
}
