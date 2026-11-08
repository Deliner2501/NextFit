package com.nextfit.ui.fx;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.RotateTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.Transition;
import javafx.animation.TranslateTransition;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.util.Duration;

import java.util.List;
import java.util.function.LongFunction;

/**
 * The small animations of the interface, in one place so every screen moves the same way: cards that rise
 * and fade in one after another, elements that lift when the mouse is over them, menu entries that slide
 * a little and primary buttons that sink when pressed. Everything is short (under half a second) and only
 * changes how a node is drawn, never its layout, so nothing jumps around.
 */
final class FxMotion {

    private static final Duration ENTER = Duration.millis(420);
    private static final double ENTER_SHIFT = 18;

    private FxMotion() {
    }

    /**
     * Makes the nodes rise a little while they fade in, one after another.
     *
     * @param nodes       the nodes, in the order they should appear
     * @param delayMillis wait before the first one starts
     * @param stepMillis  extra wait between one node and the next
     */
    static void enter(List<? extends Node> nodes, double delayMillis, double stepMillis) {
        enter(nodes, delayMillis, stepMillis, ENTER_SHIFT);
    }

    /**
     * Same as {@link #enter(List, double, double)} with the distance the nodes travel chosen by the caller.
     *
     * @param nodes       the nodes, in the order they should appear
     * @param delayMillis wait before the first one starts
     * @param stepMillis  extra wait between one node and the next
     * @param shift       how many pixels the node rises while it appears
     */
    static void enter(List<? extends Node> nodes, double delayMillis, double stepMillis, double shift) {
        int index = 0;
        for (Node node : List.copyOf(nodes)) {
            Duration delay = Duration.millis(delayMillis + index * stepMillis);
            node.setOpacity(0);
            node.setTranslateY(shift);
            FadeTransition fade = new FadeTransition(ENTER, node);
            fade.setFromValue(0);
            fade.setToValue(1);
            fade.setDelay(delay);
            TranslateTransition rise = new TranslateTransition(ENTER, node);
            rise.setFromY(shift);
            rise.setToY(0);
            rise.setDelay(delay);
            rise.setInterpolator(Interpolator.EASE_OUT);
            new ParallelTransition(fade, rise).play();
            index++;
        }
    }

    /**
     * Makes a number climb from zero to its value in a moment.
     *
     * @param label  the label that shows the number
     * @param target the final value
     * @param format how to write a value as text
     */
    static void countUp(Label label, long target, LongFunction<String> format) {
        if (target == 0) {
            label.setText(format.apply(0));
            return;
        }
        Transition animation = new Transition() {
            {
                setCycleDuration(Duration.millis(750));
            }

            @Override
            protected void interpolate(double fraction) {
                double eased = 1 - Math.pow(1 - fraction, 3);
                label.setText(format.apply(Math.round(target * eased)));
            }
        };
        animation.play();
    }

    /**
     * Makes a node pop in: it grows from small with a little overshoot while it fades in.
     *
     * @param node        the node
     * @param delayMillis wait before it starts
     */
    static void popIn(Node node, double delayMillis) {
        Duration length = Duration.millis(450);
        node.setOpacity(0);
        FadeTransition fade = new FadeTransition(length, node);
        fade.setFromValue(0);
        fade.setToValue(1);
        ScaleTransition grow = new ScaleTransition(length, node);
        grow.setFromX(0.5);
        grow.setFromY(0.5);
        grow.setToX(1);
        grow.setToY(1);
        // A curve that goes a bit past the end and comes back: the "bounce" of a badge that lands.
        grow.setInterpolator(new Interpolator() {
            @Override
            protected double curve(double t) {
                // "Back" easing: it passes the end a little and settles. (SPLINE cannot do it: it only takes points from 0 to 1.)
                double c = 1.70158;
                double u = t - 1;
                return 1 + (c + 1) * u * u * u + c * u * u;
            }
        });
        ParallelTransition all = new ParallelTransition(fade, grow);
        all.setDelay(Duration.millis(delayMillis));
        all.play();
    }

    /**
     * Makes a round chart appear spinning and growing from a bit smaller.
     *
     * @param node        the chart
     * @param delayMillis wait before it starts
     */
    static void spinIn(Node node, double delayMillis) {
        Duration length = Duration.millis(900);
        node.setOpacity(0);
        FadeTransition fade = new FadeTransition(length, node);
        fade.setFromValue(0);
        fade.setToValue(1);
        RotateTransition spin = new RotateTransition(length, node);
        spin.setFromAngle(-100);
        spin.setToAngle(0);
        spin.setInterpolator(Interpolator.EASE_OUT);
        ScaleTransition grow = new ScaleTransition(length, node);
        grow.setFromX(0.85);
        grow.setFromY(0.85);
        grow.setToX(1);
        grow.setToY(1);
        grow.setInterpolator(Interpolator.EASE_OUT);
        ParallelTransition all = new ParallelTransition(fade, spin, grow);
        all.setDelay(Duration.millis(delayMillis));
        all.play();
    }

    /**
     * Makes a node grow slightly while the mouse is over it.
     *
     * @param node  the node to animate
     * @param scale the size it reaches, for example 1.02 for two percent bigger
     */
    static void hoverLift(Node node, double scale) {
        ScaleTransition grow = new ScaleTransition(Duration.millis(150), node);
        grow.setToX(scale);
        grow.setToY(scale);
        ScaleTransition shrink = new ScaleTransition(Duration.millis(170), node);
        shrink.setToX(1);
        shrink.setToY(1);
        node.addEventHandler(MouseEvent.MOUSE_ENTERED, e -> {
            shrink.stop();
            grow.playFromStart();
        });
        node.addEventHandler(MouseEvent.MOUSE_EXITED, e -> {
            grow.stop();
            shrink.playFromStart();
        });
    }

    /**
     * Makes a node slide sideways a few pixels while the mouse is over it.
     *
     * @param node the node to animate
     * @param dx   how many pixels it moves to the right
     */
    static void nudge(Node node, double dx) {
        TranslateTransition forward = new TranslateTransition(Duration.millis(140), node);
        forward.setToX(dx);
        TranslateTransition back = new TranslateTransition(Duration.millis(170), node);
        back.setToX(0);
        node.addEventHandler(MouseEvent.MOUSE_ENTERED, e -> {
            back.stop();
            forward.playFromStart();
        });
        node.addEventHandler(MouseEvent.MOUSE_EXITED, e -> {
            forward.stop();
            back.playFromStart();
        });
    }

    /**
     * Makes every primary and secondary button of the scene sink a little while it is pressed. It is installed
     * once on the scene, so the buttons of any screen (also the ones other people write) get it without code.
     *
     * @param scene the scene of the application
     */
    static void installPressEffect(Scene scene) {
        Node[] held = new Node[1];
        scene.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {
            Node button = pressable(e.getTarget());
            if (button != null && !button.isDisabled()) {
                held[0] = button;
                scaleTo(button, 0.96, 80);
            }
        });
        scene.addEventFilter(MouseEvent.MOUSE_RELEASED, e -> {
            if (held[0] != null) {
                scaleTo(held[0], 1, 140);
                held[0] = null;
            }
        });
    }

    private static Node pressable(Object target) {
        Node node = target instanceof Node n ? n : null;
        while (node != null) {
            if (node instanceof ButtonBase && (node.getStyleClass().contains("nf-primary")
                    || node.getStyleClass().contains("nf-primary-small")
                    || node.getStyleClass().contains("nf-secondary"))) {
                return node;
            }
            node = node.getParent();
        }
        return null;
    }

    private static void scaleTo(Node node, double scale, double millis) {
        ScaleTransition transition = new ScaleTransition(Duration.millis(millis), node);
        transition.setToX(scale);
        transition.setToY(scale);
        transition.play();
    }
}
