package am2.api.power;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import am2.blocks.tileentities.TileEntityManaBatteryNew;

/**
 * The second mana API is not used by AM2's active power system. Keep it visibly deprecated, and keep the fully
 * commented-out dead types out of the artifact.
 */
public class LegacyManaApiTest {

    private static final Class<?>[] LEGACY_TYPES = { Mana.class, ManaStack.class, ManaRegistry.class,
        ManaContainer.class, IManaContainer.class, IManaHandler.class, TileManaHandler.class, ManaContainerInfo.class,
        TileEntityManaBatteryNew.class };

    @Test
    public void legacyTypesAreMarkedDeprecated() {
        for (Class<?> type : LEGACY_TYPES) {
            assertTrue(type.getName() + " must be deprecated", type.isAnnotationPresent(Deprecated.class));
        }
    }

    @Test
    public void fullyCommentedOutTypesAreNotInTheArtifact() {
        assertFalse(classExists("am2.api.power.IManaContainerItem"));
        assertFalse(classExists("am2.api.power.ManaContainerRegistry"));
        assertFalse(classExists("am2.blocks.tileentities.TileEntityAMPowerNew"));
    }

    private static boolean classExists(String name) {
        try {
            Class.forName(name);
            return true;
        } catch (ClassNotFoundException expected) {
            return false;
        }
    }
}
