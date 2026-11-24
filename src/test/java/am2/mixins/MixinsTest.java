package am2.mixins;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import com.gtnewhorizon.gtnhmixins.builders.AbstractBuilder;
import com.gtnewhorizon.gtnhmixins.builders.IBaseTransformer;
import com.gtnewhorizon.gtnhmixins.builders.MixinBuilder;

/**
 * Pins the Mixin registration wiring.
 *
 * <p>
 * {@code IMixins.getEarlyMixins} asks {@code FMLLaunchHandler} which physical side is running, and that is only set by
 * the FML tweaker, so a headless test cannot call it. The builder's protected metadata is read reflectively instead;
 * that is exactly the data GTNHMixins consumes.
 * </p>
 */
public class MixinsTest {

    @Test
    public void bothFeatureGroupsAreDeclared() {
        assertEquals(2, Mixins.values().length);
        assertEquals("POTION_IDS", Mixins.values()[0].name());
        assertEquals("REFLECTION_ACCESSORS", Mixins.values()[1].name());
    }

    @Test
    public void potionMixinIsRegisteredAsCommonEarlyContent() throws Exception {
        MixinBuilder builder = Mixins.POTION_IDS.getBuilder();
        assertNotNull(builder);

        assertEquals(IBaseTransformer.Phase.EARLY, read(builder, "phase"));

        @SuppressWarnings("unchecked")
        List<String> common = (List<String>) read(builder, "commonClasses");
        assertNotNull(common);
        assertEquals(1, common.size());
        assertTrue(common.contains("PotionMixin"));

        @SuppressWarnings("unchecked")
        List<String> client = (List<String>) read(builder, "clientClasses");
        assertTrue(client == null || client.isEmpty());
    }

    @Test
    public void reflectionAccessorsAreCommonEarlyContent() throws Exception {
        MixinBuilder builder = Mixins.REFLECTION_ACCESSORS.getBuilder();
        assertNotNull(builder);

        assertEquals(IBaseTransformer.Phase.EARLY, read(builder, "phase"));

        @SuppressWarnings("unchecked")
        List<String> common = (List<String>) read(builder, "commonClasses");
        assertNotNull(common);
        assertEquals(6, common.size());
        assertTrue(
            common.containsAll(
                Arrays.asList(
                    "EntityInvoker",
                    "EntityPlayerAccessor",
                    "EntityLivingInvoker",
                    "EntityCreeperAccessor",
                    "PotionAccessor",
                    "DamageSourceAccessor")));
    }

    @Test
    public void loaderPointsAtTheEarlyMixinConfig() {
        EarlyMixinsLoader loader = new EarlyMixinsLoader();
        assertEquals("mixins.arsmagica2.early.json", loader.getMixinConfig());
        assertNotNull(loader.getASMTransformerClass());
        assertEquals(0, loader.getASMTransformerClass().length);
        assertNull(loader.getAccessTransformerClass());
        assertNull(loader.getModContainerClass());
        assertNull(loader.getSetupClass());
    }

    private static Object read(Object target, String fieldName) throws Exception {
        Field field = AbstractBuilder.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(target);
    }
}
