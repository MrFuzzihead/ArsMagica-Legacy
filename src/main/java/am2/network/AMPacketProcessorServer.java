package am2.network;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;

import org.apache.logging.log4j.Level;

import am2.AMCore;
import am2.LogHelper;
import am2.PlayerTracker;
import am2.api.math.AMVector3;
import am2.api.power.IPowerNode;
import am2.api.power.PowerTypes;
import am2.blocks.tileentities.TileEntityArmorImbuer;
import am2.blocks.tileentities.TileEntityBlockCaster;
import am2.blocks.tileentities.TileEntityInscriptionTable;
import am2.blocks.tileentities.TileEntityMagiciansWorkbench;
import am2.blocks.tileentities.TileEntityParticleEmitter;
import am2.containers.ContainerMagiciansWorkbench;
import am2.containers.ContainerSpellCustomization;
import am2.customdata.CustomWorldData;
import am2.guis.ArsMagicaGuiIdList;
import am2.items.ContainerKeystone;
import am2.items.ItemSpellBook;
import am2.items.ItemsCommonProxy;
import am2.playerextensions.AffinityData;
import am2.playerextensions.ExtendedProperties;
import am2.playerextensions.SkillData;
import am2.power.PowerNodeRegistry;
import am2.spell.SpellUtils;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.network.FMLNetworkEvent.ServerCustomPacketEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.server.FMLServerHandler;
import io.netty.buffer.ByteBufInputStream;

public class AMPacketProcessorServer {

    private final PacketRegistry<EntityPlayerMP> packetHandlers = new PacketRegistry<EntityPlayerMP>();

    public AMPacketProcessorServer() {
        registerPacketHandlers();
    }

    private void registerPacketHandlers() {
        packetHandlers.register(
            AMPacketIDs.SPELL_SHAPE_GROUP_CHANGE,
            (player, payload) -> handleCastingModeChange(payload, player));
        packetHandlers.register(AMPacketIDs.SYNCMAPTOSERVER, (player, payload) -> handleSyncMapServer(payload));
        packetHandlers.register(AMPacketIDs.SYNCCOMPENDIUM, (player, payload) -> handleSyncCompendium(payload, player));
        packetHandlers.register(
            AMPacketIDs.SYNCCOMPENDIUMREQUEST,
            (player, payload) -> handleSyncCompendiumRequest(payload, player));
        packetHandlers.register(
            AMPacketIDs.REQUESTWORLDDATACHANGE,
            (player, payload) -> handleRequestWorldDataChange(payload, player));
        packetHandlers
            .register(AMPacketIDs.SYNC_BETA_PARTICLES, (player, payload) -> handleSyncBetaParticles(payload, player));
        packetHandlers.register(
            AMPacketIDs.REQUEST_BETA_PARTICLES,
            (player, payload) -> handleRequestBetaParticles(payload, player));
        packetHandlers
            .register(AMPacketIDs.SPELL_CUSTOMIZE, (player, payload) -> handleSpellCustomize(payload, player));
        packetHandlers.register(
            AMPacketIDs.SPELLBOOK_CHANGE_ACTIVE_SLOT,
            (player, payload) -> handleSpellBookChangeActiveSlot(payload, player));
        packetHandlers
            .register(AMPacketIDs.SYNC_SPELL_KNOWLEDGE, (player, payload) -> handleSyncSpellKnowledge(payload, player));
        packetHandlers
            .register(AMPacketIDs.DECO_BLOCK_UPDATE, (player, payload) -> handleDecoBlockUpdate(payload, player));
        packetHandlers.register(
            AMPacketIDs.INSCRIPTION_TABLE_UPDATE,
            (player, payload) -> handleInscriptionTableUpdate(payload, player));
        packetHandlers
            .register(AMPacketIDs.CASTER_BLOCK_UPDATE, (player, payload) -> handleCasterUpdate(payload, player));
        packetHandlers.register(AMPacketIDs.TK_DISTANCE_SYNC, (player, payload) -> handleTkDistance(payload, player));
        packetHandlers
            .register(AMPacketIDs.SAVE_KEYSTONE_COMBO, (player, payload) -> handleSaveKeystoneCombo(payload, player));
        packetHandlers
            .register(AMPacketIDs.SET_KEYSTONE_COMBO, (player, payload) -> handleSetKeystoneCombo(payload, player));
        packetHandlers.register(
            AMPacketIDs.SET_MAG_WORK_REC,
            (player, payload) -> handleSetMagiciansWorkbenchRecipe(payload, player));
        packetHandlers
            .register(AMPacketIDs.RUNE_BAG_GUI_OPEN, (player, payload) -> handleRuneBagGUIOpen(payload, player));
        packetHandlers
            .register(AMPacketIDs.M_BENCH_LOCK_RECIPE, (player, payload) -> handleMBenchLockRecipe(payload, player));
        packetHandlers.register(AMPacketIDs.IMBUE_ARMOR, (player, payload) -> handleImbueArmor(payload, player));
        packetHandlers
            .register(AMPacketIDs.REQUEST_PWR_PATHS, (player, payload) -> handlePowerPathSync(payload, player));
        packetHandlers
            .register(AMPacketIDs.SYNC_EXTENDED_PROPS, (player, payload) -> handleExpropOperation(payload, player));
        packetHandlers
            .register(AMPacketIDs.AFFINITY_ACTIVATE, (player, payload) -> handleAffinityActivate(payload, player));
    }

    int handlerCount() {
        return packetHandlers.size();
    }

    @SubscribeEvent
    public void onServerPacketData(ServerCustomPacketEvent event) {
        ByteBufInputStream bbis = new ByteBufInputStream(event.packet.payload());
        byte packetID = -1;
        try {
            if (event.packet.getTarget() != Side.SERVER) {
                return;
            }

            // constant details all packets share: ID, player, and remaining data
            PacketEnvelope envelope = PacketEnvelope.read(bbis, Side.SERVER);
            if (envelope == null) {
                LogHelper.warn("Rejected invalid packet envelope");
                return;
            }
            packetID = envelope.id();
            NetHandlerPlayServer srv = (NetHandlerPlayServer) event.packet.handler();
            EntityPlayerMP player = srv.playerEntity;
            byte[] remaining = envelope.payload();

            PacketRegistry.Handler<EntityPlayerMP> handler = packetHandlers.get(packetID);
            if (handler == null) return;
            handler.handle(player, remaining);
        } catch (Throwable t) {
            LogHelper.log(Level.ERROR, t, "Server packet %d failed to handle", packetID);
        } finally {
            try {
                bbis.close();
            } catch (Throwable ignored) {
                // Closing a failed stream must not mask the handler result.
            }
        }
    }

    public static void handleSyncCompendiumRequest(byte[] remaining, EntityPlayerMP player) {
        ExtendedProperties ep = ExtendedProperties.For(player);
        if (ep != null) {
            AMDataWriter writer = new AMDataWriter();
            NBTTagCompound compendium_data = new NBTTagCompound();
            int c = 0;
            for (String o : ep.getAllCompendiumEntries()
                .keySet()) {
                String iValue = ep.getAllCompendiumEntries()
                    .get(o);
                compendium_data.setString("compentry" + c, iValue);
                compendium_data.setString("compentryname" + c, o);
                c++;
            }
            compendium_data.setInteger(
                "compendiumsize",
                ep.getAllCompendiumEntries()
                    .size());
            writer.add(compendium_data);
            AMNetHandler.INSTANCE
                .sendPacketToClientPlayer(player, AMPacketIDs.SYNCCOMPENDIUMRESPONSE, writer.generate());
        } else {
            LogHelper.warn("Attempted to request sync for null properties!");
        }
    }

    private void handleRequestWorldDataChange(byte[] remaining, EntityPlayerMP player) {
        AMDataReader rdr = new AMDataReader(remaining, false);
        int dimension = rdr.getInt();
        String name = rdr.getString();
        String value = rdr.getString();
        if (rdr.hasFailed()) return;
        World world = DimensionManager.getWorld(dimension);
        CustomWorldData.processRequest(world, name, value, player);
    }

    private void handleSyncCompendium(byte[] remaining, EntityPlayerMP entity) {
        ExtendedProperties.For(entity)
            .onSyncCompendiumDataPacketServer(remaining);
    }

    private void handleSyncMapServer(byte[] remaining) {
        AMNetHandler.INSTANCE.sendPacketToAllClients(AMPacketIDs.SYNCMAPTOCLIENTS, remaining);
    }

    private void handleTkDistance(byte[] data, EntityPlayerMP player) {
        AMDataReader rdr = new AMDataReader(data, false);
        float distance = rdr.getFloat();
        if (!rdr.hasFailed()) {
            ExtendedProperties.For(player).TK_Distance = distance;
        }
    }

    private void handleCasterUpdate(byte[] data, EntityPlayerMP player) {
        World world = player.worldObj;
        AMDataReader rdr = new AMDataReader(data, false);
        int x = rdr.getInt();
        int y = rdr.getInt();
        int z = rdr.getInt();
        if (rdr.hasFailed()) return;
        TileEntity te = world.getTileEntity(x, y, z);
        if (!(te instanceof TileEntityBlockCaster) || !PacketAuthority.isWithinReach(player, te)) return;
        AMDataWriter writer = new AMDataWriter();
        writer.add(te.xCoord);
        writer.add(te.yCoord);
        writer.add(te.zCoord);
        writer.add(
            PowerNodeRegistry.For(world)
                .getPower(((TileEntityBlockCaster) te), PowerTypes.LIGHT));
        writer.add(
            PowerNodeRegistry.For(world)
                .getPower(((TileEntityBlockCaster) te), PowerTypes.NEUTRAL));
        writer.add(
            PowerNodeRegistry.For(world)
                .getPower(((TileEntityBlockCaster) te), PowerTypes.DARK));
        AMNetHandler.INSTANCE.sendPacketToClientPlayer(player, AMPacketIDs.CASTER_BLOCK_UPDATE, writer.generate());

    }

    private void handleAffinityActivate(byte[] data, EntityPlayerMP entity) {
        AffinityData.For(entity)
            .onAffinityAbility();
    }

    private void handleExpropOperation(byte[] data, EntityPlayerMP player) {
        AMDataReader rdr = new AMDataReader(data, false);
        int operation = rdr.getInt();
        if (rdr.hasFailed()) return;
        ExtendedProperties.For(player)
            .performRemoteOp(operation);
    }

    private void handlePowerPathSync(byte[] data, EntityPlayerMP player) {
        AMDataReader rdr = new AMDataReader(data, false);
        byte nom = rdr.getByte();
        if (nom == 1) {
            AMVector3 loc = new AMVector3(rdr.getFloat(), rdr.getFloat(), rdr.getFloat());
            if (rdr.hasFailed()) return;
            TileEntity te = player.worldObj.getTileEntity((int) loc.x, (int) loc.y, (int) loc.z);
            if (te instanceof IPowerNode && PacketAuthority.isWithinReach(player, te)) {
                AMNetHandler.INSTANCE.sendPowerResponseToClient(
                    PowerNodeRegistry.For(player.worldObj)
                        .getDataCompoundForNode((IPowerNode) te),
                    player,
                    te);
            }
        }
    }

    private void handleImbueArmor(byte[] data, EntityPlayerMP player) {
        AMDataReader rdr = new AMDataReader(data, false);
        int x = rdr.getInt();
        int y = rdr.getInt();
        int z = rdr.getInt();
        String infusion = rdr.getString();
        if (rdr.hasFailed()) return;
        TileEntity te = player.worldObj.getTileEntity(x, y, z);
        if (te instanceof TileEntityArmorImbuer && PacketAuthority.isWithinReach(player, te)) {
            ((TileEntityArmorImbuer) te).imbueCurrentArmor(infusion);
        }
    }

    private void handleMBenchLockRecipe(byte[] data, EntityPlayerMP player) {
        AMDataReader rdr = new AMDataReader(data, false);
        int x = rdr.getInt();
        int y = rdr.getInt();
        int z = rdr.getInt();
        int recipe = rdr.getInt();
        boolean locked = rdr.getBoolean();
        if (rdr.hasFailed()) return;

        TileEntity te = player.worldObj.getTileEntity(x, y, z);
        if (te instanceof TileEntityMagiciansWorkbench && PacketAuthority.isWithinReach(player, te)) {
            ((TileEntityMagiciansWorkbench) te).setRecipeLocked(recipe, locked);
            te.getWorldObj()
                .markBlockForUpdate(te.xCoord, te.yCoord, te.zCoord);
        }
    }

    private void handleRuneBagGUIOpen(byte[] data, EntityPlayerMP player) {
        AMDataReader rdr = new AMDataReader(data, false);
        int entityID = rdr.getInt();
        if (rdr.hasFailed()) return;

        if (player == null) {
            return;
        }

        // open the GUI
        player.openGui(
            AMCore.instance,
            ArsMagicaGuiIdList.GUI_RUNE_BAG,
            player.worldObj,
            (int) player.posX,
            (int) player.posY,
            (int) player.posZ);
    }

    private void handleSetMagiciansWorkbenchRecipe(byte[] data, EntityPlayerMP player) {
        if (player.openContainer instanceof ContainerMagiciansWorkbench) {
            ContainerMagiciansWorkbench container = (ContainerMagiciansWorkbench) player.openContainer;
            if (!PacketAuthority.isWithinReach(player, container.getWorkbench())) return;
            AMDataReader rdr = new AMDataReader(data, false);
            int index = rdr.getInt();
            if (rdr.hasFailed()) return;
            container.moveRecipeToCraftingGrid(index);
        }
    }

    private void handleSetKeystoneCombo(byte[] data, EntityPlayerMP player) {
        if (player.openContainer instanceof ContainerKeystone) {
            AMDataReader rdr = new AMDataReader(data, false);
            int combination = rdr.getInt();
            if (rdr.hasFailed()) return;
            ((ContainerKeystone) player.openContainer).setInventoryToCombination(combination);
        }
    }

    private void handleSaveKeystoneCombo(byte[] data, EntityPlayerMP player) {
        if (player.openContainer instanceof ContainerKeystone) {
            AMDataReader rdr = new AMDataReader(data, false);
            boolean add = rdr.getBoolean();
            String name = rdr.getString();
            int[] metas = new int[] { rdr.getInt(), rdr.getInt(), rdr.getInt() };
            if (rdr.hasFailed()) return;
            if (add) ItemsCommonProxy.keystone
                .addCombination(((ContainerKeystone) player.openContainer).getKeystoneStack(), name, metas);
            else ItemsCommonProxy.keystone
                .removeCombination(((ContainerKeystone) player.openContainer).getKeystoneStack(), name);
        }
    }

    private void handleInscriptionTableUpdate(byte[] data, EntityPlayerMP player) {
        World world = player.worldObj;
        AMDataReader rdr = new AMDataReader(data, false);
        int x = rdr.getInt();
        int y = rdr.getInt();
        int z = rdr.getInt();
        if (rdr.hasFailed()) return;
        TileEntity te = world.getTileEntity(x, y, z);
        if (!(te instanceof TileEntityInscriptionTable)) return;

        TileEntityInscriptionTable table = (TileEntityInscriptionTable) te;
        if (!PacketAuthority.isUsingInscriptionTable(player, table)) return;

        byte[] payload = rdr.getRemainingBytes();
        if (rdr.hasFailed()) return;
        table.HandleUpdatePacket(payload, player);
        world.markBlockForUpdate(te.xCoord, te.yCoord, te.zCoord);
    }

    private void handleDecoBlockUpdate(byte[] data, EntityPlayerMP player) {
        World world = player.worldObj;
        AMDataReader rdr = new AMDataReader(data, false);
        int x = rdr.getInt();
        int y = rdr.getInt();
        int z = rdr.getInt();
        if (rdr.hasFailed()) return;
        TileEntity te = world.getTileEntity(x, y, z);
        if (!(te instanceof TileEntityParticleEmitter) || !PacketAuthority.isWithinReach(player, te)) return;

        NBTTagCompound compound = rdr.getNBTTagCompound();
        if (rdr.hasFailed() || compound == null) return;
        te.readFromNBT(compound);
        world.markBlockForUpdate(te.xCoord, te.yCoord, te.zCoord);
    }

    private void handleSyncSpellKnowledge(byte[] data, EntityPlayerMP player) {
        SkillData.For(player)
            .handlePacketDataFromClient(data);
    }

    private void handleSpellBookChangeActiveSlot(byte[] data, EntityPlayerMP player) {
        AMDataReader rdr = new AMDataReader(data, false);
        byte subID = rdr.getByte();
        int entityID = rdr.getInt();
        int inventorySlot = rdr.getInt();
        if (rdr.hasFailed()) return;
        if (inventorySlot < 0 || inventorySlot >= player.inventory.getSizeInventory()) return;

        ItemStack stack = player.inventory.getStackInSlot(inventorySlot);
        if (stack == null || !(stack.getItem() instanceof ItemSpellBook)) return;

        int newIndex = 0;

        if (subID == ItemSpellBook.ID_NEXT_SPELL) newIndex = ItemsCommonProxy.spellBook.SetNextSlot(stack);
        else if (subID == ItemSpellBook.ID_PREV_SPELL) newIndex = ItemsCommonProxy.spellBook.SetPrevSlot(stack);
    }

    private void handleSpellCustomize(byte[] data, EntityPlayerMP player) {
        AMDataReader rdr = new AMDataReader(data, false);
        int entityID = rdr.getInt();
        if (!PacketAuthority.isSelf(player, entityID)) return;

        int IIconIndex = rdr.getInt();
        String name = rdr.getString();
        if (rdr.hasFailed()) return;

        if (player.openContainer instanceof ContainerSpellCustomization) {
            ((ContainerSpellCustomization) player.openContainer).setNameAndIndex(name, IIconIndex);
        }
    }

    private void handleRequestBetaParticles(byte[] data, EntityPlayerMP player) {
        AMDataReader rdr = new AMDataReader(data, false);
        int requesterID = rdr.getInt();
        int entityID = rdr.getInt();
        if (rdr.hasFailed()) return;
        if (!PacketAuthority.isSelf(player, requesterID)) return;
        EntityLivingBase entity = getEntityByID(entityID);

        if (player == null || !(entity instanceof EntityPlayer)) return;

        if (!PlayerTracker.hasAA((EntityPlayer) entity)) return;

        byte[] expropData = ExtendedProperties.For(entity)
            .getAuraData();

        AMDataWriter writer = new AMDataWriter();
        writer.add(entity.getEntityId());
        writer.add(expropData);

        AMNetHandler.INSTANCE.sendPacketToClientPlayer(player, AMPacketIDs.REQUEST_BETA_PARTICLES, writer.generate());
    }

    private void handleSyncBetaParticles(byte[] data, EntityPlayerMP player) {
        AMDataReader rdr = new AMDataReader(data, false);

        if (!PlayerTracker.hasAA(player)) {
            return;
        }

        int index = rdr.getInt();
        int behaviour = rdr.getInt();
        float scale = rdr.getFloat();
        float alpha = rdr.getFloat();
        boolean randomColor = rdr.getBoolean();
        boolean defaultColor = rdr.getBoolean();
        int color = rdr.getInt();
        int delay = rdr.getInt();
        int quantity = rdr.getInt();
        float speed = rdr.getFloat();
        if (rdr.hasFailed()) return;

        ExtendedProperties.For(player)
            .updateAuraData(index, behaviour, scale, alpha, randomColor, defaultColor, color, delay, quantity, speed);
    }

    private void handleCastingModeChange(byte[] data, EntityPlayerMP player) {
        AMDataReader rdr = new AMDataReader(data, false);
        int newShapeGroupOrdinal = rdr.getInt();
        int index = rdr.getInt();
        if (rdr.hasFailed()) return;

        ItemStack stack = player.getCurrentEquippedItem();
        if (stack != null) {
            if (stack.getItem() == ItemsCommonProxy.spell) {
                SpellUtils.instance.setShapeGroup(stack, newShapeGroupOrdinal);
            } else if (stack.getItem() == ItemsCommonProxy.spellBook
                || stack.getItem() == ItemsCommonProxy.arcaneSpellbook) {
                    ItemStack spellStack = ((ItemSpellBook) stack.getItem()).GetActiveItemStack(stack);
                    SpellUtils.instance.setShapeGroup(spellStack, newShapeGroupOrdinal);
                    ((ItemSpellBook) stack.getItem()).replaceAciveItemStack(stack, spellStack);
                }
        }
    }

    public WorldServer[] getWorldServers() {
        return FMLServerHandler.instance()
            .getServer().worldServers;
    }

    public EntityLivingBase getEntityByID(int entityID) {
        return AMCore.proxy.getEntityByID(entityID);
    }
}
