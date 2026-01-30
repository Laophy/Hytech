package com.LakeCountryGames.plugin.interactions;

import com.LakeCountryGames.plugin.Hytech;
import com.LakeCountryGames.plugin.components.EnergyComponent;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.protocol.BlockPosition;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInteraction;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jetbrains.annotations.NotNull;

public class ConfigureSolarInteraction extends SimpleInteraction {
    public static final BuilderCodec<ConfigureSolarInteraction> CODEC =
            BuilderCodec.builder(ConfigureSolarInteraction.class, ConfigureSolarInteraction::new,
                    SimpleInteraction.CODEC).build();

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    // The tick that an interaction happened in (mid tick)
    @Override
    protected void tick0(boolean firstRun, float time, @NotNull InteractionType type, @NotNull InteractionContext context, @NotNull CooldownHandler cooldownHandler) {
        // The player who interacted with the block
        Ref<EntityStore> owningEntity = context.getOwningEntity();
        Store<EntityStore> store = owningEntity.getStore();

        // Player store
        Player player = store.getComponent(owningEntity, Player.getComponentType());
        if(player == null) return;

        // World of the player
        World world = player.getWorld();
        if(world == null) return;

        // Get the block data we interacted with
        BlockPosition targetBlock = context.getTargetBlock();
        assert targetBlock != null;
        Vector3i targetVec = new Vector3i(targetBlock.x, targetBlock.y, targetBlock.z);

        if(type == InteractionType.Use) {
            // Get energyComponent from the chunkStore
            WorldChunk worldChunk = world.getChunk(ChunkUtil.indexChunkFromBlock(targetVec.x, targetVec.z));
            assert worldChunk != null;

            Ref<ChunkStore> componentRef = worldChunk.getBlockComponentEntity(targetVec.x, targetVec.y, targetVec.z);
            assert componentRef != null;

            // Get EnergyComponent from/with chunkstore ref
            EnergyComponent energyComponent = world.getChunkStore().getStore().getComponent(componentRef, Hytech.get().getEnergyComponentType());

            // Does the interacted block have energy?
            if(energyComponent != null) {
                player.sendMessage(Message.raw("------------------------------------------").color("#FF0000").bold(true));

                player.sendMessage(Message.raw("Current energy: " + energyComponent.getEnergy()));
                player.sendMessage(Message.raw("Type: " + energyComponent.getType().toString()));

                player.sendMessage(Message.raw("Total Touching Generator: " + energyComponent.getConnectedGeneratorCount()));
                player.sendMessage(Message.raw("Total Touching Generator Power To transfer: " + energyComponent.getConnectedTotalEnergyPerTick()));
                player.sendMessage(Message.raw("Total Touching Generator Energy: " + energyComponent.getConnectedTotalEnergy()));

                player.sendMessage(Message.raw("------------------------------------------").color("#FF0000").bold(true));
            } else {
                player.sendMessage(Message.raw("This block does NOT have an Energy Component!"));
            }
        }
    }
}
