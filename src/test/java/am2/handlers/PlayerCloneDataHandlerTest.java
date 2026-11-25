package am2.handlers;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;

import org.junit.Test;

import am2.playerextensions.AffinityData;
import am2.playerextensions.ExtendedProperties;
import am2.playerextensions.RiftStorage;
import am2.playerextensions.SkillData;

public class PlayerCloneDataHandlerTest {

    @Test
    public void allFourStoresAreTransferred() {
        EntityPlayer original = mock(EntityPlayer.class);
        EntityPlayer clone = mock(EntityPlayer.class);
        ExtendedProperties oldProps = mock(ExtendedProperties.class);
        ExtendedProperties newProps = mock(ExtendedProperties.class);
        SkillData oldSkill = mock(SkillData.class);
        SkillData newSkill = mock(SkillData.class);
        RiftStorage oldRift = mock(RiftStorage.class);
        RiftStorage newRift = mock(RiftStorage.class);
        AffinityData oldAffinity = mock(AffinityData.class);
        AffinityData newAffinity = mock(AffinityData.class);

        when(original.getExtendedProperties(ExtendedProperties.identifier)).thenReturn(oldProps);
        when(clone.getExtendedProperties(ExtendedProperties.identifier)).thenReturn(newProps);
        when(original.getExtendedProperties(SkillData.identifier)).thenReturn(oldSkill);
        when(clone.getExtendedProperties(SkillData.identifier)).thenReturn(newSkill);
        when(original.getExtendedProperties(RiftStorage.identifier)).thenReturn(oldRift);
        when(clone.getExtendedProperties(RiftStorage.identifier)).thenReturn(newRift);
        when(original.getExtendedProperties(AffinityData.identifier)).thenReturn(oldAffinity);
        when(clone.getExtendedProperties(AffinityData.identifier)).thenReturn(newAffinity);

        new PlayerCloneDataHandler().onclone(new PlayerEvent.Clone(clone, original, true));

        verify(oldProps).saveNBTData(org.mockito.ArgumentMatchers.any());
        verify(newProps).loadNBTData(org.mockito.ArgumentMatchers.any());
        verify(oldSkill).saveNBTData(org.mockito.ArgumentMatchers.any());
        verify(newSkill).loadNBTData(org.mockito.ArgumentMatchers.any());
        verify(oldRift).saveNBTData(org.mockito.ArgumentMatchers.any());
        verify(newRift).loadNBTData(org.mockito.ArgumentMatchers.any());
        verify(oldAffinity).saveNBTData(org.mockito.ArgumentMatchers.any());
        verify(newAffinity).loadNBTData(org.mockito.ArgumentMatchers.any());
    }

    @Test
    public void fakePlayersAreIgnored() {
        EntityPlayer original = mock(EntityPlayer.class);
        FakePlayer clone = mock(FakePlayer.class);

        new PlayerCloneDataHandler().onclone(new PlayerEvent.Clone(clone, original, true));

        verifyNoInteractions(original, clone);
    }
}
