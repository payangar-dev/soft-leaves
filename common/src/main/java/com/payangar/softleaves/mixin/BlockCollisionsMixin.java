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
 * Complements the pass-through {@code LeavesBlockMixin} answers on the block
 * itself, for the callers that reach this iterator with a context carrying no
 * mover and would therefore be told foliage is solid. Particle collision is the
 * one that matters: it queries with a bare context, so leaves flung inside a
 * canopy would freeze against it.
 * <p>
 * This is deliberately not the primary mechanism. Optimisation mods replace
 * vanilla's collision path with their own sweeper, which never reaches this call
 * site, so foliage has to be passable on the block or it is not passable at all
 * for them.
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
