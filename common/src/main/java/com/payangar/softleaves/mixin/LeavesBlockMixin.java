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
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Adds overrides to {@link LeavesBlock} (inherited by every leaves subclass):
 * a collision shape that only movers pass through, and an entity-inside hook
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
        // Vanilla asks this same method with a mover-less context to answer "does
        // this block fill its cube?", and caches the answer per block state. That
        // answer drives things that have nothing to do with movement: the ambient
        // occlusion leaves cast, the gate that keeps falling-leaf particles to the
        // underside of a canopy, and the motion-blocking heightmap the weather
        // renders against. Those probes keep the vanilla cube; only what actually
        // moves through the world gets to pass.
        if (context instanceof EntityCollisionContext entityContext && entityContext.getEntity() == null) {
            return super.getCollisionShape(state, level, pos, context);
        }
        return Shapes.empty();
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effectApplier, boolean isPrecise) {
        ((LeafDrag) entity).softleaves$applyLeafDrag(state, level, pos);
    }
}
