package com.payangar.softleaves;

import java.nio.file.Path;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Blocks;

public class SoftLeavesInit {

    public static void init(Path configDir) {
        SoftLeavesConfig.load(configDir);

        // Startup self-check, and a support diagnostic in user logs. Foliage is made
        // passable in the collision iterator, not on the block, so the block must
        // still report the vanilla full cube: that is the answer vanilla derives
        // ambient occlusion, the falling-leaf particle gate and the motion-blocking
        // heightmap from. The pass-through itself needs no check here, the mixin
        // fails the game load outright if its injection point ever moves.
        boolean vanillaShape = Blocks.OAK_LEAVES.defaultBlockState()
            .isCollisionShapeFullBlock(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
        if (vanillaShape) {
            Constants.LOG.info("Soft Leaves active: leaves are passable and render as vanilla.");
        } else {
            Constants.LOG.error("Soft Leaves: leaves lost their vanilla collision shape, foliage rendering will differ!");
        }
    }
}
