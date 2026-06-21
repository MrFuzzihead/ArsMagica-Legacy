package am2.items;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import am2.api.items.ICompendium;
import am2.guis.AMGuiHelper;
import am2.guis.GuiArcaneCompendium;
import am2.texture.ResourceManager;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class ItemArcaneCompendium extends ArsMagicaItem {

    public ItemArcaneCompendium() {
        super();
    }

    @Override
    public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
        if (world.isRemote) {
            AMGuiHelper.OpenCompendiumGui(stack);
        }
        return stack;
    }

    @Override
    public boolean onItemUse(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side,
        float subX, float subY, float subZ) {
        if (player.isSneaking() && world.isRemote) {
            Block block = world.getBlock(x, y, z);
            if (block instanceof ICompendium) {
                openCompendiumBlock(block);
            }
        }
        return false;
    }

    @SideOnly(Side.CLIENT)
    private void openCompendiumBlock(Block block) {
        Minecraft.getMinecraft()
            .displayGuiScreen(new GuiArcaneCompendium(block));
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerIcons(IIconRegister par1IconRegister) {
        this.itemIcon = ResourceManager.RegisterTexture("arcanecompendium", par1IconRegister);
    }
}
