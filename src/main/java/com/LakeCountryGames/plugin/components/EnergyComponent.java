package com.LakeCountryGames.plugin.components;

import com.LakeCountryGames.plugin.Hytech;
import com.LakeCountryGames.plugin.power.PowerNode;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import javax.annotation.Nullable;

/**
 * EnergyComponent -> Attaches to a block that can store/generate/transmit energy.
 *
 * Data Component, not a logical class
 */
public class EnergyComponent implements Component<ChunkStore> {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    // CODEC will map the json data that was saved
    // you must ensure the clone method is being used and not set to a new class.
    public static final BuilderCodec<EnergyComponent> CODEC = BuilderCodec.builder(
                    EnergyComponent.class,
                    EnergyComponent::new
            )
            .append(new KeyedCodec<>("Type", Codec.STRING),
                    (data, value) -> data.type = Type.valueOf(value),
                    data -> data.type.name())
            .add()
            .append(new KeyedCodec<>("Energy", Codec.INTEGER),
                    (data, value) -> data.energy = value,
                    data -> data.energy)
            .add()
            .append(new KeyedCodec<>("Capacity", Codec.INTEGER),
                    (data, value) -> data.capacity = value,
                    data -> data.capacity)
            .add()
            .append(new KeyedCodec<>("EnergyPerTick", Codec.INTEGER),
                    (data, value) -> data.energyPerTick = value,
                    data -> data.energyPerTick)
            .add()
            .build();

    public enum Type { GENERATOR, STORAGE, TRANSFER }

    private Type type;
    private int energy;
    private int capacity;
    private int energyPerTick;

    public EnergyComponent() {
        this.type = Type.STORAGE;
        this.energy = 0;
        this.capacity = 5000;
        this.energyPerTick = 1;
    }

    public EnergyComponent.Type getType() { return type; }

    public float getEnergy() {
        return energy;
    }

    public int getCapacity() {
        return capacity;
    }

    public int getEnergyPerTick() {
        return energyPerTick;
    }

    public int transferToTouchingStorage(int amount) {
        // Placeholder for future implementation
        return 0;
    }

    public void onComponentTicked(float dt) {
        // This block ticked!!!!
        //
        // Might be useful for future logic
    }

    /**
     * Insert energy.
     *
     * Cannot be more than capacity.
     */
    public synchronized int insert(int amount) {
        if (amount <= 0) return 0;
        int space = (int) (this.getCapacity() - this.getEnergy());
        int inserted = Math.min(space, amount);
        this.energy += inserted;


        return inserted;
    }

    /**
     * Extract energy.
     */
    public synchronized int extract(int amount) {
        if (amount <= 0) return 0;
        int extracted = Math.min(energy, amount);
        this.energy -= extracted;
        return extracted;
    }

    /**
     * Set energy.
     */
    public synchronized void setEnergy(int value) {
        if (value < 0) value = 0;
        if (value > capacity) value = capacity;
        this.energy = value;
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
        return clone;
    }
}