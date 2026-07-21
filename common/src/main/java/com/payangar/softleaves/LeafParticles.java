package com.payangar.softleaves;

import com.payangar.softleaves.mixin.UntintedParticleLeavesBlockAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.FoliageColor;
import net.minecraft.world.level.block.UntintedParticleLeavesBlock;
import net.minecraft.world.level.block.state.BlockState;

public final class LeafParticles {

    private LeafParticles() {
    }

    /**
     * Resolves the same falling-leaf particle the given leaves block uses for its
     * ambient effect, but server-side. Cherry and pale oak carry their own particle;
     * everything else gets a biome-tinted leaf. On dedicated servers the foliage
     * colormap is not loaded and resolves to 0, hence the default-color fallback.
     */
    public static ParticleOptions resolve(BlockState state, ServerLevel level, BlockPos pos) {
        if (state.getBlock() instanceof UntintedParticleLeavesBlock block) {
            return ((UntintedParticleLeavesBlockAccessor) block).softleaves$getLeafParticle();
        }
        int color = level.getBiome(pos).value().getFoliageColor();
        if ((color & 0xFFFFFF) == 0) {
            color = FoliageColor.FOLIAGE_DEFAULT;
        }
        return ColorParticleOption.create(ParticleTypes.TINTED_LEAVES, color);
    }
}
