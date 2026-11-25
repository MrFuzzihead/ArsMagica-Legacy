package am2.network;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import org.junit.Test;

public class PacketRegistryTest {

    @Test
    public void registersAndResolvesHandlers() {
        PacketRegistry<String> registry = new PacketRegistry<String>();
        PacketRegistry.Handler<String> handler = (player, payload) -> {};

        registry.register((byte) 7, handler);

        assertSame(handler, registry.get((byte) 7));
        assertEquals(1, registry.size());
        assertNull(registry.get((byte) 8));
    }

    @Test(expected = IllegalArgumentException.class)
    public void duplicateIdsAreRejected() {
        PacketRegistry<String> registry = new PacketRegistry<String>();
        registry.register((byte) 7, (player, payload) -> {});
        registry.register((byte) 7, (player, payload) -> {});
    }

    @Test
    public void processorsRegisterEverySwitchCase() {
        assertEquals(25, new AMPacketProcessorServer().handlerCount());
        assertEquals(35, new AMPacketProcessorClient().handlerCount());
    }
}
