package de.project.ae2aquarium.multiblock;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;

public record AquariumBounds(
        int minX,
        int minY,
        int minZ,
        int maxX,
        int maxY,
        int maxZ
) {
    public AquariumBounds {
        if (minX > maxX || minY > maxY || minZ > maxZ) {
            throw new IllegalArgumentException("Invalid AquariumBounds: min must be <= max");
        }
    }

    public int width() {
        return maxX - minX + 1;
    }

    public int height() {
        return maxY - minY + 1;
    }

    public int depth() {
        return maxZ - minZ + 1;
    }

    public int innerWidth() {
        return Math.max(0, width() - 2);
    }

    public int innerHeight() {
        return Math.max(0, height() - 2);
    }

    public int innerDepth() {
        return Math.max(0, depth() - 2);
    }

    public int innerVolume() {
        return innerWidth() * innerHeight() * innerDepth();
    }

    public int calculateDropsPerCycle(double volumeMultiplier, int maxDropsCap) {
        int vol = innerVolume();
        if (vol <= 0) {
            return 0;
        }
        int scaled = Math.max(1, (int) Math.floor(vol * Math.max(0.01, volumeMultiplier)));
        return Math.min(maxDropsCap, scaled);
    }

    public boolean contains(BlockPos pos) {
        return contains(pos.getX(), pos.getY(), pos.getZ());
    }

    public boolean contains(int x, int y, int z) {
        return x >= minX && x <= maxX
                && y >= minY && y <= maxY
                && z >= minZ && z <= maxZ;
    }

    public boolean isInterior(int x, int y, int z) {
        return x > minX && x < maxX
                && y > minY && y < maxY
                && z > minZ && z < maxZ;
    }

    public boolean isOnShell(BlockPos pos) {
        return isOnShell(pos.getX(), pos.getY(), pos.getZ());
    }

    public boolean isOnShell(int x, int y, int z) {
        return contains(x, y, z) && !isInterior(x, y, z);
    }

    public CompoundTag saveToTag() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("MinX", minX);
        tag.putInt("MinY", minY);
        tag.putInt("MinZ", minZ);
        tag.putInt("MaxX", maxX);
        tag.putInt("MaxY", maxY);
        tag.putInt("MaxZ", maxZ);
        return tag;
    }

    public static AquariumBounds loadFromTag(CompoundTag tag) {
        int minX = tag.getInt("MinX");
        int minY = tag.getInt("MinY");
        int minZ = tag.getInt("MinZ");
        int maxX = tag.getInt("MaxX");
        int maxY = tag.getInt("MaxY");
        int maxZ = tag.getInt("MaxZ");
        if (minX > maxX || minY > maxY || minZ > maxZ) {
            return null;
        }
        return new AquariumBounds(minX, minY, minZ, maxX, maxY, maxZ);
    }
}
