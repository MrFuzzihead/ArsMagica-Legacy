package am2.lore;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import am2.guis.GuiArcaneCompendium;
import am2.multiblock.IMultiblockStructureController;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class CompendiumEntryStructure extends CompendiumEntry {

    private String controllerClass;

    public CompendiumEntryStructure() {
        super(CompendiumEntryTypes.instance.STRUCTURE);
    }

    @Override
    protected void parseEx(Node node) {
        NodeList childNodes = node.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); ++i) {
            Node child = childNodes.item(i);
            if (child.getNodeName()
                .equals("controller")) {
                this.controllerClass = child.getTextContent();
            }
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    protected GuiArcaneCompendium getCompendiumGui(String searchID, int meta) {
        if (this.controllerClass != null) {
            NBTTagCompound tag = new NBTTagCompound();
            tag.setString("id", controllerClass);
            TileEntity te = TileEntity.createAndLoadEntity(tag);
            if (te instanceof IMultiblockStructureController) {
                return new GuiArcaneCompendium(((IMultiblockStructureController) te).getDefinition(), te);
            }
        }
        return new GuiArcaneCompendium(searchID);
    }

    @Override
    public ItemStack getRepresentItemStack(String searchID, int meta) {
        return null;
    }

}
