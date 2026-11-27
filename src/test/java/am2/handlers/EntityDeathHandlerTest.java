package am2.handlers;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemSnowball;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;
import net.minecraftforge.event.entity.living.LivingDeathEvent;

import org.junit.Test;

import am2.playerextensions.ExtendedProperties;

public class EntityDeathHandlerTest {

    @Test
    public void snowballContingencyAndNonSummonAreANoOp() {
        EntityLivingBase entity = mock(EntityLivingBase.class);
        ExtendedProperties properties = mock(ExtendedProperties.class);
        ItemStack snowball = mock(ItemStack.class);
        when(snowball.getItem()).thenReturn(mock(ItemSnowball.class));
        when(properties.getContingencyEffect(1)).thenReturn(snowball);
        when(entity.getExtendedProperties(ExtendedProperties.identifier)).thenReturn(properties);
        when(entity.getEntityData()).thenReturn(new NBTTagCompound());
        LivingDeathEvent event = new LivingDeathEvent(entity, mock(DamageSource.class));

        new EntityDeathHandler().onEntityDeath(event);
    }
}
