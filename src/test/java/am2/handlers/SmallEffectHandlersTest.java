package am2.handlers;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;

import net.minecraft.block.Block;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.BreakSpeed;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import am2.buffs.ArsMagicaPotion;
import am2.buffs.BuffList;

public class SmallEffectHandlersTest {

    private ArsMagicaPotion previousFury;
    private ArsMagicaPotion previousTemporalAnchor;

    @Before
    public void stubPotions() throws Exception {
        previousFury = BuffList.fury;
        previousTemporalAnchor = BuffList.temporalAnchor;
        setPotion("fury", mock(ArsMagicaPotion.class));
        setPotion("temporalAnchor", mock(ArsMagicaPotion.class));
    }

    @After
    public void restorePotions() throws Exception {
        setPotion("fury", previousFury);
        setPotion("temporalAnchor", previousTemporalAnchor);
    }

    @Test
    public void furyMultipliesBreakSpeed() {
        EntityPlayer player = mock(EntityPlayer.class);
        when(player.isPotionActive(BuffList.fury.id)).thenReturn(true);
        BreakSpeed event = new BreakSpeed(player, mock(Block.class), 0, 2.0F);
        event.newSpeed = 1.0F;

        new BreakSpeedHandler().onBreakSpeed(event);

        assertEquals(10.0F, event.newSpeed, 0.001F);
    }

    @Test
    public void noFuryLeavesBreakSpeedAlone() {
        EntityPlayer player = mock(EntityPlayer.class);
        BreakSpeed event = new BreakSpeed(player, mock(Block.class), 0, 2.0F);
        event.newSpeed = 1.0F;

        new BreakSpeedHandler().onBreakSpeed(event);

        assertEquals(1.0F, event.newSpeed, 0.001F);
    }

    @Test
    public void activeTemporalAnchorIsRemovedOnJoin() {
        EntityLivingBase entity = mock(EntityLivingBase.class);
        when(entity.isPotionActive(BuffList.temporalAnchor.id)).thenReturn(true);
        EntityJoinWorldEvent event = new EntityJoinWorldEvent(entity, mock(World.class));

        new TemporalAnchorHandler().onEntityJoinWorld(event);

        verify(entity).removePotionEffect(BuffList.temporalAnchor.id);
    }

    @Test
    public void absentTemporalAnchorDoesNotCancelDeath() {
        EntityLivingBase entity = mock(EntityLivingBase.class);
        LivingDeathEvent event = new LivingDeathEvent(entity, mock(DamageSource.class));

        new TemporalAnchorHandler().onEntityDeathChrono(event);

        org.junit.Assert.assertFalse(event.isCanceled());
    }

    @Test
    public void absentTemporalAnchorIsNotTouched() {
        EntityLivingBase entity = mock(EntityLivingBase.class);
        EntityJoinWorldEvent event = new EntityJoinWorldEvent(entity, mock(World.class));

        new TemporalAnchorHandler().onEntityJoinWorld(event);

        verify(entity, never()).removePotionEffect(anyInt());
    }

    private static void setPotion(String fieldName, ArsMagicaPotion value) throws Exception {
        Field field = BuffList.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(null, value);
    }
}
