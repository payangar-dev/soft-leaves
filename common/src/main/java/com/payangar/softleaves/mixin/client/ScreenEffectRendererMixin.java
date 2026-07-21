package com.payangar.softleaves.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockTintSource;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.Lightmap;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
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
 * outward and get backface-culled. Re-renders the six faces of the block the
 * camera is in, world-anchored (counter-rotating the view-space pose), with the
 * leaf sprite, the block's biome tint and the local light level. Faces are
 * emitted with both windings and slightly shrunk, so the result is robust to
 * the pipeline's cull mode and never z-fights neighbouring leaf faces.
 */
@Mixin(ScreenEffectRenderer.class)
public abstract class ScreenEffectRendererMixin {

    @Shadow
    @Final
    private Minecraft minecraft;

    @Inject(method = "submit", at = @At("TAIL"))
    private void softleaves$submitLeafInterior(boolean isFirstPerson, boolean isSleeping, float partialTicks, SubmitNodeCollector collector, boolean hideGui, CallbackInfo ci) {
        if (!isFirstPerson || isSleeping) {
            return;
        }
        LocalPlayer player = this.minecraft.player;
        if (player == null || player.isSpectator() || this.minecraft.level == null) {
            return;
        }

        Camera camera = this.minecraft.gameRenderer.mainCamera();
        Vec3 camPos = camera.position();
        BlockPos eyePos = BlockPos.containing(camPos);
        BlockState state = this.minecraft.level.getBlockState(eyePos);
        if (!(state.getBlock() instanceof LeavesBlock)) {
            return;
        }

        TextureAtlasSprite sprite = this.minecraft.getModelManager().getBlockStateModelSet().getParticleMaterial(state).sprite();

        int rgb = 0xFFFFFF;
        BlockTintSource tint = this.minecraft.getBlockColors().getTintSource(state, 0);
        if (tint != null) {
            rgb = tint.colorInWorld(state, this.minecraft.level, eyePos);
        }
        float brightness = Lightmap.getBrightness(this.minecraft.level.dimensionType(), this.minecraft.level.getMaxLocalRawBrightness(eyePos));
        int r = (int) ((rgb >> 16 & 0xFF) * brightness);
        int g = (int) ((rgb >> 8 & 0xFF) * brightness);
        int b = (int) ((rgb & 0xFF) * brightness);
        int color = 0xFF000000 | r << 16 | g << 8 | b;

        // The screen-effect pass draws in view space: counter-rotate the camera,
        // then place the cube at its world position relative to the camera.
        PoseStack poseStack = new PoseStack();
        poseStack.mulPose(camera.rotation().conjugate(new Quaternionf()));
        poseStack.translate((float) (eyePos.getX() - camPos.x), (float) (eyePos.getY() - camPos.y), (float) (eyePos.getZ() - camPos.z));

        float u0 = sprite.getU0();
        float u1 = sprite.getU1();
        float v0 = sprite.getV0();
        float v1 = sprite.getV1();
        collector.submitCustomGeometry(poseStack, RenderTypes.blockScreenEffect(sprite.atlasLocation()), (pose, builder) -> {
            Matrix4f m = pose.pose();
            float lo = 0.001F;
            float hi = 0.999F;
            softleaves$face(builder, m, lo, lo, lo, hi, lo, lo, hi, hi, lo, lo, hi, lo, u0, v0, u1, v1, color);
            softleaves$face(builder, m, lo, lo, hi, hi, lo, hi, hi, hi, hi, lo, hi, hi, u0, v0, u1, v1, color);
            softleaves$face(builder, m, lo, lo, lo, lo, lo, hi, lo, hi, hi, lo, hi, lo, u0, v0, u1, v1, color);
            softleaves$face(builder, m, hi, lo, lo, hi, lo, hi, hi, hi, hi, hi, hi, lo, u0, v0, u1, v1, color);
            softleaves$face(builder, m, lo, lo, lo, hi, lo, lo, hi, lo, hi, lo, lo, hi, u0, v0, u1, v1, color);
            softleaves$face(builder, m, lo, hi, lo, hi, hi, lo, hi, hi, hi, lo, hi, hi, u0, v0, u1, v1, color);
        });
    }

    @Unique
    private static void softleaves$face(
        VertexConsumer builder, Matrix4f m,
        float x1, float y1, float z1, float x2, float y2, float z2,
        float x3, float y3, float z3, float x4, float y4, float z4,
        float u0, float v0, float u1, float v1, int color
    ) {
        builder.addVertex(m, x1, y1, z1).setUv(u0, v1).setColor(color);
        builder.addVertex(m, x2, y2, z2).setUv(u1, v1).setColor(color);
        builder.addVertex(m, x3, y3, z3).setUv(u1, v0).setColor(color);
        builder.addVertex(m, x4, y4, z4).setUv(u0, v0).setColor(color);
        builder.addVertex(m, x4, y4, z4).setUv(u0, v0).setColor(color);
        builder.addVertex(m, x3, y3, z3).setUv(u1, v0).setColor(color);
        builder.addVertex(m, x2, y2, z2).setUv(u1, v1).setColor(color);
        builder.addVertex(m, x1, y1, z1).setUv(u0, v1).setColor(color);
    }
}
