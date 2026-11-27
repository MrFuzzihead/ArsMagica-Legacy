package am2.mixins.early;

import net.minecraft.block.Block;
import net.minecraft.entity.projectile.EntityFishHook;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Accessors for the private {@code EntityFishHook} state used by {@code EntityFishHookArcane} and
 * {@code EntityFishHookInfernal}. Replaces the exact-field access transformer so the mod ships with no ATs.
 */
@Mixin(EntityFishHook.class)
public interface EntityFishHookAccessor {

    @Accessor("field_146037_g")
    int am2$getX();

    @Accessor("field_146037_g")
    void am2$setX(int value);

    @Accessor("field_146048_h")
    int am2$getY();

    @Accessor("field_146048_h")
    void am2$setY(int value);

    @Accessor("field_146050_i")
    int am2$getZ();

    @Accessor("field_146050_i")
    void am2$setZ(int value);

    @Accessor("field_146046_j")
    Block am2$getInTile();

    @Accessor("field_146046_j")
    void am2$setInTile(Block value);

    @Accessor("field_146051_au")
    boolean am2$isInGround();

    @Accessor("field_146051_au")
    void am2$setInGround(boolean value);

    @Accessor("field_146049_av")
    int am2$getTicksInGround();

    @Accessor("field_146049_av")
    void am2$setTicksInGround(int value);

    @Accessor("field_146047_aw")
    int am2$getTicksInAir();

    @Accessor("field_146047_aw")
    void am2$setTicksInAir(int value);

    @Accessor("field_146045_ax")
    int am2$getTicksCatchable();

    @Accessor("field_146045_ax")
    void am2$setTicksCatchable(int value);

    @Accessor("field_146040_ay")
    int am2$getTicksCaughtDelay();

    @Accessor("field_146040_ay")
    void am2$setTicksCaughtDelay(int value);

    @Accessor("field_146038_az")
    int am2$getTicksCatchableDelay();

    @Accessor("field_146038_az")
    void am2$setTicksCatchableDelay(int value);

    @Accessor("field_146054_aA")
    float am2$getFishApproachAngle();

    @Accessor("field_146054_aA")
    void am2$setFishApproachAngle(float value);

    @Accessor("field_146055_aB")
    int am2$getCatchAnimationTicks();

    @Accessor("field_146055_aB")
    void am2$setCatchAnimationTicks(int value);

    @Accessor("field_146056_aC")
    double am2$getCaughtEntityX();

    @Accessor("field_146056_aC")
    void am2$setCaughtEntityX(double value);

    @Accessor("field_146057_aD")
    double am2$getCaughtEntityY();

    @Accessor("field_146057_aD")
    void am2$setCaughtEntityY(double value);

    @Accessor("field_146058_aE")
    double am2$getCaughtEntityZ();

    @Accessor("field_146058_aE")
    void am2$setCaughtEntityZ(double value);

    @Accessor("field_146059_aF")
    double am2$getCaughtEntityYaw();

    @Accessor("field_146059_aF")
    void am2$setCaughtEntityYaw(double value);

    @Accessor("field_146060_aG")
    double am2$getCaughtEntityPitch();

    @Accessor("field_146060_aG")
    void am2$setCaughtEntityPitch(double value);
}
