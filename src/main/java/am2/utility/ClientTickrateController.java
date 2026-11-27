package am2.utility;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Timer;

import am2.mixins.early.MinecraftAccessor;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Client-only timer replacement used by the time-manipulation spells. The common side sends a
 * {@code TickrateMessage}; this class applies it to {@code Minecraft.timer} through {@link MinecraftAccessor}.
 */
@SideOnly(Side.CLIENT)
public final class ClientTickrateController {

    private static float clienttickrate = TickrateController.DEFAULT_TICKS_PER_SECOND;

    private ClientTickrateController() {}

    public static float getClientTickrate() {
        return clienttickrate;
    }

    public static void changeClientTickrate(float tickrate) {
        clienttickrate = tickrate;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null) {
            return;
        }
        ((MinecraftAccessor) mc).am2$setTimer(new Timer(clienttickrate));
    }

    /** Package-private reset for tests. */
    static void resetForTesting() {
        clienttickrate = TickrateController.DEFAULT_TICKS_PER_SECOND;
    }
}
