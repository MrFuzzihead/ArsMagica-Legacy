package am2.mixins.early;

import net.minecraft.block.BlockLiquid;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Exposes vanilla's protected fizz sound/particles helper so {@link BlockDynamicLiquidMixin} can reuse it instead of
 * copying the effect code. The method has no MCP name in 1.7.10, so the SRG name is used.
 */
@Mixin(BlockLiquid.class)
public interface BlockLiquidInvoker {

    @Invoker("func_149799_m")
    void am2$fizz(World world, int x, int y, int z);
}
