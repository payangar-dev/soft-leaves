package com.payangar.softleaves.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Vanilla short-circuits every LeavesBlock to PathType.LEAVES (malus -1, blocked)
 * before any collision check, so mobs would still path around foliage they can now
 * walk through. Rewrites that verdict the way the generic branch would resolve a
 * collisionless block: water if waterlogged, open otherwise. FlyNodeEvaluator
 * inherits this same static helper.
 */
@Mixin(WalkNodeEvaluator.class)
public abstract class WalkNodeEvaluatorMixin {

    @Inject(method = "getPathTypeFromState", at = @At("RETURN"), cancellable = true)
    private static void softleaves$passableLeaves(BlockGetter level, BlockPos pos, CallbackInfoReturnable<PathType> cir) {
        if (cir.getReturnValue() == PathType.LEAVES) {
            boolean water = level.getBlockState(pos).getFluidState().is(FluidTags.WATER);
            cir.setReturnValue(water ? PathType.WATER : PathType.OPEN);
        }
    }
}
