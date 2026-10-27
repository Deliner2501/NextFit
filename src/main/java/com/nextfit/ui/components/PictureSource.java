package com.nextfit.ui.components;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import javax.imageio.ImageIO;

/**
 * Finds a picture from the text path stored in the data (exercise image,
 * member or trainer photo). The path may point to a file on disk or to a
 * resource inside the application; a missing picture gives an empty result.
 */
public final class PictureSource {

    private PictureSource() {
    }

    /**
     * Loads a picture from a stored path.
     *
     * @param path a file path or a classpath resource path (starting with "/"); may be null or blank
     * @return the picture, or empty when there is no path or it cannot be read
     */
    public static Optional<BufferedImage> load(String path) {
        if (path == null || path.isBlank()) {
            return Optional.empty();
        }

        // Una ruta "art:GRUPO" pide la ilustración dibujada de un grupo muscular.
        if (path.startsWith(MuscleArt.PREFIX)) {
            return MuscleArt.fromPath(path);
        }

        // Primero se busca como archivo en el disco (fotos subidas por el usuario).
        try {
            Path file = Path.of(path);
            if (Files.isRegularFile(file)) {
                return Optional.ofNullable(ImageIO.read(file.toFile()));
            }
        } catch (Exception e) {
            // Ruta inválida o archivo ilegible: se intenta como recurso de la aplicación.
        }
        // Si no es archivo, se busca dentro de los recursos (imágenes incluidas en la app).
        return Images.load(path);
    }

    /**
     * Scales a picture so it covers the whole area (like CSS "cover"): the picture
     * keeps its proportions and the part that does not fit is cut at the edges.
     *
     * @return a new picture of exactly width x height pixels
     */
    public static BufferedImage cover(BufferedImage source, int width, int height) {
        double scale = Math.max((double) width / source.getWidth(), (double) height / source.getHeight());
        int scaledWidth = Math.max(width, (int) Math.ceil(source.getWidth() * scale));
        int scaledHeight = Math.max(height, (int) Math.ceil(source.getHeight() * scale));
        BufferedImage scaled = Images.scale(source, scaledWidth, scaledHeight);
        // Se recorta el centro de la imagen ya escalada.
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = result.createGraphics();
        try {
            g2.drawImage(scaled, (width - scaledWidth) / 2, (height - scaledHeight) / 2, null);
        } finally {
            g2.dispose();
        }
        return result;
    }
}