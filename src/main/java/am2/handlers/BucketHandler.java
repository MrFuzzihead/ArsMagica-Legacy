package am2.handlers;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.player.FillBucketEvent;

import am2.blocks.BlocksCommonProxy;
import am2.items.ItemsCommonProxy;
import cpw.mods.fml.common.eventhandler.Event.Result;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;

/** Handles filling AM2's custom bucket from a liquid-essence source. */
public class BucketHandler {

    @SubscribeEvent
    public void onBucketFill(FillBucketEvent event) {
        ItemStack result = attemptFill(event.world, event.target);

        if (result != null) {
            event.result = result;
            event.setResult(Result.ALLOW);
        }
    }

    public static ItemStack attemptFill(World world, MovingObjectPosition target) {
        Block block = world.getBlock(target.blockX, target.blockY, target.blockZ);

        if (block == BlocksCommonProxy.liquidEssence) {
            if (world.getBlockMetadata(target.blockX, target.blockY, target.blockZ) == 0) {
                world.setBlock(target.blockX, target.blockY, target.blockZ, Blocks.air);
                return new ItemStack(ItemsCommonProxy.itemAMBucket);
            }
        }

        return null;
    }
}
