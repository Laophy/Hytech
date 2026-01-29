package com.LakeCountryGames.plugin.systems;

import com.LakeCountryGames.plugin.Hytech;
import com.LakeCountryGames.plugin.components.EnergyComponent;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.BlockPosition;
import com.hypixel.hytale.server.core.entity.entities.BlockEntity;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.block.BlockModule;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.chunk.section.BlockSection;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import javax.annotation.Nonnull;

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

        // Tick the component
        assert energy != null;
        energy.onComponentTicked(dt); // Tick our energy component

        onHandleEnergyGeneration(dt, energy); // Tick the energy generation logic
        onHandleTransferEnergy(dt, index, archetypeChunk, store, commandBuffer); // Tick the energy transfer logic
    }

    public void onHandleEnergyGeneration(float dt, EnergyComponent energy) {
        // Handle energy generation logic here
        energy.insert(energy.getEnergyPerTick()); // Generate energy per tick simple for now
        //LOGGER.atInfo().log("Solar block just ticked..... Current energy: " + energy.getEnergy() + " tried to add " + amountInserted);
    }

    // Check for neighbors and transfer energy accordingly
    //
    //  This tick happens on every single block with an EnergyComponent
    //
    public void onHandleTransferEnergy(float dt, int index, @Nonnull ArchetypeChunk archetypeChunk, @Nonnull Store store, @Nonnull CommandBuffer commandBuffer) {
        // Handle energy transfer logic here

        // Check for neighboring blocks with EnergyComponent???
        checkForTouchingStorage(dt, index, archetypeChunk, store, commandBuffer);

        // Check for wires???
    }

    public boolean checkForTouchingStorage(float dt, int index, @Nonnull ArchetypeChunk archetypeChunk, @Nonnull Store store, @Nonnull CommandBuffer commandBuffer){
        BlockSection blocks = (BlockSection) archetypeChunk.getComponent(index, BlockSection.getComponentType());
        assert blocks != null;

        // The block that looks for neighbors (your self)
        EnergyComponent energy = (EnergyComponent) archetypeChunk.getComponent(index, Hytech.get().getEnergyComponentType());

        // Search neighboring blocks for EnergyComponents to transfer energy to/from

        // If im a generator block ill look for touching storage blocks to push energy to
        if(energy.getType() == EnergyComponent.Type.GENERATOR) {
            // get world? get block position?
            Ref<EntityStore> ref = archetypeChunk.getReferenceTo(index);
            BlockSection blockPos = (BlockSection) store.getComponent(ref, BlockSection.getComponentType());



            //LOGGER.atInfo().log("Looking for touching storage (im a generator) at " + blockPos.toString() + " (" + energy.getEnergy() + " energy)");

            //LOGGER.atInfo().log("Checking for a touching storage (im a generator).... (" + energy.getEnergy() + " energy)");
            return false;
        }


        return true;
    }

    @Nonnull
    @Override
    public Query<ChunkStore> getQuery() {
        return Query.and(BlockModule.BlockStateInfo.getComponentType(), Hytech.get().getEnergyComponentType());
    }
}