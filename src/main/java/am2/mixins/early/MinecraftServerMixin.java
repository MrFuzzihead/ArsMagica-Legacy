package am2.mixins.early;

import net.minecraft.server.MinecraftServer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

import am2.utility.TickrateController;

/**
 * Replacement for {@code BytecodeTransformers.alterServerTickrate}.
 *
 * <p>
 * The old transformer rewrote every {@code LDC 50L} in {@code MinecraftServer.run()} to a static field on the ASM
 * patch class. {@code @ModifyConstant} does the same with a real callout, so the value is owned by
 * {@link TickrateController} and can be tested. All five 50&nbsp;ms constants are replaced (the two loop bounds, the
 * warning divisor and the sleep calculation).
 * </p>
 */
@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin {

    @ModifyConstant(method = "run", constant = @Constant(longValue = 50L), require = 1)
    private long arsmagica2$serverTickLength(long original) {
        TickrateController.markMixinApplied();
        return TickrateController.getServerTickrate();
    }
}
