package am2.network;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import net.minecraft.entity.player.EntityPlayerMP;

import org.junit.Before;
import org.junit.Test;

/**
 * Truncated client packets must be rejected before a handler touches mutable player state.
 */
public class AMPacketProcessorServerStrictReadTest {

    private AMPacketProcessorServer processor;

    @Before
    public void setUp() {
        processor = new AMPacketProcessorServer();
    }

    @Test
    public void truncatedExpropOperationDoesNotTouchThePlayer() throws Exception {
        EntityPlayerMP player = mock(EntityPlayerMP.class);
        invoke("handleExpropOperation", player);
        verifyNoInteractions(player);
    }

    @Test
    public void truncatedCastingModeChangeDoesNotTouchThePlayer() throws Exception {
        EntityPlayerMP player = mock(EntityPlayerMP.class);
        invoke("handleCastingModeChange", player);
        verifyNoInteractions(player);
    }

    @Test
    public void truncatedPowerPathSyncDoesNotTouchThePlayer() throws Exception {
        EntityPlayerMP player = mock(EntityPlayerMP.class);
        invoke("handlePowerPathSync", player);
        verifyNoInteractions(player);
    }

    private void invoke(String methodName, EntityPlayerMP player) throws Exception {
        Method method = AMPacketProcessorServer.class.getDeclaredMethod(methodName, byte[].class, EntityPlayerMP.class);
        method.setAccessible(true);
        try {
            method.invoke(processor, new byte[0], player);
        } catch (InvocationTargetException e) {
            throw new AssertionError(e.getCause());
        }
    }
}
