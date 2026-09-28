package am2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.entity.item.EntityItemFrame;
import net.minecraft.init.Items;
import net.minecraft.item.ItemBook;
import net.minecraft.item.ItemStack;

import am2.blocks.BlocksCommonProxy;
import am2.items.ItemsCommonProxy;
import am2.particles.AMParticle;
import am2.particles.ParticleArcToEntity;
import am2.particles.ParticleColorShift;
import am2.particles.ParticleHoldPosition;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Watches item frames that hold a book and turns them into Arcane Compendiums once they have been
 * surrounded by essence long enough.
 *
 * <p>
 * A single instance lives on {@link CommonProxy}, so in an integrated server this class is ticked
 * by both the client thread and the server thread. Two things follow from that:
 *
 * <ul>
 * <li>All mutable state is guarded by {@link #lock}, and {@link #watchedFrames} is only ever
 * iterated over a snapshot. Mutating a {@link HashMap} while iterating its {@code keySet} -
 * which the old code did - throws {@link java.util.ConcurrentModificationException}.
 * <li>Each side only ever touches frames that live in a world it owns. Previously the client
 * thread walked server-side frames and read blocks out of the server's
 * {@code ChunkProviderServer}, which hodgepodge reports as
 * "Off-thread read from Client thread - serving from snapshot".
 * </ul>
 */
public class ItemFrameWatcher {

    private final Map<EntityItemFrameComparator, Integer> watchedFrames;
    private final List<EntityItemFrameComparator> queuedAddFrames;
    private final List<EntityItemFrameComparator> queuedRemoveFrames;

    /**
     * Guards all three collections. The integrated server and the client share one watcher, so the
     * client thread and the server thread both enter these methods.
     */
    private final Object lock = new Object();

    private static final int processTime = 800;

    public ItemFrameWatcher() {
        watchedFrames = new HashMap<EntityItemFrameComparator, Integer>();
        queuedAddFrames = new ArrayList<EntityItemFrameComparator>();
        queuedRemoveFrames = new ArrayList<EntityItemFrameComparator>();
    }

    /**
     * @param remote {@code true} when called from the client thread, {@code false} from the server
     *               thread. Frames belonging to the other side are skipped entirely, so neither side
     *               reaches into a world it does not own.
     */
    public void checkWatchedFrames(boolean remote) {
        synchronized (lock) {
            ArrayList<EntityItemFrameComparator> toRemove = new ArrayList<EntityItemFrameComparator>();

            updateQueuedChanges(remote);

            // Iterate a snapshot: checkFrameRadius writes the counter back into watchedFrames, and
            // doing that during a keySet() walk is what used to throw CME.
            for (EntityItemFrameComparator frameComp : new ArrayList<EntityItemFrameComparator>(
                watchedFrames.keySet())) {

                Integer time = watchedFrames.get(frameComp);
                if (time == null) time = 0;

                if (frameComp.frame == null || frameComp.frame.worldObj == null) continue;

                if (frameComp.frame.worldObj.isRemote != remote) continue;

                // A frame stops being watched once it stops holding a book, once the server has
                // turned it into a compendium, or once it has run out of time to do so. The client
                // keeps watching a little longer so it can play the completion particles.
                boolean shouldRemove = !remote || time >= processTime;

                if (frameIsValid(frameComp.frame)) {
                    if (!checkFrameRadius(frameComp, remote)) {
                        shouldRemove = false;
                    }
                } else {
                    watchedFrames.put(frameComp, time + 1);
                }

                if (shouldRemove) {
                    toRemove.add(frameComp);
                }
            }

            for (EntityItemFrameComparator frame : toRemove) {
                stopWatchingFrame(frame.frame);
            }
        }
    }

    private boolean checkFrameRadius(EntityItemFrameComparator frameComp, boolean remote) {

        int radius = 2;

        boolean shouldRemove = true;

        EntityItemFrame frame = frameComp.frame;

        List<Block> targetBlock = new ArrayList<Block>();

        if (AMCore.config.isAlternativeStart()) {
            targetBlock.add(BlocksCommonProxy.witchwoodLeaves);
            targetBlock.add(BlocksCommonProxy.witchwoodLog);
        } else {
            targetBlock.add(BlocksCommonProxy.liquidEssence);
        }

        for (int i = -radius; i <= radius; ++i) {
            for (int j = -radius; j <= radius; ++j) {
                for (int k = -radius; k <= radius; ++k) {

                    if (targetBlock.contains(
                        frame.worldObj.getBlock((int) frame.posX + i, (int) frame.posY + j, (int) frame.posZ + k))) {

                        Integer time = watchedFrames.get(frameComp);
                        if (time == null) {
                            time = 0;
                        }
                        time++;

                        watchedFrames.put(frameComp, time);

                        if (time >= processTime) {
                            if (!remote) {
                                // Only the server may change the contents of a frame, otherwise the
                                // change is never replicated to other clients.
                                frame.setDisplayedItem(new ItemStack(ItemsCommonProxy.arcaneCompendium));
                                return true;
                            }
                        } else {
                            shouldRemove = false;
                            if (remote) {
                                spawnCompendiumProgressParticles(
                                    frame,
                                    (int) frame.posX + i,
                                    (int) frame.posY + j,
                                    (int) frame.posZ + k);
                            }
                        }
                    }

                }
            }
        }

        return shouldRemove;
    }

    private boolean frameIsValid(EntityItemFrame frame) {
        return frame != null && !frame.isDead
            && frame.getDisplayedItem() != null
            && frame.getDisplayedItem()
                .getItem() instanceof ItemBook;
    }

    private void updateQueuedChanges(boolean remote) {
        // Only this side's entries are consumed. Anything the other thread queued stays queued, so
        // the queues are never cleared wholesale.
        for (Iterator<EntityItemFrameComparator> it = queuedAddFrames.iterator(); it.hasNext();) {
            EntityItemFrameComparator comp = it.next();
            if (!isOwnedBy(comp, remote)) continue;
            it.remove();

            if (comp.frame.getDisplayedItem() == null || comp.frame.getDisplayedItem()
                .getItem() != ItemsCommonProxy.arcaneCompendium) watchedFrames.put(comp, 0);
        }

        for (Iterator<EntityItemFrameComparator> it = queuedRemoveFrames.iterator(); it.hasNext();) {
            EntityItemFrameComparator comp = it.next();
            if (!isOwnedBy(comp, remote)) continue;
            it.remove();

            Integer time = watchedFrames.get(comp);
            if (time != null && time >= processTime
                && !comp.frame.isDead
                && (comp.frame.getDisplayedItem() != null && (comp.frame.getDisplayedItem()
                    .getItem() == Items.book
                    || comp.frame.getDisplayedItem()
                        .getItem() == ItemsCommonProxy.arcaneCompendium))) {
                spawnCompendiumCompleteParticles(comp.frame);
            }
            watchedFrames.remove(comp);
        }
    }

    private static boolean isOwnedBy(EntityItemFrameComparator comp, boolean remote) {
        return comp != null && comp.frame != null
            && comp.frame.worldObj != null
            && comp.frame.worldObj.isRemote == remote;
    }

    public void startWatchingFrame(EntityItemFrame frame) {
        if (frame == null || frame.worldObj == null) return;
        synchronized (lock) {
            queuedAddFrames.add(new EntityItemFrameComparator(frame));
        }
    }

    public void stopWatchingFrame(EntityItemFrame frame) {
        if (frame == null || frame.worldObj == null) return;
        synchronized (lock) {
            queuedRemoveFrames.add(new EntityItemFrameComparator(frame));
        }
    }

    @SideOnly(Side.CLIENT)
    public void spawnCompendiumProgressParticles(EntityItemFrame frame, int x, int y, int z) {
        AMParticle particle = (AMParticle) AMCore.proxy.particleManager
            .spawn(frame.worldObj, "symbols", x + 0.5, y + 0.5, z + 0.5);
        if (particle != null) {
            particle.setIgnoreMaxAge(true);
            // particle.AddParticleController(new ParticleApproachEntity(particle, frame, 0.02f, 0.04f, 1,
            // false).setKillParticleOnFinish(true));
            particle.AddParticleController(
                new ParticleArcToEntity(particle, 1, frame, false).SetSpeed(0.02f)
                    .setKillParticleOnFinish(true));
            particle.setRandomScale(0.05f, 0.12f);
        }
    }

    @SideOnly(Side.CLIENT)
    public void spawnCompendiumCompleteParticles(EntityItemFrame frame) {
        AMParticle particle = (AMParticle) AMCore.proxy.particleManager
            .spawn(frame.worldObj, "radiant", frame.posX, frame.posY, frame.posZ);
        if (particle != null) {
            particle.setIgnoreMaxAge(false);
            particle.setMaxAge(40);
            particle.setParticleScale(0.3f);
            // particle.AddParticleController(new ParticleApproachEntity(particle, frame, 0.02f, 0.04f, 1,
            // false).setKillParticleOnFinish(true));
            particle.AddParticleController(new ParticleHoldPosition(particle, 40, 1, false));
            particle.AddParticleController(new ParticleColorShift(particle, 1, false).SetShiftSpeed(0.2f));
        }
    }

    private class EntityItemFrameComparator {

        private final EntityItemFrame frame;

        public EntityItemFrameComparator(EntityItemFrame frame) {
            this.frame = frame;
        }

        @Override
        public boolean equals(Object obj) {
            if (frame == null) return false;
            if (obj instanceof EntityItemFrame) {
                return ((EntityItemFrame) obj).getEntityId() == frame.getEntityId()
                    && ((EntityItemFrame) obj).worldObj.isRemote == frame.worldObj.isRemote;
            }
            if (obj instanceof EntityItemFrameComparator) {
                return ((EntityItemFrameComparator) obj).frame.getEntityId() == frame.getEntityId()
                    && ((EntityItemFrameComparator) obj).frame.worldObj.isRemote == frame.worldObj.isRemote;
            }
            return false;
        }

        @Override
        public int hashCode() {
            if (frame == null || frame.worldObj == null) return 0;
            return frame.getEntityId() + (frame.worldObj.isRemote ? 1 : 2);
        }
    }
}
