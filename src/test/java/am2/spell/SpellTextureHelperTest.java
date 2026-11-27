package am2.spell;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

public class SpellTextureHelperTest {

    @Test
    public void resourceListingIsStableOutsideADevelopmentJar() throws Exception {
        List<String> resources = SpellTextureHelper.getResourceListing();

        assertNotNull(resources);
        for (int i = 1; i < resources.size(); i++) {
            assertTrue(
                resources.get(i - 1)
                    .compareToIgnoreCase(resources.get(i)) <= 0);
        }
    }
}
