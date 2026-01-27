package com.LakeCountryGames.plugin.interactions;

import com.LakeCountryGames.plugin.power.EnergyStorage;
import com.LakeCountryGames.plugin.power.PowerNode;
import com.LakeCountryGames.plugin.power.PowerNodeRegistry;
import com.hypixel.hytale.codec.builder.BuilderCodec;
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
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("removal")
public class ConfigureSolarInteraction extends SimpleInteraction {
    public static final BuilderCodec<ConfigureSolarInteraction> CODEC =
            BuilderCodec.builder(ConfigureSolarInteraction.class, ConfigureSolarInteraction::new,
                    SimpleInteraction.CODEC).build();

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final int TICKS_PER_SECOND = 20; // used if only per-tick generation is available

    // any time an interaction happens
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
        Vector3i targetVec = new Vector3i(targetBlock.x, targetBlock.y, targetBlock.z);
        if(targetBlock == null) return;

        if(type == InteractionType.Use) {
            PowerNode node = PowerNodeRegistry.get(targetVec);
            if (node == null) {
                player.sendMessage(Message.raw("No solar node registered at this block."));
                LOGGER.atInfo().log("Use: no solar node at " + targetBlock);
                return;
            }

            EnergyStorage storage = node.getStorage();
            long stored = Long.MIN_VALUE;
            if (storage != null) {
                stored = storage.getEnergy();
            }

            StringBuilder sb = new StringBuilder();
            sb.append("Solar node registered at this block.\n");
            if (stored != Long.MIN_VALUE) {
                sb.append("Stored energy: ").append(stored).append("\n");
            } else {
                sb.append("Stored energy: unknown\n");
            }

            player.sendMessage(Message.raw(sb.toString()));
            LOGGER.atInfo().log("Use: reported solar node info at " + targetBlock);
        }
    }
}
