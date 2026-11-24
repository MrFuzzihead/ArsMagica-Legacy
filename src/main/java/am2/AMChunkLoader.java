package am2;

import java.util.HashMap;
import java.util.List;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeChunkManager;
import net.minecraftforge.common.ForgeChunkManager.LoadingCallback;
import net.minecraftforge.common.ForgeChunkManager.Ticket;

public class AMChunkLoader implements LoadingCallback {

    public static final AMChunkLoader INSTANCE = new AMChunkLoader();

    private HashMap<TicketIdentifier, ForgeChunkManager.Ticket> tickets;

    private AMChunkLoader() {
        tickets = new HashMap<TicketIdentifier, ForgeChunkManager.Ticket>();
    }

    private Ticket getOrCreateTicket(TicketIdentifier identifier, World world) {
        Ticket ticket = tickets.get(identifier);
        if (ticket != null) {
            return ticket;
        }
        ticket = ForgeChunkManager.requestTicket(AMCore.instance, world, ForgeChunkManager.Type.NORMAL);
        if (ticket != null) {
            tickets.put(identifier, ticket);
        }
        return ticket;
    }

    /** Package-private test hook: number of tickets currently tracked for release. */
    public int cachedTicketCount() {
        return tickets.size();
    }

    /**
     * Requests a static chunk load. This is a non-moving chunk load, and will be assumed that the specified class will
     * exist as a tile entity
     *
     * @param clazz the class to look for
     * @param x     the x coordinate of the tile entity
     * @param y     the y coordinate of the tile entity
     * @param z     the z coordinate of the tile entity
     * @param world The world object of the tile entity
     */
    public void requestStaticChunkLoad(Class clazz, int x, int y, int z, World world) {
        TicketIdentifier identifier = new TicketIdentifier(x, y, z, world.provider.dimensionId);
        Ticket ticket = getOrCreateTicket(identifier, world);
        if (ticket == null) {
            LogHelper.warn(
                "Unable to get a ticket for chunk loading!  The chunk identified by %d, %d is *not* loaded!",
                x,
                z);
            return;
        }

        NBTTagCompound compound = ticket.getModData();
        compound.setIntArray("StaticLoadCoords", new int[] { x, y, z });
        compound.setString("ChunkLoadClass", clazz.getName());

        ChunkCoordIntPair pair = new ChunkCoordIntPair(x >> 4, z >> 4);
        ForgeChunkManager.forceChunk(ticket, pair);
    }

    public void releaseStaticChunkLoad(Class clazz, int x, int y, int z, World world) {
        TicketIdentifier identifier = new TicketIdentifier(x, y, z, world.provider.dimensionId);
        Ticket ticket = tickets.remove(identifier);
        if (ticket == null) {
            LogHelper.warn("No ticket for specified location.  No chunk to unload!");
            return;
        }

        ChunkCoordIntPair pair = new ChunkCoordIntPair(x >> 4, z >> 4);
        ForgeChunkManager.unforceChunk(ticket, pair);
        ForgeChunkManager.releaseTicket(ticket);
        LogHelper.trace("Unloaded chunk at: %d, %d", pair.chunkXPos, pair.chunkZPos);
    }

    @Override
    public void ticketsLoaded(List<Ticket> tickets, World world) {
        for (Ticket ticket : tickets) {
            NBTTagCompound compound = ticket.getModData();
            int[] coords = compound.getIntArray("StaticLoadCoords");
            if (coords == null || coords.length < 3) {
                LogHelper.info("Discarding a chunk loading ticket with malformed coordinates.");
                ForgeChunkManager.releaseTicket(ticket);
                continue;
            }
            String clazzName = compound.getString("ChunkLoadClass");
            Class clazz = null;
            try {
                clazz = Class.forName(clazzName);
            } catch (ClassNotFoundException e) {
                LogHelper.info(
                    "Cached class not found (%s) when attempting to load a chunk loading ticket.  This ticket will be discarded, and the chunk may not be loaded.  Block Coords: %d, %d",
                    clazzName,
                    coords[0],
                    coords[2]);
                ForgeChunkManager.releaseTicket(ticket);
                continue;
            }
            if (!TileEntity.class.isAssignableFrom(clazz)) {
                LogHelper.info(
                    "Cached class %s is not a TileEntity when loading a chunk loading ticket.  This ticket will be discarded.  Block Coords: %d, %d",
                    clazzName,
                    coords[0],
                    coords[2]);
                ForgeChunkManager.releaseTicket(ticket);
                continue;
            }
            TileEntity te = world.getTileEntity(coords[0], coords[1], coords[2]);
            if (te != null && clazz.isAssignableFrom(te.getClass())) {
                ChunkCoordIntPair pair = new ChunkCoordIntPair(coords[0] >> 4, coords[2] >> 4);
                ForgeChunkManager.forceChunk(ticket, pair);
                this.tickets
                    .put(new TicketIdentifier(coords[0], coords[1], coords[2], world.provider.dimensionId), ticket);
            } else {
                LogHelper.info(
                    "Either no tile entity was found or it did not match the cached class.  This chunk loading ticket will be discarded, and the chunk may not be loaded.  Block Coords: %d, %d",
                    coords[0],
                    coords[2]);
                ForgeChunkManager.releaseTicket(ticket);
            }
        }
    }

    static class TicketIdentifier {

        public final int dimension;
        public final int x;
        public final int y;
        public final int z;

        public TicketIdentifier(int x, int y, int z, int dimension) {
            this.dimension = dimension;
            this.x = x;
            this.y = y;
            this.z = z;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof TicketIdentifier)) return false;
            TicketIdentifier other = (TicketIdentifier) obj;
            return x == other.x && y == other.y && z == other.z && dimension == other.dimension;
        }

        @Override
        public int hashCode() {
            int result = dimension;
            result = 31 * result + x;
            result = 31 * result + y;
            result = 31 * result + z;
            return result;
        }
    }
}
