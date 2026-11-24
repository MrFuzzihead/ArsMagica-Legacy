package am2.mixins.early;

import java.util.Map;

import net.minecraft.tileentity.TileEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Exposes the vanilla tile-entity name registry used by {@code CompendiumEntryStructure} to map a compendium
 * controller name to its tile-entity class. The map is private static and has no public accessor.
 */
@Mixin(TileEntity.class)
public interface TileEntityAccessor {

    @Accessor("nameToClassMap")
    static Map<String, Class<? extends TileEntity>> am2$getNameToClassMap() {
        throw new AssertionError();
    }
}
