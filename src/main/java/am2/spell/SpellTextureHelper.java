package am2.spell;

import java.io.File;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.init.Items;
import net.minecraft.util.IIcon;

import am2.LogHelper;
import am2.texture.ResourceManager;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class SpellTextureHelper {

    public static final SpellTextureHelper instance = new SpellTextureHelper();

    private IIcon[] icons;
    private static final String iconsPath = "/assets/arsmagica2/textures/items/spells/icons/";
    private static final String iconsPrefix = "/spells/icons/";

    private SpellTextureHelper() {}

    public void loadAllIcons(IIconRegister register) {
        List<String> resources;
        try {
            resources = getResourceListing();
            if (resources.isEmpty()) {
                LogHelper.error("No spell IIcons found?!?");
            } else {
                LogHelper.info("Located %d spell IIcons", resources.size());
            }
            icons = new IIcon[resources.size()];
            int count = 0;
            for (String s : resources) {
                icons[count++] = ResourceManager.RegisterTexture(s, register);
            }
        } catch (Exception e) {
            if (icons == null) icons = new IIcon[0];
            LogHelper.error(e, "Failed to register spell icons");
        }

    }

    public static List<String> getResourceListing() throws IOException, URISyntaxException {
        ArrayList<String> toReturn = new ArrayList<String>();
        URL resource = SpellTextureHelper.class.getResource(iconsPath);
        if (resource == null) return toReturn;

        if ("file".equals(resource.getProtocol())) {
            File icons = new File(resource.toURI());
            File[] files = icons.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile() && file.getName()
                        .endsWith(".png")) {
                        toReturn.add(
                            iconsPrefix + file.getName()
                                .replace(".png", ""));
                    }
                }
            }
        } else if ("jar".equals(resource.getProtocol())) {
            JarURLConnection connection = (JarURLConnection) resource.openConnection();
            String prefix = iconsPath.substring(1);
            try (JarFile jarFile = connection.getJarFile()) {
                Enumeration<JarEntry> entries = jarFile.entries();
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    if (entry.getName()
                        .startsWith(prefix)) {
                        String name = entry.getName()
                            .substring(prefix.length());
                        if (name.isEmpty() || !name.endsWith(".png")) continue;
                        toReturn.add(iconsPrefix + name.replace(".png", ""));
                    }
                }
            }
        }
        toReturn.sort(String.CASE_INSENSITIVE_ORDER);
        return toReturn;
    }

    public IIcon getIcon(int index) {
        if (icons.length == 0) return Items.diamond_sword.getIconFromDamage(0);
        if (index < 0 || index >= icons.length) index = 0;
        return icons[index];
    }

    public IIcon[] getAllIcons() {
        return icons;
    }
}
