package net.dasdar.test28_update.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import net.dasdar.test28_update.Test28;
import net.minecraft.client.gl.DynamicUniforms;
import org.joml.Matrix4fc;
import org.joml.Vector3fc;
import org.joml.Vector4fc;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(DynamicUniforms.class)
public class DynamicUniformsMixin {

    @WrapMethod(method = "write")
    private GpuBufferSlice modifyColorModulator(Matrix4fc modelView, Vector4fc colorModulator, Vector3fc modelOffset, Matrix4fc textureMatrix, float lineWidth, Operation<GpuBufferSlice> original) {
        Vector4fc color = colorModulator;
        if (Test28.colorModulator != null) color = Test28.colorModulator;
        return original.call(modelView, color, modelOffset, textureMatrix, lineWidth);
    }

}
