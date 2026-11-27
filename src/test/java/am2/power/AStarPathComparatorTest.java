package am2.power;

import static org.junit.Assert.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.List;

import org.junit.Test;

public class AStarPathComparatorTest {

    @Test
    public void comparesFractionalCostsWithoutTruncating() throws Exception {
        AStar<String> aStar = new AStar<String>() {

            @Override
            protected boolean isGoal(String node) {
                return false;
            }

            @Override
            protected Double g(String from, String to) {
                return 0.0;
            }

            @Override
            protected Double h(String from, String to) {
                return 0.0;
            }

            @Override
            protected List<String> generateSuccessors(String node) {
                return new java.util.ArrayList<String>();
            }
        };

        Class<?> pathClass = Class.forName("am2.power.AStar$Path");
        Constructor<?> constructor = pathClass.getDeclaredConstructor(AStar.class);
        constructor.setAccessible(true);
        Object low = constructor.newInstance(aStar);
        Object high = constructor.newInstance(aStar);
        setCost(low, 1.0D);
        setCost(high, 1.5D);

        assertTrue(((Comparable<Object>) low).compareTo(high) < 0);
        assertTrue(((Comparable<Object>) high).compareTo(low) > 0);
        assertTrue(((Comparable<Object>) low).compareTo(low) == 0);
    }

    private static void setCost(Object path, double cost) throws Exception {
        Field field = path.getClass()
            .getDeclaredField("f");
        field.setAccessible(true);
        field.set(path, cost);
    }
}
