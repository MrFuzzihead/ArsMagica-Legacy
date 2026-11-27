package am2.handlers;

import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.item.ItemSnowball;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import am2.buffs.ArsMagicaPotion;
import am2.buffs.BuffList;
import am2.playerextensions.ExtendedProperties;

public class LivingHurtHandlerTest {

    private ArsMagicaPotion magicShield;
    private ArsMagicaPotion manaShield;
    private ArsMagicaPotion fury;
    private ArsMagicaPotion shrink;

    @Before
    public void stubBuffs() throws Exception {
        magicShield = BuffList.magicShield;
        manaShield = BuffList.manaShield;
        fury = BuffList.fury;
        shrink = BuffList.shrink;
        set("magicShield");
        set("manaShield");
        set("fury");
        set("shrink");
    }

    @After
    public void restoreBuffs() throws Exception {
        set("magicShield", magicShield);
        set("manaShield", manaShield);
        set("fury", fury);
        set("shrink", shrink);
    }

    @Test
    public void ordinaryHurtWithSnowballContingenciesIsNotCancelled() {
        EntityLivingBase entity = mock(EntityLivingBase.class);
        IAttributeInstance knockback = mock(IAttributeInstance.class);
        when(entity.getEntityAttribute(SharedMonsterAttributes.knockbackResistance)).thenReturn(knockback);
        ExtendedProperties properties = mock(ExtendedProperties.class);
        when(entity.getExtendedProperties(ExtendedProperties.identifier)).thenReturn(properties);
        ItemStack snowball = mock(ItemStack.class);
        when(snowball.getItem()).thenReturn(mock(ItemSnowball.class));
        when(properties.getContingencyEffect(0)).thenReturn(snowball);
        when(properties.getContingencyEffect(4)).thenReturn(snowball);

        LivingHurtEvent event = new LivingHurtEvent(entity, mock(DamageSource.class), 5.0F);
        new LivingHurtHandler().onEntityHurt(event);

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
