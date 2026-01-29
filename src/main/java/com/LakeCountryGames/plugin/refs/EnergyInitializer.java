package com.LakeCountryGames.plugin.refs;

import com.LakeCountryGames.plugin.Hytech;
import com.LakeCountryGames.plugin.components.EnergyComponent;
import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.RefSystem;
import com.hypixel.hytale.server.core.modules.block.BlockModule;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import org.jetbrains.annotations.NotNull;

public class EnergyInitializer extends RefSystem<ChunkStore> {
    @Override
    public void onEntityAdded(@NotNull Ref ref, @NotNull AddReason addReason, @NotNull Store store, @NotNull CommandBuffer commandBuffer) {
        BlockModule.BlockStateInfo info = (BlockModule.BlockStateInfo) commandBuffer.getComponent(ref, BlockModule.BlockStateInfo.getComponentType());
        if (info == null) return;
        EnergyComponent energy = (EnergyComponent) commandBuffer.getComponent(ref, Hytech.get().getEnergyComponentType());
        if (energy != null) {
            // We found the block being added?

        }
    }

    @Override
    public void onEntityRemove(@NotNull Ref ref, @NotNull RemoveReason removeReason, @NotNull Store store, @NotNull CommandBuffer commandBuffer) {

    }

    @Override
    public Query getQuery() {
        return Query.and(BlockModule.BlockStateInfo.getComponentType(), Hytech.get().getEnergyComponentType());
    }
}
