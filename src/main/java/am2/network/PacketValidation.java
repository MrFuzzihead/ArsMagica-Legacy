package am2.network;

import java.util.HashMap;
import java.util.Map;

import cpw.mods.fml.relauncher.Side;

/**
 * Shared bounds checks for custom packet payloads.
 *
 * <p>
 * The payload is read into a byte array before dispatch, so the length must be validated first. This keeps a
 * client-originated packet from forcing an arbitrary allocation before the per-handler authority checks run.
 * </p>
 */
public final class PacketValidation {

    /** Matches the maximum NBT payload accepted by {@link AMDataReader}. */
    public static final int MAX_PAYLOAD_BYTES = 1 << 20;

    private enum Direction {
        CLIENT_TO_SERVER,
        SERVER_TO_CLIENT,
        BIDIRECTIONAL
    }

    private static final Map<Byte, Direction> DIRECTIONS = new HashMap<Byte, Direction>();

    static {
        clientToServer(
            AMPacketIDs.SPELL_SHAPE_GROUP_CHANGE,
            AMPacketIDs.SYNCMAPTOSERVER,
            AMPacketIDs.SYNCCOMPENDIUM,
            AMPacketIDs.SYNCCOMPENDIUMREQUEST,
            AMPacketIDs.REQUESTWORLDDATACHANGE,
            AMPacketIDs.SPELL_CUSTOMIZE,
            AMPacketIDs.SPELLBOOK_CHANGE_ACTIVE_SLOT,
            AMPacketIDs.DECO_BLOCK_UPDATE,
            AMPacketIDs.INSCRIPTION_TABLE_UPDATE,
            AMPacketIDs.TK_DISTANCE_SYNC,
            AMPacketIDs.SAVE_KEYSTONE_COMBO,
            AMPacketIDs.SET_KEYSTONE_COMBO,
            AMPacketIDs.RUNE_BAG_GUI_OPEN,
            AMPacketIDs.M_BENCH_LOCK_RECIPE,
            AMPacketIDs.IMBUE_ARMOR,
            AMPacketIDs.AFFINITY_ACTIVATE);
        serverToClient(
            AMPacketIDs.SPELL_CAST,
            AMPacketIDs.CLOAKING,
            AMPacketIDs.DEAFEN,
            AMPacketIDs.SYNCMAPTOCLIENTS,
            AMPacketIDs.SYNCCOMPENDIUMRESPONSE,
            AMPacketIDs.SYNCWORLDDATATOCLIENTS,
            AMPacketIDs.PARTICLE_SPAWN_SPECIAL,
            AMPacketIDs.PLAYER_VELOCITY_ADD,
            AMPacketIDs.FLASH_ARMOR_PIECE,
            AMPacketIDs.REMOVE_BUFF_EFFECT,
            AMPacketIDs.ENERVATOR_BLOCK_UPDATE,
            AMPacketIDs.SYNC_AIR_CHANGE,
            AMPacketIDs.SYNC_AFFINITY_DATA,
            AMPacketIDs.ENTITY_ACTION_UPDATE,
            AMPacketIDs.PLAYER_LOGIN_DATA,
            AMPacketIDs.NBT_DUMP,
            AMPacketIDs.COMPENDIUM_UNLOCK,
            AMPacketIDs.SYNC_WORLD_NAME,
            AMPacketIDs.STAR_FALL,
            AMPacketIDs.HIDDEN_COMPONENT_UNLOCK,
            AMPacketIDs.SPELL_APPLY_EFFECT,
            AMPacketIDs.HECATE_DEATH,
            AMPacketIDs.CAPABILITY_CHANGE,
            AMPacketIDs.CRAFTING_ALTAR_DATA,
            AMPacketIDs.LECTERN_DATA,
            AMPacketIDs.CALEFACTOR_DATA,
            AMPacketIDs.OBELISK_DATA);
        bidirectional(
            AMPacketIDs.MAGIC_LEVEL_UP,
            AMPacketIDs.SYNC_EXTENDED_PROPS,
            AMPacketIDs.SYNC_BETA_PARTICLES,
            AMPacketIDs.SYNC_SPELL_KNOWLEDGE,
            AMPacketIDs.REQUEST_BETA_PARTICLES,
            AMPacketIDs.CASTER_BLOCK_UPDATE,
            AMPacketIDs.SET_MAG_WORK_REC,
            AMPacketIDs.REQUEST_PWR_PATHS);
    }

    private PacketValidation() {}

    public static boolean isAllowedDirection(byte packetID, Side receivingSide) {
        if (receivingSide == null) return false;
        Direction direction = DIRECTIONS.get(packetID);
        if (direction == null) return false;
        return direction == Direction.BIDIRECTIONAL
            || (direction == Direction.CLIENT_TO_SERVER && receivingSide == Side.SERVER)
            || (direction == Direction.SERVER_TO_CLIENT && receivingSide == Side.CLIENT);
    }

    private static void clientToServer(byte... ids) {
        put(Direction.CLIENT_TO_SERVER, ids);
    }

    private static void serverToClient(byte... ids) {
        put(Direction.SERVER_TO_CLIENT, ids);
    }

    private static void bidirectional(byte... ids) {
        put(Direction.BIDIRECTIONAL, ids);
    }

    private static void put(Direction direction, byte... ids) {
        for (byte id : ids) {
            DIRECTIONS.put(id, direction);
        }
    }

    public static boolean isValidPayloadLength(int length) {
        return length >= 0 && length <= MAX_PAYLOAD_BYTES;
    }

    public static boolean isValidPayload(byte[] payload) {
        return payload != null && isValidPayloadLength(payload.length);
    }
}
