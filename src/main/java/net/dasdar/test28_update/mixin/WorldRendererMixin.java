package net.dasdar.test28_update.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import net.dasdar.test28_update.Test28;
import net.minecraft.client.render.*;
import net.minecraft.client.util.ObjectAllocator;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public class WorldRendererMixin {

    @Shadow @Final private DefaultFramebufferSet framebufferSet;

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/WorldRenderer;renderLateDebug(Lnet/minecraft/client/render/FrameGraphBuilder;Lnet/minecraft/util/math/Vec3d;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;)V", shift = At.Shift.BEFORE))
    private void encode$renderWorld(ObjectAllocator allocator, RenderTickCounter tickCounter, boolean renderBlockOutline,
                                    Camera camera, Matrix4f positionMatrix, Matrix4f projectionMatrix,
                                    GpuBufferSlice fog, Vector4f fogColor, boolean shouldRenderSky,
                                    CallbackInfo ci, @Local FrameGraphBuilder frameGraphBuilder) {
        FramePass beforeLateDebugPass = frameGraphBuilder.createPass("before_late_debug");
        framebufferSet.mainFramebuffer = beforeLateDebugPass.transfer(framebufferSet.mainFramebuffer);
        beforeLateDebugPass.setRenderer(Test28::renderWorld);
    }

}
