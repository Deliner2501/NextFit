package com.nextfit.service.suggest;

/**
 * One rule that adjusts the settings of a suggested routine. Each rule looks at the
 * request and returns the settings changed; if it does not apply, it returns them as they came.
 */
@FunctionalInterface
public interface SuggestionRule {

    // EXPLICACIÓN: es una interfaz con un solo método (ISP: pequeña a propósito).
    // El servicio recibe una lista de reglas y las aplica una tras otra. Para
    // agregar una regla nueva se crea otra clase que implemente esta interfaz y
    // no se toca el servicio (OCP: abierto a extensión, cerrado a modificación).

    /**
     * Applies the rule.
     *
     * @param settings the current settings
     * @param request  what the trainer asked for
     * @return the settings after the rule, never null
     */
    SuggestionSettings apply(SuggestionSettings settings, SuggestionRequest request);
}