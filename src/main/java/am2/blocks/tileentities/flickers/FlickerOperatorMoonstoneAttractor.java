package am2.blocks.tileentities.flickers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import am2.api.flickers.IFlickerController;
import am2.api.flickers.IFlickerFunctionality;
import am2.api.math.AMVector3;
import am2.api.spell.enums.Affinity;
import am2.blocks.BlocksCommonProxy;
import am2.items.ItemsCommonProxy;

public class FlickerOperatorMoonstoneAttractor implements IFlickerFunctionality {

    private static final Map<Integer, List<AMVector3>> attractors = new HashMap<Integer, List<AMVector3>>();

    public static AMVector3 getMeteorAttractor(int dimension, AMVector3 target) {
        List<AMVector3> dimensionAttractors = attractors.get(dimension);
        if (dimensionAttractors == null) {
            return null;
        }
        for (AMVector3 attractor : dimensionAttractors.toArray(new AMVector3[dimensionAttractors.size()])) {
            if (attractor.distanceSqTo(target) <= 16384) return attractor.copy();
        }
        return null;
    }

    private static List<AMVector3> listFor(int dimension) {
        List<AMVector3> list = attractors.get(dimension);
        if (list == null) {
            list = new ArrayList<AMVector3>();
            attractors.put(dimension, list);
        }
        return list;
    }

    /** Package-private test/reset hook. */
    static void clearAttractors() {
        attractors.clear();
    }

    @Override
    public boolean RequiresPower() {
        return true;
    }

    @Override
    public int PowerPerOperation() {
        return 10;
    }

    @Override
    public boolean DoOperation(World worldObj, IFlickerController habitat, boolean powered) {
        AMVector3 vec = new AMVector3((TileEntity) habitat);
        int dimension = worldObj.provider.dimensionId;
        if (powered) {
            List<AMVector3> list = listFor(dimension);
            if (!list.contains(vec)) {
                list.add(vec);
            }
            return true;
        } else {
            listFor(dimension).remove(vec);
        }
        return false;
    }

    @Override
    public boolean DoOperation(World worldObj, IFlickerController habitat, boolean powered, Affinity[] flickers) {
        return DoOperation(worldObj, habitat, powered);
    }

    @Override
    public void RemoveOperator(World worldObj, IFlickerController habitat, boolean powered) {
        AMVector3 vec = new AMVector3((TileEntity) habitat);
        listFor(worldObj.provider.dimensionId).remove(vec);
    }

    @Override
    public int TimeBetweenOperation(boolean powered, Affinity[] flickers) {
        return 100;
    }

    @Override
    public void RemoveOperator(World worldObj, IFlickerController habitat, boolean powered, Affinity[] flickers) {
        RemoveOperator(worldObj, habitat, powered);
    }

    @Override
    public Object[] getRecipe() {
        return new Object[] { "RLR", "AME", "I T", Character.valueOf('R'),
            new ItemStack(ItemsCommonProxy.rune, 1, ItemsCommonProxy.rune.META_ORANGE), Character.valueOf('L'),
            new ItemStack(ItemsCommonProxy.flickerJar, 1, Affinity.LIGHTNING.ordinal()), Character.valueOf('A'),
            new ItemStack(ItemsCommonProxy.flickerJar, 1, Affinity.ARCANE.ordinal()), Character.valueOf('E'),
            new ItemStack(ItemsCommonProxy.flickerJar, 1, Affinity.EARTH.ordinal()), Character.valueOf('M'),
            new ItemStack(BlocksCommonProxy.AMOres, 1, BlocksCommonProxy.AMOres.META_MOONSTONE_BLOCK),
            Character.valueOf('I'), new ItemStack(ItemsCommonProxy.essence, 1, ItemsCommonProxy.essence.META_AIR),
            Character.valueOf('T'), new ItemStack(ItemsCommonProxy.essence, 1, ItemsCommonProxy.essence.META_EARTH) };
    }

}
