package com.payangar.softleaves;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;

public class SoftLeavesInit {

    public static void init() {
        // Startup self-check: proves the LeavesBlock mixin actually replaced the
        // collision shape, and doubles as a support diagnostic in user logs. Both
        // halves of the contract are checked. A position context takes the same
        // branch as a real entity, which no world-less check could build.
        BlockState leaves = Blocks.OAK_LEAVES.defaultBlockState();
        boolean passable = leaves
            .getCollisionShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO, CollisionContext.positionContext(0.0))
            .isEmpty();
        boolean fullToProbes = !leaves
            .getCollisionShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO, CollisionContext.empty())
            .isEmpty();
        if (!passable) {
            Constants.LOG.error("Soft Leaves mixin did not apply: leaves still have a collision shape!");
        } else if (!fullToProbes) {
            // Vanilla derives ambient occlusion, the falling-leaf particle gate and
            // the motion-blocking heightmap from this probe.
            Constants.LOG.error("Soft Leaves: leaves report no collision shape to vanilla's probes, their rendering will differ!");
        } else {
            Constants.LOG.info("Soft Leaves active: leaves are passable.");
        }
    }
}
