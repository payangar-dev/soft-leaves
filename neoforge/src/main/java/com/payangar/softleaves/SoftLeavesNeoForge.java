package com.payangar.softleaves;

import net.neoforged.fml.common.Mod;

@Mod(Constants.MOD_ID)
public class SoftLeavesNeoForge {

    public SoftLeavesNeoForge() {
        // All behavior is implemented through the common mixins.
        SoftLeavesInit.init();
    }
}
