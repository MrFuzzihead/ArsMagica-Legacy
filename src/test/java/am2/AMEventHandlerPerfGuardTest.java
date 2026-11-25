package am2;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.UUID;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.entity.player.PlayerCapabilities;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import am2.buffs.ArsMagicaPotion;
import am2.buffs.BuffList;
import am2.playerextensions.AffinityData;
import am2.playerextensions.ExtendedProperties;
import am2.playerextensions.SkillData;

/**
 * The timed-effect bookkeeping in {@code onEntityLiving} mutates server worlds and reads server dimensions. These
 * guards keep it off the client and out of a null-world path.
 */
public class AMEventHandlerPerfGuardTest {

    private boolean accelerate;
    private boolean slow;
    private boolean vortex;
    private boolean fortify;
    private boolean shield;
    private ArsMagicaPotion previousFlight;
    private ArsMagicaPotion previousLevitation;
    private ArsMagicaPotion previousEntangled;
    private ArsMagicaPotion previousFrostSlowed;
    private ArsMagicaPotion previousFury;
    private ArsMagicaPotion previousHaste;

    @Before
    public void stubPotionStatics() throws Exception {
        previousFlight = BuffList.flight;
        previousLevitation = BuffList.levitation;
        previousEntangled = BuffList.entangled;
        previousFrostSlowed = BuffList.frostSlowed;
        previousFury = BuffList.fury;
        previousHaste = BuffList.haste;
        setPotion("flight", mock(ArsMagicaPotion.class));
        setPotion("levitation", mock(ArsMagicaPotion.class));
        setPotion("entangled", mock(ArsMagicaPotion.class));
        setPotion("frostSlowed", mock(ArsMagicaPotion.class));
        setPotion("fury", mock(ArsMagicaPotion.class));
        setPotion("haste", mock(ArsMagicaPotion.class));
    }

    @After
    public void restorePotionStatics() throws Exception {
        setPotion("flight", previousFlight);
        setPotion("levitation", previousLevitation);
        setPotion("entangled", previousEntangled);
        setPotion("frostSlowed", previousFrostSlowed);
        setPotion("fury", previousFury);
        setPotion("haste", previousHaste);
    }

    private static void setPotion(String fieldName, ArsMagicaPotion value) throws Exception {
        Field field = BuffList.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(null, value);
    }

    @Before
    @After
    public void clearSlowedEntities() {
        AMEventHandler.slowedEntitiesUUIDs.clear();
    }

    @Before
    public void saveFlags() {
        accelerate = AMEventHandler.enabled_accelerate;
        slow = AMEventHandler.enabled_slow;
        vortex = AMEventHandler.enable_spatialVortex;
        fortify = AMEventHandler.enabled_timeFortified;
        shield = AMEventHandler.enabled_shield;
    }

    @After
    public void restoreFlags() {
        AMEventHandler.enabled_accelerate = accelerate;
        AMEventHandler.enabled_slow = slow;
        AMEventHandler.enable_spatialVortex = vortex;
        AMEventHandler.enabled_timeFortified = fortify;
        AMEventHandler.enabled_shield = shield;
    }

    @Test
    public void remoteWorldsNeverRunTimedBookkeeping() {
        setFlags(true, true, true, true, true);
        World clientWorld = mock(World.class);
        clientWorld.isRemote = true;

        assertFalse(AMEventHandler.shouldTickAcceleratedBlocks(clientWorld));
        assertFalse(AMEventHandler.shouldTickTimedEffects(clientWorld));
    }

    @Test
    public void nullWorldsAreIgnored() {
        setFlags(true, true, true, true, true);

        assertFalse(AMEventHandler.shouldTickAcceleratedBlocks(null));
        assertFalse(AMEventHandler.shouldTickTimedEffects(null));
    }

    @Test
    public void prepareEntityPropertiesRunsTheBootstrapAndReturnsTheStore() {
        AMEventHandler handler = new AMEventHandler();
        ExtendedProperties properties = mock(ExtendedProperties.class);
        EntityLivingBase entity = mock(EntityLivingBase.class);
        when(entity.getExtendedProperties(ExtendedProperties.identifier)).thenReturn(properties);

        ExtendedProperties result = handler.prepareEntityProperties(entity);

        assertSame(properties, result);
        verify(properties).handleSpecialSyncData();
        verify(properties).manaBurnoutTick();
    }

    @Test
    public void playerPropertySyncDelegatesToAllThreeStores() {
        AMEventHandler handler = new AMEventHandler();
        ExtendedProperties properties = mock(ExtendedProperties.class);
        AffinityData affinity = mock(AffinityData.class);
        SkillData skill = mock(SkillData.class);
        EntityPlayer player = mock(EntityPlayer.class);
        when(player.getExtendedProperties(AffinityData.identifier)).thenReturn(affinity);
        when(player.getExtendedProperties(SkillData.identifier)).thenReturn(skill);

        handler.tickPropertySync(player, properties);

        verify(properties).handleExtendedPropertySync();
        verify(affinity).handleExtendedPropertySync();
        verify(skill).handleExtendedPropertySync();
    }

    @Test
    public void nonPlayerPropertySyncOnlyTouchesExtendedProperties() {
        AMEventHandler handler = new AMEventHandler();
        ExtendedProperties properties = mock(ExtendedProperties.class);
        EntityLivingBase entity = mock(EntityLivingBase.class);

        handler.tickPropertySync(entity, properties);

        verify(properties).handleExtendedPropertySync();
        verifyNoInteractions(entity);
    }

    @Test
    public void ordinaryEntityDoesNoSummonWorldWork() {
        AMEventHandler handler = new AMEventHandler();
        EntityLivingBase entity = mock(EntityLivingBase.class);
        World world = mock(World.class);
        entity.worldObj = world;
        when(entity.getEntityData()).thenReturn(new NBTTagCompound());

        handler.tickSummon(entity);

        verifyNoInteractions(world);
    }

    @Test
    public void emptyArmorAndNoInfusionIsSafe() {
        AMEventHandler handler = new AMEventHandler();
        ExtendedProperties properties = new ExtendedProperties();
        EntityPlayer player = mock(EntityPlayer.class);
        World world = mock(World.class);
        player.worldObj = world;
        player.inventory = new InventoryPlayer(player);
        player.capabilities = new PlayerCapabilities();
        when(player.getExtendedProperties(ExtendedProperties.identifier)).thenReturn(properties);
        when(player.getCurrentArmor(0)).thenReturn(null);
        when(player.getCurrentArmor(1)).thenReturn(null);
        IAttributeInstance movement = mock(IAttributeInstance.class);
        when(player.getEntityAttribute(SharedMonsterAttributes.movementSpeed)).thenReturn(movement);

        handler.tickPlayerArmor(player, properties, world);

        assertEquals(0.0F, player.stepHeight, 0.001F);
    }

    @Test
    public void emptySlowedMapNeverCancels() {
        EntityLivingBase entity = mock(EntityLivingBase.class);

        assertFalse(new AMEventHandler().shouldCancelSlowedEntity(entity));
        verifyNoInteractions(entity);
    }

    @Test
    public void slowedEntitySkipsNonIntervalTicks() {
        UUID uuid = UUID.randomUUID();
        EntityLivingBase entity = mock(EntityLivingBase.class);
        when(entity.getUniqueID()).thenReturn(uuid);
        entity.ticksExisted = 1;
        AMEventHandler.slowedEntitiesUUIDs.put(uuid, 5);

        assertTrue(new AMEventHandler().shouldCancelSlowedEntity(entity));
    }

    @Test
    public void slowedEntityRunsOnItsInterval() {
        UUID uuid = UUID.randomUUID();
        EntityLivingBase entity = mock(EntityLivingBase.class);
        when(entity.getUniqueID()).thenReturn(uuid);
        entity.ticksExisted = 10;
        AMEventHandler.slowedEntitiesUUIDs.put(uuid, 5);

        assertFalse(new AMEventHandler().shouldCancelSlowedEntity(entity));
    }

    @Test
    public void serverPlayerWithoutPsychedelicDoesNoHallucinationWork() {
        AMEventHandler handler = new AMEventHandler();
        EntityPlayer player = mock(EntityPlayer.class);
        World world = mock(World.class);
        player.worldObj = world;

        handler.tickHallucinations(player);

        verifyNoInteractions(world);
    }

    @Test
    public void serverManaLinkTickSkipsOffIntervalWorldWork() {
        AMEventHandler handler = new AMEventHandler();
        ExtendedProperties properties = new ExtendedProperties();
        EntityLivingBase entity = mock(EntityLivingBase.class);
        World world = mock(World.class);
        entity.worldObj = world;
        entity.ticksExisted = 1;

        handler.tickManaLinksAndLocalMovement(entity, properties, world);

        verifyNoInteractions(world);
    }

    @Test
    public void groundedNonBurningEntitySkipsFallContingencyAndAuraSync() {
        AMEventHandler handler = new AMEventHandler();
        ExtendedProperties properties = new ExtendedProperties();
        EntityLivingBase entity = mock(EntityLivingBase.class);
        World world = mock(World.class);
        entity.worldObj = world;
        entity.onGround = true;
        entity.fallDistance = 20.0F;
        entity.ticksExisted = 1;

        handler.tickContingenciesAndAura(entity, properties, world);

        verifyNoInteractions(world);
    }

    @Test
    public void flightIsRemovedWhenNoSourceRemains() {
        AMEventHandler handler = new AMEventHandler();
        ExtendedProperties properties = new ExtendedProperties();
        properties.hadFlight = true;
        EntityPlayer player = mock(EntityPlayer.class);
        player.capabilities = new PlayerCapabilities();

        handler.tickPlayerFlight(player, properties);

        assertFalse(properties.hadFlight);
        assertFalse(player.capabilities.allowFlying);
        assertFalse(player.capabilities.isFlying);
    }

    @Test
    public void activeFlightSourceKeepsHadFlightSet() {
        AMEventHandler handler = new AMEventHandler();
        ExtendedProperties properties = new ExtendedProperties();
        EntityPlayer player = mock(EntityPlayer.class);
        player.capabilities = new PlayerCapabilities();
        when(player.isPotionActive(BuffList.flight.id)).thenReturn(true);

        handler.tickPlayerFlight(player, properties);

        assertTrue(properties.hadFlight);
    }

    @Test
    public void levitationWhileFlyingScalesMotion() {
        AMEventHandler handler = new AMEventHandler();
        ExtendedProperties properties = new ExtendedProperties();
        EntityPlayer player = mock(EntityPlayer.class);
        player.capabilities = new PlayerCapabilities();
        player.capabilities.isFlying = true;
        player.motionX = 10.0D;
        player.motionY = 10.0D;
        player.motionZ = 10.0D;
        when(player.isPotionActive(BuffList.levitation.id)).thenReturn(true);
        when(player.isPotionActive(BuffList.levitation)).thenReturn(true);

        handler.tickPlayerFlight(player, properties);

        assertEquals(4.0D, player.motionX, 0.001D);
        assertEquals(0.001D, player.motionY, 0.001D);
        assertEquals(4.0D, player.motionZ, 0.001D);
    }

    @Test
    public void etherealFormCountsDownWithoutDeactivating() {
        AMEventHandler handler = new AMEventHandler();
        ExtendedProperties properties = new ExtendedProperties();
        properties.addToExtraVariables("ethereal", "10");

        handler.tickEtherealForm(mock(EntityPlayer.class), properties);

        assertEquals("9", properties.getExtraVariable("ethereal"));
    }

    @Test
    public void etherealFormDeactivatesAtTheEndOfItsDuration() {
        AMEventHandler handler = new AMEventHandler();
        ExtendedProperties properties = new ExtendedProperties();
        properties.addToExtraVariables("ethereal", "2");
        EntityPlayer player = mock(EntityPlayer.class);
        player.inventory = new InventoryPlayer(player);
        player.capabilities = new PlayerCapabilities();

        handler.tickEtherealForm(player, properties);

        assertFalse(properties.hasExtraVariable("ethereal"));
    }

    @Test
    public void movementBuffsLeaveMotionAloneWithoutPotions() {
        AMEventHandler handler = new AMEventHandler();
        ExtendedProperties properties = new ExtendedProperties();
        EntityLivingBase entity = mock(EntityLivingBase.class);
        entity.motionX = 2.0D;
        entity.motionY = 1.0D;
        entity.motionZ = 0.5D;

        handler.tickMovementBuffs(entity, properties);

        assertEquals(2.0D, entity.motionX, 0.001D);
        assertEquals(1.0D, entity.motionY, 0.001D);
        assertEquals(0.5D, entity.motionZ, 0.001D);
    }

    @Test
    public void unflippedStateDoesNotTouchTheWorldOrEntity() {
        AMEventHandler handler = new AMEventHandler();
        ExtendedProperties properties = new ExtendedProperties();
        World world = mock(World.class);
        EntityLivingBase entity = mock(EntityLivingBase.class);

        handler.tickFlippedState(entity, properties, world);

        verifyNoInteractions(world, entity);
    }

    @Test
    public void timedEffectsMethodNoOpsWithoutTimedKeys() {
        AMEventHandler handler = new AMEventHandler();
        ExtendedProperties properties = new ExtendedProperties();
        World serverWorld = mock(World.class);
        serverWorld.isRemote = false;
        EntityLivingBase entity = mock(EntityLivingBase.class);

        handler.tickTimedEffects(entity, properties, serverWorld);

        verifyNoInteractions(serverWorld, entity);
    }

    @Test
    public void timedEffectsMethodNoOpsOnTheClient() {
        AMEventHandler handler = new AMEventHandler();
        ExtendedProperties properties = new ExtendedProperties();
        properties.addToExtraVariables("shielded_tile_1_2_3_4", "10");
        World clientWorld = mock(World.class);
        clientWorld.isRemote = true;
        EntityLivingBase entity = mock(EntityLivingBase.class);

        handler.tickTimedEffects(entity, properties, clientWorld);

        verifyNoInteractions(clientWorld, entity);
    }

    @Test
    public void serverWorldsRunOnlyEnabledEffects() {
        World serverWorld = mock(World.class);
        serverWorld.isRemote = false;

        setFlags(true, false, false, false, false);
        assertTrue(AMEventHandler.shouldTickAcceleratedBlocks(serverWorld));
        assertTrue(AMEventHandler.shouldTickTimedEffects(serverWorld));

        setFlags(false, false, false, false, false);
        assertFalse(AMEventHandler.shouldTickAcceleratedBlocks(serverWorld));
        assertFalse(AMEventHandler.shouldTickTimedEffects(serverWorld));

        setFlags(false, true, false, false, false);
        assertFalse(AMEventHandler.shouldTickAcceleratedBlocks(serverWorld));
        assertTrue(AMEventHandler.shouldTickTimedEffects(serverWorld));

        setFlags(false, false, true, false, false);
        assertTrue(AMEventHandler.shouldTickTimedEffects(serverWorld));

        setFlags(false, false, false, true, false);
        assertTrue(AMEventHandler.shouldTickTimedEffects(serverWorld));

        setFlags(false, false, false, false, true);
        assertTrue(AMEventHandler.shouldTickTimedEffects(serverWorld));
    }

    private static void setFlags(boolean accelerate, boolean slow, boolean vortex, boolean fortify, boolean shield) {
        AMEventHandler.enabled_accelerate = accelerate;
        AMEventHandler.enabled_slow = slow;
        AMEventHandler.enable_spatialVortex = vortex;
        AMEventHandler.enabled_timeFortified = fortify;
        AMEventHandler.enabled_shield = shield;
    }
}
