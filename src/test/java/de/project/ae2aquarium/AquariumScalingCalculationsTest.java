package de.project.ae2aquarium;

import de.project.ae2aquarium.block.AquariumControllerBlockEntity;
import de.project.ae2aquarium.multiblock.AquariumBounds;
import de.project.ae2aquarium.partition.AquariumPartition;
import de.project.ae2aquarium.partition.AquariumPartitionList;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class AquariumScalingCalculationsTest {

    @Test
    public void testInnerVolumeAndDropScaling() {
        AquariumBounds minTank = new AquariumBounds(0, 0, 0, 2, 2, 2);
        assertEquals(1, minTank.innerVolume());
        assertEquals(1, minTank.calculateDropsPerCycle(1.0, 1024));

        AquariumBounds medTank = new AquariumBounds(0, 0, 0, 4, 3, 4); // 5x4x5 -> inner 3x2x3 = 18
        assertEquals(18, medTank.innerVolume());
        assertEquals(18, medTank.calculateDropsPerCycle(1.0, 1024));
        assertEquals(36, medTank.calculateDropsPerCycle(2.0, 1024));

        AquariumBounds maxTank = new AquariumBounds(0, 0, 0, 15, 7, 15); // 16x8x16 -> inner 14x6x14 = 1176
        assertEquals(1176, maxTank.innerVolume());
        assertEquals(1024, maxTank.calculateDropsPerCycle(1.0, 1024), "Should respect maxDropsCap");
    }

    @Test
    public void testSpeedCardIntervalScaling() {
        int base = 60;
        assertEquals(60, AquariumControllerBlockEntity.calculateTargetInterval(base, 0));
        assertEquals(42, AquariumControllerBlockEntity.calculateTargetInterval(base, 1));
        assertEquals(27, AquariumControllerBlockEntity.calculateTargetInterval(base, 2));
        assertEquals(18, AquariumControllerBlockEntity.calculateTargetInterval(base, 3));
        assertEquals(12, AquariumControllerBlockEntity.calculateTargetInterval(base, 4));
    }

    @Test
    public void testPartitionPercentageTotals() {
        AquariumPartition p1 = new AquariumPartition(null, 45, false);
        AquariumPartition p2 = new AquariumPartition(null, 35, true);
        AquariumPartitionList list = new AquariumPartitionList(List.of(p1, p2));

        assertEquals(80, list.getTotalPercent());
        assertEquals(20, list.getUnallocatedPercent());
    }
}
