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
 * {@code mixins.arsmagica2.late.json}.
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
            "DamageSourceAccessor"));

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
