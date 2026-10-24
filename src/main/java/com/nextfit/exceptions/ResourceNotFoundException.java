package com.nextfit.exceptions;

/**
 * Thrown when a resource (member, trainer, routine, exercise, etc.)
 * is looked up by its identifier and does not exist in the system.
 */
public class ResourceNotFoundException extends NextFitException {

    private static final String ERROR_CODE = "RESOURCE_NOT_FOUND";

    /**
     * Creates the exception building a descriptive Spanish message
     * from the type of resource and the identifier that was searched.
     *
     * @param resourceType the kind of resource (e.g. "socio", "rutina")
     * @param resourceId   the identifier that could not be found
     */
    public ResourceNotFoundException(String resourceType, String resourceId) {
        super(buildMessage(resourceType, resourceId), ERROR_CODE);
    }

    private static String buildMessage(String resourceType, String resourceId) {
        String article = (resourceType.endsWith("a") || resourceType.endsWith("ión")) ? "La" : "El";
        return article + " " + resourceType + " con identificador " + resourceId + " no fue encontrado";
    }
}