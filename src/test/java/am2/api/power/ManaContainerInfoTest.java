package am2.api.power;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;

public class ManaContainerInfoTest {

    @Test
    public void copiesStateFromContainer() {
        IManaContainer container = mock(IManaContainer.class);
        ManaStack stack = mock(ManaStack.class);
        when(container.getMana()).thenReturn(stack);
        when(container.getCapacity()).thenReturn(500);

        ManaContainerInfo info = new ManaContainerInfo(container);

        assertSame(stack, info.mana);
        assertEquals(500, info.capacity);
        verify(container).getMana();
        verify(container).getCapacity();
    }

    @Test
    public void explicitConstructorStoresValues() {
        ManaStack stack = mock(ManaStack.class);
        ManaContainerInfo info = new ManaContainerInfo(stack, 42);

        assertSame(stack, info.mana);
        assertEquals(42, info.capacity);
    }
}
