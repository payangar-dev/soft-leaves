package com.payangar.softleaves.mixin;

import com.payangar.softleaves.LeafDrag;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
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
    private static final double SOFTLEAVES$BASE_DRAG = 0.07;
    @Unique
    private static final double SOFTLEAVES$DRAG_PER_SPEED = 0.10;
    @Unique
    private static final double SOFTLEAVES$MAX_DRAG = 0.45;
    // Below this speed the movement is too subtle to rustle the leaves.
    @Unique
    private static final double SOFTLEAVES$MIN_SOUND_SPEED = 0.05;

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

        this.softleaves$lastLeafDragTick = self.tickCount;

        double drag = Math.min(SOFTLEAVES$MAX_DRAG, SOFTLEAVES$BASE_DRAG + SOFTLEAVES$DRAG_PER_SPEED * speed);
        self.setDeltaMovement(delta.scale(1.0 - drag));
        // Momentum absorbed by the foliage also softens the eventual landing.
        if (delta.y < 0.0 && self.fallDistance > 0.0) {
            self.fallDistance = self.fallDistance * (1.0 - drag);
        }

        if (!level.isClientSide()
            && speed > SOFTLEAVES$MIN_SOUND_SPEED
            && self.getRandom().nextFloat() < (float) Math.min(1.0, speed)) {
            SoundType sound = state.getSoundType();
            float volume = (float) Math.min(0.55, 0.1 + speed * 0.35);
            float pitch = sound.getPitch() * (0.9F + self.getRandom().nextFloat() * 0.2F);
            level.playSound(null, pos, sound.getStepSound(), SoundSource.BLOCKS, volume, pitch);
        }
    }
}
