package am2.network;

import static org.junit.Assert.assertEquals;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.junit.Before;
import org.junit.Test;

/**
 * Truncated server packets must not reach client world/GUI mutation.
 */
public class AMPacketProcessorClientStrictReadTest {

    private AMPacketProcessorClient processor;

    @Before
    public void setUp() {
        processor = new AMPacketProcessorClient();
    }

    @Test
    public void truncatedCloakingLeavesThePreviousValue() throws Exception {
        int previous = AMPacketProcessorClient.cloaking;
        AMPacketProcessorClient.cloaking = 7;
        try {
            invoke("handleCloaking");
            assertEquals(7, AMPacketProcessorClient.cloaking);
        } finally {
            AMPacketProcessorClient.cloaking = previous;
        }
    }

    @Test
    public void truncatedPotionReplaceDoesNotReachTheClientPlayer() throws Exception {
        invoke("handlePotionReplace");
    }

    @Test
    public void truncatedCapabilityChangeDoesNotReachTheClientPlayer() throws Exception {
        invoke("handleCapabilityChange");
    }

    @Test
    public void truncatedFlashArmorDoesNotReachTheGui() throws Exception {
        invoke("handleFlashArmor");
    }

    private void invoke(String methodName) throws Exception {
        Method method = AMPacketProcessorClient.class.getDeclaredMethod(methodName, byte[].class);
        method.setAccessible(true);
        try {
            method.invoke(processor, new byte[0]);
        } catch (InvocationTargetException e) {
            throw new AssertionError(e.getCause());
        }
    }
}
