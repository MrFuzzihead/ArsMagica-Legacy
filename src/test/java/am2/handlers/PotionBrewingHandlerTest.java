package am2.handlers;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import net.minecraft.item.Item;
import net.minecraft.item.ItemPotion;
import net.minecraft.item.ItemStack;
import net.minecraftforge.event.brewing.PotionBrewedEvent;

import org.junit.Test;

public class PotionBrewingHandlerTest {

    @Test
    public void nullAndNonPotionStacksAreIgnored() {
        PotionBrewedEvent event = mock(PotionBrewedEvent.class);
        ItemStack nonPotion = mock(ItemStack.class);
        when(nonPotion.getItem()).thenReturn(mock(Item.class));
        event.brewingStacks = new ItemStack[] { null, nonPotion };

        new PotionBrewingHandler().onPotionBrewed(event);
    }

    @Test
    public void potionWithoutEffectsDoesNotChangeTheResult() {
        PotionBrewedEvent event = mock(PotionBrewedEvent.class);
        ItemStack stack = mock(ItemStack.class);
        ItemPotion potion = mock(ItemPotion.class);
        when(stack.getItem()).thenReturn(potion);
        when(stack.getItemDamage()).thenReturn(0);
        when(potion.getEffects(0)).thenReturn(null);
        event.brewingStacks = new ItemStack[] { stack };

        new PotionBrewingHandler().onPotionBrewed(event);
    }
}
