package com.payangar.softleaves;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.shapes.CollisionContext;

public class SoftLeavesInit {

    public static void init() {
        // Startup self-check: proves the LeavesBlock mixin actually replaced the
        // collision shape, and doubles as a support diagnostic in user logs.
        boolean passable = Blocks.OAK_LEAVES.defaultBlockState()
            .getCollisionShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO, CollisionContext.empty())
            .isEmpty();
        if (passable) {
            Constants.LOG.info("Soft Leaves active: leaves are passable.");
        } else {
            Constants.LOG.error("Soft Leaves mixin did not apply: leaves still have a collision shape!");
        }
    }
}
