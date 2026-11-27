package am2.handlers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.attributes.BaseAttributeMap;
import net.minecraft.entity.item.EntityItemFrame;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.EntityEvent.EntityConstructing;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import am2.AMCore;
import am2.ItemFrameWatcher;
import am2.playerextensions.AffinityData;
import am2.playerextensions.ExtendedProperties;
import am2.playerextensions.RiftStorage;
import am2.playerextensions.SkillData;
import am2.proxy.CommonProxy;

public class EntityConstructionHandlerTest {

    private CommonProxy previousProxy;

    @Before
    public void saveProxy() {
        previousProxy = AMCore.proxy;
    }

    @After
    public void restoreProxy() {
        AMCore.proxy = previousProxy;
    }

    @Test
    public void livingEntityGetsPropertiesAndAttributes() {
        EntityLivingBase entity = mock(EntityLivingBase.class);
        BaseAttributeMap attributes = mock(BaseAttributeMap.class);
        when(entity.getAttributeMap()).thenReturn(attributes);
        when(entity.getExtendedProperties(ExtendedProperties.identifier)).thenReturn(new ExtendedProperties());

        new EntityConstructionHandler().onEntityConstructing(new EntityConstructing(entity));

        verify(entity).registerExtendedProperties(eq(ExtendedProperties.identifier), any(ExtendedProperties.class));
        verify(attributes, times(5)).registerAttribute(any());
    }

    @Test
    public void playerGetsTheAdditionalStores() {
        EntityPlayer player = mock(EntityPlayer.class);
        when(player.getAttributeMap()).thenReturn(mock(BaseAttributeMap.class));
        when(player.getExtendedProperties(ExtendedProperties.identifier)).thenReturn(new ExtendedProperties());

        new EntityConstructionHandler().onEntityConstructing(new EntityConstructing(player));

        verify(player).registerExtendedProperties(eq(RiftStorage.identifier), any(RiftStorage.class));
        verify(player).registerExtendedProperties(eq(AffinityData.identifier), any(AffinityData.class));
        verify(player).registerExtendedProperties(eq(SkillData.identifier), any(SkillData.class));
    }

    @Test
    public void itemFramesStartBeingWatched() {
        CommonProxy proxy = mock(CommonProxy.class);
        proxy.itemFrameWatcher = mock(ItemFrameWatcher.class);
        AMCore.proxy = proxy;
        EntityItemFrame frame = mock(EntityItemFrame.class);

        new EntityConstructionHandler().onEntityConstructing(new EntityConstructing(frame));

        verify(proxy.itemFrameWatcher).startWatchingFrame(frame);
    }

    @Test
    public void unrelatedEntitiesAreIgnored() {
        Entity entity = mock(Entity.class);
        World world = mock(World.class);
        entity.worldObj = world;

        new EntityConstructionHandler().onEntityConstructing(new EntityConstructing(entity));
    }
}
