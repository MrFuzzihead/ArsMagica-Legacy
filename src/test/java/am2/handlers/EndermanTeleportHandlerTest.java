package am2.handlers;

import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.mock;

import java.lang.reflect.Field;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.living.EnderTeleportEvent;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import am2.buffs.ArsMagicaPotion;
import am2.buffs.BuffList;

public class EndermanTeleportHandlerTest {

    private ArsMagicaPotion previousDistortion;

    @Before
    public void stubPotion() throws Exception {
        previousDistortion = BuffList.astralDistortion;
        Field field = BuffList.class.getDeclaredField("astralDistortion");
        field.setAccessible(true);
        field.set(null, mock(ArsMagicaPotion.class));
    }

    @After
    public void restorePotion() throws Exception {
        Field field = BuffList.class.getDeclaredField("astralDistortion");
        field.setAccessible(true);
        field.set(null, previousDistortion);
    }

    @Test
    public void ordinaryEntityTeleportIsNotCancelled() {
        EntityLivingBase entity = mock(EntityLivingBase.class);
        World world = mock(World.class);
        world.isRemote = true;
        entity.worldObj = world;
        EnderTeleportEvent event = new EnderTeleportEvent(entity, 0.0D, 64.0D, 0.0D, 0.0F);

        new EndermanTeleportHandler().onEndermanTeleport(event);

        assertFalse(event.isCanceled());
    }
}
