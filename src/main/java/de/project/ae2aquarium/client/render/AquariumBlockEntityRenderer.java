package de.project.ae2aquarium.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import de.project.ae2aquarium.AE2Aquarium;
import de.project.ae2aquarium.block.AquariumControllerBlockEntity;
import de.project.ae2aquarium.multiblock.AquariumBounds;
import de.project.ae2aquarium.partition.AquariumPartition;
import de.project.ae2aquarium.partition.AquariumPartitionList;
import net.minecraft.Util;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SeaPickleBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.model.data.ModelData;

import java.util.List;

public class AquariumBlockEntityRenderer implements BlockEntityRenderer<AquariumControllerBlockEntity> {

    public static final ResourceLocation WHITE_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(AE2Aquarium.MODID, "textures/misc/white.png");

    private static final Item[] DEFAULT_SHOWCASE_CORALS = {
            Items.TUBE_CORAL_BLOCK,
            Items.BRAIN_CORAL_FAN,
            Items.BUBBLE_CORAL,
            Items.FIRE_CORAL_BLOCK,
            Items.HORN_CORAL_FAN,
            Items.SEA_PICKLE,
            Items.KELP
    };

    private final BlockRenderDispatcher blockRenderer;
    private final ItemRenderer itemRenderer;

    public AquariumBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        this.blockRenderer = context.getBlockRenderDispatcher();
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public AABB getRenderBoundingBox(AquariumControllerBlockEntity blockEntity) {
        AquariumBounds bounds = blockEntity.getFormedBounds();
        if (bounds != null) {
            return new AABB(
                    bounds.minX() - 0.5,
                    bounds.minY() - 0.5,
                    bounds.minZ() - 0.5,
                    bounds.maxX() + 1.5,
                    bounds.maxY() + 1.5,
                    bounds.maxZ() + 1.5
            );
        }
        BlockPos pos = blockEntity.getBlockPos();
        return new AABB(pos.getX() - 1, pos.getY() - 1, pos.getZ() - 1, pos.getX() + 2, pos.getY() + 2, pos.getZ() + 2);
    }

    @Override
    public boolean shouldRenderOffScreen(AquariumControllerBlockEntity blockEntity) {
        return blockEntity.isFormed();
    }

    @Override
    public int getViewDistance() {
        return 128;
    }

    @Override
    public void render(
            AquariumControllerBlockEntity blockEntity,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay
    ) {
        if (blockEntity.getLevel() == null || !blockEntity.isFormed()) {
            return;
        }
        AquariumBounds bounds = blockEntity.getFormedBounds();
        if (bounds == null || bounds.innerVolume() <= 0) {
            return;
        }

        BlockPos ctrlPos = blockEntity.getBlockPos();
        // Expand the rendered aquarium across the full multiblock footprint:
        // - Horizontally right up to the outer glass faces (0.025m thin glass margin)
        // - Vertically from top of the 4px (0.25m) base pedestal up to the bottom of the 3px (0.8125m) top canopy
        float minX = (bounds.minX() + 0.025f) - ctrlPos.getX();
        float minY = (bounds.minY() + 0.25f) - ctrlPos.getY();
        float minZ = (bounds.minZ() + 0.025f) - ctrlPos.getZ();
        float maxX = (bounds.maxX() + 0.975f) - ctrlPos.getX();
        float maxY = (bounds.maxY() + 0.8125f) - ctrlPos.getY();
        float maxZ = (bounds.maxZ() + 0.975f) - ctrlPos.getZ();

        float timeSeconds = (Util.getMillis() % 3600000L) / 1000.0f;
        long tankSeed = ctrlPos.asLong();
        int reefLight = LightTexture.FULL_BRIGHT;

        // 1. Render slim outer corner trims & full-footprint sculpted sandy seabed
        VertexConsumer solidBuffer = bufferSource.getBuffer(RenderType.entitySolid(WHITE_TEXTURE));
        renderPanoramicCornerFrame(poseStack, solidBuffer, minX, minY, minZ, maxX, maxY, maxZ, reefLight, packedOverlay);
        renderSeabed(poseStack, solidBuffer, bounds, ctrlPos, minX, minY, minZ, maxX, maxZ, reefLight, packedOverlay);

        // 2. Render procedurally swimming 3D tropical fish across the full tank width/depth/height
        int fishCount = Math.clamp(4 + (bounds.width() * bounds.depth()) / 2, 5, 48);
        ProceduralFishRenderer.renderFishSwarm(
                poseStack,
                solidBuffer,
                tankSeed,
                minX + 0.24f,
                minY + 0.45f,
                minZ + 0.24f,
                maxX - 0.24f,
                maxY - 0.16f,
                maxZ - 0.24f,
                fishCount,
                timeSeconds,
                packedOverlay
        );

        // 3. Render dynamic 3D coral reef across the entire tank footprint
        renderCoralReef(
                blockEntity,
                bounds,
                ctrlPos,
                minY,
                maxY,
                poseStack,
                bufferSource,
                timeSeconds,
                reefLight,
                packedOverlay
        );

        // 4. Render rising 3D bubbles & seamless panoramic glass + water volume
        VertexConsumer translucentBuffer = bufferSource.getBuffer(RenderType.entityTranslucent(WHITE_TEXTURE));
        int effectiveVolume = Math.max(bounds.innerVolume(), bounds.width() * Math.max(1, bounds.height() - 1) * bounds.depth());
        renderBubbles(
                poseStack,
                translucentBuffer,
                tankSeed,
                minX, minY, minZ,
                maxX, maxY, maxZ,
                effectiveVolume,
                timeSeconds,
                reefLight,
                packedOverlay
        );
        renderWaterVolume(
                poseStack,
                translucentBuffer,
                minX, minY, minZ,
                maxX, maxY, maxZ,
                timeSeconds,
                reefLight,
                packedOverlay
        );
    }

    private void renderPanoramicCornerFrame(
            PoseStack poseStack,
            VertexConsumer solidConsumer,
            float minX, float minY, float minZ,
            float maxX, float maxY, float maxZ,
            int light,
            int overlay
    ) {
        PoseStack.Pose pose = poseStack.last();
        float post = 0.035f;
        int frameColor = 0xFF28343E;
        int accentColor = 0xFF1CB5D8;

        // 4 ultra-slim vertical corner struts connecting the 4px base pedestal to the 3px top canopy
        ProceduralFishRenderer.drawBox(pose, solidConsumer, minX - 0.01f, minY, minZ - 0.01f, minX + post, maxY, minZ + post, frameColor, light, overlay);
        ProceduralFishRenderer.drawBox(pose, solidConsumer, maxX - post, minY, minZ - 0.01f, maxX + 0.01f, maxY, minZ + post, frameColor, light, overlay);
        ProceduralFishRenderer.drawBox(pose, solidConsumer, minX - 0.01f, minY, maxZ - post, minX + post, maxY, maxZ + 0.01f, frameColor, light, overlay);
        ProceduralFishRenderer.drawBox(pose, solidConsumer, maxX - post, minY, maxZ - post, maxX + 0.01f, maxY, maxZ + 0.01f, frameColor, light, overlay);

        // Subtle glowing aqua LED strip along the underside of the top canopy
        float ledT = 0.025f;
        ProceduralFishRenderer.drawBox(pose, solidConsumer, minX + post, maxY - ledT, minZ + post, maxX - post, maxY, minZ + post * 2.2f, accentColor, light, overlay);
        ProceduralFishRenderer.drawBox(pose, solidConsumer, minX + post, maxY - ledT, maxZ - post * 2.2f, maxX - post, maxY, maxZ - post, accentColor, light, overlay);
        ProceduralFishRenderer.drawBox(pose, solidConsumer, minX + post, maxY - ledT, minZ + post, minX + post * 2.2f, maxY, maxZ - post, accentColor, light, overlay);
        ProceduralFishRenderer.drawBox(pose, solidConsumer, maxX - post * 2.2f, maxY - ledT, minZ + post, maxX - post, maxY, maxZ - post, accentColor, light, overlay);
    }

    private void renderSeabed(
            PoseStack poseStack,
            VertexConsumer solidConsumer,
            AquariumBounds bounds,
            BlockPos ctrlPos,
            float minX, float minY, float minZ,
            float maxX, float maxZ,
            int light,
            int overlay
    ) {
        PoseStack.Pose pose = poseStack.last();
        // Base sandy layer across the entire multiblock floor (inset slightly behind front controller console)
        ProceduralFishRenderer.drawBox(
                pose,
                solidConsumer,
                minX + 0.22f,
                minY + 0.004f,
                minZ + 0.22f,
                maxX - 0.22f,
                minY + 0.16f,
                maxZ - 0.22f,
                0xFFDCC99E,
                light,
                overlay
        );

        // Outer sandy rim reaching right up to the panoramic glass walls (except at controller's front console)
        for (int gx = bounds.minX(); gx <= bounds.maxX(); gx++) {
            for (int gz = bounds.minZ(); gz <= bounds.maxZ(); gz++) {
                boolean isControllerColumn = (gx == ctrlPos.getX() && gz == ctrlPos.getZ() && ctrlPos.getY() == bounds.minY());
                long tileHash = mix64(BlockPos.asLong(gx, bounds.minY(), gz));
                float rx = gx - ctrlPos.getX();
                float rz = gz - ctrlPos.getZ();

                float x0 = (gx == bounds.minX()) ? rx + 0.04f : rx + 0.02f;
                float x1 = (gx == bounds.maxX()) ? rx + 0.96f : rx + 0.98f;
                float z0 = (gz == bounds.minZ()) ? rz + 0.04f : rz + 0.02f;
                float z1 = (gz == bounds.maxZ()) ? rz + 0.96f : rz + 0.98f;

                if (isControllerColumn) {
                    // Inset behind the 4px front controller terminal console
                    x0 = Math.max(x0, rx + 0.28f);
                    x1 = Math.min(x1, rx + 0.72f);
                    z0 = Math.max(z0, rz + 0.28f);
                    z1 = Math.min(z1, rz + 0.72f);
                }

                float moundH = 0.14f + ((tileHash & 0xFF) / 255.0f) * 0.10f;
                int sandShade = ((tileHash >>> 8) & 1) == 0 ? 0xFFE3D3AC : 0xFFD2BE92;
                ProceduralFishRenderer.drawBox(
                        pose,
                        solidConsumer,
                        x0,
                        minY + 0.01f,
                        z0,
                        x1,
                        minY + moundH,
                        z1,
                        sandShade,
                        light,
                        overlay
                );

                // Occasional dark reef rock accent
                if (!isControllerColumn && ((tileHash >>> 12) & 3) == 0) {
                    float rockX = Mth.clamp(rx + 0.20f + ((tileHash >>> 16) & 0x3F) / 255.0f, x0 + 0.06f, x1 - 0.36f);
                    float rockZ = Mth.clamp(rz + 0.20f + ((tileHash >>> 24) & 0x3F) / 255.0f, z0 + 0.06f, z1 - 0.36f);
                    ProceduralFishRenderer.drawBox(
                            pose,
                            solidConsumer,
                            rockX,
                            minY + moundH,
                            rockZ,
                            rockX + 0.32f,
                            minY + moundH + 0.16f,
                            rockZ + 0.32f,
                            0xFF435660,
                            light,
                            overlay
                    );
                }
            }
        }
    }

    private void renderCoralReef(
            AquariumControllerBlockEntity blockEntity,
            AquariumBounds bounds,
            BlockPos ctrlPos,
            float floorY,
            float ceilingY,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            float timeSeconds,
            int light,
            int overlay
    ) {
        AquariumPartitionList partitionList = blockEntity.getPartitions();
        float availableHeight = Math.max(1.2f, ceilingY - floorY - 0.25f);
        int maxKelpSegments = Math.max(1, Mth.floor(availableHeight / 0.65f));

        int tileIndex = 0;
        for (int gx = bounds.minX(); gx <= bounds.maxX(); gx++) {
            for (int gz = bounds.minZ(); gz <= bounds.maxZ(); gz++) {
                tileIndex++;
                boolean isControllerColumn = (gx == ctrlPos.getX() && gz == ctrlPos.getZ() && ctrlPos.getY() == bounds.minY());
                if (isControllerColumn) {
                    continue;
                }

                long tileSeed = mix64(BlockPos.asLong(gx, bounds.minY() + 1, gz));
                Item specimen = pickSpecimenForTile(partitionList, tileSeed, tileIndex);
                if (specimen == null || specimen == Items.AIR) {
                    continue;
                }

                // Shift outer-edge tiles slightly inward so corals sit lushly against the glass without clipping outside
                float offsetX = 0.5f;
                if (gx == bounds.minX()) offsetX = 0.58f;
                else if (gx == bounds.maxX()) offsetX = 0.42f;

                float offsetZ = 0.5f;
                if (gz == bounds.minZ()) offsetZ = 0.58f;
                else if (gz == bounds.maxZ()) offsetZ = 0.42f;

                float rx = (gx - ctrlPos.getX()) + offsetX;
                float rz = (gz - ctrlPos.getZ()) + offsetZ;
                float baseMoundY = floorY + 0.14f + ((tileSeed & 0xFF) / 255.0f) * 0.08f;

                float swayX = Mth.sin(timeSeconds * 2.1f + gx * 0.8f + gz * 0.6f) * 6.5f;
                float swayZ = Mth.cos(timeSeconds * 1.8f + gx * 0.6f - gz * 0.8f) * 6.5f;
                float yaw = ((tileSeed >>> 16) & 0xFF) / 255.0f * 360.0f;

                renderSpecimenAtTile(
                        blockEntity,
                        specimen,
                        tileSeed,
                        rx,
                        baseMoundY,
                        rz,
                        maxKelpSegments,
                        swayX,
                        swayZ,
                        yaw,
                        timeSeconds,
                        poseStack,
                        bufferSource,
                        light,
                        overlay
                );
            }
        }
    }

    private Item pickSpecimenForTile(AquariumPartitionList partitionList, long tileSeed, int tileIndex) {
        List<AquariumPartition> list = partitionList.partitions();
        if (list.isEmpty()) {
            return DEFAULT_SHOWCASE_CORALS[Math.floorMod(tileIndex, DEFAULT_SHOWCASE_CORALS.length)];
        }
        int totalPct = Math.max(1, partitionList.getTotalPercent());
        int roll = Math.floorMod((int) (tileSeed >>> 8), Math.max(100, totalPct));
        if (roll >= totalPct) {
            roll = Math.floorMod(tileIndex * 17, totalPct);
        }
        int cumulative = 0;
        for (AquariumPartition p : list) {
            cumulative += p.percent();
            if (roll < cumulative) {
                return p.target();
            }
        }
        return list.get(0).target();
    }

    private void renderSpecimenAtTile(
            AquariumControllerBlockEntity blockEntity,
            Item specimen,
            long tileSeed,
            float centerX,
            float baseY,
            float centerZ,
            int maxKelpSegments,
            float swayX,
            float swayZ,
            float yaw,
            float timeSeconds,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int light,
            int overlay
    ) {
        // Special multi-segment swaying Kelp stalk
        if (specimen == Items.KELP || specimen == Items.SEAGRASS) {
            BlockState plantState = (specimen == Items.KELP)
                    ? Blocks.KELP_PLANT.defaultBlockState()
                    : Blocks.SEAGRASS.defaultBlockState();
            int segments = (specimen == Items.KELP)
                    ? Math.min(maxKelpSegments, 1 + (int) (Math.abs(tileSeed) % Math.max(1, maxKelpSegments)))
                    : 1;
            for (int s = 0; s < segments; s++) {
                poseStack.pushPose();
                poseStack.translate(centerX, baseY + s * 0.64f, centerZ);
                poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
                poseStack.mulPose(Axis.XP.rotationDegrees(swayX * (1.0f + s * 0.35f)));
                poseStack.mulPose(Axis.ZP.rotationDegrees(swayZ * (1.0f + s * 0.35f)));
                poseStack.scale(0.76f, 0.72f, 0.76f);
                poseStack.translate(-0.5f, 0.0f, -0.5f);
                blockRenderer.renderSingleBlock(plantState, poseStack, bufferSource, light, overlay, ModelData.EMPTY, RenderType.cutout());
                poseStack.popPose();
            }
            return;
        }

        // Sea Pickle cluster with 1..4 glowing pickles
        if (specimen == Items.SEA_PICKLE) {
            int pickleCount = 1 + (int) (Math.abs(tileSeed >>> 4) % 4);
            BlockState pickleState = Blocks.SEA_PICKLE.defaultBlockState()
                    .setValue(SeaPickleBlock.PICKLES, pickleCount)
                    .setValue(SeaPickleBlock.WATERLOGGED, true);
            poseStack.pushPose();
            poseStack.translate(centerX, baseY, centerZ);
            poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
            poseStack.scale(0.88f, 0.88f, 0.88f);
            poseStack.translate(-0.5f, 0.0f, -0.5f);
            blockRenderer.renderSingleBlock(pickleState, poseStack, bufferSource, light, overlay, ModelData.EMPTY, RenderType.cutout());
            poseStack.popPose();
            return;
        }

        // BlockItem specimens (Coral Blocks, Coral Plants, Coral Fans, Sponges, Sea Lanterns)
        if (specimen instanceof BlockItem blockItem) {
            Block block = blockItem.getBlock();
            BlockState state = block.defaultBlockState();
            if (state.hasProperty(BlockStateProperties.WATERLOGGED)) {
                state = state.setValue(BlockStateProperties.WATERLOGGED, true);
            }

            boolean isSolidCoralBlock = (specimen == Items.TUBE_CORAL_BLOCK
                    || specimen == Items.BRAIN_CORAL_BLOCK
                    || specimen == Items.BUBBLE_CORAL_BLOCK
                    || specimen == Items.FIRE_CORAL_BLOCK
                    || specimen == Items.HORN_CORAL_BLOCK
                    || specimen == Items.WET_SPONGE
                    || specimen == Items.SPONGE
                    || specimen == Items.SEA_LANTERN);

            if (isSolidCoralBlock) {
                float blockScale = 0.58f + ((tileSeed >>> 10) & 0x3F) / 255.0f;
                poseStack.pushPose();
                poseStack.translate(centerX, baseY, centerZ);
                poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
                poseStack.scale(blockScale, blockScale, blockScale);
                poseStack.translate(-0.5f, 0.0f, -0.5f);
                blockRenderer.renderSingleBlock(state, poseStack, bufferSource, light, overlay, ModelData.EMPTY, RenderType.solid());
                poseStack.popPose();

                // Crown the coral block with a swaying coral fan/plant of the same family
                BlockState crownState = getCompanionCrownCoral(specimen);
                if (crownState != null) {
                    poseStack.pushPose();
                    poseStack.translate(centerX, baseY + blockScale * 0.96f, centerZ);
                    poseStack.mulPose(Axis.YP.rotationDegrees(yaw + 45.0f));
                    poseStack.mulPose(Axis.XP.rotationDegrees(swayX));
                    poseStack.mulPose(Axis.ZP.rotationDegrees(swayZ));
                    poseStack.scale(0.75f, 0.75f, 0.75f);
                    poseStack.translate(-0.5f, 0.0f, -0.5f);
                    blockRenderer.renderSingleBlock(crownState, poseStack, bufferSource, light, overlay, ModelData.EMPTY, RenderType.cutout());
                    poseStack.popPose();
                }
            } else {
                // Coral Plant or Coral Fan swaying directly on the seabed
                float pulse = 0.84f + 0.05f * Mth.sin(timeSeconds * 2.5f + centerX + centerZ);
                poseStack.pushPose();
                poseStack.translate(centerX, baseY, centerZ);
                poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
                poseStack.mulPose(Axis.XP.rotationDegrees(swayX));
                poseStack.mulPose(Axis.ZP.rotationDegrees(swayZ));
                poseStack.scale(pulse, pulse, pulse);
                poseStack.translate(-0.5f, 0.0f, -0.5f);
                blockRenderer.renderSingleBlock(state, poseStack, bufferSource, light, overlay, ModelData.EMPTY, RenderType.cutout());
                poseStack.popPose();
            }
            return;
        }

        // Non-block marine items (Nautilus Shell, Prismarine Shard/Crystals, Glow Ink Sac)
        ItemStack stack = new ItemStack(specimen);
        poseStack.pushPose();
        float bobY = 0.20f + 0.05f * Mth.sin(timeSeconds * 2.2f + centerX * 1.5f);
        poseStack.translate(centerX, baseY + bobY, centerZ);
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw + timeSeconds * 22.0f));
        poseStack.mulPose(Axis.XP.rotationDegrees(25.0f + swayX));
        poseStack.scale(0.62f, 0.62f, 0.62f);
        itemRenderer.renderStatic(
                stack,
                ItemDisplayContext.FIXED,
                light,
                overlay,
                poseStack,
                bufferSource,
                blockEntity.getLevel(),
                0
        );
        poseStack.popPose();
    }

    private BlockState getCompanionCrownCoral(Item coralBlockItem) {
        if (coralBlockItem == Items.TUBE_CORAL_BLOCK) {
            return Blocks.TUBE_CORAL_FAN.defaultBlockState();
        }
        if (coralBlockItem == Items.BRAIN_CORAL_BLOCK) {
            return Blocks.BRAIN_CORAL_FAN.defaultBlockState();
        }
        if (coralBlockItem == Items.BUBBLE_CORAL_BLOCK) {
            return Blocks.BUBBLE_CORAL.defaultBlockState();
        }
        if (coralBlockItem == Items.FIRE_CORAL_BLOCK) {
            return Blocks.FIRE_CORAL.defaultBlockState();
        }
        if (coralBlockItem == Items.HORN_CORAL_BLOCK) {
            return Blocks.HORN_CORAL_FAN.defaultBlockState();
        }
        if (coralBlockItem == Items.WET_SPONGE || coralBlockItem == Items.SPONGE) {
            return Blocks.SEA_PICKLE.defaultBlockState().setValue(SeaPickleBlock.PICKLES, 2).setValue(SeaPickleBlock.WATERLOGGED, true);
        }
        return null;
    }

    private void renderBubbles(
            PoseStack poseStack,
            VertexConsumer translucentConsumer,
            long tankSeed,
            float minX, float minY, float minZ,
            float maxX, float maxY, float maxZ,
            int effectiveVolume,
            float timeSeconds,
            int light,
            int overlay
    ) {
        int bubbleCount = Math.clamp(8 + effectiveVolume / 2, 10, 72);
        float spanX = Math.max(0.4f, maxX - minX - 0.3f);
        float spanY = Math.max(0.5f, maxY - minY - 0.35f);
        float spanZ = Math.max(0.4f, maxZ - minZ - 0.3f);

        PoseStack.Pose pose = poseStack.last();

        for (int i = 0; i < bubbleCount; i++) {
            long bSeed = mix64(tankSeed ^ ((i + 101L) * 0xBF58476D1CE4E5B9L));
            float baseX = minX + 0.15f + (((bSeed >>> 8) & 0xFFFF) / 65535.0f) * spanX;
            float baseZ = minZ + 0.15f + (((bSeed >>> 24) & 0xFFFF) / 65535.0f) * spanZ;
            float speed = 0.35f + (((bSeed >>> 40) & 0xFF) / 255.0f) * 0.45f;
            float phase = (((bSeed >>> 48) & 0xFF) / 255.0f);

            float progress = (timeSeconds * speed + phase) % 1.0f;
            float y = minY + 0.22f + progress * spanY;
            float wobbleX = Mth.sin(timeSeconds * 4.5f + i) * 0.045f;
            float wobbleZ = Mth.cos(timeSeconds * 4.2f + i) * 0.045f;

            float bx = baseX + wobbleX;
            float bz = baseZ + wobbleZ;
            float r = 0.024f + (i % 3) * 0.008f;

            ProceduralFishRenderer.drawBox(
                    pose,
                    translucentConsumer,
                    bx - r, y - r, bz - r,
                    bx + r, y + r, bz + r,
                    0x88CCF6FF,
                    light,
                    overlay
            );
        }
    }

    private void renderWaterVolume(
            PoseStack poseStack,
            VertexConsumer translucentConsumer,
            float minX, float minY, float minZ,
            float maxX, float maxY, float maxZ,
            float timeSeconds,
            int light,
            int overlay
    ) {
        PoseStack.Pose pose = poseStack.last();
        float x1 = minX;
        float y1 = minY + 0.02f;
        float z1 = minZ;
        float x2 = maxX;
        float z2 = maxZ;

        float waveOffset = 0.018f * Mth.sin(timeSeconds * 1.8f);
        float waterTopY = maxY - 0.04f + waveOffset;

        // Top shimmering water surface
        ProceduralFishRenderer.quad(
                pose, translucentConsumer,
                x1, waterTopY, z1,
                x1, waterTopY, z2,
                x2, waterTopY, z2,
                x2, waterTopY, z1,
                20, 195, 235, 88,
                light, overlay,
                0, 1, 0
        );
        // Underside of top water surface
        ProceduralFishRenderer.quad(
                pose, translucentConsumer,
                x1, waterTopY, z2,
                x1, waterTopY, z1,
                x2, waterTopY, z1,
                x2, waterTopY, z2,
                35, 215, 255, 72,
                light, overlay,
                0, -1, 0
        );

        // Seamless panoramic outer water & crystal-glass walls on all 4 sides
        int wr = 18, wg = 168, wb = 220, wa = 52;
        // North (-Z)
        ProceduralFishRenderer.quad(pose, translucentConsumer, x2, waterTopY, z1, x2, y1, z1, x1, y1, z1, x1, waterTopY, z1, wr, wg, wb, wa, light, overlay, 0, 0, -1);
        // South (+Z)
        ProceduralFishRenderer.quad(pose, translucentConsumer, x1, waterTopY, z2, x1, y1, z2, x2, y1, z2, x2, waterTopY, z2, wr, wg, wb, wa, light, overlay, 0, 0, 1);
        // West (-X)
        ProceduralFishRenderer.quad(pose, translucentConsumer, x1, waterTopY, z1, x1, y1, z1, x1, y1, z2, x1, waterTopY, z2, wr, wg, wb, wa, light, overlay, -1, 0, 0);
        // East (+X)
        ProceduralFishRenderer.quad(pose, translucentConsumer, x2, waterTopY, z2, x2, y1, z2, x2, y1, z1, x2, waterTopY, z1, wr, wg, wb, wa, light, overlay, 1, 0, 0);
    }

    private static long mix64(long z) {
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }
}
