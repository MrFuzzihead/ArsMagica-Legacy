package am2.crafting;

import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.world.World;

import am2.armor.BoundArmor;
import am2.items.ItemSpellStaff;
import am2.items.SpellBase;

/**
 * The dynamic staff/spell/armor crafting combinations that used to be implemented by a global
 * {@code CraftingManager.findMatchingRecipe} patch.
 *
 * <p>
 * There are three shapes:
 * </p>
 * <ul>
 * <li>one non-magitech staff + one spell scroll: put the scroll's spell on the staff;</li>
 * <li>two non-magitech staffs: copy the charge from the second staff to the first;</li>
 * <li>one bound armor piece + one spell scroll: put the scroll's spell on the armor.</li>
 * </ul>
 *
 * <p>
 * The scan order matches the old patch (column-major over the 3x3 grid), so with two staffs the first one found is
 * the target and the second is the charge source.
 * </p>
 */
public final class SpellCraftingRecipe implements IRecipe {

    public static final SpellCraftingRecipe INSTANCE = new SpellCraftingRecipe();

    enum Type {
        NONE,
        STAFF_CHARGE,
        STAFF_SCROLL,
        ARMOR_SCROLL
    }

    static final class Scan {

        Type type = Type.NONE;
        ItemStack firstStaff;
        ItemStack secondStaff;
        ItemStack spell;
        ItemStack armor;
    }

    private SpellCraftingRecipe() {}

    @Override
    public boolean matches(InventoryCrafting grid, World world) {
        return scan(grid).type != Type.NONE;
    }

    @Override
    public ItemStack getCraftingResult(InventoryCrafting grid) {
        Scan scan = scan(grid);
        try {
            switch (scan.type) {
                case STAFF_SCROLL:
                    return scan.firstStaff != null && scan.spell != null
                        ? ItemSpellStaff.setSpellScroll(scan.firstStaff.copy(), scan.spell)
                        : null;
                case STAFF_CHARGE:
                    return scan.firstStaff != null && scan.secondStaff != null
                        ? ItemSpellStaff.copyChargeFrom(scan.firstStaff.copy(), scan.secondStaff)
                        : null;
                case ARMOR_SCROLL:
                    return scan.armor != null && scan.spell != null ? BoundArmor.setSpell(scan.armor.copy(), scan.spell)
                        : null;
                default:
                    return null;
            }
        } catch (RuntimeException e) {
            // A corrupt NBT tag should reject the craft, not crash the crafting grid.
            return null;
        }
    }

    @Override
    public int getRecipeSize() {
        return 9;
    }

    @Override
    public ItemStack getRecipeOutput() {
        // The real output depends on the inputs (which staff, which scroll). CraftingManager.findMatchingRecipe only
        // calls matches()/getCraftingResult(), so a static representative would be misleading.
        return null;
    }

    /**
     * Classifies the grid using the same column-major order as the old patch.
     */
    static Scan scan(InventoryCrafting grid) {
        Scan scan = new Scan();
        if (grid == null) {
            return scan;
        }

        int staffs = 0;
        int spells = 0;
        int armors = 0;

        for (int column = 0; column < 3; column++) {
            for (int row = 0; row < 3; row++) {
                ItemStack stack = grid.getStackInRowAndColumn(column, row);
                if (stack == null || stack.getItem() == null) {
                    continue;
                }

                if (stack.getItem() instanceof ItemSpellStaff) {
                    if (!((ItemSpellStaff) stack.getItem()).isMagiTechStaff()) {
                        if (staffs == 0) {
                            scan.firstStaff = stack;
                        } else if (staffs == 1) {
                            scan.secondStaff = stack;
                        }
                        staffs++;
                    }
                } else if (stack.getItem() instanceof SpellBase) {
                    spells++;
                    scan.spell = stack;
                } else if (stack.getItem() instanceof BoundArmor) {
                    armors++;
                    scan.armor = stack;
                }
            }
        }

        if (spells == 1 && staffs == 1) {
            scan.type = Type.STAFF_SCROLL;
        } else if (staffs == 2) {
            scan.type = Type.STAFF_CHARGE;
        } else if (spells == 1 && armors == 1) {
            scan.type = Type.ARMOR_SCROLL;
        }
        return scan;
    }
}
