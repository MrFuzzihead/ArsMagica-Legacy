package am2.api.math;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import net.minecraft.nbt.NBTTagCompound;

import org.junit.Test;

public class AMVector3Test {

    private static final float EPSILON = 0.0001F;

    @Test
    public void constructorAndCopyKeepComponents() {
        AMVector3 vector = new AMVector3(1.5D, 2.5D, 3.5D);

        assertEquals(1.5F, vector.x, EPSILON);
        assertEquals(2.5F, vector.y, EPSILON);
        assertEquals(3.5F, vector.z, EPSILON);

        AMVector3 copy = vector.copy();
        assertEquals(vector, copy);
        assertNotSame(vector, copy);
    }

    @Test
    public void arithmeticMutatesAndReturnsThis() {
        AMVector3 vector = new AMVector3(1, 2, 3);

        assertSame(vector, vector.add(new AMVector3(4, 5, 6)));
        assertEquals(new AMVector3(5, 7, 9), vector);

        assertSame(vector, vector.sub(new AMVector3(1, 2, 3)));
        assertEquals(new AMVector3(4, 5, 6), vector);

        assertSame(vector, vector.scale(2));
        assertEquals(new AMVector3(8, 10, 12), vector);

        assertSame(vector, vector.scale(0.5F, 2F, 0.25F));
        assertEquals(new AMVector3(4, 20, 3), vector);

        assertSame(vector, vector.modulo(3));
        assertEquals(new AMVector3(1, 2, 0), vector);
    }

    @Test
    public void lengthAndNormalisation() {
        AMVector3 vector = new AMVector3(3, 4, 0);
        assertEquals(5F, vector.length(), EPSILON);
        assertEquals(25F, vector.lengthPow2(), EPSILON);

        vector.normalize();
        assertEquals(0.6F, vector.x, EPSILON);
        assertEquals(0.8F, vector.y, EPSILON);
        assertEquals(0F, vector.z, EPSILON);
        assertEquals(1F, vector.length(), EPSILON);
    }

    @Test
    public void crossAndDotProducts() {
        AMVector3 x = new AMVector3(1, 0, 0);
        AMVector3 y = new AMVector3(0, 1, 0);

        assertEquals(new AMVector3(0, 0, 1), AMVector3.crossProduct(x, y));
        assertEquals(0F, AMVector3.dotProduct(x, y), EPSILON);
        assertEquals((float) (Math.PI / 2D), AMVector3.angle(x, y), EPSILON);
        assertEquals(new AMVector3(0, 0, -1), AMVector3.xCrossProduct(y));
        assertEquals(new AMVector3(-1, 0, 0), AMVector3.zCrossProduct(y));
        assertEquals(new AMVector3(0, 1, 0), AMVector3.getPerpendicular(new AMVector3(1, 0, 0)));
    }

    @Test
    public void rotationUsesDegrees() {
        AMVector3 vector = new AMVector3(1, 0, 0).rotate(90, new AMVector3(0, 1, 0));

        assertEquals(0F, vector.x, EPSILON);
        assertEquals(0F, vector.y, EPSILON);
        assertEquals(1F, vector.z, EPSILON);
    }

    @Test
    public void predicatesAndDistances() {
        assertTrue(
            AMVector3.zero()
                .isZero());
        assertFalse(new AMVector3(1, 0, 0).isZero());
        assertTrue(new AMVector3(1, 2, 3).isWithinRange(0, 4));
        assertFalse(new AMVector3(1, 2, 5).isWithinRange(0, 4));

        AMVector3 from = new AMVector3(0, 0, 0);
        AMVector3 to = new AMVector3(3, 4, 0);
        assertEquals(5D, from.distanceTo(to), 0.0001D);
        assertEquals(25D, from.distanceSqTo(to), 0.0001D);
    }

    @Test
    public void roundingHelpers() {
        AMVector3 floor = new AMVector3(1.9F, -1.1F, 2.0F);
        floor.floorToI();
        assertEquals(new AMVector3(1, -2, 2), floor);

        AMVector3 round = new AMVector3(1.4F, 1.5F, -1.5F);
        round.roundToI();
        assertEquals(new AMVector3(1, 2, -1), round);

        AMVector3 ceil = new AMVector3(1.1F, -1.9F, 2F);
        ceil.ceilToI();
        assertEquals(new AMVector3(2, -1, 2), ceil);
    }

    @Test
    public void nbtRoundTrip() {
        AMVector3 original = new AMVector3(1.25F, -2.5F, 3.75F);
        NBTTagCompound tag = new NBTTagCompound();

        original.writeToNBT(tag);

        assertEquals(original, AMVector3.readFromNBT(tag));
    }

    @Test
    public void equalityAndStringForm() {
        AMVector3 vector = new AMVector3(1, 2, 3);

        assertEquals(vector, new AMVector3(1, 2, 3));
        assertFalse(vector.equals(new AMVector3(1, 2, 4)));
        assertFalse(vector.equals("not a vector"));
        assertEquals((int) (1 + 2 + 3), vector.hashCode());
        assertEquals("[1.0,2.0,3.0]", vector.toString());
    }
}
