package am2.network;

import java.util.HashMap;
import java.util.Map;

/** Typed packet-ID to handler table for one receiving side. */
final class PacketRegistry<P> {

    interface Handler<P> {

        void handle(P player, byte[] payload) throws Exception;
    }

    private final Map<Byte, Handler<P>> handlers = new HashMap<Byte, Handler<P>>();

    void register(byte id, Handler<P> handler) {
        if (handlers.put(id, handler) != null) {
            throw new IllegalArgumentException("Duplicate packet handler for id " + id);
        }
    }

    Handler<P> get(byte id) {
        return handlers.get(id);
    }

    int size() {
        return handlers.size();
    }
}
