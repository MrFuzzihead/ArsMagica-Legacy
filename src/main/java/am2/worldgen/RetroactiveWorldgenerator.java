package am2.worldgen;

import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.World;

import am2.AMCore;
import am2.LogHelper;

public class RetroactiveWorldgenerator {

    public static final Map<Integer, Deque<ChunkCoordIntPair>> deferredChunkGeneration = new HashMap<Integer, Deque<ChunkCoordIntPair>>();
    public static final RetroactiveWorldgenerator instance = new RetroactiveWorldgenerator();

    public void continueRetrogen(World world) {
        if (world == null || world.isRemote) return;

        int dimensionID = world.provider.dimensionId;

        Deque<ChunkCoordIntPair> chunks = deferredChunkGeneration.get(dimensionID);
        if (chunks == null || chunks.isEmpty()) return;

        int count = 0;
        int amt = Math.min(5, chunks.size());
        for (int i = 0; i < amt; ++i) {
            ChunkCoordIntPair chunkPos = chunks.peekFirst();
            if (chunkPos == null) break;
            count++;
            AMCore.proxy.worldGen.generate(
                world.rand,
                chunkPos.chunkXPos,
                chunkPos.chunkZPos,
                world,
                world.getChunkProvider(),
                world.getChunkProvider());
            chunks.pollFirst();
        }

        if (chunks.isEmpty()) {
            deferredChunkGeneration.remove(dimensionID);
        }

        if (count > 0) {
            LogHelper.info("Retro-genned %d chunks, %d left to generate.", count, chunks.size());
        }
    }
}
