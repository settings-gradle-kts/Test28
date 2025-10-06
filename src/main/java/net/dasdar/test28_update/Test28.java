package net.dasdar.test28_update;

import com.mojang.blaze3d.pipeline.BlendFunction;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.loader.api.FabricLoader;
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
import net.minecraft.client.render.model.BlockModelPart;
import net.minecraft.client.render.model.BlockStateModel;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.registry.Registries;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.*;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.LightType;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.chunk.light.LightingProvider;
import org.joml.Vector4f;
import org.joml.Vector4fc;

import java.util.*;

public class Test28 implements ClientModInitializer {

    public static Vector4fc colorModulator = null;
    public static BlendFunction blendFunction = null;

    public static boolean sodium = FabricLoader.getInstance().isModLoaded("sodium");
    public static boolean iris = FabricLoader.getInstance().isModLoaded("iris");

    private static final List<QueuedBlock> renderBlocks = new ArrayList<>();

    public static MinecraftClient MC;
    private static Vec3d blockPosVector;
    private static BlockPos blockPos;
    private static Random random;

    private static boolean lastPressed;
    private static boolean dontRenderBlocks;
    private static WorldRenderContext rendererContext;
    public static void renderWorld() {
        if (rendererContext == null) return;
        if (blockPos == null) return;

        boolean pressed = InputUtil.isKeyPressed(MC.getWindow().getHandle(), InputUtil.GLFW_KEY_F4);
        if (pressed && !lastPressed) {
            dontRenderBlocks = !dontRenderBlocks;
        }
        lastPressed = pressed;
        if (dontRenderBlocks) return;


        MatrixStack matrices = rendererContext.matrixStack();

        if (!(rendererContext.consumers() instanceof VertexConsumerProvider.Immediate provider)) return;
        colorModulator = new Vector4f(.8f, .8f, 1.5f, iris ? 1 : .8f);
        blendFunction = BlendFunction.TRANSLUCENT;

        Vec3d cameraTranslation = MC.gameRenderer.getCamera().getCameraPos().negate();

        matrices.push();
        matrices.translate(cameraTranslation);

        renderBlock(Blocks.DIAMOND_BLOCK.getDefaultState(), new BlockPos(0, 0, 0));
        renderBlock(Blocks.STONE.getDefaultState(), new BlockPos(0, 0, 1));
        renderBlock(Blocks.OAK_WALL_SIGN.getDefaultState().with(Properties.HORIZONTAL_FACING, Direction.WEST), new BlockPos(-1, 0, 0));
        renderBlock(Blocks.CHEST.getDefaultState(), new BlockPos(0, 1, 0));

        // Sort blocks back to front so we can see them through each other
        Vec3d cameraPos = MC.gameRenderer.getCamera().getCameraPos();
        Vec3i blockOffset = new Vec3i(blockPos.getX(), blockPos.getY(), blockPos.getZ());
        renderBlocks.sort((a, b) -> {
            Box boxA = new Box(a.pos.add(blockOffset));
            Box boxB = new Box(b.pos.add(blockOffset));

            Vec3d closestA = new Vec3d(
                    Math.clamp(cameraPos.x, boxA.minX, boxA.maxX),
                    Math.clamp(cameraPos.y, boxA.minY, boxA.maxY),
                    Math.clamp(cameraPos.z, boxA.minZ, boxA.maxZ)
            );
            Vec3d closestB = new Vec3d(
                    Math.clamp(cameraPos.x, boxB.minX, boxB.maxX),
                    Math.clamp(cameraPos.y, boxB.minY, boxB.maxY),
                    Math.clamp(cameraPos.z, boxB.minZ, boxB.maxZ)
            );

            return Double.compare(closestB.squaredDistanceTo(cameraPos), closestA.squaredDistanceTo(cameraPos));
        });

        renderBlocks(matrices, provider);
        renderBlocks.clear();

        matrices.pop();

        colorModulator = null;
        blendFunction = null;
    }

    private static void renderBlock(BlockState state, BlockPos offset) {
        renderBlocks.add(new QueuedBlock(state, offset));
    }

    private static void renderBlocks(MatrixStack matrices, VertexConsumerProvider.Immediate provider) {
        for (QueuedBlock block : renderBlocks) {
            BlockState state = block.state;
            BlockPos translated = blockPos.add(block.pos);

            BlockRenderLayer blockRenderLayer = RenderLayers.getBlockLayer(state);
            RenderLayer renderLayer = SpecialRenderLayers.fromBlock(blockRenderLayer);

            random.setSeed(state.getRenderingSeed(translated));
            BlockRenderManager manager = MC.getBlockRenderManager();
            BlockStateModel model = manager.getModel(state);
            List<BlockModelPart> modelParts = model.getParts(random);

            matrices.push();
            matrices.translate(translated.getX(), translated.getY(), translated.getZ());

            VertexConsumer consumer = provider.getBuffer(renderLayer);
            manager.renderBlock(state, translated, MC.world, matrices, consumer, false, modelParts);

            if (state.hasBlockEntity()) {
                BlockEntityRenderDispatcher rendererDispatcher = MC.getBlockEntityRenderDispatcher();

                Optional<BlockEntityType<?>> optionalType = Registries.BLOCK_ENTITY_TYPE.stream()
                        .filter(type -> type.supports(state))
                        .findFirst();
                if (optionalType.isPresent()) {
                    BlockEntityType<?> type = optionalType.get();
                    BlockEntity instance = type.instantiate(translated, state);
                    if (instance != null) instance.setWorld(MC.world);

                    if (instance instanceof SignBlockEntity signInstance) {
                        signInstance.changeText(text -> text.withMessage(1, Text.literal("Hello")), true);
                    }

                    BlockEntityRenderer<BlockEntity> renderer = instance == null ? null : rendererDispatcher.get(instance);
                    if (renderer != null) {
                        LightingProvider lighting = MC.world.getLightingProvider();
                        int worldLight = LightmapTextureManager.pack(
                                lighting.get(LightType.BLOCK).getLightLevel(translated),
                                lighting.get(LightType.SKY).getLightLevel(translated)
                        );

                        renderer.render(
                                instance, MC.getRenderTickCounter().getTickProgress(true),
                                matrices, provider,
                                worldLight, OverlayTexture.DEFAULT_UV,
                                MC.gameRenderer.getCamera().getCameraPos()
                        );
                    }
                }
            }

            matrices.pop();

            // Sodium renders all block entities AFTER normal blocks. This means that blocks won't be visible through
            // translucent block entities. To fix this, we can call provider.draw immediately after every single block/block entity.
            // Without sodium active, we can call provider.draw after rendering all blocks and block entities.
            if (sodium) provider.draw();
        }

        // Render all blocks and block entities at once if sodium isn't installed
        if (!sodium) provider.draw();
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
            if (result instanceof BlockHitResult blockHit) {
                blockPosVector = blockHit.getPos().add(blockHit.getSide().getDoubleVector().multiply(0.2f));
            } else blockPosVector = result.getPos();
            blockPos = BlockPos.ofFloored(blockPosVector);
        });

        WorldRenderEvents.LAST.register(context -> rendererContext = context);
    }

    private record QueuedBlock(BlockState state, BlockPos pos) { }

}
