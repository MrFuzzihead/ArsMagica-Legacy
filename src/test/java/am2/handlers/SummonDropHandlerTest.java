package am2.handlers;

import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.living.LivingDropsEvent;

import org.junit.Test;

public class SummonDropHandlerTest {

    @Test
    public void ordinaryRemoteEntityDropsAreUntouched() {
        EntityLivingBase entity = mock(EntityLivingBase.class);
        World world = mock(World.class);
        world.isRemote = true;
        entity.worldObj = world;
        when(entity.getEntityData()).thenReturn(new NBTTagCompound());
        LivingDropsEvent event = new LivingDropsEvent(entity, mock(DamageSource.class), new ArrayList<>(), 0, false, 0);

        new SummonDropHandler().onLivingDrops(event);

        assertFalse(event.isCanceled());
    }
}
