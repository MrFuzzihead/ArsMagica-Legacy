package am2.api;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;

import net.minecraft.entity.player.EntityPlayer;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import am2.playerextensions.AffinityData;
import am2.playerextensions.ExtendedProperties;
import am2.playerextensions.RiftStorage;
import am2.playerextensions.SkillData;

/**
 * The API getters are called by addons at uncertain lifecycle points; they must return null rather than dereferencing a
 * null entity or an uninitialised identifier.
 */
public class ArsMagicaApiPropertiesTest {

    private String extended;
    private String affinity;
    private String skill;
    private String rift;

    @Before
    public void clearIdentifiers() throws Exception {
        extended = read("extendedPropertiesIdentifier");
        affinity = read("affinityDataIdentifier");
        skill = read("skillDataIdentifier");
        rift = read("riftStorageIdentifier");
        write("extendedPropertiesIdentifier", null);
        write("affinityDataIdentifier", null);
        write("skillDataIdentifier", null);
        write("riftStorageIdentifier", null);
    }

    @After
    public void restoreIdentifiers() throws Exception {
        write("extendedPropertiesIdentifier", extended);
        write("affinityDataIdentifier", affinity);
        write("skillDataIdentifier", skill);
        write("riftStorageIdentifier", rift);
    }

    @Test
    public void nullEntitiesReturnNull() {
        ArsMagicaApi api = ArsMagicaApi.instance;
        assertNull(api.getExtendedProperties(null));
        assertNull(api.getSkillData(null));
        assertNull(api.getAffinityData(null));
        assertNull(api.getRiftStorage(null));
    }

    @Test
    public void uninitialisedIdentifiersReturnNullWithoutTouchingTheEntity() {
        EntityPlayer player = mock(EntityPlayer.class);
        ArsMagicaApi api = ArsMagicaApi.instance;

        assertNull(api.getExtendedProperties(player));
        assertNull(api.getSkillData(player));
        assertNull(api.getAffinityData(player));
        assertNull(api.getRiftStorage(player));
        verifyNoInteractions(player);
    }

    @Test
    public void initialisedIdentifiersResolveTheRegisteredProperties() {
        EntityPlayer player = mock(EntityPlayer.class);
        ExtendedProperties extendedProperties = new ExtendedProperties();
        SkillData skillData = mock(SkillData.class);
        AffinityData affinityData = mock(AffinityData.class);
        RiftStorage riftStorage = mock(RiftStorage.class);

        when(player.getExtendedProperties("extended")).thenReturn(extendedProperties);
        when(player.getExtendedProperties("skill")).thenReturn(skillData);
        when(player.getExtendedProperties("affinity")).thenReturn(affinityData);
        when(player.getExtendedProperties("rift")).thenReturn(riftStorage);

        ArsMagicaApi api = ArsMagicaApi.instance;
        api.setExtendedPropertiesID("extended");
        api.setSkillDataID("skill");
        api.setAffinityDataID("affinity");
        api.setRiftStorageID("rift");

        assertSame(extendedProperties, api.getExtendedProperties(player));
        assertSame(skillData, api.getSkillData(player));
        assertSame(affinityData, api.getAffinityData(player));
        assertSame(riftStorage, api.getRiftStorage(player));
    }

    private static String read(String name) throws Exception {
        Field field = ArsMagicaApi.class.getDeclaredField(name);
        field.setAccessible(true);
        return (String) field.get(null);
    }

    private static void write(String name, String value) throws Exception {
        Field field = ArsMagicaApi.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(null, value);
    }
}
