package am2.playerextensions;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import net.minecraft.entity.player.EntityPlayer;

import org.junit.Test;

/**
 * Verifies all three synced stores retain their one-send-then-wait behavior after moving the countdown into
 * {@link SyncWindow}.
 */
public class SyncWindowIntegrationTest {

    @Test
    public void extendedPropertiesDelayedSyncSendsOnceThenWaits() {
        ExtendedProperties properties = new ExtendedProperties();
        properties.setDelayedSync(1);

        assertTrue(properties.getHasUpdate());
        assertFalse(properties.getHasUpdate());
    }

    @Test
    public void affinityDataDelayedSyncSendsOnceThenWaits() {
        AffinityData affinity = new AffinityData();
        affinity.setDelayedSync(1);

        assertTrue(affinity.hasUpdate());
        assertFalse(affinity.hasUpdate());
    }

    @Test
    public void skillDataDelayedSyncSendsOnceThenWaits() {
        SkillData skill = new SkillData(mock(EntityPlayer.class));
        skill.setDelayedSync(1);

        assertTrue(skill.hasUpdate());
        assertFalse(skill.hasUpdate());
    }
}
