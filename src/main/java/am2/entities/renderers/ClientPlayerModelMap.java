package am2.entities.renderers;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.entity.player.EntityPlayer;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Client-side player-model overrides (the "maid" model and the mob disguises).
 *
 * <p>
 * The map used to live on {@code MysteriumPatchesFixesMagicka} next to the ASM {@code getEntityRenderObject} patch.
 * Keeping it with the renderer lets the new {@code RenderManagerMixin} and the existing client event handlers share one
 * definition, and the special renderer is cached instead of allocated on every lookup.
 * </p>
 */
@SideOnly(Side.CLIENT)
public final class ClientPlayerModelMap {

    private static final Map<String, String> MODEL_BY_PLAYER = new HashMap<String, String>();
    private static RenderPlayerSpecial specialRenderer;

    private ClientPlayerModelMap() {}

    public static Map<String, String> map() {
        return MODEL_BY_PLAYER;
    }

    public static boolean isSpecialModel(EntityPlayer player) {
        if (player == null) {
            return false;
        }
        String model = MODEL_BY_PLAYER.get(player.getCommandSenderName());
        return model != null && model.startsWith("maid");
    }

    public static RenderPlayerSpecial getSpecialRenderer() {
        if (specialRenderer == null) {
            specialRenderer = new RenderPlayerSpecial();
        }
        return specialRenderer;
    }

    public static void clear() {
        MODEL_BY_PLAYER.clear();
        specialRenderer = null;
    }
}
