package am2.mixins.early;

import java.util.Random;

import net.minecraft.block.BlockDynamicLiquid;
import net.minecraft.block.material.Material;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import am2.blocks.liquid.BlockLiquidEssence;

/**
 * Replacement for the {@code BlockDynamicLiquid.updateTick} {@code @Fix}: flowing lava that touches liquid essence
 * turns the essence into purple stained glass and plays the vanilla fizz effect.
 */
@Mixin(BlockDynamicLiquid.class)
public abstract class BlockDynamicLiquidMixin {

    @Inject(method = "updateTick", at = @At("HEAD"), cancellable = true, require = 1)
    private void arsmagica2$lavaEssenceReaction(World world, int x, int y, int z, Random random, CallbackInfo ci) {
        if (((BlockDynamicLiquid) (Object) this).getMaterial() == Material.lava
            && world.getBlock(x, y - 1, z) instanceof BlockLiquidEssence) {
            world.setBlock(x, y - 1, z, Blocks.stained_glass, 11, 3);
            ((BlockLiquidInvoker) (Object) this).am2$fizz(world, x, y - 1, z);
            ci.cancel();
        }
    }
}
