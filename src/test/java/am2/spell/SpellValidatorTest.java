package am2.spell;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.util.ArrayList;

import org.junit.Test;

import am2.api.spell.component.interfaces.ISpellComponent;
import am2.api.spell.component.interfaces.ISpellPart;
import am2.api.spell.component.interfaces.ISpellShape;
import am2.spell.components.Summon;

public class SpellValidatorTest {

    @Test
    public void emptyRecipesAreInvalid() {
        assertFalse(SpellValidator.instance.spellDefIsValid(groups(), groups()).valid);
    }

    @Test
    public void validShapeAndComponentStagesAreAccepted() throws Exception {
        ISpellShape shape = shape(false, false);
        ISpellComponent component = mock(ISpellComponent.class);

        assertValid(shape, component);
        assertTrue(
            SpellValidator.instance
                .spellDefIsValid(groups(list(shape, component)), new ArrayList<ArrayList<ISpellPart>>()).valid);
        assertTrue(SpellValidator.instance.spellDefIsValid(groups(), groups(list(shape, component))).valid);
    }

    @Test
    public void aStageWithoutAComponentIsInvalid() throws Exception {
        assertEquals("NOT_VALID", validateStage(list(shape(false, false)), true));
        assertEquals("VALID", validateStage(list(shape(false, false)), false));

        ISpellShape shape = shape(false, false);
        SpellValidator.ValidationResult result = SpellValidator.instance.spellDefIsValid(groups(), groups(list(shape)));
        assertFalse(result.valid);
        assertSame(shape, result.offendingPart);
    }

    @Test
    public void terminusAndPrincipumShapesAreReported() throws Exception {
        assertEquals("TERMINUS", validateStage(list(shape(true, false), mock(ISpellComponent.class)), false));
        assertEquals("PRINCIPUM", validateStage(list(shape(false, true), mock(ISpellComponent.class)), false));
    }

    @Test
    public void summonIsAlwaysATerminus() throws Exception {
        assertEquals("TERMINUS", validateStage(list(mock(Summon.class), shape(false, false)), false));
    }

    @Test
    public void splittingStartsANewStageAtEachShape() {
        ArrayList<ArrayList<ISpellPart>> stages = SpellValidator.splitToStages(
            list(shape(false, false), mock(ISpellComponent.class), shape(false, false), mock(ISpellComponent.class)));

        assertEquals(2, stages.size());
        assertEquals(
            2,
            stages.get(0)
                .size());
        assertEquals(
            2,
            stages.get(1)
                .size());
    }

    @Test
    public void splittingAnEmptyRecipeProducesNoStages() {
        assertTrue(
            SpellValidator.splitToStages(new ArrayList<ISpellPart>())
                .isEmpty());
    }

    private static void assertValid(ISpellShape shape, ISpellComponent component) throws Exception {
        assertEquals("VALID", validateStage(list(shape, component), false));
        assertEquals("VALID", validateStage(list(shape, component), true));
    }

    private static String validateStage(ArrayList<ISpellPart> stage, boolean finalStage) throws Exception {
        Method method = SpellValidator.class.getDeclaredMethod("validateStage", ArrayList.class, boolean.class);
        method.setAccessible(true);
        Object result = method.invoke(SpellValidator.instance, stage, finalStage);
        return result.toString();
    }

    private static ISpellShape shape(boolean terminus, boolean principum) {
        ISpellShape shape = mock(ISpellShape.class);
        when(shape.isTerminusShape()).thenReturn(terminus);
        when(shape.isPrincipumShape()).thenReturn(principum);
        return shape;
    }

    private static ArrayList<ISpellPart> list(ISpellPart... parts) {
        ArrayList<ISpellPart> list = new ArrayList<ISpellPart>();
        for (ISpellPart part : parts) list.add(part);
        return list;
    }

    @SafeVarargs
    private static ArrayList<ArrayList<ISpellPart>> groups(ArrayList<ISpellPart>... groups) {
        ArrayList<ArrayList<ISpellPart>> list = new ArrayList<ArrayList<ISpellPart>>();
        for (ArrayList<ISpellPart> group : groups) list.add(group);
        return list;
    }

}
