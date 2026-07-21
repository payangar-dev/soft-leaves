package com.payangar.softleaves.mixin.client;

import com.payangar.softleaves.client.LeafFling;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.FallingLeavesParticle;
import net.minecraft.client.particle.Particle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FallingLeavesParticle.PaleOakProvider.class)
public abstract class PaleOakProviderMixin {

    @Inject(method = "createParticle", at = @At("RETURN"))
    private void softleaves$fling(
        SimpleParticleType options, ClientLevel level, double x, double y, double z,
        double xAux, double yAux, double zAux, RandomSource random, CallbackInfoReturnable<Particle> cir
    ) {
        LeafFling.apply(cir.getReturnValue(), xAux, yAux, zAux);
    }
}
