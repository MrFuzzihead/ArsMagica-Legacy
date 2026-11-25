package am2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import net.minecraft.block.Block;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.item.EntityBoat;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityItemFrame;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.entity.monster.EntityEnderman;
import net.minecraft.entity.passive.EntityHorse;
import net.minecraft.entity.passive.EntityPig;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemPotion;
import net.minecraft.item.ItemSnowball;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.client.event.RenderBlockOverlayEvent;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.event.brewing.PotionBrewedEvent;
import net.minecraftforge.event.entity.EntityEvent.EntityConstructing;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.event.entity.living.EnderTeleportEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingEvent.LivingJumpEvent;
import net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.EntityInteractEvent;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.event.entity.player.PlayerDropsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.BreakSpeed;
import net.minecraftforge.event.world.BlockEvent;

import am2.affinity.AffinityHelper;
import am2.api.ArsMagicaApi;
import am2.api.events.ManaCostEvent;
import am2.api.power.IPowerNode;
import am2.api.power.PowerTypes;
import am2.api.spell.enums.Affinity;
import am2.api.spell.enums.BuffPowerLevel;
import am2.armor.ArmorHelper;
import am2.armor.infusions.GenericImbuement;
import am2.blocks.tileentities.TileEntityAstralBarrier;
import am2.blocks.tileentities.TileEntityInfusedStem;
import am2.bosses.BossSpawnHelper;
import am2.buffs.BuffEffectScrambleSynapses;
import am2.buffs.BuffEffectTemporalAnchor;
import am2.buffs.BuffList;
import am2.buffs.BuffStatModifiers;
import am2.damage.DamageSourceFire;
import am2.damage.DamageSources;
import am2.enchantments.AMEnchantments;
import am2.entities.EntityFlicker;
import am2.entities.EntityHallucination;
import am2.entities.EntitySpecificHallucinations;
import am2.items.ItemOre;
import am2.items.ItemSoulspike;
import am2.items.ItemsCommonProxy;
import am2.mixins.early.EntityInvoker;
import am2.network.AMNetHandler;
import am2.playerextensions.AffinityData;
import am2.playerextensions.ExtendedProperties;
import am2.playerextensions.RiftStorage;
import am2.playerextensions.SkillData;
import am2.playerextensions.effects.SpatialVortexEffect;
import am2.playerextensions.effects.TimedBlockEffect;
import am2.playerextensions.effects.TimedEffectCodec;
import am2.playerextensions.effects.TimedEntityEffect;
import am2.power.PowerNodeRegistry;
import am2.spell.SkillManager;
import am2.spell.SkillTreeManager;
import am2.utility.DimensionUtilities;
import am2.utility.EntityUtilities;
import am2.utility.InventoryUtilities;
import am2.utility.KeystoneUtilities;
import am2.utility.MathUtilities;
import am2.utility.TickrateController;
import cofh.api.energy.IEnergyHandler;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.network.FMLNetworkEvent;
import cpw.mods.fml.relauncher.Side;

public class AMEventHandler {

    static boolean enabled_accelerate = true;
    static boolean enabled_slow = true;
    static boolean enabled_timeFortified = true;
    static boolean enabled_shield = true;
    static boolean enable_spatialVortex = true;

    private static final Random rand = new Random();
    private static final Map<ExtendedProperties, Integer> tempFlipped = new HashMap<ExtendedProperties, Integer>();

    @SubscribeEvent
    @SuppressWarnings("deprecation")
    public void onPotionBrewed(PotionBrewedEvent brewEvent) {
        for (ItemStack stack : brewEvent.brewingStacks) {
            if (stack == null) continue;
            if (stack.getItem() instanceof ItemPotion) {
                ItemPotion ptn = ((ItemPotion) stack.getItem());
                List<PotionEffect> fx = ptn.getEffects(stack.getItemDamage());
                if (fx == null) return;
                for (PotionEffect pe : fx) {
                    if (pe.getPotionID() == BuffList.greaterManaPotion.id) {
                        stack = InventoryUtilities.replaceItem(stack, ItemsCommonProxy.greaterManaPotion);
                        break;
                    } else if (pe.getPotionID() == BuffList.epicManaPotion.id) {
                        stack = InventoryUtilities.replaceItem(stack, ItemsCommonProxy.epicManaPotion);
                        break;
                    } else if (pe.getPotionID() == BuffList.legendaryManaPotion.id) {
                        stack = InventoryUtilities.replaceItem(stack, ItemsCommonProxy.legendaryManaPotion);
                        break;
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public void onEndermanTeleport(EnderTeleportEvent event) {
        EntityLivingBase ent = event.entityLiving;

        ArrayList<Long> keystoneKeys = KeystoneUtilities.instance.GetKeysInInvenory(ent);
        TileEntityAstralBarrier blockingBarrier = DimensionUtilities.GetBlockingAstralBarrier(
            event.entityLiving.worldObj,
            (int) event.targetX,
            (int) event.targetY,
            (int) event.targetZ,
            keystoneKeys);

        if (ent.isPotionActive(BuffList.astralDistortion.id) || blockingBarrier != null) {
            event.setCanceled(true);
            if (blockingBarrier != null) {
                blockingBarrier.onEntityBlocked(ent);
            }
            return;
        }

        if (!ent.worldObj.isRemote && ent instanceof EntityEnderman && ent.worldObj.rand.nextDouble() < 0.01f) {
            EntityFlicker flicker = new EntityFlicker(ent.worldObj);
            flicker.setPosition(ent.posX, ent.posY, ent.posZ);
            flicker.setFlickerType(Affinity.ENDER);
            ent.worldObj.spawnEntityInWorld(flicker);
        }
    }

    @SubscribeEvent
    public void onEntityConstructing(EntityConstructing event) {

        if (event.entity instanceof EntityLivingBase) {
            EntityLivingBase entityLB = (EntityLivingBase) event.entity;
            entityLB.registerExtendedProperties(ExtendedProperties.identifier, new ExtendedProperties());
            entityLB.getAttributeMap()
                .registerAttribute(ArsMagicaApi.maxManaBonus);
            entityLB.getAttributeMap()
                .registerAttribute(ArsMagicaApi.maxBurnoutBonus);
            entityLB.getAttributeMap()
                .registerAttribute(ArsMagicaApi.xpGainModifier);
            entityLB.getAttributeMap()
                .registerAttribute(ArsMagicaApi.burnoutReductionRate);
            entityLB.getAttributeMap()
                .registerAttribute(ArsMagicaApi.manaRegenTimeModifier);
            ExtendedProperties.For(entityLB)
                .register(event.entity.worldObj, entityLB);
            if (event.entity instanceof EntityPlayer) {
                event.entity.registerExtendedProperties(RiftStorage.identifier, new RiftStorage());
                event.entity.registerExtendedProperties(AffinityData.identifier, new AffinityData());
                event.entity
                    .registerExtendedProperties(SkillData.identifier, new SkillData((EntityPlayer) event.entity));
            }
        } else if (event.entity instanceof EntityItemFrame) {
            AMCore.proxy.itemFrameWatcher.startWatchingFrame((EntityItemFrame) event.entity);
        }
    }

    @SubscribeEvent
    public void onclone(net.minecraftforge.event.entity.player.PlayerEvent.Clone event) {
        if (event.original == null || event.entityPlayer == null || event.entityPlayer instanceof FakePlayer) {
            return;
        }
        EntityPlayer original = event.original;
        EntityPlayer newplayer = event.entityPlayer;

        // ExtendedProp.
        NBTTagCompound OldNBT = new NBTTagCompound();
        ExtendedProperties.For(original)
            .saveNBTData(OldNBT);
        ExtendedProperties.For(newplayer)
            .loadNBTData(OldNBT);

        // skill data.
        NBTTagCompound OldSkill = new NBTTagCompound();
        SkillData.For(original)
            .saveNBTData(OldSkill);
        SkillData.For(newplayer)
            .loadNBTData(OldSkill);

        // Rift storage.
        NBTTagCompound OldRift = new NBTTagCompound();
        RiftStorage.For(original)
            .saveNBTData(OldRift);
        RiftStorage.For(newplayer)
            .loadNBTData(OldRift);

        // Affinity data.
        NBTTagCompound OldAffinity = new NBTTagCompound();
        AffinityData.For(original)
            .saveNBTData(OldAffinity);
        AffinityData.For(newplayer)
            .loadNBTData(OldAffinity);

    }

    // SoulBound Handler (code taken from @EnderIO)
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onPlayerDeath(PlayerDropsEvent evt) {
        if (evt.entityPlayer == null || evt.entityPlayer instanceof FakePlayer || evt.isCanceled()) {
            return;
        }
        if (evt.entityPlayer.worldObj.getGameRules()
            .getGameRuleBooleanValue("keepInventory")) {
            return;
        }
        ListIterator<EntityItem> iter = evt.drops.listIterator();
        while (iter.hasNext()) {
            EntityItem ei = iter.next();
            ItemStack item = ei.getEntityItem();
            if (isSoulBound(item)) {
                if (addToPlayerInventory(evt.entityPlayer, item)) {
                    iter.remove();
                }
            }
        }
    }

    /*
     * This is called when the user presses the "respawn" button. The original inventory would be empty, but
     * onPlayerDeath() above placed items in it. Note: Without other death-modifying mods, the content of the old
     * inventory would always fit into the new one (both being empty but for soulbound items in the old one) and the old
     * one would be discarded just after this method. But better play it safe and assume that an overflow is possible
     * and that another mod may move stuff out of the old inventory, too.
     */
    @SubscribeEvent
    public void onPlayerClone(net.minecraftforge.event.entity.player.PlayerEvent.Clone evt) {
        if (!evt.wasDeath || evt.isCanceled()) {
            return;
        }
        if (evt.original == null || evt.entityPlayer == null || evt.entityPlayer instanceof FakePlayer) {
            return;
        }
        if (evt.entityPlayer.worldObj.getGameRules()
            .getGameRuleBooleanValue("keepInventory")) {
            return;
        }
        for (int i = 0; i < evt.original.inventory.mainInventory.length; i++) {
            ItemStack item = evt.original.inventory.mainInventory[i];
            if (isSoulBound(item)) {
                if (addToPlayerInventory(evt.entityPlayer, item)) {
                    evt.original.inventory.mainInventory[i] = null;
                }
            }
        }
        for (int i = 0; i < evt.original.inventory.armorInventory.length; i++) {
            ItemStack item = evt.original.inventory.armorInventory[i];
            if (isSoulBound(item)) {
                if (addToPlayerInventory(evt.entityPlayer, item)) {
                    evt.original.inventory.armorInventory[i] = null;
                }
            }
        }
    }

    private boolean isSoulBound(ItemStack item) {
        return EnchantmentHelper.getEnchantmentLevel(AMEnchantments.soulbound.effectId, item) > 0;
    }

    private boolean addToPlayerInventory(EntityPlayer entityPlayer, ItemStack item) {
        if (item == null || entityPlayer == null) {
            return false;
        }
        if (item.getItem() instanceof ItemArmor) {
            ItemArmor arm = (ItemArmor) item.getItem();
            int index = 3 - arm.armorType;
            if (entityPlayer.inventory.armorItemInSlot(index) == null) {
                entityPlayer.inventory.armorInventory[index] = item;
                return true;
            }
        }
        InventoryPlayer inv = entityPlayer.inventory;
        for (int i = 0; i < inv.mainInventory.length; i++) {
            if (inv.mainInventory[i] == null) {
                inv.mainInventory[i] = item.copy();
                return true;
            }
        }
        return false;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onEntityDeathChrono(LivingDeathEvent event) {
        EntityLivingBase soonToBeDead = event.entityLiving;
        if (soonToBeDead.isPotionActive(BuffList.temporalAnchor.id)) {
            event.setCanceled(true);
            PotionEffect pe = soonToBeDead.getActivePotionEffect(BuffList.temporalAnchor);
            if (pe instanceof BuffEffectTemporalAnchor) {
                BuffEffectTemporalAnchor buff = (BuffEffectTemporalAnchor) pe;
                buff.stopEffect(soonToBeDead);
            }
            soonToBeDead.removePotionEffect(BuffList.temporalAnchor.id);
        }
    }

    // @SubscribeEvent(priority = EventPriority.HIGHEST)
    // public void onEntityDeathHighPriority(LivingDeathEvent event){
    //
    // EntityLivingBase soonToBeDead = event.entityLiving;
    //
    // if (soonToBeDead instanceof EntityPlayer) {
    // // soul fragments: die with at least 5 rare items
    // if (soonToBeDead.isPotionActive(BuffList.psychedelic)){
    // if (soonToBeDead.worldObj.provider.dimensionId == 1 &&
    // soonToBeDead.getActivePotionEffect(BuffList.psychedelic).getAmplifier() == 1) {
    // EntityPlayer player = (EntityPlayer)soonToBeDead;
    // int slotCount = 0;
    // int rareCount = 0;
    // for (ItemStack stack : player.inventory.mainInventory){
    // if (stack != null) {
    // if (stack.getRarity() != EnumRarity.common) {
    // player.inventory.setInventorySlotContents(slotCount, null);
    // rareCount++;
    // }
    // }
    // slotCount++;
    // }
    // slotCount = 0;
    // for (ItemStack stack : player.inventory.armorInventory){
    // if (stack != null) {
    // if (stack.getRarity() != EnumRarity.common) {
    // player.inventory.setInventorySlotContents(slotCount + player.inventory.mainInventory.length, null);
    // rareCount++;
    // }
    // }
    // slotCount++;
    // }
    // if (rareCount >= 5) {
    // EntityItem fragment = new EntityItem(player.worldObj);
    // ItemStack stack = new ItemStack(ItemsCommonProxy.itemOre, 1, ItemOre.META_SOULFRAGMENT);
    // fragment.setPosition(player.posX+rand.nextInt(5)-2, player.posY + 10, player.posZ+rand.nextInt(5)-2);
    // fragment.setEntityItemStack(stack);
    // player.worldObj.spawnEntityInWorld(fragment);
    // player.worldObj.playSoundAtEntity(player, "ambient.weather.thunder",2F, 2F);
    // }
    // }
    // }
    // }
    // }

    @SubscribeEvent
    public void onAttack(AttackEntityEvent event) {
        if (event.entityPlayer.getHeldItem() != null) {
            if (event.entityPlayer.getHeldItem()
                .getItem() instanceof ItemSoulspike) {
                if (event.target instanceof EntityHallucination) {
                    ItemSoulspike.addManaToSpike(event.entityPlayer.getHeldItem(), 15);
                    event.target.attackEntityFrom(DamageSource.outOfWorld, 15);
                    event.target.attackEntityFrom(DamageSource.magic, 15);
                } else if (event.target instanceof EntityLivingBase) {
                    if (event.target instanceof EntityPlayer) {
                        EntityPlayer pl = (EntityPlayer) event.target;
                        if (!AMCore.config.getAllowCreativeTargets() && pl.capabilities.isCreativeMode) return;
                        ExtendedProperties properties = ExtendedProperties.For(pl);
                        if (properties.hasExtraVariable("ethereal")) {
                            ItemSoulspike.addManaToSpike(
                                event.entityPlayer.getHeldItem(),
                                (int) (properties.getCurrentMana() / 4));
                            properties.deductMana(properties.getCurrentMana() / 4);
                        }
                    }
                    ItemSoulspike.addManaToSpike(event.entityPlayer.getHeldItem(), 3);
                    event.target.attackEntityFrom(DamageSource.outOfWorld, 3);
                }
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public void onEntityDeath(LivingDeathEvent event) {
        EntityLivingBase soonToBeDead = event.entityLiving;
        if (!(ExtendedProperties.For(soonToBeDead)
            .getContingencyEffect(1)
            .getItem() instanceof ItemSnowball)) {
            ExtendedProperties.For(soonToBeDead)
                .procContingency(1, null);
        }

        if (soonToBeDead instanceof EntityHallucination && event.source.getSourceOfDamage() != null) {
            if (event.source.getSourceOfDamage() instanceof EntityPlayer) { // bad karma with the world
                ExtendedProperties exProps = ExtendedProperties.For((EntityPlayer) event.source.getSourceOfDamage());
                exProps.addToExtraVariables("karma", "bad");
            }
        } else if (soonToBeDead instanceof EntityCreature) {
            if (!EntityUtilities.isSummon(soonToBeDead) && EntityUtilities.isAIEnabled((EntityCreature) soonToBeDead)
                && event.source.getSourceOfDamage() instanceof EntityPlayer) {
                EntityUtilities.handleCrystalPhialAdd(
                    (EntityCreature) soonToBeDead,
                    (EntityPlayer) event.source.getSourceOfDamage());
            }
        }

        if (EntityUtilities.isSummon(soonToBeDead)) {
            int ownerID = EntityUtilities.getOwner(soonToBeDead);
            Entity e = soonToBeDead.worldObj.getEntityByID(ownerID);
            if (e != null & e instanceof EntityLivingBase) {
                ExtendedProperties.For((EntityLivingBase) e)
                    .removeSummon();
            }
        }

        if (soonToBeDead instanceof EntityVillager) {
            BossSpawnHelper.instance.onVillagerChildKilled((EntityVillager) soonToBeDead);
        }

        if (soonToBeDead instanceof EntityLiving && !soonToBeDead.worldObj.isRemote) {
            int x = (int) Math.floor(soonToBeDead.posX);
            int y = (int) Math.floor(soonToBeDead.posY);
            int z = (int) Math.floor(soonToBeDead.posZ);
            for (int newx = x - 3; newx <= x + 3; newx++) {
                for (int newy = y - 3; newy <= y + 3; newy++) {
                    for (int newz = z - 3; newz <= z + 3; newz++) {
                        if (soonToBeDead.worldObj.getTileEntity(newx, newy, newz) instanceof TileEntityInfusedStem) {
                            ((TileEntityInfusedStem) soonToBeDead.worldObj
                                .getTileEntity(newx, newy, newz)).killedEntities.add((EntityLiving) soonToBeDead);
                            return;
                        }
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public void onLivingDrops(LivingDropsEvent event) {
        if (EntityUtilities.isSummon(event.entityLiving) && !(event.entityLiving instanceof EntityHorse)) {
            event.setCanceled(true);
        }
        if (event.source == DamageSources.darkNexus) {
            event.setCanceled(true);
        }
        if (!event.entityLiving.worldObj.isRemote && event.entityLiving instanceof EntityPig
            && event.entityLiving.getRNG()
                .nextDouble() < 0.3f) {
            EntityItem animalFat = new EntityItem(event.entityLiving.worldObj);
            ItemStack stack = new ItemStack(ItemsCommonProxy.itemOre, 1, ItemOre.META_ANIMALFAT);
            animalFat.setPosition(event.entity.posX, event.entity.posY, event.entity.posZ);
            animalFat.setEntityItemStack(stack);
            event.drops.add(animalFat);
        }
    }

    @SubscribeEvent
    public void onEntityJump(LivingJumpEvent event) {
        if (event.entityLiving.isPotionActive(BuffList.agility.id)) {
            event.entityLiving.motionY *= 1.5f;
        }
        if (event.entityLiving.isPotionActive(BuffList.leap.id)) {

            Entity velocityTarget = event.entityLiving;

            if (event.entityLiving.ridingEntity != null) {
                if (event.entityLiving.ridingEntity instanceof EntityMinecart) {
                    event.entityLiving.ridingEntity.setPosition(
                        event.entityLiving.ridingEntity.posX,
                        event.entityLiving.ridingEntity.posY + 1.5,
                        event.entityLiving.ridingEntity.posZ);
                }
                velocityTarget = event.entityLiving.ridingEntity;
            }

            double yVelocity = 0;
            double xVelocity = 0;
            double zVelocity = 0;

            Vec3 vec = event.entityLiving.getLookVec()
                .normalize();
            switch (event.entityLiving.getActivePotionEffect(BuffList.leap)
                .getAmplifier()) {
                case BuffPowerLevel.Low:
                    yVelocity = 0.4;
                    xVelocity = velocityTarget.motionX * 1.08 * Math.abs(vec.xCoord);
                    zVelocity = velocityTarget.motionZ * 1.08 * Math.abs(vec.zCoord);
                    break;
                case BuffPowerLevel.Medium:
                    yVelocity = 0.7;
                    xVelocity = velocityTarget.motionX * 1.25 * Math.abs(vec.xCoord);
                    zVelocity = velocityTarget.motionZ * 1.25 * Math.abs(vec.zCoord);
                    break;
                case BuffPowerLevel.High:
                    yVelocity = 1;
                    xVelocity = velocityTarget.motionX * 1.75 * Math.abs(vec.xCoord);
                    zVelocity = velocityTarget.motionZ * 1.75 * Math.abs(vec.zCoord);
                    break;
                default:
                    break;
            }

            float maxHorizontalVelocity = 1.45f;

            if (event.entityLiving.ridingEntity != null
                && (event.entityLiving.ridingEntity instanceof EntityMinecart
                    || event.entityLiving.ridingEntity instanceof EntityBoat)
                || event.entityLiving.isPotionActive(BuffList.haste.id)) {
                maxHorizontalVelocity += 25;
                xVelocity *= 2.5;
                zVelocity *= 2.5;
            }

            if (xVelocity > maxHorizontalVelocity) {
                xVelocity = maxHorizontalVelocity;
            } else if (xVelocity < -maxHorizontalVelocity) {
                xVelocity = -maxHorizontalVelocity;
            }

            if (zVelocity > maxHorizontalVelocity) {
                zVelocity = maxHorizontalVelocity;
            } else if (zVelocity < -maxHorizontalVelocity) {
                zVelocity = -maxHorizontalVelocity;
            }

            if (ExtendedProperties.For(event.entityLiving)
                .getIsFlipped()) {
                yVelocity *= -1;
            }

            velocityTarget.addVelocity(xVelocity, yVelocity, zVelocity);
        }
        if (event.entityLiving.isPotionActive(BuffList.entangled.id)) {
            event.entityLiving.motionY = 0;
        }

        if (event.entityLiving instanceof EntityPlayer) {
            ItemStack boots = ((EntityPlayer) event.entityLiving).inventory.armorInventory[0];
            if (boots != null && boots.getItem() == ItemsCommonProxy.enderBoots && event.entityLiving.isSneaking()) {
                ExtendedProperties.For(event.entityLiving)
                    .toggleFlipped();
            }
        }
        if (ExtendedProperties.For(event.entityLiving)
            .getFlipRotation() > 0) event.entityLiving.addVelocity(0, -2 * event.entityLiving.motionY, 0);

    }

    @SubscribeEvent
    public void onEntityFall(LivingFallEvent event) {

        EntityLivingBase ent = event.entityLiving;
        float f = event.distance;
        ent.isAirBorne = false;

        // slowfall buff
        if (ent.isPotionActive(BuffList.slowfall.id) || ent.isPotionActive(BuffList.shrink.id)
            || (ent instanceof EntityPlayer && AffinityData.For(ent)
                .getAffinityDepth(Affinity.NATURE) == 1.0f)) {
            event.setCanceled(true);
            ent.fallDistance = 0;
            return;
        }

        if (ent instanceof EntityPlayer && ((EntityPlayer) ent).inventory.armorInventory[0] != null) {
            if (((EntityPlayer) ent).inventory.armorInventory[0].getItem() == ItemsCommonProxy.archmageBoots) {
                event.setCanceled(true);
                return;
            }
        }

        // gravity well
        if (ent.isPotionActive(BuffList.gravityWell.id)) {
            ent.fallDistance *= 1.5f;
        }

        // fall protection stat
        f -= ExtendedProperties.For(ent)
            .getFallProtection();
        ExtendedProperties.For(ent)
            .setFallProtection(0);
        if (f <= 0) {
            ent.fallDistance = 0;
            event.setCanceled(true);
        }
    }

    public static java.util.Set<String> forceShielded = new java.util.HashSet<>();
    public static Map<UUID, Integer> acceleratedEntitiesUUIDs = new HashMap<>();
    public static Map<UUID, Integer> slowedEntitiesUUIDs = new HashMap<>();

    /** Null-safe tile lookup for the spatial-vortex bookkeeping; an unloaded dimension yields a null world. */
    static TileEntity getTileEntitySafe(World world, int x, int y, int z) {
        return world == null ? null : world.getTileEntity(x, y, z);
    }

    /** CoFH is compileOnly; short-circuit before resolving IEnergyHandler when the mod is absent. */
    static boolean isEnergyHandler(Object te) {
        return AMCore.cofh && te instanceof IEnergyHandler;
    }

    /** Releases a Fortify Time chunk ticket only when the world and tile entity still exist. */
    static void releaseFortifiedChunk(World world, int x, int y, int z) {
        if (world == null) {
            return;
        }
        TileEntity te = world.getTileEntity(x, y, z);
        if (te == null) {
            return;
        }
        AMChunkLoader.INSTANCE.releaseStaticChunkLoad(te.getClass(), x, y, z, world);
    }

    /**
     * Apply extra entity ticks once per server tick.
     *
     * <p>
     * This used to run from {@link LivingUpdateEvent}, which meant that every living entity caused a complete
     * scan of every loaded entity. Apart from doing the same work L times per tick, that made the cost quadratic in
     * the number of loaded living entities. UUIDs are used as map keys so the normal update path never has to
     * allocate the string representation of an entity UUID.
     * </p>
     */
    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !enabled_accelerate || acceleratedEntitiesUUIDs.isEmpty()) {
            return;
        }

        for (WorldServer worldServer : DimensionManager.getWorlds()) {
            // Entity#onUpdate can add or remove entities. Iterate over a snapshot just as the old code did, but do it
            // only once for the whole server tick instead of once for every LivingUpdateEvent.
            List<Entity> entityList = new ArrayList<>(worldServer.loadedEntityList);
            for (Entity entity : entityList) {
                if (!(entity instanceof EntityLivingBase) || entity.isDead) {
                    continue;
                }

                Integer extraTicks = acceleratedEntitiesUUIDs.get(entity.getUniqueID());
                if (extraTicks == null || extraTicks <= 0) {
                    continue;
                }

                for (int i = 0; i < extraTicks && !entity.isDead; i++) {
                    entity.onUpdate();
                }
            }
        }
    }

    private static UUID parseUUID(String value) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @SubscribeEvent
    public void onBlockBreak(BlockEvent.BreakEvent event) {
        if (forceShielded.contains(event.x + "_" + event.y + "_" + event.z + "_" + event.world.provider.dimensionId)) {
            event.setCanceled(true);
            event.world
                .playSoundAtEntity(event.getPlayer(), "arsmagica2:spell.cast.arcane", 1F, rand.nextFloat() + 0.5f);
            event.world.spawnParticle("depthsuspend", event.x + 0.5, event.y + 0.5, event.z + 0.5, 0, 0, 0);
            event.world.spawnParticle(
                "depthsuspend",
                event.x + rand.nextFloat(),
                event.y + rand.nextFloat(),
                event.z + rand.nextFloat(),
                0,
                0,
                0);
            event.world.spawnParticle(
                "depthsuspend",
                event.x + rand.nextFloat(),
                event.y + rand.nextFloat(),
                event.z + rand.nextFloat(),
                0,
                0,
                0);
        }
    }

    @SubscribeEvent
    public void disconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) { // fired at the client. this is
                                                                                       // to reset it to normal after
                                                                                       // disconnecting
        TickrateController.setServerTickrate(TickrateController.DEFAULT_TICKS_PER_SECOND);
        TickrateController.changeClientTickratePublic(null, TickrateController.DEFAULT_TICKS_PER_SECOND);
        AMCore.proxy.itemFrameWatcher.clear();
        clearAllTransientState();
    }

    @SubscribeEvent
    public void connect(FMLNetworkEvent.ClientConnectedToServerEvent event) { // fired at client when connects to server
        if (event.isLocal) { // single player game
            float tickrate = TickrateController.DEFAULT_TICKS_PER_SECOND;
            TickrateController.setServerTickrate(tickrate);
            TickrateController.changeClientTickratePublic(null, tickrate);
        } else {
            TickrateController.changeClientTickratePublic(null, TickrateController.DEFAULT_TICKS_PER_SECOND); // it
                                                                                                              // forces
                                                                                                              // it
                                                                                                              // serverside
                                                                                                              // anyway
        }
    }

    @SubscribeEvent
    public void connect(PlayerEvent.PlayerLoggedInEvent event) {
        if (FMLCommonHandler.instance()
            .getEffectiveSide() == Side.SERVER) {
            float tickrate = (float) 1000L / TickrateController.getServerTickrate(); // 1000/50 = 20 ticks, etc
            TickrateController.changeClientTickratePublic(event.player, tickrate);
        }
    }

    private static int tick = 0;

    private static int[] getMinIndex(int[] array) {
        int min = array[0];
        int indexForMin = 0;
        for (int i = 0; i < array.length; i++) {
            int score = array[i];
            if (min > score) {
                min = score;
                indexForMin = i;
            }
        }
        return new int[] { indexForMin, min };
    }

    private static int[] getMaxIndex(int[] array) {
        int max = array[0];
        int indexForMax = 0;
        for (int i = 0; i < array.length; i++) {
            int score = array[i];
            if (max < score) {
                max = score;
                indexForMax = i;
            }
        }
        return new int[] { indexForMax, max };
    }

    /**
     * Timed block/entity bookkeeping mutates server worlds and reads server-side dimensions. Clients only render the
     * synced result, so skip the parsing and world lookups entirely there.
     */
    static boolean shouldTickAcceleratedBlocks(World world) {
        return world != null && !world.isRemote && enabled_accelerate;
    }

    static boolean shouldTickTimedEffects(World world) {
        return world != null && !world.isRemote
            && (enabled_accelerate || enabled_slow || enable_spatialVortex || enabled_timeFortified || enabled_shield);
    }

    static final int MAX_ACCELERATED_TILE_KEYS = 256;

    private static final class BoundedKeyCache<V> extends LinkedHashMap<String, V> {

        private static final long serialVersionUID = 1L;

        private BoundedKeyCache() {
            super(64, 0.75F, true);
        }

        @Override
        protected boolean removeEldestEntry(Map.Entry<String, V> eldest) {
            return size() > MAX_ACCELERATED_TILE_KEYS;
        }
    }

    private static final BoundedKeyCache<TimedBlockEffect> acceleratedTileKeys = new BoundedKeyCache<TimedBlockEffect>();
    private static final BoundedKeyCache<TimedEntityEffect> acceleratedEntityKeys = new BoundedKeyCache<TimedEntityEffect>();
    private static final BoundedKeyCache<SpatialVortexEffect> spatialVortexKeys = new BoundedKeyCache<SpatialVortexEffect>();
    private static final BoundedKeyCache<TimedBlockEffect> timedBlockKeys = new BoundedKeyCache<TimedBlockEffect>();

    static TimedBlockEffect acceleratedTileKey(String raw) {
        TimedBlockEffect cached = acceleratedTileKeys.get(raw);
        if (cached != null) return cached;

        TimedBlockEffect parsed = TimedEffectCodec.parseAcceleratedTile(raw);
        if (parsed != null) {
            acceleratedTileKeys.put(raw, parsed);
        }
        return parsed;
    }

    static TimedEntityEffect acceleratedEntityKey(String raw) {
        TimedEntityEffect cached = acceleratedEntityKeys.get(raw);
        if (cached != null) return cached;

        TimedEntityEffect parsed = TimedEffectCodec.parseEntity(raw);
        if (parsed != null) {
            acceleratedEntityKeys.put(raw, parsed);
        }
        return parsed;
    }

    static SpatialVortexEffect spatialVortexKey(String raw) {
        SpatialVortexEffect cached = spatialVortexKeys.get(raw);
        if (cached != null) return cached;

        SpatialVortexEffect parsed = TimedEffectCodec.parseSpatialVortex(raw);
        if (parsed != null) {
            spatialVortexKeys.put(raw, parsed);
        }
        return parsed;
    }

    static TimedBlockEffect timedBlockKey(String raw) {
        TimedBlockEffect cached = timedBlockKeys.get(raw);
        if (cached != null) return cached;

        TimedBlockEffect parsed = TimedEffectCodec.parseTimedBlock(raw);
        if (parsed != null) {
            timedBlockKeys.put(raw, parsed);
        }
        return parsed;
    }

    static int acceleratedTileKeyCacheSize() {
        return acceleratedTileKeys.size();
    }

    static void clearAcceleratedTileKeyCache() {
        acceleratedTileKeys.clear();
    }

    static void clearParsedEffectKeyCaches() {
        acceleratedTileKeys.clear();
        acceleratedEntityKeys.clear();
        spatialVortexKeys.clear();
        timedBlockKeys.clear();
    }

    /** Runs one accelerated update for a block position, reusing the tile/block lookups for all requested ticks. */
    static void tickAcceleratedBlock(World world, int x, int y, int z) {
        TileEntity tile = world.getTileEntity(x, y, z);
        if (tile != null && tile.canUpdate()) {
            tile.updateEntity();
        }

        Block block = world.getBlock(x, y, z);
        if (block != null && block.getTickRandomly() && world.rand.nextInt(100) == 0) {
            block.updateTick(world, x, y, z, world.rand);
        }
    }

    void tickTimedEffects(EntityLivingBase ent, ExtendedProperties extendedProperties, World world) {
        if (extendedProperties.hasTimedEffects() && shouldTickAcceleratedBlocks(world)) {
            // accelerated blocks and entities are done outside the 5-tick performance optimisation to make them smooth
            for (Map.Entry<TimedBlockEffect, Integer> entry : new ArrayList<Map.Entry<TimedBlockEffect, Integer>>(
                extendedProperties.getAcceleratedTiles()
                    .entrySet())) {
                TimedBlockEffect key = entry.getKey();
                int remaining = entry.getValue();
                if (remaining < 3) {
                    extendedProperties.removeAcceleratedTile(key);
                } else {
                    extendedProperties.setAcceleratedTileDuration(key, remaining - 1);
                    World wrld = DimensionManager.getWorld(key.dimension);
                    if (wrld != null) {
                        for (int i = 0; i < key.power; i++) {
                            tickAcceleratedBlock(wrld, key.x, key.y, key.z);
                        }
                    }
                }
            }
            // Accelerated entities are processed once per server tick in onServerTick(), not from every
            // LivingUpdateEvent. Keeping the entity-list scan out of this hot path avoids the old L^2 behavior.
        }

        if (extendedProperties.hasTimedEffects() && shouldTickTimedEffects(world)) {
            tick++;
            if (tick > 100000) tick = 0;
            if ((tick % ((ent.worldObj.playerEntities.size() + 1) * 5)) == 0) { // only does behavior every 5 player
                                                                                // ticks to ease computing load
                // e.g. if there's 4 players, they tick like 1,2,3,4,1,2,3,4,1,2,3,4, etc (In theory, hopefully), and we
                // pick 25th (1st), 50th (2nd), 75th(3rd), etc. ticks.
                // there are some free ticks in-between, e.g. with 1 player it ticks every 10 ticks instead of 5. That
                // eases load further (but causes double the duration - not a big deal for these spells).

                if (enabled_accelerate) {
                    if (SkillTreeManager.instance.isSkillDisabled(SkillManager.instance.getSkill("ConcentrateTime"))) {
                        for (TimedBlockEffect effect : new ArrayList<TimedBlockEffect>(
                            extendedProperties.getAcceleratedTiles()
                                .keySet())) {
                            extendedProperties.removeAcceleratedTile(effect);
                        }
                        for (TimedEntityEffect effect : new ArrayList<TimedEntityEffect>(
                            extendedProperties.getAcceleratedEntities()
                                .keySet())) {
                            extendedProperties.removeAcceleratedEntity(effect);
                        }
                    }
                    for (Map.Entry<TimedEntityEffect, Integer> entry : new ArrayList<Map.Entry<TimedEntityEffect, Integer>>(
                        extendedProperties.getAcceleratedEntities()
                            .entrySet())) {
                        TimedEntityEffect key = entry.getKey();
                        int remaining = entry.getValue();
                        if (remaining < 3) {
                            extendedProperties.removeAcceleratedEntity(key);
                            acceleratedEntitiesUUIDs.remove(key.entityUUID);
                        } else {
                            extendedProperties.setAcceleratedEntityDuration(key, remaining - 5);
                            if (acceleratedEntitiesUUIDs.get(key.entityUUID) == null) {
                                acceleratedEntitiesUUIDs.put(key.entityUUID, Integer.valueOf(key.power));
                            }
                        }
                    }
                }
                if (enabled_slow) {
                    if (SkillTreeManager.instance.isSkillDisabled(SkillManager.instance.getSkill("DiluteTime"))) {
                        extendedProperties.removeExtraVariablesContaining("accelerated_slow_entity_");
                        extendedProperties.removeExtraVariablesContaining("accelerated_slow_tile_");
                    }
                    for (Map.Entry<TimedEntityEffect, Integer> entry : new ArrayList<Map.Entry<TimedEntityEffect, Integer>>(
                        extendedProperties.getSlowedEntities()
                            .entrySet())) {
                        TimedEntityEffect key = entry.getKey();
                        int remaining = entry.getValue();
                        if (remaining < 3) {
                            extendedProperties.removeSlowedEntity(key);
                            slowedEntitiesUUIDs.remove(key.entityUUID);
                        } else {
                            extendedProperties.setSlowedEntityDuration(key, remaining - 5);
                            if (slowedEntitiesUUIDs.get(key.entityUUID) == null) {
                                slowedEntitiesUUIDs.put(key.entityUUID, Integer.valueOf(key.power));
                            }
                        }
                    }

                    for (Map.Entry<TimedBlockEffect, Integer> entry : new ArrayList<Map.Entry<TimedBlockEffect, Integer>>(
                        extendedProperties.getSlowedTiles()
                            .entrySet())) {
                        TimedBlockEffect key = entry.getKey();
                        int remaining = entry.getValue();
                        if (remaining < 3) {
                            extendedProperties.removeSlowedTile(key);
                        } else {
                            extendedProperties.setSlowedTileDuration(key, remaining - 5);
                        }
                    }
                }

                if (enable_spatialVortex) {
                    Map<String, String> spatialVortices = extendedProperties
                        .getExtraVariablesContains("spatialvortex_");
                    for (Iterator<Map.Entry<String, String>> it = spatialVortices.entrySet()
                        .iterator(); it.hasNext();) {
                        Map.Entry<String, String> entry = it.next();
                        if (spatialVortexKey(entry.getKey()) == null) {
                            // Do not leave malformed legacy data in a live extra-property map.
                            extendedProperties.removeFromExtraVariables(entry.getKey());
                            it.remove();
                        }
                    }
                    if (!spatialVortices.isEmpty()) {
                        int[] totalenergy = new int[spatialVortices.size()];
                        int[] totalenergyExternallyLimited = new int[spatialVortices.size()];
                        int[] totaletheriumdark = new int[spatialVortices.size()];
                        int[] totaletheriumlight = new int[spatialVortices.size()];

                        int vIndex = 0;
                        for (Map.Entry<String, String> entry : spatialVortices.entrySet()) {
                            SpatialVortexEffect key = spatialVortexKey(entry.getKey());
                            if (key == null) continue;
                            int x = key.x;
                            int y = key.y;
                            int z = key.z;
                            World thisdim = DimensionManager.getWorld(key.dimension);
                            totalenergy[vIndex] = 0;
                            totaletheriumdark[vIndex] = 0;
                            totaletheriumlight[vIndex] = 0;
                            for (int xadd = -1; xadd <= 1; xadd += 2) {
                                for (int zadd = -1; zadd <= 1; zadd += 2) {
                                    TileEntity te = getTileEntitySafe(thisdim, x + xadd, y, z + zadd);
                                    if (te != null) {
                                        if (isEnergyHandler(te)) {
                                            totalenergy[vIndex] += ((IEnergyHandler) te)
                                                .getEnergyStored(ForgeDirection.UNKNOWN);
                                            totalenergyExternallyLimited[vIndex] += ((IEnergyHandler) te)
                                                .extractEnergy(ForgeDirection.UNKNOWN, 50000, true); // sim only
                                        }
                                        if (te instanceof IPowerNode) {
                                            totaletheriumdark[vIndex] += (int) PowerNodeRegistry.For(thisdim)
                                                .getPower((IPowerNode) te, PowerTypes.DARK);
                                            totaletheriumlight[vIndex] += (int) PowerNodeRegistry.For(thisdim)
                                                .getPower((IPowerNode) te, PowerTypes.LIGHT);
                                        }
                                    }
                                }
                            }
                            vIndex++;
                        }
                        if (AMCore.config.getDebugVortex()) {
                            System.out.println(Arrays.toString(totalenergy));
                            System.out.println(Arrays.toString(totalenergyExternallyLimited));
                            System.out.println(Arrays.toString(totaletheriumdark));
                            System.out.println(Arrays.toString(totaletheriumlight));
                        }
                        int[] maxE = getMaxIndex(totalenergy);
                        int[] maxD = getMaxIndex(totaletheriumdark);
                        int[] maxL = getMaxIndex(totaletheriumlight);
                        int[] minE = getMinIndex(totalenergy);
                        int[] minD = getMinIndex(totaletheriumdark);
                        int[] minL = getMinIndex(totaletheriumlight);

                        if (AMCore.config.getDebugVortex()) {
                            System.out.println("E" + Arrays.toString(minE));
                            System.out.println(Arrays.toString(maxE));
                            System.out.println("D" + Arrays.toString(minD));
                            System.out.println(Arrays.toString(maxD));
                            System.out.println("L" + Arrays.toString(minL));
                            System.out.println(Arrays.toString(maxL));
                        }

                        int halfDiffE = (maxE[1] - minE[1]) / 2;
                        int halfDiffD = (maxD[1] - minD[1]) / 2;
                        int halfDiffL = (maxL[1] - minL[1]) / 2;

                        if (AMCore.config.getDebugVortex()) {
                            System.out.println(halfDiffD + "," + halfDiffE + "," + halfDiffL + " half diff");
                        }

                        // total energy available to transfer, limited by: 50,000 per 5 ticks, half the diff between max
                        // and min, and external factors (such as device's throughput rate)
                        int toTransferE = Math.min(totalenergyExternallyLimited[maxE[0]], halfDiffE); // 50000 is the
                                                                                                      // max
                                                                                                      // externallyLimited
                                                                                                      // can be anyways
                        int toTransferD = Math.min(50000, halfDiffD);
                        int toTransferL = Math.min(50000, halfDiffL);

                        if (AMCore.config.getDebugVortex()) {
                            System.out.println(toTransferD + "," + toTransferE + "," + toTransferL + " to transfer");
                        }

                        int maximumEnergyMinimumVortexCanAccept = 0;
                        if (toTransferE > 0) {
                            // maximum energy we can *accept* right now (RF only)
                            int tIndex = 0;
                            for (Map.Entry<String, String> entry : spatialVortices.entrySet()) {
                                if (AMCore.config.getDebugVortex()) {
                                    System.out.println(minE[0]);
                                }
                                if (tIndex == minE[0]) {
                                    SpatialVortexEffect key = spatialVortexKey(entry.getKey());
                                    if (key == null) continue;
                                    int x = key.x;
                                    int y = key.y;
                                    int z = key.z;
                                    World thisdim = DimensionManager.getWorld(key.dimension);
                                    for (int xadd = -1; xadd <= 1; xadd += 2) {
                                        for (int zadd = -1; zadd <= 1; zadd += 2) {
                                            TileEntity te = getTileEntitySafe(thisdim, x + xadd, y, z + zadd);
                                            if (te != null) {
                                                if (isEnergyHandler(te)) {
                                                    if (AMCore.config.getDebugVortex()) {
                                                        System.out.println("got here!");
                                                    }
                                                    maximumEnergyMinimumVortexCanAccept += ((IEnergyHandler) te)
                                                        .receiveEnergy(ForgeDirection.UNKNOWN, 50000, true); // sim only
                                                }
                                            }
                                        }
                                    }
                                    break;
                                }
                                tIndex++;
                            }
                        }

                        if (AMCore.config.getDebugVortex()) {
                            System.out.println(maximumEnergyMinimumVortexCanAccept + " max can accept");
                        }

                        int toTransferEActual = Math.min(toTransferE, maximumEnergyMinimumVortexCanAccept);
                        boolean ELeft = toTransferEActual > 0;
                        boolean LLeft = toTransferL > 0;
                        boolean DLeft = toTransferD > 0;

                        int localIndex = 0;
                        // energy subtraction. BEWARE: Terrible code to follow!! Prepare bleach for eyes.
                        if (ELeft) {
                            int toSubtractE = toTransferEActual;
                            for (Map.Entry<String, String> entry : spatialVortices.entrySet()) {
                                if (localIndex == maxE[0]) {
                                    SpatialVortexEffect key = spatialVortexKey(entry.getKey());
                                    if (key == null) continue;
                                    int x = key.x;
                                    int y = key.y;
                                    int z = key.z;
                                    World thisdim = DimensionManager.getWorld(key.dimension);
                                    for (int xadd = -1; xadd <= 1; xadd += 2) {
                                        for (int zadd = -1; zadd <= 1; zadd += 2) {
                                            TileEntity te = getTileEntitySafe(thisdim, x + xadd, y, z + zadd);
                                            if (te != null) {
                                                if (isEnergyHandler(te)) {
                                                    if (toSubtractE > 0) toSubtractE -= ((IEnergyHandler) te)
                                                        .extractEnergy(ForgeDirection.UNKNOWN, toSubtractE, false); // real
                                                }
                                            }
                                        }
                                    }
                                }
                                localIndex++;
                            }
                            localIndex = 0;
                        }
                        if (DLeft) {
                            int toSubtractD = toTransferD;
                            for (Map.Entry<String, String> entry : spatialVortices.entrySet()) {
                                if (localIndex == maxD[0]) {
                                    SpatialVortexEffect key = spatialVortexKey(entry.getKey());
                                    if (key == null) continue;
                                    int x = key.x;
                                    int y = key.y;
                                    int z = key.z;
                                    World thisdim = DimensionManager.getWorld(key.dimension);
                                    for (int xadd = -1; xadd <= 1; xadd += 2) {
                                        for (int zadd = -1; zadd <= 1; zadd += 2) {
                                            TileEntity te = getTileEntitySafe(thisdim, x + xadd, y, z + zadd);
                                            if (te != null) {
                                                if (te instanceof IPowerNode) {
                                                    if (toSubtractD > 0) toSubtractD -= (int) PowerNodeRegistry
                                                        .For(thisdim)
                                                        .consumePower((IPowerNode) te, PowerTypes.DARK, toSubtractD); // real
                                                }
                                            }
                                        }
                                    }
                                }
                                localIndex++;
                            }
                            localIndex = 0;
                        }
                        if (LLeft) {
                            int toSubtractL = toTransferL;
                            for (Map.Entry<String, String> entry : spatialVortices.entrySet()) {
                                if (localIndex == maxL[0]) {
                                    SpatialVortexEffect key = spatialVortexKey(entry.getKey());
                                    if (key == null) continue;
                                    int x = key.x;
                                    int y = key.y;
                                    int z = key.z;
                                    World thisdim = DimensionManager.getWorld(key.dimension);
                                    for (int xadd = -1; xadd <= 1; xadd += 2) {
                                        for (int zadd = -1; zadd <= 1; zadd += 2) {
                                            TileEntity te = getTileEntitySafe(thisdim, x + xadd, y, z + zadd);
                                            if (te != null) {
                                                if (te instanceof IPowerNode) {
                                                    if (toSubtractL > 0) toSubtractL -= (int) PowerNodeRegistry
                                                        .For(thisdim)
                                                        .consumePower((IPowerNode) te, PowerTypes.LIGHT, toSubtractL); // real
                                                }
                                            }
                                        }
                                    }
                                }
                                localIndex++;
                            }
                        }

                        if (ELeft || DLeft || LLeft) {
                            // actually do the energy transfer. Finally. Note: I *know* this whole algo I came up with
                            // is badly optimized, and I welcome any PRs to optimize it.
                            int fIndex = 0;
                            for (Map.Entry<String, String> entry : spatialVortices.entrySet()) {
                                if (fIndex == minE[0] && ELeft) {
                                    SpatialVortexEffect key = spatialVortexKey(entry.getKey());
                                    if (key == null) continue;
                                    int x = key.x;
                                    int y = key.y;
                                    int z = key.z;
                                    World thisdim = DimensionManager.getWorld(key.dimension);
                                    for (int xadd = -1; xadd <= 1; xadd += 2) {
                                        for (int zadd = -1; zadd <= 1; zadd += 2) {
                                            TileEntity te = getTileEntitySafe(thisdim, x + xadd, y, z + zadd);
                                            if (te != null) {
                                                if (isEnergyHandler(te)) {
                                                    if (toTransferEActual > 0)
                                                        toTransferEActual -= ((IEnergyHandler) te).receiveEnergy(
                                                            ForgeDirection.UNKNOWN,
                                                            toTransferEActual,
                                                            false); // real
                                                    else ELeft = false;
                                                }
                                            }
                                        }
                                    }
                                }
                                if (fIndex == minD[0] && DLeft) {
                                    if (AMCore.config.getDebugVortex()) {
                                        System.out.println(fIndex + " fIndex when minD");
                                    }
                                    SpatialVortexEffect key = spatialVortexKey(entry.getKey());
                                    if (key == null) continue;
                                    int x = key.x;
                                    int y = key.y;
                                    int z = key.z;
                                    World thisdim = DimensionManager.getWorld(key.dimension);
                                    for (int xadd = -1; xadd <= 1; xadd += 2) {
                                        for (int zadd = -1; zadd <= 1; zadd += 2) {
                                            TileEntity te = getTileEntitySafe(thisdim, x + xadd, y, z + zadd);
                                            if (te != null) {
                                                if (te instanceof IPowerNode) {
                                                    if (toTransferD > 0)
                                                        toTransferD -= (int) PowerNodeRegistry.For(thisdim)
                                                            .insertPower((IPowerNode) te, PowerTypes.DARK, toTransferD);
                                                    else DLeft = false;
                                                }
                                            }
                                        }
                                    }
                                }
                                if (fIndex == minL[0] && LLeft) {
                                    if (AMCore.config.getDebugVortex()) {
                                        System.out.println(fIndex + " fIndex when minL");
                                    }
                                    SpatialVortexEffect key = spatialVortexKey(entry.getKey());
                                    if (key == null) continue;
                                    int x = key.x;
                                    int y = key.y;
                                    int z = key.z;
                                    World thisdim = DimensionManager.getWorld(key.dimension);
                                    for (int xadd = -1; xadd <= 1; xadd += 2) {
                                        for (int zadd = -1; zadd <= 1; zadd += 2) {
                                            TileEntity te = getTileEntitySafe(thisdim, x + xadd, y, z + zadd);
                                            if (te != null) {
                                                if (te instanceof IPowerNode) {
                                                    if (toTransferL > 0) toTransferL -= (int) PowerNodeRegistry
                                                        .For(thisdim)
                                                        .insertPower((IPowerNode) te, PowerTypes.LIGHT, toTransferL);
                                                    else LLeft = false;
                                                }
                                            }
                                        }
                                    }
                                }
                                fIndex++;
                            }
                        }
                    }
                }

                if (enabled_timeFortified) {
                    Map<String, String> loadedBlocks = extendedProperties
                        .getExtraVariablesContains("timefortified_tile_");

                    for (Map.Entry<String, String> entry : loadedBlocks.entrySet()) {
                        int remaining = Integer.parseInt(entry.getValue());
                        if (remaining < 3) {
                            extendedProperties.removeFromExtraVariables(entry.getKey());
                            if (!world.isRemote) {
                                TimedBlockEffect key = timedBlockKey(entry.getKey());
                                if (key != null) {
                                    releaseFortifiedChunk(
                                        DimensionManager.getWorld(key.dimension),
                                        key.x,
                                        key.y,
                                        key.z);
                                }
                            }
                        } else {
                            extendedProperties.addToExtraVariables(entry.getKey(), String.valueOf(remaining - 5));
                        }
                    }
                }
                if (enabled_shield) {
                    Map<String, String> shieldedBlocks = extendedProperties.getExtraVariablesContains("shielded_tile_");

                    for (Map.Entry<String, String> entry : shieldedBlocks.entrySet()) {
                        TimedBlockEffect key = timedBlockKey(entry.getKey());
                        if (key == null) {
                            // Do not leave malformed legacy data in a live extra-property map.
                            extendedProperties.removeFromExtraVariables(entry.getKey());
                            continue;
                        }
                        int remaining = Integer.parseInt(entry.getValue());
                        if (remaining < 3) {
                            extendedProperties.removeFromExtraVariables(entry.getKey());
                            if (!world.isRemote) {
                                forceShielded.remove(key.representation());
                            }
                        } else {
                            String represent = key.representation();
                            if (!(forceShielded.contains(represent))) forceShielded.add(represent);
                            extendedProperties.addToExtraVariables(entry.getKey(), String.valueOf(remaining - 5));
                        }
                    }
                }
            }
        }

    }

    void tickFlippedState(EntityLivingBase ent, ExtendedProperties extendedProperties, World world) {
        // unflip flipped players
        if (tempFlipped.containsKey(extendedProperties)) {
            tempFlipped.put(extendedProperties, tempFlipped.get(extendedProperties) - 1);
            if (tempFlipped.get(extendedProperties) <= 0) {
                tempFlipped.remove(extendedProperties);
                extendedProperties.toggleFlipped();
            }
        }

        extendedProperties.flipTick();

        if (extendedProperties.getIsFlipped()) {
            if ((ent).motionY < 2) (ent).motionY += 0.15f;

            double posY = ent.posY + ent.height;
            if (!world.isRemote) posY += ent.getEyeHeight();
            if (world.rayTraceBlocks(
                Vec3.createVectorHelper(ent.posX, posY, ent.posZ),
                Vec3.createVectorHelper(ent.posX, posY + 1, ent.posZ),
                true) != null) {
                if (!ent.onGround) {
                    if (ent.fallDistance > 0) {
                        ((EntityInvoker) ent).am2$invokeFall(ent.fallDistance);
                        ent.fallDistance = 0;
                    }
                }
                ent.onGround = true;
            } else {
                if (ent instanceof EntityPlayer) {
                    if (ent.motionY > 0) {
                        if (world.isRemote) ent.fallDistance += (float) (ent.posY - ent.prevPosY);
                        else ent.fallDistance += (float) ((((EntityPlayer) ent).field_71095_bQ
                            - ((EntityPlayer) ent).field_71096_bN) * 2);
                    }
                }
                ent.onGround = false;
            }
        }

    }

    void tickMovementBuffs(EntityLivingBase ent, ExtendedProperties extendedProperties) {
        // leap buff
        if (ent.isPotionActive(BuffList.leap)) {
            int amplifier = ent.getActivePotionEffect(BuffList.leap)
                .getAmplifier();

            switch (amplifier) {
                case BuffPowerLevel.Low:
                    extendedProperties.setFallProtection(8);
                    break;
                case BuffPowerLevel.Medium:
                    extendedProperties.setFallProtection(20);
                    break;
                case BuffPowerLevel.High:
                    extendedProperties.setFallProtection(45);
                    break;
                default:
                    break;
            }
        }

        if (ent.isPotionActive(BuffList.gravityWell)) {
            if (ent.motionY < 0 && ent.motionY > -3f) {
                ent.motionY *= 1.59999999999999998D;
            }
        }

        // slowfall/shrink buff
        // (isSneaking calls DataWatcher which are slow, so we test it late)
        if (ent.isPotionActive(BuffList.slowfall) || ent.isPotionActive(BuffList.shrink)
            || (ent instanceof EntityPlayer && AffinityData.For(ent)
                .getAffinityDepth(Affinity.NATURE) == 1.0f && !ent.isSneaking())) {
            if (!ent.onGround && ent.motionY < 0.0D) {
                ent.motionY *= 0.79999999999999998D;
            }
        }

        // swift swim
        if (ent.isPotionActive(BuffList.swiftSwim)) {
            if (ent.isInWater()) {
                if (!(ent instanceof EntityPlayer) || !((EntityPlayer) ent).capabilities.isFlying) {
                    ent.motionX *= (1.133f + 0.03 * ent.getActivePotionEffect(BuffList.swiftSwim)
                        .getAmplifier());
                    ent.motionZ *= (1.133f + 0.03 * ent.getActivePotionEffect(BuffList.swiftSwim)
                        .getAmplifier());

                    if (ent.motionY > 0) {
                        ent.motionY *= 1.134;
                    }
                }
            }
        }

        // watery grave
        if (ent.isPotionActive(BuffList.wateryGrave)) {
            if (ent.isInWater()) {
                double pullVel = -0.5f;
                pullVel *= (ent.getActivePotionEffect(BuffList.wateryGrave)
                    .getAmplifier() + 1);
                if (ent.motionY > pullVel) ent.motionY -= 0.1;
            }
        }

    }

    void tickEtherealForm(EntityPlayer player, ExtendedProperties extendedProperties) {
        int durationLeft = Integer.parseInt(extendedProperties.getExtraVariable("ethereal"));
        if (durationLeft < 3) {
            // deactivate form
            ItemSoulspike.removeTagFromBoots(player.inventory.armorInventory[0]);
            player.capabilities.disableDamage = false;
            player.capabilities.allowEdit = true;
            player.capabilities.isFlying = false;
            player.noClip = false;
            player.setInvisible(false);
            extendedProperties.removeFromExtraVariables("ethereal");
        } else {
            // tick ethereal form
            // decrease duration
            extendedProperties.addToExtraVariables("ethereal", String.valueOf(durationLeft - 1));
        }
    }

    void tickPlayerFlight(EntityPlayer player, ExtendedProperties extendedProperties) {
        if (player.isPotionActive(BuffList.flight.id) || player.isPotionActive(BuffList.levitation.id)
            || player.capabilities.isCreativeMode) {
            extendedProperties.hadFlight = true;
            if (player.isPotionActive(BuffList.levitation)) {
                if (player.capabilities.isFlying) {
                    float factor = 0.4f;
                    player.motionX *= factor;
                    player.motionZ *= factor;
                    player.motionY *= 0.0001f;
                }
            }
        } else if (extendedProperties.hadFlight) {
            player.capabilities.allowFlying = false;
            player.capabilities.isFlying = false;
            extendedProperties.hadFlight = false;
        }
    }

    void tickContingenciesAndAura(EntityLivingBase ent, ExtendedProperties extendedProperties, World world) {
        if (!ent.onGround && ent.fallDistance >= 4f
            && !(extendedProperties.getContingencyEffect(2)
                .getItem() instanceof ItemSnowball)) {
            int distanceToGround = MathUtilities.getDistanceToGround(ent, world);
            if (distanceToGround < -8 * ent.motionY) {
                extendedProperties.procContingency(2, null);
            }
        }
        if (!(extendedProperties.getContingencyEffect(3)
            .getItem() instanceof ItemSnowball) && ent.isBurning()) {
            extendedProperties.procContingency(3, null);
        }

        if (!ent.worldObj.isRemote && ent.ticksExisted % 200 == 0) {
            extendedProperties.setSyncAuras();
        }

    }

    void tickManaLinksAndLocalMovement(EntityLivingBase ent, ExtendedProperties extendedProperties, World world) {
        // mana link pfx
        if (ent.worldObj.isRemote) extendedProperties.spawnManaLinkParticles();

        if (ent.ticksExisted % 20 == 0) extendedProperties.cleanupManaLinks();

        if (world.isRemote) {
            AMCore.proxy.sendLocalMovementData(ent);
        }
    }

    void tickHallucinations(EntityPlayer player) {
        if (player.worldObj.isRemote) return;
        // hallucination effects
        if (player.isPotionActive(BuffList.psychedelic)) {
            if (player.worldObj.provider.dimensionId == -1) { // any amplifier can cause lower level hallucinations
                if (player.getActivePotionEffect(BuffList.psychedelic)
                    .getDuration() > 10) lowerHallucinationTick(player);
                else cleanupLowerHallucination(player);
            } else if (player.worldObj.provider.dimensionId == 1 && player.getActivePotionEffect(BuffList.psychedelic)
                .getAmplifier() == 1) { // only amplified for higher
                    if (player.getActivePotionEffect(BuffList.psychedelic)
                        .getDuration() > 10) higherHallucinationTick(player);
                    else cleanupHigherHallucination(player);
                }
        }
    }

    boolean shouldCancelSlowedEntity(EntityLivingBase ent) {
        if (slowedEntitiesUUIDs.isEmpty()) return false;

        Integer updateInterval = slowedEntitiesUUIDs.get(ent.getUniqueID());
        return updateInterval != null
            && !SkillTreeManager.instance.isSkillDisabled(SkillManager.instance.getSkill("DiluteTime"))
            && updateInterval > 0
            && ent.ticksExisted % updateInterval != 0;
    }

    void tickPlayerArmor(EntityPlayer player, ExtendedProperties extendedProperties, World world) {
        if (player.worldObj.isRemote) {
            int divisor = extendedProperties.getAuraDelay() > 0 ? extendedProperties.getAuraDelay() : 1;
            if (player.ticksExisted % divisor == 0) AMCore.proxy.particleManager.spawnAuraParticles(player);
            AMCore.proxy.setViewSettings();
        }

        ArmorHelper.HandleArmorInfusion(player);
        ArmorHelper.HandleArmorEffects(player, world);

        if (ArmorHelper.isInfusionPreset(player.getCurrentArmor(1), GenericImbuement.stepAssist)) {
            player.stepHeight = 1.0111f;
        } else if (player.stepHeight == 1.0111f) {
            player.stepHeight = 0.5f;
        }

        IAttributeInstance attr = player.getEntityAttribute(SharedMonsterAttributes.movementSpeed);
        if (ArmorHelper.isInfusionPreset(player.getCurrentArmor(0), GenericImbuement.runSpeed)) {
            if (attr.getModifier(GenericImbuement.imbuedHasteID) == null) {
                attr.applyModifier(GenericImbuement.imbuedHaste);
            }
        } else {
            if (attr.getModifier(GenericImbuement.imbuedHasteID) != null) {
                attr.removeModifier(GenericImbuement.imbuedHaste);
            }
        }
    }

    void tickSummon(EntityLivingBase ent) {
        if (!ent.worldObj.isRemote && EntityUtilities.isSummon(ent) && !EntityUtilities.isTileSpawnedAndValid(ent)) {
            int owner = EntityUtilities.getOwner(ent);
            Entity ownerEnt = ent.worldObj.getEntityByID(owner);
            if (!EntityUtilities.decrementSummonDuration(ent)) {
                ent.attackEntityFrom(DamageSources.unsummon, 5000);
            }
            if (owner == -1 || ownerEnt == null || ownerEnt.isDead || ownerEnt.getDistanceSqToEntity(ent) > 900) {
                if (ent instanceof EntityLiving && !((EntityLiving) ent).getCustomNameTag()
                    .isEmpty()) {
                    EntityUtilities.setOwner(ent, null);
                    EntityUtilities.setSummonDuration(ent, -1);
                    EntityUtilities.revertAI((EntityCreature) ent);
                } else {
                    ent.attackEntityFrom(DamageSources.unsummon, 5000);
                }
            }
        }

    }

    void tickPropertySync(EntityLivingBase ent, ExtendedProperties extendedProperties) {
        // data sync
        extendedProperties.handleExtendedPropertySync();

        if (ent instanceof EntityPlayer) {
            AffinityData.For(ent)
                .handleExtendedPropertySync();
            SkillData.For((EntityPlayer) ent)
                .handleExtendedPropertySync();
        }
    }

    ExtendedProperties prepareEntityProperties(EntityLivingBase ent) {
        BuffStatModifiers.instance.applyStatModifiersBasedOnBuffs(ent);

        ExtendedProperties extendedProperties = ExtendedProperties.For(ent);
        extendedProperties.handleSpecialSyncData();
        extendedProperties.manaBurnoutTick();
        return extendedProperties;
    }

    void tickAgility(EntityLivingBase ent) {
        if (ent.isPotionActive(BuffList.agility.id)) {
            ent.stepHeight = 1.01f;
        } else if (ent.stepHeight == 1.01f) {
            ent.stepHeight = 0.5f;
        }
    }

    @SubscribeEvent
    public void onEntityLiving(LivingUpdateEvent event) {

        EntityLivingBase ent = event.entityLiving;

        if (shouldCancelSlowedEntity(ent)) {
            event.setCanceled(true);
            return;
        }

        World world = ent.worldObj;

        ExtendedProperties extendedProperties = prepareEntityProperties(ent);

        if (ent instanceof EntityPlayer && extendedProperties.hasExtraVariable("ethereal")) {
            tickEtherealForm((EntityPlayer) ent, extendedProperties);
        }

        tickTimedEffects(ent, extendedProperties, world);

        // ================================================================================

        tickFlippedState(ent, extendedProperties, world);

        if (ent instanceof EntityPlayer) {
            tickPlayerArmor((EntityPlayer) ent, extendedProperties, world);
        }

        tickContingenciesAndAura(ent, extendedProperties, world);

        // buff particles
        // if (ent.worldObj.isRemote)
        // AMCore.instance.proxy.particleManager.spawnBuffParticles(ent);

        tickPropertySync(ent, extendedProperties);

        if (ent instanceof EntityPlayer) {
            tickPlayerFlight((EntityPlayer) ent, extendedProperties);
        }

        tickAgility(ent);

        tickSummon(ent);

        tickMovementBuffs(ent, extendedProperties);

        if (ent instanceof EntityPlayer) {
            tickHallucinations((EntityPlayer) ent);
        }

        tickManaLinksAndLocalMovement(ent, extendedProperties, world);
    }

    public static Map<EntityPlayer, ArrayList<EntityCreature>> hallucinationMap = new HashMap<EntityPlayer, ArrayList<EntityCreature>>();
    public static Map<EntityPlayer, ArrayList<EntityItem>> dustMap = new HashMap<EntityPlayer, ArrayList<EntityItem>>();
    public static Map<EntityCreature, Integer> tempCurseMap = new HashMap<EntityCreature, Integer>();

    /** Drops world-bound transient state when a dimension unloads. */
    public static void clearWorldState(World world) {
        hallucinationMap.entrySet()
            .removeIf(entry -> entry.getKey() == null || entry.getKey().worldObj == world);
        dustMap.entrySet()
            .removeIf(entry -> entry.getKey() == null || entry.getKey().worldObj == world);
        tempCurseMap.entrySet()
            .removeIf(entry -> entry.getKey() == null || entry.getKey().worldObj == world);
        forceShielded.removeIf(key -> key.endsWith("_" + world.provider.dimensionId));
    }

    /** Clears all transient state on server stop or client disconnect. */
    public static void clearAllTransientState() {
        hallucinationMap.clear();
        dustMap.clear();
        tempCurseMap.clear();
        forceShielded.clear();
        acceleratedEntitiesUUIDs.clear();
        slowedEntitiesUUIDs.clear();
        tempFlipped.clear();
    }

    private void cleanupLowerHallucination(EntityPlayer player) {
        player.removePotionEffect(BuffList.scrambleSynapses.id);
        player.extinguish();
        player.removePotionEffect(Potion.confusion.id);
        if (hallucinationMap.get(player) != null) {
            for (EntityCreature creature : hallucinationMap.get(player)) {
                if (creature != null) creature.setDead();
            }
            hallucinationMap.get(player)
                .clear();
        }
        if (dustMap.get(player) != null) {
            for (EntityItem item : dustMap.get(player)) {
                if (item != null) item.setDead();
            }
            dustMap.get(player)
                .clear();
        }
    }

    private void cleanupHigherHallucination(EntityPlayer player) {
        cleanupLowerHallucination(player);
        player.curePotionEffects(new ItemStack(Items.milk_bucket));
    }

    private void lowerHallucinationTick(EntityPlayer player) {
        if (player.worldObj.rand.nextInt(103) == 0) { // scramble
            player.addPotionEffect(new BuffEffectScrambleSynapses(50, 0));
        }
        if (player.worldObj.rand.nextInt(75) == 0) { // spontaneous combustion
            player.setFire(7);
        }
        if (player.worldObj.rand.nextInt(50) == 0) { // nausea
            player.addPotionEffect(new PotionEffect(Potion.confusion.id, 100, 0));
        }

        int x = MathHelper.floor_double(player.posX); // creatures
        int y = MathHelper.floor_double(player.posY);
        int z = MathHelper.floor_double(player.posZ);
        if (player.worldObj.rand.nextInt(550) == 0) {
            Class<? extends EntityHallucination> halclass = null;
            switch (player.worldObj.rand.nextInt(5)) {
                case 1:
                    halclass = EntitySpecificHallucinations.EntityHallucinationZombie.class;
                    break;
                case 2:
                    halclass = EntitySpecificHallucinations.EntityHallucinationSpider.class;
                    break;
                case 3:
                    halclass = EntitySpecificHallucinations.EntityHallucinationWitherSkeleton.class;
                    break;
                case 4:
                    halclass = EntitySpecificHallucinations.EntityHallucinationMagmacube.class;
                    break;
                default:
                    halclass = EntitySpecificHallucinations.EntityHallucinationCreeper.class;
                    break;
            }

            EntityCreature summoned = summonCreature(player.worldObj, halclass, x, y, z, player, 4, 9);
            hallucinationMap.computeIfAbsent(player, k -> new ArrayList<>());
            hallucinationMap.get(player)
                .add(summoned);
        }
        if (player.worldObj.rand.nextInt(100) == 0) { // dust
            EntityItem item = new EntityItem(player.worldObj);
            item.setPosition(
                (x - 15) + player.worldObj.rand.nextInt(16),
                y + player.worldObj.rand.nextInt(6),
                (z - 15) + player.worldObj.rand.nextInt(16));
            item.setEntityItemStack(new ItemStack(ItemsCommonProxy.itemOre, 1, ItemOre.META_COGNITIVEDUST));
            player.worldObj.spawnEntityInWorld(item);
            dustMap.computeIfAbsent(player, k -> new ArrayList<EntityItem>());
            dustMap.get(player)
                .add(item);
        }
    }

    private void higherHallucinationTick(EntityPlayer player) {
        lowerHallucinationTick(player); // all the effects of the lower... and more!
        if (player.worldObj.rand.nextInt(75) == 0) { // random effects
            player.addPotionEffect(new PotionEffect(player.worldObj.rand.nextInt(20) + 1, 100, 0));
        }
        int x = MathHelper.floor_double(player.posX);
        int y = MathHelper.floor_double(player.posY);
        int z = MathHelper.floor_double(player.posZ);
        if (player.worldObj.rand.nextInt(80) == 0) { // random noises
            String soundString = null;
            switch (player.worldObj.rand.nextInt(8)) {
                case 1:
                    soundString = "mob.skeleton.say";
                    break;
                case 2:
                    soundString = "game.tnt.primed";
                    break;
                case 3:
                    soundString = "random.bow";
                    break;
                case 4:
                    soundString = "random.chestopen";
                    break;
                case 5:
                    soundString = "random.chestclosed";
                    break;
                case 6:
                    soundString = "random.door_open";
                    break;
                case 7:
                    soundString = "random.fuse";
                    break;
                default:
                    soundString = "mob.zombie.say";
                    break;
            }
            player.worldObj.playSoundAtEntity(player, soundString, 1F, 1F);
        }
        if (player.worldObj.rand.nextInt(150) == 0) { // random floating explosion things
            int explosionX = (x - 30) + player.worldObj.rand.nextInt(31);
            int explosionY = y + player.worldObj.rand.nextInt(25);
            int explosionZ = (z - 30) + player.worldObj.rand.nextInt(31);
            player.worldObj.spawnParticle("largeexplode", explosionX, explosionY, explosionZ, 1.0D, 0.0D, 0.0D);
            player.worldObj.spawnParticle("hugeexplosion", explosionX, explosionY, explosionZ, 0.0D, 0.0D, 0.0D);
            player.worldObj.playSoundEffect(
                explosionX,
                explosionY,
                explosionZ,
                "random.explode",
                4.0F,
                (1.0F + (rand.nextFloat() - rand.nextFloat()) * 0.2F) * 0.7F);
            player.attackEntityFrom(DamageSource.magic, 1 + player.worldObj.rand.nextInt(10));
        }
        if (player.worldObj.rand.nextInt(50) == 0) { // random teleporations and snaps
            int cx = 0, cy = 0, cz = 0;
            if (player.worldObj.rand.nextBoolean()) { // teleport
                cx = rand.nextInt(50);
                cy = rand.nextInt(7);
                cz = rand.nextInt(50);
                if (!player.worldObj.isAirBlock((int) player.posX + cx, (int) player.posY + cy, (int) player.posZ + cz)
                    || !player.worldObj
                        .isAirBlock((int) player.posX + cx, (int) player.posY + cy - 1, (int) player.posZ + cz)) {
                    cx = 0;
                    cy = 0;
                    cz = 0;
                }
            }
            player.setPositionAndRotation(
                player.posX + cx,
                player.posY + cy,
                player.posZ + cz,
                player.rotationYaw * (rand.nextFloat() + rand.nextFloat()),
                player.rotationPitch * (rand.nextFloat() + rand.nextFloat()));
        }
        if (player.worldObj.rand.nextInt(750) == 0) { // even more hallucinations + ones specific to end
            Class<? extends EntityHallucination> halclass = null;
            switch (player.worldObj.rand.nextInt(8)) {
                case 1:
                    halclass = EntitySpecificHallucinations.EntityHallucinationZombie.class;
                    break;
                case 2:
                    halclass = EntitySpecificHallucinations.EntityHallucinationSpider.class;
                    break;
                case 3:
                    halclass = EntitySpecificHallucinations.EntityHallucinationWitherSkeleton.class;
                    break;
                case 4:
                    halclass = EntitySpecificHallucinations.EntityHallucinationEnderman.class;
                    break;
                case 5:
                case 6:
                case 7:
                    halclass = EntitySpecificHallucinations.EntityHallucinationEndermite.class;
                    break;
                default:
                    halclass = EntitySpecificHallucinations.EntityHallucinationCreeper.class;
                    break;
            }

            EntityCreature summoned = summonCreature(player.worldObj, halclass, x, y, z, player, 5, 13);
            hallucinationMap.computeIfAbsent(player, k -> new ArrayList<>());
            hallucinationMap.get(player)
                .add(summoned);
        }
    }

    public static EntityCreature summonCreature(World world, Class<? extends EntityHallucination> creatureClass, int x,
        int y, int z, EntityLivingBase target, int minRange, int maxRange) {
        if (!world.isRemote) {
            int activeRadius = maxRange - minRange;
            int ax = world.rand.nextInt(activeRadius * 2 + 1);
            if (ax > activeRadius) {
                ax += minRange * 2;
            }
            int nx = x - maxRange + ax;
            int az = world.rand.nextInt(activeRadius * 2 + 1);
            if (az > activeRadius) {
                az += minRange * 2;
            }
            int nz = z - maxRange + az;
            int ny;
            for (ny = y; ny < y + 8; ++ny) {
                if (world.isAirBlock(nx, ny, nz)) break;
            }
            while (world.isAirBlock(nx, ny, nz) && ny > 0) {
                --ny;
            }
            int hy;
            for (hy = 0; hy < 6; ++hy) {
                if (!world.isAirBlock(nx, ny + hy + 1, nz)) break;
            }
            if (hy >= 2) {
                EntityHallucination creature = EntitySpecificHallucinations.create(creatureClass, world);
                if (creature != null) {
                    if (target instanceof EntityPlayer) {
                        EntityPlayer player = (EntityPlayer) target;
                        creature.setTarget(player.getCommandSenderName());
                    }

                    creature.setLocationAndAngles(
                        0.5D + (double) nx,
                        0.05D + (double) ny + 1.0D,
                        0.5D + (double) nz,
                        0.0F,
                        0.0F);
                    world.spawnEntityInWorld(creature);

                    return creature;
                }
            }
        }
        return null;
    }

    @SubscribeEvent
    public void onEntityInteract(EntityInteractEvent event) {
        if (!(event.entityLiving instanceof FakePlayer) && event.target instanceof EntityItemFrame)
            AMCore.proxy.itemFrameWatcher.startWatchingFrame((EntityItemFrame) event.target);
    }

    @SubscribeEvent
    public void onPlayerTossItem(ItemTossEvent event) {
        if (!event.entityItem.worldObj.isRemote) EntityItemWatcher.instance.addWatchedItem(event.entityItem);
    }

    @SubscribeEvent
    public void onEntityAttacked(LivingAttackEvent event) {
        if (event.source.isFireDamage() && event.entityLiving instanceof EntityPlayer
            && ((EntityPlayer) event.entityLiving).inventory.armorInventory[3] != null) {
            if (((EntityPlayer) event.entityLiving).inventory.armorInventory[3].getItem() == ItemsCommonProxy.fireEars
                || ((EntityPlayer) event.entityLiving).inventory.armorInventory[3].getItem()
                    == ItemsCommonProxy.archmageHood) {
                event.entityLiving.setFire(0);
                event.setCanceled(true);
                return;
            }
        } else if (event.entityLiving instanceof EntityPlayer
            && ((EntityPlayer) event.entityLiving).inventory.armorInventory[3] != null
            && ((EntityPlayer) event.entityLiving).inventory.armorInventory[3].getItem()
                == ItemsCommonProxy.archmageHood) {
                    if (event.source.getEntity() != null) {
                        event.source.getEntity()
                            .attackEntityFrom(
                                new DamageSourceFire(event.entityLiving),
                                event.entityLiving.worldObj.rand.nextInt(3));
                        event.source.getEntity()
                            .setFire(8);
                    }
                }

        if (event.entityLiving.isPotionActive(BuffList.manaShield)) {
            if (ExtendedProperties.For(event.entityLiving)
                .getCurrentMana() >= event.ammount * 250f) {
                ExtendedProperties.For(event.entityLiving)
                    .deductMana(event.ammount * 100f);
                ExtendedProperties.For(event.entityLiving)
                    .forceSync();
                for (int i = 0; i < Math.min(event.ammount, 5 * AMCore.config.getGFXLevel()); ++i)
                    AMCore.proxy.particleManager.BoltFromPointToPoint(
                        event.entityLiving.worldObj,
                        event.entityLiving.posX,
                        event.entityLiving.posY
                            + event.entityLiving.worldObj.rand.nextFloat() * event.entityLiving.getEyeHeight(),
                        event.entityLiving.posZ,
                        event.entityLiving.posX - 1 + event.entityLiving.worldObj.rand.nextFloat() * 2,
                        event.entityLiving.posY - 1 + event.entityLiving.worldObj.rand.nextFloat() * 2,
                        event.entityLiving.posZ - 1 + event.entityLiving.worldObj.rand.nextFloat() * 2,
                        6,
                        -1);
                event.entityLiving.worldObj.playSoundAtEntity(
                    event.entityLiving,
                    "arsmagica2:spell.cast.arcane",
                    1.0f,
                    event.entityLiving.worldObj.rand.nextFloat() + 0.5f);
                event.setCanceled(true);
            }
        }
    }

    private double oldKnockbackValue = -1;

    @SubscribeEvent
    public void onEntityHurt(LivingHurtEvent event) {

        if (event.source.isFireDamage() && event.entityLiving instanceof EntityPlayer
            && ((EntityPlayer) event.entityLiving).inventory.armorInventory[3] != null) {
            if (((EntityPlayer) event.entityLiving).inventory.armorInventory[3].getItem() == ItemsCommonProxy.fireEars
                || ((EntityPlayer) event.entityLiving).inventory.armorInventory[3].getItem()
                    == ItemsCommonProxy.archmageHood) {
                event.setCanceled(true);
                return;
            }
        } else if (event.entityLiving instanceof EntityPlayer
            && ((EntityPlayer) event.entityLiving).inventory.armorInventory[3] != null
            && ((EntityPlayer) event.entityLiving).inventory.armorInventory[3].getItem()
                == ItemsCommonProxy.archmageHood) {
                    if (event.source.getEntity() != null) {
                        event.source.getEntity()
                            .attackEntityFrom(
                                new DamageSourceFire(event.entityLiving),
                                event.entityLiving.worldObj.rand.nextInt(3));
                        event.source.getEntity()
                            .setFire(8);
                    }
                }

        if (event.entityLiving instanceof EntityPlayer
            && ((EntityPlayer) event.entityLiving).inventory.armorInventory[2] != null) {
            if (((EntityPlayer) event.entityLiving).inventory.armorInventory[2].getItem()
                == ItemsCommonProxy.archmageArmor) {
                if (oldKnockbackValue == -1) {
                    oldKnockbackValue = event.entityLiving
                        .getEntityAttribute(SharedMonsterAttributes.knockbackResistance)
                        .getAttributeValue();
                }
                event.entityLiving.getEntityAttribute(SharedMonsterAttributes.knockbackResistance)
                    .setBaseValue(1);
            } else {
                event.entityLiving.getEntityAttribute(SharedMonsterAttributes.knockbackResistance)
                    .setBaseValue(oldKnockbackValue);
                oldKnockbackValue = -1;
            }
        } else {
            event.entityLiving.getEntityAttribute(SharedMonsterAttributes.knockbackResistance)
                .setBaseValue(oldKnockbackValue);
            oldKnockbackValue = -1;
        }

        PotionEffect magicShield = event.entityLiving.getActivePotionEffect(BuffList.magicShield);
        if (magicShield != null) {
            event.ammount *= 0.5f;
            event.ammount *= (1f / (magicShield.getAmplifier() + 1f));
        }

        if (event.entityLiving.isPotionActive(BuffList.manaShield)) {
            float manaToTake = Math.min(
                ExtendedProperties.For(event.entityLiving)
                    .getCurrentMana(),
                event.ammount * 250f);
            event.ammount -= manaToTake / 250f;
            ExtendedProperties.For(event.entityLiving)
                .deductMana(manaToTake);
            ExtendedProperties.For(event.entityLiving)
                .forceSync();
            for (int i = 0; i < Math.min(event.ammount, 5 * AMCore.config.getGFXLevel()); ++i)
                AMCore.proxy.particleManager.BoltFromPointToPoint(
                    event.entityLiving.worldObj,
                    event.entityLiving.posX,
                    event.entityLiving.posY
                        + event.entityLiving.worldObj.rand.nextFloat() * event.entityLiving.getEyeHeight(),
                    event.entityLiving.posZ,
                    event.entityLiving.posX - 1 + event.entityLiving.worldObj.rand.nextFloat() * 2,
                    event.entityLiving.posY + event.entityLiving.getEyeHeight()
                        - 1
                        + event.entityLiving.worldObj.rand.nextFloat() * 2,
                    event.entityLiving.posZ - 1 + event.entityLiving.worldObj.rand.nextFloat() * 2,
                    6,
                    -1);
            event.entityLiving.worldObj.playSoundAtEntity(
                event.entityLiving,
                "arsmagica2:spell.cast.arcane",
                1.0f,
                event.entityLiving.worldObj.rand.nextFloat() + 0.5f);
            if (event.ammount <= 0) {
                event.setCanceled(true);
                return;
            }
        }

        Entity entityAttacker = event.source.getSourceOfDamage();
        if (entityAttacker instanceof EntityPlayer
            && ((EntityPlayer) entityAttacker).inventory.armorInventory[2] != null
            && ((EntityPlayer) entityAttacker).inventory.armorInventory[2].getItem()
                == ItemsCommonProxy.earthGuardianArmor
            && ((EntityPlayer) entityAttacker).getCurrentEquippedItem() == null) {
            event.ammount += 4;

            double deltaZ = event.entityLiving.posZ - entityAttacker.posZ;
            double deltaX = event.entityLiving.posX - entityAttacker.posX;
            double angle = Math.atan2(deltaZ, deltaX);
            double speed = entityAttacker.isSprinting() ? 3 : 2;
            double vertSpeed = entityAttacker.isSprinting() ? 0.5 : 0.325;

            if (event.entityLiving instanceof EntityPlayer) {
                AMNetHandler.INSTANCE.sendVelocityAddPacket(
                    event.entityLiving.worldObj,
                    event.entityLiving,
                    speed * Math.cos(angle),
                    vertSpeed,
                    speed * Math.sin(angle));
            } else {
                event.entityLiving.motionX += (speed * Math.cos(angle));
                event.entityLiving.motionZ += (speed * Math.sin(angle));
                event.entityLiving.motionY += vertSpeed;
            }
            event.entityLiving.worldObj.playSoundAtEntity(
                event.entityLiving,
                "arsmagica2:spell.cast.earth",
                0.4f,
                event.entityLiving.worldObj.rand.nextFloat() * 0.1F + 0.9F);
        }
        if (entityAttacker instanceof EntityPlayer
            && ((EntityPlayer) entityAttacker).inventory.armorInventory[2] != null
            && ((EntityPlayer) entityAttacker).inventory.armorInventory[2].getItem()
                == ItemsCommonProxy.archmageArmor) {
            event.ammount += 5;

            double deltaZ = event.entityLiving.posZ - entityAttacker.posZ;
            double deltaX = event.entityLiving.posX - entityAttacker.posX;
            double angle = Math.atan2(deltaZ, deltaX);
            double speed = entityAttacker.isSprinting() ? 5 : 3;
            double vertSpeed = entityAttacker.isSprinting() ? 1 : 0.65;

            if (event.entityLiving instanceof EntityPlayer) {
                AMNetHandler.INSTANCE.sendVelocityAddPacket(
                    event.entityLiving.worldObj,
                    event.entityLiving,
                    speed * Math.cos(angle),
                    vertSpeed,
                    speed * Math.sin(angle));
            } else {
                event.entityLiving.motionX += (speed * Math.cos(angle));
                event.entityLiving.motionZ += (speed * Math.sin(angle));
                event.entityLiving.motionY += vertSpeed;
            }
            event.entityLiving.worldObj.playSoundAtEntity(
                event.entityLiving,
                "arsmagica2:spell.cast.earth",
                0.4f,
                event.entityLiving.worldObj.rand.nextFloat() * 0.1F + 0.9F);
        }

        ExtendedProperties extendedProperties = ExtendedProperties.For(event.entityLiving);
        EntityLivingBase entityAttacked = event.entityLiving;
        if (!(extendedProperties.getContingencyEffect(0)
            .getItem() instanceof ItemSnowball)) {
            extendedProperties.procContingency(
                0,
                (event.source.getEntity() != null && event.source.getEntity() instanceof EntityLivingBase)
                    ? (EntityLivingBase) event.source.getEntity()
                    : null);
        }
        if (!(extendedProperties.getContingencyEffect(4)
            .getItem() instanceof ItemSnowball) && entityAttacked.getHealth() <= entityAttacked.getMaxHealth() / 3) {
            extendedProperties.procContingency(
                4,
                (event.source.getEntity() != null && event.source.getEntity() instanceof EntityLivingBase)
                    ? (EntityLivingBase) event.source.getEntity()
                    : null);
        }

        if (entityAttacker instanceof EntityPlayer
            && ((EntityPlayer) entityAttacker).inventory.armorInventory[3] != null
            && ((EntityPlayer) entityAttacker).inventory.armorInventory[2] != null
            && ((EntityPlayer) entityAttacker).inventory.armorInventory[1] != null
            && ((EntityPlayer) entityAttacker).inventory.armorInventory[0] != null
            && ((EntityPlayer) entityAttacker).inventory.armorInventory[3].getItem() == ItemsCommonProxy.archmageHood
            && ((EntityPlayer) entityAttacker).inventory.armorInventory[2].getItem() == ItemsCommonProxy.archmageArmor
            && ((EntityPlayer) entityAttacker).inventory.armorInventory[1].getItem()
                == ItemsCommonProxy.archmageLeggings
            && ((EntityPlayer) entityAttacker).inventory.armorInventory[0].getItem()
                == ItemsCommonProxy.archmageBoots) {
            if (entityAttacker.isSneaking()) {
                extendedProperties.toggleFlipped();
                tempFlipped.put(extendedProperties, 50);
            }
        }

        if (entityAttacked.isPotionActive(BuffList.fury.id)) event.ammount /= 2;

        if (entityAttacker instanceof EntityLivingBase
            && ((EntityLivingBase) entityAttacker).isPotionActive(BuffList.shrink)) event.ammount /= 2;
    }

    @SubscribeEvent
    public void onBlockOverlay(RenderBlockOverlayEvent event) {
        if (event.overlayType == RenderBlockOverlayEvent.OverlayType.WATER) {
            if (AffinityHelper.isNotInWaterActually.contains(event.player)) {
                event.setCanceled(true);
            }
        } else if (event.overlayType == RenderBlockOverlayEvent.OverlayType.FIRE) {
            if (event.player != null && event.player.inventory.armorInventory[3] != null
                && event.player.inventory.armorInventory[3].getItem() == ItemsCommonProxy.archmageHood) {
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public void onEntityJoinWorld(EntityJoinWorldEvent event) {
        if (event.entity instanceof EntityLivingBase
            && ((EntityLivingBase) event.entity).isPotionActive(BuffList.temporalAnchor.id)) {
            ((EntityLivingBase) event.entity).removePotionEffect(BuffList.temporalAnchor.id);
        }
    }

    @SubscribeEvent
    public void onBreakSpeed(BreakSpeed event) {
        EntityPlayer player = event.entityPlayer;
        if (player.isPotionActive(BuffList.fury.id)) event.newSpeed = event.originalSpeed * 5;
    }

    @SubscribeEvent
    public void onManaCost(ManaCostEvent event) {
        if (event.caster.getHeldItem() != null && event.caster.getHeldItem()
            .getItem() == ItemsCommonProxy.arcaneSpellbook) {
            event.manaCost *= 0.75f;
            event.burnout *= 0.4f;
        }
    }

    @SubscribeEvent
    public void onPlayerPickupItem(EntityItemPickupEvent event) {
        if (event.entityPlayer == null) return;

        if (!event.entityPlayer.worldObj.isRemote && ExtendedProperties.For(event.entityPlayer)
            .getMagicLevel() <= 0
            && event.item.getEntityItem()
                .getItem() == ItemsCommonProxy.arcaneCompendium) {
            event.entityPlayer.addChatMessage(new ChatComponentText("You have unlocked the secrets of the arcane!"));
            AMNetHandler.INSTANCE.sendCompendiumUnlockPacket((EntityPlayerMP) event.entityPlayer, "shapes", true);
            AMNetHandler.INSTANCE.sendCompendiumUnlockPacket((EntityPlayerMP) event.entityPlayer, "components", true);
            AMNetHandler.INSTANCE.sendCompendiumUnlockPacket((EntityPlayerMP) event.entityPlayer, "modifiers", true);
            ExtendedProperties.For(event.entityPlayer)
                .setMagicLevelWithMana(1);
            ExtendedProperties.For(event.entityPlayer)
                .forceSync();
            return;
        }

        if (event.item.getEntityItem()
            .getItem() == ItemsCommonProxy.spell) {
            if (event.entityPlayer.worldObj.isRemote) {
                AMNetHandler.INSTANCE
                    .sendCompendiumUnlockPacket((EntityPlayerMP) event.entityPlayer, "spell_book", false);
            }
        } else {
            Item item = event.item.getEntityItem()
                .getItem();
            int meta = event.item.getEntityItem()
                .getItemDamage();

            if (event.entityPlayer.worldObj.isRemote && item.getUnlocalizedName() != null
                && (AMCore.proxy.items.getArsMagicaItems()
                    .contains(item))
                || (item instanceof ItemBlock && AMCore.proxy.blocks.getArsMagicaBlocks()
                    .contains(((ItemBlock) item).field_150939_a))) {
                AMNetHandler.INSTANCE.sendCompendiumUnlockPacket(
                    (EntityPlayerMP) event.entityPlayer,
                    item.getUnlocalizedName()
                        .replace("item.", "")
                        .replace("arsmagica2:", "")
                        .replace("tile.", "") + ((meta > -1) ? "@" + meta : ""),
                    false);
            }
        }
    }
}
