package am2;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

/**
 * Pins the IMC blacklist parser. The old {@code split("|")} treated the pipe as a regex alternation and returned every
 * character, so every message looked malformed.
 */
public class AMCoreImcParsingTest {

    @Test
    public void parsesTheDocumentedSyntax() {
        assertArrayEquals(new String[] { "EntityDryad", "22" }, AMCore.parseBlacklistImc("EntityDryad|22"));
        assertArrayEquals(
            new String[] { "am2.entities.EntityDryad", "-1" },
            AMCore.parseBlacklistImc("am2.entities.EntityDryad|-1"));
    }

    @Test
    public void trimsWhitespace() {
        assertArrayEquals(new String[] { "EntityDryad", "22" }, AMCore.parseBlacklistImc(" EntityDryad | 22 "));
    }

    @Test
    public void rejectsMissingOrExtraFields() {
        assertNull(AMCore.parseBlacklistImc(null));
        assertNull(AMCore.parseBlacklistImc("EntityDryad"));
        assertNull(AMCore.parseBlacklistImc("EntityDryad|22|33"));
        assertNull(AMCore.parseBlacklistImc("|22"));
        assertNull(AMCore.parseBlacklistImc("EntityDryad|"));
        assertNull(AMCore.parseBlacklistImc(" | "));
    }
}
