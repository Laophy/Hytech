package com.LakeCountryGames.plugin.systems;

import com.LakeCountryGames.plugin.Hytech;
import com.LakeCountryGames.plugin.components.EnergyComponent;
import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.modules.block.BlockModule;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.chunk.section.BlockSection;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import javax.annotation.Nonnull;
import java.util.*;

/**
 * EnergySystem (Extending the EntityTickingSystem)-> Handles logic for blocks that can store/generate/transmit energy.
 *
 * Logical system class that can communicate with the EnergyComponent.
 */
public class EnergySystem extends EntityTickingSystem<ChunkStore> {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    @Override
    public void tick(float dt, int index, @Nonnull ArchetypeChunk archetypeChunk, @Nonnull Store store, @Nonnull CommandBuffer commandBuffer) {
        // Only generator-marked blocks are returned by the query now, so work is limited.
        BlockSection blocks = (BlockSection) archetypeChunk.getComponent(index, BlockSection.getComponentType());
        assert blocks != null;

        EnergyComponent energy = (EnergyComponent) archetypeChunk.getComponent(index, Hytech.get().getEnergyComponentType());
        if (energy == null) return;

        // Per-tick lightweight work
        energy.onComponentTicked(dt);
        onHandleEnergyGeneration(dt, energy); // generation happens every tick

        // Heavy network logic (BFS + transfers + connected aggregates) runs only when cooldown allows.
        if (energy.shouldProcessNetworkTick()) {
            // compute world/positions
            BlockModule.BlockStateInfo stateInfo = (BlockModule.BlockStateInfo) archetypeChunk.getComponent(index, BlockModule.BlockStateInfo.getComponentType());
            if (stateInfo == null) return;
            WorldChunk wc = (WorldChunk) commandBuffer.getComponent(stateInfo.getChunkRef(), WorldChunk.getComponentType());
            if (wc == null) return;

            int i = stateInfo.getIndex();
            int x = ChunkUtil.worldCoordFromLocalCoord(wc.getX(), ChunkUtil.xFromBlockInColumn(i));
            int y = ChunkUtil.yFromBlockInColumn(i);
            int z = ChunkUtil.worldCoordFromLocalCoord(wc.getZ(), ChunkUtil.zFromBlockInColumn(i));
            Vector3i start = new Vector3i(x, y, z);

            updateConnectedGeneratorsForComponent(index, archetypeChunk, commandBuffer);
            onHandleTransferEnergy(dt, index, archetypeChunk, store, commandBuffer);
        }
    }

    public void onHandleEnergyGeneration(float dt, EnergyComponent energy) {
        if(energy.getType() == EnergyComponent.Type.GENERATOR){
            energy.insert(energy.getEnergyPerTick());
        }
    }

    public void onHandleTransferEnergy(float dt, int index, @Nonnull ArchetypeChunk archetypeChunk, @Nonnull Store store, @Nonnull CommandBuffer commandBuffer) {
        // Transfer logic expects generator context; the query ensures the entity is a generator.
        BlockModule.BlockStateInfo stateInfo = (BlockModule.BlockStateInfo) archetypeChunk.getComponent(index, BlockModule.BlockStateInfo.getComponentType());
        if (stateInfo == null) return;
        WorldChunk wc = (WorldChunk) commandBuffer.getComponent(stateInfo.getChunkRef(), WorldChunk.getComponentType());
        if (wc == null) return;

        int i = stateInfo.getIndex();
        int x = ChunkUtil.worldCoordFromLocalCoord(wc.getX(), ChunkUtil.xFromBlockInColumn(i));
        int y = ChunkUtil.yFromBlockInColumn(i);
        int z = ChunkUtil.worldCoordFromLocalCoord(wc.getZ(), ChunkUtil.zFromBlockInColumn(i));
        Vector3i start = new Vector3i(x, y, z);

        EnergyComponent energy = (EnergyComponent) archetypeChunk.getComponent(index, Hytech.get().getEnergyComponentType());
        if (energy == null) return;

        transferEnergyThroughNetwork(energy, start, wc.getWorld(), wc, commandBuffer);
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

            // Use safe neighbor scan that avoids calling CommandBuffer with null refs
            scanAdjacentForStorage(generatorComponent, wc.getWorld(), wc, commandBuffer);

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

            // Try command buffer first, but guard against null Ref from wc.getBlockComponentEntity(...)
            Ref<ChunkStore> neighborRef = wc.getBlockComponentEntity(neighbor.x, neighbor.y, neighbor.z);
            EnergyComponent touchingStorageComponent = null;

            if (neighborRef != null) {
                touchingStorageComponent = (EnergyComponent) commandBuffer.getComponent(neighborRef, Hytech.get().getEnergyComponentType());
            }

            // Fallback to holder if nothing in command buffer
            if (touchingStorageComponent == null) {
                touchingStorageComponent = holder.getComponent(Hytech.get().getEnergyComponentType());
            }

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

                    // Only put to command buffer if we have a valid Ref
                    if (neighborRef != null) {
                        commandBuffer.putComponent(
                                neighborRef,
                                Hytech.get().getEnergyComponentType(),
                                touchingStorageComponent
                        );
                    }
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
        Ref<ChunkStore> startRef = wc.getBlockComponentEntity(start.x, start.y, start.z);
        if (startRef != null) {
            commandBuffer.putComponent(
                    startRef,
                    Hytech.get().getEnergyComponentType(),
                    energy
            );
        }
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

    private void transferEnergyThroughNetwork(@Nonnull EnergyComponent generatorComponent,
                                              @Nonnull Vector3i start,
                                              @Nonnull World world,
                                              @Nonnull WorldChunk wc,
                                              @Nonnull CommandBuffer commandBuffer) {
        // BFS to discover storages in distance order
        Set<Vector3i> visited = new HashSet<>();
        Deque<Vector3i> q = new ArrayDeque<>();
        List<Vector3i> foundStorages = new ArrayList<>();

        visited.add(start);
        q.addLast(start);

        while (!q.isEmpty()) {
            Vector3i cur = q.removeFirst();

            for (Vector3i dir : Vector3i.BLOCK_SIDES) {
                Vector3i nb = cur.clone().add(dir);
                if (visited.contains(nb)) continue;

                Holder<ChunkStore> holder = world.getBlockComponentHolder(nb.x, nb.y, nb.z);
                if (holder == null) continue;

                EnergyComponent nbComp = holder.getComponent(Hytech.get().getEnergyComponentType());
                if (nbComp == null) {
                    visited.add(nb);
                    continue;
                }

                // If it's a storage, record it as an endpoint (do not enqueue further from storage)
                if (nbComp.getType() == EnergyComponent.Type.STORAGE) {
                    visited.add(nb);
                    foundStorages.add(nb);
                    continue;
                }

                // Traverse through wires and other generators so networks are followed
                if (nbComp.getType() == EnergyComponent.Type.TRANSFER || nbComp.getType() == EnergyComponent.Type.GENERATOR) {
                    visited.add(nb);
                    q.addLast(nb);
                }
            }
        }

        // Fill storages in BFS order closest first until generator energy is empty
        for (Vector3i storagePos : foundStorages) {
            if (generatorComponent.getEnergy() <= 0f) break;

            Ref<ChunkStore> storageRef = wc.getBlockComponentEntity(storagePos.x, storagePos.y, storagePos.z);
            EnergyComponent storageComp = null;

            if (storageRef != null) {
                storageComp = (EnergyComponent) commandBuffer.getComponent(storageRef, Hytech.get().getEnergyComponentType());
            }

            if (storageComp == null) {
                Holder<ChunkStore> h = world.getBlockComponentHolder(storagePos.x, storagePos.y, storagePos.z);
                if (h == null) continue;
                storageComp = h.getComponent(Hytech.get().getEnergyComponentType());
                if (storageComp == null) continue;
            }

            float freeSpace = Math.max(0f, storageComp.getCapacity() - storageComp.getEnergy());
            float transferable = Math.min(generatorComponent.getStorageRate(), Math.min(generatorComponent.getEnergy(), freeSpace));

            if (transferable > 0f) {
                storageComp.insert(transferable);
                generatorComponent.extract(transferable);

                if (storageRef != null) {
                    commandBuffer.putComponent(
                            storageRef,
                            Hytech.get().getEnergyComponentType(),
                            storageComp
                    );
                }
            }
        }

        Ref<ChunkStore> genRef = wc.getBlockComponentEntity(start.x, start.y, start.z);
        if (genRef != null) {
            commandBuffer.putComponent(
                    genRef,
                    Hytech.get().getEnergyComponentType(),
                    generatorComponent
            );
        }
    }

    @Nonnull
    @Override
    public Query<ChunkStore> getQuery() {
        return Query.and(BlockModule.BlockStateInfo.getComponentType(), Hytech.get().getGeneratorMarkerType());
    }
}