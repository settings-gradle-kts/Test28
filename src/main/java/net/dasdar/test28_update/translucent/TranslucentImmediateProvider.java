package net.dasdar.test28_update.translucent;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gl.Defines;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderPhase;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class TranslucentImmediateProvider extends VertexConsumerProvider.Immediate {
    private static final Map<RenderLayer, RenderLayer> translucentLayers = new HashMap<>();
    private static final Map<RenderPipeline, RenderPipeline> translucentPipelines = new HashMap<>();

    private final VertexConsumerProvider.Immediate immediate;
    private final VertexConsumerProvider translucentBufferProvider;
    public TranslucentImmediateProvider(VertexConsumerProvider.Immediate immediate) {
        super(null, null);

        this.immediate = immediate;
        translucentBufferProvider = layer -> {
            RenderLayer translucentLayer = getTranslucentLayer(layer);
            return immediate.getBuffer(translucentLayer);
        };
    }

    /**
     * Creates a new {@link RenderPipeline} with {@link BlendFunction#TRANSLUCENT}
     * @return A {@link RenderPipeline} capable of rendering translucent vertices to the world
     */
    public static RenderPipeline getTranslucentPipeline(RenderPipeline pipeline) {
        Identifier pipelineIdentifier = pipeline.getLocation();
        return translucentPipelines.computeIfAbsent(pipeline, k -> {
            RenderPipeline.Snippet snippet = createPipelineSnippet(pipeline);
            RenderPipeline.Builder pipelineBuilder = RenderPipeline.builder(snippet)
                    .withLocation(Identifier.of(pipelineIdentifier.getNamespace(), pipelineIdentifier.getPath() + "_translucent"))
                    .withColorWrite(true, true)
                    .withBlend(BlendFunction.TRANSLUCENT);

            Defines defines = pipeline.getShaderDefines();
            addDefineIfAbsent(pipelineBuilder, defines, "ALPHA_CUTOUT", 0.1f);

            return pipelineBuilder.build();
        });
    }

    /**
     * Maps the original {@link RenderLayer} to a new one with a new {@link RenderPipeline} from {@link TranslucentImmediateProvider#getTranslucentPipeline(RenderPipeline)}
     * <p>
     * Will not map RenderLayers already made for translucent rendering
     * @return A {@link RenderLayer} capable fo rendering translucent vertices to the world
     */
    public static RenderLayer getTranslucentLayer(RenderLayer layer) {
        if (!(layer instanceof RenderLayer.MultiPhase multiPhase)) return layer;
        if (layer.isTranslucent()) return layer;

        return translucentLayers.computeIfAbsent(multiPhase, k -> {
            RenderLayer.MultiPhaseParameters phases = multiPhase.phases;
            RenderLayer.MultiPhaseParameters.Builder parameters = createParameterBuilder(phases);

            RenderPipeline pipeline = getTranslucentPipeline(multiPhase.pipeline);
            return RenderLayer.of(
                    layer.getName() + "_translucent",
                    layer.getExpectedBufferSize(),
                    layer.hasCrumbling(), true,
                    pipeline,
                    parameters.build(phases.outlineMode)
            );
        });
    }

    private static void addDefineIfAbsent(RenderPipeline.Builder builder, Defines defines, String defineName, float defineValue) {
        boolean alphaCutout = defines.values().keySet().stream().anyMatch(define -> define.equals(defineName));
        if (!alphaCutout) builder.withShaderDefine(defineName, defineValue);
    }

    private static RenderPipeline.Snippet createPipelineSnippet(RenderPipeline pipeline) {
        return new RenderPipeline.Snippet(
                Optional.of(pipeline.getVertexShader()),
                Optional.of(pipeline.getFragmentShader()),
                Optional.of(pipeline.getShaderDefines()),
                Optional.of(pipeline.getSamplers()),
                Optional.of(pipeline.getUniforms()),
                pipeline.getBlendFunction(),
                Optional.of(pipeline.getDepthTestFunction()),
                Optional.of(pipeline.getPolygonMode()),
                Optional.of(pipeline.isCull()),
                Optional.of(pipeline.isWriteColor()),
                Optional.of(pipeline.isWriteAlpha()),
                Optional.of(pipeline.isWriteDepth()),
                Optional.of(pipeline.getColorLogic()),
                Optional.of(pipeline.getVertexFormat()),
                Optional.of(pipeline.getVertexFormatMode())
        );
    }

    private static RenderLayer.MultiPhaseParameters.Builder createParameterBuilder(RenderLayer.MultiPhaseParameters parameters) {
        return RenderLayer.MultiPhaseParameters.builder()
                .texture(parameters.texture)
                .lightmap((RenderPhase.Lightmap) parameters.phases.get(1))
                .overlay((RenderPhase.Overlay) parameters.phases.get(2))
                .layering((RenderPhase.Layering) parameters.phases.get(3))
                .target(RenderPhase.MAIN_TARGET)
                .texturing((RenderPhase.Texturing) parameters.phases.get(5))
                .lineWidth((RenderPhase.LineWidth) parameters.phases.get(6));
    }

    private boolean coloredBuffers;
    private float r, g, b, a;

    /**
     * Sets all subsequent calls to {@link #getBuffer(RenderLayer)} to return buffers with tinted RGBA values
     */
    public void colorAllBuffers(float r, float g, float b, float a) {
        coloredBuffers = true;
        this.r = r;
        this.g = g;
        this.b = b;
        this.a = a;
    }

    /**
     * Disables coloring by {@link #colorAllBuffers(float, float, float, float)}
     * <p>
     * Subsequent calls to {@link #getBuffer(RenderLayer)} will return unmodified buffers. Already tinted buffers will continue coloring their output
     */
    public void disableBufferColors() {
        coloredBuffers = false;
    }

    @Override
    public VertexConsumer getBuffer(RenderLayer renderLayer) {
        if (coloredBuffers) return getColoredBuffer(renderLayer, r, g, b, a);
        return translucentBufferProvider.getBuffer(renderLayer);
    }

    /**
     * @return A {@link VertexConsumer} with every color multiplied by the given r, g, b, and a floats
     */
    public VertexConsumer getColoredBuffer(RenderLayer renderLayer, float r, float g, float b, float a) {
        VertexConsumer buffer = translucentBufferProvider.getBuffer(renderLayer);
        return new ColoredVertexConsumer(buffer, r, g, b, a);
    }

    @Override
    public void draw(RenderLayer layer) {
        immediate.draw(layer);
    }

    @Override
    public void draw() {
        immediate.draw();
    }

    @Override
    public void drawCurrentLayer() {
        immediate.drawCurrentLayer();
    }

    private static class ColoredVertexConsumer implements VertexConsumer {
        private final VertexConsumer consumer;
        private final float r, g, b, a;
        public ColoredVertexConsumer(VertexConsumer consumer, float r, float g, float b, float a) {
            this.consumer = consumer;
            this.r = r;
            this.g = g;
            this.b = b;
            this.a = a;
        }

        @Override
        public VertexConsumer vertex(float x, float y, float z) {
            return consumer.vertex(x, y, z);
        }

        @Override
        public VertexConsumer color(int red, int green, int blue, int alpha) {
            float nr = red * r;
            float ng = green * g;
            float nb = blue * b;
            float na = alpha * a;

            float maxValue = Math.max(Math.max(nr, ng), nb);
            if (maxValue > 255) {
                float scale = 255f / maxValue;
                nr = nr * scale;
                ng = ng * scale;
                nb = nb * scale;
            }

            return consumer.color(Math.round(nr), Math.round(ng), Math.round(nb), Math.round(na));
        }

        @Override
        public VertexConsumer texture(float u, float v) {
            return consumer.texture(u, v);
        }

        @Override
        public VertexConsumer overlay(int u, int v) {
            return consumer.overlay(u, v);
        }

        @Override
        public VertexConsumer light(int u, int v) {
            return consumer.light(u, v);
        }

        @Override
        public VertexConsumer normal(float x, float y, float z) {
            return consumer.normal(x, y, z);
        }

    }

}
