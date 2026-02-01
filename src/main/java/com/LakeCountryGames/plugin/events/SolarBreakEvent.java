//package com.LakeCountryGames.plugin.events;
//
//import com.LakeCountryGames.plugin.Hytech;
//import com.LakeCountryGames.plugin.components.EnergyComponent;
//import com.LakeCountryGames.plugin.power.PowerNode;
//import com.LakeCountryGames.plugin.power.PowerNodeRegistry;
//import com.hypixel.hytale.component.ArchetypeChunk;
//import com.hypixel.hytale.component.CommandBuffer;
//import com.hypixel.hytale.component.Ref;
//import com.hypixel.hytale.component.Store;
//import com.hypixel.hytale.component.query.Query;
//import com.hypixel.hytale.component.system.EntityEventSystem;
//import com.hypixel.hytale.logger.HytaleLogger;
//import com.hypixel.hytale.math.vector.Vector3i;
//import com.hypixel.hytale.server.core.Message;
//import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
//import com.hypixel.hytale.server.core.entity.entities.Player;
//import com.hypixel.hytale.server.core.event.events.ecs.BreakBlockEvent;
//import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
//import org.jetbrains.annotations.NotNull;
//import org.jetbrains.annotations.Nullable;
//
//// TODO: Convert to component system?
//public class SolarBreakEvent extends EntityEventSystem<EntityStore, BreakBlockEvent> {
//    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
//
//    public SolarBreakEvent() {
//        super(BreakBlockEvent.class);
//    }
//
//    @Override
//    public void handle(int i, @NotNull ArchetypeChunk<EntityStore> archetypeChunk, @NotNull Store<EntityStore> store, @NotNull CommandBuffer<EntityStore> commandBuffer, @NotNull BreakBlockEvent breakBlockEvent) {
//        // Get player
//        Ref<EntityStore> ref = archetypeChunk.getReferenceTo(i);
//        Player player = store.getComponent(ref, Player.getComponentType());
//
//        // Get block
//        if(player != null && player.getWorld() != null){
//            Vector3i blockPos = breakBlockEvent.getTargetBlock();
//            PowerNode brokenNode = PowerNodeRegistry.get(blockPos);
//
//            BlockType targetType = breakBlockEvent.getBlockType();
//            boolean isSolarBlock = targetType.getId().toLowerCase().contains("hytech_solar_block");
//
//            LOGGER.atInfo().log("targetType.getId().toLowerCase() " + targetType.getId().toLowerCase());
//            LOGGER.atInfo().log("isSolarBlock " + isSolarBlock);
//
//            // If we break a node, unregister it
//            if(isSolarBlock) {
//                commandBuffer.removeComponent(ref, Hytech.get().getEnergyComponentType());
//                if (brokenNode != null) {
//                    PowerNodeRegistry.unregister(brokenNode.getPosition()); // TODO: unregisters node location NOT BY VECTOR!!
//                    player.sendMessage(Message.raw("Unregistered solar node at " + blockPos));
//                    LOGGER.atInfo().log("Unregistered solar node at " + blockPos);
//                }
//            }
//        }
//    }
//
//    @Override
//    public @Nullable Query<EntityStore> getQuery() {
//        return Query.and();
//    }
//}
