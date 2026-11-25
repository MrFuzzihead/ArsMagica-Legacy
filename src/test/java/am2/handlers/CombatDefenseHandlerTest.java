package am2.handlers;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.DamageSource;
import net.minecraftforge.event.entity.living.LivingAttackEvent;

import org.junit.Test;

public class CombatDefenseHandlerTest {

    @Test
    public void ordinaryAttackIsUntouched() {
        EntityLivingBase entity = mock(EntityLivingBase.class);
        DamageSource source = mock(DamageSource.class);
        LivingAttackEvent event = new LivingAttackEvent(entity, source, 5.0F);

        new CombatDefenseHandler().onEntityAttacked(event);

        verify(entity, never()).setFire(anyInt());
    }
}
