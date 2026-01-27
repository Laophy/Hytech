package com.LakeCountryGames.plugin.power;

import com.hypixel.hytale.protocol.BlockPosition;

import java.util.ArrayList;
import java.util.List;
import java.util.Collection;

/**
 * Simple helper methods for balancing/distributing energy among nodes.
 * Connection discovery (scanning neighbouring wire blocks) should be done by your block placement/tick logic
 * — use PowerNodeRegistry to register nodes when blocks are placed and remove on break.
 */
public class PowerNetwork {

    /**
     * Evenly distributes available energy across all capacitor nodes in the provided collection.
     * Solar and wire nodes are ignored for storage balancing (wire nodes typically have zero capacity).
     */
    public static void balanceCapacitors(Collection<PowerNode> nodes) {
        List<PowerNode> caps = new ArrayList<>();
        int totalEnergy = 0;
        int totalCapacity = 0;

        for (PowerNode n : nodes) {
            if (n.getType() == PowerNode.Type.CAPACITOR) {
                caps.add(n);
                totalEnergy += n.getStorage().getEnergy();
                totalCapacity += n.getStorage().getCapacity();
            }
        }

        if (caps.isEmpty()) return;

        // Distribute proportionally by capacity
        for (PowerNode cap : caps) {
            int target = (int) ((long) totalEnergy * cap.getStorage().getCapacity() / Math.max(1, totalCapacity));
            cap.getStorage().setEnergy(target);
        }
    }

    /**
     * Pull energy from all solar nodes into connected capacitors (simple push model).
     * The caller should supply the list of nodes that form a connected network.
     */
    public static void pushSolarToCapacitors(Collection<PowerNode> nodes, int maxTransferPerTick) {
        List<PowerNode> solar = new ArrayList<>();
        List<PowerNode> caps = new ArrayList<>();

        for (PowerNode n : nodes) {
            if (n.getType() == PowerNode.Type.SOLAR) solar.add(n);
            if (n.getType() == PowerNode.Type.CAPACITOR) caps.add(n);
        }

        if (solar.isEmpty() || caps.isEmpty()) return;

        // collect total generated this tick from solar nodes
        int totalGenerated = 0;
        for (PowerNode s : solar) {
            int produced = s.getStorage().extract(maxTransferPerTick); // assume solar already generated into its own storage
            totalGenerated += produced;
        }

        if (totalGenerated <= 0) return;

        // simple round-robin fill capacitors
        int idx = 0;
        while (totalGenerated > 0) {
            PowerNode cap = caps.get(idx % caps.size());
            int inserted = cap.getStorage().insert(totalGenerated);
            totalGenerated -= inserted;
            idx++;
            // safety: break if no capacity in any cap
            if (idx >= caps.size() && caps.stream().allMatch(c -> c.getStorage().getEnergy() == c.getStorage().getCapacity())) break;
        }
    }
}