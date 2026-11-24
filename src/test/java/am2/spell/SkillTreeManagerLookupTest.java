package am2.spell;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Constructor;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import am2.AMCore;
import am2.api.SkillTreeEntry;
import am2.api.spell.component.interfaces.ISkillTreeEntry;
import am2.api.spell.enums.SkillPointTypes;
import am2.api.spell.enums.SkillTrees;
import am2.configuration.SkillConfiguration;

/**
 * Covers the identity index and cached tree views that replaced the per-call tree copies in the spell hot path.
 */
public class SkillTreeManagerLookupTest {

    private SkillConfiguration previousConfig;
    private SkillTreeManager manager;

    @Before
    public void setUp() throws Exception {
        previousConfig = AMCore.skillConfig;
        AMCore.skillConfig = mock(SkillConfiguration.class);
        when(AMCore.skillConfig.isSkillEnabled(any())).thenReturn(true);

        Constructor<SkillTreeManager> constructor = SkillTreeManager.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        manager = constructor.newInstance();
    }

    @After
    public void tearDown() {
        AMCore.skillConfig = previousConfig;
    }

    @Test
    public void registeredPartIsFoundByIdentity() {
        ISkillTreeEntry part = mock(ISkillTreeEntry.class);
        manager.RegisterPart(part, 10, 20, SkillTrees.Offense, SkillPointTypes.BLUE);

        SkillTreeEntry entry = manager.getSkillTreeEntry(part);
        assertNotNull(entry);
        assertSame(part, entry.registeredItem);
        assertSame(
            entry,
            manager.getTree(SkillTrees.Offense)
                .get(0));
    }

    @Test
    public void lookupDoesNotMatchEqualButDifferentInstances() {
        ISkillTreeEntry registered = mock(ISkillTreeEntry.class);
        ISkillTreeEntry other = mock(ISkillTreeEntry.class);
        manager.RegisterPart(registered, 10, 20, SkillTrees.Utility, SkillPointTypes.GREEN);

        assertNotNull(manager.getSkillTreeEntry(registered));
        assertNull(manager.getSkillTreeEntry(other));
    }

    @Test
    public void unknownPartReturnsNull() {
        assertNull(manager.getSkillTreeEntry(mock(ISkillTreeEntry.class)));
    }

    @Test
    public void treeViewsAreCachedAndUnmodifiable() {
        List<SkillTreeEntry> first = manager.getTree(SkillTrees.Defense);
        List<SkillTreeEntry> second = manager.getTree(SkillTrees.Defense);
        assertSame(first, second);

        boolean threw = false;
        try {
            first.add(
                new SkillTreeEntry(0, 0, SkillTrees.Defense, new SkillTreeEntry[0], mock(ISkillTreeEntry.class), true));
        } catch (UnsupportedOperationException expected) {
            threw = true;
        }
        assertTrue(threw);
    }

    @Test
    public void eachTreeHasItsOwnCachedView() {
        assertSame(manager.getTree(SkillTrees.Offense), manager.getTree(SkillTrees.Offense));
        assertSame(manager.getTree(SkillTrees.Utility), manager.getTree(SkillTrees.Utility));
        assertSame(manager.getTree(SkillTrees.Talents), manager.getTree(SkillTrees.Talents));
    }
}
