package am2.utility;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;

import am2.AMCore;
import am2.network.TickrateMessage;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.relauncher.Side;

/**
 * Owns the configurable server tick length used by the {@code MinecraftServerMixin}, plus the common side of the
 * time-manipulation spells and the client tickrate handshake.
 *
 * <p>
 * The old ASM transformer rewrote every {@code 50L} constant in {@code MinecraftServer.run()} to a public static field
 * on {@code MysteriumPatchesFixesMagicka}. This small helper keeps that state in normal code so the mixin is a thin
 * {@code @ModifyConstant} callout and the value can be unit-tested. Client-only timer changes go through
 * {@code AMCore.proxy.changeClientTickrate}.
 * </p>
 */
public final class TickrateController {

    /** The vanilla 20 ticks/second. */
    public static final float DEFAULT_TICKS_PER_SECOND = 20.0F;

    /** The vanilla 50 ms per tick. */
    public static final long DEFAULT_TICK_LENGTH_MS = 50L;

    /** Countdown used by Concentrate Time / Dilute Time to restore normal time. */
    public static int countdownToChangeBack = -1;

    private static volatile long serverTickLengthMs = DEFAULT_TICK_LENGTH_MS;
    private static volatile boolean mixinApplied;

    private TickrateController() {}

    public static long getServerTickrate() {
        return serverTickLengthMs;
    }

    /**
     * Sets the tick length from a ticks-per-second value. Non-positive, NaN and infinite values reset to vanilla so a
     * bad config or spell modifier cannot stall the server loop.
     */
    public static void setServerTickrate(float ticksPerSecond) {
        if (ticksPerSecond <= 0.0F || Float.isNaN(ticksPerSecond) || Float.isInfinite(ticksPerSecond)) {
            serverTickLengthMs = DEFAULT_TICK_LENGTH_MS;
            return;
        }

        // 1000 ms in a second / 20 ticks a second = 50 ms per tick.
        serverTickLengthMs = Math.max(1L, (long) (1000.0F / ticksPerSecond));
    }

    public static void resetServerTickrate() {
        serverTickLengthMs = DEFAULT_TICK_LENGTH_MS;
    }

    /**
     * Common entry point for Concentrate Time / Dilute Time. Applies the server tickrate and asks the client proxy to
     * update the local timer. The client-only implementation lives in {@code ClientTickrateController}.
     */
    public static void changeTickrate(float ticksPerSecond) {
        if (AMCore.config == null || !AMCore.config.isGlobalTimeManipulationEnabled()) {
            return;
        }
        setServerTickrate(ticksPerSecond);
        changeClientTickratePublic(ticksPerSecond);
    }

    public static void changeClientTickratePublic(float ticksPerSecond) {
        MinecraftServer server = MinecraftServer.getServer();
        if (server != null && server.getConfigurationManager() != null) {
            for (EntityPlayer player : server.getConfigurationManager().playerEntityList) {
                changeClientTickratePublic(player, ticksPerSecond);
            }
        } else {
            changeClientTickratePublic(null, ticksPerSecond);
        }
    }

    public static void changeClientTickratePublic(EntityPlayer player, float ticksPerSecond) {
        if (player == null || player.worldObj == null || player.worldObj.isRemote) {
            if (FMLCommonHandler.instance()
                .getSide() == Side.CLIENT) {
                AMCore.proxy.changeClientTickrate(ticksPerSecond);
            }
        } else if (AMCore.NETWORK != null) {
            AMCore.NETWORK.sendTo(new TickrateMessage(ticksPerSecond), (EntityPlayerMP) player);
        }
    }

    /** Set by the {@code MinecraftServerMixin} handler so the in-game tests can prove the transform ran. */
    public static boolean isMixinApplied() {
        return mixinApplied;
    }

    public static void markMixinApplied() {
        mixinApplied = true;
    }

    /** Package-private for tests that need a clean starting point. */
    static void setServerTickrateForTesting(long tickLengthMs) {
        serverTickLengthMs = tickLengthMs;
    }

    static void resetForTesting() {
        serverTickLengthMs = DEFAULT_TICK_LENGTH_MS;
        mixinApplied = false;
    }
}
