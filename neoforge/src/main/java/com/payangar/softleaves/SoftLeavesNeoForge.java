package com.payangar.softleaves;

import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;

@Mod(Constants.MOD_ID)
public class SoftLeavesNeoForge {

    public SoftLeavesNeoForge() {
        // All behavior is implemented through the common mixins; the loader only has
        // to say where the config lives.
        SoftLeavesInit.init(FMLPaths.CONFIGDIR.get());
    }
}
