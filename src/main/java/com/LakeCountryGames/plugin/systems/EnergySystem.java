package com.LakeCountryGames.plugin.systems;

import com.LakeCountryGames.plugin.Hytech;
import com.LakeCountryGames.plugin.components.EnergyComponent;
import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.BlockPosition;
import com.hypixel.hytale.server.core.modules.block.BlockModule;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.chunk.section.BlockSection;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
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
        if(energy.getType() == EnergyComponent.Type.GENERATOR){
            energy.insert(energy.getEnergyPerTick()); // Generate energy per tick simple for now
        }
    }

    // Check for neighbors and transfer energy accordingly
    //
    //  This tick happens on every single block with an EnergyComponent
    public void onHandleTransferEnergy(float dt, int index, @Nonnull ArchetypeChunk archetypeChunk, @Nonnull Store store, @Nonnull CommandBuffer commandBuffer) {
        // Handle energy transfer logic here

        // Is a storage block touching a generator block (not checking for wires)
        checkForTouchingStorage(dt, index, archetypeChunk, store, commandBuffer);

        // TODO: Check for wires until we reach a storage/generator block and transfer energy accordingly
    }

    // Returns true if touching storage found and energy transferred (or attempted)
    public boolean checkForTouchingStorage(float dt, int index, @Nonnull ArchetypeChunk archetypeChunk, @Nonnull Store store, @Nonnull CommandBuffer commandBuffer){
        BlockSection blocks = (BlockSection) archetypeChunk.getComponent(index, BlockSection.getComponentType());
        assert blocks != null;

        // The block that looks for neighbors (your self)
        EnergyComponent energy = (EnergyComponent) archetypeChunk.getComponent(index, Hytech.get().getEnergyComponentType());

        // Search neighboring blocks for EnergyComponents to transfer energy to/from
        // If im a generator block ill look for touching storage blocks to push energy to
        if(energy.getType() == EnergyComponent.Type.GENERATOR) {
            BlockModule.BlockStateInfo stateInfo = (BlockModule.BlockStateInfo) archetypeChunk.getComponent(index, BlockModule.BlockStateInfo.getComponentType());
            WorldChunk wc = (WorldChunk) commandBuffer.getComponent(stateInfo.getChunkRef(), WorldChunk.getComponentType());

            int i = stateInfo.getIndex();
            int x = ChunkUtil.worldCoordFromLocalCoord(wc.getX(), ChunkUtil.xFromBlockInColumn(i));
            int y = ChunkUtil.yFromBlockInColumn(i);
            int z = ChunkUtil.worldCoordFromLocalCoord(wc.getZ(), ChunkUtil.zFromBlockInColumn(i));

            Vector3i startingBlockPositionToScan = new Vector3i(x, y, z);

            // TODO: Function that will take the startingBlock and check all 6 sides for EnergyComponents
            scanAdjacentForStorage(energy, startingBlockPositionToScan, wc.getWorld(), wc, commandBuffer);

            //LOGGER.atInfo().log("Checking for a touching storage (im a generator).... (" + energy.getEnergy() + " energy)");
            return false;
        }

        return true;
    }

    private void scanAdjacentForStorage(@Nonnull EnergyComponent energy, @Nonnull Vector3i start, @Nonnull World world, @Nonnull WorldChunk wc, @Nonnull CommandBuffer commandBuffer) {
        for (Vector3i dir : Vector3i.BLOCK_SIDES) {
            Vector3i neighbor = start.clone().add(dir);

            // Resolve the block component holder at the neighbor position from the world
            Holder<ChunkStore> holder = world.getBlockComponentHolder(neighbor.getX(), neighbor.getY(), neighbor.getZ());
            if (holder == null) {
                continue;
            }

            // Try to fetch an EnergyComponent from the holder

            EnergyComponent neighborEnergy = holder.getComponent(Hytech.get().getEnergyComponentType());
            if (neighborEnergy != null && neighborEnergy.getType() == EnergyComponent.Type.STORAGE) {
                BlockPosition bp = new BlockPosition(neighbor.getX(), neighbor.getY(), neighbor.getZ());
                LOGGER.atInfo().log("Touching storage found at " + bp + " (energy=" + neighborEnergy.getEnergy() + ")");

                // TODO: JUST A TEST TRANSFER FOR NOW
                // Transfer 1 energy unit from generator to storage if possible

                // TODO: Do i need to call these updates via different thread?

                //.atInfo().log("Transferring 100 energy to storage");
                energy.extract(5); // THIS WORKS!!
                neighborEnergy.setEnergy(500); // THIS DOESNT SAVE? OR UPDATE
            }
        }
    }

    @Nonnull
    @Override
    public Query<ChunkStore> getQuery() {
        return Query.and(BlockModule.BlockStateInfo.getComponentType(), Hytech.get().getEnergyComponentType());
    }
}