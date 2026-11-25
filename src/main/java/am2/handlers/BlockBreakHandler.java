package am2.handlers;

import java.util.Random;

import net.minecraftforge.event.world.BlockEvent;

import am2.AMEventHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;

/** Blocks breaking protected force-shielded positions. */
public class BlockBreakHandler {

    private final Random rand = new Random();

    @SubscribeEvent
    public void onBlockBreak(BlockEvent.BreakEvent event) {
        if (AMEventHandler.forceShielded
            .contains(event.x + "_" + event.y + "_" + event.z + "_" + event.world.provider.dimensionId)) {
            event.setCanceled(true);
            event.world
                .playSoundAtEntity(event.getPlayer(), "arsmagica2:spell.cast.arcane", 1F, rand.nextFloat() + 0.5f);
            event.world.spawnParticle("depthsuspend", event.x + 0.5, event.y + 0.5, event.z + 0.5, 0, 0, 0);
            event.world.spawnParticle(
                "depthsuspend",
                event.x + rand.nextFloat(),
                event.y + rand.nextFloat(),
                event.z + rand.nextFloat(),
                0,
                0,
                0);
            event.world.spawnParticle(
                "depthsuspend",
                event.x + rand.nextFloat(),
                event.y + rand.nextFloat(),
                event.z + rand.nextFloat(),
                0,
                0,
                0);
        }
    }
}
