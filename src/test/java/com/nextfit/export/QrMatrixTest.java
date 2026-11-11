package com.nextfit.export;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QrMatrixTest {

    @Test
    void theGridIsSquareAndKeepsAQuietZone() {
        boolean[][] grid = QrMatrix.of("http://192.168.1.10:8765/a/token");
        assertEquals(grid.length, grid[0].length);
        for (int i = 0; i < grid.length; i++) {
            assertFalse(grid[0][i], "the outer ring is the quiet zone");
            assertFalse(grid[i][0]);
        }
    }

    @Test
    void theTopLeftFinderPatternStartsAfterTheQuietZone() {
        boolean[][] grid = QrMatrix.of("NF:1065400000");
        for (int i = 1; i <= 7; i++) {
            assertTrue(grid[1][i], "the finder pattern has a solid top edge of seven modules");
        }
    }

    @Test
    void aLongerTextMakesABiggerGrid() {
        assertTrue(QrMatrix.of("x".repeat(120)).length > QrMatrix.of("x").length);
    }
}
