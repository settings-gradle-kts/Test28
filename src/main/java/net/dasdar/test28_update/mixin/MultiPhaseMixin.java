package net.dasdar.test28_update.mixin;

import net.dasdar.test28_update.Test28;
import net.minecraft.client.render.RenderLayer;
import org.joml.Vector4fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

@Mixin(RenderLayer.MultiPhase.class)
public class MultiPhaseMixin {

    @ModifyArg(method = "draw", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gl/DynamicUniforms;write(Lorg/joml/Matrix4fc;Lorg/joml/Vector4fc;Lorg/joml/Vector3fc;Lorg/joml/Matrix4fc;F)Lcom/mojang/blaze3d/buffers/GpuBufferSlice;"), index = 1)
    private Vector4fc modifyColorModulator(Vector4fc colorModulator) {
        if (Test28.colorModulator != null) return Test28.colorModulator;
        return colorModulator;
    }

}
