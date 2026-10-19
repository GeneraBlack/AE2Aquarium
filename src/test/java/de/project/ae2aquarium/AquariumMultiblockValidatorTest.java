package de.project.ae2aquarium;

import de.project.ae2aquarium.multiblock.AquariumBounds;
import de.project.ae2aquarium.multiblock.AquariumMultiblockValidator;
import de.project.ae2aquarium.multiblock.AquariumMultiblockValidator.BlockClassifier;
import de.project.ae2aquarium.multiblock.AquariumMultiblockValidator.BlockRole;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class AquariumMultiblockValidatorTest {

    private static class FakeWorld implements BlockClassifier {
        private final Map<BlockPos, BlockRole> blocks = new HashMap<>();

        public void set(int x, int y, int z, BlockRole role) {
            blocks.put(new BlockPos(x, y, z), role);
        }

        public void buildValidTank(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, BlockPos controllerPos) {
            for (int y = minY; y <= maxY; y++) {
                for (int x = minX; x <= maxX; x++) {
                    for (int z = minZ; z <= maxZ; z++) {
                        boolean interior = x > minX && x < maxX && y > minY && y < maxY && z > minZ && z < maxZ;
                        if (interior) {
                            set(x, y, z, BlockRole.AIR);
                        } else if (x == controllerPos.getX() && y == controllerPos.getY() && z == controllerPos.getZ()) {
                            set(x, y, z, BlockRole.CONTROLLER);
                        } else if (y == minY) {
                            set(x, y, z, BlockRole.CASING);
                        } else {
                            set(x, y, z, BlockRole.GLASS);
                        }
                    }
                }
            }
        }

        @Override
        public BlockRole classify(int x, int y, int z) {
            return blocks.getOrDefault(new BlockPos(x, y, z), BlockRole.AIR);
        }
    }

    @Test
    public void testSmall3x3x3TankWithBottomEdgeController() {
        FakeWorld world = new FakeWorld();
        BlockPos ctrl = new BlockPos(1, 0, 0);
        world.buildValidTank(0, 0, 0, 2, 2, 2, ctrl);

        AquariumBounds bounds = AquariumMultiblockValidator.findValidAquarium(world, ctrl, 16, 8, 16);
        assertNotNull(bounds, "3x3x3 aquarium should be valid");
        assertEquals(3, bounds.width());
        assertEquals(3, bounds.height());
        assertEquals(3, bounds.depth());
        assertEquals(1, bounds.innerVolume());
    }

    @Test
    public void testSmall3x3x3TankWithCornerController() {
        FakeWorld world = new FakeWorld();
        BlockPos ctrl = new BlockPos(0, 0, 0);
        world.buildValidTank(0, 0, 0, 2, 2, 2, ctrl);

        AquariumBounds bounds = AquariumMultiblockValidator.findValidAquarium(world, ctrl, 16, 8, 16);
        assertNotNull(bounds, "3x3x3 aquarium with corner controller should be valid");
        assertEquals(1, bounds.innerVolume());
    }

    @Test
    public void testLargeRectangular7x5x9Tank() {
        FakeWorld world = new FakeWorld();
        BlockPos ctrl = new BlockPos(10, 64, 20);
        world.buildValidTank(10, 64, 20, 16, 68, 28, ctrl);

        AquariumBounds bounds = AquariumMultiblockValidator.findValidAquarium(world, ctrl, 16, 8, 16);
        assertNotNull(bounds, "7x5x9 aquarium should be valid");
        assertEquals(7, bounds.width());
        assertEquals(5, bounds.height());
        assertEquals(9, bounds.depth());
        assertEquals(5 * 3 * 7, bounds.innerVolume());
    }

    @Test
    public void testRejectNonEmptyInterior() {
        FakeWorld world = new FakeWorld();
        BlockPos ctrl = new BlockPos(0, 0, 0);
        world.buildValidTank(0, 0, 0, 4, 4, 4, ctrl);
        // Place an obstruction inside the tank
        world.set(2, 2, 2, BlockRole.OTHER);

        AquariumBounds bounds = AquariumMultiblockValidator.findValidAquarium(world, ctrl, 16, 8, 16);
        assertNull(bounds, "Aquarium with obstructed interior should be rejected");
    }

    @Test
    public void testRejectSecondControllerInSameTank() {
        FakeWorld world = new FakeWorld();
        BlockPos ctrl = new BlockPos(0, 0, 0);
        world.buildValidTank(0, 0, 0, 3, 3, 3, ctrl);
        world.set(3, 0, 3, BlockRole.CONTROLLER);

        AquariumBounds bounds = AquariumMultiblockValidator.findValidAquarium(world, ctrl, 16, 8, 16);
        assertNull(bounds, "Aquarium with two controllers should be rejected");
    }

    @Test
    public void testRejectGlassOnBottomFloor() {
        FakeWorld world = new FakeWorld();
        BlockPos ctrl = new BlockPos(0, 0, 0);
        world.buildValidTank(0, 0, 0, 3, 3, 3, ctrl);
        world.set(1, 0, 1, BlockRole.GLASS);

        AquariumBounds bounds = AquariumMultiblockValidator.findValidAquarium(world, ctrl, 16, 8, 16);
        assertNull(bounds, "Aquarium with glass on bottom floor should be rejected");
    }
}
