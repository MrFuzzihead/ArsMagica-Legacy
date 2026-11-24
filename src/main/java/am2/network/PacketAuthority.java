package am2.network;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;

import am2.blocks.tileentities.TileEntityInscriptionTable;
import am2.containers.ContainerInscriptionTable;

/**
 * Server-side authority checks for client-originated block packets.
 *
 * <p>
 * Coordinates in these packets are attacker-controlled. Every handler must verify the sender is in the same world,
 * close enough to the block, and (where relevant) actually has that block's container open. The GUI hiding a button
 * is not an authority check.
 * </p>
 */
public final class PacketAuthority {

    /** Vanilla's standard "can still interact" distance, squared. */
    public static final double MAX_INTERACTION_DISTANCE_SQ = 64.0D;

    private PacketAuthority() {}

    public static boolean isWithinReach(EntityPlayer player, TileEntity te) {
        if (player == null || te == null) {
            return false;
        }
        if (player.worldObj != te.getWorldObj()) {
            return false;
        }
        return player.getDistanceSq(te.xCoord + 0.5D, te.yCoord + 0.5D, te.zCoord + 0.5D)
            <= MAX_INTERACTION_DISTANCE_SQ;
    }

    public static boolean isUsingInscriptionTable(EntityPlayer player, TileEntityInscriptionTable table) {
        return isWithinReach(player, table) && player.openContainer instanceof ContainerInscriptionTable
            && ((ContainerInscriptionTable) player.openContainer).getTable() == table;
    }
}
