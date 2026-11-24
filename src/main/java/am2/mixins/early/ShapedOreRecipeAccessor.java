package am2.mixins.early;

import net.minecraftforge.oredict.ShapedOreRecipe;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Exposes Forge's {@code ShapedOreRecipe.width}/{@code height} for the compendium recipe renderer. Forge classes are
 * not obfuscated, so this mixin uses {@code remap = false}.
 */
@Mixin(value = ShapedOreRecipe.class, remap = false)
public interface ShapedOreRecipeAccessor {

    @Accessor("width")
    int am2$getWidth();

    @Accessor("height")
    int am2$getHeight();
}
