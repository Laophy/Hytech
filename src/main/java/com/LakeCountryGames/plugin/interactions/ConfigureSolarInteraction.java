package com.LakeCountryGames.plugin.interactions;

import com.LakeCountryGames.plugin.Hytech;
import com.LakeCountryGames.plugin.components.EnergyComponent;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.BlockPosition;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInteraction;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jetbrains.annotations.NotNull;

public class ConfigureSolarInteraction extends SimpleInteraction {
    public static final BuilderCodec<ConfigureSolarInteraction> CODEC =
            BuilderCodec.builder(ConfigureSolarInteraction.class, ConfigureSolarInteraction::new,
                    SimpleInteraction.CODEC).build();

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    @Override
    protected void tick0(boolean firstRun, float time, @NotNull InteractionType type, @NotNull InteractionContext context, @NotNull CooldownHandler cooldownHandler) {
        Ref<EntityStore> owningEntity = context.getOwningEntity();
        Store<EntityStore> store = owningEntity.getStore();

        Player player = store.getComponent(owningEntity, Player.getComponentType());
        if (player == null) {
            return;
        }

        World world = player.getWorld();
        if (world == null) {
            player.sendMessage(Message.raw("LOST A REF???! - world").color("#FF0000").bold(true));
            return;
        }

        BlockPosition targetBlock = context.getTargetBlock();
        if (targetBlock == null) {
            player.sendMessage(Message.raw("LOST A REF???! - targetBlock").color("#FF0000").bold(true));
            return;
        }
        Vector3i targetVec = new Vector3i(targetBlock.x, targetBlock.y, targetBlock.z);

        if (type == InteractionType.Use) {
            // Use the world's Holder API which is much cheaper than forcing a chunk/ref lookup.
            Holder<ChunkStore> holder = world.getBlockComponentHolder(targetVec.x, targetVec.y, targetVec.z);
            if (holder == null) {
                player.sendMessage(Message.raw("No data for that block (chunk/unloaded).").color("#FF0000").bold(true));
                return;
            }

            EnergyComponent energyComponent = holder.getComponent(Hytech.get().getEnergyComponentType());
            if (energyComponent == null) {
                player.sendMessage(Message.raw("No energy component on that block.").color("#FF0000").bold(true));
                return;
            }

            // Display lightweight cached values only (avoid heavy recompute here)
            player.sendMessage(Message.raw("------------------------------------------").color("#FF0000").bold(true));
            player.sendMessage(Message.raw("Type: " + energyComponent.getType()).color("#3687C2").bold(true));
            player.sendMessage(Message.raw("Current energy: " + energyComponent.getEnergy() + " / " + energyComponent.getCapacity()));
            if (energyComponent.getType() == EnergyComponent.Type.GENERATOR) {
                player.sendMessage(Message.raw("Generators connected: " + energyComponent.getConnectedGeneratorCount()));
                player.sendMessage(Message.raw("Total generation/tick (cached): " + energyComponent.getConnectedTotalEnergyPerTick()));
            }
            player.sendMessage(Message.raw("------------------------------------------").color("#FF0000").bold(true));
        }
    }
}
