package am2.blocks.tileentities.flickers;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;

import java.lang.reflect.Field;

import net.minecraft.world.World;
import net.minecraft.world.WorldProvider;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import am2.api.math.AMVector3;
import am2.blocks.tileentities.TileEntityFlickerHabitat;

/**
 * Pins the dimension-scoped meteor attractors: an attractor in one dimension must not steer a meteor in another.
 */
public class FlickerOperatorMoonstoneAttractorTest {

    private final FlickerOperatorMoonstoneAttractor operator = new FlickerOperatorMoonstoneAttractor();

    @Before
    @After
    public void clearAttractors() {
        FlickerOperatorMoonstoneAttractor.clearAttractors();
    }

    @Test
    public void attractorOnlyAppliesToItsOwnDimension() throws Exception {
        World overworld = world(0);
        TileEntityFlickerHabitat habitat = mock(TileEntityFlickerHabitat.class);

        operator.DoOperation(overworld, habitat, true);

        AMVector3 target = new AMVector3(0, 0, 0);
        assertNotNull(FlickerOperatorMoonstoneAttractor.getMeteorAttractor(0, target));
        assertNull(FlickerOperatorMoonstoneAttractor.getMeteorAttractor(-1, target));
    }

    @Test
    public void removingTheOperatorRemovesTheAttractor() throws Exception {
        World overworld = world(0);
        TileEntityFlickerHabitat habitat = mock(TileEntityFlickerHabitat.class);
        operator.DoOperation(overworld, habitat, true);

        operator.RemoveOperator(overworld, habitat, true);

        assertNull(FlickerOperatorMoonstoneAttractor.getMeteorAttractor(0, new AMVector3(0, 0, 0)));
    }

    private static World world(int dimension) throws Exception {
        World world = mock(World.class);
        WorldProvider provider = mock(WorldProvider.class);
        provider.dimensionId = dimension;
        Field field = World.class.getDeclaredField("provider");
        field.setAccessible(true);
        field.set(world, provider);
        return world;
    }
}
