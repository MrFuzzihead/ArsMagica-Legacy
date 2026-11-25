package am2.handlers;

import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;

import net.minecraft.entity.EntityLivingBase;
import net.minecraftforge.event.entity.living.LivingFallEvent;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import am2.buffs.ArsMagicaPotion;
import am2.buffs.BuffList;
import am2.playerextensions.ExtendedProperties;

public class FallHandlerTest {

    private ArsMagicaPotion slowfall;
    private ArsMagicaPotion shrink;
    private ArsMagicaPotion gravityWell;

    @Before
    public void stubBuffs() throws Exception {
        slowfall = BuffList.slowfall;
        shrink = BuffList.shrink;
        gravityWell = BuffList.gravityWell;
        set("slowfall");
        set("shrink");
        set("gravityWell");
    }

    @After
    public void restoreBuffs() throws Exception {
        set("slowfall", slowfall);
        set("shrink", shrink);
        set("gravityWell", gravityWell);
    }

    @Test
    public void ordinaryFallIsNotCancelled() {
        EntityLivingBase entity = mock(EntityLivingBase.class);
        ExtendedProperties properties = mock(ExtendedProperties.class);
        when(entity.getExtendedProperties(ExtendedProperties.identifier)).thenReturn(properties);
        when(properties.getFallProtection()).thenReturn(0);

        LivingFallEvent event = new LivingFallEvent(entity, 10.0F);
        new FallHandler().onEntityFall(event);

        assertFalse(event.isCanceled());
    }

    private static void set(String name) throws Exception {
        set(name, mock(ArsMagicaPotion.class));
    }

    private static void set(String name, ArsMagicaPotion value) throws Exception {
        Field field = BuffList.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(null, value);
    }
}
