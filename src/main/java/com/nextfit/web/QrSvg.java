package com.nextfit.web;

import com.nextfit.export.QrMatrix;

/** Draws a QR code as an SVG picture, so a phone shows it sharp at any size without any library. */
final class QrSvg {

    private QrSvg() {
    }

    /**
     * @param text the text to put in the QR
     * @return the SVG document, dark squares on a white background
     */
    static String of(String text) {
        boolean[][] grid = QrMatrix.of(text);
        int size = grid.length;
        StringBuilder path = new StringBuilder();
        for (int y = 0; y < size; y++) {
            int x = 0;
            while (x < size) {
                if (!grid[y][x]) {
                    x++;
                    continue;
                }
                int start = x;
                while (x < size && grid[y][x]) {
                    x++;
                }
                path.append('M').append(start).append(' ').append(y).append('h').append(x - start).append("v1h-")
                        .append(x - start).append('z');
            }
        }
        return "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 " + size + ' ' + size
                + "\" shape-rendering=\"crispEdges\"><rect width=\"100%\" height=\"100%\" fill=\"#fff\"/>"
                + "<path fill=\"#14101F\" d=\"" + path + "\"/></svg>";
    }
}
