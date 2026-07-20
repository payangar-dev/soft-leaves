package com.payangar.softleaves;

import net.fabricmc.api.ModInitializer;

public class SoftLeavesFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        // All behavior is implemented through the common mixins.
        SoftLeavesInit.init();
    }
}
