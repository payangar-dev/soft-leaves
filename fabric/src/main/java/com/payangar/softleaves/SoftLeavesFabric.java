package com.payangar.softleaves;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;

public class SoftLeavesFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        // All behavior is implemented through the common mixins; the loader only has
        // to say where the config lives.
        SoftLeavesInit.init(FabricLoader.getInstance().getConfigDir());
    }
}
