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
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

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
        updateConnectedGeneratorsForComponent(index, archetypeChunk, commandBuffer); // Update connected generators
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
        EnergyComponent generatorComponent = (EnergyComponent) archetypeChunk.getComponent(index, Hytech.get().getEnergyComponentType());

        // Search neighboring blocks for EnergyComponents to transfer energy to/from
        // If im a generator block ill look for touching storage blocks to push energy to
        if(generatorComponent.getType() == EnergyComponent.Type.GENERATOR) {
            BlockModule.BlockStateInfo stateInfo = (BlockModule.BlockStateInfo) archetypeChunk.getComponent(index, BlockModule.BlockStateInfo.getComponentType());
            WorldChunk wc = (WorldChunk) commandBuffer.getComponent(stateInfo.getChunkRef(), WorldChunk.getComponentType());

//            int i = stateInfo.getIndex();
//            int x = ChunkUtil.worldCoordFromLocalCoord(wc.getX(), ChunkUtil.xFromBlockInColumn(i));
//            int y = ChunkUtil.yFromBlockInColumn(i);
//            int z = ChunkUtil.worldCoordFromLocalCoord(wc.getZ(), ChunkUtil.zFromBlockInColumn(i));
//
//            Vector3i startingBlockPositionToScan = new Vector3i(x, y, z);

            // TODO: Function that will take the startingBlock and check all 6 sides for EnergyComponents
            scanAdjacentForStorage(generatorComponent, wc.getWorld(), wc, commandBuffer);

            //LOGGER.atInfo().log("Checking for a touching storage (im a generator).... (" + energy.getEnergy() + " energy)");
            return false;
        }

        return true;
    }

    private void scanAdjacentForStorage(@Nonnull EnergyComponent generatorComponent, @Nonnull World world, @Nonnull WorldChunk wc, @Nonnull CommandBuffer commandBuffer) {
        for (Vector3i dir : Vector3i.BLOCK_SIDES) {
            Vector3i neighbor = generatorComponent.getBlockPosition3d().clone().add(dir);

            // Resolve the block component holder at the neighbor position from the world
            Holder<ChunkStore> holder = world.getBlockComponentHolder(neighbor.getX(), neighbor.getY(), neighbor.getZ());
            if (holder == null) {
                continue;
            }

            // Prefer any pending update in the command buffer so multiple generators accumulate correctly
            EnergyComponent touchingStorageComponent = (EnergyComponent) commandBuffer.getComponent(
                    wc.getBlockComponentEntity(neighbor.x, neighbor.y, neighbor.z),
                    Hytech.get().getEnergyComponentType()
            );

//            if (touchingStorageComponent == null) {
//                touchingStorageComponent = holder.getComponent(Hytech.get().getEnergyComponentType());
//            }

            if (touchingStorageComponent != null && touchingStorageComponent.getType() == EnergyComponent.Type.STORAGE) {
                float transferable = Math.min(
                        generatorComponent.getStorageRate(), // rate at which energy can be transferred default 0.5
                        Math.min(
                                generatorComponent.getEnergy(), // how much STORED energy we have to give
                                Math.max(0f, touchingStorageComponent.getCapacity() - touchingStorageComponent.getEnergy())
                        )
                );

                if (transferable > 0) {
                    touchingStorageComponent.insert(transferable); // Insert into neighbor storage
                    generatorComponent.extract(transferable); // Extract from self generator

                    commandBuffer.putComponent(
                            wc.getBlockComponentEntity(neighbor.x, neighbor.y, neighbor.z),
                            Hytech.get().getEnergyComponentType(),
                            touchingStorageComponent
                    );
                }
            }
        }
    }

    private void updateConnectedGeneratorsForComponent(int index,
                                                       @Nonnull ArchetypeChunk archetypeChunk,
                                                       @Nonnull CommandBuffer commandBuffer) {
        EnergyComponent energy = (EnergyComponent) archetypeChunk.getComponent(index, Hytech.get().getEnergyComponentType());
        if (energy == null) return;
        if (energy.getType() != EnergyComponent.Type.GENERATOR) return;

        BlockModule.BlockStateInfo stateInfo = (BlockModule.BlockStateInfo) archetypeChunk.getComponent(index, BlockModule.BlockStateInfo.getComponentType());
        if (stateInfo == null) return;

        WorldChunk wc = (WorldChunk) commandBuffer.getComponent(stateInfo.getChunkRef(), WorldChunk.getComponentType());
        if (wc == null) return;

        int i = stateInfo.getIndex();
        int x = ChunkUtil.worldCoordFromLocalCoord(wc.getX(), ChunkUtil.xFromBlockInColumn(i));
        int y = ChunkUtil.yFromBlockInColumn(i);
        int z = ChunkUtil.worldCoordFromLocalCoord(wc.getZ(), ChunkUtil.zFromBlockInColumn(i));
        Vector3i start = new Vector3i(x, y, z);

        World world = wc.getWorld();

        // gather connected generator positions (including start if applicable)
        Set<Vector3i> connected = gatherConnectedGenerators(start, world);

        // update component with positions
        energy.setConnectedGenerators(connected);

        // provide a lookup to update cached aggregates
        energy.updateConnectedAggregates(pos -> {
            Holder<ChunkStore> h = world.getBlockComponentHolder(pos.x, pos.y, pos.z);
            if (h == null) return null;
            return h.getComponent(Hytech.get().getEnergyComponentType());
        });

        // persist the updated component so later systems / commands see it
        commandBuffer.putComponent(
                wc.getBlockComponentEntity(start.x, start.y, start.z),
                Hytech.get().getEnergyComponentType(),
                energy
        );
    }

    /**
     * BFS over adjacent blocks to collect all connected generator block positions.
     * Only traverses blocks that have an EnergyComponent with Type.GENERATOR.
     */
    private Set<Vector3i> gatherConnectedGenerators(@Nonnull Vector3i start, @Nonnull World world) {
        Set<Vector3i> visited = new HashSet<>();
        Deque<Vector3i> q = new ArrayDeque<>();

        // quick check: start must be a generator
        Holder<ChunkStore> startHolder = world.getBlockComponentHolder(start.x, start.y, start.z);
        if (startHolder == null) return visited;
        EnergyComponent startEc = startHolder.getComponent(Hytech.get().getEnergyComponentType());
        if (startEc == null || startEc.getType() != EnergyComponent.Type.GENERATOR) return visited;

        visited.add(start);
        q.add(start);

        while (!q.isEmpty()) {
            Vector3i cur = q.removeFirst();

            // iterate 6 neighbours
            for (Vector3i dir : Vector3i.BLOCK_SIDES) {
                Vector3i nb = cur.clone().add(dir);
                if (visited.contains(nb)) continue;

                Holder<ChunkStore> holder = world.getBlockComponentHolder(nb.x, nb.y, nb.z);
                if (holder == null) continue;

                EnergyComponent nbEc = holder.getComponent(Hytech.get().getEnergyComponentType());
                if (nbEc != null && nbEc.getType() == EnergyComponent.Type.GENERATOR) {
                    visited.add(nb);
                    q.addLast(nb);
                }
            }
        }

        return visited;
    }

    @Nonnull
    @Override
    public Query<ChunkStore> getQuery() {
        return Query.and(BlockModule.BlockStateInfo.getComponentType(), Hytech.get().getEnergyComponentType());
    }
}