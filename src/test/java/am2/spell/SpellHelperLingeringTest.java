package am2.spell;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import net.minecraft.world.World;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Pins the per-world lingering-spell tick: a spell is advanced only by its own world and only from the single server
 * tick site.
 */
public class SpellHelperLingeringTest {

    @Before
    public void clearBefore() {
        SpellHelper.lingeringSpellList.clear();
    }

    @After
    public void clearAfter() {
        SpellHelper.lingeringSpellList.clear();
    }

    @Test
    public void onlySpellsFromTheTickedWorldAdvance() {
        World worldA = mock(World.class);
        World worldB = mock(World.class);
        SpellHelper.LingeringSpell a = spellIn(worldA, true);
        SpellHelper.LingeringSpell b = spellIn(worldB, false);
        SpellHelper.lingeringSpellList.add(a);
        SpellHelper.lingeringSpellList.add(b);

        SpellHelper.tickLingeringSpells(worldA);

        assertFalse(SpellHelper.lingeringSpellList.contains(a));
        assertTrue(SpellHelper.lingeringSpellList.contains(b));
        verify(a).doUpdate();
        verify(b, never()).doUpdate();
    }

    @Test
    public void clearWorldRemovesOnlyThatWorldsSpells() {
        World worldA = mock(World.class);
        World worldB = mock(World.class);
        SpellHelper.LingeringSpell a = spellIn(worldA, false);
        SpellHelper.LingeringSpell b = spellIn(worldB, false);
        SpellHelper.lingeringSpellList.add(a);
        SpellHelper.lingeringSpellList.add(b);

        SpellHelper.clearLingeringSpells(worldA);

        assertFalse(SpellHelper.lingeringSpellList.contains(a));
        assertTrue(SpellHelper.lingeringSpellList.contains(b));
    }

    private static SpellHelper.LingeringSpell spellIn(World world, boolean removeAfterUpdate) {
        SpellHelper.LingeringSpell spell = mock(SpellHelper.LingeringSpell.class);
        spell.world = world;
        when(spell.doUpdate()).thenReturn(removeAfterUpdate);
        return spell;
    }
}
