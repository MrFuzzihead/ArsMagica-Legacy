package am2.interop;

import codechicken.nei.api.API;
import codechicken.nei.api.IConfigureNEI;

/**
 * NEI discovers configuration classes by the {@code NEI*Config} naming convention, so this class must not be renamed
 * without an explicit registration path.
 */
public class NEIAM2Config implements IConfigureNEI {

    @Override
    public String getName() {
        return "Ars Magica 2.5";
    }

    @Override
    public String getVersion() {
        return "1.4.1";
    }

    @Override
    public void loadConfig() {
        API.registerRecipeHandler(new RecipeHandlerEssence());
        API.registerUsageHandler(new RecipeHandlerEssence());
        API.registerRecipeHandler(new RecipeHandlerEnervator());
        API.registerUsageHandler(new RecipeHandlerEnervator());
    }

}
