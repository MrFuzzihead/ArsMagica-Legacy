package am2.api.math;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class AMVector2Test {

    private static final double EPSILON = 0.0001D;

    @Test
    public void constructorKeepsComponentsAndIntegerTruncation() {
        AMVector2 vector = new AMVector2(1.9D, -2.9D);

        assertEquals(1.9D, vector.x, EPSILON);
        assertEquals(-2.9D, vector.y, EPSILON);
        assertEquals(1, vector.iX);
        assertEquals(-2, vector.iY);
    }

    @Test
    public void arithmeticReturnsNewVectors() {
        AMVector2 left = new AMVector2(4, 8);
        AMVector2 right = new AMVector2(2, 2);

        assertEquals(6D, left.add(right).x, EPSILON);
        assertEquals(10D, left.add(right).y, EPSILON);
        assertEquals(2D, left.subtract(right).x, EPSILON);
        assertEquals(6D, left.subtract(right).y, EPSILON);
        assertEquals(8D, left.multiply(right).x, EPSILON);
        assertEquals(16D, left.multiply(right).y, EPSILON);
        assertEquals(2D, left.divide(right).x, EPSILON);
        assertEquals(4D, left.divide(right).y, EPSILON);
    }
}
