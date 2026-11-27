package am2;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.event.entity.living.LivingEvent;

import org.junit.Test;
import org.mockito.MockedStatic;

import am2.api.math.AMVector2;
import am2.playerextensions.ExtendedProperties;
import am2.utility.EntityUtilities;

public class ShrinkHandlerTest {

    @Test
    public void handlerOnlyListensToLivingUpdateEvents() throws Exception {
        assertNotNull(ShrinkHandler.class.getDeclaredMethod("onEntityLiving", LivingEvent.LivingUpdateEvent.class));
    }

    @Test
    public void restoreSizeUndoesShrinkAndClearsFlags() {
        EntityPlayer player = mock(EntityPlayer.class);
        ExtendedProperties properties = mock(ExtendedProperties.class);
        when(properties.getOriginalSize()).thenReturn(new AMVector2(1.8D, 1.8D));
        when(properties.getIsShrunk()).thenReturn(true);
        properties.shrinkAmount = 0.5F;

        try (MockedStatic<EntityUtilities> utilities = mockStatic(EntityUtilities.class)) {
            ShrinkHandler.restoreSize(player, properties);

            utilities.verify(() -> EntityUtilities.setSize(player, 1.8F, 1.8F));
        }

        verify(properties).setShrinkPct(0F);
        verify(properties).setIsShrunk(false);
    }
}
