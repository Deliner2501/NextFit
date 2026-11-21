package com.nextfit.ui.fx;

import com.nextfit.service.AccessResult;
import javafx.css.PseudoClass;
import javafx.scene.control.Label;

/** Displays a consistent entry, exit or denial message for each access method. */
final class AccessFeedback extends Label {

    private static final PseudoClass ENTRY = PseudoClass.getPseudoClass("entry");
    private static final PseudoClass EXIT = PseudoClass.getPseudoClass("exit");
    private static final PseudoClass DENIED = PseudoClass.getPseudoClass("denied");

    AccessFeedback() {
        getStyleClass().add("nf-access-feedback");
        setWrapText(true);
        setMaxWidth(Double.MAX_VALUE);
        visibleProperty().bind(textProperty().isNotEmpty());
        managedProperty().bind(visibleProperty());
    }

    /** @param result the recorded entry, exit or denial */
    void showResult(AccessResult result) {
        setState(result.outcome());
        setText(switch (result.outcome()) {
            case ENTRY -> "Entrada registrada\n" + result.member().getFullName();
            case EXIT -> "Salida registrada\n" + result.member().getFullName();
            case DENIED -> "Acceso denegado\n" + result.message();
        });
    }

    /** @param message the enrollment message, or an empty string to clear the panel */
    void showMessage(String message) {
        setState(null);
        setText(message);
    }

    private void setState(AccessResult.Outcome state) {
        pseudoClassStateChanged(ENTRY, state == AccessResult.Outcome.ENTRY);
        pseudoClassStateChanged(EXIT, state == AccessResult.Outcome.EXIT);
        pseudoClassStateChanged(DENIED, state == AccessResult.Outcome.DENIED);
    }
}
