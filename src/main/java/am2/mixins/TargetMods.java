package am2.mixins;

import javax.annotation.Nonnull;

import com.gtnewhorizon.gtnhmixins.builders.ITargetMod;
import com.gtnewhorizon.gtnhmixins.builders.TargetModBuilder;

/** Mods whose presence changes whether one of AM2's compatibility mixins should load. */
public enum TargetMods implements ITargetMod {

    ENDLESS_IDS("com.falsepattern.endlessids.asm.EndlessIDsCore", "endlessids");

    private final TargetModBuilder builder;

    TargetMods(String coreModClass, String modId) {
        this.builder = new TargetModBuilder().setCoreModClass(coreModClass)
            .setModId(modId);
    }

    @Nonnull
    @Override
    public TargetModBuilder getBuilder() {
        return builder;
    }
}
