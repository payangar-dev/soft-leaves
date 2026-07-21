package com.payangar.softleaves.mixin.client;

import net.minecraft.client.particle.Particle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Particle.class)
public interface ParticleAccessor {

    @Accessor("gravity")
    void softleaves$setGravity(float gravity);

    @Accessor("friction")
    void softleaves$setFriction(float friction);
}
