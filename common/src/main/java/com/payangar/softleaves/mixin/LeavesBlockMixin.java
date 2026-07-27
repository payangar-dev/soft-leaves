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
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/**
 * Hands the "entity is inside foliage" event of {@link LeavesBlock} (inherited by
 * every leaves subclass) to the entity, which owns the speed-scaled drag and the
 * rustling. The block is left vanilla otherwise, including its collision shape:
 * {@code BlockCollisionsMixin} is what lets entities through, so that nothing
 * vanilla derives from that shape changes.
 */
@Mixin(LeavesBlock.class)
public abstract class LeavesBlockMixin extends Block {

    // Radius around the viewer's eyes, in blocks, where foliage stops blocking the
    // third-person camera. Below the 4-block default camera distance, so distant
    // foliage keeps framing the shot.
    @Unique
    private static final double SOFTLEAVES$CAMERA_CLEARANCE = 2.5;

    protected LeavesBlockMixin(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getVisualShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        // Only the third-person camera pull-in and see-through-transparent-blocks
        // targeting read this shape, never anything vanilla derives its "is this
        // block full?" answers from. Foliage keeps stopping both like vanilla, or
        // the camera would sink into a canopy and frame leaves instead of the
        // player. Close foliage is the exception: vanilla keeps the shortest of
        // eight jittered rays, so a single leaf brushing the viewer would pin the
        // camera onto them. Clearing a bubble around the viewer lets the camera
        // slip past what it is standing in while distant foliage still holds it.
        if (context instanceof EntityCollisionContext entityContext) {
            Entity viewer = entityContext.getEntity();
            if (viewer != null
                && Vec3.atCenterOf(pos).distanceToSqr(viewer.getEyePosition()) < SOFTLEAVES$CAMERA_CLEARANCE * SOFTLEAVES$CAMERA_CLEARANCE) {
                return Shapes.empty();
            }
        }
        return super.getVisualShape(state, level, pos, context);
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effectApplier, boolean isPrecise) {
        ((LeafDrag) entity).softleaves$applyLeafDrag(state, level, pos);
    }
}
