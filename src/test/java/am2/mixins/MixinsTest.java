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
    public void allFeatureGroupsAreDeclaredInOrder() {
        assertEquals(12, Mixins.values().length);
        assertEquals("POTION_IDS", Mixins.values()[0].name());
        assertEquals("REFLECTION_ACCESSORS", Mixins.values()[1].name());
        assertEquals("TICKRATE", Mixins.values()[2].name());
        assertEquals("SOUND_PITCH", Mixins.values()[3].name());
        assertEquals("SHRINK_FLIP", Mixins.values()[4].name());
        assertEquals("ETHEREAL", Mixins.values()[5].name());
        assertEquals("REDSTONE_INLAY", Mixins.values()[6].name());
        assertEquals("WATER_ETHEREAL", Mixins.values()[7].name());
        assertEquals("LAVA_ESSENCE", Mixins.values()[8].name());
        assertEquals("CLIENT_ETHEREAL", Mixins.values()[9].name());
        assertEquals("CLIENT_RENDER", Mixins.values()[10].name());
        assertEquals("CLIENT_ACCESSORS", Mixins.values()[11].name());
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
        assertEquals(7, common.size());
        assertTrue(
            common.containsAll(
                Arrays.asList(
                    "EntityInvoker",
                    "EntityPlayerAccessor",
                    "EntityLivingInvoker",
                    "EntityCreeperAccessor",
                    "PotionAccessor",
                    "DamageSourceAccessor",
                    "TileEntityAccessor")));
    }

    @Test
    public void serverSideCoreTransformsAreCommon() throws Exception {
        assertCommonEarly(Mixins.TICKRATE, "MinecraftServerMixin");
        assertCommonEarly(Mixins.SOUND_PITCH, "WorldMixin");
    }

    @Test
    public void clientRenderTransformsAreClientOnly() throws Exception {
        MixinBuilder builder = Mixins.SHRINK_FLIP.getBuilder();
        assertNotNull(builder);
        assertEquals(IBaseTransformer.Phase.EARLY, read(builder, "phase"));

        @SuppressWarnings("unchecked")
        List<String> client = (List<String>) read(builder, "clientClasses");
        assertNotNull(client);
        assertEquals(4, client.size());
        assertTrue(
            client.containsAll(
                Arrays.asList(
                    "EntityRendererMixin",
                    "EntityRendererAccessor",
                    "EntityPlayerSPMixin",
                    "RendererLivingEntityMixin")));

        @SuppressWarnings("unchecked")
        List<String> common = (List<String>) read(builder, "commonClasses");
        assertTrue(common == null || common.isEmpty());
    }

    @Test
    public void etherealFixesAreCommon() throws Exception {
        MixinBuilder builder = Mixins.ETHEREAL.getBuilder();
        assertNotNull(builder);
        assertEquals(IBaseTransformer.Phase.EARLY, read(builder, "phase"));

        @SuppressWarnings("unchecked")
        List<String> common = (List<String>) read(builder, "commonClasses");
        assertNotNull(common);
        assertEquals(4, common.size());
        assertTrue(
            common.containsAll(
                Arrays.asList(
                    "WorldEntityQueryMixin",
                    "EntityMixin",
                    "EntityLivingBaseMixin",
                    "NetHandlerPlayServerMixin")));
    }

    @Test
    public void elementalFixesAreCommon() throws Exception {
        assertCommonEarly(Mixins.REDSTONE_INLAY, "WorldRedstoneMixin");
        assertCommonEarly(Mixins.WATER_ETHEREAL, "BlockLiquidMixin");

        MixinBuilder lava = Mixins.LAVA_ESSENCE.getBuilder();
        assertNotNull(lava);
        assertEquals(IBaseTransformer.Phase.EARLY, read(lava, "phase"));

        @SuppressWarnings("unchecked")
        List<String> common = (List<String>) read(lava, "commonClasses");
        assertNotNull(common);
        assertEquals(2, common.size());
        assertTrue(common.containsAll(Arrays.asList("BlockDynamicLiquidMixin", "BlockLiquidInvoker")));
    }

    @Test
    public void clientEtherealVisibilityIsClientOnly() throws Exception {
        MixinBuilder builder = Mixins.CLIENT_ETHEREAL.getBuilder();
        assertNotNull(builder);
        assertEquals(IBaseTransformer.Phase.EARLY, read(builder, "phase"));

        @SuppressWarnings("unchecked")
        List<String> client = (List<String>) read(builder, "clientClasses");
        assertNotNull(client);
        assertEquals(1, client.size());
        assertTrue(client.contains("EntityClientMixin"));
    }

    @Test
    public void clientRenderFixesAreClientOnly() throws Exception {
        MixinBuilder builder = Mixins.CLIENT_RENDER.getBuilder();
        assertNotNull(builder);
        assertEquals(IBaseTransformer.Phase.EARLY, read(builder, "phase"));

        @SuppressWarnings("unchecked")
        List<String> client = (List<String>) read(builder, "clientClasses");
        assertNotNull(client);
        assertEquals(5, client.size());
        assertTrue(
            client.containsAll(
                Arrays.asList(
                    "Vec3Mixin",
                    "EffectRendererMixin",
                    "ModelMagmaCubeMixin",
                    "ModelSkeletonMixin",
                    "RenderManagerMixin")));
    }

    @Test
    public void clientAccessorsAreClientOnly() throws Exception {
        MixinBuilder builder = Mixins.CLIENT_ACCESSORS.getBuilder();
        assertNotNull(builder);
        assertEquals(IBaseTransformer.Phase.EARLY, read(builder, "phase"));

        @SuppressWarnings("unchecked")
        List<String> client = (List<String>) read(builder, "clientClasses");
        assertNotNull(client);
        assertEquals(2, client.size());
        assertTrue(client.containsAll(Arrays.asList("ShapedOreRecipeAccessor", "MinecraftAccessor")));
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

    private static void assertCommonEarly(Mixins group, String mixinName) throws Exception {
        MixinBuilder builder = group.getBuilder();
        assertNotNull(builder);
        assertEquals(IBaseTransformer.Phase.EARLY, read(builder, "phase"));

        @SuppressWarnings("unchecked")
        List<String> common = (List<String>) read(builder, "commonClasses");
        assertNotNull(common);
        assertEquals(1, common.size());
        assertTrue(common.contains(mixinName));
    }

    private static Object read(Object target, String fieldName) throws Exception {
        Field field = AbstractBuilder.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(target);
    }
}
