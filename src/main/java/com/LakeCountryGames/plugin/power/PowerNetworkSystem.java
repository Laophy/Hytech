package com.LakeCountryGames.plugin.power;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

public class PowerNetworkSystem extends EntityTickingSystem<EntityStore> {
    private static final HytaleLogger LOGGER = HytaleLogger.getLogger();

    // Desired generation rate for each solar node in energy units per second.
    private final double solarPerSecond = 1.0;

    public PowerNetworkSystem() {
        LOGGER.atInfo().log("PowerNetworkSystem initialized with solarPerSecond=" + solarPerSecond);
    }

    @Override
    public void tick(float dt, int index, @NonNull ArchetypeChunk<EntityStore> archetypeChunk, @NonNull Store<EntityStore> store, @NonNull CommandBuffer<EntityStore> commandBuffer) {
        // run generation once per system tick
        if (index != 0) return;

        double amountPerNodeThisTick = solarPerSecond * dt;
        if (amountPerNodeThisTick <= 0.0) return;

        int totalInserted = 0;
        for (PowerNode node : PowerNodeRegistry.allNodes()) {
            if (node.getType() == PowerNode.Type.SOLAR) {
                int inserted = node.generateFractional(amountPerNodeThisTick);
                totalInserted += inserted;
            }
        }

        // Only log when actual energy was inserted (avoids spamming when storages are full
        // or when fractional accumulation hasn't produced a whole unit yet).
        if (totalInserted > 0) {
            //LOGGER.atInfo().log("PowerNetworkSystem: generated " + totalInserted + " total for solar nodes (dt=" + dt + ")");
        }
    }

    @Override
    public @Nullable Query<EntityStore> getQuery() {
        return Query.and();
    }
}