package am2.handlers;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import am2.api.events.ManaCostEvent;
import am2.items.ItemArcaneGuardianSpellbook;
import am2.items.ItemsCommonProxy;

public class ManaCostHandlerTest {

    private ItemArcaneGuardianSpellbook previousSpellbook;

    @Before
    public void replaceSpellbook() {
        previousSpellbook = ItemsCommonProxy.arcaneSpellbook;
        ItemsCommonProxy.arcaneSpellbook = mock(ItemArcaneGuardianSpellbook.class);
    }

    @After
    public void restoreSpellbook() {
        ItemsCommonProxy.arcaneSpellbook = previousSpellbook;
    }

    @Test
    public void spellbookAppliesTheDiscount() {
        EntityLivingBase caster = mock(EntityLivingBase.class);
        ItemStack held = mock(ItemStack.class);
        when(held.getItem()).thenReturn(ItemsCommonProxy.arcaneSpellbook);
        when(caster.getHeldItem()).thenReturn(held);
        ManaCostEvent event = new ManaCostEvent(null, caster, 100.0F, 50.0F);

        new ManaCostHandler().onManaCost(event);

        assertEquals(75.0F, event.manaCost, 0.001F);
        assertEquals(20.0F, event.burnout, 0.001F);
    }

    @Test
    public void otherHeldItemsDoNotDiscount() {
        EntityLivingBase caster = mock(EntityLivingBase.class);
        ItemStack held = mock(ItemStack.class);
        when(held.getItem()).thenReturn(mock(Item.class));
        when(caster.getHeldItem()).thenReturn(held);
        ManaCostEvent event = new ManaCostEvent(null, caster, 100.0F, 50.0F);

        new ManaCostHandler().onManaCost(event);

        assertEquals(100.0F, event.manaCost, 0.001F);
        assertEquals(50.0F, event.burnout, 0.001F);
    }
}
