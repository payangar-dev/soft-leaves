package com.payangar.softleaves.client;

import com.payangar.softleaves.mixin.client.ParticleAccessor;
import net.minecraft.client.particle.Particle;

public final class LeafFling {

    private LeafFling() {
    }

    /**
     * Vanilla falling-leaf particle providers discard the velocity they are given,
     * and the particle itself has friction 1.0 and near-zero gravity, so a flung
     * leaf would rise forever. Ambient leaves (spawned with zero velocity) are left
     * untouched; leaves flung by Soft Leaves get the velocity plus real-ish physics:
     * air drag bleeds the fling off in ~10 ticks, then they flutter back down.
     */
    public static void apply(Particle particle, double vx, double vy, double vz) {
        if (particle == null || (vx == 0.0 && vy == 0.0 && vz == 0.0)) {
            return;
        }
        particle.setParticleSpeed(vx, vy, vz);
        ((ParticleAccessor) particle).softleaves$setGravity(0.012F);
        ((ParticleAccessor) particle).softleaves$setFriction(0.92F);
    }
}
