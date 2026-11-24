package am2.mixins.early;

import net.minecraft.potion.Potion;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Reads {@code Potion.isBadEffect}, replacing the {@code field_76418_K} access transformer and the index-based
 * reflection in {@code Dispelling}. The public {@code isBadEffect()} method is {@code @SideOnly(Side.CLIENT)}, so
 * common code cannot call it.
 */
@Mixin(Potion.class)
public interface PotionAccessor {

    @Accessor("isBadEffect")
    boolean am2$isBadEffect();
}
