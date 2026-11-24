package am2.mixins.early;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Timer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Replaces the reflective {@code Minecraft.timer} field lookup in the client tickrate code.
 */
@Mixin(Minecraft.class)
public interface MinecraftAccessor {

    @Accessor("timer")
    void am2$setTimer(Timer timer);
}
