package com.LakeCountryGames.plugin.power;

import com.hypixel.hytale.math.vector.Vector3i;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public class PowerNodeRegistry {
    private static final Map<Vector3i, PowerNode> NODES = new ConcurrentHashMap<>();

    public static void register(PowerNode node) {
        if (node == null || node.getPosition() == null) return;
        NODES.put(node.getPosition(), node);
    }

    public static PowerNode get(Vector3i pos) {
        return pos == null ? null : NODES.get(pos);
    }

    public static void unregister(Vector3i pos) {
        if (pos == null) return;
        NODES.remove(pos);
    }

    public static Collection<PowerNode> allNodes() {
        return NODES.values();
    }
}