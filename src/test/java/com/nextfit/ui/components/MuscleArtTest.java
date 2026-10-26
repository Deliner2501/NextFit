package com.nextfit.ui.components;

import com.nextfit.model.MuscleGroup;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MuscleArtTest {

    @Test
    void everyMuscleGroupHasAnIllustration() {
        for (MuscleGroup group : MuscleGroup.values()) {
            BufferedImage picture = MuscleArt.render(group);

            assertEquals(640, picture.getWidth());
            assertEquals(400, picture.getHeight());
        }
    }

    @Test
    void groupsAreDrawnDifferently() {
        int chestCenter = MuscleArt.render(MuscleGroup.CHEST).getRGB(320, 120);
        int legsCenter = MuscleArt.render(MuscleGroup.LEGS).getRGB(320, 120);

        assertNotEquals(chestCenter, legsCenter);
    }

    @Test
    void theStoredPathLoadsTheIllustration() {
        String path = MuscleArt.pathFor(MuscleGroup.BACK);

        assertEquals("art:BACK", path);
        assertTrue(PictureSource.load(path).isPresent());
    }

    @Test
    void anUnknownGroupGivesNoPicture() {
        assertFalse(MuscleArt.fromPath("art:UNKNOWN").isPresent());
        assertFalse(PictureSource.load("art:UNKNOWN").isPresent());
    }
}
