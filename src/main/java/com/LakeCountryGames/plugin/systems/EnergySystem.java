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
import java.util.concurrent.CompletionException;

/**
 * EnergySystem (Extending the EntityTickingSystem)-> Handles logic for blocks that can store/generate/transmit energy.
 *
 * Defensive world access: use safeGetHolder(...) everywhere to avoid triggering chunk loads / store writes while the store is processing.
 */
public class EnergySystem extends EntityTickingSystem<ChunkStore> {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    // Helper that returns null instead of allowing an exception to bubble up and trigger store writes.
    private Holder<ChunkStore> safeGetHolder(@Nonnull World world, int x, int y, int z) {
        try {
            return world.getBlockComponentHolder(x, y, z);
        } catch (CompletionException | IllegalStateException e) {
            // Chunk is being loaded / would trigger store ops — skip it.
            LOGGER.atFine().log("Skipping holder access for %d,%d,%d due to chunk load/store: %s", x, y, z, e.getMessage());
            return null;
        } catch (Exception e) {
            LOGGER.atWarning().log("Unexpected error getting holder for %d,%d,%d: %s", x, y, z, e.getMessage());
            return null;
        }
    }

    @Override
    public void tick(float dt, int index, @Nonnull ArchetypeChunk archetypeChunk, @Nonnull Store store, @Nonnull CommandBuffer commandBuffer) {
        BlockSection blocks = (BlockSection) archetypeChunk.getComponent(index, BlockSection.getComponentType());
        assert blocks != null;

        EnergyComponent energy = (EnergyComponent) archetypeChunk.getComponent(index, Hytech.get().getEnergyComponentType());
        if (energy == null) return;

        // Lightweight per-tick work
        energy.onComponentTicked(dt);
        onHandleEnergyGeneration(dt, energy);

        if (energy.shouldProcessNetworkTick()) {
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
        if (energy.getType() == EnergyComponent.Type.GENERATOR) {
            energy.insert(energy.getEnergyPerTick());
        }
    }

    public void onHandleTransferEnergy(float dt, int index, @Nonnull ArchetypeChunk archetypeChunk, @Nonnull Store store, @Nonnull CommandBuffer commandBuffer) {
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

        EnergyComponent generatorComponent = (EnergyComponent) archetypeChunk.getComponent(index, Hytech.get().getEnergyComponentType());
        if (generatorComponent == null) return true;

        if (generatorComponent.getType() == EnergyComponent.Type.GENERATOR) {
            BlockModule.BlockStateInfo stateInfo = (BlockModule.BlockStateInfo) archetypeChunk.getComponent(index, BlockModule.BlockStateInfo.getComponentType());
            if (stateInfo == null) return true;

            WorldChunk wc = (WorldChunk) commandBuffer.getComponent(stateInfo.getChunkRef(), WorldChunk.getComponentType());
            if (wc == null) return true;

            // Use safe neighbor scan that avoids blocking chunk loads
            scanAdjacentForStorage(generatorComponent, wc.getWorld(), wc, commandBuffer);
            return false;
        }

        return true;
    }

    private void scanAdjacentForStorage(@Nonnull EnergyComponent generatorComponent, @Nonnull World world, @Nonnull WorldChunk wc, @Nonnull CommandBuffer commandBuffer) {
        for (Vector3i dir : Vector3i.BLOCK_SIDES) {
            Vector3i neighbor = generatorComponent.getBlockPosition3d().clone().add(dir);

            Holder<ChunkStore> holder = safeGetHolder(world, neighbor.getX(), neighbor.getY(), neighbor.getZ());
            if (holder == null) continue;

            Ref<ChunkStore> neighborRef = null;
            try {
                neighborRef = wc.getBlockComponentEntity(neighbor.x, neighbor.y, neighbor.z);
            } catch (Exception e) {
                // wc.getBlockComponentEntity may throw for edge cases; treat as missing
                neighborRef = null;
            }

            EnergyComponent touchingStorageComponent = null;

            if (neighborRef != null) {
                try {
                    touchingStorageComponent = (EnergyComponent) commandBuffer.getComponent(neighborRef, Hytech.get().getEnergyComponentType());
                } catch (Exception e) {
                    touchingStorageComponent = null;
                }
            }

            if (touchingStorageComponent == null) {
                try {
                    touchingStorageComponent = holder.getComponent(Hytech.get().getEnergyComponentType());
                } catch (Exception e) {
                    touchingStorageComponent = null;
                }
            }

            if (touchingStorageComponent != null && touchingStorageComponent.getType() == EnergyComponent.Type.STORAGE) {
                float transferable = Math.min(
                        generatorComponent.getStorageRate(),
                        Math.min(
                                generatorComponent.getEnergy(),
                                Math.max(0f, touchingStorageComponent.getCapacity() - touchingStorageComponent.getEnergy())
                        )
                );

                if (transferable > 0) {
                    touchingStorageComponent.insert(transferable);
                    generatorComponent.extract(transferable);

                    if (neighborRef != null) {
                        try {
                            commandBuffer.putComponent(
                                    neighborRef,
                                    Hytech.get().getEnergyComponentType(),
                                    touchingStorageComponent
                            );
                        } catch (Exception e) {
                            // ignore failures for now
                        }
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

        //  safe lookup to update cached aggregates (avoid throwing during system tick)
        energy.updateConnectedAggregates(pos -> {
            Holder<ChunkStore> h = safeGetHolder(world, pos.x, pos.y, pos.z);
            if (h == null) return null;
            try {
                return h.getComponent(Hytech.get().getEnergyComponentType());
            } catch (Exception e) {
                LOGGER.atFine().log("Skipping aggregate lookup for %s: %s", pos, e.getMessage());
                return null;
            }
        });

        // persist the updated component so later systems / commands see it (only if ref exists)
        Ref<ChunkStore> startRef = null;
        try {
            startRef = wc.getBlockComponentEntity(start.x, start.y, start.z);
        } catch (Exception e) {
            startRef = null;
        }
        if (startRef != null) {
            try {
                commandBuffer.putComponent(
                        startRef,
                        Hytech.get().getEnergyComponentType(),
                        energy
                );
            } catch (Exception e) {
                // ignore for now
            }
        }
    }

    private Set<Vector3i> gatherConnectedGenerators(@Nonnull Vector3i start, @Nonnull World world) {
        Set<Vector3i> visited = new HashSet<>();
        Deque<Vector3i> q = new ArrayDeque<>();

        Holder<ChunkStore> startHolder = safeGetHolder(world, start.x, start.y, start.z);
        if (startHolder == null) return visited;

        EnergyComponent startEc = null;
        try {
            startEc = startHolder.getComponent(Hytech.get().getEnergyComponentType());
        } catch (Exception e) {
            return visited;
        }
        if (startEc == null || startEc.getType() != EnergyComponent.Type.GENERATOR) return visited;

        visited.add(start);
        q.add(start);

        while (!q.isEmpty()) {
            Vector3i cur = q.removeFirst();

            for (Vector3i dir : Vector3i.BLOCK_SIDES) {
                Vector3i nb = cur.clone().add(dir);
                if (visited.contains(nb)) continue;

                Holder<ChunkStore> holder = safeGetHolder(world, nb.x, nb.y, nb.z);
                if (holder == null) continue;

                EnergyComponent nbEc = null;
                try {
                    nbEc = holder.getComponent(Hytech.get().getEnergyComponentType());
                } catch (Exception e) {
                    continue;
                }

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

                Holder<ChunkStore> holder = safeGetHolder(world, nb.x, nb.y, nb.z);
                if (holder == null) continue;

                EnergyComponent nbComp = null;
                try {
                    nbComp = holder.getComponent(Hytech.get().getEnergyComponentType());
                } catch (Exception e) {
                    continue;
                }

                if (nbComp == null) continue;

                if (nbComp.getType() == EnergyComponent.Type.STORAGE) {
                    foundStorages.add(nb);
                    visited.add(nb);
                    // do not enqueue further from storage
                    continue;
                }

                if (nbComp.getType() == EnergyComponent.Type.TRANSFER || nbComp.getType() == EnergyComponent.Type.GENERATOR) {
                    visited.add(nb);
                    q.addLast(nb);
                }
            }
        }

        // Fill storages in BFS order closest first until generator energy is exhausted
        for (Vector3i storagePos : foundStorages) {
            if (generatorComponent.getEnergy() <= 0f) break;

            Ref<ChunkStore> storageRef = null;
            try {
                storageRef = wc.getBlockComponentEntity(storagePos.x, storagePos.y, storagePos.z);
            } catch (Exception e) {
                storageRef = null;
            }

            EnergyComponent storageComp = null;

            if (storageRef != null) {
                try {
                    storageComp = (EnergyComponent) commandBuffer.getComponent(storageRef, Hytech.get().getEnergyComponentType());
                } catch (Exception e) {
                    storageComp = null;
                }
            }

            if (storageComp == null) {
                Holder<ChunkStore> h = safeGetHolder(world, storagePos.x, storagePos.y, storagePos.z);
                if (h == null) continue;
                try {
                    storageComp = h.getComponent(Hytech.get().getEnergyComponentType());
                } catch (Exception e) {
                    continue;
                }
                if (storageComp == null) continue;
            }

            float freeSpace = Math.max(0f, storageComp.getCapacity() - storageComp.getEnergy());
            float transferable = Math.min(generatorComponent.getStorageRate(), Math.min(generatorComponent.getEnergy(), freeSpace));

            if (transferable > 0f) {
                storageComp.insert(transferable);
                generatorComponent.extract(transferable);

                if (storageRef != null) {
                    try {
                        commandBuffer.putComponent(storageRef, Hytech.get().getEnergyComponentType(), storageComp);
                    } catch (Exception e) {
                        // ignore
                    }
                }
            }
        }

        Ref<ChunkStore> genRef = null;
        try {
            genRef = wc.getBlockComponentEntity(start.x, start.y, start.z);
        } catch (Exception e) {
            genRef = null;
        }
        if (genRef != null) {
            try {
                commandBuffer.putComponent(
                        genRef,
                        Hytech.get().getEnergyComponentType(),
                        generatorComponent
                );
            } catch (Exception e) {
                // ignore
            }
        }
    }

    @Nonnull
    @Override
    public Query<ChunkStore> getQuery() {
        return Query.and(BlockModule.BlockStateInfo.getComponentType(), Hytech.get().getGeneratorMarkerType());
    }
}