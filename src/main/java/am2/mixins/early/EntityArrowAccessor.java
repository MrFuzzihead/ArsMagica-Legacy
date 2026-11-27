package am2.mixins.early;

import net.minecraft.block.Block;
import net.minecraft.entity.projectile.EntityArrow;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Accessors for the private {@code EntityArrow} state used by {@code EntityBoundArrow}. Replaces the exact-field
 * access transformer so the mod ships with no ATs.
 */
@Mixin(EntityArrow.class)
public interface EntityArrowAccessor {

    @Accessor("field_145791_d")
    int am2$getXTile();

    @Accessor("field_145791_d")
    void am2$setXTile(int value);

    @Accessor("field_145792_e")
    int am2$getYTile();

    @Accessor("field_145792_e")
    void am2$setYTile(int value);

    @Accessor("field_145789_f")
    int am2$getZTile();

    @Accessor("field_145789_f")
    void am2$setZTile(int value);

    @Accessor("field_145790_g")
    Block am2$getInTile();

    @Accessor("field_145790_g")
    void am2$setInTile(Block value);

    @Accessor("inData")
    int am2$getInData();

    @Accessor("inData")
    void am2$setInData(int value);

    @Accessor("inGround")
    boolean am2$isInGround();

    @Accessor("inGround")
    void am2$setInGround(boolean value);

    @Accessor("ticksInGround")
    int am2$getTicksInGround();

    @Accessor("ticksInGround")
    void am2$setTicksInGround(int value);

    @Accessor("ticksInAir")
    int am2$getTicksInAir();

    @Accessor("ticksInAir")
    void am2$setTicksInAir(int value);

    @Accessor("damage")
    double am2$getDamage();

    @Accessor("damage")
    void am2$setDamage(double value);

    @Accessor("knockbackStrength")
    int am2$getKnockbackStrength();

    @Accessor("knockbackStrength")
    void am2$setKnockbackStrength(int value);
}
