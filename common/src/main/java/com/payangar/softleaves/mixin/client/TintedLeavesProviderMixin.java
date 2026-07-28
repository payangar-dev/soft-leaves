package com.payangar.softleaves.mixin.client;

import com.payangar.softleaves.client.LeafFling;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.FallingLeavesParticle;
import net.minecraft.client.particle.Particle;
import net.minecraft.core.particles.ColorParticleOption;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FallingLeavesParticle.TintedLeavesProvider.class)
public abstract class TintedLeavesProviderMixin {

    @Inject(method = "createParticle", at = @At("RETURN"))
    private void softleaves$fling(
        ColorParticleOption options, ClientLevel level, double x, double y, double z,
        double xAux, double yAux, double zAux, CallbackInfoReturnable<Particle> cir
    ) {
        LeafFling.apply(cir.getReturnValue(), xAux, yAux, zAux);
    }
}
