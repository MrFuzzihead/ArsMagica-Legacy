package am2.network;

import java.io.IOException;

import cpw.mods.fml.relauncher.Side;
import io.netty.buffer.ByteBufInputStream;

/**
 * One validated custom-packet frame: the packet ID, the receiving side, and the remaining payload.
 *
 * <p>
 * Direction and payload-size validation happen before the payload is allocated, so processors and handlers only see
 * frames that are valid for the side that received them.
 * </p>
 */
final class PacketEnvelope {

    private final byte id;
    private final byte[] payload;

    private PacketEnvelope(byte id, byte[] payload) {
        this.id = id;
        this.payload = payload;
    }

    static PacketEnvelope read(ByteBufInputStream input, Side receivingSide) throws IOException {
        if (input == null || receivingSide == null) return null;

        byte id = input.readByte();
        if (!PacketValidation.isAllowedDirection(id, receivingSide)) return null;

        int length = input.available();
        if (!PacketValidation.isValidPayloadLength(length)) return null;

        byte[] payload = new byte[length];
        input.readFully(payload);
        return new PacketEnvelope(id, payload);
    }

    byte id() {
        return id;
    }

    byte[] payload() {
        return payload;
    }
}
