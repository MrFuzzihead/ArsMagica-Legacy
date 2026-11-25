package am2;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.entity.player.PlayerCapabilities;
import net.minecraft.world.World;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import am2.buffs.ArsMagicaPotion;
import am2.buffs.BuffList;
import am2.playerextensions.ExtendedProperties;

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

    @Before
    public void stubPotionStatics() throws Exception {
        previousFlight = BuffList.flight;
        previousLevitation = BuffList.levitation;
        setPotion("flight", mock(ArsMagicaPotion.class));
        setPotion("levitation", mock(ArsMagicaPotion.class));
    }

    @After
    public void restorePotionStatics() throws Exception {
        setPotion("flight", previousFlight);
        setPotion("levitation", previousLevitation);
    }

    private static void setPotion(String fieldName, ArsMagicaPotion value) throws Exception {
        Field field = BuffList.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(null, value);
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
