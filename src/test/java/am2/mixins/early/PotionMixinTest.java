package am2.mixins.early;

import static am2.mixins.MixinTestSupport.mixinTarget;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import net.minecraft.potion.Potion;

import org.junit.Test;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Bytecode guard for {@link PotionMixin}.
 *
 * <p>
 * A Mixin that compiles but no longer targets the intended method is exactly how the old coremod became inert. The
 * class-level {@code @Mixin} annotation is {@code CLASS} retention, so it is read with ASM; the handler annotations
 * are {@code RUNTIME} and are read directly.
 * </p>
 */
public class PotionMixinTest {

    @Test
    public void mixinTargetsPotion() throws Exception {
        assertEquals("net.minecraft.potion.Potion", mixinTarget(PotionMixin.class));
    }

    @Test
    public void handlerInjectsAtTheEndOfPotionStaticInitializer() throws Exception {
        Method handler = PotionMixin.class.getDeclaredMethod("arsmagica2$growPotionArray", CallbackInfo.class);
        Inject inject = handler.getAnnotation(Inject.class);
        assertNotNull("@Inject is missing", inject);
        assertEquals(1, inject.require());
        assertEquals(1, inject.method().length);
        assertEquals("<clinit>", inject.method()[0]);

        At[] at = inject.at();
        assertEquals(1, at.length);
        assertEquals("TAIL", at[0].value());
    }

    @Test
    public void shadowedPotionArrayIsWritable() throws Exception {
        Field field = PotionMixin.class.getDeclaredField("potionTypes");
        assertNotNull("@Shadow is missing", field.getAnnotation(Shadow.class));
        assertNotNull("@Final is missing", field.getAnnotation(Final.class));
        assertNotNull("@Mutable is missing", field.getAnnotation(Mutable.class));
        assertEquals(Potion[].class, field.getType());
    }

    @Test
    public void arraySizeStaysWithinTheByteSafeRange() throws Exception {
        Field field = PotionMixin.class.getDeclaredField("ARSMAGICA2_POTION_ARRAY_SIZE");
        field.setAccessible(true);
        int size = field.getInt(null);
        assertTrue("potion IDs above 127 truncate as a byte", size == 128);
        assertEquals(128, size);
    }
}
