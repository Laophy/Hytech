package com.LakeCountryGames.plugin.refs;

import com.LakeCountryGames.plugin.Hytech;
import com.LakeCountryGames.plugin.components.EnergyComponent;
import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.RefSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.modules.block.BlockModule;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import org.jetbrains.annotations.NotNull;

public class EnergyInitializer extends RefSystem<ChunkStore> {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    @Override
    public void onEntityAdded(@NotNull Ref ref, @NotNull AddReason addReason, @NotNull Store store, @NotNull CommandBuffer commandBuffer) {
        // Player store
        // Player player = (Player) store.getComponent(ref, Player.getComponentType());
        // player.sendMessage(Message.raw("EnergyInitializer triggered onEntityAdded"));

        BlockModule.BlockStateInfo info = (BlockModule.BlockStateInfo) commandBuffer.getComponent(ref, BlockModule.BlockStateInfo.getComponentType());
        if (info == null) return;
        EnergyComponent energy = (EnergyComponent) commandBuffer.getComponent(ref, Hytech.get().getEnergyComponentType());
        if (energy != null) {
            // We found the block being added?
            //player.sendMessage(Message.raw("is energy???"));
            LOGGER.atInfo().log("Entity was added to the world! ");

            // Init any component things we need
            WorldChunk wc = (WorldChunk) commandBuffer.getComponent(info.getChunkRef(), WorldChunk.getComponentType());

            int i = info.getIndex();
            int x = ChunkUtil.worldCoordFromLocalCoord(wc.getX(), ChunkUtil.xFromBlockInColumn(i));
            int y = ChunkUtil.yFromBlockInColumn(i);
            int z = ChunkUtil.worldCoordFromLocalCoord(wc.getZ(), ChunkUtil.zFromBlockInColumn(i));

            // Helper to save block location
            Vector3i blockPos = new Vector3i(x, y, z);
            energy.setBlockPosition3d(blockPos);
        }
    }

    @Override
    public void onEntityRemove(@NotNull Ref ref, @NotNull RemoveReason removeReason, @NotNull Store store, @NotNull CommandBuffer commandBuffer) {
        BlockModule.BlockStateInfo info = (BlockModule.BlockStateInfo) commandBuffer.getComponent(ref, BlockModule.BlockStateInfo.getComponentType());
        if (info == null) return;
        EnergyComponent energy = (EnergyComponent) commandBuffer.getComponent(ref, Hytech.get().getEnergyComponentType());
        if (energy != null) {
            // We found the block being added?
            LOGGER.atInfo().log("Entity was removed from the world.... ");
        }
    }

    @Override
    public Query getQuery() {
        return Query.and(BlockModule.BlockStateInfo.getComponentType(), Hytech.get().getEnergyComponentType());
    }
}
