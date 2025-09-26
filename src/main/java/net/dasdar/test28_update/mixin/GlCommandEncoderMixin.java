package net.dasdar.test28_update.mixin;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.dasdar.test28_update.Test28;
import net.minecraft.client.gl.GlCommandEncoder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Optional;

@Mixin(GlCommandEncoder.class)
public class GlCommandEncoderMixin {

    @Redirect(method = "setPipelineAndApplyState", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/pipeline/RenderPipeline;getBlendFunction()Ljava/util/Optional;", ordinal = 0, remap = false))
    private Optional<BlendFunction> modifyBlendFunction0(RenderPipeline instance) {
        if (Test28.blendFunction != null) return Optional.of(Test28.blendFunction);
        return instance.getBlendFunction();
    }

    @Redirect(method = "setPipelineAndApplyState", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/pipeline/RenderPipeline;getBlendFunction()Ljava/util/Optional;", ordinal = 1, remap = false))
    private Optional<BlendFunction> modifyBlendFunction1(RenderPipeline instance) {
        if (Test28.blendFunction != null) return Optional.of(Test28.blendFunction);
        return instance.getBlendFunction();
    }

}
