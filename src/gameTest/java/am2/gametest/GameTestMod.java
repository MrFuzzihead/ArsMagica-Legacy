package am2.gametest;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.WorldServer;

import am2.utility.TickrateController;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartedEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Test-only mod: a separate, non-shipped {@code @Mod} that asserts Ars Magica 2 works against the real game.
 *
 * <p>
 * It lives in the {@code gameTest} source set and is only put on the classpath by {@code ./gradlew gameTestServer},
 * {@code ./gradlew gameTestClient} (or {@code -PgameTest}), so it never reaches the published jar. Run it with:
 * </p>
 *
 * <pre>
 * ./gradlew gameTestServer
 * ./gradlew gameTestClient
 * </pre>
 *
 * <p>
 * The assertions themselves live in {@link ArsMagicaGameTests} and {@link ClientGameTests}; this class owns the
 * lifecycle and the exit code. Every server assertion runs on the server thread after the first server tick, so mixins
 * applied to the tick loop have demonstrably executed before the checks run.
 * </p>
 */
@Mod(
    modid = GameTestMod.MOD_ID,
    name = "Ars Magica 2 Game Tests",
    version = "1.0",
    acceptableRemoteVersions = "*",
    dependencies = "required-after:arsmagica2")
public class GameTestMod {

    public static final String MOD_ID = "arsmagicagametest";
    static final String OK_MARKER = "ARSMAGICA_GAMETEST_OK";
    static final String FAIL_MARKER = "ARSMAGICA_GAMETEST_FAILED";

    /** Stop waiting for the tick-loop mixin after this many ticks so a broken transform fails instead of hanging. */
    private static final int MIXIN_GRACE_TICKS = 20;

    private boolean serverStarted;
    private boolean ran;
    private int serverTicks;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        // The client tick hook lives on this instance; without it the client half never runs.
        FMLCommonHandler.instance()
            .bus()
            .register(this);
    }

    /**
     * Client half of the harness: runs once at the main menu and exits the client with the result.
     */
    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent pEvent) {
        if (pEvent.phase == TickEvent.Phase.END && Boolean.getBoolean("arsmagica2.gameTest")) {
            ClientGameTests.onClientTick();
        }
    }

    @Mod.EventHandler
    public void onServerStarted(FMLServerStartedEvent event) {
        // The test mod only runs when the build actually asked for it, so a stray copy on a normal dev classpath is
        // inert instead of failing a launch.
        if (!Boolean.getBoolean("arsmagica2.gameTest")) {
            System.out.println("[ARSMAGICA-GAMETEST] arsmagica2.gameTest is not set, skipping");
            return;
        }
        serverStarted = true;
    }

    /**
     * Runs the server assertions after at least one server tick. That gives the {@code MinecraftServerMixin} a chance
     * to run, so {@link TickrateController#isMixinApplied()} is a real check that the transform loaded.
     */
    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !serverStarted || ran) {
            return;
        }

        ++serverTicks;
        if (!TickrateController.isMixinApplied() && serverTicks < MIXIN_GRACE_TICKS) {
            return;
        }

        ran = true;
        runServerTests();
    }

    private void runServerTests() {
        WorldServer overworld = MinecraftServer.getServer()
            .worldServerForDimension(0);
        if (overworld == null) {
            fail("the overworld is not available");
            return;
        }

        try {
            ArsMagicaGameTests.runAll(overworld);
        } catch (Throwable t) {
            t.printStackTrace();
            fail("the game tests threw: " + t);
            return;
        }

        int checks = ArsMagicaGameTests.checkCount();
        int failures = ArsMagicaGameTests.failureCount();
        System.out.println("[ARSMAGICA-GAMETEST] " + (checks - failures) + "/" + checks + " checks passed");
        if (failures > 0) {
            fail(failures + " assertion(s) failed, see above");
            return;
        }
        System.out.println(OK_MARKER);
        exit(0);
    }

    private void fail(String pMessage) {
        System.out.println(FAIL_MARKER + ": " + pMessage);
        exit(1);
    }

    /**
     * Called from the server thread: hand the exit to a worker so the server can return from its callback before
     * {@code Runtime.exit} starts the shutdown hooks.
     */
    private static void exit(int pStatus) {
        Thread worker = new Thread(new Runnable() {

            @Override
            public void run() {
                FMLCommonHandler.instance()
                    .exitJava(pStatus, false);
            }
        }, "ArsMagica-game-test-exit");
        worker.setDaemon(true);
        worker.start();
    }
}
