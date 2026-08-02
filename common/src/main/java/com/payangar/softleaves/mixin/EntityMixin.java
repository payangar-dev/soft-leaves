package com.payangar.softleaves.mixin;

import com.payangar.softleaves.LeafDrag;
import com.payangar.softleaves.LeafParticles;
import com.payangar.softleaves.SoftLeavesConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(Entity.class)
public abstract class EntityMixin implements LeafDrag {

    // Fraction of velocity removed per tick inside foliage: BASE + PER_SPEED * |velocity|,
    // capped at MAX so a terminal-velocity fall is braked hard but never stopped dead.
    @Unique
    private static final double SOFTLEAVES$BASE_DRAG = 0.30;
    @Unique
    private static final double SOFTLEAVES$DRAG_PER_SPEED = 0.15;
    @Unique
    private static final double SOFTLEAVES$MAX_DRAG = 0.70;
    // Fully buried in foliage (leaves at both feet and eye level) brakes harder.
    @Unique
    private static final double SOFTLEAVES$BURIED_MULTIPLIER = 1.8;
    @Unique
    private static final double SOFTLEAVES$MAX_DRAG_BURIED = 0.90;
    // The configured resistance scales the whole drag, so it has its own cap above the
    // tuned ones: foliage must keep letting the mover through, never hold it in place.
    @Unique
    private static final double SOFTLEAVES$MAX_DRAG_CONFIGURED = 0.95;
    // Sneaking while falling is a dive: the entity slips between the leaves
    // instead of being caught by them. What is left of the drag after this
    // multiplier is the light resistance of the dive.
    @Unique
    private static final double SOFTLEAVES$DIVE_DRAG_MULTIPLIER = 0.30;
    // Below this speed the movement is too subtle to rustle or shake off leaves;
    // low enough that sneaking through foliage still sheds the occasional leaf.
    @Unique
    private static final double SOFTLEAVES$MIN_EFFECT_SPEED = 0.02;
    // Entering foliage at or above this speed plays the leaf break sound
    // (audio only, the block is untouched). Sprint tops out around 0.3,
    // so only real falls and elytra crashes qualify.
    @Unique
    private static final double SOFTLEAVES$CRASH_SPEED = 0.5;
    // Leaf particles per tick = speed * (LINEAR + QUADRATIC * speed), stochastically
    // rounded and capped below: sparse while walking or running (a leaf every few
    // ticks), only a genuine high fall reaches burst territory.
    @Unique
    private static final double SOFTLEAVES$COUNT_LINEAR = 2.0;
    @Unique
    private static final double SOFTLEAVES$COUNT_QUADRATIC = 6.0;
    @Unique
    private static final int SOFTLEAVES$MAX_PARTICLES = 16;
    // Upward fling: gentle hops at walking and running speeds, flattening above so
    // a small jump lofts leaves two-ish blocks while only a long fall sends them
    // high, hard-capped. Min-of-two-curves shape.
    @Unique
    private static final double SOFTLEAVES$FLING_LOW_SLOPE = 1.4;
    @Unique
    private static final double SOFTLEAVES$FLING_HIGH_BASE = 0.15;
    @Unique
    private static final double SOFTLEAVES$FLING_HIGH_SLOPE = 0.18;
    @Unique
    private static final double SOFTLEAVES$FLING_CAP = 0.7;

    @Unique
    private int softleaves$lastLeafDragTick = Integer.MIN_VALUE;

    @Override
    public void softleaves$applyLeafDrag(BlockState state, Level level, BlockPos pos) {
        Entity self = (Entity) (Object) this;
        // Several leaves blocks can report the entity inside during the same tick;
        // the drag must only be applied once or it would compound per block.
        if (self.tickCount == this.softleaves$lastLeafDragTick) {
            return;
        }

        Vec3 delta = self.getDeltaMovement();
        // Actual movement this tick, not deltaMovement: a grounded entity keeps a
        // residual gravity delta (~0.08/tick) that would fake motion while standing
        // still. Same pattern as vanilla SweetBerryBushBlock.
        Vec3 movement = self.isClientAuthoritative()
            ? self.getKnownMovement()
            : self.position().subtract(self.oldPosition());
        double speed = movement.length();
        if (speed < 1.0E-4) {
            return;
        }

        // Not inside foliage on the previous tick means this is a fresh entry;
        // an air gap between two canopy layers counts as a new entry too.
        boolean entering = self.tickCount != this.softleaves$lastLeafDragTick + 1;
        this.softleaves$lastLeafDragTick = self.tickCount;

        // Only mid-fall: sneaking on the ground keeps costing speed, as it should.
        // The shift key state, not the crouching pose, so a dive still counts when
        // the entity has no room to actually crouch.
        boolean diving = self.isShiftKeyDown() && movement.y < 0.0 && !self.onGround();

        boolean buried = level.getBlockState(self.blockPosition()).getBlock() instanceof LeavesBlock
            && level.getBlockState(BlockPos.containing(self.getEyePosition())).getBlock() instanceof LeavesBlock;
        double drag = SOFTLEAVES$BASE_DRAG + SOFTLEAVES$DRAG_PER_SPEED * speed;
        if (buried) {
            drag *= SOFTLEAVES$BURIED_MULTIPLIER;
        }
        drag = Math.min(buried ? SOFTLEAVES$MAX_DRAG_BURIED : SOFTLEAVES$MAX_DRAG, drag);
        if (diving) {
            drag *= SOFTLEAVES$DIVE_DRAG_MULTIPLIER;
        }
        drag = Math.min(SOFTLEAVES$MAX_DRAG_CONFIGURED, drag * SoftLeavesConfig.resistance());
        self.setDeltaMovement(delta.scale(1.0 - drag));
        // Momentum absorbed by the foliage also softens the eventual landing, but
        // a dive cuts the canopy open instead of resting on it: the fall distance
        // is kept whole, so diving in from too high still hurts on impact.
        if (!diving && movement.y < 0.0 && self.fallDistance > 0.0) {
            self.fallDistance = self.fallDistance * (1.0 - drag);
        }

        if (level instanceof ServerLevel serverLevel && speed > SOFTLEAVES$MIN_EFFECT_SPEED) {
            SoundType sound = state.getSoundType();
            if (entering && speed >= SOFTLEAVES$CRASH_SPEED) {
                // Crash through the foliage: break sound only, the block stays intact.
                float volume = (float) Math.min(1.0, 0.4 + speed * 0.2);
                float pitch = sound.getPitch() * (0.9F + self.getRandom().nextFloat() * 0.2F);
                level.playSound(null, pos, sound.getBreakSound(), SoundSource.BLOCKS, volume, pitch);
            } else if (self.getRandom().nextFloat() < (float) Math.min(1.0, speed)) {
                float volume = (float) Math.min(0.55, 0.1 + speed * 0.35);
                float pitch = sound.getPitch() * (0.9F + self.getRandom().nextFloat() * 0.2F);
                level.playSound(null, pos, sound.getStepSound(), SoundSource.BLOCKS, volume, pitch);
            }

            // Falling-leaf particles of the traversed foliage, flung upward and
            // sideways. Speed scales both the count and the fling velocity; the
            // client-side provider mixins give flung leaves their ballistics.
            double expected = speed * (SOFTLEAVES$COUNT_LINEAR + SOFTLEAVES$COUNT_QUADRATIC * speed);
            int count = (int) expected;
            if (self.getRandom().nextFloat() < expected - count) {
                count++;
            }
            if (count > 0) {
                ParticleOptions leafParticle = LeafParticles.resolve(state, serverLevel, pos);
                double sideSpread = Math.min(speed * 2.0, 1.6);
                double flingBase = Math.min(speed * SOFTLEAVES$FLING_LOW_SLOPE,
                    SOFTLEAVES$FLING_HIGH_BASE + speed * SOFTLEAVES$FLING_HIGH_SLOPE);
                AABB box = self.getBoundingBox();
                for (int i = 0; i < Math.min(count, SOFTLEAVES$MAX_PARTICLES); i++) {
                    // Random point on the entity, clamped into the touched block:
                    // leaves shed from where the mover actually brushes the foliage.
                    double px = Math.max(pos.getX(), Math.min(pos.getX() + 1.0,
                        box.minX + self.getRandom().nextDouble() * (box.maxX - box.minX)));
                    double py = Math.max(pos.getY(), Math.min(pos.getY() + 1.0,
                        box.minY + self.getRandom().nextDouble() * (box.maxY - box.minY)));
                    double pz = Math.max(pos.getZ(), Math.min(pos.getZ() + 1.0,
                        box.minZ + self.getRandom().nextDouble() * (box.maxZ - box.minZ)));
                    double vy = Math.min(SOFTLEAVES$FLING_CAP, flingBase * (0.8 + self.getRandom().nextDouble() * 0.5));
                    // Keep the fling in an upward cone: sideways never beats upward,
                    // so no leaf darts off near-horizontally.
                    double maxSide = vy * 0.7;
                    double vx = Math.max(-maxSide, Math.min(maxSide,
                        movement.x * 0.4 + (self.getRandom().nextDouble() - 0.5) * sideSpread));
                    double vz = Math.max(-maxSide, Math.min(maxSide,
                        movement.z * 0.4 + (self.getRandom().nextDouble() - 0.5) * sideSpread));
                    serverLevel.sendParticles(leafParticle, px, py, pz, 0, vx, vy, vz, 1.0);
                }
            }
        }
    }
}
