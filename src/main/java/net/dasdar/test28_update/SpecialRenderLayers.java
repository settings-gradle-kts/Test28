package net.dasdar.test28_update;

import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.BlockRenderLayer;
import net.minecraft.client.render.RenderLayer;

import static net.minecraft.client.render.RenderPhase.ENABLE_LIGHTMAP;
import static net.minecraft.client.render.RenderPhase.MIPMAP_BLOCK_ATLAS_TEXTURE;

public class SpecialRenderLayers {
    private SpecialRenderLayers() { }

    private static final RenderLayer TRANSLUCENT = RenderLayer.of("translucent", 1536, true, true, RenderPipelines.TRANSLUCENT, RenderLayer.MultiPhaseParameters.builder().lightmap(ENABLE_LIGHTMAP).texture(MIPMAP_BLOCK_ATLAS_TEXTURE).build(true));

    public static RenderLayer fromBlock(BlockRenderLayer blockRenderLayer) {
        return switch (blockRenderLayer) {
            case TRIPWIRE -> RenderLayer.getTripwire();
            case CUTOUT -> RenderLayer.getCutout();
            case CUTOUT_MIPPED -> RenderLayer.getCutoutMipped();
            case null, default -> TRANSLUCENT; // RenderLayer.getTranslucent doesn't exist anymore, so we return our own
        };
    }

}
