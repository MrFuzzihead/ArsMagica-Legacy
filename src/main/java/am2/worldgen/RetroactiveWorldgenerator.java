package am2.worldgen;

import java.util.Deque;

import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.World;

import am2.AMCore;
import am2.LogHelper;
import am2.world.WorldStateManager;

public class RetroactiveWorldgenerator {

    public static final RetroactiveWorldgenerator instance = new RetroactiveWorldgenerator();

    public void continueRetrogen(World world) {
        if (world == null || world.isRemote) return;

        Deque<ChunkCoordIntPair> chunks = WorldStateManager.retroGenChunks(world);
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

        if (count > 0) {
            LogHelper.info("Retro-genned %d chunks, %d left to generate.", count, chunks.size());
        }
    }
}
