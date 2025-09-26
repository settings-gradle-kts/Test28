package net.dasdar.test28_update;

import net.minecraft.client.render.BlockRenderLayer;
import net.minecraft.client.render.RenderLayer;

public class SpecialRenderLayers {
    private SpecialRenderLayers() { }

    public static RenderLayer fromBlock(BlockRenderLayer blockRenderLayer) {
        return switch (blockRenderLayer) {
            case TRIPWIRE -> RenderLayer.getTripwire();
            case CUTOUT -> RenderLayer.getCutout();
            case CUTOUT_MIPPED -> RenderLayer.getCutoutMipped();
            case null, default -> RenderLayer.getSolid(); // RenderLayer.getTranslucent doesn't exist anymore, so just return RenderLayer.getSolid
        };
    }

}
