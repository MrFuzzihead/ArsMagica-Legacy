package am2.handlers;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import net.minecraft.entity.player.EntityPlayer;

import org.junit.Test;
import org.mockito.MockedStatic;

import am2.playerextensions.ExtendedProperties;
import cpw.mods.fml.common.gameevent.PlayerEvent;

public class SoulspikeEtherealLoginTest {

    @Test
    public void malformedEtherealDurationIsClearedOnLogin() {
        EntityPlayer player = mock(EntityPlayer.class);
        ExtendedProperties properties = mock(ExtendedProperties.class);
        when(properties.hasExtraVariable("ethereal")).thenReturn(true);
        when(properties.getExtraVariable("ethereal")).thenReturn("not-a-number");

        try (MockedStatic<ExtendedProperties> staticProperties = mockStatic(ExtendedProperties.class)) {
            staticProperties.when(() -> ExtendedProperties.For(player))
                .thenReturn(properties);

            new SoulspikeAttackHandler().onLogin(new PlayerEvent.PlayerLoggedInEvent(player));

            verify(properties).removeFromExtraVariables("ethereal");
        }
    }

    @Test
    public void validEtherealDurationIsLeftAloneOnLogin() {
        EntityPlayer player = mock(EntityPlayer.class);
        ExtendedProperties properties = mock(ExtendedProperties.class);
        when(properties.hasExtraVariable("ethereal")).thenReturn(true);
        when(properties.getExtraVariable("ethereal")).thenReturn("200");

        try (MockedStatic<ExtendedProperties> staticProperties = mockStatic(ExtendedProperties.class)) {
            staticProperties.when(() -> ExtendedProperties.For(player))
                .thenReturn(properties);

            new SoulspikeAttackHandler().onLogin(new PlayerEvent.PlayerLoggedInEvent(player));

            verify(properties, never()).removeFromExtraVariables("ethereal");
        }
    }
}
