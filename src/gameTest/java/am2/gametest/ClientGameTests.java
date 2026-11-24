package am2.gametest;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;

import am2.AMCore;
import am2.AMKeyBindings;
import am2.blocks.BlocksCommonProxy;
import am2.items.ItemsCommonProxy;
import am2.proxy.ClientProxy;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * The client half of the in-game test mod.
 *
 * <p>
 * Runs once, from the main menu, so the client-only classes are guaranteed to be loaded and the language file is
 * active. The checks cover what a dedicated server cannot reach: the client proxy, the key bindings, the creative
 * tabs and the localised names. The client is exited as soon as they finish.
 * </p>
 */
@SideOnly(Side.CLIENT)
final class ClientGameTests {

    static final String OK_MARKER = "ARSMAGICA_CLIENTTEST_OK";
    static final String FAIL_MARKER = "ARSMAGICA_CLIENTTEST_FAILED";

    /** Roughly a minute: long enough for mod loading and resource packs, short enough to fail fast in CI. */
    private static final int MAIN_MENU_TIMEOUT_TICKS = 20 * 60;

    private static int sTicks;
    private static boolean sRan;
    private static int sChecks;
    private static int sFailures;

    private ClientGameTests() {}

    static void onClientTick() {
        if (sRan) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null) {
            return;
        }
        if (!(mc.currentScreen instanceof GuiMainMenu)) {
            if (++sTicks > MAIN_MENU_TIMEOUT_TICKS) {
                sRan = true;
                fail("the client never reached the main menu");
            }
            return;
        }

        sRan = true;
        try {
            checkClientProxy();
            checkKeyBindings();
            checkCreativeTabs();
            checkLocalisation();
            checkConfiguration();
        } catch (Throwable t) {
            ++sFailures;
            System.out.println("[ARSMAGICA-CLIENTTEST] FAILED with an exception: " + t);
            t.printStackTrace();
        }

        System.out.println("[ARSMAGICA-CLIENTTEST] " + (sChecks - sFailures) + "/" + sChecks + " checks passed");
        if (sFailures > 0) {
            fail(sFailures + " client assertion(s) failed");
            return;
        }
        System.out.println(OK_MARKER);
        exit(0);
    }

    private static void checkClientProxy() {
        check(AMCore.instance != null, "AMCore.instance must be set");
        check(AMCore.proxy != null, "AMCore.proxy must be set");
        check(AMCore.proxy instanceof ClientProxy, "the client must use the client proxy");
        check(
            Minecraft.getMinecraft()
                .getTextureManager() != null,
            "the client texture manager must be available");
    }

    private static void checkKeyBindings() {
        check(
            AMKeyBindings.ShapeGroupKey != null
                && "key.ShapeGroups".equals(AMKeyBindings.ShapeGroupKey.getKeyDescription()),
            "the shape group key must be created");
        check(
            "key.am2.category".equals(AMKeyBindings.ShapeGroupKey.getKeyCategory()),
            "the shape group key must use the Ars Magica category");
        check(
            KeyBinding.getKeybinds()
                .contains("key.am2.category"),
            "the Ars Magica key category must be registered");
    }

    private static void checkCreativeTabs() {
        check(BlocksCommonProxy.blockTab != null, "the blocks creative tab must exist");
        check(ItemsCommonProxy.itemTab != null, "the items creative tab must exist");
        if (BlocksCommonProxy.blockTab != null) {
            check(BlocksCommonProxy.blockTab.getTabIconItem() != null, "the blocks tab must have an icon");
        }
        if (ItemsCommonProxy.itemTab != null) {
            check(ItemsCommonProxy.itemTab.getTabIconItem() != null, "the items tab must have an icon");
        }
    }

    private static void checkLocalisation() {
        checkTranslated("tile.arsmagica2:witchwoodlog.name");
        checkTranslated("tile.arsmagica2:occulus.name");
        checkTranslated("item.arsmagica2:ArcaneCompendium.name");
        checkTranslated("key.am2.category");

        String logName = new ItemStack(BlocksCommonProxy.witchwoodLog).getDisplayName();
        check(logName != null && !logName.isEmpty(), "the witchwood log must have a display name");
        check(
            !"tile.arsmagica2:witchwoodlog.name".equals(logName),
            "the witchwood log display name must be translated");
    }

    private static void checkTranslated(String pKey) {
        String translated = StatCollector.translateToLocal(pKey);
        check(translated != null && !translated.isEmpty(), "the key '" + pKey + "' must translate");
        check(!pKey.equals(translated), "the key '" + pKey + "' must actually be translated");
    }

    private static void checkConfiguration() {
        check(AMCore.config != null, "AMCore.config must be loaded");
        if (AMCore.config != null) {
            check(
                AMCore.config.getGFXLevel() >= 0 && AMCore.config.getGFXLevel() <= 2,
                "the graphics level must be in [0, 2]");
        }
    }

    private static void check(boolean pCondition, String pDescription) {
        ++sChecks;
        if (!pCondition) {
            ++sFailures;
            System.out.println("[ARSMAGICA-CLIENTTEST] FAILED: " + pDescription);
        }
    }

    private static void fail(String pMessage) {
        System.out.println(FAIL_MARKER + ": " + pMessage);
        exit(1);
    }

    /** Hand the exit to a worker so the client can return from its tick callback before the JVM stops. */
    private static void exit(int pStatus) {
        Thread worker = new Thread(new Runnable() {

            @Override
            public void run() {
                FMLCommonHandler.instance()
                    .exitJava(pStatus, false);
            }
        }, "ArsMagica-client-test-exit");
        worker.setDaemon(true);
        worker.start();
    }
}
