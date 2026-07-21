package com.payangar.softleaves.mixin.client;

import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.LeavesBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractClientPlayer.class)
public abstract class AbstractClientPlayerMixin {

    // Slight FOV squeeze while the head is buried in foliage. Camera.tickFov
    // already eases the modifier at 0.5/tick, so the transition is smooth and
    // quick for free. Scaled by the FOV-effects accessibility option.
    @Inject(method = "getFieldOfViewModifier", at = @At("RETURN"), cancellable = true)
    private void softleaves$leafFov(boolean firstPerson, float effectScale, CallbackInfoReturnable<Float> cir) {
        AbstractClientPlayer self = (AbstractClientPlayer) (Object) this;
        BlockPos eyePos = BlockPos.containing(self.getEyePosition());
        if (self.level().getBlockState(eyePos).getBlock() instanceof LeavesBlock) {
            cir.setReturnValue(cir.getReturnValue() * Mth.lerp(effectScale, 1.0F, 0.9F));
        }
    }
}
