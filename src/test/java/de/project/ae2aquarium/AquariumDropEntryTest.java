package de.project.ae2aquarium;

import de.project.ae2aquarium.recipe.AquariumDropEntry;
import net.minecraft.util.RandomSource;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class AquariumDropEntryTest {

    @Test
    public void testRollCountFixed() {
        AquariumDropEntry fixed = new AquariumDropEntry(null, 55, 2, 2);
        RandomSource random = RandomSource.create(42L);
        for (int i = 0; i < 30; i++) {
            assertEquals(2, fixed.rollCount(random));
        }
    }

    @Test
    public void testRollCountRange() {
        AquariumDropEntry ranged = new AquariumDropEntry(null, 55, 1, 4);
        RandomSource random = RandomSource.create(1234L);
        boolean seenMin = false;
        boolean seenMax = false;
        for (int i = 0; i < 200; i++) {
            int c = ranged.rollCount(random);
            assertTrue(c >= 1 && c <= 4);
            if (c == 1) seenMin = true;
            if (c == 4) seenMax = true;
        }
        assertTrue(seenMin && seenMax, "Should roll both min and max counts");
    }

    @Test
    public void testRollCountClamping() {
        AquariumDropEntry invalid = new AquariumDropEntry(null, 10, -3, 0);
        RandomSource random = RandomSource.create(7L);
        assertEquals(1, invalid.rollCount(random));
    }
}
