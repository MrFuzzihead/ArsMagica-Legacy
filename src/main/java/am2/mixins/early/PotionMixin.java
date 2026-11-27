package am2.mixins.early;

import net.minecraft.potion.Potion;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Replaces the reflection-based potion-array extension in {@code BuffList}.
 *
 * <p>
 * Vanilla 1.7.10 allocates {@code Potion.potionTypes} with 32 slots. AM2 needs more, but potion IDs are written as a
 * single byte to NBT and to the effect packets, so IDs above 127 silently truncate. Growing the array to 128 keeps
 * every ID representable and lets the potion-ID widening ASM transformers be deleted instead of ported.
 * </p>
 */
@Mixin(Potion.class)
public abstract class PotionMixin {

    /** 128 is the largest size whose highest valid index (127) still fits in a signed byte. */
    private static final int ARSMAGICA2_POTION_ARRAY_SIZE = 128;

    @Shadow
    @Final
    @Mutable
    public static Potion[] potionTypes;

    /**
     * Runs at the end of {@code Potion.<clinit>}, after vanilla has registered its own potions, and copies them into
     * the larger array. Growing in place is not possible; the static final field must be reassigned.
     */
    @Inject(method = "<clinit>", at = @At("TAIL"), require = 1)
    private static void arsmagica2$growPotionArray(CallbackInfo ci) {
        if (potionTypes.length >= ARSMAGICA2_POTION_ARRAY_SIZE) {
            return;
        }

        Potion[] grown = new Potion[ARSMAGICA2_POTION_ARRAY_SIZE];
        System.arraycopy(potionTypes, 0, grown, 0, potionTypes.length);
        potionTypes = grown;
    }
}
