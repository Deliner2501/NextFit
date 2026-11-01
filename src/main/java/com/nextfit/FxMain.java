package com.nextfit;

/**
 * Starts NextFit with the JavaFX interface. It exists so the interface can be
 * run from NetBeans with "Run File" without writing the {@code --fx} argument.
 */
public final class FxMain {

    private FxMain() {
    }

    /**
     * Starts the application in JavaFX mode.
     *
     * @param args unused
     */
    public static void main(String[] args) {
        Main.main(new String[] {"--fx"});
    }
}
