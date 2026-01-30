package com.LakeCountryGames.plugin;

import com.LakeCountryGames.plugin.commands.ExampleCommand;
import com.LakeCountryGames.plugin.components.EnergyComponent;
import com.LakeCountryGames.plugin.interactions.ConfigureSolarInteraction;
import com.LakeCountryGames.plugin.refs.EnergyInitializer;
import com.LakeCountryGames.plugin.systems.EnergySystem;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;

public class Hytech extends JavaPlugin {
    private static Hytech instance;
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private ComponentType<ChunkStore, EnergyComponent> energyComponentType;


    public Hytech(JavaPluginInit init) {
        super(init);
        instance = this;

        LOGGER.atInfo().log("Hello from %s version %s", this.getName(), this.getManifest().getVersion().toString());
    }

    @Override
    protected void setup() {
        this.getCommandRegistry().registerCommand(new ExampleCommand(this.getName(), this.getManifest().getVersion().toString()));

        this.getCodecRegistry(Interaction.CODEC)
                .register("ConfigureSolar", ConfigureSolarInteraction.class, ConfigureSolarInteraction.CODEC);

        this.energyComponentType = this.getChunkStoreRegistry().registerComponent(EnergyComponent.class, "EnergyComponent", EnergyComponent.CODEC);
    }

    @Override
    protected void start() {
        this.getChunkStoreRegistry().registerSystem(new EnergySystem());
        this.getChunkStoreRegistry().registerSystem(new EnergyInitializer());
    }

    public ComponentType<ChunkStore, EnergyComponent> getEnergyComponentType() {
        return this.energyComponentType;
    }

    public static Hytech get() {
        return instance;
    }
}
