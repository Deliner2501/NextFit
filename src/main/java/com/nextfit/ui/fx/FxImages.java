package com.nextfit.ui.fx;

import javafx.geometry.Side;
import javafx.scene.image.Image;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundImage;
import javafx.scene.layout.BackgroundPosition;
import javafx.scene.layout.BackgroundRepeat;
import javafx.scene.layout.BackgroundSize;
import javafx.scene.layout.Region;

import java.net.URL;

/**
 * Pictures for the JavaFX interface. JavaFX draws images at the real pixel size
 * of the screen by itself, so the photos stay sharp at any Windows scale.
 */
public final class FxImages {

    private FxImages() {
    }

    /**
     * Creates an area that shows a photo covering all of it (like CSS "cover").
     * A missing photo gives an empty area instead of an error.
     *
     * @param resource the classpath path, e.g. "/images/login/background.jpg"
     * @param focusX   horizontal point to keep, 0 (left) to 1 (right)
     * @param focusY   vertical point to keep, 0 (top) to 1 (bottom)
     * @return the area
     */
    public static Region cover(String resource, double focusX, double focusY) {
        Region area = new Region();
        URL url = FxImages.class.getResource(resource);
        if (url == null) {
            return area;
        }
        Image image = new Image(url.toExternalForm(), false);
        BackgroundImage background = new BackgroundImage(image,
                BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT,
                new BackgroundPosition(Side.LEFT, focusX, true, Side.TOP, focusY, true),
                new BackgroundSize(BackgroundSize.AUTO, BackgroundSize.AUTO, false, false, false, true));
        area.setBackground(new Background(background));
        return area;
    }

    /**
     * Loads a picture, smoothed, for an ImageView.
     *
     * @param resource the classpath path
     * @return the image, or null when it does not exist
     */
    public static Image load(String resource) {
        URL url = FxImages.class.getResource(resource);
        return url == null ? null : new Image(url.toExternalForm(), false);
    }
}
