package com.payangar.softleaves.mixin;

import com.payangar.softleaves.LeafDrag;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Adds overrides to {@link LeavesBlock} (inherited by every leaves subclass):
 * an empty collision shape so entities pass through, and an entity-inside hook
 * that delegates the speed-scaled drag and rustling to the entity.
 * The outline shape is untouched, so leaves can still be targeted and broken,
 * and {@code getEntityInsideCollisionShape} stays a full block, which is what
 * keeps {@code entityInside} firing with no collision shape.
 */
@Mixin(LeavesBlock.class)
public abstract class LeavesBlockMixin extends Block {

    protected LeavesBlockMixin(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effectApplier, boolean isPrecise) {
        ((LeafDrag) entity).softleaves$applyLeafDrag(state, level, pos);
    }
}
