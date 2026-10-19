package de.project.ae2aquarium.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.util.Mth;

public final class ProceduralFishRenderer {

    private static final int[][] TROPICAL_PALETTES = {
            {0xFFFF6B1A, 0xFFFFFFFF, 0xFF1A1A1A}, // Clownfish (Orange / White stripe / Black rim)
            {0xFF00B4D8, 0xFFFFD166, 0xFF073B4C}, // Regal Blue Tang (Cyan-Blue / Yellow tail)
            {0xFF9D4EDD, 0xFFFFBE0B, 0xFF3C096C}, // Royal Gramma (Purple / Gold)
            {0xFFE63946, 0xFFFF9F1C, 0xFF6A040F}, // Flame Angelfish (Crimson / Amber)
            {0xFF2EC4B6, 0xFFCBF3F0, 0xFF014F86}, // Reef Chromis (Turquoise / Pearl)
            {0xFFFF5D8F, 0xFFFFC8DD, 0xFF590D22}  // Anthias (Coral Pink / Rose)
    };

    private ProceduralFishRenderer() {
    }

    public static void renderFishSwarm(
            PoseStack poseStack,
            VertexConsumer solidConsumer,
            long tankSeed,
            float minX, float minY, float minZ,
            float maxX, float maxY, float maxZ,
            int fishCount,
            float timeSeconds,
            int packedOverlay
    ) {
        float spanX = Math.max(0.5f, maxX - minX);
        float spanY = Math.max(0.4f, maxY - minY);
        float spanZ = Math.max(0.5f, maxZ - minZ);

        float centerX = (minX + maxX) * 0.5f;
        float centerY = (minY + maxY) * 0.5f;
        float centerZ = (minZ + maxZ) * 0.5f;

        float radiusX = Math.max(0.20f, spanX * 0.5f - 0.25f);
        float radiusY = Math.max(0.12f, spanY * 0.5f - 0.20f);
        float radiusZ = Math.max(0.20f, spanZ * 0.5f - 0.25f);

        int fullBright = LightTexture.FULL_BRIGHT;

        for (int i = 0; i < fishCount; i++) {
            long seed = mix64(tankSeed ^ ((i + 1L) * 0x9E3779B97F4A7C15L));

            float freqX = 0.45f + ( positiveFloat(seed, 0) * 0.45f);
            float freqY = 0.30f + (positiveFloat(seed, 1) * 0.35f);
            float freqZ = 0.40f + (positiveFloat(seed, 2) * 0.45f);

            float phaseX = positiveFloat(seed, 3) * Mth.TWO_PI;
            float phaseY = positiveFloat(seed, 4) * Mth.TWO_PI;
            float phaseZ = positiveFloat(seed, 5) * Mth.TWO_PI;

            float orbitScaleX = 0.55f + 0.42f * positiveFloat(seed, 6);
            float orbitScaleY = 0.45f + 0.45f * positiveFloat(seed, 7);
            float orbitScaleZ = 0.55f + 0.42f * positiveFloat(seed, 8);

            float rx = radiusX * orbitScaleX;
            float ry = radiusY * orbitScaleY;
            float rz = radiusZ * orbitScaleZ;

            float t = timeSeconds;
            float dt = 0.04f;

            float x0 = centerX + sampleX(t, rx, freqX, freqZ, phaseX, phaseZ);
            float y0 = centerY + sampleY(t, ry, freqY, phaseY);
            float z0 = centerZ + sampleZ(t, rz, freqX, freqZ, phaseX, phaseZ);

            float x1 = centerX + sampleX(t + dt, rx, freqX, freqZ, phaseX, phaseZ);
            float y1 = centerY + sampleY(t + dt, ry, freqY, phaseY);
            float z1 = centerZ + sampleZ(t + dt, rz, freqX, freqZ, phaseX, phaseZ);

            float vx = x1 - x0;
            float vy = y1 - y0;
            float vz = z1 - z0;
            float horizDist = Mth.sqrt(vx * vx + vz * vz);

            float yawDeg = (float) Math.toDegrees(Mth.atan2(vx, vz));
            float pitchDeg = (float) -Math.toDegrees(Mth.atan2(vy, Math.max(0.0001f, horizDist)));

            int paletteIdx = Math.floorMod((int) (seed >>> 16), TROPICAL_PALETTES.length);
            int[] palette = TROPICAL_PALETTES[paletteIdx];
            float fishScale = 0.65f + 0.45f * positiveFloat(seed, 9);
            float tailWagAngle = Mth.sin(t * 9.5f + phaseX) * 24.0f;
            float finFlapAngle = Mth.cos(t * 7.0f + phaseY) * 18.0f;

            poseStack.pushPose();
            poseStack.translate(x0, y0, z0);
            poseStack.mulPose(Axis.YP.rotationDegrees(yawDeg));
            poseStack.mulPose(Axis.XP.rotationDegrees(pitchDeg * 0.65f));
            poseStack.scale(fishScale, fishScale, fishScale);

            drawSingleFish(poseStack, solidConsumer, palette[0], palette[1], palette[2], tailWagAngle, finFlapAngle, fullBright, packedOverlay);

            poseStack.popPose();
        }
    }

    private static float sampleX(float t, float rx, float fx, float fz, float px, float pz) {
        return rx * (0.84f * Mth.sin(fx * t + px) + 0.16f * Mth.sin(2.0f * fz * t + pz));
    }

    private static float sampleY(float t, float ry, float fy, float py) {
        return ry * Mth.sin(fy * t + py);
    }

    private static float sampleZ(float t, float rz, float fx, float fz, float px, float pz) {
        return rz * (0.84f * Mth.cos(fz * t + pz) + 0.16f * Mth.cos(2.0f * fx * t + px));
    }

    private static void drawSingleFish(
            PoseStack poseStack,
            VertexConsumer consumer,
            int primaryColor,
            int accentColor,
            int detailColor,
            float tailWagDeg,
            float finFlapDeg,
            int light,
            int overlay
    ) {
        // Main body (oval-like stepped box along Z: nose at +Z, tail at -Z)
        drawBox(poseStack.last(), consumer, -0.045f, -0.065f, -0.09f, 0.045f, 0.065f, 0.11f, primaryColor, light, overlay);

        // Nose taper
        drawBox(poseStack.last(), consumer, -0.032f, -0.042f, 0.11f, 0.032f, 0.042f, 0.155f, primaryColor, light, overlay);

        // Tropical vertical stripe band
        drawBox(poseStack.last(), consumer, -0.049f, -0.067f, -0.01f, 0.049f, 0.067f, 0.045f, accentColor, light, overlay);

        // Eyes (left and right)
        drawBox(poseStack.last(), consumer, -0.051f, 0.005f, 0.08f, -0.044f, 0.028f, 0.105f, detailColor, light, overlay);
        drawBox(poseStack.last(), consumer, 0.044f, 0.005f, 0.08f, 0.051f, 0.028f, 0.105f, detailColor, light, overlay);

        // Dorsal fin (top)
        drawBox(poseStack.last(), consumer, -0.008f, 0.065f, -0.07f, 0.008f, 0.115f, 0.05f, accentColor, light, overlay);

        // Ventral fin (bottom)
        drawBox(poseStack.last(), consumer, -0.008f, -0.105f, -0.06f, 0.008f, -0.065f, 0.02f, accentColor, light, overlay);

        // Pectoral fins (animated side fins)
        poseStack.pushPose();
        poseStack.translate(0.045f, -0.015f, 0.03f);
        poseStack.mulPose(Axis.YP.rotationDegrees(-25.0f - finFlapDeg));
        drawBox(poseStack.last(), consumer, 0.0f, -0.02f, -0.04f, 0.055f, 0.02f, 0.0f, accentColor, light, overlay);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(-0.045f, -0.015f, 0.03f);
        poseStack.mulPose(Axis.YP.rotationDegrees(25.0f + finFlapDeg));
        drawBox(poseStack.last(), consumer, -0.055f, -0.02f, -0.04f, 0.0f, 0.02f, 0.0f, accentColor, light, overlay);
        poseStack.popPose();

        // Articulated Caudal Tail
        poseStack.pushPose();
        poseStack.translate(0.0f, 0.0f, -0.09f);
        poseStack.mulPose(Axis.YP.rotationDegrees(tailWagDeg));
        // Tail peduncle
        drawBox(poseStack.last(), consumer, -0.025f, -0.040f, -0.055f, 0.025f, 0.040f, 0.0f, primaryColor, light, overlay);
        // Forked tail fin
        poseStack.translate(0.0f, 0.0f, -0.05f);
        poseStack.mulPose(Axis.YP.rotationDegrees(tailWagDeg * 0.65f));
        drawBox(poseStack.last(), consumer, -0.010f, -0.085f, -0.075f, 0.010f, 0.085f, 0.0f, accentColor, light, overlay);
        poseStack.popPose();
    }

    public static void drawBox(
            PoseStack.Pose pose,
            VertexConsumer consumer,
            float x1, float y1, float z1,
            float x2, float y2, float z2,
            int argb,
            int light,
            int overlay
    ) {
        int a = (argb >>> 24) & 0xFF;
        int r = (argb >>> 16) & 0xFF;
        int g = (argb >>> 8) & 0xFF;
        int b = argb & 0xFF;

        // Top (+Y)
        quad(pose, consumer, x1, y2, z1, x1, y2, z2, x2, y2, z2, x2, y2, z1, r, g, b, a, light, overlay, 0, 1, 0);
        // Bottom (-Y)
        int br = (int) (r * 0.75f), bg = (int) (g * 0.75f), bb = (int) (b * 0.75f);
        quad(pose, consumer, x1, y1, z2, x1, y1, z1, x2, y1, z1, x2, y1, z2, br, bg, bb, a, light, overlay, 0, -1, 0);
        // North (-Z)
        int sr = (int) (r * 0.88f), sg = (int) (g * 0.88f), sb = (int) (b * 0.88f);
        quad(pose, consumer, x2, y2, z1, x2, y1, z1, x1, y1, z1, x1, y2, z1, sr, sg, sb, a, light, overlay, 0, 0, -1);
        // South (+Z)
        quad(pose, consumer, x1, y2, z2, x1, y1, z2, x2, y1, z2, x2, y2, z2, sr, sg, sb, a, light, overlay, 0, 0, 1);
        // West (-X)
        quad(pose, consumer, x1, y2, z1, x1, y1, z1, x1, y1, z2, x1, y2, z2, sr, sg, sb, a, light, overlay, -1, 0, 0);
        // East (+X)
        quad(pose, consumer, x2, y2, z2, x2, y1, z2, x2, y1, z1, x2, y2, z1, sr, sg, sb, a, light, overlay, 1, 0, 0);
    }

    public static void quad(
            PoseStack.Pose pose,
            VertexConsumer consumer,
            float x1, float y1, float z1,
            float x2, float y2, float z2,
            float x3, float y3, float z3,
            float x4, float y4, float z4,
            int r, int g, int b, int a,
            int light, int overlay,
            float nx, float ny, float nz
    ) {
        consumer.addVertex(pose, x1, y1, z1).setColor(r, g, b, a).setUv(0.0f, 0.0f).setOverlay(overlay).setLight(light).setNormal(pose, nx, ny, nz);
        consumer.addVertex(pose, x2, y2, z2).setColor(r, g, b, a).setUv(0.0f, 1.0f).setOverlay(overlay).setLight(light).setNormal(pose, nx, ny, nz);
        consumer.addVertex(pose, x3, y3, z3).setColor(r, g, b, a).setUv(1.0f, 1.0f).setOverlay(overlay).setLight(light).setNormal(pose, nx, ny, nz);
        consumer.addVertex(pose, x4, y4, z4).setColor(r, g, b, a).setUv(1.0f, 0.0f).setOverlay(overlay).setLight(light).setNormal(pose, nx, ny, nz);
    }

    private static long mix64(long z) {
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }

    private static float positiveFloat(long seed, int salt) {
        long h = mix64(seed + salt * 0x9E3779B97F4A7C15L);
        return ((h >>> 40) & 0xFFFFFF) / (float) 0x1000000;
    }
}
