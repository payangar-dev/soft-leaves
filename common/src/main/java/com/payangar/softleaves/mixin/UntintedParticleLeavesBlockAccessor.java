package com.payangar.softleaves.mixin;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.level.block.UntintedParticleLeavesBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(UntintedParticleLeavesBlock.class)
public interface UntintedParticleLeavesBlockAccessor {

    @Accessor("leafParticle")
    ParticleOptions softleaves$getLeafParticle();
}
