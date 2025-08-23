package net.dasdar.test28_update;

import net.dasdar.test28_update.translucent.SpecialRenderLayers;
import net.dasdar.test28_update.translucent.TranslucentImmediateProvider;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.chunk.*;
import net.minecraft.client.render.model.BlockModelPart;
import net.minecraft.client.render.model.BlockStateModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.registry.Registries;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.RaycastContext;

import java.util.*;

public class Test28 implements ClientModInitializer {

    public static MinecraftClient MC;
    private static Vec3d bpv;
    private static BlockPos bp;
    private static Random random;

    private static WorldRenderContext rendererContext;
    public static void renderWorld() {
        if (rendererContext == null) return;
        if (bp == null) return;

        MatrixStack matrices = rendererContext.matrixStack();

        if (!(rendererContext.consumers() instanceof VertexConsumerProvider.Immediate contextProvider)) return;
        TranslucentImmediateProvider provider = new TranslucentImmediateProvider(contextProvider);
        provider.colorAllBuffers(.8f, .8f, 1.5f, .8f);

        Vec3d cameraTranslation = MC.gameRenderer.getCamera().getCameraPos().negate();

        matrices.push();
        matrices.translate(cameraTranslation);
        matrices.translate(bp.getX(), bp.getY(), bp.getZ());

        // We want to render block entities first, or we won't be able to see them through other blocks
        renderBlock(matrices, Blocks.OAK_WALL_SIGN.getDefaultState().with(Properties.HORIZONTAL_FACING, Direction.WEST), provider, new Vec3d(-1, 0, 0));
        renderBlock(matrices, Blocks.CHEST.getDefaultState(), provider, new Vec3d(0, 1, 0));

        renderBlock(matrices, Blocks.DIAMOND_BLOCK.getDefaultState(), provider, new Vec3d(0, 0, 0));
        renderBlock(matrices, Blocks.STONE.getDefaultState(), provider, new Vec3d(0, 0, 1));

        matrices.pop();
    }

    private static void renderBlock(MatrixStack matrices, BlockState state, TranslucentImmediateProvider provider, Vec3d offset) {
        BlockRenderLayer blockRenderLayer = RenderLayers.getBlockLayer(state);
        RenderLayer renderLayer = SpecialRenderLayers.fromBlock(blockRenderLayer);

        random.setSeed(state.getRenderingSeed(bp));
        BlockRenderManager manager = MC.getBlockRenderManager();
        BlockStateModel model = manager.getModel(state);
        List<BlockModelPart> modelParts = model.getParts(random);

        matrices.push();
        matrices.translate(offset);

        VertexConsumer consumer = provider.getBuffer(renderLayer);
        manager.renderBlock(state, bp, MC.world, matrices, consumer, true, modelParts);

        if (state.hasBlockEntity()) {
            BlockEntityRenderDispatcher rendererDispatcher = MC.getBlockEntityRenderDispatcher();

            Optional<BlockEntityType<?>> optionalType = Registries.BLOCK_ENTITY_TYPE.stream()
                    .filter(type -> type.supports(state))
                    .findFirst();
            if (optionalType.isPresent()) {
                BlockEntityType<?> type = optionalType.get();
                BlockEntity instance = type.instantiate(bp, state);
                if (instance != null) instance.setWorld(MC.world);

                if (instance instanceof SignBlockEntity signInstance) {
                    signInstance.changeText(text -> text.withMessage(1, Text.literal("Hello")), true);
                }

                BlockEntityRenderer<BlockEntity> renderer = instance == null ? null : rendererDispatcher.get(instance);
                if (renderer != null) {
                    renderer.render(
                            instance, 1f,
                            matrices, provider,
                            LightmapTextureManager.pack(15, 15), OverlayTexture.DEFAULT_UV,
                            Vec3d.ZERO
                    );
                }
            }
        }

        matrices.pop();
    }

    @Override
    public void onInitializeClient() {
        MC = MinecraftClient.getInstance();
        random = Random.create();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (MC.player == null) return;
            Vec3d cameraPos = MC.gameRenderer.getCamera().getCameraPos();

            float pitch = MC.gameRenderer.getCamera().getPitch();
            float yaw = MC.gameRenderer.getCamera().getYaw();

            Vec3d lookDirection = new Vec3d(
                    -Math.sin(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch)),
                    -Math.sin(Math.toRadians(pitch)),
                    Math.cos(Math.toRadians(yaw)) * Math.cos(Math.toRadians(pitch))
            );

            RaycastContext context = new RaycastContext(
                    cameraPos,
                    cameraPos.add(lookDirection.multiply(20)),
                    RaycastContext.ShapeType.COLLIDER,
                    RaycastContext.FluidHandling.ANY,
                    MC.cameraEntity
            );

            HitResult result = MC.world.raycast(context);
            if (result instanceof BlockHitResult bhr) {
                bpv = bhr.getPos().add(bhr.getSide().getDoubleVector().multiply(0.2f));
            } else bpv = result.getPos();
            bp = BlockPos.ofFloored(bpv);
        });

        WorldRenderEvents.LAST.register(context -> rendererContext = context);
    }

}
