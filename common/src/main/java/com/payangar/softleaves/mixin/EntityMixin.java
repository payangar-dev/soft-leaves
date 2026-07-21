package com.payangar.softleaves.mixin;

import com.payangar.softleaves.LeafDrag;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.SoundType;
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
    // Below this speed the movement is too subtle to rustle the leaves.
    @Unique
    private static final double SOFTLEAVES$MIN_SOUND_SPEED = 0.05;
    // Entering foliage at or above this speed plays the leaf break sound
    // (audio only, the block is untouched). Sprint tops out around 0.3,
    // so only real falls and elytra crashes qualify.
    @Unique
    private static final double SOFTLEAVES$CRASH_SPEED = 0.5;

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
        double speed = delta.length();
        if (speed < 1.0E-4) {
            return;
        }

        // Not inside foliage on the previous tick means this is a fresh entry;
        // an air gap between two canopy layers counts as a new entry too.
        boolean entering = self.tickCount != this.softleaves$lastLeafDragTick + 1;
        this.softleaves$lastLeafDragTick = self.tickCount;

        boolean buried = level.getBlockState(self.blockPosition()).getBlock() instanceof LeavesBlock
            && level.getBlockState(BlockPos.containing(self.getEyePosition())).getBlock() instanceof LeavesBlock;
        double drag = SOFTLEAVES$BASE_DRAG + SOFTLEAVES$DRAG_PER_SPEED * speed;
        if (buried) {
            drag *= SOFTLEAVES$BURIED_MULTIPLIER;
        }
        drag = Math.min(buried ? SOFTLEAVES$MAX_DRAG_BURIED : SOFTLEAVES$MAX_DRAG, drag);
        self.setDeltaMovement(delta.scale(1.0 - drag));
        // Momentum absorbed by the foliage also softens the eventual landing.
        if (delta.y < 0.0 && self.fallDistance > 0.0) {
            self.fallDistance = self.fallDistance * (1.0 - drag);
        }

        if (!level.isClientSide() && speed > SOFTLEAVES$MIN_SOUND_SPEED) {
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
        }
    }
}
