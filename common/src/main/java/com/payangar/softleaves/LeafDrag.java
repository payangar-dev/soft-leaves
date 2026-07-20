package com.payangar.softleaves;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Duck interface implemented on {@link net.minecraft.world.entity.Entity} through
 * {@code EntityMixin}. Lets the leaves block hand the "entity is inside foliage"
 * event to the entity, which owns the per-tick state needed to apply the drag
 * exactly once per tick no matter how many leaves blocks the entity intersects.
 */
public interface LeafDrag {

    void softleaves$applyLeafDrag(BlockState state, Level level, BlockPos pos);
}
