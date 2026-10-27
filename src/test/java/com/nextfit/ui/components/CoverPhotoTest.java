package com.nextfit.ui.components;

import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CoverPhotoTest {

    @Test
    void aMissingPhotoIsNotAvailableAndDrawsNothing() {
        CoverPhoto photo = CoverPhoto.load("/images/does-not-exist.jpg", 0.5, 0.5);
        BufferedImage canvas = new BufferedImage(40, 20, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = canvas.createGraphics();
        photo.paint(g2, 40, 20);
        g2.dispose();

        assertFalse(photo.isAvailable());
        assertEquals(0, canvas.getRGB(10, 10));
    }

    @Test
    void anExistingPictureFillsTheWholeArea() {
        CoverPhoto photo = CoverPhoto.load("art:CHEST", 0.5, 0.5);
        BufferedImage canvas = new BufferedImage(300, 40, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = canvas.createGraphics();
        g2.setColor(Color.MAGENTA);
        g2.fillRect(0, 0, 300, 40);
        photo.paint(g2, 300, 40);
        g2.dispose();

        assertTrue(photo.isAvailable());
        assertNotEquals(Color.MAGENTA.getRGB(), canvas.getRGB(0, 0));
        assertNotEquals(Color.MAGENTA.getRGB(), canvas.getRGB(299, 39));
    }
}
