package am2.lore;

import java.util.function.Supplier;

import net.minecraft.util.IIcon;

import org.w3c.dom.Node;

public class CompendiumEntryType {

    private String categoryName;
    private String nodeName;
    private String categoryLabel;
    private int order;
    private IIcon representItem;
    private final Supplier<CompendiumEntry> compendiumFactory;

    public CompendiumEntryType(String categoryName, String nodeName, String categoryLabel, int order,
        Supplier<CompendiumEntry> compendiumFactory) {
        this.categoryName = categoryName;
        this.nodeName = nodeName;
        this.compendiumFactory = compendiumFactory;
        this.categoryLabel = categoryLabel;
        this.order = order;
    }

    public void setRepresentIcon(IIcon IIcon) {
        this.representItem = IIcon;
    }

    public String getCategoryName() {
        return this.categoryName;
    }

    public String getNodeName() {
        return this.nodeName;
    }

    public CompendiumEntry createCompendiumEntry(Node node) {
        CompendiumEntry ce = createBlank();
        if (ce == null) {
            return null;
        }
        ce.parse(node);
        return ce;
    }

    CompendiumEntry createBlank() {
        return compendiumFactory.get();
    }

    public String getCategoryLabel() {
        return this.categoryLabel;
    }

    public IIcon getRepresentItem() {
        return representItem;
    }

}
