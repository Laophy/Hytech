package com.LakeCountryGames.plugin.components;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;

public class GeneratorMarker implements Component<ChunkStore> {
    public static final BuilderCodec<GeneratorMarker> CODEC =
            BuilderCodec.builder(GeneratorMarker.class, GeneratorMarker::new).build();

    public GeneratorMarker() {}

    @Override
    public Component<ChunkStore> clone() {
        return new GeneratorMarker();
    }
}