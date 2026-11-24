package am2.gametest;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.entity.projectile.EntityFishHook;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.Potion;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.oredict.OreDictionary;

import am2.AMCore;
import am2.api.ArsMagicaApi;
import am2.blocks.BlocksCommonProxy;
import am2.blocks.tileentities.TileEntityEssenceRefiner;
import am2.blocks.tileentities.TileEntityManaBattery;
import am2.blocks.tileentities.TileEntityOcculus;
import am2.buffs.BuffList;
import am2.enchantments.AMEnchantments;
import am2.entities.EntityManaCreeper;
import am2.items.AMSpawnEgg;
import am2.items.ItemOre;
import am2.items.ItemsCommonProxy;
import am2.mixins.early.DamageSourceAccessor;
import am2.mixins.early.EntityArrowAccessor;
import am2.mixins.early.EntityFishHookAccessor;
import am2.mixins.early.PotionAccessor;
import am2.utility.RedstoneInlayState;
import am2.utility.TickrateController;
import cpw.mods.fml.common.registry.GameRegistry;

/**
 * The assertions the in-game test mod runs against a live dedicated server.
 *
 * <p>
 * Everything here needs state a headless unit test cannot create: the real Forge block and item registries, the real
 * recipe list, the real tile-entity mapping, and a world that can accept block mutations. The unit tests in
 * {@code src/test/java} cover the arithmetic; this covers the wiring.
 * </p>
 *
 * <p>
 * Every check goes through {@link #check(boolean, String)} so a failure is counted and reported instead of thrown: one
 * broken assumption should not hide the rest.
 * </p>
 */
final class ArsMagicaGameTests {

    /** Above the world, so the test never collides with generated terrain. */
    private static final int TEST_HEIGHT = 200;

    private static int sChecks;
    private static int sFailures;

    private ArsMagicaGameTests() {}

    static int checkCount() {
        return sChecks;
    }

    static int failureCount() {
        return sFailures;
    }

    private static void check(boolean pCondition, String pDescription) {
        ++sChecks;
        if (!pCondition) {
            ++sFailures;
            System.out.println("[ARSMAGICA-GAMETEST] FAILED: " + pDescription);
        }
    }

    private static void checkEquals(Object pExpected, Object pActual, String pDescription) {
        check(
            pExpected == null ? pActual == null : pExpected.equals(pActual),
            pDescription + " (expected " + pExpected + ", got " + pActual + ")");
    }

    /**
     * Runs every assertion. Must be called on the server thread with a live overworld.
     */
    static void runAll(WorldServer pWorld) {
        checksCore();
        checksConfiguration();
        checksBlocks();
        checksItems();
        checksTileEntities(pWorld);
        checksWorldPlacement(pWorld);
        checksOreDictionary();
        checksRecipes();
        checksSpellCraftingRecipe(pWorld);
        checksApi();
        checksFixMixins(pWorld);
    }

    // -------------------------------------------------------------------------------------------------------
    // Core / configuration
    // -------------------------------------------------------------------------------------------------------

    private static void checksCore() {
        check(AMCore.instance != null, "AMCore.instance must be set");
        check(AMCore.proxy != null, "AMCore.proxy must be set");
        check(AMCore.proxy.blocks != null, "the block proxy must be constructed");
        check(AMCore.proxy.items != null, "the item proxy must be constructed");

        // Proves the Mixin loader, manifest entry and refmap are active. Before the migration the ASM coremod was
        // silently absent and nothing noticed; this assertion fails loudly if the same happens to the Mixins.
        check(
            Potion.potionTypes.length >= 128,
            "PotionMixin must grow Potion.potionTypes to at least 128 (got " + Potion.potionTypes.length + ")");

        // One runtime check per accessor/invoker feature group, so a missing refmap mapping fails here instead of
        // surfacing later as a ClassCastException in combat or armor handling.
        check(
            !((PotionAccessor) Potion.potionTypes[Potion.moveSpeed.id]).am2$isBadEffect(),
            "PotionAccessor must report move speed as a good effect");
        DamageSource testSource = new DamageSource("am2gametest");
        ((DamageSourceAccessor) testSource).am2$setUnblockable(true);
        check(testSource.isUnblockable(), "DamageSourceAccessor must set isUnblockable");

        // The tick-loop mixin runs on every server tick, so this is a real proof that MinecraftServer.run() was
        // transformed rather than a check that the class merely loaded.
        check(TickrateController.isMixinApplied(), "MinecraftServerMixin must run on the server tick loop");
        check(TickrateController.getServerTickrate() > 0L, "the server tick length must be positive");

        check(
            AMCore.proxy.blocks.getArsMagicaBlocks() != null && !AMCore.proxy.blocks.getArsMagicaBlocks()
                .isEmpty(),
            "the block list must not be empty");
        check(
            AMCore.proxy.items.getArsMagicaItems() != null && !AMCore.proxy.items.getArsMagicaItems()
                .isEmpty(),
            "the item list must not be empty");

        check(BlocksCommonProxy.blockTab != null, "the blocks creative tab must exist");
        check(ItemsCommonProxy.itemTab != null, "the items creative tab must exist");
        if (BlocksCommonProxy.blockTab != null) {
            check(BlocksCommonProxy.blockTab.getTabIconItem() != null, "the blocks tab must have an icon");
        }
        if (ItemsCommonProxy.itemTab != null) {
            check(ItemsCommonProxy.itemTab.getTabIconItem() != null, "the items tab must have an icon");
        }
    }

    private static void checksConfiguration() {
        check(AMCore.config != null, "AMCore.config must be loaded");
        if (AMCore.config == null) {
            return;
        }

        check(
            DimensionManager.isDimensionRegistered(AMCore.config.getMMFDimensionID()),
            "the Moo Moo Farm dimension must be registered");
        check(AMCore.config.getDamageMultiplier() > 0F, "the damage multiplier must be positive");
        check(
            AMCore.config.getGFXLevel() >= 0 && AMCore.config.getGFXLevel() <= 2,
            "the graphics level must be in [0, 2]");
        check(
            AMCore.config.spawnEtherMode() >= 0 && AMCore.config.spawnEtherMode() <= 3,
            "the etherium spawn mode must be in [0, 3]");
    }

    // -------------------------------------------------------------------------------------------------------
    // Registries
    // -------------------------------------------------------------------------------------------------------

    private static void checksBlocks() {
        check(BlocksCommonProxy.essenceRefiner != null, "the essence refiner block must be instantiated");
        check(BlocksCommonProxy.witchwoodLog != null, "the witchwood log block must be instantiated");
        check(BlocksCommonProxy.witchwoodLeaves != null, "the witchwood leaves block must be instantiated");
        check(BlocksCommonProxy.occulus != null, "the occulus block must be instantiated");
        check(BlocksCommonProxy.craftingAltar != null, "the crafting altar block must be instantiated");
        check(BlocksCommonProxy.magiciansWorkbench != null, "the magician's workbench block must be instantiated");
        check(BlocksCommonProxy.manaBattery != null, "the mana battery block must be instantiated");
        check(BlocksCommonProxy.liquidEssence != null, "the liquid essence block must be instantiated");
        check(BlocksCommonProxy.redstoneInlay != null, "the redstone inlay block must be instantiated");
        check(BlocksCommonProxy.illusionBlock != null, "the illusion block must be instantiated");
        check(BlocksCommonProxy.crystalMarker != null, "the crystal marker block must be instantiated");

        checkRegisteredBlock("essenceRefiner", BlocksCommonProxy.essenceRefiner);
        checkRegisteredBlock("WitchwoodLog", BlocksCommonProxy.witchwoodLog);
        checkRegisteredBlock("WitchwoodLeaves", BlocksCommonProxy.witchwoodLeaves);
        checkRegisteredBlock("occulus", BlocksCommonProxy.occulus);
        checkRegisteredBlock("CraftingAltar", BlocksCommonProxy.craftingAltar);
        checkRegisteredBlock("magiciansWorkbench", BlocksCommonProxy.magiciansWorkbench);
        checkRegisteredBlock("manaBattery", BlocksCommonProxy.manaBattery);
        checkRegisteredBlock("liquidEssence", BlocksCommonProxy.liquidEssence);
        checkRegisteredBlock("redstoneInlay", BlocksCommonProxy.redstoneInlay);

        check(
            Item.getItemFromBlock(BlocksCommonProxy.witchwoodLog) != null,
            "the witchwood log must have an ItemBlock");
        check(Item.getItemFromBlock(BlocksCommonProxy.occulus) != null, "the occulus must have an ItemBlock");
    }

    private static void checkRegisteredBlock(String pName, Block pBlock) {
        Block registered = GameRegistry.findBlock("arsmagica2", pName);
        check(registered != null, "the block '" + pName + "' must be registered");
        checkEquals(pBlock, registered, "the block '" + pName + "' must resolve to the expected instance");
    }

    private static void checksItems() {
        check(ItemsCommonProxy.itemOre != null, "the ore item must be instantiated");
        check(ItemsCommonProxy.essence != null, "the essence item must be instantiated");
        check(ItemsCommonProxy.spell != null, "the spell item must be instantiated");
        check(ItemsCommonProxy.spellBook != null, "the spell book must be instantiated");
        check(ItemsCommonProxy.arcaneSpellbook != null, "the arcane spellbook must be instantiated");
        check(ItemsCommonProxy.rune != null, "the rune item must be instantiated");
        check(ItemsCommonProxy.manaCake != null, "the mana cake must be instantiated");
        check(ItemsCommonProxy.keystone != null, "the keystone must be instantiated");
        check(ItemsCommonProxy.crystalWrench != null, "the crystal wrench must be instantiated");
        check(ItemsCommonProxy.arcaneCompendium != null, "the arcane compendium must be instantiated");
        check(ItemsCommonProxy.mageHood != null, "the mage hood must be instantiated");

        checkRegisteredItem("itemOre", ItemsCommonProxy.itemOre);
        checkRegisteredItem("essence", ItemsCommonProxy.essence);
        checkRegisteredItem("spellBase", ItemsCommonProxy.spell);
        checkRegisteredItem("spellBook", ItemsCommonProxy.spellBook);
        checkRegisteredItem("rune", ItemsCommonProxy.rune);
        checkRegisteredItem("manaCake", ItemsCommonProxy.manaCake);
        checkRegisteredItem("crystal_wrench", ItemsCommonProxy.crystalWrench);
        checkRegisteredItem("ArcaneCompendium", ItemsCommonProxy.arcaneCompendium);
    }

    private static void checkRegisteredItem(String pName, Item pItem) {
        Item registered = GameRegistry.findItem("arsmagica2", pName);
        check(registered != null, "the item '" + pName + "' must be registered");
        checkEquals(pItem, registered, "the item '" + pName + "' must resolve to the expected instance");
    }

    // -------------------------------------------------------------------------------------------------------
    // Tile entities
    // -------------------------------------------------------------------------------------------------------

    private static void checksTileEntities(World pWorld) {
        check(
            BlocksCommonProxy.occulus.createTileEntity(pWorld, 0) instanceof TileEntityOcculus,
            "the occulus must create a TileEntityOcculus");
        check(
            BlocksCommonProxy.essenceRefiner.createTileEntity(pWorld, 0) instanceof TileEntityEssenceRefiner,
            "the essence refiner must create a TileEntityEssenceRefiner");
        check(
            BlocksCommonProxy.manaBattery.createTileEntity(pWorld, 0) instanceof TileEntityManaBattery,
            "the mana battery must create a TileEntityManaBattery");

        checkTileEntityMapping("TileEntityOcculus", TileEntityOcculus.class);
        checkTileEntityMapping("TileEntityEssenceRefiner", TileEntityEssenceRefiner.class);
        checkTileEntityMapping("TileEntityManaBattery", TileEntityManaBattery.class);

        TileEntity occulus = createTileEntity("TileEntityOcculus");
        if (occulus instanceof TileEntityOcculus) {
            NBTTagCompound saved = new NBTTagCompound();
            occulus.writeToNBT(saved);

            checkEquals("TileEntityOcculus", saved.getString("id"), "the occulus NBT must carry its registered id");
        }
    }

    private static void checkTileEntityMapping(String pId, Class<? extends TileEntity> pClass) {
        TileEntity entity = createTileEntity(pId);
        check(entity != null, "the tile entity '" + pId + "' must be registered");
        check(pClass.isInstance(entity), "the tile entity '" + pId + "' must load as " + pClass.getSimpleName());
    }

    private static TileEntity createTileEntity(String pId) {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("id", pId);
        return TileEntity.createAndLoadEntity(tag);
    }

    // -------------------------------------------------------------------------------------------------------
    // World, ore dictionary and recipes
    // -------------------------------------------------------------------------------------------------------

    private static void checksWorldPlacement(WorldServer pWorld) {
        int x = 0;
        int z = 0;
        int y = TEST_HEIGHT;

        try {
            check(pWorld.setBlock(x, y, z, BlocksCommonProxy.witchwoodLog), "a witchwood log must be placeable");
            checkEquals(
                BlocksCommonProxy.witchwoodLog,
                pWorld.getBlock(x, y, z),
                "the placed block must read back as the witchwood log");

            check(pWorld.setBlock(x, y, z, BlocksCommonProxy.occulus), "the occulus must be placeable");
            checkEquals(
                BlocksCommonProxy.occulus,
                pWorld.getBlock(x, y, z),
                "the placed block must read back as the occulus");
            check(
                pWorld.getTileEntity(x, y, z) instanceof TileEntityOcculus,
                "placing the occulus must create its tile entity");

            check(pWorld.setBlock(x, y, z, BlocksCommonProxy.manaBattery), "the mana battery must be placeable");
            check(
                pWorld.getTileEntity(x, y, z) instanceof TileEntityManaBattery,
                "placing the mana battery must create its tile entity");
        } finally {
            pWorld.setBlockToAir(x, y, z);
        }

        check(pWorld.getTileEntity(x, y, z) == null, "removing the block must remove its tile entity");
    }

    private static void checksOreDictionary() {
        check(
            !OreDictionary.getOres("witchwoodLog")
                .isEmpty(),
            "the witchwoodLog ore dictionary entry must exist");
        check(
            !OreDictionary.getOres("logWood")
                .isEmpty(),
            "the logWood ore dictionary entry must exist");
        check(
            !OreDictionary.getOres("plankMagical")
                .isEmpty(),
            "the plankMagical ore dictionary entry must exist");

        check(
            oreDictionaryContains("dustVinteum", ItemsCommonProxy.itemOre, ItemOre.META_VINTEUMDUST),
            "dustVinteum must contain vinteum dust");
        check(
            oreDictionaryContains("gemMoonstone", ItemsCommonProxy.itemOre, ItemOre.META_MOONSTONE),
            "gemMoonstone must contain a moonstone");
    }

    private static boolean oreDictionaryContains(String pOre, Item pItem, int pMetadata) {
        List<ItemStack> stacks = OreDictionary.getOres(pOre);
        for (ItemStack stack : stacks) {
            if (stack != null && stack.getItem() == pItem && stack.getItemDamage() == pMetadata) {
                return true;
            }
        }
        return false;
    }

    private static void checksRecipes() {
        List<IRecipe> recipes = CraftingManager.getInstance()
            .getRecipeList();
        check(recipes != null && !recipes.isEmpty(), "the crafting recipe list must not be empty");
        if (recipes == null) {
            return;
        }

        Item planks = Item.getItemFromBlock(BlocksCommonProxy.witchwoodPlanks);
        boolean found = false;
        for (IRecipe recipe : recipes) {
            ItemStack output = recipe.getRecipeOutput();
            if (output != null && output.getItem() == planks && output.stackSize == 4) {
                found = true;
                break;
            }
        }
        check(found, "the witchwood log must craft into four witchwood planks");
    }

    // -------------------------------------------------------------------------------------------------------
    // API
    // -------------------------------------------------------------------------------------------------------

    /**
     * Runtime proof for one ported {@code @Fix} group. The unit tests pin the annotations; this proves the mixin is
     * actually applied to the live world class.
     */
    private static void checksFixMixins(WorldServer pWorld) {
        int x = 0;
        int y = TEST_HEIGHT + 5;
        int z = 0;

        RedstoneInlayState.clear();
        RedstoneInlayState.add(0, x, y, z, 1);
        check(
            pWorld.isBlockIndirectlyGettingPowered(x, y, z),
            "WorldRedstoneMixin must report an active Redstone Signal inlay");
        check(!pWorld.isBlockIndirectlyGettingPowered(x, y, z), "WorldRedstoneMixin must consume the inlay tick");

        // The spawn egg now uses EntityList.createEntityByName instead of reflected constructors.
        Entity spawned = AMSpawnEgg.spawnCreature(pWorld, 0, x, y, z);
        check(spawned instanceof EntityManaCreeper, "AMSpawnEgg must create a registered entity through EntityList");
        if (spawned != null) {
            spawned.setDead();
        }

        // The projectile subclasses now use accessor mixins instead of access transformers.
        EntityFishHook hook = new EntityFishHook(pWorld);
        EntityFishHookAccessor hookAccessor = (EntityFishHookAccessor) hook;
        hookAccessor.am2$setX(123);
        hookAccessor.am2$setInGround(true);
        check(
            hookAccessor.am2$getX() == 123 && hookAccessor.am2$isInGround(),
            "EntityFishHookAccessor must expose the private fish-hook state");

        EntityArrow arrow = new EntityArrow(pWorld);
        EntityArrowAccessor arrowAccessor = (EntityArrowAccessor) arrow;
        arrowAccessor.am2$setInGround(true);
        arrowAccessor.am2$setInData(7);
        check(
            arrowAccessor.am2$isInGround() && arrowAccessor.am2$getInData() == 7,
            "EntityArrowAccessor must expose the private arrow state");
    }

    /**
     * Runtime proof for the dynamic staff/scroll/armor recipe that replaced the global
     * {@code CraftingManager.findMatchingRecipe} patch.
     */
    private static void checksSpellCraftingRecipe(WorldServer pWorld) {
        InventoryCrafting grid = new InventoryCrafting(new Container() {

            @Override
            public void onCraftMatrixChanged(IInventory inventory) {}

            @Override
            public boolean canInteractWith(EntityPlayer player) {
                return true;
            }
        }, 3, 3);

        // One staff plus one spell scroll puts the spell on the staff.
        grid.setInventorySlotContents(0, new ItemStack(ItemsCommonProxy.spellStaffNovice));
        grid.setInventorySlotContents(4, new ItemStack(ItemsCommonProxy.spell));
        ItemStack result = CraftingManager.getInstance()
            .findMatchingRecipe(grid, pWorld);
        check(
            result != null && result.getItem() == ItemsCommonProxy.spellStaffNovice,
            "a staff plus a spell scroll must craft a staff");

        // Two staffs copy the charge from the second to the first.
        ItemStack source = new ItemStack(ItemsCommonProxy.spellStaffNovice);
        source.setTagCompound(new NBTTagCompound());
        source.getTagCompound()
            .setFloat("current_charge", 123.0F);
        grid.setInventorySlotContents(0, new ItemStack(ItemsCommonProxy.spellStaffNovice));
        grid.setInventorySlotContents(4, source);
        result = CraftingManager.getInstance()
            .findMatchingRecipe(grid, pWorld);
        check(result != null && result.getItem() == ItemsCommonProxy.spellStaffNovice, "two staffs must craft a staff");
        check(
            result != null && result.getTagCompound() != null
                && Math.abs(
                    result.getTagCompound()
                        .getFloat("current_charge") - 123.0F)
                    < 0.01F,
            "two staffs must copy the source charge");

        // Bound armor plus a spell scroll puts the spell on the armor.
        grid.setInventorySlotContents(0, new ItemStack(ItemsCommonProxy.BoundArmor));
        grid.setInventorySlotContents(4, new ItemStack(ItemsCommonProxy.spell));
        result = CraftingManager.getInstance()
            .findMatchingRecipe(grid, pWorld);
        check(
            result != null && result.getItem() == ItemsCommonProxy.BoundArmor,
            "bound armor plus a spell scroll must craft bound armor");
    }

    private static void checksApi() {
        check(AMEnchantments.magicResist != null, "the magic-resist enchantment must be registered");
        check(AMEnchantments.soulbound != null, "the soulbound enchantment must be registered");
        checkEquals(
            BuffList.flight.getId(),
            BuffList.instance.getPotionID("flight"),
            "BuffList must look up potions by field name");
        checkEquals(-1, BuffList.instance.getPotionID("not_a_buff"), "unknown buff names must return -1");
        check(
            BuffList.buffEffectFromPotionID(BuffList.flight.getId(), 100, 0) != null,
            "the buff-effect factory must construct a known buff");
        check(ArsMagicaApi.instance != null, "the Ars Magica API singleton must exist");
        check(ArsMagicaApi.instance.getSkillTreeManager() != null, "the skill tree manager must be set");
        check(ArsMagicaApi.instance.getEntityManager() != null, "the entity manager must be set");
        check(ArsMagicaApi.instance.getFuelHelper() != null, "the obelisk fuel helper must be set");
        check(ArsMagicaApi.instance.getFlickerOperatorRegistry() != null, "the flicker operator registry must be set");
        check(ArsMagicaApi.instance.getArmorInfusionRegistry() != null, "the armor infusion registry must be set");
        check(ArsMagicaApi.instance.getBuffHelper() != null, "the buff helper must be set");
        check(ArsMagicaApi.instance.getSpellUtils() != null, "the spell utils helper must be set");
        check(ArsMagicaApi.instance.getKeystoneHelper() != null, "the keystone helper must be set");
        check(ArsMagicaApi.instance.getEssenceRecipeManager() != null, "the essence recipe manager must be set");

        checkEquals(38F, ArsMagicaApi.getBurnoutFromMana(100F), "the burnout ratio");
    }

    private static void checkEquals(float pExpected, float pActual, String pDescription) {
        check(
            Math.abs(pExpected - pActual) < 0.0001F,
            pDescription + " (expected " + pExpected + ", got " + pActual + ")");
    }
}
