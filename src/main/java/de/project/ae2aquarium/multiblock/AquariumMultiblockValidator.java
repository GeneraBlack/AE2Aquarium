package de.project.ae2aquarium.multiblock;

import de.project.ae2aquarium.block.AquariumCasingBlock;
import de.project.ae2aquarium.block.AquariumControllerBlock;
import de.project.ae2aquarium.block.AquariumGlassBlock;
import de.project.ae2aquarium.config.AquariumConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

public final class AquariumMultiblockValidator {

    public enum BlockRole {
        CONTROLLER,
        CASING,
        GLASS,
        AIR,
        OTHER
    }

    @FunctionalInterface
    public interface BlockClassifier {
        BlockRole classify(int x, int y, int z);
    }

    private AquariumMultiblockValidator() {
    }

    public static BlockRole classifyState(BlockState state) {
        if (state.isAir()) {
            return BlockRole.AIR;
        }
        if (state.getBlock() instanceof AquariumControllerBlock) {
            return BlockRole.CONTROLLER;
        }
        if (state.getBlock() instanceof AquariumCasingBlock) {
            return BlockRole.CASING;
        }
        if (state.getBlock() instanceof AquariumGlassBlock) {
            return BlockRole.GLASS;
        }
        return BlockRole.OTHER;
    }

    public static boolean isShellRole(BlockRole role) {
        return role == BlockRole.CONTROLLER || role == BlockRole.CASING || role == BlockRole.GLASS;
    }

    @Nullable
    public static AquariumBounds findValidAquarium(BlockGetter level, BlockPos controllerPos) {
        int maxW = 16;
        int maxH = 8;
        int maxD = 16;
        try {
            if (AquariumConfig.SPEC.isLoaded()) {
                maxW = AquariumConfig.MAX_TANK_WIDTH.get();
                maxH = AquariumConfig.MAX_TANK_HEIGHT.get();
                maxD = AquariumConfig.MAX_TANK_DEPTH.get();
            }
        } catch (Exception ignored) {
        }

        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        BlockClassifier classifier = (x, y, z) -> classifyState(level.getBlockState(mutable.set(x, y, z)));
        return findValidAquarium(classifier, controllerPos, maxW, maxH, maxD);
    }

    @Nullable
    public static AquariumBounds findValidAquarium(
            BlockClassifier classifier,
            BlockPos controllerPos,
            int maxWidth,
            int maxHeight,
            int maxDepth
    ) {
        if (classifier.classify(controllerPos.getX(), controllerPos.getY(), controllerPos.getZ()) != BlockRole.CONTROLLER) {
            return null;
        }

        // Strategy 1: Find an adjacent interior AIR block (including 3D diagonals for corner/edge controllers)
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) {
                        continue;
                    }
                    int ax = controllerPos.getX() + dx;
                    int ay = controllerPos.getY() + dy;
                    int az = controllerPos.getZ() + dz;
                    if (classifier.classify(ax, ay, az) == BlockRole.AIR) {
                        AquariumBounds candidate = expandFromAir(classifier, ax, ay, az, maxWidth, maxHeight, maxDepth);
                        if (candidate != null && isValidBounds(classifier, controllerPos, candidate, maxWidth, maxHeight, maxDepth)) {
                            return candidate;
                        }
                    }
                }
            }
        }

        // Strategy 2: Flood-fill connected shell blocks
        AquariumBounds shellCandidate = floodFillShellBounds(classifier, controllerPos, maxWidth, maxHeight, maxDepth);
        if (shellCandidate != null && isValidBounds(classifier, controllerPos, shellCandidate, maxWidth, maxHeight, maxDepth)) {
            return shellCandidate;
        }

        return null;
    }

    @Nullable
    private static AquariumBounds expandFromAir(
            BlockClassifier classifier,
            int startX,
            int startY,
            int startZ,
            int maxWidth,
            int maxHeight,
            int maxDepth
    ) {
        int maxInnerW = maxWidth - 2;
        int maxInnerH = maxHeight - 2;
        int maxInnerD = maxDepth - 2;

        int minX = startX;
        while (startX - minX < maxInnerW && classifier.classify(minX - 1, startY, startZ) == BlockRole.AIR) {
            minX--;
        }
        int maxX = startX;
        while (maxX - minX + 1 < maxInnerW && classifier.classify(maxX + 1, startY, startZ) == BlockRole.AIR) {
            maxX++;
        }

        int minY = startY;
        while (startY - minY < maxInnerH && classifier.classify(startX, minY - 1, startZ) == BlockRole.AIR) {
            minY--;
        }
        int maxY = startY;
        while (maxY - minY + 1 < maxInnerH && classifier.classify(startX, maxY + 1, startZ) == BlockRole.AIR) {
            maxY++;
        }

        int minZ = startZ;
        while (startZ - minZ < maxInnerD && classifier.classify(startX, startY, minZ - 1) == BlockRole.AIR) {
            minZ--;
        }
        int maxZ = startZ;
        while (maxZ - minZ + 1 < maxInnerD && classifier.classify(startX, startY, maxZ + 1) == BlockRole.AIR) {
            maxZ++;
        }

        return new AquariumBounds(minX - 1, minY - 1, minZ - 1, maxX + 1, maxY + 1, maxZ + 1);
    }

    @Nullable
    private static AquariumBounds floodFillShellBounds(
            BlockClassifier classifier,
            BlockPos origin,
            int maxWidth,
            int maxHeight,
            int maxDepth
    ) {
        int minX = origin.getX(), maxX = origin.getX();
        int minY = origin.getY(), maxY = origin.getY();
        int minZ = origin.getZ(), maxZ = origin.getZ();

        Set<Long> visited = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(origin);
        visited.add(origin.asLong());

        int maxNodes = (maxWidth + 2) * (maxHeight + 2) * (maxDepth + 2);

        while (!queue.isEmpty() && visited.size() <= maxNodes) {
            BlockPos cur = queue.removeFirst();
            minX = Math.min(minX, cur.getX());
            maxX = Math.max(maxX, cur.getX());
            minY = Math.min(minY, cur.getY());
            maxY = Math.max(maxY, cur.getY());
            minZ = Math.min(minZ, cur.getZ());
            maxZ = Math.max(maxZ, cur.getZ());

            if (maxX - minX + 1 > maxWidth || maxY - minY + 1 > maxHeight || maxZ - minZ + 1 > maxDepth) {
                return null;
            }

            for (Direction dir : Direction.values()) {
                int nx = cur.getX() + dir.getStepX();
                int ny = cur.getY() + dir.getStepY();
                int nz = cur.getZ() + dir.getStepZ();
                if (Math.abs(nx - origin.getX()) >= maxWidth
                        || Math.abs(ny - origin.getY()) >= maxHeight
                        || Math.abs(nz - origin.getZ()) >= maxDepth) {
                    continue;
                }
                long key = BlockPos.asLong(nx, ny, nz);
                if (!visited.contains(key)) {
                    BlockRole role = classifier.classify(nx, ny, nz);
                    if (isShellRole(role)) {
                        visited.add(key);
                        queue.addLast(new BlockPos(nx, ny, nz));
                    }
                }
            }
        }

        return new AquariumBounds(minX, minY, minZ, maxX, maxY, maxZ);
    }

    public static boolean isValidBounds(
            BlockClassifier classifier,
            BlockPos controllerPos,
            AquariumBounds bounds,
            int maxWidth,
            int maxHeight,
            int maxDepth
    ) {
        if (bounds.width() < 3 || bounds.width() > maxWidth
                || bounds.height() < 3 || bounds.height() > maxHeight
                || bounds.depth() < 3 || bounds.depth() > maxDepth) {
            return false;
        }
        if (!bounds.isOnShell(controllerPos)) {
            return false;
        }

        int controllerCount = 0;
        int glassCount = 0;

        for (int y = bounds.minY(); y <= bounds.maxY(); y++) {
            for (int x = bounds.minX(); x <= bounds.maxX(); x++) {
                for (int z = bounds.minZ(); z <= bounds.maxZ(); z++) {
                    BlockRole role = classifier.classify(x, y, z);

                    if (bounds.isInterior(x, y, z)) {
                        if (role != BlockRole.AIR) {
                            return false;
                        }
                    } else {
                        if (!isShellRole(role)) {
                            return false;
                        }
                        if (role == BlockRole.CONTROLLER) {
                            controllerCount++;
                            if (x != controllerPos.getX() || y != controllerPos.getY() || z != controllerPos.getZ()) {
                                return false;
                            }
                        }
                        if (y == bounds.minY()) {
                            // Bottom floor must be solid casing or the controller itself
                            if (role != BlockRole.CASING && role != BlockRole.CONTROLLER) {
                                return false;
                            }
                        } else if (role == BlockRole.GLASS) {
                            glassCount++;
                        }
                    }
                }
            }
        }

        return controllerCount == 1 && glassCount >= 4;
    }
}
