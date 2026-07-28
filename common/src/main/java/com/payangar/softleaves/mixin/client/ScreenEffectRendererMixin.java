package com.payangar.softleaves.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * From inside a leaves block the player sees nothing of it: its faces all point
 * outward and get backface-culled. Re-renders the faces of the block the camera
 * is in, world-anchored (counter-rotating the view-space pose), with the leaf
 * sprite, the block's biome tint and the local light level. Faces are emitted
 * with both windings and slightly shrunk, so the result is robust to the
 * pipeline's cull mode and never z-fights neighbouring leaf faces.
 */
@Mixin(ScreenEffectRenderer.class)
public abstract class ScreenEffectRendererMixin {

    @Shadow
    @Final
    private Minecraft minecraft;

    @Shadow
    @Final
    private MultiBufferSource bufferSource;

    @Inject(method = "renderScreenEffect", at = @At("TAIL"))
    private void softleaves$submitLeafInterior(boolean isSleeping, float partialTicks, CallbackInfo ci) {
        // 1.21.x passes no first-person flag: vanilla reads the camera type itself.
        if (isSleeping || !this.minecraft.options.getCameraType().isFirstPerson()) {
            return;
        }
        LocalPlayer player = this.minecraft.player;
        if (player == null || player.isSpectator() || this.minecraft.level == null) {
            return;
        }

        Camera camera = this.minecraft.gameRenderer.getMainCamera();
        Vec3 camPos = camera.position();
        BlockPos eyePos = BlockPos.containing(camPos);
        BlockState state = this.minecraft.level.getBlockState(eyePos);
        if (!(state.getBlock() instanceof LeavesBlock)) {
            return;
        }

        // A leaves neighbour already draws its own face on the shared plane, so
        // ours would only double the texture over it. Emit a face only where the
        // camera block borders something else.
        boolean drawNorth = softleaves$needsFace(state, eyePos, Direction.NORTH);
        boolean drawSouth = softleaves$needsFace(state, eyePos, Direction.SOUTH);
        boolean drawWest = softleaves$needsFace(state, eyePos, Direction.WEST);
        boolean drawEast = softleaves$needsFace(state, eyePos, Direction.EAST);
        boolean drawDown = softleaves$needsFace(state, eyePos, Direction.DOWN);
        boolean drawUp = softleaves$needsFace(state, eyePos, Direction.UP);
        if (!drawNorth && !drawSouth && !drawWest && !drawEast && !drawDown && !drawUp) {
            return;
        }

        TextureAtlasSprite sprite = this.minecraft.getBlockRenderer().getBlockModelShaper().getParticleIcon(state);

        int rgb = this.minecraft.getBlockColors().getColor(state, this.minecraft.level, eyePos, 0);
        if (rgb == -1) {
            rgb = 0xFFFFFF;
        }
        // Vanilla lights a face with the light of the block it points toward
        // (sky above the canopy, shadow below it) and shades it by direction.
        // Sampling per-face neighbour light reproduces that look.
        int lightUp = softleaves$packedLight(eyePos.above());
        int lightDown = softleaves$packedLight(eyePos.below());
        int lightNorth = softleaves$packedLight(eyePos.north());
        int lightSouth = softleaves$packedLight(eyePos.south());
        int lightWest = softleaves$packedLight(eyePos.west());
        int lightEast = softleaves$packedLight(eyePos.east());
        int colorUp = softleaves$shade(rgb, 1.0F);
        int colorDown = softleaves$shade(rgb, 0.5F);
        int colorNS = softleaves$shade(rgb, 0.8F);
        int colorEW = softleaves$shade(rgb, 0.6F);

        // The screen-effect pass draws in view space: counter-rotate the camera,
        // then place the cube at its world position relative to the camera.
        PoseStack poseStack = new PoseStack();
        poseStack.mulPose(camera.rotation().conjugate(new Quaternionf()));
        poseStack.translate((float) (eyePos.getX() - camPos.x), (float) (eyePos.getY() - camPos.y), (float) (eyePos.getZ() - camPos.z));

        float u0 = sprite.getU0();
        float u1 = sprite.getU1();
        float v0 = sprite.getV0();
        float v1 = sprite.getV1();
        // Depth-tested world-text render type: the faces integrate with the scene
        // instead of drawing over it (blockScreenEffect ignores the depth buffer).
        // Pre-1.21.9 there is no submission collector: emit into the pass's own
        // buffer source, which the game renderer flushes after this method.
        VertexConsumer builder = this.bufferSource.getBuffer(RenderType.text(sprite.atlasLocation()));
        Matrix4f m = poseStack.last().pose();
        float lo = 0.001F;
        float hi = 0.999F;
        if (drawNorth) {
            softleaves$face(builder, m, lo, lo, lo, hi, lo, lo, hi, hi, lo, lo, hi, lo, u0, v0, u1, v1, colorNS, lightNorth);
        }
        if (drawSouth) {
            softleaves$face(builder, m, lo, lo, hi, hi, lo, hi, hi, hi, hi, lo, hi, hi, u0, v0, u1, v1, colorNS, lightSouth);
        }
        if (drawWest) {
            softleaves$face(builder, m, lo, lo, lo, lo, lo, hi, lo, hi, hi, lo, hi, lo, u0, v0, u1, v1, colorEW, lightWest);
        }
        if (drawEast) {
            softleaves$face(builder, m, hi, lo, lo, hi, lo, hi, hi, hi, hi, hi, hi, lo, u0, v0, u1, v1, colorEW, lightEast);
        }
        if (drawDown) {
            softleaves$face(builder, m, lo, lo, lo, hi, lo, lo, hi, lo, hi, lo, lo, hi, u0, v0, u1, v1, colorDown, lightDown);
        }
        if (drawUp) {
            softleaves$face(builder, m, lo, hi, lo, hi, hi, lo, hi, hi, hi, lo, hi, hi, u0, v0, u1, v1, colorUp, lightUp);
        }
    }

    @Unique
    private boolean softleaves$needsFace(BlockState state, BlockPos pos, Direction direction) {
        BlockState neighbour = this.minecraft.level.getBlockState(pos.relative(direction));
        if (!(neighbour.getBlock() instanceof LeavesBlock)) {
            return true;
        }
        // Fast graphics culls the faces between two leaves blocks, so there the
        // neighbour draws nothing back at us and the camera block still needs its
        // own face to surround the head.
        return neighbour.skipRendering(state, direction.getOpposite());
    }

    @Unique
    private int softleaves$packedLight(BlockPos pos) {
        return this.minecraft.level.getBrightness(LightLayer.SKY, pos) << 20
            | this.minecraft.level.getBrightness(LightLayer.BLOCK, pos) << 4;
    }

    @Unique
    private static int softleaves$shade(int rgb, float shade) {
        int r = (int) ((rgb >> 16 & 0xFF) * shade);
        int g = (int) ((rgb >> 8 & 0xFF) * shade);
        int b = (int) ((rgb & 0xFF) * shade);
        return 0xFF000000 | r << 16 | g << 8 | b;
    }

    @Unique
    private static void softleaves$face(
        VertexConsumer builder, Matrix4f m,
        float x1, float y1, float z1, float x2, float y2, float z2,
        float x3, float y3, float z3, float x4, float y4, float z4,
        float u0, float v0, float u1, float v1, int color, int light
    ) {
        builder.addVertex(m, x1, y1, z1).setUv(u0, v1).setColor(color).setLight(light);
        builder.addVertex(m, x2, y2, z2).setUv(u1, v1).setColor(color).setLight(light);
        builder.addVertex(m, x3, y3, z3).setUv(u1, v0).setColor(color).setLight(light);
        builder.addVertex(m, x4, y4, z4).setUv(u0, v0).setColor(color).setLight(light);
        builder.addVertex(m, x4, y4, z4).setUv(u0, v0).setColor(color).setLight(light);
        builder.addVertex(m, x3, y3, z3).setUv(u1, v0).setColor(color).setLight(light);
        builder.addVertex(m, x2, y2, z2).setUv(u1, v1).setColor(color).setLight(light);
        builder.addVertex(m, x1, y1, z1).setUv(u0, v1).setColor(color).setLight(light);
    }
}
