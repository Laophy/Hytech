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
//import com.hypixel.hytale.protocol.BlockPosition;
//import com.hypixel.hytale.server.core.Message;
//import com.hypixel.hytale.server.core.entity.entities.Player;
//import com.hypixel.hytale.server.core.event.events.ecs.PlaceBlockEvent;
//import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
//import org.jetbrains.annotations.NotNull;
//import org.jetbrains.annotations.Nullable;
//
//import java.awt.*;
//
//// TODO: Convert to component system?
//@SuppressWarnings("removal")
//public class SolarPlacedEvent extends EntityEventSystem<EntityStore, PlaceBlockEvent> {
//    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
//
//    public SolarPlacedEvent() {
//        super(PlaceBlockEvent.class);
//    }
//
//    @Override
//    public void handle(int i, @NotNull ArchetypeChunk<EntityStore> archetypeChunk, @NotNull Store<EntityStore> store, @NotNull CommandBuffer<EntityStore> commandBuffer, @NotNull PlaceBlockEvent placeBlockEvent) {
////        // Get player
////        Ref<EntityStore> ref = archetypeChunk.getReferenceTo(i);
////        Player player = store.getComponent(ref, Player.getComponentType());
////
////        // Get block
////        if(player != null && player.getWorld() != null){
////            String placedItemId = placeBlockEvent.getItemInHand().getItemId();
////            Vector3i blockPos = placeBlockEvent.getTargetBlock();
////
////            if (placedItemId.toLowerCase().contains("hytech_solar_block")) {
////                // Register solar panel
////                //PowerNode existing = PowerNodeRegistry.get(blockPos);
////
//////                EnergyComponent energy = new EnergyComponent();
//////
//////                // Change ref to get the ref of the store
//////                store.addComponent(ref, Hytech.get().getEnergyComponentType(), energy);
//////                player.sendMessage(Message.raw("You have registered a new COMPONENT solar block!").color(Color.GREEN).bold(true));
////
////               // commandBuffer.addComponent(ref, Hytech.get().getEnergyComponentType(), new EnergyComponent());
////
////
//////                if (existing == null) {
//////                    final int capacity = 5000; // TODO: Make configurable
//////                    PowerNode node = new PowerNode(blockPos, PowerNode.Type.SOLAR, capacity);
//////                    PowerNodeRegistry.register(node);
//////                    player.sendMessage(Message.raw("Registered solar node (capacity: " + capacity + ")"));
//////                    LOGGER.atInfo().log("Registered solar node at " + blockPos + " with capacity " + capacity);
//////                } else {
//////                    // Should never happen?
//////                    player.sendMessage(Message.raw("Solar node already registered at this block."));
//////                    LOGGER.atInfo().log("Attempted to register solar node but one already exists at " + blockPos);
//////                }
////            } else if (placedItemId.toLowerCase().contains("hytech_capacitor_block")){
////                // Register capacitor block
////                PowerNode existing = PowerNodeRegistry.get(blockPos);
////
////                if (existing == null) {
////                    final int capacity = 20000; // TODO: Make configurable
////                    PowerNode node = new PowerNode(blockPos, PowerNode.Type.CAPACITOR, capacity);
////                    PowerNodeRegistry.register(node);
////                    player.sendMessage(Message.raw("Registered capacitor node (capacity: " + capacity + ")"));
////                    LOGGER.atInfo().log("Registered capacitor node at " + blockPos + " with capacity " + capacity);
////                } else {
////                    // Should never happen?
////                    player.sendMessage(Message.raw("Capacitor node already registered at this block."));
////                    LOGGER.atInfo().log("Attempted to register capacitor node but one already exists at " + blockPos);
////                }
////            }
////
////        } else {
////            // Something went VERY wrong with player or block
////            throw new IllegalStateException("Player or World is null when placing a block.");
////        }
//    }
//
//    @Override
//    public @Nullable Query<EntityStore> getQuery() {
//        return Query.and();
//    }
//}
