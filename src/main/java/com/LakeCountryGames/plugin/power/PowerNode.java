package com.LakeCountryGames.plugin.power;

import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.BlockPosition;

public class PowerNode {
    public enum Type { SOLAR, WIRE, CAPACITOR, OTHER }

    private final Vector3i position;
    private final Type type;
    private final EnergyStorage storage;

    // accumulator for fractional generation (units are energy units)
    private double generationAccumulator = 0.0;

    public PowerNode(Vector3i position, Type type, int capacity) {
        this.position = position;
        this.type = type;
        this.storage = new EnergyStorage(capacity);
    }

    public Vector3i getPosition() { return position; }
    public Type getType() { return type; }
    public EnergyStorage getStorage() { return storage; }

    /**
     * Backwards-compatible: called with a whole number amount (int).
     * Returns the amount actually inserted.
     */
    public synchronized int tickGenerate(int solarPerTick) {
        if (type != Type.SOLAR || solarPerTick <= 0) return 0;
        return storage.insert(solarPerTick);
    }

    /**
     * Accumulate fractional generation (e.g. solarPerSecond * dt). When
     * the accumulator reaches 1.0 or more, insert the whole units into storage.
     * Returns the amount actually inserted into storage this call.
     *
     * The accumulator is only reduced by the amount actually inserted so energy
     * isn't lost when storage is full.
     */
    public synchronized int generateFractional(double amount) {
        if (type != Type.SOLAR || amount <= 0.0) return 0;
        generationAccumulator += amount;
        int toGenerate = (int) Math.floor(generationAccumulator);
        if (toGenerate <= 0) return 0;

        int inserted = storage.insert(toGenerate);
        if (inserted > 0) {
            // only subtract the portion that was actually stored
            generationAccumulator -= inserted;
        }
        // if nothing inserted, keep accumulator intact so energy is not lost
        return inserted;
    }
}