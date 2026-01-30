// java
// File: src/main/java/com/LakeCountryGames/plugin/components/EnergyComponent.java

package com.LakeCountryGames.plugin.components;

import com.LakeCountryGames.plugin.Hytech;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import javax.annotation.Nullable;
import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

/**
 * EnergyComponent -> Attaches to a block that can store/generate/transmit energy.
 *
 * Added: tracking of connected generator positions and cached aggregate getters.
 *
 * Note: This class does NOT perform world lookups. Callers (EnergySystem) should
 * compute the connected set (e.g., BFS) and then call setConnectedGenerators(...)
 * or call updateConnectedAggregates(...) with a lookup function.
 */
public class EnergyComponent implements Component<ChunkStore> {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public static final BuilderCodec<EnergyComponent> CODEC = BuilderCodec.builder(
                    EnergyComponent.class,
                    EnergyComponent::new
            )
            .append(new KeyedCodec<>("Type", Codec.STRING),
                    (data, value) -> data.type = Type.valueOf(value),
                    data -> data.type.name())
            .add()
            .append(new KeyedCodec<>("Energy", Codec.FLOAT),
                    (data, value) -> data.energy = value,
                    data -> data.energy)
            .add()
            .append(new KeyedCodec<>("Capacity", Codec.FLOAT),
                    (data, value) -> data.capacity = value,
                    data -> data.capacity)
            .add()
            .append(new KeyedCodec<>("EnergyPerTick", Codec.FLOAT),
                    (data, value) -> data.energyPerTick = value,
                    data -> data.energyPerTick)
            .add()
            .build();

    public enum Type { GENERATOR, STORAGE, TRANSFER }

    private Type type;
    private float energy;
    private float capacity;
    private float energyPerTick;
    private float storageRate;
    private Vector3i blockPosition3d;

    // New: positions of connected generators (including self if generator)
    // Use a synchronized set to allow simple concurrent access patterns.
    private final Set<Vector3i> connectedGenerators = Collections.synchronizedSet(new HashSet<>());

    // Cached aggregates for quick reads after an update
    private volatile float connectedTotalEnergy = 0f;
    private volatile float connectedTotalCapacity = 0f;
    private volatile float connectedTotalEnergyPerTick = 0f;
    private volatile float connectedTotalStorageRate = 0f;

    public EnergyComponent() {
        this.type = Type.STORAGE;
        this.energy = 0;
        this.capacity = 5000;
        this.energyPerTick = 1;
        this.storageRate = 0.5f;
    }

    public EnergyComponent.Type getType() { return type; }

    /**
     * Gets stored energy amount
     */
    public float getEnergy() {
        return energy;
    }

    /**
     * Gets max capacity of energy
     */
    public float getCapacity() {
        return capacity;
    }

    /**
     * Gets amount of energy generated/used per tick
     */
    public float getEnergyPerTick() {
        return energyPerTick;
    }

    /**
     * Gets amount of energy that can be stored and transferred per tick
     */
    public float getStorageRate() {
        return storageRate;
    }

    /**
     * Block tick sent from EnergySystem
     */
    public void onComponentTicked(float dt) {
    }

    public Vector3i getBlockPosition3d() {
        return blockPosition3d;
    }

    public void setBlockPosition3d(Vector3i blockPosition3d) {
        this.blockPosition3d = blockPosition3d;
    }

    /**
     * Insert energy.
     *
     * Cannot be more than capacity.
     */
    public synchronized float insert(float amount) {
        if (amount <= 0) return 0;
        float space = this.getCapacity() - this.getEnergy();
        float inserted = Math.min(space, amount);
        this.energy += inserted;
        return inserted;
    }

    /**
     * Extract energy.
     */
    public synchronized float extract(float amount) {
        if (amount <= 0) return 0;
        float extracted = Math.min(energy, amount);
        this.energy -= extracted;
        return extracted;
    }

    /**
     * Set energy.
     */
    public synchronized void setEnergy(float value) {
        if (value < 0) value = 0;
        if (value > capacity) value = capacity;
        this.energy = value;
    }

    /**
     * Sets amount of energy that can be stored and transferred per tick
     */
    public void setStorageRate(float rate) {
        this.storageRate = rate;
    }

    public static ComponentType<ChunkStore, EnergyComponent> getComponentType() {
        return Hytech.get().getEnergyComponentType();
    }

    // Finds saved component info
    @Nullable
    @Override
    public Component<ChunkStore> clone() {
        EnergyComponent clone = new EnergyComponent();
        clone.type = this.type;
        clone.energy = this.energy;
        clone.capacity = this.capacity;
        clone.energyPerTick = this.energyPerTick;
        clone.storageRate = this.storageRate;
        // copy connected positions snapshot
        synchronized (connectedGenerators) {
            clone.connectedGenerators.addAll(this.connectedGenerators);
        }
        clone.connectedTotalEnergy = this.connectedTotalEnergy;
        clone.connectedTotalCapacity = this.connectedTotalCapacity;
        clone.connectedTotalEnergyPerTick = this.connectedTotalEnergyPerTick;
        clone.connectedTotalStorageRate = this.connectedTotalStorageRate;
        return clone;
    }

    // ---------- Connected generators API ----------

    /**
     * Replace the set of connected generator positions. Caller should compute the set
     * (for example with a BFS over generator blocks) and provide it here. This method
     * does not perform world lookups.
     */
    public void setConnectedGenerators(@Nonnull Set<Vector3i> positions) {
        synchronized (connectedGenerators) {
            connectedGenerators.clear();
            connectedGenerators.addAll(positions);
        }
    }

    /**
     * Returns an immutable snapshot of connected generator positions.
     */
    @Nonnull
    public Set<Vector3i> getConnectedGeneratorsSnapshot() {
        synchronized (connectedGenerators) {
            return Collections.unmodifiableSet(new HashSet<>(connectedGenerators));
        }
    }

    /**
     * Clears connected generator list.
     */
    public void clearConnectedGenerators() {
        synchronized (connectedGenerators) {
            connectedGenerators.clear();
        }
        connectedTotalEnergy = 0f;
        connectedTotalCapacity = 0f;
        connectedTotalEnergyPerTick = 0f;
        connectedTotalStorageRate = 0f;
    }

    /**
     * Update cached aggregate values by looking up each position using the provided
     * lookup function. The lookup should return the EnergyComponent for the given
     * position or null if none. This allows systems to perform the world/component
     * access and then push a computed aggregate.
     *
     * Example lookup: pos -> world.getBlockComponentHolder(...).getComponent(EnergyComponent.getComponentType())
     */
    public void updateConnectedAggregates(@Nonnull Function<Vector3i, EnergyComponent> lookup) {
        float sumEnergy = 0f;
        float sumCapacity = 0f;
        float sumEnergyPerTick = 0f;
        float sumStorageRate = 0f;

        synchronized (connectedGenerators) {
            for (Vector3i pos : connectedGenerators) {
                try {
                    EnergyComponent ec = lookup.apply(pos);
                    if (ec == null) continue;
                    sumEnergy += ec.getEnergy();
                    sumCapacity += ec.getCapacity();
                    sumEnergyPerTick += ec.getEnergyPerTick();
                    sumStorageRate += ec.getStorageRate();
                } catch (Exception e) {
                    LOGGER.atWarning().log("Error looking up energy component for " + pos + ": " + e.getMessage());
                }
            }
        }

        connectedTotalEnergy = sumEnergy;
        connectedTotalCapacity = sumCapacity;
        connectedTotalEnergyPerTick = sumEnergyPerTick;
        connectedTotalStorageRate = sumStorageRate;
    }

    // Aggregate getters (from last update)
    public float getConnectedTotalEnergy() { return connectedTotalEnergy; }
    public float getConnectedTotalCapacity() { return connectedTotalCapacity; }
    public float getConnectedTotalEnergyPerTick() { return connectedTotalEnergyPerTick; }
    public float getConnectedTotalStorageRate() { return connectedTotalStorageRate; }
    public int getConnectedGeneratorCount() {
        synchronized (connectedGenerators) {
            return connectedGenerators.size();
        }
    }
}
