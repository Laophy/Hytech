package com.LakeCountryGames.plugin;

import com.LakeCountryGames.plugin.commands.ExampleCommand;
import com.LakeCountryGames.plugin.components.EnergyComponent;
import com.LakeCountryGames.plugin.events.SolarBreakEvent;
import com.LakeCountryGames.plugin.events.SolarPlacedEvent;
import com.LakeCountryGames.plugin.interactions.ConfigureSolarInteraction;
import com.LakeCountryGames.plugin.power.PowerNetworkSystem;
import com.LakeCountryGames.plugin.refs.EnergyInitializer;
import com.LakeCountryGames.plugin.systems.EnergySystem;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class Hytech extends JavaPlugin {
    private static Hytech instance;
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private ComponentType energyComponentType;

    private ComponentType<ChunkStore, EnergyComponent> energyComponent;

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

        // TODO: Convert to ECS systems
        // Register event systems (LEGACY??????)
        this.getEntityStoreRegistry().registerSystem(new PowerNetworkSystem());
        this.getEntityStoreRegistry().registerSystem(new SolarPlacedEvent());
        this.getEntityStoreRegistry().registerSystem(new SolarBreakEvent());


        this.energyComponentType = this.getChunkStoreRegistry().registerComponent(EnergyComponent.class, "EnergySystem", EnergyComponent.CODEC);

        // System
        //this.getEntityStoreRegistry().registerSystem(new EnergySystem(this.energyComponent));
    }

    @Override
    protected void start() {
        this.getChunkStoreRegistry().registerSystem(new EnergySystem());
        this.getChunkStoreRegistry().registerSystem(new EnergyInitializer());
    }

    public ComponentType getEnergyComponentType() {
        return this.energyComponentType;
    }

    public static Hytech get() {
        return instance;
    }
}
