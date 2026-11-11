package com.nextfit.export;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CarnetPdfTest {

    @TempDir
    Path folder;

    private static boolean[][] checker(int n) {
        boolean[][] grid = new boolean[n][n];
        for (int y = 0; y < n; y++) {
            for (int x = 0; x < n; x++) {
                grid[y][x] = (x + y) % 2 == 0;
            }
        }
        return grid;
    }

    private static CarnetPdf.Card card(String name, Path photo) {
        return new CarnetPdf.Card(name, "1065400000", "Membresía hasta 30/12/2026", checker(29), photo);
    }

    private static int pages(byte[] pdf) {
        String text = new String(pdf, StandardCharsets.ISO_8859_1);
        int count = 0;
        for (int at = text.indexOf("/Type /Page "); at >= 0; at = text.indexOf("/Type /Page ", at + 1)) {
            count++;
        }
        return count;
    }

    @Test
    void oneCarnetMakesAValidSinglePageFile() {
        byte[] pdf = CarnetPdf.toBytes(List.of(card("Valentina Rojas", null)));
        String head = new String(pdf, 0, 8, StandardCharsets.ISO_8859_1);
        assertTrue(head.startsWith("%PDF-1.4"));
        assertTrue(new String(pdf, StandardCharsets.ISO_8859_1).trim().endsWith("%%EOF"));
        assertEquals(1, pages(pdf));
    }

    @Test
    void fourCarnetsFitOnePageAndTheFifthStartsAnother() {
        List<CarnetPdf.Card> cards = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            cards.add(card("Socio " + i, null));
        }
        assertEquals(1, pages(CarnetPdf.toBytes(cards)));
        cards.add(card("Socio 4", null));
        assertEquals(2, pages(CarnetPdf.toBytes(cards)));
    }

    @Test
    void aVeryLongNameDoesNotBreakTheLayout() {
        String name = "María de los Ángeles Fernanda Rodríguez de la Hoz y Valderrama";
        assertEquals(1, pages(CarnetPdf.toBytes(List.of(card(name, null)))));
    }

    @Test
    void thePhotoIsEmbeddedAndAMissingOneFallsBackToInitials() throws IOException {
        Path photo = folder.resolve("foto.png");
        BufferedImage image = new BufferedImage(300, 200, BufferedImage.TYPE_INT_RGB);
        ImageIO.write(image, "png", photo.toFile());
        byte[] withPhoto = CarnetPdf.toBytes(List.of(card("Ana Pérez", photo)));
        byte[] missing = CarnetPdf.toBytes(List.of(card("Ana Pérez", folder.resolve("no-existe.png"))));
        byte[] without = CarnetPdf.toBytes(List.of(card("Ana Pérez", null)));
        assertTrue(new String(withPhoto, StandardCharsets.ISO_8859_1).contains("/Im1"));
        assertEquals(without.length, missing.length);
    }

    @Test
    void savesTheFile() throws IOException {
        Path file = folder.resolve("carnets.pdf");
        CarnetPdf.save(file, List.of(card("Ana Pérez", null)));
        assertTrue(Files.size(file) > 1000);
    }

    @Test
    void refusesAnEmptyList() {
        assertThrows(IllegalArgumentException.class, () -> CarnetPdf.toBytes(List.of()));
    }
}
