package am2.utility;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.security.InvalidParameterException;

import org.junit.Test;

import am2.api.math.AMVector3;

public class MathUtilitiesTest {

    private static final float EPSILON = 0.0001F;

    @Test
    public void normalizesRotationIntoOneTurn() {
        assertEquals(350D, MathUtilities.NormalizeRotation(-10D), 0.0001D);
        assertEquals(10D, MathUtilities.NormalizeRotation(370D), 0.0001D);
        assertEquals(0D, MathUtilities.NormalizeRotation(360D), 0.0001D);
        assertEquals(359D, MathUtilities.NormalizeRotation(359D), 0.0001D);
    }

    @Test
    public void colourConversionIsSymmetric() {
        float[] colours = MathUtilities.colorIntToFloats(0x336699);

        assertEquals(0x33 / 255F, colours[0], EPSILON);
        assertEquals(0x66 / 255F, colours[1], EPSILON);
        assertEquals(0x99 / 255F, colours[2], EPSILON);
        assertEquals(0x336699, MathUtilities.colorFloatsToInt(colours[0], colours[1], colours[2]));
    }

    @Test
    public void pushAndSpliceResizeArrays() {
        assertArrayEquals(new int[] { 1, 2, 3, 4 }, MathUtilities.push(new int[] { 1, 2, 3 }, 4));
        assertArrayEquals(new int[] { 1, 2, 3 }, MathUtilities.splice(new int[] { 1, 2, 9, 3 }, 2));
        assertArrayEquals(new int[] { 1 }, MathUtilities.splice(new int[] { 1 }, 0));
    }

    @Test
    public void bezierHitsItsEndpoints() {
        AMVector3 start = new AMVector3(1, 2, 3);
        AMVector3 end = new AMVector3(4, 5, 6);
        AMVector3 control1 = new AMVector3(1, 5, 3);
        AMVector3 control2 = new AMVector3(4, 2, 6);

        assertEquals(start, MathUtilities.bezier(start, control1, control2, end, 0F));
        assertEquals(end, MathUtilities.bezier(start, control1, control2, end, 1F));
    }

    @Test(expected = InvalidParameterException.class)
    public void bezierRejectsOutOfRangeParameters() {
        MathUtilities.bezier(AMVector3.zero(), AMVector3.zero(), AMVector3.zero(), AMVector3.zero(), 1.5F);
    }

    @Test
    public void movementVectorIsNormalisedDelta() {
        AMVector3 movement = MathUtilities.GetMovementVectorBetweenPoints(new AMVector3(3, 4, 0), AMVector3.zero());

        assertEquals(0.6F, movement.x, EPSILON);
        assertEquals(0.8F, movement.y, EPSILON);
        assertEquals(0F, movement.z, EPSILON);
    }
}
