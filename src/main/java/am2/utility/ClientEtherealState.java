package am2.utility;

import net.minecraft.client.Minecraft;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Client-side ethereal camera state.
 *
 * <p>
 * The old ASM {@code @Fix} methods on {@code EntityRenderer.orientCamera} and {@code Vec3.distanceTo} used a flag on
 * {@code MysteriumPatchesFixesMagicka} to stop the local camera from colliding with blocks while the player is
 * ethereal. This is the normal-code home for that flag so the client Mixins and the still-compiled old class can share
 * one definition.
 * </p>
 */
@SideOnly(Side.CLIENT)
public final class ClientEtherealState {

    private static boolean orientingCamera;

    private ClientEtherealState() {}

    public static void setOrientingCamera(boolean value) {
        orientingCamera = value;
    }

    public static boolean isOrientingCamera() {
        return orientingCamera;
    }

    /**
     * True only while the local player is ethereal and the camera is being oriented for that same player.
     */
    public static boolean isEtherealCamera() {
        if (!orientingCamera) {
            return false;
        }

        Minecraft mc = Minecraft.getMinecraft();
        return mc != null && mc.thePlayer != null
            && mc.renderViewEntity == mc.thePlayer
            && EtherealState.isEthereal(mc.thePlayer);
    }
}
