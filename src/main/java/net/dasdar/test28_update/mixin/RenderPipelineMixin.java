package net.dasdar.test28_update.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.dasdar.test28_update.Test28;
import org.spongepowered.asm.mixin.Mixin;

import java.util.Optional;

@Mixin(value = RenderPipeline.class, remap = false)
public class RenderPipelineMixin {

    @WrapMethod(method = "getBlendFunction", remap = false)
    private Optional<BlendFunction> modifyBlendFunction(Operation<Optional<BlendFunction>> original) {
        if (Test28.blendFunction != null) return Optional.of(Test28.blendFunction);
        return original.call();
    }

}
