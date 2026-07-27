package com.payangar.softleaves.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockCollisions;
import net.minecraft.world.level.CollisionGetter;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Makes foliage transparent to physics, and to physics only. Every collision
 * query the game runs funnels through this iterator: entity movement, ground
 * support, the emptiness checks, and particle collision. Dropping leaves here
 * is what lets everything walk and fall through them.
 * <p>
 * The block keeps its vanilla collision shape, which matters because vanilla
 * reads that shape to answer "does this block fill its cube?" and derives from
 * it the ambient occlusion leaves cast, the gate that keeps falling-leaf
 * particles to the underside of a canopy, and the motion-blocking heightmap the
 * weather renders against. Lying to the block instead of to the iterator is
 * what used to make foliage render differently from vanilla.
 */
@Mixin(BlockCollisions.class)
public abstract class BlockCollisionsMixin {

    @WrapOperation(
        method = "computeNext",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/phys/shapes/CollisionContext;getCollisionShape(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/CollisionGetter;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/phys/shapes/VoxelShape;"
        )
    )
    private VoxelShape softleaves$passThroughLeaves(
        CollisionContext context, BlockState state, CollisionGetter level, BlockPos pos, Operation<VoxelShape> original
    ) {
        return state.getBlock() instanceof LeavesBlock ? Shapes.empty() : original.call(context, state, level, pos);
    }
}
