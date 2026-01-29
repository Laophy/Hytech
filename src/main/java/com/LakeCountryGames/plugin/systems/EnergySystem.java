package com.LakeCountryGames.plugin.systems;

import com.LakeCountryGames.plugin.Hytech;
import com.LakeCountryGames.plugin.components.EnergyComponent;
import com.LakeCountryGames.plugin.power.PowerNode;
import com.LakeCountryGames.plugin.power.PowerNodeRegistry;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.SystemGroup;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.modules.block.BlockModule;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageCause;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageModule;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageSystems;
import com.hypixel.hytale.server.core.universe.world.chunk.section.BlockSection;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * EnergySystem (Extending the EntityTickingSystem)-> Handles logic for blocks that can store/generate/transmit energy.
 *
 * Logical system class that can communicate with the EnergyComponent.
 */
public class EnergySystem extends EntityTickingSystem<ChunkStore> {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    @Override
    public void tick(float dt, int index, @Nonnull ArchetypeChunk archetypeChunk, @Nonnull Store store, @Nonnull CommandBuffer commandBuffer) {
        BlockSection blocks = (BlockSection) archetypeChunk.getComponent(index, BlockSection.getComponentType());

        assert blocks != null;

        EnergyComponent energy = (EnergyComponent) archetypeChunk.getComponent(index, Hytech.get().getEnergyComponentType());

        // do something every tick
        // dt is the deltaTime or time since last tick in seconds
        assert energy != null;
        int amountInserted = energy.insert(1);
        LOGGER.atInfo().log("Solar block just ticked..... Current energy: " + energy.getEnergy() + " tried to add " + amountInserted);
    }


    @Nonnull
    @Override
    public Query<ChunkStore> getQuery() {
        return Query.and(BlockModule.BlockStateInfo.getComponentType(), Hytech.get().getEnergyComponentType());
    }
}