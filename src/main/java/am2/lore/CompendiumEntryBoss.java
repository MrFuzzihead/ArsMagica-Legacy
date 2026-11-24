package am2.lore;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.item.ItemStack;

import org.w3c.dom.Node;

import am2.guis.GuiArcaneCompendium;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class CompendiumEntryBoss extends CompendiumEntry {

    public CompendiumEntryBoss() {
        super(CompendiumEntryTypes.instance.BOSS);
    }

    @Override
    protected void parseEx(Node node) {}

    @Override
    @SideOnly(Side.CLIENT)
    protected GuiArcaneCompendium getCompendiumGui(String searchID, int meta) {
        String modEntityID = "arsmagica2." + searchID;
        Entity entity = EntityList.createEntityByName(modEntityID, Minecraft.getMinecraft().theWorld);
        if (entity != null) {
            return new GuiArcaneCompendium(entity);
        }
        return new GuiArcaneCompendium(searchID);
    }

    @Override
    public ItemStack getRepresentItemStack(String searchID, int meta) {
        return null;
    }

}
