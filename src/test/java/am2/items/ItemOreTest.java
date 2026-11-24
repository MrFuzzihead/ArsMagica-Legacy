package am2.items;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;

import org.junit.Test;

/**
 * Covers the per-item replacement for the global {@code EntityItem.onUpdate} coremod patch. The liquid-etherium
 * branch needs a real world/material and is covered by the in-game test instead.
 */
public class ItemOreTest {

    @Test
    public void cognitiveDustDriftsUpward() {
        EntityItem entityItem = entityItem(ItemOre.META_COGNITIVEDUST);
        new ItemOre().onEntityItemUpdate(entityItem);

        assertEquals(0.025D, entityItem.motionY, 1.0E-9D);
        verify(entityItem).moveEntity(0.0D, 0.0D, 0.0D);
    }

    @Test
    public void otherOresAreUnaffectedOutsideEtherium() {
        EntityItem entityItem = entityItem(ItemOre.META_VINTEUMDUST);
        new ItemOre().onEntityItemUpdate(entityItem);

        assertEquals(0.0D, entityItem.motionY, 1.0E-9D);
        verify(entityItem, never()).moveEntity(anyDouble(), anyDouble(), anyDouble());
    }

    private static EntityItem entityItem(int meta) {
        EntityItem entityItem = mock(EntityItem.class);
        ItemStack stack = mock(ItemStack.class);
        when(stack.getItemDamage()).thenReturn(meta);
        when(entityItem.getEntityItem()).thenReturn(stack);
        return entityItem;
    }
}
