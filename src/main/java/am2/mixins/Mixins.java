package am2.mixins;

import javax.annotation.Nonnull;

import com.gtnewhorizon.gtnhmixins.builders.IMixins;
import com.gtnewhorizon.gtnhmixins.builders.MixinBuilder;

/**
 * Mixin registration for Ars Magica 2.
 *
 * <p>
 * Each entry is a feature group so a group can be enabled, side-gated or phase-gated independently. The class name
 * strings are relative to the phase package declared in {@code mixins.arsmagica2.early.json} /
 * {@code mixins.arsmagica2.late.json}. Client-only targets must stay in {@code addClientMixins} so a dedicated server
 * never loads client classes.
 * </p>
 */
public enum Mixins implements IMixins {

    // Grows Potion.potionTypes to 128 so the mod's own buff IDs stay below the byte-safe limit of 127. This is also
    // the smoke mixin that proves the Mixin loader, manifest and refmap are wired up.
    POTION_IDS(new MixinBuilder().setPhase(Phase.EARLY)
        .addCommonMixins("PotionMixin")),

    // Accessors and invokers that replace ReflectionHelper calls from normal AM2 code. They are common because the
    // replaced calls (entity physics, potion classification, damage sources) run on both sides.
    REFLECTION_ACCESSORS(new MixinBuilder().setPhase(Phase.EARLY)
        .addCommonMixins(
            "EntityInvoker",
            "EntityPlayerAccessor",
            "EntityLivingInvoker",
            "EntityCreeperAccessor",
            "PotionAccessor",
            "DamageSourceAccessor")),

    // Replaces BytecodeTransformers.alterServerTickrate. The tick length is owned by TickrateController.
    TICKRATE(new MixinBuilder().setPhase(Phase.EARLY)
        .addCommonMixins("MinecraftServerMixin")),

    // Replaces BytecodeTransformers.alterWorld (shrink potion changes the sound pitch).
    SOUND_PITCH(new MixinBuilder().setPhase(Phase.EARLY)
        .addCommonMixins("WorldMixin")),

    // Replaces BytecodeTransformers.alterEntityRenderer, alterRendererLivingEntity and alterEntityPlayerSP. Everything
    // here touches client render/input classes, so it must not be loaded on a dedicated server.
    SHRINK_FLIP(new MixinBuilder().setPhase(Phase.EARLY)
        .addClientMixins(
            "EntityRendererMixin",
            "EntityRendererAccessor",
            "EntityPlayerSPMixin",
            "RendererLivingEntityMixin")),

    // Replaces the ethereal @Fix group: entity-query filtering, no-clip collision, water movement, true sight and the
    // ethereal inventory-click guard. All targets are common classes.
    ETHEREAL(new MixinBuilder().setPhase(Phase.EARLY)
        .addCommonMixins("WorldEntityQueryMixin", "EntityMixin", "EntityLivingBaseMixin", "NetHandlerPlayServerMixin")),

    // Replaces the isBlockIndirectlyGettingPowered @Fix used by the Redstone Signal spell component.
    REDSTONE_INLAY(new MixinBuilder().setPhase(Phase.EARLY)
        .addCommonMixins("WorldRedstoneMixin")),

    // Replaces the water-affinity parts of the Entity/BlockLiquid @Fix group (Archmage leggings).
    WATER_ETHEREAL(new MixinBuilder().setPhase(Phase.EARLY)
        .addCommonMixins("BlockLiquidMixin")),

    // Replaces the BlockDynamicLiquid.updateTick @Fix (lava + liquid essence).
    LAVA_ESSENCE(new MixinBuilder().setPhase(Phase.EARLY)
        .addCommonMixins("BlockDynamicLiquidMixin", "BlockLiquidInvoker")),

    // The True Sight invisibility override targets a @SideOnly(CLIENT) method that is stripped on a dedicated server.
    CLIENT_ETHEREAL(new MixinBuilder().setPhase(Phase.EARLY)
        .addClientMixins("EntityClientMixin"));

    // Deliberately not ported: BytecodeTransformers.alterNetHandlerPlayServer overwrote the per-player stance
    // difference with 1.5, which disables vanilla's illegal-stance check for every player. The old coremod was never
    // registered, so this was never active. If ethereal players need it, add an ethereal-only redirect after a
    // product decision instead of restoring the global bypass.

    private final MixinBuilder builder;

    Mixins(MixinBuilder builder) {
        this.builder = builder;
    }

    @Nonnull
    @Override
    public MixinBuilder getBuilder() {
        return builder;
    }
}
