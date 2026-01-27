package com.LakeCountryGames.plugin.power;

public class EnergyStorage {
    private final int capacity;
    private int energy;

    public EnergyStorage(int capacity) {
        if (capacity < 0) throw new IllegalArgumentException("capacity < 0");
        this.capacity = capacity;
        this.energy = 0;
    }

    public synchronized int getEnergy() {
        return energy;
    }

    public synchronized int getCapacity() {
        return capacity;
    }

    /**
     * Attempts to insert energy. Returns the amount actually inserted.
     */
    public synchronized int insert(int amount) {
        if (amount <= 0) return 0;
        int space = capacity - energy;
        int inserted = Math.min(space, amount);
        energy += inserted;
        return inserted;
    }

    /**
     * Attempts to extract energy. Returns the amount actually extracted.
     */
    public synchronized int extract(int amount) {
        if (amount <= 0) return 0;
        int extracted = Math.min(energy, amount);
        energy -= extracted;
        return extracted;
    }

    public synchronized void setEnergy(int value) {
        if (value < 0) value = 0;
        if (value > capacity) value = capacity;
        this.energy = value;
    }
}