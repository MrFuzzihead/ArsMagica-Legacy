package am2.handlers;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.event.entity.player.AttackEntityEvent;

import org.junit.Test;

public class SoulspikeAttackHandlerTest {

    @Test
    public void emptyHandDoesNothing() {
        EntityPlayer player = mock(EntityPlayer.class);
        Entity target = mock(Entity.class);

        new SoulspikeAttackHandler().onAttack(new AttackEntityEvent(player, target));

        verifyNoInteractions(target);
    }

    @Test
    public void nonSoulspikeWeaponDoesNothing() {
        EntityPlayer player = mock(EntityPlayer.class);
        Entity target = mock(Entity.class);
        ItemStack held = mock(ItemStack.class);
        when(held.getItem()).thenReturn(mock(Item.class));
        when(player.getHeldItem()).thenReturn(held);

        new SoulspikeAttackHandler().onAttack(new AttackEntityEvent(player, target));

        verifyNoInteractions(target);
    }
}
