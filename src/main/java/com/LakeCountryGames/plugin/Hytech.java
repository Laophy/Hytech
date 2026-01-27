package com.LakeCountryGames.plugin;

import com.LakeCountryGames.plugin.commands.ExampleCommand;
import com.LakeCountryGames.plugin.events.SolarPlacedEvent;
import com.LakeCountryGames.plugin.interactions.ConfigureSolarInteraction;
import com.LakeCountryGames.plugin.power.PowerNetworkSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;

public class Hytech extends JavaPlugin {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    public Hytech(JavaPluginInit init) {
        super(init);
        LOGGER.atInfo().log("Hello from %s version %s", this.getName(), this.getManifest().getVersion().toString());
    }

    @Override
    protected void setup() {
        this.getCommandRegistry().registerCommand(new ExampleCommand(this.getName(), this.getManifest().getVersion().toString()));

        this.getCodecRegistry(Interaction.CODEC)
                .register("ConfigureSolar", ConfigureSolarInteraction.class, ConfigureSolarInteraction.CODEC);

        this.getEntityStoreRegistry().registerSystem(new PowerNetworkSystem());
        this.getEntityStoreRegistry().registerSystem(new SolarPlacedEvent());
    }
}
