package am2.handlers;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;

import net.minecraft.entity.EntityLivingBase;
import net.minecraftforge.event.entity.living.LivingEvent.LivingJumpEvent;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import am2.buffs.ArsMagicaPotion;
import am2.buffs.BuffList;
import am2.playerextensions.ExtendedProperties;

public class JumpHandlerTest {

    private ArsMagicaPotion agility;
    private ArsMagicaPotion leap;
    private ArsMagicaPotion entangled;
    private ArsMagicaPotion haste;

    @Before
    public void stubBuffs() throws Exception {
        agility = BuffList.agility;
        leap = BuffList.leap;
        entangled = BuffList.entangled;
        haste = BuffList.haste;
        set("agility");
        set("leap");
        set("entangled");
        set("haste");
    }

    @After
    public void restoreBuffs() throws Exception {
        set("agility", agility);
        set("leap", leap);
        set("entangled", entangled);
        set("haste", haste);
    }

    @Test
    public void potionFreeJumpIsANoOp() {
        EntityLivingBase entity = mock(EntityLivingBase.class);
        when(entity.getExtendedProperties(ExtendedProperties.identifier)).thenReturn(new ExtendedProperties());

        new JumpHandler().onEntityJump(new LivingJumpEvent(entity));
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
