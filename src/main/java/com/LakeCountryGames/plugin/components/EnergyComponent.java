package com.LakeCountryGames.plugin.components;

import com.LakeCountryGames.plugin.Hytech;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import javax.annotation.Nullable;

/**
 * EnergyComponent -> Attaches to a block that can store/generate/transmit energy.
 *
 * Data Component, not a logical class
 */
public class EnergyComponent implements Component<ChunkStore> {
    public static final BuilderCodec CODEC;
    private enum Type { SOLAR, WIRE, CAPACITOR, OTHER }

    private final Type type;
    private int energy;
    private int capacity; // TODO: Upgrade storage?
    private float energyPerSecond;
    private float tickInterval;


    public EnergyComponent() {
        this.type = Type.SOLAR;
        this.energy = 0;
        this.capacity = 5000; // Default capacity
        this.energyPerSecond = 1.0f; // Default energy Per Tick
        this.tickInterval = 1.0f; // Default tick interval in seconds
    }

    public EnergyComponent(Type type, int energy, int capacity, float energyPerSecond, float tickInterval) {
        this.type = type;
        this.energy = energy;
        this.capacity = capacity;
        this.energyPerSecond = energyPerSecond;
        this.tickInterval = tickInterval;
    }

    public static ComponentType getComponentType() {
        return Hytech.get().getEnergyComponentType();
    }

    @Nullable
    @Override
    public Component<ChunkStore> clone() {
        return new EnergyComponent();
    }

    static {
        CODEC = BuilderCodec.builder(EnergyComponent.class, EnergyComponent::new).build();
    }

    public float getEnergy() {
        return energy;
    }

    public int getCapacity() {
        return capacity;
    }

    public float getEnergyPerSecond() {
        return energyPerSecond;
    }

    public float getTickInterval() {
        return tickInterval;
    }

    // TODO: REMOVE FUNCTIONS BELOW AND MOVE TO SYSTEM
    //
    //
    // TODO: REMOVE FUNCTIONS BELOW AND MOVE TO SYSTEM

    /**
     * Insert energy.
     *
     * Cannot be more than capacity.
     */
    public synchronized int insert(int amount) {
        if (amount <= 0) return 0;
        int space = capacity - energy;
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
}